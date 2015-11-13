package com.haoutil.xposed.xrecorder.hook;

import com.android.server.CustomService;
import com.haoutil.xposed.xrecorder.util.Logger;
import com.haoutil.xposed.xrecorder.util.SettingsHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookBuiltin extends BaseHook {
    private Object mCM;
    private Object mCallRecorder;
    private Object previousState;

    private Class<? extends Enum> CallState;

    public HookBuiltin(SettingsHelper mSettingsHelper, Logger mLogger) {
        super(mSettingsHelper, mLogger);
    }

    @Override
    public void hook(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            Class.forName("com.android.phone.SomcCallRecorder");
            CustomService.getClient().setBuiltinRecorderExist(true);
        } catch (Exception e) {
            return;
        }

        mLogger.log("hook com.android.phone.SomcInCallScreen...");
        Class<?> somcInCallScreen = XposedHelpers.findClass("com.android.phone.SomcInCallScreen", loadPackageParam.classLoader);
        XposedBridge.hookAllMethods(somcInCallScreen, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                mCM = XposedHelpers.getObjectField(param.thisObject, "mCM");
                Object mApp = XposedHelpers.getObjectField(param.thisObject, "mApp");
                if (mApp != null) {
                    mCallRecorder = XposedHelpers.getObjectField(mApp, "mCallRecorder");
                }
            }
        });

        XposedBridge.hookAllMethods(somcInCallScreen, "onResume", new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                CallState = (Class<? extends Enum>) Class.forName("com.android.internal.telephony.Call$State");
            }
        });

        XposedBridge.hookAllMethods(somcInCallScreen, "onPhoneStateChanged", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mSettingsHelper.isEnableRecordOutgoing() && !mSettingsHelper.isEnableRecordIncoming()) {
                    return;
                }
                if (mCM == null || mCallRecorder == null) {
                    mLogger.log("not support call recording");
                    return;
                }
                if (XposedHelpers.callMethod(mCM, "getActiveFgCallState") == Enum.valueOf(CallState, "ACTIVE")) {
                    if ((Boolean) XposedHelpers.callMethod(mCallRecorder, "isRecording")) {
                        return;
                    }
                    try {
                        Object callerInfo = XposedHelpers.callMethod(param.thisObject, "getCallerInfoFromConnection",
                                XposedHelpers.callMethod(param.thisObject, "getConnectionFromCall", XposedHelpers.callMethod(mCM, "getActiveFgCall")));
                        callerName = (String) XposedHelpers.getObjectField(callerInfo, "name");
                        phoneNumber = (String) XposedHelpers.getObjectField(callerInfo, "phoneNumber");
                        if (phoneNumber.startsWith("sip:")) {
                            phoneNumber = phoneNumber.substring(4);
                        }
                    } catch (Exception e) {
                        callerName = null;
                        phoneNumber = null;
                    }
                    try {
                        if (previousState == Enum.valueOf(CallState, "ALERTING")) {
                            if (!mSettingsHelper.isEnableRecordOutgoing()) {
                                return;
                            }
                            XposedHelpers.callMethod(mCallRecorder, "setSaveDirectory", mSettingsHelper.getSaveDirectory() + "/outgoing");
                            mLogger.log("recording outgoing call");
                        } else {
                            if (!mSettingsHelper.isEnableRecordIncoming()) {
                                return;
                            }
                            XposedHelpers.callMethod(mCallRecorder, "setSaveDirectory", mSettingsHelper.getSaveDirectory() + "/incoming");
                            mLogger.log("recording incoming call");
                        }
                        XposedHelpers.callMethod(mCallRecorder, "start");
                    } catch (Exception e) {
                        mLogger.log("can not start call recorder.");
                    }
                }
                previousState = XposedHelpers.callMethod(mCM, "getActiveFgCallState");
            }
        });

        mLogger.log("hook com.android.phone.SomcCallRecorder...");
        Class<?> somcCallRecorder = XposedHelpers.findClass("com.android.phone.SomcCallRecorder", loadPackageParam.classLoader);
        XposedBridge.hookAllMethods(somcCallRecorder, "generateFilename", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mSettingsHelper.isEnableRecordOutgoing() && !mSettingsHelper.isEnableRecordIncoming()) {
                    return;
                }
                changeFileName(param);
            }
        });
    }
}
