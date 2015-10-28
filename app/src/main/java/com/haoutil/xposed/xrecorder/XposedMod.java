package com.haoutil.xposed.xrecorder;

import com.haoutil.xposed.xrecorder.util.Constants;
import com.haoutil.xposed.xrecorder.util.Logger;
import com.haoutil.xposed.xrecorder.util.SettingsHelper;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private SettingsHelper mSettingsHelper;
    private Logger mLogger;
    private Object mCM;
    private Object mCallRecorder;
    private boolean isEnableRecordOutgoing;
    private boolean isEnableRecordIncoming;
    private boolean isOptimizeDisplayCallerName;
    private String saveDirectory;
    private String fileFormat;
    private String callerName;
    private String phoneNumber;
    private Object previousState;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mSettingsHelper = new SettingsHelper();
        mLogger = new Logger(mSettingsHelper);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals("com.android.phone")) {
            return;
        }

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
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mSettingsHelper.reload();
                isEnableRecordOutgoing = mSettingsHelper.getBoolean("pref_enable_outgoing_call_recording", true);
                isEnableRecordIncoming = mSettingsHelper.getBoolean("pref_enable_incoming_call_recording", true);
                isOptimizeDisplayCallerName = mSettingsHelper.getBoolean("pref_optimize_display_caller_name", true);
                saveDirectory = mSettingsHelper.getString("pref_file_path", Constants.DEFAULT_FILE_PATH);
                fileFormat = mSettingsHelper.getString("pref_file_format", Constants.DEFAULT_FILE_FORMAT);
            }
        });

        XposedBridge.hookAllMethods(somcInCallScreen, "onPhoneStateChanged", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!isEnableRecordIncoming && isEnableRecordOutgoing) {
                    return;
                }
                if (mCM == null || mCallRecorder == null) {
                    mLogger.log("not support call recording");
                    return;
                }
                final Class<? extends Enum> CallState = (Class<? extends Enum>) Class.forName("com.android.internal.telephony.Call$State");
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
                        callerName = "unkown caller";
                        phoneNumber = "";
                    }
                    try {
                        if (previousState == Enum.valueOf(CallState, "ALERTING")) {
                            if (!isEnableRecordOutgoing) {
                                return;
                            }
                            XposedHelpers.callMethod(mCallRecorder, "setSaveDirectory", saveDirectory + "/outgoing");
                            mLogger.log("recording outgoing call");
                        } else {
                            if (!isEnableRecordIncoming) {
                                return;
                            }
                            XposedHelpers.callMethod(mCallRecorder, "setSaveDirectory", saveDirectory + "/incoming");
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

        Class<?> somcCallRecorder = XposedHelpers.findClass("com.android.phone.SomcCallRecorder", loadPackageParam.classLoader);
        XposedBridge.hookAllMethods(somcCallRecorder, "generateFilename", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String[] results = ((String) param.getResult()).replace(".amr", "").split("-");
                String fileName = fileFormat;
                if (callerName == null || "".equals(callerName)) {
                    if (isOptimizeDisplayCallerName) {
                        fileName = fileName.replaceAll("nn((?!pp|yyyy|MM|dd|HH|mm|ss).)*", "");
                    } else {
                        callerName = "unknown caller";
                    }
                }
                fileName = fileName
                        .replaceAll("nn", callerName)
                        .replaceAll("pp", phoneNumber.replaceAll(" ", ""))
                        .replaceAll("yyyy", results[0])
                        .replaceAll("MM", results[1])
                        .replaceAll("dd", results[2])
                        .replaceAll("HH", results[3])
                        .replaceAll("mm", results[4])
                        .replaceAll("ss", results[5]);
                param.setResult(fileName + ".amr");
            }
        });
    }
}
