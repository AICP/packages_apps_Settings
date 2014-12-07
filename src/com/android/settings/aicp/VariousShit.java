/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
 * Copyright (C) 2014 The Android Ice Cold Project
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

package com.android.settings.aicp;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;

/**
 * LAB files borrowed from excellent ChameleonOS for AICP
 */
public class VariousShit extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "VariousShit";

    private static final String KEY_LOCKCLOCK = "lock_clock";
    private static final String PROP_DISPLAY_DENSITY = "persist.sf.lcd_density";
    private static final String KEY_DISPLAY_DENSITY = "display_density";

    // Package name of the cLock app
    public static final String LOCKCLOCK_PACKAGE_NAME = "com.cyanogenmod.lockclock";

    private EditTextPreference mDisplayDensity;
    private SwitchPreference mProximityWake;
    private PreferenceScreen mVariousShitScreen;

    private Preference mLockClock;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.aicp_various_shit);

        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getPackageManager();
        Resources res = getResources();

        mVariousShitScreen = (PreferenceScreen) findPreference("various_shit_screen");

        // Proximity wake up
        mProximityWake = (SwitchPreference) findPreference("proximity_on_wake");
        boolean proximityCheckOnWait = res.getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        if (!proximityCheckOnWait) {
            mVariousShitScreen.removePreference(mProximityWake);
        }

        // cLock app check
        mLockClock = (Preference) findPreference(KEY_LOCKCLOCK);
        if (!Helpers.isPackageInstalled(LOCKCLOCK_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mLockClock);
        }

        // Custom density
        mDisplayDensity = (EditTextPreference) findPreference(KEY_DISPLAY_DENSITY);
        mDisplayDensity.setText(SystemProperties.get(PROP_DISPLAY_DENSITY, "0"));
        mDisplayDensity.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_DISPLAY_DENSITY.equals(key)) {
            final int max = getResources().getInteger(R.integer.display_density_max);
            final int min = getResources().getInteger(R.integer.display_density_min);

            int value = SystemProperties.getInt(PROP_DISPLAY_DENSITY, 0);
            try {
                value = Integer.parseInt((String) objValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid input", e);
            }

            // 0 disables the custom density, so do not check for the value, else…
            if (value != 0) {
                // …cap the value
                if (value < min) {
                    value = min;
                } else if (value > max) {
                    value = max;
                }
            }

            SystemProperties.set(PROP_DISPLAY_DENSITY, String.valueOf(value));
            mDisplayDensity.setText(String.valueOf(value));

            // we handle it, return false
            return false;
        }
        return true;
    }
}

