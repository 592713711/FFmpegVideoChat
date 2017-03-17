package com.zsg.ffmpegvideolib;

/**
 * Created by zsg on 2017/3/16.
 */
public class Ffmpeg {
    public native int videoinit();      //初始化

    public native byte[] videoencode(byte[] yuvdata);       //放入实时视频流   传出

    public native int videoclose();

    public native byte[] videodecode(byte[] in);



    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("ffmpeg_coderc");
    }
}
