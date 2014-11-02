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

import android.content.ContentResolver;
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
import android.view.WindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class ActiveNotificationSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "ActiveDisplaySettings";

    private static final String KEY_SHOW_TEXT = "ad_text";
    private static final String KEY_REDISPLAY = "ad_redisplay";
    private static final String KEY_SHOW_DATE = "ad_show_date";
    private static final String KEY_TIMEOUT = "ad_timeout";
    private static final String KEY_THRESHOLD = "ad_threshold";
    private static final String KEY_ACTIVE_DISPLAY_DOUBLE_TAP = "active_display_double_tap";
    private static final String KEY_OFFSET_TOP = "offset_top";
    private static final String KEY_EXPANDED_VIEW = "expanded_view";
    private static final String KEY_DISMISS_ALL = "dismiss_all";
    private static final String KEY_FORCE_EXPANDED_VIEW = "force_expanded_view";
    private static final String KEY_NOTIFICATIONS_HEIGHT = "notifications_height";
    private static final String KEY_EXCLUDED_NOTIF_APPS = "excluded_apps";
    private static final String KEY_WAKE_ON_NOTIFICATION = "wake_on_notification";
    private static final String KEY_NOTIFICATION_COLOR = "notification_color";

    private CheckBoxPreference mShowTextPref;
    private CheckBoxPreference mShowDatePref;
    private CheckBoxPreference mAdDoubleTap;
    private ListPreference mRedisplayPref;
    private ListPreference mDisplayTimeout;
    private ListPreference mProximityThreshold;
    private SeekBarPreference mOffsetTop;
    private CheckBoxPreference mWakeOnNotification;
    private CheckBoxPreference mExpandedView;
    private CheckBoxPreference mForceExpandedView;
    private NumberPickerPreference mNotificationsHeight;
    private ColorPickerPreference mNotificationColor;
    private CheckBoxPreference mDismissAll;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.active_notification_settings);
        ContentResolver cr = getContentResolver();

        mShowTextPref = (CheckBoxPreference) findPreference(KEY_SHOW_TEXT);
        mShowTextPref.setChecked((Settings.System.getInt(cr,
                Settings.System.ACTIVE_DISPLAY_TEXT, 0) == 1));

        PreferenceScreen prefSet = getPreferenceScreen();
        mRedisplayPref = (ListPreference) prefSet.findPreference(KEY_REDISPLAY);
        mRedisplayPref.setOnPreferenceChangeListener(this);
        long timeout = Settings.System.getLong(cr,
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, 0);
        mRedisplayPref.setValue(String.valueOf(timeout));
        updateRedisplaySummary(timeout);

        mShowDatePref = (CheckBoxPreference) findPreference(KEY_SHOW_DATE);
        mShowDatePref.setChecked((Settings.System.getInt(cr,
                Settings.System.ACTIVE_DISPLAY_DOUBLE_TAP, 0) == 1));

        mAdDoubleTap = (CheckBoxPreference) findPreference(KEY_ACTIVE_DISPLAY_DOUBLE_TAP);
        mAdDoubleTap.setChecked((Settings.System.getInt(cr,
                Settings.System.ACTIVE_DISPLAY_SHOW_DATE, 0) == 1));

        mDisplayTimeout = (ListPreference) prefSet.findPreference(KEY_TIMEOUT);
        mDisplayTimeout.setOnPreferenceChangeListener(this);
        timeout = Settings.System.getLong(cr,
                Settings.System.ACTIVE_DISPLAY_TIMEOUT, 8000L);
        mDisplayTimeout.setValue(String.valueOf(timeout));
        updateTimeoutSummary(timeout);

        mWakeOnNotification = (CheckBoxPreference) prefSet.findPreference(KEY_WAKE_ON_NOTIFICATION);
        mWakeOnNotification.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_WAKE_ON_NOTIFICATION, 0) == 1);

        mProximityThreshold = (ListPreference) prefSet.findPreference(KEY_THRESHOLD);
        mProximityThreshold.setOnPreferenceChangeListener(this);
        long threshold = Settings.System.getLong(cr,
                Settings.System.ACTIVE_DISPLAY_THRESHOLD, 5000L);
        mProximityThreshold.setValue(String.valueOf(threshold));
        updateThresholdSummary(threshold);

        mOffsetTop = (SeekBarPreference) findPreference(KEY_OFFSET_TOP);
        mOffsetTop.setProgress((int)(Settings.System.getFloat(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_OFFSET_TOP, 0.3f) * 100));
        mOffsetTop.setTitle(getResources().getText(R.string.offset_top) + " " + mOffsetTop.getProgress() + "%");
        mOffsetTop.setOnPreferenceChangeListener(this);

        mExpandedView = (CheckBoxPreference) findPreference(KEY_EXPANDED_VIEW);
        mExpandedView.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_EXPANDED_VIEW, 1) == 1);

        mForceExpandedView = (CheckBoxPreference) findPreference(KEY_FORCE_EXPANDED_VIEW);
        mForceExpandedView.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_FORCE_EXPANDED_VIEW, 0) == 1);

        mDismissAll = (CheckBoxPreference) prefSet.findPreference(KEY_DISMISS_ALL);
        mDismissAll.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_DISMISS_ALL, 1) == 1);

        mNotificationsHeight = (NumberPickerPreference) findPreference(KEY_NOTIFICATIONS_HEIGHT);
        mNotificationsHeight.setValue(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HEIGHT, 4));

        mNotificationColor = (ColorPickerPreference) prefSet.findPreference(KEY_NOTIFICATION_COLOR);
        mNotificationColor.setAlphaSliderEnabled(true);
        int color = Settings.System.getInt(cr,
                Settings.System.LOCKSCREEN_NOTIFICATIONS_COLOR, 0x55555555);
        String hexColor = String.format("#%08x", (0xffffffff & color));
        mNotificationColor.setSummary(hexColor);
        mNotificationColor.setDefaultValue(color);
        mNotificationColor.setNewPreviewColor(color);
        mNotificationColor.setOnPreferenceChangeListener(this);

        Point displaySize = new Point();
        ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);
        int max = Math.round((float)displaySize.y * (1f - (mOffsetTop.getProgress() / 100f)) /
                (float)getResources().getDimensionPixelSize(R.dimen.notification_row_min_height));
        mNotificationsHeight.setMinValue(1);
        mNotificationsHeight.setMaxValue(max);
        mNotificationsHeight.setOnPreferenceChangeListener(this);

        updateEnabled();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnabled();
    }

    private void updateEnabled() {
        ContentResolver cr = getContentResolver();
        boolean Megadeth = Settings.System.getInt(cr,
                Settings.System.LOCKSCREEN_NOTIFICATIONS, 0) == 1;

        boolean AmonAmarth = Settings.System.getInt(cr,
                Settings.System.ENABLE_ACTIVE_DISPLAY, 0) == 1;

        if (!AmonAmarth) {
            mRedisplayPref.setEnabled(false);
            mShowDatePref.setEnabled(false);
            mAdDoubleTap.setEnabled(false);
            mProximityThreshold.setEnabled(false);
            mDisplayTimeout.setEnabled(false);
            mShowTextPref.setEnabled(false);
        } else {
            mRedisplayPref.setEnabled(true);
            mShowDatePref.setEnabled(true);
            mAdDoubleTap.setEnabled(true);
            mProximityThreshold.setEnabled(true);
            mDisplayTimeout.setEnabled(true);
            mShowTextPref.setEnabled(true);
        }

        if (!Megadeth) {
            mOffsetTop.setEnabled(false);
            mNotificationsHeight.setEnabled(false);
            mNotificationColor.setEnabled(false);
            mForceExpandedView.setEnabled(false);
            mExpandedView.setEnabled(false);
            mDismissAll.setEnabled(false);
        } else {
            mOffsetTop.setEnabled(true);
            mNotificationsHeight.setEnabled(true);
            mNotificationColor.setEnabled(true);
            mForceExpandedView.setEnabled(true);
            mExpandedView.setEnabled(true);
            mDismissAll.setEnabled(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_NON_CLEARABLE, 0) == 0);
        }

        if (AmonAmarth) {
            mWakeOnNotification.setEnabled(false);
            mWakeOnNotification.setSummary(R.string.wake_on_notification_disable);
        } else {
            if (!Megadeth) {
                mWakeOnNotification.setEnabled(false);
            } else {
                mWakeOnNotification.setEnabled(true);
            }
            mWakeOnNotification.setSummary(R.string.wake_on_notification_summary);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRedisplayPref) {
            int timeout = Integer.valueOf((String) newValue);
            updateRedisplaySummary(timeout);
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

        } else if (preference == mNotificationColor) {
            String hex = ColorPickerPreference.convertToARGB(
            Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_COLOR, intHex);
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
            Settings.System.putInt(getContentResolver(), Settings.System.ACTIVE_DISPLAY_TEXT,
                    mShowTextPref.isChecked() ? 1 : 0);

        } else if (preference == mExpandedView) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS_EXPANDED_VIEW,
                    mExpandedView.isChecked() ? 1 : 0);
            mForceExpandedView.setEnabled(mExpandedView.isChecked());

        } else if (preference == mForceExpandedView) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS_FORCE_EXPANDED_VIEW,
                    mForceExpandedView.isChecked() ? 1 : 0);

        } else if (preference == mDismissAll) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS_DISMISS_ALL,
                    mDismissAll.isChecked() ? 1 : 0);

        } else if (preference == mWakeOnNotification) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS_WAKE_ON_NOTIFICATION,
                    mWakeOnNotification.isChecked() ? 1 : 0);

        } else if (preference == mShowDatePref) {
            Settings.System.putInt(getContentResolver(), Settings.System.ACTIVE_DISPLAY_SHOW_DATE,
                    mShowDatePref.isChecked() ? 1 : 0);

        } else if (preference == mAdDoubleTap) {
            Settings.System.putInt(getContentResolver(), Settings.System.ACTIVE_DISPLAY_DOUBLE_TAP,
                    mAdDoubleTap.isChecked() ? 1 : 0);

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
}
