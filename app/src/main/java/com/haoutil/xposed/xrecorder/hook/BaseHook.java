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

        String[] results = ((String) param.getResult()).replace(".amr", "").split("-");
        fileName = fileName
                .replaceAll("nn", callerName)
                .replaceAll("pp", phoneNumber)
                .replaceAll("yyyy", results[0])
                .replaceAll("MM", results[1])
                .replaceAll("dd", results[2])
                .replaceAll("HH", results[3])
                .replaceAll("mm", results[4])
                .replaceAll("ss", results[5]);

        param.setResult(fileName + ".amr");

        callerName = null;
        phoneNumber = null;
    }
}
