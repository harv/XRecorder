package com.haoutil.xposed.xrecorder;

import java.lang.reflect.Field;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallerInfo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	private final static String TAG = "XRecorder";
	
	private XSharedPreferences pref;
	
	private Field mApp, mCM, mCallRecorder;
	private Object callRecorder;
	private CallManager cm;
	private String callerName, phoneNumber;
	private Call.State previousState;
	
	private boolean isEnableRecord;
	private boolean isEnableRecordOutgoing;
	private boolean isEnableRecordIncoming;
	private boolean isOptimizeDisplayCallerName;
	private String saveDirectory;
	private String fielFormat;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		pref = new XSharedPreferences(XposedMod.class.getPackage().getName());
	}
	
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
		
		XposedBridge.hookAllMethods(clazz, "onResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				pref.reload();
				
				isEnableRecord = pref.getBoolean("pref_enable_auto_call_recording", true);
				isEnableRecordOutgoing = pref.getBoolean("pref_enable_outgoing_call_recording", true);
				isEnableRecordIncoming = pref.getBoolean("pref_enable_incoming_call_recording", true);
				isOptimizeDisplayCallerName = pref.getBoolean("pref_optimize_display_caller_name", true);
				saveDirectory = pref.getString("pref_file_path", Common.DEFAULT_SAVEDIRECTORY);
				fielFormat = pref.getString("pref_file_format", Common.DEFAULT_FIELFORMAT);
			}
		});
		
		XposedBridge.hookAllMethods(clazz, "onPhoneStateChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!isEnableRecord) {
					return;
				}
				
				if (cm == null || callRecorder == null) {
					return;
				}

				if (cm.getActiveFgCallState() == Call.State.ACTIVE) {
					if ((Boolean) XposedHelpers.callMethod(callRecorder, "isRecording")) {
						return;
					}
					
					try {
						CallerInfo callerInfo = (CallerInfo) XposedHelpers.callMethod(param.thisObject, "getCallerInfoFromConnection",
								XposedHelpers.callMethod(param.thisObject, "getConnectionFromCall", cm.getActiveFgCall()));
						callerName = callerInfo.name;
						phoneNumber = callerInfo.phoneNumber.startsWith("sip:") ? callerInfo.phoneNumber.substring(4) : callerInfo.phoneNumber;
					} catch (Exception e) {
						callerName = "unkown caller";
						phoneNumber = "";
						XposedBridge.log(TAG + " can not get caller info.");
					}
					
					try {
						if (previousState == Call.State.ALERTING) {
							if (!isEnableRecordOutgoing) {
								return;
							}
							
							XposedHelpers.callMethod(callRecorder, "setSaveDirectory", saveDirectory + "/outgoing");
						} else {
							if (!isEnableRecordIncoming) {
								return;
							}
							
							XposedHelpers.callMethod(callRecorder, "setSaveDirectory", saveDirectory + "/incoming");
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
				String[] results = ((String) param.getResult()).replace(".amr", "").split("-");
				String fileName = fielFormat;
				if (callerName == null || "".equals(callerName)) {
					if (isOptimizeDisplayCallerName) {
						fileName = fileName.replaceAll("nn((?!pp|yyyy|MM|dd|HH|mm|ss).)*", "");
					} else {
						callerName = "unkown caller";
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
