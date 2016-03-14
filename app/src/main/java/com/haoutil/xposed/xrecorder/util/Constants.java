package com.haoutil.xposed.xrecorder.util;

import android.os.Environment;

public class Constants {
    public static final String DEFAULT_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/recorder";
    public static final String DEFAULT_FILE_FORMAT = "nn_pp_yyyyMMddHHmmss";
    public static final String DEFAULT_FILE_CALLTYPE = "incoming:outgoing";
}
