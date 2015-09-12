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

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class LockScreenWeatherSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String PREF_CAT_NOTIFICATIONS =
            "weather_cat_notifications";
    private static final String PREF_SHOW_WEATHER =
            "weather_show_weather";
    private static final String PREF_SHOW_LOCATION =
            "weather_show_location";
    private static final String PREF_SHOW_TIMESTAMP =
            "weather_show_timestamp";
    private static final String PREF_CONDITION_ICON =
            "weather_condition_icon";
    private static final String PREF_HIDE_WEATHER =
            "weather_hide_panel";
    private static final String PREF_NUMBER_OF_NOTIFICATIONS =
            "weather_number_of_notifications";
    private static final String PREF_TEXT_COLOR =
	        "weather_text_color";
    private static final String PREF_ICON_COLOR =
	        "weather_icon_color";

    private static final int MONOCHROME_ICON = 0;
	private static final int DEFAULT_COLOR = 0xffffffff;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    private CheckBoxPreference mShowWeather;
    private CheckBoxPreference mShowLocation;
    private CheckBoxPreference mShowTimestamp;
    private ListPreference mConditionIcon;
    private ColorPickerPreference mTextColor;
    private ColorPickerPreference mIconColor;
    private ListPreference mHideWeather;
    private ListPreference mNumberOfNotifications;

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

        int intColor;
        String hexColor;

        mShowWeather =
                (CheckBoxPreference) findPreference(PREF_SHOW_WEATHER);
        mShowWeather.setChecked(showWeather);
        mShowWeather.setOnPreferenceChangeListener(this);

        PreferenceCategory catNotifications =
                (PreferenceCategory) findPreference(PREF_CAT_NOTIFICATIONS);
        mHideWeather =
                (ListPreference) findPreference(PREF_HIDE_WEATHER);
        mNumberOfNotifications =
                (ListPreference) findPreference(PREF_NUMBER_OF_NOTIFICATIONS);

        mTextColor = (ColorPickerPreference) findPreference(PREF_TEXT_COLOR);
        mIconColor = (ColorPickerPreference) findPreference(PREF_ICON_COLOR);

        if (showWeather) {
            mShowLocation =
                    (CheckBoxPreference) findPreference(PREF_SHOW_LOCATION);
            mShowLocation.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 1) == 1);
            mShowLocation.setOnPreferenceChangeListener(this);

            mShowTimestamp =
                    (CheckBoxPreference) findPreference(PREF_SHOW_TIMESTAMP);
            mShowTimestamp.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_TIMESTAMP, 0) == 1);
            mShowTimestamp.setOnPreferenceChangeListener(this);

            mConditionIcon =
                    (ListPreference) findPreference(PREF_CONDITION_ICON);
            int conditionIcon = Settings.System.getInt(mResolver,
                   Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, MONOCHROME_ICON);
            mConditionIcon.setValue(String.valueOf(conditionIcon));
            mConditionIcon.setSummary(mConditionIcon.getEntry());
            mConditionIcon.setOnPreferenceChangeListener(this);

            intColor = Settings.System.getInt(mResolver,
                Settings.System.LOCK_SCREEN_WEATHER_TEXT_COLOR, -2);
            if (intColor == -2) {
                intColor = 0xffffffff;
                mTextColor.setSummary(getResources().getString(R.string.default_string));
            } else {
                hexColor = String.format("#%08x", (0xffffffff & intColor));
                mTextColor.setSummary(hexColor);
            }
            mTextColor.setNewPreviewColor(intColor);
            mTextColor.setOnPreferenceChangeListener(this);

            intColor = Settings.System.getInt(mResolver,
                Settings.System.LOCK_SCREEN_WEATHER_ICON_COLOR, -2);
            if (intColor == -2) {
                intColor = 0xffffffff;
                mIconColor.setSummary(getResources().getString(R.string.default_string));
            } else {
                hexColor = String.format("#%08x", (0xffffffff & intColor));
                mIconColor.setSummary(hexColor);
            }
            mIconColor.setNewPreviewColor(intColor);
            mIconColor.setOnPreferenceChangeListener(this);

            int  hideWeather = Settings.System.getInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, 0);
            mHideWeather.setValue(String.valueOf(hideWeather));
            mHideWeather.setOnPreferenceChangeListener(this);

            if (hideWeather == 0) {
                mHideWeather.setSummary(R.string.weather_hide_panel_auto_summary);
                catNotifications.removePreference(mNumberOfNotifications);
            } else if (hideWeather == 1) {
                int numberOfNotifications = Settings.System.getInt(mResolver,
                       Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS, 6);
                mNumberOfNotifications.setValue(String.valueOf(numberOfNotifications));
                mNumberOfNotifications.setSummary(mNumberOfNotifications.getEntry());
                mNumberOfNotifications.setOnPreferenceChangeListener(this);

                mHideWeather.setSummary(getString(R.string.weather_hide_panel_custom_summary,
                        mNumberOfNotifications.getEntry()));
            } else {
                mHideWeather.setSummary(R.string.weather_hide_panel_never_summary);
                catNotifications.removePreference(mNumberOfNotifications);
            }

        } else {
            removePreference(PREF_SHOW_LOCATION);
            removePreference(PREF_SHOW_TIMESTAMP);
            removePreference(PREF_CONDITION_ICON);
            catNotifications.removePreference(mHideWeather);
            catNotifications.removePreference(mNumberOfNotifications);
            removePreference(PREF_CAT_NOTIFICATIONS);
            removePreference(PREF_TEXT_COLOR);
            removePreference(PREF_ICON_COLOR);
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
        int intValue;
        int index;
        String hex;
        int intHex;

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
            intValue = Integer.valueOf((String) newValue);
            index = mConditionIcon.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, intValue);
            mConditionIcon.setSummary(mConditionIcon.getEntries()[index]);
            refreshSettings();
            return true;
        } else if (preference == mHideWeather) {
            intValue = Integer.valueOf((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, intValue);
            refreshSettings();
            return true;
        } else if (preference == mNumberOfNotifications) {
            intValue = Integer.valueOf((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS, intValue);
            refreshSettings();
            return true;
        } else if (preference == mTextColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_TEXT_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mIconColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_ICON_COLOR, intHex);
            preference.setSummary(hex);
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
                                    Settings.System.LOCK_SCREEN_WEATHER_TEXT_COLOR, -2);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_ICON_COLOR, -2);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS, 6);
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
                                    Settings.System.LOCK_SCREEN_WEATHER_TEXT_COLOR, -2);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_ICON_COLOR, -2);

                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS, 6);
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

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.aicp_lock_screen_weather_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}
