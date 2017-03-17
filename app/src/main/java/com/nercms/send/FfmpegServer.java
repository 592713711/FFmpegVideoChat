package com.nercms.send;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.nercms.Config;
import com.nercms.R;
import com.nercms.receive.Receive;
import com.nercms.receive.Videoplay;
import com.nercms.video.VideoServer;
import com.zsg.ffmpegvideolib.Ffmpeg;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by zsg on 2017/3/16.
 */
public class FfmpegServer {


    //private Send encode;      //编码器
    //public Receive decode;   //解码器
    private boolean isRunning; //线程运行标志

    private RtpSocket rtp_socket = null; //创建RTP套接字

    private RtpPacket rtp_send_packet = null; //创建RTP发送包
    private RtpPacket rtp_receive_packet = null; //创建RTP接受包

    //接受 处理
    //private long decoder_handle = 0; //拼帧器的句柄
    private byte[] frmbuf = new byte[65536]; //帧缓存
    private byte[] socket_receive_Buffer = new byte[10240]; //包缓存
    private byte[] buffer = new byte[2048];

    //发送
    private long encoder_handle = -1; //创建编码器的句柄
    private int send_packetNum = 0; //包的数目
    private int[] send_packetSize = new int[200]; //包的尺寸
   // private byte[] send_stream = new byte[65536]; //码流
    private byte[] socket_send_Buffer = new byte[65536]; //缓存 stream->socketBuffer->rtp_socket

    private int server_video_port;

    private Context context;

    private VideoServer.ReceiveVideoCallback receiveVideoCallback;

    public static long sendDatatime = 0;

    InetSocketAddress serverAddress;        //服务器地址
    InetSocketAddress clientAddress;        //客户端地址   用于p2p

    int type;

    private static String GET_CLIENT_IP_CMD = "001";            //向服务器获取对方ip和端口
    private static String GET_CLIENT_IP_RSP = "002";
    private static String REPLAY_CMD = "003";
    private static String REPLAY_RSP = "004";

    public Ffmpeg ffmpeg;

    public FfmpegServer(VideoServer.ReceiveVideoCallback callback, int server_port) {
        this.receiveVideoCallback = callback;
        this.server_video_port = server_port;
        ffmpeg = new Ffmpeg();
        initServer();
    }

    public void initServer() {
        // encode = new Send();
        // decode = new Receive();
        ffmpeg.videoinit();

        serverAddress = new InetSocketAddress(Config.serverIP,
                server_video_port);
        try {
            //rtp_socket = new RtpSocket(new SipdroidSocket(20000)); //初始化套接字，20000为接收端口号
            rtp_socket = new RtpSocket(new SipdroidSocket(20000));
            //doStart();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //throughNet();

    }


    /**
     * 发起net穿透
     */
    private void throughNet() {

        Thread thread = new Thread(new Runnable() {
            boolean b = true;
            byte data[] = new byte[2048];
            DatagramPacket packet = new DatagramPacket(data, 2048);

            @Override
            public void run() {
                String msg = "QC@001@00";
                try {
                    rtp_socket.send(msg, serverAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (b) {
                    try {
                        rtp_socket.socket.receive(packet);
                        String receivemsg = new String(data).trim();
                        Log.d(Config.TAG, "收到消息：" + receivemsg);
                        String m[] = receivemsg.split("[@]");
                        if (m.length > 0 && m[1].equals(GET_CLIENT_IP_RSP)) {
                            Log.d("InetSocketAddress", m[2] + " " + m[3]);
                            clientAddress = new InetSocketAddress(m[2], Integer.parseInt(m[3]) + 1);
                            rtp_socket.send("QC@003@00", clientAddress);
                            rtp_socket.send("QC@003@00", serverAddress);
                            b = false;
                            doStart();
                        } else if (m.length > 0 && m[1].equals(REPLAY_RSP)) {

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }


    public void doStart() {
        //初始化解码器


        //初始化接受包
        rtp_receive_packet = new RtpPacket(socket_receive_Buffer, 0); //初始化 ,socketBuffer改变时rtp_Packet也跟着改变
        /**
         * 因为可能传输数据过大 会将一次数据分割成好几段来传输
         * 接受方 根据序列号和结束符 来将这些数据拼接成完整数据
         */
        //初始化解码器
        //decoder_handle = decode.CreateH264Packer(); //创建拼帧器
        //decode.CreateDecoder(352, 288); //创建解码器
        isRunning = true;
        DecoderThread decoder = new DecoderThread();
        decoder.start(); //启动一个线程

        //初始化发送包
        rtp_send_packet = new RtpPacket(socket_send_Buffer, 0);

        //初始化编码器
        // if (encoder_handle == -1)
        //   encoder_handle = encode.CreateEncoder(352, 288); //调用底层函数，创建编码器
    }


    int count = 0;
    int sequence=0;
    public byte[] sendData(byte[] frame) {
        //Log.i(Config.TAG,"数据："+ Arrays.toString(frame));


        final byte[] send_stream = ffmpeg.videoencode(frame);


        //通过RTP协议发送帧

        final long timestamp = System.currentTimeMillis(); //设定时间戳
        sendDatatime = timestamp;


        /**
         * 因为可能传输数据过大 会将一次数据分割成好几段来传输
         * 接受方 根据序列号和结束符 来将这些数据拼接成完整数据
         */
        new Thread(new Runnable() {
            @Override
            public void run() {

                rtp_send_packet.setPayloadType(2);//定义负载类型，视频为2
                rtp_send_packet.setMarker(true); //是否是最后一个RTP包
                rtp_send_packet.setSequenceNumber(sequence); //序列号依次加1
                rtp_send_packet.setTimestamp(timestamp); //时间戳

                //Log.d("log", "序列号:" + sequence + " 时间：" + timestamp);
                rtp_send_packet.setPayloadLength(send_stream.length); //包的长度，packetSize[i]+头文件
                //从码流stream的pos处开始复制，从socketBuffer的第12个字节开始粘贴，packetSize为粘贴的长度
                System.arraycopy(send_stream, 0, socket_send_Buffer, 12, send_stream.length); //把一个包存在socketBuffer中
                //rtp_packet.setPayload(socketBuffer, rtp_packet.getLength());
                //Log.e(Config.TAG, "发送 timestamp:" + timestamp+"  发送大小："+rtp_send_packet.getLength());
                Log.e(Config.TAG, "发送 sequence:" + sequence+" 总长度："+rtp_send_packet.getLength()+"  包长度"+send_stream.length+"  "+Arrays.toString(send_stream));
                sequence++;
                try {
                    //Log.e(Config.TAG, "发送视频数据："+send_stream.length);
                    rtp_socket.send(rtp_send_packet, serverAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }).start();

        return ffmpeg.videodecode(send_stream);

    }


    /**
     * 接收rtp数据并解码 线程
     */
    class DecoderThread extends Thread {
        public void run() {

            while (isRunning) {
                try {
                    if (rtp_socket != null)
                        rtp_socket.receive(rtp_receive_packet); //接收一个包
                    else
                        stopServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                int packetSize = rtp_receive_packet.getPayloadLength(); //获取包的大小
               // Log.e(Config.TAG, "接收 packetSize:"+packetSize+"  总大小："+rtp_receive_packet.getLength()+"  Type:"+rtp_receive_packet.getPayloadType()+" sequence:"+rtp_receive_packet.getSequenceNumber()+" Timestamp"+rtp_receive_packet.getTimestamp());
                if (packetSize <= 0 || packetSize >= socket_receive_Buffer.length)
                    continue;
                if (rtp_receive_packet.getPayloadType() != 2) //确认负载类型为2
                    continue;
                //System.arraycopy(socket_receive_Buffer, 12, buffer, 0, packetSize); //socketBuffer->buffer
                int sequence = rtp_receive_packet.getSequenceNumber(); //获取序列号
                long timestamp = rtp_receive_packet.getTimestamp(); //获取时间戳
                int bMark = rtp_receive_packet.hasMarker() == true ? 1 : 0; //是否是最后一个包
                byte[] data = rtp_receive_packet.getPayload();
                //int frmSize = decode.PackH264Frame(decoder_handle, buffer, packetSize, bMark, (int) timestamp, sequence, frmbuf); //packer=拼帧器，frmbuf=帧缓存
                Log.e(Config.TAG, "接收  sequence:" + sequence +"总长度："+rtp_receive_packet.getLength()+ " 包长度:" + data.length+"  "+rtp_receive_packet.getPayloadLength()+" "+Arrays.toString(data));

                receiveVideoCallback.receiveVideoStream(data, data.length, timestamp);
            }


            if (rtp_socket != null) {
                rtp_socket.close();
                rtp_socket = null;
            }

        }
    }

    public void stopServer() {
        isRunning = false;
        if (rtp_socket != null) {
            rtp_socket.close();
            rtp_socket = null;
        }

    }


}
