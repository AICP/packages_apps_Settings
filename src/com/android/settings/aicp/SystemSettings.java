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
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Camera;
import android.media.Ringtone;
import android.media.RingtoneManager;
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
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.hfm.HfmHelpers;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

public class SystemSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "SystemSettings";

    private static final String CATEGORY_SYSTEM = "system_category";
    private static final String CATEGORY_NAVBAR = "navbar_category";
    private static final String HFM_DISABLE_ADS = "hfm_disable_ads";
    private static final String KEY_NAVBAR_LEFT_IN_LANDSCAPE = "navigation_bar_left";
    private static final String DISABLE_FC_NOTIFICATIONS = "disable_fc_notifications";
    private static final String KEY_DONT_SHOW_NAVBAR_ON_SWIPE_EXPANDED_DESKTOP_ENABLED =
            "dont_show_navbar_on_swipe_expanded_desktop_enabled";
    private static final String KEY_NAVIGATION_MENU = "navigation_menu";
    private static final String KEY_NAVIGATION_MENU_FORCE = "navigation_menu_force";

    private CheckBoxPreference mDisableFC;
    private CheckBoxPreference mHfmDisableAds;
    private CheckBoxPreference mNavigationBarLeft;
    private CheckBoxPreference mDontShowNavbar;
    private CheckBoxPreference mNavigationMenuForce;
    private ListPreference mNavigationMenu;

    private PreferenceScreen mSystemScreen;
    private PreferenceCategory mNavbarCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.aicp_system);

        PreferenceScreen prefSet = getPreferenceScreen();

        mSystemScreen = (PreferenceScreen) findPreference("system_screen");
        mNavbarCategory = (PreferenceCategory) findPreference("navbar_category");

        // Disable ads
        mHfmDisableAds = (CheckBoxPreference) prefSet.findPreference(HFM_DISABLE_ADS);
        mHfmDisableAds.setChecked((Settings.System.getInt(resolver,
                Settings.System.HFM_DISABLE_ADS, 0) == 1));

        // Navigation bar left
        mNavigationBarLeft = (CheckBoxPreference) prefSet.findPreference(KEY_NAVBAR_LEFT_IN_LANDSCAPE);
        mNavigationBarLeft.setChecked((Settings.System.getInt(resolver,
                Settings.System.NAVBAR_LEFT_IN_LANDSCAPE, 0) == 1));

        // Disable FC notification
        mDisableFC = (CheckBoxPreference) prefSet.findPreference(DISABLE_FC_NOTIFICATIONS);
        mDisableFC.setChecked((Settings.System.getInt(resolver,
                Settings.System.DISABLE_FC_NOTIFICATIONS, 0) == 1));

        // Don't show navbar on swipe up while in expanded mode
        mDontShowNavbar = (CheckBoxPreference) prefSet.findPreference(
                KEY_DONT_SHOW_NAVBAR_ON_SWIPE_EXPANDED_DESKTOP_ENABLED);
        mDontShowNavbar.setChecked((Settings.System.getInt(resolver,
                Settings.System.DONT_SHOW_NAVBAR_ON_SWIPE_EXPANDED_DESKTOP_ENABLED, 0) == 1));

        // Navigation mnenu force
        mNavigationMenuForce = (CheckBoxPreference) prefSet.findPreference(KEY_NAVIGATION_MENU_FORCE);
        mNavigationMenuForce.setChecked((Settings.System.getInt(resolver,
                Settings.System.NAVIGATION_MENU_FORCE, 0) == 1));

        // Navigation menu
        mNavigationMenu = (ListPreference) prefSet.findPreference(KEY_NAVIGATION_MENU);
        mNavigationMenu.setOnPreferenceChangeListener(this);
        mNavigationMenu.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.NAVIGATION_MENU, 0)));
        mNavigationMenu.setSummary(mNavigationMenu.getEntry());


        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
            PreferenceCategory navCategory = (PreferenceCategory) findPreference(CATEGORY_NAVBAR);

            if (hasNavBar) {
                if (!Utils.isPhone(getActivity())) {
                    navCategory.removePreference(mNavigationBarLeft);
                }
            } else {
                mSystemScreen.removePreference(mNavbarCategory);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

    }
       

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if  (preference == mHfmDisableAds) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.HFM_DISABLE_ADS, checked ? 1:0);
            HfmHelpers.checkStatus(getActivity());
        } else if (preference == mNavigationBarLeft) {
            value = mNavigationBarLeft.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.NAVBAR_LEFT_IN_LANDSCAPE, value ? 1 : 0);
        } else if  (preference == mDisableFC) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.DISABLE_FC_NOTIFICATIONS, checked ? 1:0);
        } else if  (preference == mDontShowNavbar) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.DONT_SHOW_NAVBAR_ON_SWIPE_EXPANDED_DESKTOP_ENABLED, checked ? 1:0);
        } else if (preference == mNavigationMenuForce) {
            value = mNavigationMenuForce.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.NAVIGATION_MENU_FORCE, value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (preference == mNavigationMenu) {
            int val = Integer.parseInt((String) objValue);
            int index = mNavigationMenu.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.NAVIGATION_MENU, val);
            mNavigationMenu.setSummary(mNavigationMenu.getEntries()[index]);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}
