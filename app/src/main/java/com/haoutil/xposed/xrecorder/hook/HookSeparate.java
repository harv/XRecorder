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
    private Class<? extends Enum> InCallState;

    private Object mCallRecordingService;
    private Class<? extends Enum> State;
    private Class<? extends Enum> Transition;

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
                mLogger.log("hook com.android.incallui.CallCardPresenter...");
                final Class<?> callCardPresenter = XposedHelpers.findClass("com.android.incallui.CallCardPresenter", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callCardPresenter, "init", new XC_MethodHook() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        InCallState = (Class<? extends Enum>) XposedHelpers.findClass("com.android.incallui.InCallPresenter$InCallState", loadPackageParam.classLoader);
                    }
                });
                XposedBridge.hookAllMethods(callCardPresenter, "onStateChange", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0] == Enum.valueOf(InCallState, "INCOMING")) {
                            CustomService.getClient().setPhoneState("INCOMING");
                        } else if (param.args[0] == Enum.valueOf(InCallState, "OUTGOING")) {
                            CustomService.getClient().setPhoneState("OUTGOING");
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
                        ICustomService customService = CustomService.getClient();
                        switch (intent.getAction()) {
                            case "com.sonymobile.callrecording.callstarted":
                                // call START and END both trigger "com.sonymobile.callrecording.callstarted" Action
                                if (customService.isRecordingStopped()) {
                                    // call end and manual stop recording, skip
                                    // otherwise it will start recording again
                                    customService.setRecordingStopped(false);
                                    return;
                                }
                                if (customService.isWaitingForRecording()) {
                                    // clear if delay recording is failed
                                    customService.setWaitingForRecording(false);
                                }
                                Object mState = XposedHelpers.getObjectField(param.thisObject, "mState");
                                if (Enum.valueOf(State, "IDLE") == mState) {
                                    if (customService.getPhoneState().equals("INCOMING") && !mSettingsHelper.isEnableRecordIncoming()
                                            || customService.getPhoneState().equals("OUTGOING") && !mSettingsHelper.isEnableRecordOutgoing()) {
                                        return;
                                    }
                                    mLogger.log("start recording");
                                    XposedHelpers.callMethod(param.thisObject, "transitionToState", Enum.valueOf(Transition, "START_RECORDING"));
                                } else if (Enum.valueOf(State, "RECORDING") == mState) {
                                    mLogger.log("end recording");
                                    XposedHelpers.callMethod(param.thisObject, "transitionToState", Enum.valueOf(Transition, "STOP_RECORDING"));
                                } else if (Enum.valueOf(State, "OFF") == mState) {
                                    // CallRecorder is not prepared, wait...
                                    // often occurs when CallRecorder first create
                                    customService.setWaitingForRecording(true);
                                }
                                break;
                            case "com.sonymobile.callrecording.stoprecodring":
                                // manual stop recording
                                mLogger.log("end recording(manual)");
                                customService.setRecordingStopped(true);
                                break;
                            case "com.sonymobile.callwidgetframework.WIDGET_ACTION_SELECTED":
                                // manual start recording
                                mLogger.log("start recording(manual)");
                                customService.setRecordingStopped(false);
                                break;
                        }
                    }
                });

                mLogger.log("hook com.sonymobile.callrecording.CallRecorder...");
                Class<?> callRecorder = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecorder", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callRecorder, "generateFilename", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ICustomService customService = CustomService.getClient();
                        if (customService.getPhoneState().equals("INCOMING") && !mSettingsHelper.isEnableRecordIncoming()
                                || customService.getPhoneState().equals("OUTGOING") && !mSettingsHelper.isEnableRecordOutgoing()) {
                            return;
                        }
                        callType = customService.getPhoneState();
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
                        ICustomService customService = CustomService.getClient();
                        if (customService.getPhoneState().equals("INCOMING") && !mSettingsHelper.isEnableRecordIncoming()
                                || customService.getPhoneState().equals("OUTGOING") && !mSettingsHelper.isEnableRecordOutgoing()) {
                            return;
                        }
                        if (customService.isWaitingForRecording() && (Boolean) param.args[0] && mCallRecordingService != null) {
                            mLogger.log("start recording(delay)");
                            XposedHelpers.callMethod(mCallRecordingService, "transitionToState", Enum.valueOf(Transition, "START_RECORDING"));
                            customService.setWaitingForRecording(false);
                        }
                    }
                });
                break;
        }
    }
}
