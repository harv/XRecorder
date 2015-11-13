package com.haoutil.xposed.xrecorder.util;

import de.robv.android.xposed.XposedBridge;

public class Logger {
    private SettingsHelper settingsHelper;

    public Logger(SettingsHelper settingsHelper) {
        this.settingsHelper = settingsHelper;
    }

    public void log(String msg) {
        if (settingsHelper.isEnableLogging()) {
            XposedBridge.log("[XRecorder]" + msg);
        }
    }
}
