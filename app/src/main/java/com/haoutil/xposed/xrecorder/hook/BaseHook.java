package com.haoutil.xposed.xrecorder.hook;

import android.text.TextUtils;

import com.haoutil.xposed.xrecorder.util.Logger;
import com.haoutil.xposed.xrecorder.util.SettingsHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class BaseHook {
    public SettingsHelper mSettingsHelper;
    public Logger mLogger;

    public String callerName;
    public String phoneNumber;

    public BaseHook(SettingsHelper mSettingsHelper, Logger mLogger) {
        this.mSettingsHelper = mSettingsHelper;
        this.mLogger = mLogger;
    }

    public abstract void hook(XC_LoadPackage.LoadPackageParam loadPackageParam);

    public void changeFileName(XC_MethodHook.MethodHookParam param) {
        if (TextUtils.isEmpty(phoneNumber)) {
            mLogger.log("can not change file name.");
            return;
        }
        phoneNumber = phoneNumber.replaceAll(" ", "");

        String fileName = mSettingsHelper.getFileFormat();
        if (TextUtils.isEmpty(callerName)) {
            if (mSettingsHelper.isOptimizeDisplayCallerName()) {
                fileName = fileName.replaceAll("nn((?!pp|yyyy|MM|dd|HH|mm|ss).)*", "");
            } else {
                fileName = "unknown";
            }
        }

        String[] results = ((String) param.getResult()).split("\\.");
        String[] names = results[0].split("-");
        fileName = fileName
                .replaceAll("nn", callerName)
                .replaceAll("pp", phoneNumber)
                .replaceAll("yyyy", names[0])
                .replaceAll("MM", names[1])
                .replaceAll("dd", names[2])
                .replaceAll("HH", names[3])
                .replaceAll("mm", names[4])
                .replaceAll("ss", names[5]);

        param.setResult(fileName + "." + results[1]);

        callerName = null;
        phoneNumber = null;
    }
}
