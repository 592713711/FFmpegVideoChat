package com.nercms.send;

/*import java.io.File;*/

import java.io.IOException;
/*import java.io.RandomAccessFile;*/
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.nercms.R;
import com.nercms.receive.Videoplay;

public class Send extends Activity {
    //包含库文件
    static {
        System.loadLibrary("VideoEncoder");
    }

    //接口函数
    public native long CreateEncoder(int width, int height); //底层创建编码器，返回编码器

    //编码一帧图像，返回包的数目
    //type=编码帧的类型，frame=原始yuv图像，stream=原始图像码流，packetSize=包的尺寸
    public native int EncoderOneFrame(long encoder, int type, byte[] frame, byte[] stream, int[] packetSize);

    public native int DestroyEncoder(long encoder); //销毁编码器，释放资源

    private long encoder = -1; //创建编码器的句柄
    private int[] packetSize = new int[200]; //包的尺寸
    private byte[] stream = new byte[65536]; //码流
    private int packetNum = 0; //包的数目

    private Camera mCamera = null; //创建摄像头处理类
    private SurfaceHolder holder = null; //创建界面句柄，显示视频的窗口句柄

    private RtpSocket rtp_socket = null; //创建RTP套接字
    private RtpPacket rtp_packet = null; //创建RTP包
    private byte[] socketBuffer = new byte[65536]; //缓存 stream->socketBuffer->rtp_socket

    Videoplay view = null;

    /*private RandomAccessFile raf=null;*/
    int temp = 0;

    //建立回调的类
    class Callback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] frame, Camera camera) {

            if (encoder != -1) {
                //底层函数，返回包的数目，返回包的大小存储在数组packetSize中，返回码流在stream中
                packetNum = EncoderOneFrame(encoder, -1, frame, stream, packetSize);
                Log.d("log", "原始数据大小：" + frame.length + "  转码后数据大小：" + stream.length);
                if (packetNum > 0) {

                    //通过RTP协议发送帧
                    final int[] pos = {0}; //从码流头部开始取
                    final long timestamp = System.currentTimeMillis(); //设定时间戳
            /**
             * 因为可能传输数据过大 会将一次数据分割成好几段来传输
             * 接受方 根据序列号和结束符 来将这些数据拼接成完整数据
            */
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int sequence = 0; //初始化序列号
                            for (int i = 0; i < packetNum; i++) {

                                rtp_packet.setPayloadType(2);//定义负载类型，视频为2
                                rtp_packet.setMarker(i == packetNum - 1 ? true : false); //是否是最后一个RTP包
                                rtp_packet.setSequenceNumber(sequence++); //序列号依次加1
                                rtp_packet.setTimestamp(timestamp); //时间戳
                                //Log.d("log", "序列号:" + sequence + " 时间：" + timestamp);
                                rtp_packet.setPayloadLength(packetSize[i]); //包的长度，packetSize[i]+头文件
                                //从码流stream的pos处开始复制，从socketBuffer的第12个字节开始粘贴，packetSize为粘贴的长度
                                System.arraycopy(stream, pos[0], socketBuffer, 12, packetSize[i]); //把一个包存在socketBuffer中
                                pos[0] += packetSize[i]; //重定义下次开始复制的位置
                                //rtp_packet.setPayload(socketBuffer, rtp_packet.getLength());
                                //  Log.d("log", "序列号:" + sequence + " bMark:" + rtp_packet.hasMarker() + " packetSize:" + packetSize[i] + " tPayloadType:2" + " timestamp:" + timestamp);
                                try {
                                    rtp_socket.send(rtp_packet);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }).start();


                }
            }
        }
    }



    public void doStart(View v) {
        if (encoder == -1)
            encoder = CreateEncoder(352, 288); //调用底层函数，创建编码器
        //encoder = CreateEncoder(800, 600);
        if (rtp_socket == null) {
            try {
                //RTP在1025到65535之间选择一个未使用的偶数端口号，而在同一次会话中的RTCP则使用下一个奇数
                //20000为接收端口号，1234发送端口号？
                //设定套接字
                rtp_socket = new RtpSocket(new SipdroidSocket(1234), InetAddress.getByName("192.168.191.2"), 20000);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            rtp_packet = new RtpPacket(socketBuffer, 0); //初始化 ,socketBuffer改变时rtp_Packet也跟着改变
        }
        if (mCamera == null) {

            //摄像头设置，预览视频
            mCamera = Camera.open(1); //实例化摄像头类对象
            Camera.Parameters p = mCamera.getParameters(); //将摄像头参数传入p中
            p.setFlashMode("off"); // �������
            p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //p.setPreviewFormat(PixelFormat.YCbCr_420_SP); //设置预览视频的格式
            p.setPreviewFormat(ImageFormat.NV21);
            p.setPreviewSize(352, 288); //设置预览视频的尺寸，CIF格式352×288
            //p.setPreviewSize(800, 600);
            p.setPreviewFrameRate(15); //设置预览的帧率，15帧/秒
            mCamera.setParameters(p); //设置参数
            byte[] rawBuf = new byte[1400];
            mCamera.addCallbackBuffer(rawBuf);
            mCamera.setDisplayOrientation(90); //视频旋转90度
            try {
                mCamera.setPreviewDisplay(holder); //预览的视频显示到指定窗口
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview(); //开始预览

            //获取帧
            //预览的回调函数在开始预览的时候以中断方式被调用，每秒调用15次，回调函数在预览的同时调出正在播放的帧
            Callback a = new Callback();
            mCamera.setPreviewCallback(a);
        }

    }

    public void doStop(View v) {
        close();
        finish(); //退回初始界面
    }


    public void close() {
        //释放摄像头资源
        if (mCamera != null) {
            mCamera.stopPreview(); //停止预览
            mCamera.setPreviewCallback(null); //停止回调函数
            mCamera.release(); //释放资源
            mCamera = null; //重新初始化
        }

        //释放编码器资源
        if (encoder != -1) {
            DestroyEncoder(encoder); //底层函数，销毁编码器
            encoder = -1; //重新初始化
        }

        //释放rtp资源
        if (rtp_socket != null) {
            rtp_socket.close();
            rtp_socket = null; //重新初始化
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        SurfaceView mSurfaceView = (SurfaceView) this.findViewById(R.id.camera_preview);
        holder = mSurfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
       // view = (Videoplay) this.findViewById(R.id.video_play);

    }

    @Override
    public void finalize() //在退出界面的时候自动调用
    {
        try {
            super.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        close();
    }


}