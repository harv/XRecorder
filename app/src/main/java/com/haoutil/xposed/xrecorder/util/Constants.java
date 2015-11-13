package com.haoutil.xposed.xrecorder.util;

import android.os.Environment;

import com.haoutil.xposed.xrecorder.BuildConfig;

public class Constants {
    public static final String DEFAULT_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/recorder";
    public static final String DEFAULT_FILE_FORMAT = "nn_pp_yyyyMMddHHmmss";
}
