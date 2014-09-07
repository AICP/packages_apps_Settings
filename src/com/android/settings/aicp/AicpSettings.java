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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;

/**
 * LAB files borrowed from excellent ChameleonOS for AICP
 * Settings for new features that we want to test out with our users should go
 * here.  If a feature is to be accepted into ChaOS permanently we can move
 * the settings for that feature to the appropriate place.  Features that don't
 * get approved can simply be removed.
 */
public class AicpSettings extends SettingsPreferenceFragment
        implements OnSharedPreferenceChangeListener {

    private static final String TAG = "AicpLabs";

    private static final String KEY_OMNISWITCH = "omniswitch";
    private static final String KEY_KERNELTWEAKER_START = "kerneltweaker_start";
    private static final String KEY_AICPOTA_START = "aicp_ota_start";

    // Package name of the OmniSwitch app
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";

    // Package name of the KernelTweaker app
    public static final String KERNELTWEAKER_PACKAGE_NAME = "com.dsht.kerneltweaker";
    // Intent for launching the KernelTweaker main actvity
    public static Intent INTENT_KERNELTWEAKER = new Intent(Intent.ACTION_MAIN)
            .setClassName(KERNELTWEAKER_PACKAGE_NAME, KERNELTWEAKER_PACKAGE_NAME + ".MainActivity");

    // Package name of the AICP OTA app
    public static final String AICPOTA_PACKAGE_NAME = "com.paranoid.paranoidota";
    // Intent for launching the AICP OTA main actvity
    public static Intent INTENT_AICPOTA = new Intent(Intent.ACTION_MAIN)
            .setClassName(AICPOTA_PACKAGE_NAME, AICPOTA_PACKAGE_NAME + ".MainActivity");

    private Preference mOmniSwitch;
    private Preference mKernelTweaker;
    private Preference mAicpOta;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.aicp_lab_prefs);

        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getPackageManager();       

        mOmniSwitch = (Preference)
                prefSet.findPreference(KEY_OMNISWITCH);
        if (!Helpers.isPackageInstalled(OMNISWITCH_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mOmniSwitch);
        }

        mKernelTweaker = (Preference)
                prefSet.findPreference(KEY_KERNELTWEAKER_START);
        if (!Helpers.isPackageInstalled(KERNELTWEAKER_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mKernelTweaker);
        }

        mAicpOta = (Preference)
                prefSet.findPreference(KEY_AICPOTA_START);
        if (!Helpers.isPackageInstalled(AICPOTA_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mAicpOta);
        }
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
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mKernelTweaker) {
            startActivity(INTENT_KERNELTWEAKER);
            return true;
        } else if (preference == mAicpOta) {
            startActivity(INTENT_AICPOTA);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}

