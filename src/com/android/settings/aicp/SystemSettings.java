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
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import com.android.settings.widget.SeekBarPreferenceCham;
import com.android.settings.widget.SeekBarPreferenceGlow;

import java.io.File;
import java.lang.Thread;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;

public class SystemSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "SystemSettings";

    private static final String CATEGORY_SYSTEM = "system_category";
    private static final String CATEGORY_NAVBAR = "navbar_category";

    private static final String KEY_NAVBAR_LEFT_IN_LANDSCAPE = "navigation_bar_left";
    private static final String DISABLE_FC_NOTIFICATIONS = "disable_fc_notifications";
    private static final String KEY_DONT_SHOW_NAVBAR_ON_SWIPE_EXPANDED_DESKTOP_ENABLED =
            "dont_show_navbar_on_swipe_expanded_desktop_enabled";
    private static final String KEY_NAVIGATION_MENU_FORCE = "navigation_menu_force";
    private static final String PREF_PROXIMITY_ON_WAKE = "proximity_on_wake";
    private static final String DISABLE_BOOTAUDIO = "disable_bootaudio";
    private static final String KEY_POWER_MENU_QUICKCAM = "power_menu_quickcam";
    private static final String NAVIGATION_BUTTON_GLOW_TIME = "navigation_button_glow_time";

    private CheckBoxPreference mDisableFC;
    private CheckBoxPreference mHfmDisableAds;
    private CheckBoxPreference mNavigationBarLeft;
    private CheckBoxPreference mDontShowNavbar;
    private CheckBoxPreference mNavigationMenuForce;
    private CheckBoxPreference mProximityWake;
    private CheckBoxPreference mDisableBootAudio;
    private CheckBoxPreference mPowerMenuQuickcam;

    private ListPreference mNavigationMenu;

    private SeekBarPreferenceGlow mNavigationButtonGlowTime;

    private PreferenceScreen mSystemScreen;
    private PreferenceCategory mNavbarCategory;
    private PreferenceCategory mSystemCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();

        addPreferencesFromResource(R.xml.aicp_system);

        PreferenceScreen prefSet = getPreferenceScreen();

        mSystemScreen = (PreferenceScreen) findPreference("system_screen");
        mNavbarCategory = (PreferenceCategory) findPreference("navbar_category");
        mSystemCategory = (PreferenceCategory) findPreference("system_category");

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

        // Navbar glow time
        mNavigationButtonGlowTime = (SeekBarPreferenceGlow) prefSet.findPreference(NAVIGATION_BUTTON_GLOW_TIME);
        mNavigationButtonGlowTime.setDefault(Settings.System.getInt(resolver,
                Settings.System.NAVIGATION_BUTTON_GLOW_TIME, 500));
        mNavigationButtonGlowTime.setOnPreferenceChangeListener(this);

        // Navigation mnenu force
        mNavigationMenuForce = (CheckBoxPreference) prefSet.findPreference(KEY_NAVIGATION_MENU_FORCE);
        mNavigationMenuForce.setChecked((Settings.System.getInt(resolver,
                Settings.System.NAVIGATION_MENU_FORCE, 0) == 1));

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

        // Proximity wake up
        mProximityWake = (CheckBoxPreference) prefSet.findPreference(PREF_PROXIMITY_ON_WAKE);
        mProximityWake.setChecked((Settings.System.getInt(resolver,
                Settings.System.PROXIMITY_ON_WAKE, 0) == 1));

        boolean proximityCheckOnWait = res.getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        PreferenceCategory sysCategory = (PreferenceCategory) findPreference(CATEGORY_SYSTEM);

        if (!proximityCheckOnWait) {
            sysCategory.removePreference(mProximityWake);
            Settings.System.putInt(getContentResolver(), Settings.System.PROXIMITY_ON_WAKE, 1);
        }

        // Boot audio
        mDisableBootAudio = (CheckBoxPreference) findPreference("disable_bootaudio");

        if(!new File("/system/media/audio.mp3").exists() &&
                !new File("/system/media/boot_audio").exists() ) {
            mDisableBootAudio.setEnabled(false);
            mDisableBootAudio.setSummary(R.string.disable_bootaudio_summary_disabled);
        } else {
            mDisableBootAudio.setChecked(!new File("/system/media/audio.mp3").exists());
            if (mDisableBootAudio.isChecked())
                mDisableBootAudio.setSummary(R.string.disable_bootaudio_summary);
        }

        // Power menu quickcam
        mPowerMenuQuickcam = (CheckBoxPreference) prefSet.findPreference(KEY_POWER_MENU_QUICKCAM);
        mPowerMenuQuickcam.setChecked((Settings.System.getInt(resolver,
                Settings.System.POWER_MENU_QUICKCAM, 0) == 1));

    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mNavigationBarLeft) {
            value = mNavigationBarLeft.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.NAVBAR_LEFT_IN_LANDSCAPE, value ? 1 : 0);
        } else if  (preference == mDisableFC) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.DISABLE_FC_NOTIFICATIONS, checked ? 1 : 0);
        } else if  (preference == mDontShowNavbar) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.DONT_SHOW_NAVBAR_ON_SWIPE_EXPANDED_DESKTOP_ENABLED, checked ? 1 : 0);
        } else if (preference == mNavigationMenuForce) {
            value = mNavigationMenuForce.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.NAVIGATION_MENU_FORCE, value ? 1 : 0);
            if (!value) {
                Settings.System.putInt(resolver,
                        Settings.System.NAVIGATION_MENU, 0);
            }
        } else if  (preference == mProximityWake) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.PROXIMITY_ON_WAKE, checked ? 1 : 0);
        } else if (preference == mDisableBootAudio) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            if (checked) {
                Helpers.getMount("rw");
                CMDProcessor.runSuCommand(
                        "mv /system/media/audio.mp3 /system/media/boot_audio");
                Helpers.getMount("ro");
                preference.setSummary(R.string.disable_bootaudio_summary);
            } else {
                Helpers.getMount("rw");
                CMDProcessor.runSuCommand(
                        "mv /system/media/boot_audio /system/media/audio.mp3");
                Helpers.getMount("ro");
            }
        } else if (preference == mPowerMenuQuickcam) {
            value = mPowerMenuQuickcam.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.POWER_MENU_QUICKCAM, value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
	if (preference == mNavigationButtonGlowTime) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BUTTON_GLOW_TIME, (Integer)objValue);
        }
        return true;
     }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

}
