package com.nercms.receive;

import java.nio.ByteBuffer;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.nercms.MyApplication;
import com.nercms.filter.ImageFilterFactory;
import com.nercms.filter.MagicFilterType;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSepiaFilter;

public class SelfVideoplay extends View {
    public static int width = 352;
    public static int height = 288;
    public  byte[] mPixel = new byte[width * height * 3];
    public ByteBuffer buffer = ByteBuffer.wrap(mPixel);
    public Bitmap VideoBit = Bitmap.createBitmap(width, height, Config.RGB_565);
    private Matrix matrix = null;
    public Bitmap VideoBit2 = Bitmap.createBitmap(width, height, Config.RGB_565);

    private RectF rectF;

    private int cameraPosition = 1;


    private Paint photoPaint;


    public  byte[] jpegData;

    private GPUImage gpuImage;
    private MagicFilterType filterType=MagicFilterType.NONE;

    public SelfVideoplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        matrix = new Matrix();
        //   DisplayMetrics dm = getResources().getDisplayMetrics();
        //   int W = dm.widthPixels;
        //  int H = dm.heightPixels;


        photoPaint = new Paint();
        photoPaint.setColor(Color.WHITE);
        photoPaint.setStyle(Paint.Style.STROKE);   //空心
        photoPaint.setStrokeWidth(35);

        gpuImage=new GPUImage(MyApplication.getInstance());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        buffer.rewind();

        if(jpegData!=null){

            VideoBit=BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
            //Log.e("yyyy","count:"+VideoBit.getByteCount());
            gpuImage=new GPUImage(MyApplication.getInstance());
            gpuImage.setImage(VideoBit);
            gpuImage.setFilter(ImageFilterFactory.getInstance().getFilter(filterType));
            VideoBit = gpuImage.getBitmapWithFilterApplied();

            setAngle();
            canvas.drawBitmap(VideoBit2, null, rectF, null);
            gpuImage.deleteImage();

        }

/*       VideoBit.copyPixelsFromBuffer(buffer);


        //canvas.drawBitmap(adjustPhotoRotation(VideoBit,90), 0, 0, null);
        //
        //Bitmap b = BitmapFactory.decodeByteArray(mPixel, 0, mPixel.length);
        synchronized (VideoBit2) {
            canvas.drawBitmap(VideoBit2, null, rectF, null);
            //canvas.drawText("sdfasfasdfaasdf",0,0,photoPaint);
        }*/


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d("VideoChatActivity",w+"  "+h);
        rectF = new RectF(0, 0, w, h);

    }

    //  设置旋转比例
    private void setAngle() {
        matrix.reset();
        if (cameraPosition == 1)
            matrix.setRotate(-90);
        else
            matrix.setRotate(90);

        synchronized (VideoBit2) {
            VideoBit2 = Bitmap.createBitmap(VideoBit, 0, 0, VideoBit.getWidth(), VideoBit.getHeight(), matrix, true);
        }
    }

    private Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {

        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }

        final float[] values = new float[9];
        m.getValues(values);

        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];

        m.postTranslate(targetX - x1, targetY - y1);

        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);

        return bm1;
    }

    public void setCameraPosition(int position) {
        this.cameraPosition = position;
    }

    public void setFilterType(MagicFilterType magicFilterType){
        this.filterType=magicFilterType;
    }
}
