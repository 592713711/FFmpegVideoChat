package com.nercms.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nercms.Config;
import com.nercms.MyApplication;
import com.nercms.R;
import com.nercms.adapter.FilterAdapter;
import com.nercms.audio.AudioServer;
import com.nercms.filter.FilterTypeHelper;
import com.nercms.message.MessageTag;
import com.nercms.message.request.ChangeFilterRequest;
import com.nercms.message.request.Request;
import com.nercms.filter.MagicFilterType;
import com.nercms.model.User;
import com.nercms.model.VideoData;
import com.nercms.receive.SelfVideoplay;
import com.nercms.receive.Videoplay;
import com.nercms.receive.WrapperLayout;
import com.nercms.send.FfmpegServer;
import com.nercms.util.MediaScanner;
import com.nercms.util.SystemBarTintManager;
import com.nercms.util.TimeUtil;
import com.nercms.util.Util;
import com.nercms.video.VideoServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 */
public class VideoChatActivity extends AppCompatActivity implements VideoServer.ReceiveVideoCallback, View.OnClickListener, FilterAdapter.onFilterChangeListener {
    private static final String TAG = "VideoChatActivity";
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 10;
    private User remote_user; //通话的对方

    private Videoplay view = null;      //对面大视图视频
    private Videoplay view2 = null;       //对面小视图视频
    private boolean isSmallView = false;      //是否是小视图

    // private SurfaceView surfaceView;
    private SelfVideoplay selfVideo;
    private Camera mCamera = null; //创建摄像头处理类
    private int cameraPosition = 1;//1代表前置摄像头，0代表后置摄像头
    //  private SurfaceHolder holder = null; //创建界面句柄，显示视频的窗口句柄


    private RelativeLayout bottom_layout;
    private RelativeLayout head_layout;
    private TextView duration_text;

    //滤镜面板
    private LinearLayout mFilterLayout;
    private RecyclerView mFilterListView;
    private FilterAdapter mAdapter;
    private final MagicFilterType[] types = new MagicFilterType[]{
            MagicFilterType.NONE,
            MagicFilterType.SUNRISE,
            MagicFilterType.SUNSET,
            MagicFilterType.WHITECAT,
            MagicFilterType.BLACKCAT,
            MagicFilterType.ROMANCE,
            MagicFilterType.SAKURA,
            MagicFilterType.WARM,
            MagicFilterType.ANTIQUE,
            MagicFilterType.NOSTALGIA,
            MagicFilterType.CALM,
            MagicFilterType.CRAYON,
            MagicFilterType.SKETCH
    };

    private long chat_start_time;

    public static final int SHOW_TIME = 5;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SHOW_TIME) {
                //更改通话时间
                duration_text.setText(TimeUtil.getTimeStr(chat_start_time, System.currentTimeMillis()));
                handler.sendEmptyMessageDelayed(SHOW_TIME, 1000);

            }
        }
    };

    //语音通话
    private AudioServer audioServer;

    //private VideoServer videoServer;
    private FfmpegServer ffmpegServer;

    public LinkedList<VideoData> dataLinkedList;

    public SoundPool soundPool;
    public HashMap<Integer, Integer> sounddata;


    //桌面视频显示 当程序不可见时 在桌面显示
    private boolean isStop = false;
    private boolean isDestory = false;
    private WindowManager mWindowManager;
    private WrapperLayout wrapper_layout;
    private Videoplay wrapper_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        //initWindow();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        EventBus.getDefault().register(this);

        remote_user = (User) getIntent().getSerializableExtra("remote_user");
        chat_start_time = System.currentTimeMillis();
        audioServer = new AudioServer(getIntent().getIntExtra("remote_audio_port", 0));
        //videoServer = new VideoServer(this, getIntent().getIntExtra("remote_video_port", 0));
        ffmpegServer=new FfmpegServer(this, getIntent().getIntExtra("remote_video_port", 0));
        dataLinkedList = new LinkedList<>();

        soundPool = new SoundPool(12, 0, 5);
        sounddata = new HashMap();
        sounddata.put(1, soundPool.load(this, R.raw.kacha, 1));
        initView();
        //获取的是LocalWindowManager对象
        mWindowManager = this.getWindowManager();
    }

    private void initView() {
        view = (Videoplay) this.findViewById(R.id.video_play);
        view2 = (Videoplay) this.findViewById(R.id.video_play2);

        // surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        // holder = surfaceView.getHolder();
        // holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        selfVideo = (SelfVideoplay) findViewById(R.id.surface_view);

        (findViewById(R.id.click_view)).setOnClickListener(this);
        (findViewById(R.id.btn_camera_closefilter)).setOnClickListener(this);

        mFilterLayout = (LinearLayout) findViewById(R.id.layout_filter);
        mFilterListView = (RecyclerView) findViewById(R.id.filter_listView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mFilterListView.setLayoutManager(linearLayoutManager);

        mAdapter = new FilterAdapter(this, types);
        mFilterListView.setAdapter(mAdapter);
        mAdapter.setOnFilterChangeListener(this);


        bottom_layout = (RelativeLayout) findViewById(R.id.bottom_layout);
        bottom_layout.setVisibility(View.GONE);
        head_layout = (RelativeLayout) findViewById(R.id.head_layout);
        head_layout.setVisibility(View.GONE);

        TextView name_text = (TextView) findViewById(R.id.user_name);
        name_text.setText(remote_user.username);

        duration_text = (TextView) findViewById(R.id.chat_duration);

        handler.sendEmptyMessage(SHOW_TIME);

        audioServer.startRecording();

        //videoServer.doStart();
        ffmpegServer.doStart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isStop = false;
        if (wrapper_layout != null)
            mWindowManager.removeView(wrapper_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("xxxx", "onResume");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStart();

            }
        }, 100);

    }

    @Override
    protected void onStop() {
        Log.e("xxxx", "onStop2");
        super.onStop();
        Log.e("xxxx", "onStop");
        isStop = true;
        // handler.postDelayed(new Runnable() {
        //    @Override
        //   public void run() {
        doWrapper();
        //    }
        //  }, 500);
    }


    /**
     * 开启 接受 发送rtp线程  开启本地摄像头
     */
    public void doStart() {
        openCamera();

    }

    public void openCamera() {
        if (mCamera == null) {

            //摄像头设置，预览视频
            mCamera = Camera.open(cameraPosition); //实例化摄像头类对象
            Camera.Parameters p = mCamera.getParameters(); //将摄像头参数传入p中
            p.setFlashMode("off");
            p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            p.setPictureFormat(PixelFormat.JPEG); // Sets the image format for
            // picture 设定相片格式为JPEG，默认为NV21
            p.setPreviewFormat(PixelFormat.YCbCr_420_SP); // Sets the image format
            //p.setPreviewFormat(ImageFormat.NV21);
            //p.setPreviewFormat(PixelFormat.YCbCr_420_SP); //设置预览视频的格式


            p.setPreviewSize(352, 288); //设置预览视频的尺寸，CIF格式352×288
            //p.setPreviewSize(800, 600);
            p.setPreviewFrameRate(15); //设置预览的帧率，15帧/秒
            mCamera.setParameters(p); //设置参数



  /*          int PreviewWidth = 0;
            int PreviewHeight = 0;
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);//获取窗口的管理器
            Display display = wm.getDefaultDisplay();//获得窗口里面的屏幕
            Camera.Parameters parameters  = mCamera.getParameters();
            // 选择合适的预览尺寸
            List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();

            // 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
            if (sizeList.size() > 1) {
                Iterator<Camera.Size> itor = sizeList.iterator();
                while (itor.hasNext()) {
                    Camera.Size cur = itor.next();
                    if (cur.width >= PreviewWidth
                            && cur.height >= PreviewHeight) {
                        PreviewWidth = cur.width;
                        PreviewHeight = cur.height;
                        break;
                    }
                }
            }
            parameters.setPreviewSize(PreviewWidth, PreviewHeight); //获得摄像区域的大小
            parameters.setPreviewFrameRate(3);//每秒3帧  每秒从摄像头里面获得3个画面
            parameters.setPictureFormat(PixelFormat.JPEG);//设置照片输出的格式
            parameters.set("jpeg-quality", 85);//设置照片质量
            parameters.setPictureSize(PreviewWidth, PreviewHeight);//设置拍出来的屏幕大小
            //
            mCamera.setParameters(parameters);//把上面的设置 赋给摄像头
            */


            byte[] rawBuf = new byte[1400];
            mCamera.addCallbackBuffer(rawBuf);
            mCamera.setDisplayOrientation(90); //视频旋转90度
         /*   try {
                mCamera.setPreviewDisplay(holder); //预览的视频显示到指定窗口
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            mCamera.startPreview(); //开始预览

            //获取帧
            //预览的回调函数在开始预览的时候以中断方式被调用，每秒调用15次，回调函数在预览的同时调出正在播放的帧
            Callback a = new Callback();
            mCamera.setPreviewCallback(a);

        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_camera_closefilter) {
            //关闭滤镜面板
            Animation anmi = AnimationUtils.loadAnimation(this, R.anim.bottim_out);
            anmi.setDuration(500);
            mFilterLayout.setAnimation(anmi);
            mFilterLayout.setVisibility(View.INVISIBLE);
        } else {
            if (isSmallView) {
                view2.setVisibility(View.INVISIBLE);
                view.setVisibility(View.VISIBLE);

                //改变surfaceView 大小 位置  变小

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view2.getWidth(), view2.getHeight());
                params.leftMargin = view2.getLeft();
                params.topMargin = view2.getTop();

                selfVideo.setLayoutParams(params);

                isSmallView = false;
            } else {
                view2.setVisibility(View.VISIBLE);
                view.setVisibility(View.INVISIBLE);

                //变大
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view.getWidth(), view.getHeight());
                params.leftMargin = 0;
                params.topMargin = 0;


                selfVideo.setLayoutParams(params);


                isSmallView = true;
            }
        }
        // }
    }


    //mCamera回调的类
    class Callback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] frame, Camera camera) {
            //audioServer.encode();
            // Log.d("xxxxx","size1:"+Arrays.toString(frame));
            //YUVtoRGBUtil.decodeYUV420SP(selfVideo.mPixel,frame,352,288);

            YuvImage image = new YuvImage(ffmpegServer.sendData(frame), ImageFormat.NV21, 352, 288, null);            //ImageFormat.NV21  640 480
            ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 70, outputSteam); // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流
            selfVideo.jpegData = outputSteam.toByteArray();
            selfVideo.postInvalidate();

        }
    }


    @Override
    public void receiveVideoStream(byte[] frmbuf, int frmSize, long timestamp) {
        //与语音同步
    //    videoServer.decode.DecoderNal(frmbuf, frmSize, wrapper_view.mPixel);//解码后的图像存在mPixel中
    //    wrapper_view.postInvalidate();
        //Log.e(Config.TAG, "接受视频数据间隔x：" + (audioServer.lastTime - timestamp));

        byte decode[]=ffmpegServer.ffmpeg.videodecode(frmbuf);
        YuvImage image = new YuvImage(decode, ImageFormat.NV21, 352, 288, null);            //ImageFormat.NV21  640 480
        ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 70, outputSteam); // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流


        if (isStop && wrapper_view != null) {
            wrapper_view.jpegData = outputSteam.toByteArray();
            wrapper_view.postInvalidate();

        } else if (isSmallView) {
            view2.jpegData = outputSteam.toByteArray();
            view2.postInvalidate();
        } else {
            view.jpegData = outputSteam.toByteArray();
            view.postInvalidate();
        }

        //播方视频
        //Log.e(Config.TAG,"播放视频");
       /* if (isStop && wrapper_view != null) {
            videoServer.decode.DecoderNal(frmbuf, frmSize, wrapper_view.mPixel);
            wrapper_view.postInvalidate();

        } else if (isSmallView) {
            videoServer.decode.DecoderNal(frmbuf, frmSize, view2.mPixel);//解码后的图像存在mPixel中
            view2.postInvalidate();
        } else {
            videoServer.decode.DecoderNal(frmbuf, frmSize, view.mPixel);//解码后的图像存在mPixel中
            view.postInvalidate();
        }

        */

      /*
        VideoData videoData = new VideoData(frmbuf, frmSize, timestamp);
        dataLinkedList.addLast(videoData);
       while (dataLinkedList.size() > 0) {
            final VideoData data = dataLinkedList.getFirst();
            if (Math.abs(data.time - audioServer.lastTime) <= 200) {
                //播方视频
                //Log.e(Config.TAG,"播放视频");
                if (isStop && wrapper_view != null) {
                    videoServer.decode.DecoderNal(data.data, data.size, wrapper_view.mPixel);//解码后的图像存在mPixel中
                    wrapper_view.postInvalidate();

                } else if (isSmallView) {
                    videoServer.decode.DecoderNal(data.data, data.size, view2.mPixel);//解码后的图像存在mPixel中
                    view2.postInvalidate();
                } else {
                    videoServer.decode.DecoderNal(data.data, data.size, view.mPixel);//解码后的图像存在mPixel中
                    view.postInvalidate();
                }
                dataLinkedList.removeFirst();
                break;
            } else if (data.time - audioServer.lastTime > 200) {
                //视频流比音频流快  不播放
                //Log.e(Config.TAG,"视频流比音频流快  不播放:"+(data.time - audioServer.lastTime));
                break;
            } else if (data.time - audioServer.lastTime < 200) {
                //视频流比音频流慢  舍弃  继续遍历
                //Log.e(Config.TAG,"视频流比音频流慢  舍弃");
                dataLinkedList.removeFirst();
                continue;
            }
        }
        //Log.d(com.nercms.Config.TAG,"size:"+frmSize+"  gg1:"+ Arrays.toString(frmbuf));
        // }
*/

    }


    /**
     * 关闭摄像头 并释放资源
     */
    public void close() {

        //释放摄像头资源
        if (mCamera != null) {
            mCamera.setPreviewCallback(null); //停止回调函数
            mCamera.stopPreview(); //停止预览
            mCamera.release(); //释放资源
            mCamera = null; //重新初始化
        }

        //videoServer.stopServer();
        ffmpegServer.stopServer();
        audioServer.stopRecording();

        //通知对方关闭
        if (!isClosed) {
            Request request = new Request();
            request.from_id = MyApplication.getInstance().getSpUtil().getUser().id;
            request.into_id = remote_user.id;
            request.tag = MessageTag.STOP_VIDEO;

            MyApplication.getInstance().getSendMsgUtil().sendMessageToServer(
                    MyApplication.getInstance().getGson().toJson(request)
            );
        }

        soundPool.release();

        //初始化selfvideoplay  Videoplay
        Videoplay.close();

    }

    public boolean isClosed = false;        //是不是对方关闭的

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("xxxx", "onDestroy");
        EventBus.getDefault().unregister(this);
        close();
        if (wrapper_layout != null) {
            mWindowManager.removeView(wrapper_layout);
        }
        isDestory = true;
    }

    /**
     * 关闭视频聊天
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleVideoChat(Request request) {

        if (request.tag == MessageTag.STOP_VIDEO && request.from_id == remote_user.id) {
            Log.d(Config.TAG, "关闭视频通话");
            isClosed = true;
            if (isStop)
                finish();
            else {
                //弹出对话框
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("视频已关闭")
                        .setMessage("对方关闭了视频")
                        .setCancelable(false)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                builder.show();
            }

            close();
        } else if (request.tag == MessageTag.CHANGE_CAMERA) {
            Log.d(Config.TAG, "改变摄像模式");
            //对方改变了摄像头模式
            view.setCameraPosition(request.extra_int);
            view2.setCameraPosition(request.extra_int);
            wrapper_view.setCameraPosition(request.extra_int);
        } else if (request.tag == MessageTag.CHANGE_FILTER) {
            Log.d(TAG, "改变滤镜");
            ChangeFilterRequest changeFilterRequest = (ChangeFilterRequest) request;
            Videoplay.setFilterType(changeFilterRequest.type);
        }
    }

    /**
     * 改变视频视角
     *
     * @param v
     */
    public void doChange(View v) {
        //切换前后摄像头
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (cameraPosition == 1) {
                //现在是前置，变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.setPreviewCallback(null); //停止回调函数
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    cameraPosition = 0;
                    openCamera();
                    break;
                }
            } else if (cameraPosition == 0) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.setPreviewCallback(null); //停止回调函数
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    cameraPosition = 1;
                    openCamera();
                    break;
                }
            }

        }

        //将当前的摄像头模式发给对面方便视频图像的翻转
        Request request = new Request();
        request.tag = MessageTag.CHANGE_CAMERA;
        request.from_id = MyApplication.getInstance().getSpUtil().getUser().id;
        request.into_id = remote_user.id;
        request.extra_int = cameraPosition;

        MyApplication.getInstance().getSendMsgUtil().sendMessageToServer(
                MyApplication.getInstance().getGson().toJson(request)
        );

        showLayout();
        selfVideo.setCameraPosition(cameraPosition);
    }

    //截图
    public void doCamera(View v) {
        checkPermission();
    }


    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            final Videoplay cameraView;
            if (isSmallView)
                cameraView = view2;
            else
                cameraView = view;
            //闪烁
            Animation alphaAnimation = new AlphaAnimation(0.5f, 1f);

            alphaAnimation.setDuration(800);

            alphaAnimation.setRepeatCount(0);

            //alphaAnimation.setRepeatMode( Animation.REVERSE );

            cameraView.startAnimation(alphaAnimation);

            //声音
            soundPool.play(sounddata.get(1), 1, 1, 0, 0, 1);

            //线条
            cameraView.startCamera();
            cameraView.postInvalidate();


            //保存图片
            if (cameraView.VideoBit2 != null) ;
            String filePath = Util.saveBitmap(this, cameraView.VideoBit2);

            /**
             * 4.4以上要手动多媒体扫描才能在系统图库里发现图片
             * 4.4一下用系统通知
             */
            MediaScanner mediaScanner = new MediaScanner(this);
            String[] filePaths = new String[]{filePath};
            String[] mimeTypes = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("jpg")};
            mediaScanner.scanFiles(filePaths, mimeTypes);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cameraView.stopCamera();
                    cameraView.postInvalidate();
                }
            }, 1000);

            showLayout();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                //takePhoto();
            } else {
                Toast.makeText(this, "保存图片失败...", Toast.LENGTH_SHORT).show();

            }
        }
    }

    /**
     * 关闭视频
     *
     * @param v
     */

    public void doCancle(View v) {
        finish();
    }

    private boolean isAlreadyShow = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //手势抬起
            Log.d("VideoChatActivity", "抬起");
            showLayout();
        }
        return super.onTouchEvent(event);
    }



    /**
     * 动画显示上下布局
     */
    public void showLayout() {
        if (isAlreadyShow) {
            //已经在显示 就隐藏
            Animation anmi = AnimationUtils.loadAnimation(this, R.anim.bottim_out);
            anmi.setDuration(500);
            bottom_layout.setAnimation(anmi);
            bottom_layout.setVisibility(View.GONE);

            Animation anmi2 = AnimationUtils.loadAnimation(this, R.anim.head_out);
            anmi2.setDuration(500);
            head_layout.setAnimation(anmi2);
            head_layout.setVisibility(View.GONE);


            isAlreadyShow = false;
        } else {
            //已经隐藏 就显示
            Animation anmi = AnimationUtils.loadAnimation(this, R.anim.bottom_coming);
            anmi.setDuration(500);
            bottom_layout.setAnimation(anmi);
            bottom_layout.setVisibility(View.VISIBLE);

            Animation anmi2 = AnimationUtils.loadAnimation(this, R.anim.head_coming);
            anmi2.setDuration(500);
            head_layout.setAnimation(anmi2);
            head_layout.setVisibility(View.VISIBLE);

            isAlreadyShow = true;
        }
    }

    //本机滤镜滤镜改变
    @Override
    public void onFilterChanged(MagicFilterType filterType) {
        selfVideo.setFilterType(filterType);
        //发送滤镜改变请求个对方
        ChangeFilterRequest request = new ChangeFilterRequest();
        request.from_id = MyApplication.getInstance().getSpUtil().getUser().id;
        request.into_id = remote_user.id;
        request.type = filterType;

        MyApplication.getInstance().getSendMsgUtil().sendMessageToServer(
                MyApplication.getInstance().getGson().toJson(request)
        );

    }

    public void doFilter(View v) {
        showLayout();
        //已经隐藏 就显示
        Animation anmi = AnimationUtils.loadAnimation(this, R.anim.bottom_coming);
        anmi.setDuration(500);
        mFilterLayout.setAnimation(anmi);
        mFilterLayout.setVisibility(View.VISIBLE);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            //全屏隐藏状态栏
            View mDecorView = getWindow().getDecorView();
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | TextView.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    //启动桌面视频
    private void doWrapper() {
        if (wrapper_layout == null) {
            LayoutInflater inflater = this.getLayoutInflater();//LayoutInflater.from(getApplication());
            wrapper_layout = (WrapperLayout) inflater.inflate(R.layout.wrapper_layout, null);
            wrapper_view = (Videoplay) wrapper_layout.findViewById(R.id.wrapper_view);
        }

        //获取LayoutParams对象
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();

        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;      //以左上角为原点
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;


        wrapper_layout.setWindowManager(mWindowManager);
        if (!isDestory)
            mWindowManager.addView(wrapper_layout, wmParams);

    }
}
