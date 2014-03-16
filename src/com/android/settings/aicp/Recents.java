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

import android.content.ContentResolver;
import android.content.Context;
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
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.Gravity;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

public class Recents extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "RecentsSettings";

    private static final String RECENTS_CLEAR_ALL = "recents_clear_all";
    private static final String CUSTOM_RECENT_MODE = "custom_recent_mode";
    private static final String RECENT_PANEL_LEFTY_MODE = "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SCALE = "recent_panel_scale";

    private ListPreference mClearAll;
    private CheckBoxPreference mRecentsCustom;
    private CheckBoxPreference mRecentPanelLeftyMode;
    private ListPreference mRecentPanelScale;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.aicp_recents);

        PreferenceScreen prefSet = getPreferenceScreen();

        // Clear all position
        mClearAll = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL);
        int value = Settings.System.getInt(resolver,
                Settings.System.CLEAR_RECENTS_BUTTON_LOCATION, 4);
        mClearAll.setValue(String.valueOf(value));
        mClearAll.setSummary(mClearAll.getEntry());
        mClearAll.setOnPreferenceChangeListener(this);

        // Slim's recents
        boolean enableRecentsCustom = Settings.AOKP.getBoolean(getContentResolver(),
                Settings.System.CUSTOM_RECENT, false);
        mRecentsCustom = (CheckBoxPreference) prefSet.findPreference(CUSTOM_RECENT_MODE);
        mRecentsCustom.setChecked(enableRecentsCustom);
        mRecentsCustom.setOnPreferenceChangeListener(this);

        boolean recentLeftyMode = Settings.System.getInt(resolver,
                Settings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
        mRecentPanelLeftyMode = (CheckBoxPreference) prefSet.findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setChecked(recentLeftyMode);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);

        int recentScale = Settings.System.getInt(resolver,
                Settings.System.RECENT_PANEL_SCALE_FACTOR, 100);
        mRecentPanelScale = (ListPreference) prefSet.findPreference(RECENT_PANEL_SCALE);
        mRecentPanelScale.setValue(recentScale + "");
        mRecentPanelScale.setOnPreferenceChangeListener(this);

    }
       

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (RECENTS_CLEAR_ALL.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mClearAll.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.CLEAR_RECENTS_BUTTON_LOCATION,
                    value);
            mClearAll.setSummary(mClearAll.getEntries()[index]);
        } else if (preference == mRecentsCustom) {
            Settings.AOKP.putBoolean(resolver,
                    Settings.System.CUSTOM_RECENT,
                    ((Boolean) objValue) ? true : false);
            Helpers.restartSystemUI();
        } else if (preference == mRecentPanelScale) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.RECENT_PANEL_SCALE_FACTOR, value);
        } else if (preference == mRecentPanelLeftyMode) {
            Settings.System.putInt(resolver,
                    Settings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) objValue) ? Gravity.LEFT : Gravity.RIGHT);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}
