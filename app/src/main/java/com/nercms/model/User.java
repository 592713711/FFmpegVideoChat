package com.nercms.model;

import java.io.Serializable;

/**
 * Created by zsg on 2016/6/3.
 */
public class User implements Serializable{
    public int id;		        //用户id
    public String username;		//用户名
    public int icon;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", icon=" + icon +
                '}';
    }
}
