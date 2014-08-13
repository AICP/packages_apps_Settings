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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

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
    private static final String RECENT_PANEL_BG_COLOR =
            "recent_panel_bg_color";

    private static final String RAM_CIRCLE = "ram_circle";
    private static final String PREF_RECENTS_SWIPE_FLOATING = "recents_swipe";

    private ListPreference mClearAll;
    private CheckBoxPreference mRecentsCustom;
    private CheckBoxPreference mRecentPanelLeftyMode;
    private ListPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;
    private CheckBoxPreference mRecentsShowTopmost;
    private ListPreference mRamCircle;
    private CheckBoxPreference mRecentsSwipe;
    private ColorPickerPreference mRecentPanelBgColor;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x00ffffff;

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

        // Recent panel background color
        mRecentPanelBgColor =
                (ColorPickerPreference) findPreference(RECENT_PANEL_BG_COLOR);
        mRecentPanelBgColor.setOnPreferenceChangeListener(this);
        final int intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_BG_COLOR, 0x00ffffff);
        String hexColor = String.format("#%08x", (0x00ffffff & intColor));
        if (hexColor.equals("#00ffffff")) {
            mRecentPanelBgColor.setSummary("TRDS default");
        } else {
            mRecentPanelBgColor.setSummary(hexColor);
        }
            mRecentPanelBgColor.setNewPreviewColor(intColor);
            setHasOptionsMenu(true);

        // Recents swipe
        mRecentsSwipe = (CheckBoxPreference) prefSet.findPreference(PREF_RECENTS_SWIPE_FLOATING);
        mRecentsSwipe.setChecked((Settings.System.getInt(resolver,
                Settings.System.RECENTS_SWIPE_FLOATING, 0) == 1));

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
        } else if (preference == mRecentPanelBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
            Integer.valueOf(String.valueOf(objValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary("TRDS default");
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_BG_COLOR,
                    intHex);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset_default_message)
                .setIcon(R.drawable.ic_settings_backup)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.reset);
        alertDialog.setMessage(R.string.qs_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.RECENT_PANEL_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
        mRecentPanelBgColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
        mRecentPanelBgColor.setSummary("TRDS default");
    }

}
