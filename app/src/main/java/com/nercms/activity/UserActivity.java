package com.nercms.activity;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nercms.Config;
import com.nercms.MyApplication;
import com.nercms.R;
import com.nercms.adapter.RecycleOnClickListener;
import com.nercms.adapter.UserRecycleViewAdapter;
import com.nercms.client.MsgHandle;
import com.nercms.message.MessageTag;
import com.nercms.message.request.Request;
import com.nercms.model.User;
import com.nercms.util.HandlerUtil;
import com.nercms.util.SystemBarTintManager;
import com.nercms.util.Util;
import com.nercms.view.DragLayout;
import com.nercms.view.wave.WaterSceneView;
import com.nineoldandroids.view.ViewHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserActivity extends AppCompatActivity implements RecycleOnClickListener {
    private ArrayList<User> userList;
    private RecyclerView recyclerView;
    private UserRecycleViewAdapter adapter;
    private DragLayout dl;
    private TextView title_text;
    private WaterSceneView waterScene;

    private ImageView hintImage;
    private TextView hintText;
    private CoordinatorLayout coordinatorLayout;
    private Dialog unConnect_dialog;        //断线提示对话框

    private ImageView user_icon;
    private TextView user_name;

    private Handler handler=new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        EventBus.getDefault().register(this);
        MsgHandle.getInstance().setActivity(this);
        initWindow();
        initView();
    }

    private void initView() {
        hintImage = (ImageView) findViewById(R.id.hint);
        hintText = (TextView) findViewById(R.id.hint_text);
        user_icon= (ImageView) findViewById(R.id.user_icon);
        user_icon.setImageResource(Util.getHeadId(MyApplication.getInstance().getSpUtil().getUser().icon));
        user_name= (TextView) findViewById(R.id.user_name);
        user_name.setText(MyApplication.getInstance().getSpUtil().getUser().username);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        waterScene = (WaterSceneView) findViewById(R.id.water_scene);
        title_text = (TextView) findViewById(R.id.tv_title);

        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        adapter=new UserRecycleViewAdapter(this,this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        updateData();


        initDialog();
        initDragLayout();
    }

    private void initDragLayout() {
        dl = (DragLayout) findViewById(R.id.dl);
        dl.setDragListener(new DragLayout.DragListener() {
            @Override
            public void onOpen() {
                //lv.smoothScrollToPosition(new Random().nextInt(30));
            }

            @Override
            public void onClose() {
                title_text.startAnimation(AnimationUtils.loadAnimation(UserActivity.this, R.anim.shake));
            }

            @Override
            public void onDrag(float percent) {
                ViewHelper.setAlpha(title_text, 1 - percent);
            }
        });
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("断开连接")
                .setMessage("与服务器断开，请重新登陆")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(UserActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
        unConnect_dialog = builder.create();
        unConnect_dialog.setCancelable(false);
    }

    public void updateData() {
        //新建List
        userList = MyApplication.getInstance().getUserDao().getuserList();
        Log.e(Config.TAG, userList.toString());
        if(userList.size()>0){
            changeHint(View.GONE);
        }else{
            changeHint(View.VISIBLE);
        }

        adapter.updateData(userList);

    }

    public void changeHint(int t) {
        hintText.setVisibility(t);
        hintImage.setVisibility(t);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    /**
     * 登陆成功或失败事件总线回调方法
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateList(Integer tag) {
        if (tag == HandlerUtil.UPDATE_LIST) {
            Log.e(Config.TAG, "更新列表");
            updateData();

        } else if (tag == HandlerUtil.CONNECT_FAIL) {
            //断线
            //退出当前活动 打开登陆活动
            unConnect_dialog.dismiss();
            ;
            unConnect_dialog.show();
        }

    }

    public void doQuit(View v) {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                waterScene.setWaveHeight(50 / 4f * getResources().getDisplayMetrics().density);
                waterScene.setNoiseIntensity((float) 18 / 100f);
            }
        }, 500);
    }

    @Override
    public void onClickItem(int pos) {
        final User user = userList.get(pos);
        Log.e(Config.TAG, "点击：" + user.toString() + "   " + MyApplication.getInstance().getSpUtil().getUser().toString());
        /**
         * 弹出snackbar
         */
        final Snackbar snackbar = Snackbar.make(coordinatorLayout, "开启与 " + user.username + " 的视频通话？", Snackbar.LENGTH_LONG);

        //设置snackbar自带的按钮监听器
        snackbar.setAction("确定", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //发送视频通话请求
                Request request = new Request();
                request.tag = MessageTag.VIDEOCHAT_REQ;
                request.from_id = MyApplication.getInstance().getSpUtil().getUser().id;
                request.into_id = user.id;
                MyApplication.getInstance().getSendMsgUtil().sendMessageToServer(
                        MyApplication.getInstance().getGson().toJson(request)
                );

                Intent intent = new Intent(MyApplication.getInstance(), VideoClockActivity.class);
                intent.putExtra("remoteUser", user);
                intent.putExtra("type", VideoClockActivity.SEND_TYPE);
                startActivity(intent);
            }
        });

        snackbar.show();
    }
}
