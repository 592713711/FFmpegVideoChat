# Android.mk for FFmpeg
#
# Lei Xiaohua 雷霄骅
# leixiaohua1020@126.com
# http://blog.csdn.net/leixiaohua1020
# 

LOCAL_PATH := $(call my-dir)

# FFmpeg library
include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeg
LOCAL_SRC_FILES := libffmpeg.so
include $(PREBUILT_SHARED_LIBRARY)


# Program
include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeg_coderc
LOCAL_SRC_FILES :=simplest_ffmpeg_encoder.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_LDLIBS := -llog -lz
LOCAL_SHARED_LIBRARIES := ffmpeg
include $(BUILD_SHARED_LIBRARY)
