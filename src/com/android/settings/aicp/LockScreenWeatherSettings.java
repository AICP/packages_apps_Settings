/* 
 * Copyright (C) 2015 DarkKat
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
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class LockScreenWeatherSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_SHOW_WEATHER =
            "weather_show_weather";
    private static final String PREF_SHOW_LOCATION =
            "weather_show_location";
    private static final String PREF_SHOW_TIMESTAMP =
            "weather_show_timestamp";
    private static final String PREF_CONDITION_ICON =
            "weather_condition_icon";
    private static final String PREF_COLORIZE_ALL_ICONS =
            "weather_colorize_all_icons";

    private static final int MONOCHROME_ICON = 0;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    private CheckBoxPreference mShowWeather;
    private CheckBoxPreference mShowLocation;
    private CheckBoxPreference mShowTimestamp;
    private ListPreference mConditionIcon;
    private CheckBoxPreference mColorizeAllIcons;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    public void refreshSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.aicp_lock_screen_weather_settings);
        mResolver = getActivity().getContentResolver();

        boolean showWeather = Settings.System.getInt(mResolver,
                Settings.System.LOCK_SCREEN_SHOW_WEATHER, 0) == 1;

        mShowWeather =
                (CheckBoxPreference) findPreference(PREF_SHOW_WEATHER);
        mShowWeather.setChecked(showWeather);
        mShowWeather.setOnPreferenceChangeListener(this);

        if (showWeather) {
            mShowLocation =
                    (CheckBoxPreference) findPreference(PREF_SHOW_LOCATION);
            mShowLocation.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 1) == 1);
            mShowLocation.setOnPreferenceChangeListener(this);

            mShowTimestamp =
                    (CheckBoxPreference) findPreference(PREF_SHOW_TIMESTAMP);
            mShowTimestamp.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_TIMESTAMP, 1) == 1);
            mShowTimestamp.setOnPreferenceChangeListener(this);

            mConditionIcon =
                    (ListPreference) findPreference(PREF_CONDITION_ICON);
            int conditionIcon = Settings.System.getInt(mResolver,
                   Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, MONOCHROME_ICON);
            mConditionIcon.setValue(String.valueOf(conditionIcon));
            mConditionIcon.setSummary(mConditionIcon.getEntry());
            mConditionIcon.setOnPreferenceChangeListener(this);

            mColorizeAllIcons =
                    (CheckBoxPreference) findPreference(PREF_COLORIZE_ALL_ICONS);
            mColorizeAllIcons.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_COLORIZE_ALL_ICONS, 0) == 1);
            mColorizeAllIcons.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_SHOW_LOCATION);
            removePreference(PREF_SHOW_TIMESTAMP);
            removePreference(PREF_CONDITION_ICON);
            removePreference(PREF_COLORIZE_ALL_ICONS);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value;

        if (preference == mShowWeather) {
            value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER,
                    value ? 1 : 0);
            refreshSettings();
            return true;
        } else if (preference == mShowLocation) {
            value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION,
                    value ? 1 : 0);
            return true;
        } else if (preference == mShowTimestamp) {
            value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_TIMESTAMP,
                    value ? 1 : 0);
            return true;
        } else if (preference == mConditionIcon) {
            int intValue = Integer.valueOf((String) newValue);
            int index = mConditionIcon.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, intValue);
            mConditionIcon.setSummary(mConditionIcon.getEntries()[index]);
            return true;
        } else if (preference == mColorizeAllIcons) {
            value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_COLORIZE_ALL_ICONS,
                    value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        LockScreenWeatherSettings getOwner() {
            return (LockScreenWeatherSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.dlg_reset_values_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.dlg_reset_android,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_SHOW_WEATHER, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_TIMESTAMP, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON,
                                    MONOCHROME_ICON);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_COLORIZE_ALL_ICONS, 0);
                            getOwner().refreshSettings();
                        }
                    })
                    .setPositiveButton(R.string.dlg_reset_aicp,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_SHOW_WEATHER, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_TIMESTAMP, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, 2);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_COLORIZE_ALL_ICONS, 1);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}
