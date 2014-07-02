package com.haoutil.xposed.xrecorder;

import android.os.Environment;

public class Common {
	public final static String DEFAULT_SAVEDIRECTORY = Environment
			.getExternalStorageDirectory().getPath() + "/recorder";
	public final static String DEFAULT_FIELFORMAT = "nn_pp_yyyyMMddHHmmss";
}
