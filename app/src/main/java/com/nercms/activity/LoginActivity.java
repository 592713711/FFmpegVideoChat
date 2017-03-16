package com.nercms.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.nercms.Config;
import com.nercms.MyApplication;
import com.nercms.R;
import com.nercms.message.request.LoginRequest;
import com.nercms.message.response.LoginResponse;
import com.nercms.model.User;
import com.nercms.util.HandlerUtil;
import com.nercms.util.SelectHeadDialog;
import com.nercms.util.SystemBarTintManager;
import com.nercms.util.Util;
import com.nercms.view.CircleImageView;
import com.nercms.view.morphingbutton.MorphingButton;
import com.nercms.view.morphingbutton.impl.IndeterminateProgressButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 登录界面
 * 活动
 */
public class LoginActivity extends AppCompatActivity {
    //自定义按钮
    private IndeterminateProgressButton btnMorph;
    //自定义选择对话框
    private SelectHeadDialog selectHeadDialog;
    private EditText usernameText;
    private CircleImageView head_btn;
    private int head_id =0;      //头像id
    private Gson gson;

    private int btn_width;
    private RelativeLayout rl_user;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what== HandlerUtil.REQUEST_ERROR){
                //登陆失败
                Toast.makeText(LoginActivity.this, "登陆失败，请检查网络", Toast.LENGTH_SHORT).show();
                btnMorph.unblockTouch();
                morphToSquare(btnMorph, integer(R.integer.mb_animation), "重新登陆", color(R.color.theme));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);
        gson = new Gson();
        EventBus.getDefault().register(this);
        initWindow();
        initView();
    }

    private void initView() {
        usernameText = (EditText) findViewById(R.id.user_text);
        head_btn = (CircleImageView) findViewById(R.id.headicon_btn);
        rl_user = (RelativeLayout) findViewById(R.id.rl_user);

        User user=MyApplication.getInstance().getSpUtil().getUser();
        if(user!=null) {
            usernameText.setText(user.username);
            head_id=user.icon;
            head_btn.setImageResource(Util.getHeadId(head_id));
        }

        selectHeadDialog = new SelectHeadDialog();

        btnMorph = (IndeterminateProgressButton) findViewById(R.id.btnMorph);
        btnMorph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMorphButtonClicked(btnMorph);
            }
        });
        btn_width=btnMorph.getMeasuredWidth();

        morphToSquare(btnMorph, 0, "登陆", color(R.color.theme));

        Animation anim = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.login_anim);
        anim.setFillAfter(true);
        rl_user.startAnimation(anim);

    }


    /**
     * 点击头像按钮 进行头像选择
     *
     * @param v
     */
    public void doSetHead(View v) {
        SelectHeadDialog.OnClickReturn onreturn = new SelectHeadDialog.OnClickReturn() {

            @Override
            public void onClick(int icon,int icon_id) {
                Log.d(Config.TAG, "id:" + icon);
                if (icon_id != -1) {
                    head_id = icon;
                    head_btn.setImageResource(icon_id);
                }
                selectHeadDialog.dismiss();
            }
        };
        selectHeadDialog.setOnClickReturn(onreturn);
        selectHeadDialog.show(getSupportFragmentManager(), "SELECT_DIALOG");


    }


    private void onMorphButtonClicked(final IndeterminateProgressButton btnMorph) {
        String name = usernameText.getText().toString();
        if (name == null || name.trim().isEmpty()) {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        //登陆
        requestLogin(name, head_id);
        simulateProgress(btnMorph, 6000);


    }

    private void requestLogin(String name, int head_id) {
        User user = new User();
        user.username = name;
        user.icon = head_id;
        LoginRequest request = new LoginRequest();
        request.user = user;
        MyApplication.getInstance().getSendMsgUtil().sendMessageToServer(gson.toJson(request));

    }


    /**
     * 变成按钮状态
     *
     * @param btnMorph
     * @param duration
     */
    private void morphToSquare(final IndeterminateProgressButton btnMorph, int duration, String msg, int bg_color) {
        Log.e("xxx","width:"+btn_width);
        MorphingButton.Params square = MorphingButton.Params.create()
                .duration(duration)
                .cornerRadius(dimen(R.dimen.mb_height_8)) // 56 dp
                .width(btn_width)
                .height(dimen(R.dimen.mb_height_56))
                .color(bg_color)
                .colorPressed(color(R.color.mb_blue))
                .text(msg);
        btnMorph.morph(square);
    }


    /**
     * 变成进度条
     *
     * @param button
     */
    private void simulateProgress(@NonNull final IndeterminateProgressButton button, int duration) {
        int progressColor = color(R.color.mb_blue);
        int color = color(R.color.mb_gray);
        int progressCornerRadius = dimen(R.dimen.mb_corner_radius_4);
        btn_width =button.getWidth();
        int height = dimen(R.dimen.mb_height_8);
        handler.sendEmptyMessageDelayed(HandlerUtil.REQUEST_ERROR,duration);


        button.blockTouch(); // prevent user from clicking while button is in progress
        button.morphToProgress(color, progressCornerRadius, btn_width, height, 500, progressColor);
    }


    /**
     * 登陆成功或失败事件总线回调方法
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getLoginResponse(LoginResponse response) {
        Log.d(Config.TAG, "收到登陆响应" + response.toString());
        if (response.isSuccess) {
            handler.removeMessages(HandlerUtil.REQUEST_ERROR);
            //登陆成功
            //跳转
            Intent intent=new Intent(this,UserActivity.class);
            startActivity(intent);
            finish();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public int integer(@IntegerRes int resId) {
        return getResources().getInteger(resId);
    }

    public int color(@ColorRes int resId) {
        return getResources().getColor(resId);
    }

    public int dimen(@DimenRes int resId) {
        return (int) getResources().getDimension(resId);
    }


    public void doQuit(View v) {
        finish();
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
