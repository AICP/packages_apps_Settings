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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

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

    private static final String KERNELTWEAKER_START = "kerneltweaker_start";

    // Package name of the kernel tweaker app
    public static final String KERNELTWEAKER_PACKAGE_NAME = "com.dsht.kerneltweaker";
    // Intent for launching the kernel tweaker main actvity
    public static Intent INTENT_KERNELTWEAKER = new Intent(Intent.ACTION_MAIN)
            .setClassName(KERNELTWEAKER_PACKAGE_NAME, KERNELTWEAKER_PACKAGE_NAME + ".MainActivity");

    private Preference mKernelTweaker;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.aicp_lab_prefs);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mKernelTweaker = (Preference)
                prefSet.findPreference(KERNELTWEAKER_START);

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
        if (preference == mKernelTweaker){
            startActivity(INTENT_KERNELTWEAKER);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}

