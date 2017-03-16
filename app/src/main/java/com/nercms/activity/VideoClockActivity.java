package com.nercms.activity;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nercms.Config;
import com.nercms.MyApplication;
import com.nercms.R;
import com.nercms.message.StartVideoChatMsg;
import com.nercms.message.response.VideoChatResponse;
import com.nercms.model.User;
import com.nercms.util.SystemBarTintManager;
import com.nercms.util.Util;
import com.nercms.util.WaitDialog;
import com.nercms.view.CircleImageView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

/**
 * 视频通话响铃界面
 */
public class VideoClockActivity extends AppCompatActivity {
    public User remoteUser;
    public int type;
    public static int SEND_TYPE = 1;
    public static int RECEIVE_TYPE = 2;
    public static int TIME_OUT = 8;
    public static int TIME_OUT_DURATION = 20000;      //20s后 关闭
    private WaitDialog waitDialog;
    private Dialog hintDialog;
    private Dialog refuseDialog;        //对方拒绝对话框

    private RelativeLayout bottom_layout;
    private RelativeLayout bottom_layout2;
    private RelativeLayout head_layout;
    private CircleImageView user_icon_image;
    private TextView user_name_text;

    private MediaPlayer music_play;    //铃声

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == TIME_OUT) {
                if (type == SEND_TYPE) {
                    //关闭音乐
                    //显示未接听对话框
                    hintDialog.show();
                } else {
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_clock);
        initWindow();
        EventBus.getDefault().register(this);
        remoteUser = (User) getIntent().getSerializableExtra("remoteUser");
        type = getIntent().getIntExtra("type", 0);
        initView();
        initPlay();
    }

    private void initPlay() {
        music_play = MediaPlayer.create(this, R.raw.waitmusic);
        music_play.setLooping(true);
        music_play.start();
    }


    private void initView() {
        waitDialog = new WaitDialog();
        waitDialog.setCancelable(false);

        handler.sendEmptyMessageDelayed(TIME_OUT, TIME_OUT_DURATION);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("对方未接听")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        hintDialog = builder.create();
        hintDialog.setCancelable(false);

        AlertDialog.Builder builder2 = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("对方拒绝了你的视频邀请")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        refuseDialog = builder2.create();
        refuseDialog.setCancelable(false);

        user_icon_image = (CircleImageView) findViewById(R.id.user_icon);
        user_name_text = (TextView) findViewById(R.id.user_name);
        if (remoteUser != null) {
            user_icon_image.setImageResource(Util.getHeadId(remoteUser.icon));
            user_name_text.setText(remoteUser.username);
        }

        bottom_layout = (RelativeLayout)

                findViewById(R.id.bottom_layout);

        bottom_layout2 = (RelativeLayout)

                findViewById(R.id.bottom_layout2);

        head_layout = (RelativeLayout)

                findViewById(R.id.head_layout);

        if (type == SEND_TYPE)

        {
            bottom_layout2.setVisibility(View.VISIBLE);
            bottom_layout.setVisibility(View.GONE);
        } else

        {
            bottom_layout2.setVisibility(View.GONE);
            bottom_layout.setVisibility(View.VISIBLE);
        }

        showAnim();


    }

    public void doSure(View v) {
        VideoChatResponse response = new VideoChatResponse();
        response.isReceive = true;
        response.from_id = MyApplication.getInstance().getSpUtil().getUser().id;
        response.into_id = remoteUser.id;

        MyApplication.getInstance().getSendMsgUtil().sendMessageToServer(
                MyApplication.getInstance().getGson().toJson(response)
        );

        waitDialog.show(getSupportFragmentManager(), "wait");
        handler.removeMessages(TIME_OUT);

        //关闭声音
        stopMusicPlay();
    }

    public void doCancle(View v) {
        VideoChatResponse response = new VideoChatResponse();
        response.isReceive = false;
        response.from_id = MyApplication.getInstance().getSpUtil().getUser().id;
        response.into_id = remoteUser.id;

        MyApplication.getInstance().getSendMsgUtil().sendMessageToServer(
                MyApplication.getInstance().getGson().toJson(response)
        );

        //关闭声音
        stopMusicPlay();
        finish();
    }

    /**
     * 显示出现动画
     */
    public void showAnim() {
        Animation anmi = AnimationUtils.loadAnimation(this, R.anim.head_coming);
        Animation anmi2 = AnimationUtils.loadAnimation(this, R.anim.bottom_coming);

        head_layout.startAnimation(anmi);
        bottom_layout.setAnimation(anmi2);
        bottom_layout2.setAnimation(anmi2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        handler.removeMessages(TIME_OUT);
    }

    public void stopMusicPlay() {
        music_play.stop();
        music_play.release();
    }

    /**
     * 登陆成功或失败事件总线回调方法
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateList(VideoChatResponse response) {
        User user = MyApplication.getInstance().getUserDao().getUserById(response.from_id);
        if (response.isReceive) {
            Log.d(Config.TAG, user.username + " 接受了视频邀请");
            waitDialog.show(getSupportFragmentManager(), "wait");
            handler.removeMessages(TIME_OUT);
            //关闭声音
            stopMusicPlay();
        } else {
            Log.d(Config.TAG, user.username + " 拒绝了视频邀请");
            handler.removeMessages(TIME_OUT);
            //关闭声音
            stopMusicPlay();

            if (type == SEND_TYPE) {
                //发送方就显示对话框
                refuseDialog.show();
            } else {
                //接收方就直接关闭
                finish();
            }


        }
    }

    /**
     * 开启视频聊天
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void startVideoChat(StartVideoChatMsg msg) {
        Log.d(Config.TAG, "开启视频通话");
        if (msg.into_id == remoteUser.id || msg.from_id == remoteUser.id) {
            Log.d(Config.TAG, "开启视频通话");
            //开启视频通话
            Intent intent = new Intent(this, VideoChatActivity.class);
            intent.putExtra("remote_ip", Config.serverIP);
            intent.putExtra("remote_video_port", msg.server_video_port);
            intent.putExtra("remote_audio_port", msg.server_audio_port);
            intent.putExtra("remote_user", remoteUser);
            intent.putExtra("type",type);
            startActivity(intent);
            waitDialog.dismiss();
            finish();
        }
    }

    /**
     * 初始化通知栏颜色
     */
    private void initWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
        }
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(R.color.statusbar_bg);//通知栏所需颜色

    }


    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

}
