package com.nercms.model;

import java.util.Arrays;

/**
 * 音频缓存数据
 * Created by zsg on 2016/10/27.
 */
public class AudioData {
    public byte[] data;
    public int size;
    public long time;

    public AudioData(byte[] decodedData, int decodeSize,long time) {
        this.data = Arrays.copyOf(decodedData, decodedData.length);
        this.size = decodeSize;
        this.time=time;
    }
}
