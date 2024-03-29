/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.cyanogenmod.settings.device.R;
import com.cyanogenmod.settings.device.VibratorIntensity;

public class SensorsFragmentActivity extends PreferenceFragment {

    private static final String PREF_ENABLED = "1";
    private static final String TAG = "GalaxyS2Parts_General";

    private static final String FILE_USE_GYRO_CALIB = "/sys/class/sec/gsensorcal/calibration";
    private static final String FILE_TOUCHKEY_LIGHT = "/data/.disable_touchlight";
    private static final String FILE_TOUCHKEY_TOGGLE = "/sys/class/misc/sec_touchkey/brightness";
    private static final String FILE_BLN_TOGGLE = "/sys/class/misc/backlightnotification/enabled";
    private static final String FILE_BLN_CONTROL = "/sys/class/misc/backlightnotification/blink_control";
    private static final String FILE_BLN_INTERVAL = "/sys/class/misc/backlightnotification/blink_interval";
    private static final String FILE_S2W_TOGGLE = "/sys/android_touch/sweep2wake";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.sensors_preferences);

        final PreferenceGroup motorsCategory =
                (PreferenceGroup) findPreference(DisplaySettings.KEY_SENSORS_MOTORS_CATEGORY);

        if (!VibratorIntensity.isSupported()) {
            getPreferenceScreen().removePreference(motorsCategory);
        }

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        String boxValue;
        String key = preference.getKey();

        Log.w(TAG, "key: " + key);

        if (key.compareTo(DisplaySettings.KEY_USE_GYRO_CALIBRATION) == 0) {
            boxValue = (((CheckBoxPreference)preference).isChecked() ? "1" : "0");
            Utils.writeValue(FILE_USE_GYRO_CALIB, boxValue);
        } else if (key.compareTo(DisplaySettings.KEY_CALIBRATE_GYRO) == 0) {
            // when calibration data utilization is disablen and enabled back,
            // calibration is done at the same time by driver
            Utils.writeValue(FILE_USE_GYRO_CALIB, "0");
            Utils.writeValue(FILE_USE_GYRO_CALIB, "1");
            Utils.showDialog((Context)getActivity(), "Calibration done", "The gyroscope has been successfully calibrated!");
        } else if (key.compareTo(DisplaySettings.KEY_TOUCHKEY_LIGHT) == 0) {
            Utils.writeValue(FILE_TOUCHKEY_LIGHT, ((CheckBoxPreference)preference).isChecked() ? "0" : "1");
            Utils.writeValue(FILE_TOUCHKEY_TOGGLE, ((CheckBoxPreference)preference).isChecked() ? "1" : "2");
        } else if (key.compareTo(DisplaySettings.KEY_TOUCHKEY_BLN) == 0) {
            Utils.writeValue(FILE_BLN_TOGGLE, ((CheckBoxPreference)preference).isChecked() ? "1" : "0");
        } else if (key.compareTo(DisplaySettings.KEY_TOUCHKEY_BLN_OFF) == 0) {
            Utils.writeValue(FILE_BLN_CONTROL, "0");
            Utils.showToast((Context)getActivity(), "BLN has been successfully turned off!");
        } else if (key.compareTo(DisplaySettings.KEY_TOUCHKEY_S2W) == 0) {
            Utils.writeValue(FILE_S2W_TOGGLE, ((CheckBoxPreference)preference).isChecked() ? "1" : "0");
        }

        return true;
    }

    public static boolean isSupported(String FILE) {
        return Utils.fileExists(FILE);
    }

    public static void restore(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // When use gyro calibration value is set to 1, calibration is done at the same time, which
        // means it is reset at each boot, providing wrong calibration most of the time at each reboot.
        // So we only set it to "0" if user wants it, as it defaults to 1 at boot
        if (!sharedPrefs.getBoolean(DisplaySettings.KEY_USE_GYRO_CALIBRATION, true))
            Utils.writeValue(FILE_USE_GYRO_CALIB, "0");

        Utils.writeValue(FILE_TOUCHKEY_LIGHT, sharedPrefs.getBoolean(DisplaySettings.KEY_TOUCHKEY_LIGHT, true) ? "0" : "1");
        Utils.writeValue(FILE_TOUCHKEY_TOGGLE, sharedPrefs.getBoolean(DisplaySettings.KEY_TOUCHKEY_LIGHT, true) ? "1" : "2");
        Utils.writeValue(FILE_BLN_TOGGLE, sharedPrefs.getBoolean(DisplaySettings.KEY_TOUCHKEY_BLN, false) ? "1" : "0");
        Utils.writeValue(FILE_BLN_INTERVAL, sharedPrefs.getString(DisplaySettings.KEY_TOUCHKEY_BLN_INTERVAL, "500 500"));
        Utils.writeValue(FILE_S2W_TOGGLE, sharedPrefs.getBoolean(DisplaySettings.KEY_TOUCHKEY_S2W, false) ? "1" : "0");
    }
}
