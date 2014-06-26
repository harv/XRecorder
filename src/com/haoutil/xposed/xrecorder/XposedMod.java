package com.haoutil.xposed.xrecorder;

import java.lang.reflect.Field;

import android.os.Environment;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallerInfo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {
	private final static String TAG = "XRecorder";
	
	private Field mApp, mCM, mCallRecorder;
	private Object callRecorder;
	private CallManager cm;
	private String phoneName, phoneNumber;
	private Call.State previousState;
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.android.phone")) {
			return;
		}
		
		Class<?> clazz1 = XposedHelpers.findClass("com.android.phone.SomcInCallScreen", lpparam.classLoader);
		Class<?> clazz2 = XposedHelpers.findClass("com.android.phone.SomcCallRecorder", lpparam.classLoader);
		Class<?> clazz3 = XposedHelpers.findClass("com.android.phone.PhoneGlobals", lpparam.classLoader);
		
		mApp = XposedHelpers.findField(clazz1, "mApp");
		mCM = XposedHelpers.findField(clazz1, "mCM");
		mCallRecorder = XposedHelpers.findField(clazz3, "mCallRecorder");
		
		hook1(clazz1);
		hook2(clazz2);
	}

	private void hook1(Class<?> clazz) {
		XposedBridge.hookAllMethods(clazz, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				cm = (CallManager) mCM.get(param.thisObject);
				Object app = mApp.get(param.thisObject);
				if (app != null) {
					callRecorder = mCallRecorder.get(app);
				}
			}
		});
		
		XposedBridge.hookAllMethods(clazz, "onPhoneStateChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (cm == null || callRecorder == null || (Boolean) XposedHelpers.callMethod(callRecorder, "isRecording")) {
					return;
				}

				if (cm.getActiveFgCallState() == Call.State.ACTIVE) {
					try {
						CallerInfo callerInfo = (CallerInfo) XposedHelpers.callMethod(param.thisObject, "getCallerInfoFromConnection",
								XposedHelpers.callMethod(param.thisObject, "getConnectionFromCall", cm.getActiveFgCall()));
						phoneName = callerInfo.name;
						phoneNumber = callerInfo.phoneNumber.startsWith("sip:") ? callerInfo.phoneNumber.substring(4) : callerInfo.phoneNumber;
					} catch (Exception e) {
						XposedBridge.log(TAG + " can not get caller info.");
					}
					
					try {
						if (previousState == Call.State.ALERTING) {
							XposedHelpers.callMethod(callRecorder, "setSaveDirectory", Environment.getExternalStorageDirectory().getPath() + "/recorder/outgoing");
						} else {
							XposedHelpers.callMethod(callRecorder, "setSaveDirectory", Environment.getExternalStorageDirectory().getPath() + "/recorder/incoming");
						}
						
						XposedHelpers.callMethod(callRecorder, "start");
					} catch (Exception e) {
						XposedBridge.log(TAG + " can not start call recorder.");
					}
				}
				
				previousState = cm.getActiveFgCallState();
			}
		});
	}

	private void hook2(Class<?> clazz) {
		XposedBridge.hookAllMethods(clazz, "generateFilename", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				String result = ((String) param.getResult()).replaceAll("-", "");
				if (phoneNumber != null && !"".equals(phoneNumber)) {
					result = phoneNumber.replaceAll(" ", "") + "_" + result;
				}
				if (phoneName != null && !"".equals(phoneName)) {
					result = phoneName + "_" + result;
				}
				param.setResult(result);
			}
		});
	}
}
