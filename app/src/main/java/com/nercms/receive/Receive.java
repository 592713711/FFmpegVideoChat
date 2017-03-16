package com.nercms.receive;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.nercms.R;

public class Receive extends Activity {
    static {
        System.loadLibrary("H264Decoder_neon");
    }

    private boolean isRunning; //程序运行标志
    private RtpSocket rtp_socket = null; //创建RTP套接字
    private RtpPacket rtp_packet = null; //创建RTP包
    private byte[] socketBuffer = new byte[2048]; //包缓存
    private byte[] buffer = new byte[2048];
    private long handle = 0; //拼帧器的句柄
    private byte[] frmbuf = new byte[65536]; //帧缓存

    private Videoplay view = null;
    private SurfaceView surfaceView;
    private Camera mCamera = null; //创建摄像头处理类
    private SurfaceHolder holder = null; //创建界面句柄，显示视频的窗口句柄

    public native long CreateH264Packer();

    public native int PackH264Frame(long handle, byte[] pPayload, int payloadlen, int bMark, int pts, int sequence, byte[] frmbuf);

    public native void DestroyH264Packer(long handle);

    public native int CreateDecoder(int width, int height);

    public native int DecoderNal(byte[] in, int insize, byte[] out);

    public native int DestoryDecoder();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);
        initView();
    }

    private void initView() {
        view = (Videoplay) this.findViewById(R.id.video_play);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doStart();
            }
        }, 1000);

    }

    /**
     * 启动一个接受rtp数据包的线程
     */
    public void doStart() {
//接收包
        if (rtp_socket == null) {
            try {
                rtp_socket = new RtpSocket(new SipdroidSocket(20000)); //初始化套接字，20000为接收端口号
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            rtp_packet = new RtpPacket(socketBuffer, 0); //初始化 ,socketBuffer改变时rtp_Packet也跟着改变
            /**
             * 因为可能传输数据过大 会将一次数据分割成好几段来传输
             * 接受方 根据序列号和结束符 来将这些数据拼接成完整数据
             */
            handle = CreateH264Packer(); //创建拼帧器
            CreateDecoder(352, 288); //创建解码器
            isRunning = true;
            Decoder decoder = new Decoder();
            decoder.start(); //启动一个线程
        }

        if (mCamera == null) {

            //摄像头设置，预览视频
            mCamera = Camera.open(1); //实例化摄像头类对象
            Camera.Parameters p = mCamera.getParameters(); //将摄像头参数传入p中
            p.setFlashMode("off");
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

    //建立回调的类
    class Callback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] frame, Camera camera) {
        }
    }

    public void close() {
        isRunning = false;
        //释放摄像头资源
        if (mCamera != null) {
            mCamera.setPreviewCallback(null); //停止回调函数
            mCamera.stopPreview(); //停止预览
            mCamera.release(); //释放资源
            mCamera = null; //重新初始化
        }

        if (rtp_socket != null) {
            rtp_socket.close();
            rtp_socket = null;
        }

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

    class Decoder extends Thread {
        public void run() {
            while (isRunning) {
                try {
                    rtp_socket.receive(rtp_packet); //接收一个包
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int packetSize = rtp_packet.getPayloadLength(); //获取包的大小

                if (packetSize <= 0)
                    continue;
                if (rtp_packet.getPayloadType() != 2) //确认负载类型为2
                    continue;
                System.arraycopy(socketBuffer, 12, buffer, 0, packetSize); //socketBuffer->buffer
                int sequence = rtp_packet.getSequenceNumber(); //获取序列号
                long timestamp = rtp_packet.getTimestamp(); //获取时间戳
                int bMark = rtp_packet.hasMarker() == true ? 1 : 0; //是否是最后一个包
                int frmSize = PackH264Frame(handle, buffer, packetSize, bMark, (int) timestamp, sequence, frmbuf); //packer=拼帧器，frmbuf=帧缓存
                Log.d("log", "序列号:" + sequence + " bMark:" + bMark + " packetSize:" + packetSize + " PayloadType:" + rtp_packet.getPayloadType() + " timestamp:" + timestamp + " frmSize:" + frmSize);
                if (frmSize <= 0)
                    continue;

                DecoderNal(frmbuf, frmSize, view.mPixel);//解码后的图像存在mPixel中

                //Log.d("log","序列号:"+sequence+" 包大小："+packetSize+" 时间："+timestamp+"  frmbuf[30]:"+frmbuf[30]);
                view.postInvalidate();
            }

            //关闭
            if (handle != 0) {
                DestroyH264Packer(handle);
                handle = 0;
            }
            if (rtp_socket != null) {
                rtp_socket.close();
                rtp_socket = null;
            }
            DestoryDecoder();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }
}
