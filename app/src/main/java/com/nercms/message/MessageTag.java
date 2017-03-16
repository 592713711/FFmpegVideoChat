package com.nercms.message;

/**
 * Created by zsg on 2016/6/3.
 */
public class MessageTag {
    //心跳包标识
    public static final int HEART_MSG = 0;

    //登陆请求和响应标识
    public static final int LOGIN_REQ = 1;
    public static final int LOGIN_RES = 2;

    //用户 上线离线
    public static final int ADDUSER_REQ=3;
    public static final int REMOVEUSER_REQ=4;

    //视频通话请求响应
    public static final int VIDEOCHAT_REQ=5;
    public static final int VIDEOCHAT_RES=6;

    //不在线
    public static final int NOT_ONLINE=7;

    public static final int START_VIDEO=8;
    public static final int STOP_VIDEO=9;

    //更改摄像头模式  前置后置
    public static final int CHANGE_CAMERA=10;

    //更改滤镜
    public static final int CHANGE_FILTER=11;

}
