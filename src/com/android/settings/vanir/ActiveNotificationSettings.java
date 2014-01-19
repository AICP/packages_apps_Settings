/*
 * Copyright (C) 2013 The ChameleonOS Project
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

package com.android.settings.vanir;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ActiveNotificationSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "ActiveDisplaySettings";

    private static final String KEY_SHOW_TEXT = "ad_text";
    private static final String KEY_REDISPLAY = "ad_redisplay";
    private static final String KEY_EXCLUDED_APPS = "ad_excluded_apps";
    private static final String KEY_SHOW_DATE = "ad_show_date";
    private static final String KEY_SHOW_AMPM = "ad_show_ampm";
    private static final String KEY_BRIGHTNESS = "ad_brightness";
    private static final String KEY_TIMEOUT = "ad_timeout";
    private static final String KEY_THRESHOLD = "ad_threshold";
    private static final String KEY_OFFSET_TOP = "offset_top";
    private static final String KEY_EXPANDED_VIEW = "expanded_view";
    private static final String KEY_FORCE_EXPANDED_VIEW = "force_expanded_view";
    private static final String KEY_NOTIFICATIONS_HEIGHT = "notifications_height";
    private static final String KEY_EXCLUDED_NOTIF_APPS = "excluded_apps";

    private CheckBoxPreference mShowTextPref;
    private CheckBoxPreference mShowDatePref;
    private CheckBoxPreference mShowAmPmPref;
    private ListPreference mRedisplayPref;
    private SeekBarPreference mBrightnessLevel;
    private AppMultiSelectListPreference mExcludedAppsPref;
    private ListPreference mDisplayTimeout;
    private ListPreference mProximityThreshold;
    private SeekBarPreference mOffsetTop;
    private CheckBoxPreference mExpandedView;
    private CheckBoxPreference mForceExpandedView;
    private NumberPickerPreference mNotificationsHeight;
    private AppMultiSelectListPreference mNotifAppsPref;		

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.active_notification_settings);

        mShowTextPref = (CheckBoxPreference) findPreference(KEY_SHOW_TEXT);
        mShowTextPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_TEXT, 0) == 1));

        PreferenceScreen prefSet = getPreferenceScreen();
        mRedisplayPref = (ListPreference) prefSet.findPreference(KEY_REDISPLAY);
        mRedisplayPref.setOnPreferenceChangeListener(this);
        long timeout = Settings.System.getLong(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, 0);
        mRedisplayPref.setValue(String.valueOf(timeout));
        updateRedisplaySummary(timeout);

        mShowDatePref = (CheckBoxPreference) findPreference(KEY_SHOW_DATE);
        mShowDatePref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_SHOW_DATE, 0) == 1));

        mShowAmPmPref = (CheckBoxPreference) findPreference(KEY_SHOW_AMPM);
        mShowAmPmPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_SHOW_AMPM, 0) == 1));

        int level = Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_BRIGHTNESS, 0);
        mBrightnessLevel = (SeekBarPreference) findPreference(KEY_BRIGHTNESS);
        mBrightnessLevel.setProgress((int) (level));
        mBrightnessLevel.setOnPreferenceChangeListener(this);

        mDisplayTimeout = (ListPreference) prefSet.findPreference(KEY_TIMEOUT);
        mDisplayTimeout.setOnPreferenceChangeListener(this);
        timeout = Settings.System.getLong(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_TIMEOUT, 8000L);
        mDisplayTimeout.setValue(String.valueOf(timeout));
        updateTimeoutSummary(timeout);

        mProximityThreshold = (ListPreference) prefSet.findPreference(KEY_THRESHOLD);
        mProximityThreshold.setOnPreferenceChangeListener(this);
        long threshold = Settings.System.getLong(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_THRESHOLD, 5000L);
        mProximityThreshold.setValue(String.valueOf(threshold));
        updateThresholdSummary(threshold);

        mExcludedAppsPref = (AppMultiSelectListPreference) findPreference(KEY_EXCLUDED_APPS);
        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) mExcludedAppsPref.setValues(excludedApps);
        mExcludedAppsPref.setOnPreferenceChangeListener(this);

        mNotifAppsPref = (AppMultiSelectListPreference) findPreference(KEY_EXCLUDED_NOTIF_APPS);
        Set<String> excludedNotifApps = getExcludedNotifApps();
        if (excludedNotifApps != null) mNotifAppsPref.setValues(excludedNotifApps);
        mNotifAppsPref.setOnPreferenceChangeListener(this);

        mOffsetTop = (SeekBarPreference) findPreference(KEY_OFFSET_TOP);
        mOffsetTop.setProgress((int)(Settings.System.getFloat(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_OFFSET_TOP, 0.3f) * 100));
        mOffsetTop.setTitle(getResources().getText(R.string.offset_top) + " " + mOffsetTop.getProgress() + "%");
        mOffsetTop.setOnPreferenceChangeListener(this);

        mExpandedView = (CheckBoxPreference) findPreference(KEY_EXPANDED_VIEW);
        mExpandedView.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_EXPANDED_VIEW, 1) == 1);

        mForceExpandedView = (CheckBoxPreference) findPreference(KEY_FORCE_EXPANDED_VIEW);
        mForceExpandedView.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_FORCE_EXPANDED_VIEW, 0) == 1);

        mNotificationsHeight = (NumberPickerPreference) findPreference(KEY_NOTIFICATIONS_HEIGHT);
        mNotificationsHeight.setValue(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HEIGHT, 4));

        Point displaySize = new Point();
        ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);
        int max = Math.round((float)displaySize.y * (1f - (mOffsetTop.getProgress() / 100f)) /
                (float)getResources().getDimensionPixelSize(R.dimen.notification_row_min_height));
        mNotificationsHeight.setMinValue(1);
        mNotificationsHeight.setMaxValue(max);
        mNotificationsHeight.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRedisplayPref) {
            int timeout = Integer.valueOf((String) newValue);
            updateRedisplaySummary(timeout);
            return true;
        } else if (preference == mBrightnessLevel) {
            int brightness = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_BRIGHTNESS, brightness);
            return true;
        } else if (preference == mNotificationsHeight) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HEIGHT, (Integer)newValue);
            return true;
        } else if (preference == mDisplayTimeout) {
            long timeout = Integer.valueOf((String) newValue);
            updateTimeoutSummary(timeout);
            return true;
        } else if (preference == mProximityThreshold) {
            long threshold = Integer.valueOf((String) newValue);
            updateThresholdSummary(threshold);
            return true;
        } else if (preference == mExcludedAppsPref) {
            storeExcludedApps((Set<String>) newValue);
            return true;
        } else if (preference == mNotifAppsPref) {
			storeExcludedNotifApps((Set<String>) newValue);
			return true;
        } else if (preference == mOffsetTop) {
            Settings.System.putFloat(getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS_OFFSET_TOP,
                    (Integer)newValue / 100f);
            mOffsetTop.setTitle(getResources().getText(R.string.offset_top) + " " + (Integer)newValue + "%");
            Point displaySize = new Point();
            ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);
            int max = Math.round((float)displaySize.y * (1f - (mOffsetTop.getProgress() / 100f)) /
                    (float)getResources().getDimensionPixelSize(R.dimen.notification_row_min_height));
            mNotificationsHeight.setMaxValue(max);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mShowTextPref) {
            value = mShowTextPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_TEXT,
                    value ? 1 : 0);
        } else if (preference == mExpandedView) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS_EXPANDED_VIEW,
                    mExpandedView.isChecked() ? 1 : 0);
            mForceExpandedView.setEnabled(mExpandedView.isChecked());
        } else if (preference == mForceExpandedView) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS_FORCE_EXPANDED_VIEW,
                    mForceExpandedView.isChecked() ? 1 : 0);
        } else if (preference == mShowDatePref) {
            value = mShowDatePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_SHOW_DATE,
                    value ? 1 : 0);
        } else if (preference == mShowAmPmPref) {
            value = mShowAmPmPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_SHOW_AMPM,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private void updateRedisplaySummary(long value) {
        mRedisplayPref.setSummary(mRedisplayPref.getEntries()[mRedisplayPref.findIndexOfValue("" + value)]);
        Settings.System.putLong(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, value);
    }

    private void updateTimeoutSummary(long value) {
        try {
            mDisplayTimeout.setSummary(mDisplayTimeout.getEntries()[mDisplayTimeout.findIndexOfValue("" + value)]);
            Settings.System.putLong(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_TIMEOUT, value);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private void updateThresholdSummary(long value) {
        try {
            mProximityThreshold.setSummary(mProximityThreshold.getEntries()[mProximityThreshold.findIndexOfValue("" + value)]);
            Settings.System.putLong(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_THRESHOLD, value);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private Set<String> getExcludedApps() {
        String excluded = Settings.System.getString(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excluded))
            return null;

        return new HashSet<String>(Arrays.asList(excluded.split("\\|")));
    }

    private void storeExcludedApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS, builder.toString());
    }

    private Set<String> getExcludedNotifApps() {
        String excludedNotif = Settings.System.getString(getContentResolver(),
        Settings.System.LOCKSCREEN_NOTIFICATIONS_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excludedNotif)) return null;

        return new HashSet<String>(Arrays.asList(excludedNotif.split("\\|")));
    }

    private void storeExcludedNotifApps(Set<String> values) {
        StringBuilder Notifbuilder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
			Notifbuilder.append(delimiter);
			Notifbuilder.append(value);
			delimiter = "|";
        }
        Settings.System.putString(getContentResolver(),
			Settings.System.LOCKSCREEN_NOTIFICATIONS_EXCLUDED_APPS, Notifbuilder.toString());
    }
}
