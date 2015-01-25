/*
 * Copyright (C) 2014 The LiquidSmooth Project
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

package com.android.settings.liquid;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.widget.Toast;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class NavigationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "NavigationSettings";

    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String CATEGORY_NAV_BAR_ENABLE = "navigation_bar_enable";

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.liquid_navigation_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        final boolean hasRealNavigationBar = getResources()
                .getBoolean(com.android.internal.R.bool.config_showNavigationBar);
        if (hasRealNavigationBar) { // only disable on devices with REAL navigation bars
            final Preference pref = findPreference(CATEGORY_NAV_BAR_ENABLE);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        // Attach final settings screen.
        reloadSettings();
        }

        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();

            // Hide navigation bar category on devices without navigation bar
            if (!hasNavBar) {
                prefSet.removePreference(findPreference(CATEGORY_NAVBAR));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {

        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
 		return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private PreferenceScreen reloadSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.liquid_navigation_settings);
        prefs = getPreferenceScreen();

        mCheckPreferences = true;
        return prefs;
    }
}
