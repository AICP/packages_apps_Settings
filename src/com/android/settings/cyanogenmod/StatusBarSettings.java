/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cyanogenmod.providers.CMSettings;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBarSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, Indexable {

    private static final String TAG = "StatusBar";

    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String STATUS_BAR_DATE = "status_bar_date";
    private static final String STATUS_BAR_DATE_STYLE = "status_bar_date_style";
    private static final String STATUS_BAR_DATE_FORMAT = "status_bar_date_format";
    private static final String PREF_COLOR_PICKER = "clock_color";
    private static final String PREF_FONT_STYLE = "font_style";
    private static final String PREF_STATUS_BAR_CLOCK_FONT_SIZE  = "status_bar_clock_font_size";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";
    private static final String PREF_SMART_PULLDOWN = "smart_pulldown";
    private static final String PREF_CLOCK_DATE_POSITION = "clock_date_position";
    private static final String PREF_CLOCK_COLOR_SWITCH = "statusbar_clock_color_switch";

    private static final int STATUS_BAR_BATTERY_STYLE_HIDDEN = 4;
    private static final int STATUS_BAR_BATTERY_STYLE_TEXT = 6;

    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET = 0;

    private ListPreference mStatusBarClock;
    private ListPreference mStatusBarAmPm;
    private ListPreference mStatusBarDate;
    private ListPreference mStatusBarDateStyle;
    private ListPreference mStatusBarDateFormat;
    private ColorPickerPreference mColorPicker;
    private SwitchPreference mClockColorSwitch;
    private ListPreference mFontStyle;
    private ListPreference mStatusBarClockFontSize;
    private ListPreference mClockDatePosition;

    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarBatteryShowPercent;

    private ListPreference mQuickPulldown;
    private ListPreference mSmartPulldown;

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
	createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;
        addPreferencesFromResource(R.xml.status_bar_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();
        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return null;
        }

        mStatusBarClock = (ListPreference) findPreference(STATUS_BAR_CLOCK_STYLE);
        mStatusBarAmPm = (ListPreference) findPreference(STATUS_BAR_AM_PM);
        mStatusBarDate = (ListPreference) findPreference(STATUS_BAR_DATE);
        mStatusBarDateStyle = (ListPreference) findPreference(STATUS_BAR_DATE_STYLE);
        mStatusBarDateFormat = (ListPreference) findPreference(STATUS_BAR_DATE_FORMAT);
        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBatteryShowPercent =
                (ListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);
        mQuickPulldown = (ListPreference) findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);

        int clockStyle = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_CLOCK, 1);
        mStatusBarClock.setValue(String.valueOf(clockStyle));
        mStatusBarClock.setSummary(mStatusBarClock.getEntry());
        mStatusBarClock.setOnPreferenceChangeListener(this);

        if (DateFormat.is24HourFormat(getActivity())) {
            mStatusBarAmPm.setEnabled(false);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
        } else {
            int statusBarAmPm = CMSettings.System.getInt(resolver,
                    CMSettings.System.STATUS_BAR_AM_PM, 2);
            mStatusBarAmPm.setValue(String.valueOf(statusBarAmPm));
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntry());
            mStatusBarAmPm.setOnPreferenceChangeListener(this);
        }

        int showDate = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_DATE, 0);
        mStatusBarDate.setValue(String.valueOf(showDate));
        mStatusBarDate.setSummary(mStatusBarDate.getEntry());
        mStatusBarDate.setOnPreferenceChangeListener(this);

        int dateStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_DATE_STYLE, 0);
        mStatusBarDateStyle.setValue(String.valueOf(dateStyle));
        mStatusBarDateStyle.setSummary(mStatusBarDateStyle.getEntry());
        mStatusBarDateStyle.setOnPreferenceChangeListener(this);

        mClockDatePosition = (ListPreference) findPreference(PREF_CLOCK_DATE_POSITION);
        mClockDatePosition.setOnPreferenceChangeListener(this);
        mClockDatePosition.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_DATE_POSITION,
                0)));
        mClockDatePosition.setSummary(mClockDatePosition.getEntry());

        mStatusBarDateFormat.setOnPreferenceChangeListener(this);
        mStatusBarDateFormat.setSummary(mStatusBarDateFormat.getEntry());
        if (mStatusBarDateFormat.getValue() == null) {
            mStatusBarDateFormat.setValue("EEE");
        }

        parseClockDateFormats();

        int batteryStyle = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_BATTERY_STYLE, 0);
        mStatusBarBattery.setValue(String.valueOf(batteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        int batteryShowPercent = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0);
        mStatusBarBatteryShowPercent.setValue(String.valueOf(batteryShowPercent));
        mStatusBarBatteryShowPercent.setSummary(mStatusBarBatteryShowPercent.getEntry());
        enableStatusBarBatteryDependents(batteryStyle);
        mStatusBarBatteryShowPercent.setOnPreferenceChangeListener(this);

        mClockColorSwitch =  (SwitchPreference) findPreference(PREF_CLOCK_COLOR_SWITCH);
        mClockColorSwitch.setOnPreferenceChangeListener(this);
        mClockColorSwitch.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_COLOR_SWITCH, 0) == 1);

        mColorPicker = (ColorPickerPreference) findPreference(PREF_COLOR_PICKER);
        mColorPicker.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_COLOR, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/status_bar_clock_color", null, null));
            mColorPicker.setSummary(getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mColorPicker.setSummary(hexColor);
        }
        mColorPicker.setNewPreviewColor(intColor);

        updateColorVisibility();

        mFontStyle = (ListPreference) findPreference(PREF_FONT_STYLE);
        mFontStyle.setOnPreferenceChangeListener(this);
        mFontStyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_FONT_STYLE,
                0)));
        mFontStyle.setSummary(mFontStyle.getEntry());

        mStatusBarClockFontSize = (ListPreference) findPreference(PREF_STATUS_BAR_CLOCK_FONT_SIZE);
        mStatusBarClockFontSize.setOnPreferenceChangeListener(this);
        mStatusBarClockFontSize.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_FONT_SIZE,
                14)));
        mStatusBarClockFontSize.setSummary(mStatusBarClockFontSize.getEntry());

        int quickPulldown = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 1);
        mQuickPulldown.setValue(String.valueOf(quickPulldown));
        if (quickPulldown == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.status_bar_quick_qs_pulldown_off));
        } else {
            String direction = res.getString(quickPulldown == 2
                    ? R.string.status_bar_quick_qs_pulldown_left
                    : R.string.status_bar_quick_qs_pulldown_right);
            mQuickPulldown.setSummary(
                    res.getString(R.string.status_bar_quick_qs_pulldown_summary, direction));
        }
        mQuickPulldown.setOnPreferenceChangeListener(this);

        mSmartPulldown = (ListPreference) findPreference(PREF_SMART_PULLDOWN);
        mSmartPulldown.setOnPreferenceChangeListener(this);
        int smartPulldown = Settings.System.getInt(resolver,
                Settings.System.QS_SMART_PULLDOWN, 0);
        mSmartPulldown.setValue(String.valueOf(smartPulldown));
        updateSmartPulldownSummary(smartPulldown);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefSet;
    }

    @Override
    protected int getMetricsCategory() {
        // todo add a constant in MetricsLogger.java
        return MetricsLogger.MAIN_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Adjust clock position for RTL if necessary
        Configuration config = getResources().getConfiguration();
        if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                mStatusBarClock.setEntries(getActivity().getResources().getStringArray(
                        R.array.status_bar_clock_style_entries_rtl));
                mStatusBarClock.setSummary(mStatusBarClock.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        AlertDialog dialog;

        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();
        if (preference == mStatusBarClock) {
            int clockStyle = Integer.parseInt((String) newValue);
            int index = mStatusBarClock.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(
                    resolver, CMSettings.System.STATUS_BAR_CLOCK, clockStyle);
            mStatusBarClock.setSummary(mStatusBarClock.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarAmPm) {
            int statusBarAmPm = Integer.valueOf((String) newValue);
            int index = mStatusBarAmPm.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(
                    resolver, CMSettings.System.STATUS_BAR_AM_PM, statusBarAmPm);
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarDate) {
            int statusBarDate = Integer.valueOf((String) newValue);
            int index = mStatusBarDate.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, STATUS_BAR_DATE, statusBarDate);
            mStatusBarDate.setSummary(mStatusBarDate.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarDateStyle) {
            int statusBarDateStyle = Integer.parseInt((String) newValue);
            int index = mStatusBarDateStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, STATUS_BAR_DATE_STYLE, statusBarDateStyle);
            mStatusBarDateStyle.setSummary(mStatusBarDateStyle.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mClockDatePosition) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDatePosition.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_DATE_POSITION, val);
            mClockDatePosition.setSummary(mClockDatePosition.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference ==  mStatusBarDateFormat) {
            int index = mStatusBarDateFormat.findIndexOfValue((String) newValue);
            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.status_bar_date_string_edittext_title);
                alert.setMessage(R.string.status_bar_date_string_edittext_summary);

                final EditText input = new EditText(getActivity());
                String oldText = Settings.System.getString(
                    getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);

                alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.STATUS_BAR_DATE_FORMAT, value);

                        return;
                    }
                });

                alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    Settings.System.putString(resolver,
                        Settings.System.STATUS_BAR_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
        } else if (preference == mStatusBarBattery) {
            int batteryStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(
                    resolver, CMSettings.System.STATUS_BAR_BATTERY_STYLE, batteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            enableStatusBarBatteryDependents(batteryStyle);
            return true;
        } else if (preference == mStatusBarBatteryShowPercent) {
            int batteryShowPercent = Integer.valueOf((String) newValue);
            int index = mStatusBarBatteryShowPercent.findIndexOfValue((String) newValue);
            CMSettings.System.putInt(
                    resolver, CMSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, batteryShowPercent);
            mStatusBarBatteryShowPercent.setSummary(
                    mStatusBarBatteryShowPercent.getEntries()[index]);
            return true;
        } else if (preference == mClockColorSwitch) {
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_CLOCK_COLOR_SWITCH,
                    ((Boolean) newValue) ? 1 : 0);
            updateColorVisibility();
            return true;
        } else if (preference == mColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_CLOCK_COLOR, intHex);
            return true;
        } else if (preference == mFontStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mFontStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_CLOCK_FONT_STYLE, val);
            mFontStyle.setSummary(mFontStyle.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarClockFontSize) {
            int val = Integer.parseInt((String) newValue);
            int index = mStatusBarClockFontSize.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_CLOCK_FONT_SIZE, val);
            mStatusBarClockFontSize.setSummary(mStatusBarClockFontSize.getEntries()[index]);
            return true;
        } else if (preference == mQuickPulldown) {
            int quickPulldown = Integer.valueOf((String) newValue);
            CMSettings.System.putInt(resolver, CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                    quickPulldown);
            if (quickPulldown == 0) {
                // quick pulldown deactivated
                mQuickPulldown.setSummary(res.getString(R.string.status_bar_quick_qs_pulldown_off));
            } else {
                String direction = res.getString(quickPulldown == 2
                        ? R.string.status_bar_quick_qs_pulldown_left
                        : R.string.status_bar_quick_qs_pulldown_right);
                mQuickPulldown.setSummary(
                        res.getString(R.string.status_bar_quick_qs_pulldown_summary, direction));
            }
            return true;
        } else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(resolver, Settings.System.QS_SMART_PULLDOWN, smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
            return true;
        }
        return false;
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

    private void enableStatusBarBatteryDependents(int batteryIconStyle) {
        if (batteryIconStyle == STATUS_BAR_BATTERY_STYLE_HIDDEN ||
                batteryIconStyle == STATUS_BAR_BATTERY_STYLE_TEXT) {
            mStatusBarBatteryShowPercent.setEnabled(false);
        } else {
            mStatusBarBatteryShowPercent.setEnabled(true);
        }
    }

    private void enableStatusBarClockDependents() {
        int clockStyle = CMSettings.System.getInt(getActivity()
                .getContentResolver(), CMSettings.System.STATUS_BAR_CLOCK, 1);
        if (clockStyle == 0) {
            mStatusBarDate.setEnabled(false);
            mStatusBarDateStyle.setEnabled(false);
            mStatusBarDateFormat.setEnabled(false);
        } else {
            mStatusBarDate.setEnabled(true);
            mStatusBarDateStyle.setEnabled(true);
            mStatusBarDateFormat.setEnabled(true);
        }
    }

    private void parseClockDateFormats() {
        // Parse and repopulate mClockDateFormats's entries based on current date.
        String[] dateEntries = getResources().getStringArray(R.array.status_bar_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateFormat = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUS_BAR_DATE_STYLE, 0);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }

                parsedDateEntries[i] = newDate;
            }
        }
        mStatusBarDateFormat.setEntries(parsedDateEntries);
    }

    private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = null;
            switch (value) {
                case 1:
                    type = res.getString(R.string.smart_pulldown_dismissable);
                    break;
                case 2:
                    type = res.getString(R.string.smart_pulldown_persistent);
                    break;
                default:
                    type = res.getString(R.string.smart_pulldown_all);
                    break;
            }
            // Remove title capitalized formatting
            type = type.toLowerCase();
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }

    private void updateColorVisibility() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_COLOR_SWITCH, 0) == 1) {
            mColorPicker.setEnabled(true);
        } else {
            mColorPicker.setEnabled(false);
        }
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

        StatusBarSettings getOwner() {
            return (StatusBarSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.status_bar_clock_style_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                Settings.System.STATUSBAR_CLOCK_COLOR, -2);
                            getOwner().createCustomView();
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
                    sir.xmlResId = R.xml.status_bar_settings;
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
