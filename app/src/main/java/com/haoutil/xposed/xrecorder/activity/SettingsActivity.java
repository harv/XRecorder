package com.haoutil.xposed.xrecorder.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

import com.haoutil.xposed.xrecorder.BuildConfig;
import com.haoutil.xposed.xrecorder.R;
import com.haoutil.xposed.xrecorder.hook.HookBuiltin;
import com.haoutil.xposed.xrecorder.util.Constants;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.content, new PrefsFragment()).commit();
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_settings;
    }

    public static class PrefsFragment extends PreferenceFragment {
        private CheckBoxPreference cbEnableAll;
        private CheckBoxPreference cbEnableOutgoing;
        private CheckBoxPreference cbEnableIncoming;
        private EditTextPreference etFilePath;
        private EditTextPreference etFileFormat;
        private Preference pAppInfo;

        private boolean isBuiltinRecorderExist;
        private boolean isSeparateRecorderExist;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);

            addPreferencesFromResource(R.xml.preferences);

            isBuiltinRecorderExist = HookBuiltin.isBuiltinRecorderExist;
            isSeparateRecorderExist = packageExists("com.sonymobile.callrecording");

            cbEnableAll = (CheckBoxPreference) findPreference("pref_enable_auto_call_recording");
            if (!isSeparateRecorderExist) {
                cbEnableAll.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        cbEnableOutgoing.setEnabled((Boolean) newValue);
                        cbEnableIncoming.setEnabled((Boolean) newValue);
                        return true;
                    }
                });
            }

            cbEnableOutgoing = (CheckBoxPreference) findPreference("pref_enable_outgoing_call_recording");
            if (isSeparateRecorderExist) {
                cbEnableOutgoing.setEnabled(false);
                cbEnableOutgoing.setSummary(R.string.only_support_builtin_recorder);
            } else {
                cbEnableOutgoing.setEnabled(cbEnableAll.isChecked());
            }

            cbEnableIncoming = (CheckBoxPreference) findPreference("pref_enable_incoming_call_recording");
            if (isSeparateRecorderExist) {
                cbEnableIncoming.setEnabled(false);
                cbEnableIncoming.setSummary(R.string.only_support_builtin_recorder);
            } else {
                cbEnableIncoming.setEnabled(cbEnableAll.isChecked());
            }

            etFilePath = (EditTextPreference) findPreference("pref_file_path");
            etFilePath.setDialogMessage(getString(R.string.file_path_note) + Constants.DEFAULT_FILE_PATH);
            if (isSeparateRecorderExist) {
                etFilePath.setEnabled(false);
                etFilePath.setSummary(R.string.only_support_builtin_recorder);
            }
            if (TextUtils.isEmpty(etFilePath.getText())) {
                etFilePath.setText(Constants.DEFAULT_FILE_PATH);
            }

            etFileFormat = (EditTextPreference) findPreference("pref_file_format");
            if (TextUtils.isEmpty(etFileFormat.getText())) {
                etFileFormat.setText(Constants.DEFAULT_FILE_FORMAT);
            }

            pAppInfo = findPreference("pref_app_info");
            pAppInfo.setSummary(getString(R.string.app_info_version) + " v" + BuildConfig.VERSION_NAME + "\n" + getString(R.string.app_info_author));

            if (!Build.MANUFACTURER.startsWith("Sony")) {
                showAlert(R.string.alert_only_support_sony_device);
            }

            boolean isRecorderExist = isBuiltinRecorderExist || isSeparateRecorderExist;
            if (!isRecorderExist) {
                showAlert(R.string.alert_no_recorder_exist);
            }
        }

        private boolean packageExists(String packageName) {
            try {
                getActivity().getPackageManager().getPackageInfo(packageName, 1);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        private void showAlert(int msgResId) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.alert_warning)
                    .setMessage(msgResId)
                    .setPositiveButton(R.string.alert_ok_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }
}
