package com.nercms.message.response;

import com.nercms.message.MessageTag;
import com.nercms.model.User;

import java.util.ArrayList;

/**
 * Created by zsg on 2016/6/4.
 */
public class LoginResponse extends Response {
    public boolean isSuccess = false;
    public User user;        //自己的用户信息
    public ArrayList<User> list;        //在线用户信息

    public LoginResponse() {
        this.tag = MessageTag.LOGIN_RES;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "isSuccess=" + isSuccess +
                ", user=" + user +
                ", list=" + list +
                '}';
    }
}
