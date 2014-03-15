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
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.WindowManagerGlobal;
import android.util.Log;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

public class Powermenu extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "PowermenuSettings";

    private static final String KEY_ENABLE_POWER_MENU = "lockscreen_enable_power_menu";
    private static final String KEY_EXPANDED_DESKTOP = "power_menu_expanded_desktop";
    private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "power_menu_expanded_desktop_no_navbar";
    private static final String POWER_MENU_ONTHEGO_ENABLED = "power_menu_onthego_enabled";

    private CheckBoxPreference mEnablePowerMenu;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private CheckBoxPreference mOnTheGoPowerMenu;
    private ListPreference mExpandedDesktopPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.aicp_powermenu);

        PreferenceScreen prefSet = getPreferenceScreen();

        // Enable powermenu
        mEnablePowerMenu = (CheckBoxPreference) prefSet.findPreference(KEY_ENABLE_POWER_MENU);
        mEnablePowerMenu.setChecked(Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_ENABLE_POWER_MENU, 1) == 1);
        mEnablePowerMenu.setOnPreferenceChangeListener(this);

        // Expanded desktop
        mExpandedDesktopPref = (ListPreference) prefSet.findPreference(KEY_EXPANDED_DESKTOP);
        mExpandedDesktopNoNavbarPref = (CheckBoxPreference) prefSet.findPreference(KEY_EXPANDED_DESKTOP_NO_NAVBAR);
        int expandedDesktopValue = Settings.System.getInt(resolver,
                Settings.System.EXPANDED_DESKTOP_STYLE, 0);
        // Hide no-op "Status bar visible" mode on devices without navbar
        try {
            if (WindowManagerGlobal.getWindowManagerService().hasNavigationBar()) {
                mExpandedDesktopPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
                updateExpandedDesktop(expandedDesktopValue);

                prefSet.removePreference(mExpandedDesktopNoNavbarPref);
            } else {
                mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);

                prefSet.removePreference(mExpandedDesktopPref);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

        // On the go
        mOnTheGoPowerMenu = (CheckBoxPreference) prefSet.findPreference(POWER_MENU_ONTHEGO_ENABLED);
        mOnTheGoPowerMenu.setChecked(Settings.System.getInt(resolver,
        Settings.System.POWER_MENU_ONTHEGO_ENABLED, 0) == 1);
        mOnTheGoPowerMenu.setOnPreferenceChangeListener(this);
    }
       

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mEnablePowerMenu) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_ENABLE_POWER_MENU,
                    mEnablePowerMenu.isChecked() ? 1: 0);
        } else if (preference == mExpandedDesktopNoNavbarPref) {
            value = mExpandedDesktopNoNavbarPref.isChecked();
            updateExpandedDesktop(value ? 2 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.EXPANDED_DESKTOP_STYLE, expandedDesktopValue);
            Settings.System.putInt(resolver,
                    Settings.System.EXPANDED_DESKTOP_STYLE_TEMP, expandedDesktopValue);
            updateExpandedDesktop(expandedDesktopValue);
        } else if (preference == mOnTheGoPowerMenu) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                    Settings.System.POWER_MENU_ONTHEGO_ENABLED, value ? 1 : 0);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    private void updateExpandedDesktop(int value) {
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(getContentResolver(),
                Settings.System.EXPANDED_DESKTOP_STYLE, value);

        if (value == 0) {
            // Expanded desktop deactivated
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            summary = R.string.expanded_desktop_disabled;
            // Disable expanded desktop if enabled
            Settings.System.putInt(getContentResolver(),
                    Settings.System.EXPANDED_DESKTOP_STATE, 0);
        } else if (value == 1) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_status_bar;
        } else if (value == 2) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_no_status_bar;
        }

        if (mExpandedDesktopPref != null && summary != -1) {
            mExpandedDesktopPref.setSummary(res.getString(summary));
        }
    }
}
