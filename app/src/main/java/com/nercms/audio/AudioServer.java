package com.nercms.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

import com.googlecode.androidilbc.Codec;
import com.nercms.Config;
import com.nercms.audio.AudioConfig;
import com.nercms.model.AudioData;
import com.nercms.video.VideoServer;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;


public class AudioServer {

    String LOG = "Recorder ";
    private AudioRecord audioRecord;
    private boolean isRecording = false;

    //编码
    private static final int BUFFER_FRAME_SIZE = 480;
    private int audioBufSize = 0;
    private byte[] samples;// data
    private int bufferSize = 0;     //每次读取缓冲区的大小


    byte[] encodedData = new byte[256];     //编码后的 数据
    int encodeSize = 0;                 //编码后的大小

    //解码
    private static final int MAX_BUFFER_SIZE = 2048;
    private byte[] decodedData = new byte[1024];// data of decoded

    private RtpSocket rtp_socket = null; //创建RTP套接字
    private RtpPacket rtp_send_packet = null; //创建RTP发送包
    private RtpPacket rtp_receive_packet = null; //创建RTP接受包
    private int remote_port;

    //接受 处理
    private byte[] socket_receive_Buffer = new byte[2048]; //包缓存
    private byte[] buffer = new byte[2048];

    //发送
    private byte[] socket_send_Buffer = new byte[65536]; //缓存 stream->socketBuffer->rtp_socket

    //播放
    private AudioTrack audioTrack;

    DecoderThread decoderThread;
    EncoderThread encoderThread;
    PlayAudioThread playAudioThread;

    public static long lastTime;

    public LinkedList<AudioData> dataLinkedList;        //缓存数组

    public AudioServer(int server_port) {
        this.remote_port = server_port;
        dataLinkedList=new LinkedList<>();
        initSocket();
    }

    /**
     * 初始化rtp通道
     */
    private void initSocket() {
        //初始化解码器
        if (rtp_socket == null) {
            try {
                //rtp_socket = new RtpSocket(new SipdroidSocket(20000)); //初始化套接字，20000为接收端口号
                rtp_socket = new RtpSocket(new SipdroidSocket(13243), InetAddress.getByName(Config.serverIP), remote_port);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            //初始化接受包
            rtp_receive_packet = new RtpPacket(socket_receive_Buffer, 0); //初始化 ,socketBuffer改变时rtp_Packet也跟着改变
            //初始化发送包
            rtp_send_packet = new RtpPacket(socket_send_Buffer, 0);
        }

        decoderThread = new DecoderThread();
        encoderThread = new EncoderThread();
        playAudioThread=new PlayAudioThread();
    }


    /*
     * start recording
     */
    public void startRecording() {


        bufferSize = BUFFER_FRAME_SIZE;

        audioBufSize = AudioRecord.getMinBufferSize(AudioConfig.SAMPLERATE,
                AudioConfig.RECORDER_CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT);
        if (audioBufSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(LOG, "audioBufSize error");
            return;
        }

        // 初始化recorder
        if (null == audioRecord) {
            audioBufSize = AudioRecord.getMinBufferSize(8000, 16, 2);
            audioRecord = new AudioRecord(AudioConfig.AUDIO_RESOURCE,
                    AudioConfig.SAMPLERATE,
                    AudioConfig.RECORDER_CHANNEL_CONFIG,
                    AudioConfig.AUDIO_FORMAT, audioBufSize);
            audioRecord = new AudioRecord(1, 8000, 16, 2, audioBufSize);

        }
        initAudioTrack();

        //录制结束


        samples = new byte[audioBufSize];
        audioRecord.startRecording();

        isRecording = true;
        decoderThread.start();
        encoderThread.start();
        playAudioThread.start();

    }

    /*
     * stop
     */
    public void stopRecording() {
        this.isRecording = false;
        close();
    }

    int bufferRead;

    public boolean isRecording() {
        return isRecording;
    }

    int count = 0;
    long time1;

    public void encode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                samples = new byte[audioBufSize];
                bufferRead = audioRecord.read(samples, 0, bufferSize);
                //Log.e(Config.TAG, "bufferRead：" + bufferRead + " " + audioBufSize + " " + bufferSize);
                if (bufferRead > 0) {
                    //编码

                    encodeSize = Codec.instance().encode(samples, 0,
                            bufferRead, encodedData, 0);

                    //通过RTP协议发送帧
                    final long timestamp = System.currentTimeMillis(); //设定时间戳

                    if (encodeSize > 0) {
                        rtp_send_packet.setPayloadType(1);      //定义负载类型，音频为1
                        rtp_send_packet.setMarker(true);
                        rtp_send_packet.setSequenceNumber(0);
                        rtp_send_packet.setTimestamp(timestamp);
                        rtp_send_packet.setPayloadLength(encodeSize);   //编码数据长度
                        //从码流stream的pos处开始复制，从socketBuffer的第12个字节开始粘贴，packetSize为粘贴的长度
                        System.arraycopy(encodedData, 0, socket_send_Buffer, 12, encodeSize); //把一个包存在socketBuffer中

                        try {
                            if (rtp_socket != null) {
                               // Log.d(Config.TAG, "发送音频数据 :" + (count++) + " 间隔：" + (System.currentTimeMillis() - time1));
                                time1 = System.currentTimeMillis();
                                rtp_socket.send(rtp_send_packet);
                            } else {
                                Log.d(Config.TAG, "rtp_socket=null");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // clear data
                        encodedData = new byte[encodedData.length];
                    }
                }
            }
        }).start();


    }


    class EncoderThread extends Thread {
        public void run() {
            while (isRecording) {
                samples = new byte[audioBufSize];
                bufferRead = audioRecord.read(samples, 0, bufferSize);
                //Log.e(Config.TAG, "bufferRead：" + bufferRead + " " + audioBufSize + " " + bufferSize);
                if (bufferRead > 0) {
                    //编码

                    encodeSize = Codec.instance().encode(samples, 0,
                            bufferRead, encodedData, 0);
                    //encodeSize=100;

                    //通过RTP协议发送帧
                    final long timestamp = System.currentTimeMillis(); //设定时间戳

                    if (encodeSize > 0) {
                        rtp_send_packet.setPayloadType(1);      //定义负载类型，音频为1
                        rtp_send_packet.setMarker(true);
                        rtp_send_packet.setSequenceNumber(0);
                        rtp_send_packet.setTimestamp(timestamp);
                        rtp_send_packet.setPayloadLength(encodeSize);   //编码数据长度
                        //从码流stream的pos处开始复制，从socketBuffer的第12个字节开始粘贴，packetSize为粘贴的长度
                        System.arraycopy(encodedData, 0, socket_send_Buffer, 12, encodeSize); //把一个包存在socketBuffer中

                        try {
                            if (rtp_socket != null) {
                                 Log.e(Config.TAG, "发送音频数据 :"+timestamp);
                                // time1 = System.currentTimeMillis();
                                rtp_socket.send(rtp_send_packet);
                            } else {
                                Log.d(Config.TAG, "rtp_socket=null");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // clear data
                        encodedData = new byte[encodedData.length];
                    }


                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(LOG + "end recording");

        }

    }

    /**
     * 接收rtp数据并解码 线程
     */
    class DecoderThread extends Thread {
        public void run() {
            while (isRecording) {
                try {
                    if (rtp_socket != null)
                        rtp_socket.receive(rtp_receive_packet); //接收一个包
                    else
                        continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int packetSize = rtp_receive_packet.getPayloadLength(); //获取包的大小
                //Log.e(Config.TAG, "接受音频数据:"+packetSize);
                if (packetSize <= 0||packetSize>2048)
                    continue;
                if (rtp_receive_packet.getPayloadType() != 1) //确认负载类型为1
                    continue;
                System.arraycopy(socket_receive_Buffer, 12, buffer, 0, packetSize); //socketBuffer->buffer
                int sequence = rtp_receive_packet.getSequenceNumber(); //获取序列号
                long timestamp = rtp_receive_packet.getTimestamp(); //获取时间戳
                int bMark = rtp_receive_packet.hasMarker() == true ? 1 : 0; //是否是最后一个包
                byte[] encoded = rtp_receive_packet.getPayload();

                Log.e(Config.TAG, "接受音频数据:"+timestamp);
               // Log.d("log", "Type:" + rtp_receive_packet.getPayloadType() + " bMark:" + bMark + " packetSize:" + packetSize + " PayloadType:" + rtp_receive_packet.getPayloadType() + " timestamp:" + timestamp);
                if (encoded.length >= 0) {
                    //解码
                    int decodeSize = Codec.instance().decode(encoded, 0,
                            encoded.length, decodedData, 0);
                    if (decodeSize > 0) {
                        // 放入缓存数组中
                        synchronized (dataLinkedList) {
                            dataLinkedList.addLast(new AudioData(decodedData, decodeSize, timestamp));

                            //lastTime = timestamp;
                            //int audiosize = audioTrack.write(decodedData, 0, decodeSize);
                            // clear data
                            decodedData = new byte[decodedData.length];
                        }
                    }
                }


            }

        }
    }

    //定时播放数组中的内容
    class PlayAudioThread extends Thread{
        @Override
        public void run() {
            while (isRecording){
                if(dataLinkedList.size()>0){
                    AudioData audioData=null;
                    //Log.d(Config.TAG, "播放音频数据1");
                    synchronized (dataLinkedList) {
                         audioData = dataLinkedList.removeFirst();
                    }
                    if(audioData!=null) {
                        lastTime = audioData.time;
                       // Log.d(Config.TAG, "播放音频数据2");
                        int audiosize = audioTrack.write(audioData.data, 0, audioData.size);
                    }
                }

                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean initAudioTrack() {
        int bufferSize = AudioRecord.getMinBufferSize(AudioConfig.SAMPLERATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioConfig.AUDIO_FORMAT);
        if (bufferSize < 0) {
            Log.d(Config.TAG, LOG + "initialize error!");
            return false;
        }
        Log.d(LOG, "Player初始化的 buffersize是 " + bufferSize);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                AudioConfig.SAMPLERATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioConfig.AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);
        // set volume:设置播放音量
        audioTrack.setStereoVolume(1.0f, 1.0f);
        audioTrack.play();
        return true;
    }

    public void close() {
        if (this.audioTrack != null) {
            if (this.audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                this.audioTrack.stop();
                this.audioTrack.release();
            }
        }

        if (rtp_socket != null) {
            rtp_socket.close();
            rtp_socket = null;

        }

        if (audioRecord != null) {
            audioRecord.stop();
        }

    }


}
