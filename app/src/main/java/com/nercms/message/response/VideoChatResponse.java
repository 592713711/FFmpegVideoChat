package com.nercms.message.response;

import com.nercms.message.MessageTag;

/**
 * Created by zsg on 2016/6/5.
 */
public class VideoChatResponse extends Response{
    public boolean isReceive = false;

    public VideoChatResponse() {
        this.tag = MessageTag.VIDEOCHAT_RES;
    }
}
