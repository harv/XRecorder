package com.haoutil.xposed.xrecorder;

import com.android.server.CustomService;
import com.haoutil.xposed.xrecorder.hook.BaseHook;
import com.haoutil.xposed.xrecorder.hook.HookBuiltin;
import com.haoutil.xposed.xrecorder.hook.HookSeparate;
import com.haoutil.xposed.xrecorder.util.Logger;
import com.haoutil.xposed.xrecorder.util.SettingsHelper;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit {
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
            case "com.android.phone":
                hookBuiltin.hook(loadPackageParam);
                break;
            case "com.android.incallui":
            case "com.sonymobile.callrecording":
                hookSeparate.hook(loadPackageParam);
                break;
        }
    }
}
