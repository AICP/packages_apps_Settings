/*
 *  Copyright (C) 2014 The Dirty Unicorns Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.settings.aicp;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.util.Helpers;
import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBarColor extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "StatusBarColor";

    private static final String PREF_CUSTOM_STATUS_BAR_COLOR = "custom_status_bar_color";
    private static final String PREF_STATUS_BAR_OPAQUE_COLOR = "status_bar_opaque_color";
//    private static final String PREF_STATUS_BAR_SEMI_TRANS_COLOR = "status_bar_trans_color";
    private static final String PREF_CUSTOM_SYSTEM_ICON_COLOR = "custom_system_icon_color";
    private static final String PREF_SYSTEM_ICON_COLOR = "system_icon_color";

    private CheckBoxPreference mCustomBarColor;
    private ColorPickerPreference mBarOpaqueColor;
//    private ColorPickerPreference mBarTransColor;
    private CheckBoxPreference mCustomIconColor;
    private ColorPickerPreference mIconColor;
    private Preference mApplyCustomBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.aicp_statusbarcolor);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();


        int intColor;
        String hexColor;


        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return;
        }

        mCustomBarColor = (CheckBoxPreference) findPreference(PREF_CUSTOM_STATUS_BAR_COLOR);
        mCustomBarColor.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.CUSTOM_STATUS_BAR_COLOR, 0) == 1);

        mCustomIconColor = (CheckBoxPreference) findPreference(PREF_CUSTOM_SYSTEM_ICON_COLOR);
        mCustomIconColor.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.CUSTOM_SYSTEM_ICON_COLOR, 0) == 1);

        mApplyCustomBar = (Preference) findPreference("custom_status_bar_apply");

        mBarOpaqueColor = (ColorPickerPreference) findPreference(PREF_STATUS_BAR_OPAQUE_COLOR);
        mBarOpaqueColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.STATUS_BAR_OPAQUE_COLOR, 0xff000000);
        mBarOpaqueColor.setSummary(getResources().getString(R.string.default_string));
        if (intColor == 0xff000000) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/system_bar_background_opaque", null, null));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mBarOpaqueColor.setSummary(hexColor);
        }
        mBarOpaqueColor.setNewPreviewColor(intColor);

//        mBarTransColor = (ColorPickerPreference) findPreference(PREF_STATUS_BAR_SEMI_TRANS_COLOR);
//        mBarTransColor.setOnPreferenceChangeListener(this);
//        intColor = Settings.System.getInt(getActivity().getContentResolver(),
//                    Settings.System.STATUS_BAR_SEMI_TRANS_COLOR, 0x66000000);
//        mBarTransColor.setSummary(getResources().getString(R.string.default_string));
//        if (intColor == 0xff000000) {
//            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
//                    "com.android.systemui:color/system_bar_background_semi_transparent", null, null));
//        } else {
//            hexColor = String.format("#%08x", (0x66ffffff & intColor));
//            mBarTransColor.setSummary(hexColor);
//        }
//        mBarTransColor.setNewPreviewColor(intColor);

        mIconColor = (ColorPickerPreference) findPreference(PREF_SYSTEM_ICON_COLOR);
        mIconColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.SYSTEM_ICON_COLOR, -1);
        mIconColor.setSummary(getResources().getString(R.string.default_string));
        if (intColor == 0xffffffff) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/status_bar_clock_color", null, null));
        } else {
           hexColor = String.format("#%08x", (0xffffffff & intColor));
            mIconColor.setSummary(hexColor);
        }
        mIconColor.setNewPreviewColor(intColor);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mCustomBarColor) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CUSTOM_STATUS_BAR_COLOR,
            mCustomBarColor.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mCustomIconColor) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CUSTOM_SYSTEM_ICON_COLOR,
            mCustomIconColor.isChecked() ? 1 : 0);
           return true;
        } else if (preference == mApplyCustomBar) {
           Helpers.restartSystemUI();
           return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mBarOpaqueColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_OPAQUE_COLOR, intHex);
        } else if (preference == mIconColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_ICON_COLOR, intHex);
//        } else if (preference == mBarTransColor) {
//            String hex = ColorPickerPreference.convertToARGB(Integer
//                    .valueOf(String.valueOf(objValue)));
//            preference.setSummary(hex);
//            int intHex = ColorPickerPreference.convertToColorInt(hex);
//            Settings.System.putInt(getActivity().getContentResolver(),
//                    Settings.System.STATUS_BAR_SEMI_TRANS_COLOR, intHex);
        } else {
            return false;
        }

        return true;
    }
}
