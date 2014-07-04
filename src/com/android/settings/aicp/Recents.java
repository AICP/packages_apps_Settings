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
    private static final String RECENT_PANEL_EXPANDED_MODE =
            "recent_panel_expanded_mode";
    private static final String RECENT_PANEL_SHOW_TOPMOST =
            "recent_panel_show_topmost";
    private static final String RAM_CIRCLE = "ram_circle";
    private static final String PREF_RECENTS_SWIPE_FLOATING = "recents_swipe";
    private static final String PREF_SYSTEMUI_WEATHER_HEADER_VIEW = "cfx_systemui_header_weather_view";
    private static final String PREF_SYSTEMUI_WEATHER_NOTIFICATION = "cfx_weather_notification";

    private ListPreference mClearAll;
    private CheckBoxPreference mRecentsCustom;
    private CheckBoxPreference mRecentPanelLeftyMode;
    private ListPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;
    private CheckBoxPreference mRecentsShowTopmost;
    private ListPreference mRamCircle;
    private CheckBoxPreference mRecentsSwipe;

    private CheckBoxPreference mWeather;
    private CheckBoxPreference mWeatherNotification;

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

        // RAM circle
        mRamCircle = (ListPreference) prefSet.findPreference(RAM_CIRCLE);
        int circleStatus = Settings.System.getInt(resolver,
                Settings.System.RAM_CIRCLE, 0);
        mRamCircle.setValue(String.valueOf(circleStatus));
        mRamCircle.setSummary(mRamCircle.getEntry());
        mRamCircle.setOnPreferenceChangeListener(this);

        // Slim's recents
        boolean enableRecentsCustom = Settings.AOKP.getBoolean(getContentResolver(),
                Settings.System.CUSTOM_RECENT, false);
        mRecentsCustom = (CheckBoxPreference) prefSet.findPreference(CUSTOM_RECENT_MODE);
        mRecentsCustom.setChecked(enableRecentsCustom);
        mRecentsCustom.setOnPreferenceChangeListener(this);

        boolean recentLeftyMode = Settings.System.getInt(resolver,
                Settings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
        mRecentPanelLeftyMode =
                (CheckBoxPreference) prefSet.findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setChecked(recentLeftyMode);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);

        int recentScale = Settings.System.getInt(resolver,
                Settings.System.RECENT_PANEL_SCALE_FACTOR, 100);
        mRecentPanelScale = (ListPreference) prefSet.findPreference(RECENT_PANEL_SCALE);
        mRecentPanelScale.setValue(recentScale + "");
        mRecentPanelScale.setOnPreferenceChangeListener(this);

        int recentExpandedMode = Settings.System.getInt(resolver,
                Settings.System.RECENT_PANEL_EXPANDED_MODE, 0);
        mRecentPanelExpandedMode =
                (ListPreference) prefSet.findPreference(RECENT_PANEL_EXPANDED_MODE);
        mRecentPanelExpandedMode.setValue(recentExpandedMode + "");
        mRecentPanelExpandedMode.setOnPreferenceChangeListener(this);

        boolean enableRecentsShowTopmost = Settings.System.getInt(resolver,
                Settings.System.RECENT_PANEL_SHOW_TOPMOST, 0) == 1;
        mRecentsShowTopmost = (CheckBoxPreference) prefSet.findPreference(RECENT_PANEL_SHOW_TOPMOST);
        mRecentsShowTopmost.setChecked(enableRecentsShowTopmost);
        mRecentsShowTopmost.setOnPreferenceChangeListener(this);

        // Recents swipe
        mRecentsSwipe = (CheckBoxPreference) prefSet.findPreference(PREF_RECENTS_SWIPE_FLOATING);
        mRecentsSwipe.setChecked((Settings.System.getInt(resolver,
                Settings.System.RECENTS_SWIPE_FLOATING, 0) == 1));


        boolean enableWeather = Settings.AOKP.getBoolean(getContentResolver(),
                Settings.System.SYSTEMUI_WEATHER_HEADER_VIEW, false);
        mWeather = (CheckBoxPreference) prefSet.findPreference(PREF_SYSTEMUI_WEATHER_HEADER_VIEW);
        mWeather.setChecked(enableWeather);
        mWeather.setOnPreferenceChangeListener(this);

        boolean enableWeatherNotification = Settings.AOKP.getBoolean(getContentResolver(),
                Settings.System.SYSTEMUI_WEATHER_NOTIFICATION, false);
        mWeatherNotification = (CheckBoxPreference) prefSet.findPreference(PREF_SYSTEMUI_WEATHER_NOTIFICATION);
        mWeatherNotification.setChecked(enableWeatherNotification);
        mWeatherNotification.setOnPreferenceChangeListener(this);

    }
       

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mRecentsSwipe) {
            value = mRecentsSwipe.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.RECENTS_SWIPE_FLOATING, value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

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
        } else if (preference == mRamCircle) {
            int value = Integer.valueOf((String) objValue);
            int index = mRamCircle.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.RAM_CIRCLE, value);
            mRamCircle.setSummary(mRamCircle.getEntries()[index]);
        } else if (preference == mRecentPanelExpandedMode) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.RECENT_PANEL_EXPANDED_MODE, value);
        } else if (preference == mRecentsShowTopmost) {
            Settings.System.putInt(resolver,
                    Settings.System.RECENT_PANEL_SHOW_TOPMOST,
                    ((Boolean) objValue) ? 1 : 0);
        } else if (preference == mWeather) {
            Settings.AOKP.putBoolean(resolver,
                    Settings.System.SYSTEMUI_WEATHER_HEADER_VIEW,
                    ((Boolean) objValue) ? true : false);
            Helpers.restartSystemUI();
        } else if (preference == mWeatherNotification) {
            Settings.AOKP.putBoolean(resolver,
                    Settings.System.SYSTEMUI_WEATHER_NOTIFICATION,
                    ((Boolean) objValue) ? true : false);
            Helpers.restartSystemUI();
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}
