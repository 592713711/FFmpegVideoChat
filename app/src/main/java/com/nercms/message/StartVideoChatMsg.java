package com.nercms.message;

import com.nercms.message.Message;
import com.nercms.message.MessageTag;

/**
 * Created by zsg on 2016/6/5.
 */
public class StartVideoChatMsg extends Message{
    public int server_video_port;
    public int server_audio_port;
    public StartVideoChatMsg(){
        this.tag= MessageTag.START_VIDEO;
    }
}