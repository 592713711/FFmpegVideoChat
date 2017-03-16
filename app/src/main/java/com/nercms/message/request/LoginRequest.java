package com.nercms.message.request;

import com.nercms.message.MessageTag;
import com.nercms.model.User;

/**
 * Created by zsg on 2016/6/4.
 */
public class LoginRequest extends Request{
    public User user;
    public LoginRequest(){
        this.tag= MessageTag.LOGIN_REQ;
    }

}
