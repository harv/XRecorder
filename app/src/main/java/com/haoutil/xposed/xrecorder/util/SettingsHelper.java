package com.haoutil.xposed.xrecorder.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.haoutil.xposed.xrecorder.BuildConfig;

import de.robv.android.xposed.XSharedPreferences;

@SuppressWarnings("unused")
public class SettingsHelper {
    private SharedPreferences mPreferences = null;
    private XSharedPreferences mXPreferences = null;

    public SettingsHelper() {
        mXPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID);
        mXPreferences.makeWorldReadable();
        this.reload();
    }

    public SettingsHelper(Context context) {
        this.mPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", 1);
    }

    public boolean isEnableAutoRecord() {
        reload();
        return getBoolean("pref_enable_auto_call_recording", true);
    }

    public boolean isEnableRecordOutgoing() {
        reload();
        return getBoolean("pref_enable_auto_call_recording", true) && getBoolean("pref_enable_outgoing_call_recording", true);
    }

    public boolean isEnableRecordIncoming() {
        reload();
        return getBoolean("pref_enable_auto_call_recording", true) && getBoolean("pref_enable_incoming_call_recording", true);
    }

    public boolean isOptimizeDisplayCallerName() {
        reload();
        return getBoolean("pref_optimize_display_caller_name", true);
    }

    public String getSaveDirectory() {
        reload();
        return getString("pref_file_path", Constants.DEFAULT_FILE_PATH);
    }

    public String getFileFormat() {
        reload();
        return getString("pref_file_format", Constants.DEFAULT_FILE_FORMAT);
    }

    public String[] getFileCallType() {
        reload();
        return getString("pref_file_calltype", Constants.DEFAULT_FILE_CALLTYPE).split(":");
    }

    public boolean isEnableLogging() {
        return getBoolean("pref_enable_logging", false);
    }

    private String getString(String key, String defaultValue) {
        if (mPreferences != null) {
            return mPreferences.getString(key, defaultValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getString(key, defaultValue);
        }

        return defaultValue;
    }

    private int getInt(String key, int defaultValue) {
        if (mPreferences != null) {
            return mPreferences.getInt(key, defaultValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getInt(key, defaultValue);
        }

        return defaultValue;
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        if (mPreferences != null) {
            return mPreferences.getBoolean(key, defaultValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getBoolean(key, defaultValue);
        }

        return defaultValue;
    }

    private void setString(String key, String value) {
        Editor editor = null;
        if (mPreferences != null) {
            editor = mPreferences.edit();
        } else if (mXPreferences != null) {
            editor = mXPreferences.edit();
        }

        if (editor != null) {
            editor.putString(key, value);
            editor.apply();
        }
    }

    private void setBoolean(String key, boolean value) {
        Editor editor = null;
        if (mPreferences != null) {
            editor = mPreferences.edit();
        } else if (mXPreferences != null) {
            editor = mXPreferences.edit();
        }

        if (editor != null) {
            editor.putBoolean(key, value);
            editor.apply();
        }
    }

    private void setInt(String key, int value) {
        Editor editor = null;
        if (mPreferences != null) {
            editor = mPreferences.edit();
        } else if (mXPreferences != null) {
            editor = mXPreferences.edit();
        }

        if (editor != null) {
            editor.putInt(key, value);
            editor.apply();
        }
    }

    private boolean contains(String key) {
        if (mPreferences != null) {
            return mPreferences.contains(key);
        } else if (mXPreferences != null) {
            return mXPreferences.contains(key);
        }

        return false;
    }

    private void reload() {
        if (mXPreferences != null) {
            mXPreferences.reload();
        }
    }
}
