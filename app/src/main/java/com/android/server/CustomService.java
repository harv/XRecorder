package com.android.server;

import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.ICustomService;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * register a custom service as a system service for sharing data
 */
public class CustomService extends ICustomService.Stub {
    private static final String SERVICE_NAME = "xrecorder";
    private static Context mContext;
    private static CustomService mCustomService;
    private static ICustomService mClient;

    private boolean waitingForRecording;
    private boolean recordingStopped;
    private boolean builtinRecorderExist;
    private String callerName;
    private String phoneNumber;
    private boolean setSaveDirectoryable;

    public CustomService(Context context) {
        mContext = context;
    }

    public static ICustomService getClient() {
        if (mClient == null) {
            try {
                Class<?> ServiceManager = Class.forName("android.os.ServiceManager");
                Method getService = ServiceManager.getDeclaredMethod("getService", String.class);
                mClient = ICustomService.Stub.asInterface((IBinder) getService.invoke(null, getServiceName()));
            } catch (Throwable t) {
                mClient = null;
            }
        }

        return mClient;
    }

    public static String getServiceName() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? "user." + SERVICE_NAME : SERVICE_NAME;
    }

    public static void register(final ClassLoader classLoader) {
        Class<?> ActivityManagerService = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", classLoader);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            XposedBridge.hookAllConstructors(ActivityManagerService, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    register(classLoader, (Context) XposedHelpers.getObjectField(param.thisObject, "mContext"));
                }
            });
        } else {
            XposedBridge.hookAllMethods(ActivityManagerService, "main", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    register(classLoader, (Context) param.getResult());
                }
            });
        }

        XposedBridge.hookAllMethods(ActivityManagerService, "systemReady", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mCustomService.systemReady();
            }
        });
    }

    private static void register(final ClassLoader classLoader, Context context) {
        mCustomService = new CustomService(context);

        Class<?> ServiceManager = XposedHelpers.findClass("android.os.ServiceManager", classLoader);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            XposedHelpers.callStaticMethod(
                    ServiceManager,
                    "addService",
                    getServiceName(),
                    mCustomService,
                    true
            );
        } else {
            XposedHelpers.callStaticMethod(
                    ServiceManager,
                    "addService",
                    getServiceName(),
                    mCustomService
            );
        }
    }

    private void systemReady() {
        // Make initialization here
    }

    @Override
    public boolean isBuiltinRecorderExist() {
        return builtinRecorderExist;
    }

    @Override
    public void setBuiltinRecorderExist(boolean builtinRecorderExist) {
        this.builtinRecorderExist = builtinRecorderExist;
    }

    @Override
    public boolean isWaitingForRecording() {
        return waitingForRecording;
    }

    @Override
    public void setWaitingForRecording(boolean waitingForRecording) {
        this.waitingForRecording = waitingForRecording;
    }

    @Override
    public boolean isRecordingStopped() {
        return recordingStopped;
    }

    @Override
    public void setRecordingStopped(boolean recordingStopped) {
        this.recordingStopped = recordingStopped;
    }

    @Override
    public String getCallerName() {
        return callerName;
    }

    @Override
    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    @Override
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public boolean isSetSaveDirectoryable() {
        return setSaveDirectoryable;
    }

    @Override
    public void setSetSaveDirectoryable(boolean setSaveDirectoryable) {
        this.setSaveDirectoryable = setSaveDirectoryable;
    }
}
