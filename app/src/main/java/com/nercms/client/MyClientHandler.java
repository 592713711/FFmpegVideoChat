package com.nercms.client;

import android.util.Log;

import com.nercms.Config;
import com.nercms.MyApplication;
import com.nercms.util.HandlerUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class MyClientHandler extends SimpleChannelInboundHandler<String> {

    public MyClientHandler() {

    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String body = (String) msg;
        MsgHandle.getInstance().handleMsg(body);


    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.d(Config.TAG, "连接上服务器");
        super.channelActive(ctx);
        MsgHandle.getInstance().channel = ctx;
        // 发送自动登录消息
        //MyApplication.getInstance().getSendMsgUtil().sendLoginRequest(null,null);
        //发送给主线程 连接成功
        //EventBus.getDefault().postSticky(HandlerUtil.CONNECT_SUC);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Log.d(Config.TAG, "与服务器断开连接服务器");
        super.channelInactive(ctx);


        //重新连接服务器
        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            public void run() {
                MyClient.doConnect();
            }
        }, 2, TimeUnit.SECONDS);
        ctx.close();

        //发送个主线程 连接断开
        EventBus.getDefault().post(HandlerUtil.CONNECT_FAIL);

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {

            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state().equals(IdleState.READER_IDLE)) {


            } else if (event.state().equals(IdleState.WRITER_IDLE)) {

            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                // 未进行读写
                //System.out.println("ALL_IDLE");
                // 发送心跳消息  在客户端上实现心跳包的发送
                MyApplication.getInstance().getSendMsgUtil().sendHeartMessage();

            }

        }
    }
}