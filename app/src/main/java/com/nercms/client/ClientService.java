package com.nercms.client;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.nercms.Config;


/**
 * 用来启动客户端的服务
 */
public class ClientService extends Service implements Config {
    private PowerManager.WakeLock wakeLock=null;
    public ClientService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        //开启线程（服务是工作在主线程上） 用来启动客户端与服务器的连接
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MyClient.initClient();
                MyClient.doConnect();
            }
        });
        thread.start();

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"com.task.TalkMessageService");
        wakeLock.acquire();

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
