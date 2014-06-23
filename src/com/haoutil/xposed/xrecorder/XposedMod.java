package com.haoutil.xposed.xrecorder;

import java.lang.reflect.Field;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {
	private Field mApp, mCM, mCallRecorder;
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals("com.android.phone")) {
			return;
		}
		
		Class<?> clazz1 = XposedHelpers.findClass("com.android.phone.SomcInCallScreen", lpparam.classLoader);
		Class<?> clazz2 = XposedHelpers.findClass("com.android.phone.PhoneGlobals", lpparam.classLoader);
		
		mApp = XposedHelpers.findField(clazz1, "mApp");
		mCM = XposedHelpers.findField(clazz1, "mCM");
		mCallRecorder = XposedHelpers.findField(clazz2, "mCallRecorder");
		
		hook(clazz1);
	}

	private void hook(Class<?> clazz) {
		XposedBridge.hookAllMethods(clazz, "onPhoneStateChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				CallManager cm = (CallManager) mCM.get(param.thisObject);
				Object callRecorder = mCallRecorder.get(mApp.get(param.thisObject));
				
				if ((Boolean) XposedHelpers.callMethod(callRecorder, "isRecording")) {
					return;
				}
				
				if (cm.getActiveFgCallState() == Call.State.ACTIVE) {
					XposedHelpers.callMethod(callRecorder, "start");
				}
			}
		});
	}
}
