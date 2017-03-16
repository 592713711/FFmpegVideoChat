package com.nercms.filter;

import com.nercms.R;

import jp.co.cyberagent.android.gpuimage.GPUImageColorDodgeBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageCrosshatchFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageDilationFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageExposureFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSaturationFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSepiaFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSketchFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSoftLightBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageToonFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageWhiteBalanceFilter;

/**
 * Created by zsg on 2016/11/8.
 */
public class ImageFilterFactory {
    GPUImageContrastFilter mContrastFilter;     //对比度
    GPUImageWhiteBalanceFilter mWhiteBalanceFilter; //色温
    GPUImageSaturationFilter mSaturationFilter;     //饱和度
    GPUImageExposureFilter  mExposureFilter;        //曝光度
    GPUImageSketchFilter mSketchFilter;     //素描
    GPUImageToonFilter mToonFilter;     //卡通
    GPUImageSoftLightBlendFilter mSoftLightBlendFilter;     //柔光混合
    GPUImageColorDodgeBlendFilter mColorDodgeBlendFilter;  //色彩减淡混合
    GPUImageSepiaFilter mSepiaFilter;       //褐色 复古
    GPUImageDilationFilter mDilationFilter;
    private static ImageFilterFactory mInstance;
    private ImageFilterFactory(){
        mContrastFilter=new GPUImageContrastFilter();
        mWhiteBalanceFilter=new GPUImageWhiteBalanceFilter();
        mSaturationFilter=new GPUImageSaturationFilter();
        mExposureFilter=new GPUImageExposureFilter();
        mSketchFilter=new GPUImageSketchFilter();
        mToonFilter=new GPUImageToonFilter();
        mSoftLightBlendFilter=new GPUImageSoftLightBlendFilter();
        mColorDodgeBlendFilter=new GPUImageColorDodgeBlendFilter();
        mSepiaFilter=new GPUImageSepiaFilter();
        mDilationFilter=new GPUImageDilationFilter();
    }

    public static ImageFilterFactory getInstance(){
        if(mInstance==null)
            mInstance=new ImageFilterFactory();
        return mInstance;
    }

    public GPUImageFilter getFilter(MagicFilterType filterType){
        switch (filterType) {
            case NONE:
                mContrastFilter.setContrast(1.0f);
                return  mContrastFilter;
            case SUNRISE:
                mExposureFilter.setExposure(0.3f);
                return mExposureFilter;
            case SUNSET:
                mWhiteBalanceFilter.setTemperature(8000f);
                return mWhiteBalanceFilter;
            case WHITECAT:
                mContrastFilter.setContrast(0.8f);
                return  mContrastFilter;
            case BLACKCAT:
                mContrastFilter.setContrast(3.0f);
                return  mContrastFilter;
            case ROMANCE:
                return mSoftLightBlendFilter;
            case SAKURA:
                return mColorDodgeBlendFilter;
            case WARM:
                mSaturationFilter.setSaturation(1.5f);
                return mSaturationFilter;
            case ANTIQUE:
                return mSepiaFilter;
            case NOSTALGIA:
                return mDilationFilter;
            case CALM:
                mSaturationFilter.setSaturation(0.5f);
                return mSaturationFilter;
            case CRAYON:        //蜡笔
                return mToonFilter;
            case SKETCH:        //素描
                return mSketchFilter;
            default:
                mContrastFilter.setContrast(1.0f);
                return  mContrastFilter;
        }


    }
}
