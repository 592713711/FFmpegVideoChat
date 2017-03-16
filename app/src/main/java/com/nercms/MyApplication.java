package com.nercms;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.nercms.client.ClientService;
import com.nercms.dao.UserDao;
import com.nercms.util.SendMessageUtil;
import com.nercms.util.SharePreferenceUtil;

/**
 * Created by zsg on 2016/6/3.
 */
public class MyApplication extends Application {
    private static MyApplication mApplication;
    private SendMessageUtil sendMsgUtil;
    private UserDao userDao;
    private SharePreferenceUtil mSpUtil;
    private Gson gson;

    public static final String SP_FILE_NAME = "user_sp";

    public synchronized static MyApplication getInstance() {
        return mApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //比活动创建时启动服务要先执行
        Log.d("mylog", "执行了");
        startService(new Intent(this, ClientService.class));
        mApplication = this;
    }

    public synchronized SendMessageUtil getSendMsgUtil() {
        if (sendMsgUtil == null)
            sendMsgUtil = new SendMessageUtil();
        return sendMsgUtil;
    }

    public synchronized UserDao getUserDao() {
        if (userDao == null)
            userDao = new UserDao(this);
        return userDao;
    }

    public synchronized SharePreferenceUtil getSpUtil() {

        if (mSpUtil == null)
            mSpUtil = new SharePreferenceUtil(this, SP_FILE_NAME);
        return mSpUtil;
    }

    public synchronized Gson getGson() {

        if (gson == null)
            gson = new Gson();
        return gson;
    }
}
