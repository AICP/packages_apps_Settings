/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;
import com.android.settings.widget.SeekBarPreferenceCham;

public class Hover extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "HoverSettings";

    private static final String PREF_HOVER_STATE = "hover_state";
    private static final String PREF_SHOW_HOVER_TIME = "show_hover_time";
    private static final String PREF_HOVER_REQUIRE_FULLSCREEN_MODE = "hover_require_fullscreen_mode";
    private static final String PREF_HOVER_EXCLUDE_NON_CLEARABLE = "hover_exclude_non_clearable";
    private static final String PREF_HOVER_EXCLUDE_LOW_PRIORITY = "hover_exclude_low_priority";
    private static final String PREF_HOVER_EXCLUDE_TOPMOST = "hover_exclude_topmost";

    private CheckBoxPreference mHoverState;
    private CheckBoxPreference mHoverRequireFullScreen;
    private CheckBoxPreference mHoverNonClearable;
    private CheckBoxPreference mHoverLowPriority;
    private CheckBoxPreference mHoverTopmost;
    private SeekBarPreferenceCham mShowHoverTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.aicp_hover);

        PreferenceScreen prefSet = getPreferenceScreen();

        // Hover
        mHoverState = (CheckBoxPreference) prefSet.findPreference(PREF_HOVER_STATE);
        mHoverState.setChecked((Settings.System.getInt(resolver,
                Settings.System.HOVER_STATE, 0) == 1));

        mShowHoverTime = (SeekBarPreferenceCham) prefSet.findPreference(PREF_SHOW_HOVER_TIME);
        mShowHoverTime.setValue(Settings.System.getInt(resolver,
                Settings.System.SHOW_HOVER_TIME, 5000));
        mShowHoverTime.setOnPreferenceChangeListener(this);

        mHoverRequireFullScreen = (CheckBoxPreference) prefSet.findPreference(PREF_HOVER_REQUIRE_FULLSCREEN_MODE);
        mHoverRequireFullScreen.setChecked((Settings.System.getInt(resolver,
                Settings.System.HOVER_REQUIRE_FULLSCREEN_MODE, 0) == 1));

        mHoverNonClearable = (CheckBoxPreference) prefSet.findPreference(PREF_HOVER_EXCLUDE_NON_CLEARABLE);
        mHoverNonClearable.setChecked((Settings.System.getInt(resolver,
                Settings.System.HOVER_EXCLUDE_NON_CLEARABLE, 0) == 1));

        mHoverLowPriority = (CheckBoxPreference) prefSet.findPreference(PREF_HOVER_EXCLUDE_LOW_PRIORITY);
        mHoverLowPriority.setChecked((Settings.System.getInt(resolver,
                Settings.System.HOVER_EXCLUDE_LOW_PRIORITY, 0) == 1));

        mHoverTopmost = (CheckBoxPreference) prefSet.findPreference(PREF_HOVER_EXCLUDE_TOPMOST);
        mHoverTopmost.setChecked((Settings.System.getInt(resolver,
                Settings.System.HOVER_EXCLUDE_TOPMOST, 0) == 1));

    }
       
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mHoverState) {
            value = mHoverState.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.HOVER_STATE, value ? 1 : 0);
        } else if (preference == mHoverRequireFullScreen) {
            value = mHoverRequireFullScreen.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.HOVER_REQUIRE_FULLSCREEN_MODE, value ? 1 : 0);
        } else if (preference == mHoverNonClearable) {
            value = mHoverNonClearable.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.HOVER_EXCLUDE_NON_CLEARABLE, value ? 1 : 0);
        } else if (preference == mHoverLowPriority) {
            value = mHoverLowPriority.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.HOVER_EXCLUDE_LOW_PRIORITY, value ? 1 : 0);
        } else if (preference == mHoverTopmost) {
            value = mHoverTopmost.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.HOVER_EXCLUDE_TOPMOST, value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if ( preference == mShowHoverTime) {
            int time = ((Integer)objValue).intValue();
            Settings.System.putInt(resolver,
                    Settings.System.SHOW_HOVER_TIME, time);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}
