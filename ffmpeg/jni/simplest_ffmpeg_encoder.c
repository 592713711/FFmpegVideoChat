/**
 * 最简单的基于FFmpeg的视频解码器-安卓 - 单库版
 * Simplest FFmpeg Android Decoder - One Library
 *
 * 雷霄骅 Lei Xiaohua
 * leixiaohua1020@126.com
 * 中国传媒大学/数字电视技术
 * Communication University of China / Digital TV Technology
 * http://blog.csdn.net/leixiaohua1020
 *
 * 本程序是安卓平台下最简单的基于FFmpeg的视频解码器。
 * 它可以将输入的视频数据解码成YUV像素数据。
 *
 * This software is the simplest decoder based on FFmpeg in Android.
 * It can decode video stream to raw YUV data.
 *
 */


#include <stdio.h>
#include <time.h>

#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavutil/log.h"

#ifdef ANDROID
#include <jni.h>
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "(>_<)", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "(^_^)", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("(>_<) " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("(^_^) " format "\n", ##__VA_ARGS__)
#endif


AVCodecContext *pCodecCtx= NULL;
AVPacket avpkt;
//FILE * video_file;
unsigned char *outbuf=NULL;
unsigned char *yuv420buf=NULL;
AVFrame * yuv420pframe = NULL;
static int outsize=0;
static int mwidth = 352;
static int mheight = 288;
int count = 0;

int isStart=0;  //1 开启状态  0关闭状态


//decoder
AVCodecContext *decoder_CodecCtx= NULL;
AVFrame * decoder_pFrame = NULL;


/*
* encording init
*/
JNIEXPORT jint JNICALL Java_com_zsg_ffmpegvideolib_Ffmpeg_videoinit(JNIEnv * env, jclass obj)
{
    if(isStart)
        return -1;

    LOGI("%s\n",__func__);
    AVCodec * pCodec=NULL;

    avcodec_register_all();
    pCodec=avcodec_find_encoder(AV_CODEC_ID_MPEG4);  //AV_CODEC_ID_H264//AV_CODEC_ID_MPEG1VIDEO
    if(pCodec == NULL) {
        LOGE("++++++++++++codec not found\n");
        return -1;
    }
    pCodecCtx=avcodec_alloc_context3(pCodec);
    if (pCodecCtx == NULL) {
        LOGE("++++++Could not allocate video codec context\n");
        return -1;
    }
    /* put sample parameters */
    pCodecCtx->bit_rate = 400000;
    /* resolution must be a multiple of two */
    pCodecCtx->width = mwidth;
    pCodecCtx->height = mheight;
    /* frames per second */
    pCodecCtx->time_base= (AVRational){1,25};
    pCodecCtx->gop_size = 10; /* emit one intra frame every ten frames */
    pCodecCtx->max_b_frames=1;
    pCodecCtx->pix_fmt = AV_PIX_FMT_YUV420P;//AV_PIX_FMT_YUYV422;
    /* open it */
    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGE("+++++++Could not open codec\n");
        return -1;
    }
    outsize = mwidth * mheight*2;
    outbuf = malloc(outsize*sizeof(char));
    yuv420buf = malloc(outsize*sizeof(char));
    //jbyte *filedir = (jbyte*)(*env)->GetByteArrayElements(env, filename, 0);
 /*   if ((video_file = fopen(filedir, "wb")) == NULL) {
        LOGE("++++++++++++open %s failed\n",filedir);
        return -1;
    }*/
    //(*env)->ReleaseByteArrayElements(env, filename, filedir, 0);


    //初始化解码器
    AVCodec * decoder_pCodec = avcodec_find_decoder(AV_CODEC_ID_MPEG4);
    decoder_CodecCtx = avcodec_alloc_context3(decoder_pCodec);
    decoder_CodecCtx->time_base.num = 1; //这两行：一秒钟25帧

    decoder_CodecCtx->time_base.den = 25;

    decoder_CodecCtx->bit_rate = 0; //初始化为0

    decoder_CodecCtx->frame_number = 1; //每包一个视频帧

    //decoder_CodecCtx->codec_type = CODEC_TYPE_VIDEO;

    decoder_CodecCtx->width = mwidth; //这两行：视频的宽度和高度

    decoder_CodecCtx->height = mheight;

    if(avcodec_open2(decoder_CodecCtx, decoder_pCodec, NULL) >= 0)

    {

        decoder_pFrame = avcodec_alloc_frame();// Allocate video frame

    }

    isStart=1;
    return 1;
}

JNIEXPORT jbyteArray JNICALL Java_com_zsg_ffmpegvideolib_Ffmpeg_videoencode(JNIEnv * env, jclass obj,jbyteArray yuvdata)
{
    int frameFinished=0,size=0;
	LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart1");
    jbyte *ydata = (jbyte*)(*env)->GetByteArrayElements(env, yuvdata, 0);
    LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart2");
    yuv420pframe=NULL;
    //AVFrame * yuv422frame=NULL;
    //struct SwsContext *swsctx = NULL;
	av_init_packet(&avpkt);
	avpkt.data = NULL;    // packet data will be allocated by the encoder
    avpkt.size = 0;
    yuv420pframe=avcodec_alloc_frame();
	int y_size = pCodecCtx->width * pCodecCtx->height;


	uint8_t* picture_buf;
    int size1 = avpicture_get_size(pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height);
    picture_buf = (uint8_t*)av_malloc(y_size);
    if (!picture_buf)
    {
        av_free(yuv420pframe);
     }
    avpicture_fill((AVPicture*)yuv420pframe, picture_buf, pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height);

    //yuv422frame=avcodec_alloc_frame();
	yuv420pframe->pts = count;
	yuv420pframe->data[0] = ydata;  //PCM Data
	yuv420pframe->data[1] = ydata+ y_size;      // U
	yuv420pframe->data[2] = ydata+ y_size*5/4;  // V

    //avpicture_fill((AVPicture *) yuv420pframe, (uint8_t *)ydata, AV_PIX_FMT_YUV420P,mwidth,mheight);//(uint8_t *)yuv420buf
    LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart3");
        //avpicture_fill((AVPicture *) yuv422frame, (uint8_t *)ydata, AV_PIX_FMT_YUYV422,mwidth,mheight);
    LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart4");
        //swsctx = sws_getContext(mwidth,mheight, AV_PIX_FMT_YUYV422, mwidth, mheight,AV_PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);
    LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart5");
        //sws_scale(swsctx,(const uint8_t* const*)yuv422frame->data,yuv422frame->linesize,0,mheight,yuv420pframe->data,yuv420pframe->linesize);
    LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart6");
    LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart7");
    if(!isStart)     //开启状态
        return NULL;
    size = avcodec_encode_video2(pCodecCtx, &avpkt, yuv420pframe, &frameFinished);
    LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart8");
    count++;
    if (size < 0) {
        LOGE("+++++Error encoding frame\n");
        return -1;
    }

    jbyteArray jarray = NULL;
    if(frameFinished){
       LOGE("JNICALL Java_com_hua_cameraandroidtest_MainActivity_videostart9");
       jarray = (*env)->NewByteArray(env,avpkt.size);
       jbyte *by = (jbyte*)avpkt.data;
       (*env)->SetByteArrayRegion(env,jarray, 0, avpkt.size, by);
    }else{
       jarray = (*env)->NewByteArray(env,0);
    }

   LOGE("zsg:  tagB1");
   av_free_packet(&avpkt);
   //sws_freeContext(swsctx);
   av_free(yuv420pframe);
   //av_free(yuv422frame);
   (*env)->ReleaseByteArrayElements(env, yuvdata, ydata, 0);
   LOGE("zsg:  tagB2");
   return jarray;
}



JNIEXPORT jint JNICALL Java_com_zsg_ffmpegvideolib_Ffmpeg_videoclose(JNIEnv * env, jclass obj)
{
    if(!isStart)
        return -1;
     LOGE("zsg:  tag1");
    avcodec_close(pCodecCtx);
         LOGE("zsg:  tag2");
    avcodec_close(decoder_CodecCtx);
         LOGE("zsg:  tag3");
    av_free(pCodecCtx);
         LOGE("zsg:  tag4");
    av_free(decoder_CodecCtx);
         LOGE("zsg:  tag5");
	av_freep(&yuv420pframe->data[0]);
	     LOGE("zsg:  tag6");
    av_frame_free(&yuv420pframe);
         LOGE("zsg:  tag7");
    av_frame_free(&decoder_pFrame);
         LOGE("zsg:  tag8");
    free(outbuf);
         LOGE("zsg:  tag9");
    isStart=0;

    return 1;
}


JNIEXPORT jbyteArray JNICALL Java_com_zsg_ffmpegvideolib_Ffmpeg_videodecode
  (JNIEnv *env, jobject obj, jbyteArray data)
{

    jbyte *ydata = (jbyte*)(*env)->GetByteArrayElements(env, data, 0);

    AVPacket avpkt;

    int ret, got_picture;

    av_init_packet(&avpkt);
    avpkt.data = ydata;    // packet data will be allocated by the encoder
    avpkt.size = (*env)->GetArrayLength(env,data); //获取长度;

     //解码一帧视频数据。输入一个压缩编码的结构体AVPacket，输出一个解码后的结构体AVFrame。
    //avcodec_decode_video2(AVCodecContext *avctx, AVFrame *picture,
                                       //int *got_picture_ptr,
                                       //const AVPacket *avpkt);
    if(!isStart)
         return NULL;
    ret = avcodec_decode_video2(decoder_CodecCtx, decoder_pFrame, &got_picture, &avpkt);

    int newSize=decoder_CodecCtx->height * decoder_CodecCtx->width * 3 / 2;

    //要返回到java层的数据

    LOGE("zsg:  got_picture %d",got_picture);
    if(got_picture){

        //将数据变成YUV数组
        char* buf =(char *)malloc(sizeof(char)*newSize);

        memset(buf, 0, newSize);
        int height = decoder_CodecCtx->height;
        int width = decoder_CodecCtx->width;
        printf("decode video ok\n");
        int a = 0, i;
        for (i = 0; i<height; i++)
        {
            memcpy(buf + a, decoder_pFrame->data[0] + i * decoder_pFrame->linesize[0], width);
            a += width;
        }
        for (i = 0; i<height / 2; i++)
        {
            memcpy(buf + a, decoder_pFrame->data[1] + i * decoder_pFrame->linesize[1], width / 2);
            a += width / 2;
        }
        for (i = 0; i<height / 2; i++)
        {
            memcpy(buf + a, decoder_pFrame->data[2] + i * decoder_pFrame->linesize[2], width / 2);
            a += width / 2;
        }


         //返回将byte数组转换成jbyteArray返回的java层

         jbyte *by = (jbyte*)buf;
         jbyteArray jarray = (*env)->NewByteArray(env,newSize);

         (*env)->SetByteArrayRegion(env,jarray, 0, newSize, by);

         av_free_packet(&avpkt);

         free(&buf);

        (*env)->ReleaseByteArrayElements(env,data, ydata, 0);


         return jarray;
    }

    av_free_packet(&avpkt);
    (*env)->ReleaseByteArrayElements(env,data, ydata, 0);

    return (*env)->NewByteArray(env,0);

}

