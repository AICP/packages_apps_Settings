/*
 *  Copyright (C) 2013 The OmniROM Project
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

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.cyanogenmod.SystemSettingCheckBoxPreference;
import com.android.settings.widget.SeekBarPreferenceCham;
import com.android.settings.Utils;

public class TintedStatusBar extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "TintedStatusBar";

    private static final String TINTED_STATUSBAR = "tinted_statusbar";
    private static final String TINTED_STATUSBAR_OPTION = "tinted_statusbar_option";
    private static final String TINTED_STATUSBAR_FILTER = "status_bar_tinted_filter";
    private static final String TINTED_STATUSBAR_TRANSPARENT = "tinted_statusbar_transparent";
    private static final String TINTED_NAVBAR_TRANSPARENT = "tinted_navbar_transparent";
    private static final String TINTED_STATUSBAR_FULL_MODE = "status_bar_tinted_full_mode";
    private static final String CATEGORY_TINTED = "category_tinted_statusbar";

    private ListPreference mTintedStatusbar;
    private ListPreference mTintedStatusbarOption;

    private SystemSettingCheckBoxPreference mTintedStatusbarFilter;
    private SystemSettingCheckBoxPreference mTintedStatusbarFullmode;
    private SeekBarPreferenceCham mTintedStatusbarTransparency;
    private SeekBarPreferenceCham mTintedNavbarTransparency;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.aicp_tintedstatusbar);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        final PreferenceCategory tintedCategory =
                     (PreferenceCategory) prefSet.findPreference(CATEGORY_TINTED);

        mTintedStatusbar = (ListPreference) findPreference(TINTED_STATUSBAR);
        int tintedStatusbar = Settings.System.getInt(resolver,
                    Settings.System.STATUS_BAR_TINTED_COLOR, 0);
        mTintedStatusbar.setValue(String.valueOf(tintedStatusbar));
        mTintedStatusbar.setSummary(mTintedStatusbar.getEntry());
        mTintedStatusbar.setOnPreferenceChangeListener(this);

        mTintedStatusbarFilter = (SystemSettingCheckBoxPreference) findPreference(TINTED_STATUSBAR_FILTER);
        mTintedStatusbarFilter.setEnabled(tintedStatusbar != 0);

        mTintedStatusbarFullmode = (SystemSettingCheckBoxPreference)
                findPreference(TINTED_STATUSBAR_FULL_MODE);
        mTintedStatusbarFullmode.setEnabled(tintedStatusbar != 0);

        mTintedStatusbarTransparency = (SeekBarPreferenceCham) findPreference(TINTED_STATUSBAR_TRANSPARENT);
        mTintedStatusbarTransparency.setValue(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_TINTED_STATBAR_TRANSPARENT, 100));
        mTintedStatusbarTransparency.setEnabled(tintedStatusbar != 0);
        mTintedStatusbarTransparency.setOnPreferenceChangeListener(this);

        mTintedStatusbarOption = (ListPreference) findPreference(TINTED_STATUSBAR_OPTION);

        mTintedNavbarTransparency = (SeekBarPreferenceCham) findPreference(TINTED_NAVBAR_TRANSPARENT);
        mTintedNavbarTransparency.setValue(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_TINTED_NAVBAR_TRANSPARENT, 100));
        mTintedNavbarTransparency.setEnabled(tintedStatusbar != 0);
        mTintedNavbarTransparency.setOnPreferenceChangeListener(this);

        int tintedStatusbarOption = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_TINTED_OPTION, 0);
        mTintedStatusbarOption.setValue(String.valueOf(tintedStatusbarOption));
        mTintedStatusbarOption.setSummary(mTintedStatusbarOption.getEntry());
        mTintedStatusbarOption.setEnabled(tintedStatusbar != 0);
        mTintedStatusbarOption.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mTintedStatusbar) {
            int val = Integer.parseInt((String) objValue);
            int index = mTintedStatusbar.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_TINTED_COLOR, val);
            mTintedStatusbar.setSummary(mTintedStatusbar.getEntries()[index]);
            if (mTintedStatusbarOption != null) {
                mTintedStatusbarOption.setEnabled(val != 0);
            }
            mTintedStatusbarFilter.setEnabled(val != 0);
            mTintedStatusbarTransparency.setEnabled(val != 0);
            mTintedStatusbarFullmode.setEnabled(val != 0);
            if (mTintedNavbarTransparency != null) {
                mTintedNavbarTransparency.setEnabled(val != 0);
            }
        } else if (preference == mTintedStatusbarOption) {
            int val = Integer.parseInt((String) objValue);
            int index = mTintedStatusbarOption.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_TINTED_OPTION, val);
            mTintedStatusbarOption.setSummary(mTintedStatusbarOption.getEntries()[index]);
            mTintedStatusbarFullmode.setEnabled((val == 0) || (val == 2));
        } else if (preference == mTintedStatusbarTransparency) {
            int val = ((Integer)objValue).intValue();
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_TINTED_STATBAR_TRANSPARENT, val);
            return true;
        } else if (preference == mTintedNavbarTransparency) {
            int val = ((Integer)objValue).intValue();
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_TINTED_NAVBAR_TRANSPARENT, val);
            return true;
        } else {
            return false;
        }
        return true;
    }
}

