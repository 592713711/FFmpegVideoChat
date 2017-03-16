package com.nercms.model;

import java.util.Arrays;

/**
 * Created by zsg on 2016/10/27.
 */
public class VideoData {
    public byte[] data;
    public int size;
    public long time;

    public VideoData(byte[] decodedData, int decodeSize,long time) {
        this.data = Arrays.copyOf(decodedData, decodedData.length);
        this.size = decodeSize;
        this.time=time;
    }
}
