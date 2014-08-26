/*
 * Copyright (C) 2012 ParanoidAndroid Project
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

package com.android.settings.paranoid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PieTargets extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String PIE_MENU = "pie_menu";
    private static final String PIE_SEARCH = "pie_search";
    private static final String PIE_LASTAPP = "pie_lastapp";
    private static final String PIE_KILLTASK = "pie_killtask";
    private static final String PIE_APPWINDOW = "pie_appwindow";
    private static final String PIE_ACTNOTIF = "pie_actnotif";
    private static final String PIE_POWER = "pie_power";
    private static final String PIE_TORCH = "pie_torch";
    private static final String PIE_SCREENSHOT = "pie_screenshot";

    private CheckBoxPreference mPieMenu;
    private CheckBoxPreference mPieSearch;
    private CheckBoxPreference mPieLastApp;
    private CheckBoxPreference mPieKillTask;
    private CheckBoxPreference mPieAppWindow;
    private CheckBoxPreference mPieActNotif;
    private CheckBoxPreference mPiePower;
    private CheckBoxPreference mPieTorch;
    private CheckBoxPreference mPieScreenshot;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.aicp_pie_targets);

        PreferenceScreen prefSet = getPreferenceScreen();

        Context context = getActivity();
        mResolver = context.getContentResolver();

        mPieMenu = (CheckBoxPreference) prefSet.findPreference(PIE_MENU);
        mPieMenu.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_MENU, 1) != 0);

        mPieSearch = (CheckBoxPreference) prefSet.findPreference(PIE_SEARCH);
        mPieSearch.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_SEARCH, 1) != 0);

        mPieLastApp = (CheckBoxPreference) prefSet.findPreference(PIE_LASTAPP);
        mPieLastApp.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_LAST_APP, 0) != 0);

        mPieKillTask = (CheckBoxPreference) prefSet.findPreference(PIE_KILLTASK);
        mPieKillTask.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_KILL_TASK, 0) != 0);

        mPieAppWindow = (CheckBoxPreference) prefSet.findPreference(PIE_APPWINDOW);
        mPieAppWindow.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_APP_WINDOW, 0) != 0);

        mPieActNotif = (CheckBoxPreference) prefSet.findPreference(PIE_ACTNOTIF);
        mPieActNotif.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_ACT_NOTIF, 0) != 0);

        mPiePower = (CheckBoxPreference) prefSet.findPreference(PIE_POWER);
        mPiePower.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_POWER, 0) != 0);

        mPieTorch = (CheckBoxPreference) prefSet.findPreference(PIE_TORCH);
        mPieTorch.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_TORCH, 0) != 0);

        mPieScreenshot = (CheckBoxPreference) prefSet.findPreference(PIE_SCREENSHOT);
        mPieScreenshot.setChecked(Settings.System.getInt(mResolver,
                Settings.System.PIE_SCREENSHOT, 0) != 0);

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPieMenu) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_MENU,
                    mPieMenu.isChecked() ? 1 : 0);
        } else if (preference == mPieSearch) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_SEARCH,
                    mPieSearch.isChecked() ? 1 : 0);
        } else if (preference == mPieLastApp) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_LAST_APP,
                    mPieLastApp.isChecked() ? 1 : 0);
        } else if (preference == mPieKillTask) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_KILL_TASK,
                    mPieKillTask.isChecked() ? 1 : 0);
        } else if (preference == mPieAppWindow) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_APP_WINDOW,
                    mPieAppWindow.isChecked() ? 1 : 0);
        } else if (preference == mPieActNotif) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_ACT_NOTIF,
                    mPieActNotif.isChecked() ? 1 : 0);
        } else if (preference == mPiePower) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_POWER,
                    mPiePower.isChecked() ? 1 : 0);
        } else if (preference == mPieTorch) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_TORCH,
                    mPieTorch.isChecked() ? 1 : 0);
        } else if (preference == mPieScreenshot) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PIE_SCREENSHOT,
                    mPieScreenshot.isChecked() ? 1 : 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
