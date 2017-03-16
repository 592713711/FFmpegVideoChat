package com.nercms.util;

import android.util.Log;

import com.google.gson.Gson;
import com.nercms.Config;
import com.nercms.client.MsgHandle;
import com.nercms.message.HeartMessage;

import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 用来发送消息给服务器
 * Created by zsg on 2016/2/16.
 */
public class SendMessageUtil {
    private Gson gson;
    String heartMessage;

    public SendMessageUtil() {
        gson = new Gson();
        heartMessage = gson.toJson(new HeartMessage())+"#";

    }


    /**
     * 发送心跳消息
     */
    public void sendHeartMessage() {
        ByteBuf msgbuf = Unpooled.copiedBuffer(heartMessage.getBytes());
        MsgHandle.getInstance().channel.writeAndFlush(msgbuf);
    }

    /**
     * 发送实时消息到服务器
     */
    public void sendMessageToServer(String msg){
        Log.e(Config.TAG, "发送:"+msg);
        msg+="#";
        ByteBuf msgbuf = Unpooled.copiedBuffer(msg.getBytes());
        if (MsgHandle.getInstance().channel != null) {
            MsgHandle.getInstance().channel.writeAndFlush(msgbuf);
        }
    }

}
