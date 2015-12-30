package com.haoutil.xposed.xrecorder;

import android.content.res.Resources;

import com.android.server.CustomService;
import com.haoutil.xposed.xrecorder.hook.BaseHook;
import com.haoutil.xposed.xrecorder.hook.HookBuiltin;
import com.haoutil.xposed.xrecorder.hook.HookSeparate;
import com.haoutil.xposed.xrecorder.util.Logger;
import com.haoutil.xposed.xrecorder.util.SettingsHelper;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {
    private BaseHook hookBuiltin;
    private BaseHook hookSeparate;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        SettingsHelper mSettingsHelper = new SettingsHelper();
        Logger mLogger = new Logger(mSettingsHelper);
        hookBuiltin = new HookBuiltin(mSettingsHelper, mLogger);
        hookSeparate = new HookSeparate(mSettingsHelper, mLogger);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {
        switch (loadPackageParam.packageName) {
            case "android":
                CustomService.register(loadPackageParam.classLoader);
                break;
            case "com.android.phone":
                hookBuiltin.hook(loadPackageParam);
                break;
            case "com.android.incallui":
            case "com.sonymobile.callrecording":
                hookSeparate.hook(loadPackageParam);
                break;
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam) throws Throwable {
        if (initPackageResourcesParam.packageName.endsWith("com.android.phone")) {
            try {
                initPackageResourcesParam.res.setReplacement("com.android.phone", "bool", "enable_call_recording", true);
            } catch (Resources.NotFoundException e) {
            }
        }
    }
}
