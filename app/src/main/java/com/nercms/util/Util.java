package com.nercms.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.nercms.Config;
import com.nercms.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by zsg on 2016/6/4.
 */
public class Util {

    private static final File parentPath = Environment.getExternalStorageDirectory();
    private static String storagePath = "";
    private static final String DST_FOLDER_NAME = "PlayCamera";

    /**
     * 根据位置得到 头像id；
     *
     * @param position
     * @return
     */
    public static int getHeadId(int position) {
        switch (position) {
            case 0:
                return R.drawable.h1;
            case 1:
                return R.drawable.h2;
            case 2:
                return R.drawable.h3;
            case 3:
                return R.drawable.h4;
            case 4:
                return R.drawable.h5;
            case 5:
                return R.drawable.h6;
            case 6:
                return R.drawable.h7;
            case 7:
                return R.drawable.h8;
            case 8:
                return R.drawable.h9;
            case 9:
                return R.drawable.h10;
        }

        return R.drawable.h10;
    }

    /**
     * 保存Bitmap到sdcard
     *
     * @param b
     */
    public static String saveBitmap(Context context, Bitmap b) {
        String path = initPath(context);
        long dataTake = System.currentTimeMillis();
        String jpegName = path + "/" + dataTake + ".jpg";
        Log.e(Config.TAG, "saveBitmap:jpegName = " + jpegName);
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            Log.i(Config.TAG, "saveBitmap成功");


            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
//            MediaScannerConnection.scanFile(context,
//                    new String[]{path}, null,
//                    new MediaScannerConnection.OnScanCompletedListener() {
//                        public void onScanCompleted(String path, Uri uri) {
//                            Log.i("ExternalStorage", "Scanned " + path + ":");
//                            Log.i("ExternalStorage", "-> uri=" + uri);
//                        }
//                    });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(Config.TAG, "saveBitmap:失败");
            e.printStackTrace();

            return null;
        }

        return jpegName;
    }

    /**
     * 初始化保存路径
     *
     * @return
     */
    private static String initPath(Context context) {
        if (storagePath.equals("")) {
            File file = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES+"/FengLing");
            // Make sure the Pictures directory exists.
            file.mkdirs();
            storagePath = file.getPath();
            Log.e(Config.TAG,"保存路径："+storagePath);
        }

        return storagePath;
    }

}
