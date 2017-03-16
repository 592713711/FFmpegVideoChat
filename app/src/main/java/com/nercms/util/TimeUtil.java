package com.nercms.util;

import java.text.SimpleDateFormat;

/**
 * Created by zsg on 2016/6/5.
 */
public class TimeUtil {
    public static String getTimeStr(long oldTime, long currenttime) {
        long time = currenttime - oldTime;
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        return sdf.format(time);

    }
}
