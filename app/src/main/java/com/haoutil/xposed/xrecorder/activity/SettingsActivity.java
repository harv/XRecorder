package com.haoutil.xposed.xrecorder.activity;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

import com.haoutil.xposed.xrecorder.R;
import com.haoutil.xposed.xrecorder.util.Constants;

public class SettingsActivity extends Activity {
    private static String versionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
        }

        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            versionName = info.versionName;
        } catch (NameNotFoundException e) {
        }
    }

    public static class PrefsFragment extends PreferenceFragment {
        private CheckBoxPreference cbEnableAll;
        private CheckBoxPreference cbEnableOutgoing;
        private CheckBoxPreference cbEnableIncoming;
        private EditTextPreference etFilePath;
        private EditTextPreference etFileFormat;
        private Preference pAppinfo;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);

            addPreferencesFromResource(R.xml.preferences);

            cbEnableAll = (CheckBoxPreference) findPreference("pref_enable_auto_call_recording");
            cbEnableAll.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    cbEnableOutgoing.setEnabled((Boolean) newValue);
                    cbEnableIncoming.setEnabled((Boolean) newValue);
                    return true;
                }
            });

            cbEnableOutgoing = (CheckBoxPreference) findPreference("pref_enable_outgoing_call_recording");
            cbEnableOutgoing.setEnabled(cbEnableAll.isChecked());

            cbEnableIncoming = (CheckBoxPreference) findPreference("pref_enable_incoming_call_recording");
            cbEnableIncoming.setEnabled(cbEnableAll.isChecked());

            etFilePath = (EditTextPreference) findPreference("pref_file_path");
            etFilePath.setDialogMessage(getString(R.string.file_path_note) + Constants.DEFAULT_FILE_PATH);
            if (TextUtils.isEmpty(etFilePath.getText())) {
                etFilePath.setText(Constants.DEFAULT_FILE_PATH);
            }

            etFileFormat = (EditTextPreference) findPreference("pref_file_format");
            if (TextUtils.isEmpty(etFileFormat.getText())) {
                etFileFormat.setText(Constants.DEFAULT_FILE_FORMAT);
            }

            pAppinfo = findPreference("pref_app_info");
            pAppinfo.setSummary(getString(R.string.app_info_version) + " v" + versionName + "\n" + getString(R.string.app_info_author));
        }
    }
}
