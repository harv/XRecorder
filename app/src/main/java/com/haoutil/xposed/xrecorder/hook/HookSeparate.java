package com.haoutil.xposed.xrecorder.hook;

import android.content.Intent;
import android.os.ICustomService;

import com.android.server.CustomService;
import com.haoutil.xposed.xrecorder.util.Logger;
import com.haoutil.xposed.xrecorder.util.SettingsHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookSeparate extends BaseHook {
    private Object mCallRecordingService;
    private Class<? extends Enum> State;
    private Class<? extends Enum> Transition;

    private boolean waitingForRecording;
    private boolean recordingStopped;

    public HookSeparate(SettingsHelper mSettingsHelper, Logger mLogger) {
        super(mSettingsHelper, mLogger);
    }

    @Override
    public void hook(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        switch (loadPackageParam.packageName) {
            case "com.android.incallui":
                mLogger.log("hook com.android.incallui.CallCardFragment...");
                final Class<?> callCardFragment = XposedHelpers.findClass("com.android.incallui.CallCardFragment", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callCardFragment, "setPrimary", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!mSettingsHelper.isEnableAutoRecord()) {
                            return;
                        }
                        // share data via custom system services between different processes
                        ICustomService customService = CustomService.getClient();
                        boolean nameIsNumber = (Boolean) param.args[2];
                        if (nameIsNumber) {
                            customService.setCallerName(null);
                            customService.setPhoneNumber((String) param.args[1]);
                        } else {
                            customService.setCallerName((String) param.args[1]);
                            customService.setPhoneNumber((String) param.args[0]);
                        }
                    }
                });
                break;
            case "com.sonymobile.callrecording":
                mLogger.log("hook com.sonymobile.callrecording.CallRecordingService...");
                Class<?> callRecordingService = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingService", loadPackageParam.classLoader);
                XposedBridge.hookAllConstructors(callRecordingService, new XC_MethodHook() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        mCallRecordingService = param.thisObject;
                        State = (Class<? extends Enum>) XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingStateMachine$State", loadPackageParam.classLoader);
                        Transition = (Class<? extends Enum>) XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingStateMachine$Transition", loadPackageParam.classLoader);
                    }
                });
                XposedBridge.hookAllMethods(callRecordingService, "onStartCommand", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                        Intent intent = (Intent) param.args[0];
                        if (intent == null) {
                            return;
                        }
                        if (!mSettingsHelper.isEnableAutoRecord()) {
                            return;
                        }
                        switch (intent.getAction()) {
                            case "com.sonymobile.callrecording.callstarted":
                                // call START and END both trigger "com.sonymobile.callrecording.callstarted" Action
                                if (recordingStopped) {
                                    // call end and manual stop recording, skip
                                    // otherwise it will start recording again
                                    recordingStopped = false;
                                    return;
                                }
                                if (waitingForRecording) {
                                    // clear if delay recording is failed
                                    waitingForRecording = false;
                                }
                                Object mState = XposedHelpers.getObjectField(param.thisObject, "mState");
                                if (Enum.valueOf(State, "IDLE") == mState) {
                                    mLogger.log("start recording");
                                    XposedHelpers.callMethod(param.thisObject, "transitionToState", Enum.valueOf(Transition, "START_RECORDING"));
                                } else if (Enum.valueOf(State, "RECORDING") == mState) {
                                    mLogger.log("end recording");
                                    XposedHelpers.callMethod(param.thisObject, "transitionToState", Enum.valueOf(Transition, "STOP_RECORDING"));
                                } else if (Enum.valueOf(State, "OFF") == mState) {
                                    // CallRecorder is not prepared, wait...
                                    // often occurs when CallRecorder first create
                                    waitingForRecording = true;
                                }
                                break;
                            case "com.sonymobile.callrecording.stoprecodring":
                                // manual stop recording
                                mLogger.log("end recording(manual)");
                                recordingStopped = true;
                                break;
                            case "com.sonymobile.callwidgetframework.WIDGET_ACTION_SELECTED":
                                // manual start recording
                                mLogger.log("start recording(manual)");
                                recordingStopped = false;
                                break;
                        }
                    }
                });

                mLogger.log("hook com.sonymobile.callrecording.CallRecorder...");
                Class<?> callRecorder = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecorder", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callRecorder, "generateFilename", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!mSettingsHelper.isEnableAutoRecord()) {
                            return;
                        }
                        ICustomService customService = CustomService.getClient();
                        callerName = customService.getCallerName();
                        phoneNumber = customService.getPhoneNumber();
                        changeFileName(param);
                    }
                });

                mLogger.log("hook com.sonymobile.callrecording.CallRecordingRemoteUI...");
                Class<?> callRecordingRemoteUI = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingRemoteUI", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callRecordingRemoteUI, "setEnabled", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (waitingForRecording && (Boolean) param.args[0] && mCallRecordingService != null) {
                            mLogger.log("start recording(delay)");
                            XposedHelpers.callMethod(mCallRecordingService, "transitionToState", Enum.valueOf(Transition, "START_RECORDING"));
                            waitingForRecording = false;
                        }
                    }
                });
                break;
        }
    }
}
