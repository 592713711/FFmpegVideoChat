package com.nercms.client;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.nercms.Config;
import com.nercms.MyApplication;
import com.nercms.activity.VideoClockActivity;
import com.nercms.message.Message;
import com.nercms.message.MessageTag;
import com.nercms.message.request.AlterUserResquest;
import com.nercms.message.request.ChangeFilterRequest;
import com.nercms.message.request.Request;
import com.nercms.message.StartVideoChatMsg;
import com.nercms.message.response.LoginResponse;
import com.nercms.message.response.Response;
import com.nercms.message.response.VideoChatResponse;
import com.nercms.model.User;
import com.nercms.util.HandlerUtil;

import org.greenrobot.eventbus.EventBus;

import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zsg on 2016/6/3.
 */
public class MsgHandle {
    private Gson gson;
    public static MsgHandle msgHandle;
    public ChannelHandlerContext channel;     //与服务器连接的通道
    private Activity activity;

    private MsgHandle() {
        gson = new Gson();

    }

    public static MsgHandle getInstance() {
        if (msgHandle == null) {
            msgHandle = new MsgHandle();
        }
        return msgHandle;
    }

    /**
     * 处理服务器发来的请求
     */
    public void handleMsg(String msgJson) {
        Message message = gson.fromJson(msgJson, Message.class);
        Log.e(Config.TAG, msgJson);
        switch (message.tag) {
            case MessageTag.LOGIN_RES:
                //登陆响应
                handelLogin(msgJson);
                break;
            case MessageTag.ADDUSER_REQ:
                //登陆响应
                handelAddUser(msgJson);
                break;
            case MessageTag.REMOVEUSER_REQ:
                //登陆响应
                handelRemoveUser(msgJson);
                break;
            case MessageTag.NOT_ONLINE:
                //对方不在线
                handleNotOnline(msgJson);
                break;
            case MessageTag.VIDEOCHAT_REQ:
                handleVideoChatRequest(msgJson);
                break;
            case MessageTag.VIDEOCHAT_RES:
                handleVideoChatResponse(msgJson);
                break;
            case MessageTag.START_VIDEO:
                handleStartVideoChat(msgJson);
                break;
            case MessageTag.STOP_VIDEO:
                handleStopVideoChat(msgJson);
                break;
            case MessageTag.CHANGE_CAMERA:
                handleChangeCamera(msgJson);
                break;
            case MessageTag.CHANGE_FILTER:
                handeleChangeFilter(msgJson);
                break;

        }
    }

    private void handeleChangeFilter(String msgJson) {
        ChangeFilterRequest request=gson.fromJson(msgJson,ChangeFilterRequest.class);
        EventBus.getDefault().post(request);
    }

    private void handleChangeCamera(String msgJson) {
        Request request=gson.fromJson(msgJson,Request.class);
        EventBus.getDefault().post(request);
    }

    private void handleStopVideoChat(String msgJson) {
        Request request=gson.fromJson(msgJson, Request.class);
        EventBus.getDefault().post(request);
    }

    private void handleVideoChatResponse(String msgJson) {
        VideoChatResponse response = gson.fromJson(msgJson, VideoChatResponse.class);
        EventBus.getDefault().post(response);
    }

    private void handleStartVideoChat(String msgJson) {
        StartVideoChatMsg response = gson.fromJson(msgJson, StartVideoChatMsg.class);
        EventBus.getDefault().post(response);
    }

    /**
     * 处理视频通话请求
     */
    private void handleVideoChatRequest(String msgJson) {
        //开启界面
        Request request = gson.fromJson(msgJson, Request.class);
        User user = MyApplication.getInstance().getUserDao().getUserById(request.from_id);
        Intent intent = new Intent(MyApplication.getInstance(), VideoClockActivity.class);
        intent.putExtra("remoteUser", user);
        intent.putExtra("type", VideoClockActivity.RECEIVE_TYPE);
        if (activity != null)
            activity.startActivity(intent);
        Log.d(Config.TAG, "收到 " + user.username + " 视频请求");
    }

    private void handleNotOnline(String msgJson) {
        Response response = gson.fromJson(msgJson, Response.class);
        EventBus.getDefault().post(response);
    }

    private void handelRemoveUser(String msgJson) {
        AlterUserResquest resquest = gson.fromJson(msgJson, AlterUserResquest.class);
        MyApplication.getInstance().getUserDao().removeUser(resquest.user);
        EventBus.getDefault().post(HandlerUtil.UPDATE_LIST);
    }

    private void handelAddUser(String msgJson) {
        AlterUserResquest resquest = gson.fromJson(msgJson, AlterUserResquest.class);
        MyApplication.getInstance().getUserDao().insertUser(resquest.user);
        EventBus.getDefault().post(HandlerUtil.UPDATE_LIST);
    }

    private void handelLogin(String msgJson) {
        LoginResponse response = gson.fromJson(msgJson, LoginResponse.class);
        if (response.isSuccess) {
            MyApplication.getInstance().getSpUtil().writeUser(response.user);
            MyApplication.getInstance().getUserDao().insertUser(response.list);
        }
        EventBus.getDefault().post(response);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

}
