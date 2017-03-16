package com.nercms.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.nercms.MyApplication;
import com.nercms.model.User;

import java.util.ArrayList;

/**
 * 存储在线用户表
 * Created by zsg on 2016/6/4.
 */
public class UserDao {
    private static final String TABLE_NAME = "userdao";      //表名
    public static final String COL_USER_ID = "user_id";    //对象id
    public static final String COL_USERNAME = "username";    //联系人名
    public static final String COL_USRRICON = "usericon";    //将x系统储存的号码解析的联系人号码


    public static final String SQL_CREATE_TABLE = String.format(
            "CREATE table IF NOT EXISTS %s(%s integer,%s text,%s integer)",
            TABLE_NAME,
            COL_USER_ID,
            COL_USERNAME,
            COL_USRRICON
    );

    //删除表语句
    public static final String SQL_DROP_TABLE = String.format(
            "drop table if exists %s",
            TABLE_NAME
    );

    public static final String[] ALLCOL = {COL_USER_ID, COL_USERNAME, COL_USRRICON};

    private SQLiteDatabase db;
    private DBHelper helper;

    public UserDao(Context context) {
        helper = new DBHelper(context);
        db = helper.getWritableDatabase();
    }

    /**
     * 插入用户列表 登录时
     *
     * @param list
     */
    public void insertUser(ArrayList<User> list) {
        db.delete(TABLE_NAME, null, null);
        for (User user : list) {
            ContentValues values = new ContentValues();
            values.put(COL_USER_ID, user.id);
            values.put(COL_USERNAME, user.username);
            values.put(COL_USRRICON, user.icon);
            db.insert(TABLE_NAME, null, values);
        }
    }

    public void insertUser(User user) {
        //先判断表中是否存在
        Cursor cursor = db.query(TABLE_NAME, ALLCOL, COL_USER_ID + "=" + user.id, null, null, null, null);
        if (cursor.moveToNext()) {
            //存在就修改
            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, user.username);
            values.put(COL_USRRICON, user.icon);
            db.update(TABLE_NAME, values, COL_USER_ID + "=" + user.id, null);
        } else {
            //不存在就添加
            ContentValues values = new ContentValues();
            values.put(COL_USER_ID, user.id);
            values.put(COL_USERNAME, user.username);
            values.put(COL_USRRICON, user.icon);
            db.insert(TABLE_NAME, null, values);
        }
    }

    public ArrayList<User> getuserList() {
        int mainid= MyApplication.getInstance().getSpUtil().getUser().id;
        ArrayList<User> list = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME, ALLCOL, COL_USER_ID+"!="+mainid, null, null, null, null);
        while (cursor.moveToNext()) {
            User user = new User();
            user.id = cursor.getInt(0);
            user.username = cursor.getString(1);
            user.icon = cursor.getInt(2);
            list.add(user);
        }
        return list;
    }

    public void removeUser(User user) {
        db.delete(TABLE_NAME, COL_USER_ID + "=" + user.id, null);
    }

    public User getUserById(int from_id) {
        User user=null;
        Cursor cursor=db.query(TABLE_NAME,ALLCOL,COL_USER_ID+"="+from_id,null,null,null,null);
        if(cursor.moveToNext()){
            user = new User();
            user.id = cursor.getInt(0);
            user.username = cursor.getString(1);
            user.icon = cursor.getInt(2);
        }

        return user;


    }
}
