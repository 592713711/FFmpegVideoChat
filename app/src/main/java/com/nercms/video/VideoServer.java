package com.nercms.video;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nercms.Config;
import com.nercms.activity.UserActivity;
import com.nercms.activity.VideoClockActivity;
import com.nercms.model.VideoData;
import com.nercms.receive.Receive;
import com.nercms.send.Send;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by zsg on 2016/10/20.
 */
public class VideoServer {
    private Send encode;      //编码器
    public Receive decode;   //解码器
    private boolean isRunning; //线程运行标志

    private RtpSocket rtp_socket = null; //创建RTP套接字

    private RtpPacket rtp_send_packet = null; //创建RTP发送包
    private RtpPacket rtp_receive_packet = null; //创建RTP接受包

    //接受 处理
    private long decoder_handle = 0; //拼帧器的句柄
    private byte[] frmbuf = new byte[65536]; //帧缓存
    private byte[] socket_receive_Buffer = new byte[2048]; //包缓存
    private byte[] buffer = new byte[2048];

    //发送
    private long encoder_handle = -1; //创建编码器的句柄
    private int send_packetNum = 0; //包的数目
    private int[] send_packetSize = new int[200]; //包的尺寸
    private byte[] send_stream = new byte[65536]; //码流
    private byte[] socket_send_Buffer = new byte[65536]; //缓存 stream->socketBuffer->rtp_socket

    private int server_video_port;

    private Context context;

    private ReceiveVideoCallback receiveVideoCallback;

    public static long sendDatatime = 0;

    InetSocketAddress serverAddress;        //服务器地址
    InetSocketAddress clientAddress;        //客户端地址   用于p2p

    int type;

    private static String GET_CLIENT_IP_CMD = "001";            //向服务器获取对方ip和端口
    private static String GET_CLIENT_IP_RSP = "002";
    private static String REPLAY_CMD = "003";
    private static String REPLAY_RSP = "004";



    public VideoServer(ReceiveVideoCallback callback, int server_port) {
        this.receiveVideoCallback = callback;
        this.server_video_port = server_port;

        initServer();
    }

    public void initServer() {
        encode = new Send();
        decode = new Receive();

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
        decoder_handle = decode.CreateH264Packer(); //创建拼帧器
        decode.CreateDecoder(352, 288); //创建解码器
        isRunning = true;
        DecoderThread decoder = new DecoderThread();
        decoder.start(); //启动一个线程

        //初始化发送包
        rtp_send_packet = new RtpPacket(socket_send_Buffer, 0);

        //初始化编码器
        if (encoder_handle == -1)
            encoder_handle = encode.CreateEncoder(352, 288); //调用底层函数，创建编码器
    }


    int count = 0;

    public void sendData(byte[] frame) {
        //Log.i(Config.TAG,"数据："+ Arrays.toString(frame));
        if (encoder_handle != -1) {
            //底层函数，返回包的数目，返回包的大小存储在数组packetSize中，返回码流在stream中
            send_packetNum = encode.EncoderOneFrame(encoder_handle, -1, frame, send_stream, send_packetSize);
            // Log.d("log", "原始数据大小：" + frame.length + "  转码后数据大小：" + send_stream.length);
            if (send_packetNum > 0) {

                //通过RTP协议发送帧
                final int[] pos = {0}; //从码流头部开始取
                final long timestamp = System.currentTimeMillis(); //设定时间戳
                sendDatatime = timestamp;
                /**
                 * 因为可能传输数据过大 会将一次数据分割成好几段来传输
                 * 接受方 根据序列号和结束符 来将这些数据拼接成完整数据
                 */
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int sequence = 0; //初始化序列号
                        for (int i = 0; i < send_packetNum; i++) {

                            rtp_send_packet.setPayloadType(2);//定义负载类型，视频为2
                            rtp_send_packet.setMarker(i == send_packetNum - 1 ? true : false); //是否是最后一个RTP包
                            rtp_send_packet.setSequenceNumber(sequence++); //序列号依次加1
                            rtp_send_packet.setTimestamp(timestamp); //时间戳
                            //Log.d("log", "序列号:" + sequence + " 时间：" + timestamp);
                            rtp_send_packet.setPayloadLength(send_packetSize[i]); //包的长度，packetSize[i]+头文件
                            //从码流stream的pos处开始复制，从socketBuffer的第12个字节开始粘贴，packetSize为粘贴的长度
                            System.arraycopy(send_stream, pos[0], socket_send_Buffer, 12, send_packetSize[i]); //把一个包存在socketBuffer中
                            pos[0] += send_packetSize[i]; //重定义下次开始复制的位置
                            //rtp_packet.setPayload(socketBuffer, rtp_packet.getLength());
                            //  Log.d("log", "序列号:" + sequence + " bMark:" + rtp_packet.hasMarker() + " packetSize:" + packetSize[i] + " tPayloadType:2" + " timestamp:" + timestamp);
                            try {
                               // Log.e(Config.TAG, "发送视频数据：");

                                rtp_socket.send(rtp_send_packet, serverAddress);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }).start();


            }
        }
    }

    public byte[] encodeData(byte[] frame) {
        send_packetNum = encode.EncoderOneFrame(encoder_handle, -1, frame, send_stream, send_packetSize);
        if (send_packetNum > 0) {
            return Arrays.copyOf(send_stream,send_stream.length);
        }

        return null;
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

                if (packetSize <= 0 || packetSize >= socket_receive_Buffer.length)
                    continue;
                if (rtp_receive_packet.getPayloadType() != 2) //确认负载类型为2
                    continue;
                System.arraycopy(socket_receive_Buffer, 12, buffer, 0, packetSize); //socketBuffer->buffer
                int sequence = rtp_receive_packet.getSequenceNumber(); //获取序列号
                long timestamp = rtp_receive_packet.getTimestamp(); //获取时间戳
                int bMark = rtp_receive_packet.hasMarker() == true ? 1 : 0; //是否是最后一个包
                int frmSize = decode.PackH264Frame(decoder_handle, buffer, packetSize, bMark, (int) timestamp, sequence, frmbuf); //packer=拼帧器，frmbuf=帧缓存
                // Log.d("log", "序列号:" + sequence + " bMark:" + bMark + " pac ketSize:" + packetSize + " PayloadType:" + rtp_receive_packet.getPayloadType() + " timestamp:" + timestamp + " frmSize:" + frmSize);
                if (frmSize <= 0) {
                    continue;
                }
                receiveVideoCallback.receiveVideoStream(frmbuf, frmSize, timestamp);
            }

            //关闭
            if (decoder_handle != 0) {
                decode.DestroyH264Packer(decoder_handle);
                decoder_handle = 0;
            }
            if (rtp_socket != null) {
                rtp_socket.close();
                rtp_socket = null;
            }
            decode.DestoryDecoder();
        }
    }

    public void stopServer() {
        isRunning = false;
        if (rtp_socket != null) {
            rtp_socket.close();
            rtp_socket = null;
        }

    }

    public interface ReceiveVideoCallback {
        public void receiveVideoStream(byte[] frmbuf, int frmSize, long timestamp);

    }

}
