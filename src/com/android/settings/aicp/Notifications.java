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
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import com.android.settings.Utils;
import com.android.settings.util.Helpers;
import com.android.settings.widget.SeekBarPreferenceCham;

public class Notifications extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "NotificationsSettings";

    private static final String KEY_SMS_BREATH = "sms_breath";
    private static final String KEY_MISSED_CALL_BREATH = "missed_call_breath";
    private static final String KEY_VOICEMAIL_BREATH = "voicemail_breath";
    private static final String PREF_LESS_NOTIFICATION_SOUNDS = "less_notification_sounds";
    private static final String SMART_PULLDOWN = "smart_pulldown";
    private static final String PREF_FONT_STYLE = "font_style";
    private static final String PREF_NOTI_REMINDER_SOUND =  "noti_reminder_sound";
    private static final String PREF_NOTI_REMINDER_ENABLED = "noti_reminder_enabled";
    private static final String PREF_NOTI_REMINDER_RINGTONE = "noti_reminder_ringtone";
    private static final String PREF_NOTI_REMINDER_INTERVAL = "noti_reminder_interval";
    private static final String PREF_STATUS_BAR_CUSTOM_HEADER = "status_bar_custom_header";
    private static final String CLOCK_USE_SECOND = "clock_use_second";
    private static final String PREF_NOTIFICAITION_SWIPE = "notification_swipe";
    private static final String STATUSBAR_6BAR_SIGNAL = "statusbar_6bar_signal";
    private static final String PREF_NOTIFICATION_BRIGHTNESS_SLIDER = "notification_brightness_slider";
    private static final String PREF_FORCE_EXPANDED_NOTIFICATIONS = "force_expanded_notifications";
    private static final String PREF_TICKER = "ticker_disabled";

    private CheckBoxPreference mBrightnessSlider;
    private CheckBoxPreference mClockUseSecond;
    private CheckBoxPreference mCustomHeader;
    private CheckBoxPreference mMissedCallBreath;
    private CheckBoxPreference mNotificationSwipe;
    private CheckBoxPreference mReminder;
    private CheckBoxPreference mSMSBreath;
    private CheckBoxPreference mStatusBarSixBarSignal;
    private CheckBoxPreference mVoicemailBreath;
    private CheckBoxPreference mForceExpandedNotifications;
    private CheckBoxPreference mTicker;

    private ListPreference mAnnoyingNotifications;
    private ListPreference mFontStyle;
    private ListPreference mReminderInterval;
    private ListPreference mReminderMode;
    private ListPreference mSmartPulldown;

    private Preference mHeadsUp;

    private RingtonePreference mReminderRingtone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.aicp_status_notif);

        PreferenceScreen prefSet = getPreferenceScreen();

        // Breath Notification
        mSMSBreath = (CheckBoxPreference) prefSet.findPreference(KEY_SMS_BREATH);
        mSMSBreath.setChecked((Settings.System.getInt(resolver, Settings.System.KEY_SMS_BREATH, 0) == 1));

        mMissedCallBreath = (CheckBoxPreference) prefSet.findPreference(KEY_MISSED_CALL_BREATH);
        mMissedCallBreath.setChecked((Settings.System.getInt(resolver, Settings.System.KEY_MISSED_CALL_BREATH, 0) == 1));

        mVoicemailBreath = (CheckBoxPreference) prefSet.findPreference(KEY_VOICEMAIL_BREATH);
        mVoicemailBreath.setChecked((Settings.System.getInt(resolver, Settings.System.KEY_VOICEMAIL_BREATH, 0) == 1));

        // Less Notifications sound
        mAnnoyingNotifications = (ListPreference) prefSet.findPreference(PREF_LESS_NOTIFICATION_SOUNDS);
        int notificationThreshold = Settings.System.getInt(resolver,
                Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, 0);
        mAnnoyingNotifications.setValue(Integer.toString(notificationThreshold));
        mAnnoyingNotifications.setOnPreferenceChangeListener(this);

        // Smart pulldown
        mSmartPulldown = (ListPreference) prefSet.findPreference(SMART_PULLDOWN);
        if (Utils.isPhone(getActivity())) {
            int smartPulldown = Settings.System.getInt(resolver,
                    Settings.System.QS_SMART_PULLDOWN, 2);
            mSmartPulldown.setValue(String.valueOf(smartPulldown));
            updateSmartPulldownSummary(smartPulldown);
            mSmartPulldown.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mSmartPulldown);
        }

        // Clock style
        mFontStyle = (ListPreference) prefSet.findPreference(PREF_FONT_STYLE);
        mFontStyle.setOnPreferenceChangeListener(this);
        mFontStyle.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_FONT_STYLE, 4)));
        mFontStyle.setSummary(mFontStyle.getEntry());

        // Notification Remider
        mReminder = (CheckBoxPreference) prefSet.findPreference(PREF_NOTI_REMINDER_ENABLED);
        mReminder.setChecked(Settings.System.getIntForUser(resolver,
                Settings.System.REMINDER_ALERT_ENABLED, 0, UserHandle.USER_CURRENT) == 1);
        mReminder.setOnPreferenceChangeListener(this);

        mReminderInterval = (ListPreference) prefSet.findPreference(PREF_NOTI_REMINDER_INTERVAL);
        int interval = Settings.System.getIntForUser(resolver,
                Settings.System.REMINDER_ALERT_INTERVAL, 0, UserHandle.USER_CURRENT);
        mReminderInterval.setOnPreferenceChangeListener(this);
        updateReminderIntervalSummary(interval);

        mReminderMode = (ListPreference) prefSet.findPreference(PREF_NOTI_REMINDER_SOUND);
        int mode = Settings.System.getIntForUser(resolver,
                Settings.System.REMINDER_ALERT_NOTIFY, 0, UserHandle.USER_CURRENT);
        mReminderMode.setValue(String.valueOf(mode));
        mReminderMode.setOnPreferenceChangeListener(this);
        updateReminderModeSummary(mode);

        mReminderRingtone =
                (RingtonePreference) prefSet.findPreference(PREF_NOTI_REMINDER_RINGTONE);
        Uri ringtone = null;
        String ringtoneString = Settings.System.getStringForUser(resolver,
                Settings.System.REMINDER_ALERT_RINGER, UserHandle.USER_CURRENT);
        if (ringtoneString == null) {
            // Value not set, defaults to Default Ringtone
            ringtone = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_RINGTONE);
        } else {
            ringtone = Uri.parse(ringtoneString);
        }
        Ringtone alert = RingtoneManager.getRingtone(getActivity(), ringtone);
        mReminderRingtone.setSummary(alert.getTitle(getActivity()));
        mReminderRingtone.setOnPreferenceChangeListener(this);
        mReminderRingtone.setEnabled(mode != 0);

        // Custom notif header
        mCustomHeader = (CheckBoxPreference) prefSet.findPreference(PREF_STATUS_BAR_CUSTOM_HEADER);
        mCustomHeader.setChecked((Settings.System.getInt(resolver, 
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1));

        // Clock seconds
        mClockUseSecond = (CheckBoxPreference) prefSet.findPreference(CLOCK_USE_SECOND);
        mClockUseSecond.setChecked((Settings.System.getInt(resolver,
                Settings.System.CLOCK_USE_SECOND, 0) == 1));

        // Heads up
        mHeadsUp = findPreference(Settings.System.HEADS_UP_NOTIFICATION);

        // Notification swipe
        mNotificationSwipe = (CheckBoxPreference) prefSet.findPreference(PREF_NOTIFICAITION_SWIPE);
        mNotificationSwipe.setChecked((Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NOTIFICATION_SWIPE_FLOATING, 0) == 1));

        // 6 bar signal
        mStatusBarSixBarSignal = (CheckBoxPreference) findPreference(STATUSBAR_6BAR_SIGNAL);
        mStatusBarSixBarSignal.setChecked((Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_6BAR_SIGNAL, 0) == 1));

        // Beightness slider
        boolean enableBrightnessSlider = Settings.AOKP.getBoolean(getContentResolver(),
                Settings.System.NOTIFICATION_BRIGHTNESS_SLIDER, false);
        mBrightnessSlider = (CheckBoxPreference) prefSet.findPreference(PREF_NOTIFICATION_BRIGHTNESS_SLIDER);
        mBrightnessSlider.setChecked(enableBrightnessSlider);
        mBrightnessSlider.setOnPreferenceChangeListener(this);

        // Force Expanded Notifications
        mForceExpandedNotifications = (CheckBoxPreference) prefSet.findPreference(PREF_FORCE_EXPANDED_NOTIFICATIONS);
        mForceExpandedNotifications.setChecked((Settings.System.getInt(resolver,
                Settings.System.FORCE_EXPANDED_NOTIFICATIONS, 0) == 1));

        // Ticker
        mTicker = (CheckBoxPreference) prefSet.findPreference(PREF_TICKER);
        mTicker.setChecked(Settings.System.getInt(resolver, Settings.System.TICKER_DISABLED, 0) == 1);

    }

    @Override
    public void onResume() {
        super.onResume();
        boolean headsUpEnabled = Settings.System.getInt(
                getContentResolver(), Settings.System.HEADS_UP_NOTIFICATION, 0) == 1;
        mHeadsUp.setSummary(headsUpEnabled
                ? R.string.summary_heads_up_enabled : R.string.summary_heads_up_disabled);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mSMSBreath) {
            value = mSMSBreath.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.KEY_SMS_BREATH, value ? 1 : 0);
        } else if (preference == mMissedCallBreath) {
            value = mMissedCallBreath.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.KEY_MISSED_CALL_BREATH, value ? 1 : 0);
        } else if (preference == mVoicemailBreath) {
            value = mVoicemailBreath.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.KEY_VOICEMAIL_BREATH, value ? 1 : 0);
        } else if (preference == mCustomHeader) {
            value = mCustomHeader.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
        } else if (preference == mClockUseSecond) {
            value = mClockUseSecond.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.CLOCK_USE_SECOND, value ? 1 : 0);
        } else if (preference == mNotificationSwipe) {
            value = mNotificationSwipe.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_NOTIFICATION_SWIPE_FLOATING, value ? 1 : 0);
        } else if (preference == mStatusBarSixBarSignal) {
            value = mStatusBarSixBarSignal.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_6BAR_SIGNAL, value ? 1 : 0);
        } else if (preference == mForceExpandedNotifications) {
            value = mForceExpandedNotifications.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.FORCE_EXPANDED_NOTIFICATIONS, value ? 1 : 0);
        } else if (preference == mTicker) {
            value = mTicker.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.TICKER_DISABLED, value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (PREF_LESS_NOTIFICATION_SOUNDS.equals(key)) {
            final int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, value);
        } else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) objValue);
            Settings.System.putInt(resolver, Settings.System.QS_SMART_PULLDOWN,
                    smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
        } else if (preference == mFontStyle) {
            int val = Integer.parseInt((String) objValue);
            int index = mFontStyle.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_CLOCK_FONT_STYLE, val);
            mFontStyle.setSummary(mFontStyle.getEntries()[index]);
        } else if (preference == mReminder) {
            Settings.System.putIntForUser(resolver,
                    Settings.System.REMINDER_ALERT_ENABLED,
                    (Boolean) objValue ? 1 : 0, UserHandle.USER_CURRENT);
        } else if (preference == mReminderInterval) {
            int interval = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.REMINDER_ALERT_INTERVAL,
                    interval, UserHandle.USER_CURRENT);
            updateReminderIntervalSummary(interval);
        } else if (preference == mReminderMode) {
            int mode = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.REMINDER_ALERT_NOTIFY,
                    mode, UserHandle.USER_CURRENT);
            updateReminderModeSummary(mode);
            mReminderRingtone.setEnabled(mode != 0);
        } else if (preference == mReminderRingtone) {
            Uri val = Uri.parse((String) objValue);
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), val);
            mReminderRingtone.setSummary(ringtone.getTitle(getActivity()));
            Settings.System.putStringForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_RINGER,
                    val.toString(), UserHandle.USER_CURRENT);
        } else if (preference == mBrightnessSlider) {
            Settings.AOKP.putBoolean(resolver,
                    Settings.System.NOTIFICATION_BRIGHTNESS_SLIDER,
                    ((Boolean) objValue) ? true : false);
            Helpers.restartSystemUI();
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    private void updateSmartPulldownSummary(int i) {
        if (i == 0) {
            mSmartPulldown.setSummary(R.string.smart_pulldown_off);
        } else if (i == 1) {
            mSmartPulldown.setSummary(R.string.smart_pulldown_dismissable);
        } else if (i == 2) {
            mSmartPulldown.setSummary(R.string.smart_pulldown_persistent);
        }
    }

    private void updateReminderIntervalSummary(int value) {
        int resId;
        switch (value) {
            case 1000:
                resId = R.string.noti_reminder_interval_1s;
                break;
            case 2000:
                resId = R.string.noti_reminder_interval_2s;
                break;
            case 2500:
                resId = R.string.noti_reminder_interval_2dot5s;
                break;
            case 3000:
                resId = R.string.noti_reminder_interval_3s;
                break;
            case 3500:
                resId = R.string.noti_reminder_interval_3dot5s;
                break;
            case 4000:
                resId = R.string.noti_reminder_interval_4s;
                break;
            default:
                resId = R.string.noti_reminder_interval_1dot5s;
                break;
        }
        mReminderInterval.setValue(Integer.toString(value));
        mReminderInterval.setSummary(getResources().getString(resId));
    }

    private void updateReminderModeSummary(int value) {
        int resId;
        switch (value) {
            case 1:
                resId = R.string.enabled;
                break;
            case 2:
                resId = R.string.noti_reminder_sound_looping;
                break;
            default:
                resId = R.string.disabled;
                break;
        }
        mReminderMode.setSummary(getResources().getString(resId));
    }
}
