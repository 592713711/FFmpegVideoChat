package com.nercms.receive;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.nercms.activity.VideoChatActivity;

/**
 * Created by zsg on 2016/11/26.
 */
public class WrapperLayout extends RelativeLayout {
    private WindowManager mWindowManager;
    float mRawX, mRawY, mStartX, mStartY;
    private Context context;

    public WrapperLayout(Context context) {
        super(context, null);
    }

    public WrapperLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
    }

    public void setWindowManager(WindowManager manager) {
        this.mWindowManager = manager;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 消耗触摸事件
        return detector.onTouchEvent(event);
    }

    /**
     * 更新窗口参数，控制浮动窗口移动
     */
    private void updateWindowPosition() {

        // 更新坐标
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();

        wmParams.x = (int) (mRawX - mStartX);
        wmParams.y = (int) (mRawY - mStartY);
        Log.e("xxxx", "更改：" + wmParams.x + "  " + wmParams.y);
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        if (mWindowManager != null)
            // 使参数生效
            mWindowManager.updateViewLayout(this, wmParams);

    }


    private GestureDetector detector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent event) {
            // 以当前父视图左上角为原点
            mStartX = event.getX();
            mStartY = event.getY();
            //  Log.e("xxxx","mStartX："+mStartX+"  mStartY:"+mStartY);
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mRawX = e2.getRawX();
            mRawY = e2.getRawY();
          //  Log.d("xxxx", mRawX + "  " + mRawY);
            updateWindowPosition();
            return true;
        }


        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.e("xxxx", "双击");
            Intent intent = new Intent(context, VideoChatActivity.class);
            context.startActivity(intent);
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return super.onDoubleTapEvent(e);
        }
    });

}
