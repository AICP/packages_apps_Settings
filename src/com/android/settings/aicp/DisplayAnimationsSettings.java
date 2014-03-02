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
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import java.util.ArrayList;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.hfm.HfmHelpers;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

public class DisplayAnimationsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "DisplayAnimationsSettings";

    private static final String CATEGORY_SYSTEM = "system_category";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_BLUR_BEHIND = "blur_behind";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
    private static final String KEY_ALLOW_ROTATION = "allow_rotation";
    private static final String STATUS_BAR_NETWORK_HIDE = "status_bar_network_hide";
    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";
    private static final String KEY_SMS_BREATH = "sms_breath";
    private static final String KEY_MISSED_CALL_BREATH = "missed_call_breath";
    private static final String KEY_VOICEMAIL_BREATH = "voicemail_breath";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";
    private static final String PREF_LESS_NOTIFICATION_SOUNDS = "less_notification_sounds";
    private static final String PREF_LOCKSCREEN_TORCH = "lockscreen_torch";
    private static final String KEY_ENABLE_POWER_MENU = "lockscreen_enable_power_menu";
    private static final String SREC_ENABLE_TOUCHES = "srec_enable_touches";
    private static final String SREC_ENABLE_MIC = "srec_enable_mic";
    private static final String RECENTS_CLEAR_ALL = "recents_clear_all";
    private static final String HFM_DISABLE_ADS = "hfm_disable_ads";
    private static final String KEY_NAVBAR_LEFT_IN_LANDSCAPE = "navigation_bar_left";
    private static final String SMART_PULLDOWN = "smart_pulldown";
    private static final String KEY_TOAST_ANIMATION = "toast_animation";
    private static final String PREF_FONT_STYLE = "font_style";

    private CheckBoxPreference mAllowRotation;
    private CheckBoxPreference mBlurBehind;
    private CheckBoxPreference mEnableCameraWidget;
    private CheckBoxPreference mEnablePowerMenu;
    private CheckBoxPreference mGlowpadTorch;
    private CheckBoxPreference mHfmDisableAds;
    private CheckBoxPreference mLockRingBattery;
    private CheckBoxPreference mMissedCallBreath;
    private CheckBoxPreference mNavigationBarLeft;
    private CheckBoxPreference mSMSBreath;
    private CheckBoxPreference mStatusBarNetworkHide;
    private CheckBoxPreference mSrecEnableTouches;
    private CheckBoxPreference mSrecEnableMic;
    private CheckBoxPreference mVoicemailBreath;
    private ListPreference mAnnoyingNotifications;
    private ListPreference mClearAll;
    private ListPreference mFontStyle;
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;
    private ListPreference mSmartPulldown;
    private ListPreference mToastAnimation;
    private SeekBarPreference mBlurRadius;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockUtils;
    private DevicePolicyManager mDPM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.aicp_display_animations_settings);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        PreferenceScreen prefSet = getPreferenceScreen();

        // ListView Animations
        mListViewAnimation = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(resolver,
                Settings.System.LISTVIEW_ANIMATION, 0);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(resolver,
                Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);
        mListViewInterpolator.setEnabled(listviewanimation > 0);

        // Blur lockscreen
        mBlurBehind = (CheckBoxPreference) prefSet.findPreference(KEY_BLUR_BEHIND);
        mBlurBehind.setChecked(Settings.System.getInt(resolver,
            Settings.System.LOCKSCREEN_BLUR_BEHIND, 0) == 1);
        mBlurRadius = (SeekBarPreference) findPreference(KEY_BLUR_RADIUS);
        mBlurRadius.setProgress(Settings.System.getInt(resolver,
            Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
        mBlurRadius.setOnPreferenceChangeListener(this);

        // Rotate
        mAllowRotation = (CheckBoxPreference) prefSet.findPreference(KEY_ALLOW_ROTATION);
        mAllowRotation.setChecked(Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_ROTATION, 0) == 1);

        // NetStat hide if there's no traffic
        mStatusBarNetworkHide = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NETWORK_HIDE);
        mStatusBarNetworkHide.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_HIDE, 0) == 1);

        // Breath Notification
        mSMSBreath = (CheckBoxPreference) prefSet.findPreference(KEY_SMS_BREATH);
        mSMSBreath.setChecked((Settings.System.getInt(resolver, Settings.System.KEY_SMS_BREATH, 0) == 1));

        mMissedCallBreath = (CheckBoxPreference) prefSet.findPreference(KEY_MISSED_CALL_BREATH);
        mMissedCallBreath.setChecked((Settings.System.getInt(resolver, Settings.System.KEY_MISSED_CALL_BREATH, 0) == 1));

        mVoicemailBreath = (CheckBoxPreference) prefSet.findPreference(KEY_VOICEMAIL_BREATH);
        mVoicemailBreath.setChecked((Settings.System.getInt(resolver, Settings.System.KEY_VOICEMAIL_BREATH, 0) == 1));

        // GlowPad torch
        mGlowpadTorch = (CheckBoxPreference) prefSet.findPreference(PREF_LOCKSCREEN_TORCH);
        mGlowpadTorch.setChecked(Settings.System.getInt(resolver,
                 Settings.System.LOCKSCREEN_GLOWPAD_TORCH, 0) == 1);
        mGlowpadTorch.setOnPreferenceChangeListener(this);

        // Lock ring battery
        mLockRingBattery = (CheckBoxPreference) prefSet.findPreference(BATTERY_AROUND_LOCKSCREEN_RING);
        if (mLockRingBattery != null) {
            mLockRingBattery.setChecked(Settings.System.getInt(resolver,
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, 0) == 1);
        }

        // Hide camera widget
        mEnableCameraWidget = (CheckBoxPreference) prefSet.findPreference(KEY_ENABLE_CAMERA);

        // Less Notifications sound
        mAnnoyingNotifications = (ListPreference) prefSet.findPreference(PREF_LESS_NOTIFICATION_SOUNDS);
        int notificationThreshold = Settings.System.getInt(resolver,
                Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, 0);
        mAnnoyingNotifications.setValue(Integer.toString(notificationThreshold));
        mAnnoyingNotifications.setOnPreferenceChangeListener(this);

        mEnablePowerMenu = (CheckBoxPreference) prefSet.findPreference(KEY_ENABLE_POWER_MENU);
        mEnablePowerMenu.setChecked(Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_ENABLE_POWER_MENU, 1) == 1);
        mEnablePowerMenu.setOnPreferenceChangeListener(this);

        // Screen recording
        mSrecEnableTouches = (CheckBoxPreference) prefSet.findPreference(SREC_ENABLE_TOUCHES);
        mSrecEnableTouches.setChecked((Settings.System.getInt(resolver,
                Settings.System.SREC_ENABLE_TOUCHES, 0) == 1));
        mSrecEnableTouches.setOnPreferenceChangeListener(this);
        mSrecEnableMic = (CheckBoxPreference) prefSet.findPreference(SREC_ENABLE_MIC);
        mSrecEnableMic.setChecked((Settings.System.getInt(resolver,
                Settings.System.SREC_ENABLE_MIC, 0) == 1));

        // Clear all position
        mClearAll = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL);
        int value = Settings.System.getInt(resolver,
                Settings.System.CLEAR_RECENTS_BUTTON_LOCATION, 4);
        mClearAll.setValue(String.valueOf(value));
        mClearAll.setSummary(mClearAll.getEntry());
        mClearAll.setOnPreferenceChangeListener(this);

        // Disable ads
        mHfmDisableAds = (CheckBoxPreference) prefSet.findPreference(HFM_DISABLE_ADS);
        mHfmDisableAds.setChecked((Settings.System.getInt(resolver,
                Settings.System.HFM_DISABLE_ADS, 0) == 1));

        // Navigation bar left
        mNavigationBarLeft = (CheckBoxPreference) prefSet.findPreference(KEY_NAVBAR_LEFT_IN_LANDSCAPE);
        mNavigationBarLeft.setChecked((Settings.System.getInt(resolver,
                Settings.System.NAVBAR_LEFT_IN_LANDSCAPE, 0) == 1));
        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
            PreferenceCategory navCategory = (PreferenceCategory) findPreference(CATEGORY_SYSTEM);

            if (hasNavBar) {
                if (!Utils.isPhone(getActivity())) {
                    navCategory.removePreference(mNavigationBarLeft);
                }
            } else {
                navCategory.removePreference(mNavigationBarLeft);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

        // Smart pulldown
        mSmartPulldown = (ListPreference) prefSet.findPreference(SMART_PULLDOWN);
        if (Utils.isPhone(getActivity())) {
            int smartPulldown = Settings.System.getInt(resolver,
                    Settings.System.QS_SMART_PULLDOWN, 0);
            mSmartPulldown.setValue(String.valueOf(smartPulldown));
            updateSmartPulldownSummary(smartPulldown);
            mSmartPulldown.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mSmartPulldown);
        }

        // Toast animations
        mToastAnimation = (ListPreference) prefSet.findPreference(KEY_TOAST_ANIMATION);
        mToastAnimation.setSummary(mToastAnimation.getEntry());
        int CurrentToastAnimation = Settings.System.getInt(resolver,
                Settings.System.TOAST_ANIMATION, 1);
        mToastAnimation.setValueIndex(CurrentToastAnimation);
        mToastAnimation.setOnPreferenceChangeListener(this);

        // Clock style
        mFontStyle = (ListPreference) prefSet.findPreference(PREF_FONT_STYLE);
        mFontStyle.setOnPreferenceChangeListener(this);
        mFontStyle.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_FONT_STYLE, 4)));
        mFontStyle.setSummary(mFontStyle.getEntry());
    }
       

    @Override
    public void onResume() {
        super.onResume();

        // Update camera widget
        if (mEnableCameraWidget != null) {
            mEnableCameraWidget.setChecked(mLockUtils.getCameraEnabled());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mBlurBehind) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_BLUR_BEHIND,
                    mBlurBehind.isChecked() ? 1 : 0);
        } else if (preference == mAllowRotation) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_ROTATION, mAllowRotation.isChecked()
                    ? 1 : 0);
        } else if (preference == mStatusBarNetworkHide) {
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_NETWORK_HIDE, mStatusBarNetworkHide.isChecked()
                    ? 1 : 0);
            if (mStatusBarNetworkHide.isChecked()) {
                Toast.makeText(getActivity(), "Network traffic must be enabled in ROMControl!",
                        Toast.LENGTH_LONG).show();
            }
        } else if (preference == mSMSBreath) {
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
        } else if (preference == mLockRingBattery) {
            Settings.System.putInt(resolver,
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING,
                    mLockRingBattery.isChecked() ? 1 : 0);
        } else if (preference == mEnableCameraWidget) {
            mLockUtils.setCameraEnabled(mEnableCameraWidget.isChecked());
        } else if (preference == mGlowpadTorch) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_GLOWPAD_TORCH, mGlowpadTorch.isChecked() ? 1 : 0);
        } else if (preference == mEnablePowerMenu) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_ENABLE_POWER_MENU,
                    mEnablePowerMenu.isChecked() ? 1: 0);
        } else if  (preference == mSrecEnableTouches) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.SREC_ENABLE_TOUCHES, checked ? 1:0);
        } else if  (preference == mSrecEnableMic) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.SREC_ENABLE_MIC, checked ? 1:0);
        } else if  (preference == mHfmDisableAds) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.HFM_DISABLE_ADS, checked ? 1:0);
            HfmHelpers.checkStatus(getActivity());
        } else if (preference == mNavigationBarLeft) {
            value = mNavigationBarLeft.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.NAVBAR_LEFT_IN_LANDSCAPE, value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (KEY_LISTVIEW_ANIMATION.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.LISTVIEW_ANIMATION,
                    value);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            mListViewInterpolator.setEnabled(value > 0);
        } else if (KEY_LISTVIEW_INTERPOLATOR.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    value);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
        } else if (preference == mBlurRadius) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer)objValue);
        } else if (PREF_LESS_NOTIFICATION_SOUNDS.equals(key)) {
            final int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, value);
        } else if (RECENTS_CLEAR_ALL.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mClearAll.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.CLEAR_RECENTS_BUTTON_LOCATION,
                    value);
            mClearAll.setSummary(mClearAll.getEntries()[index]);
        } else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) objValue);
            Settings.System.putInt(resolver, Settings.System.QS_SMART_PULLDOWN,
                    smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
        } else if (preference == mToastAnimation) {
            int index = mToastAnimation.findIndexOfValue((String) objValue);
            Settings.System.putString(resolver,
                    Settings.System.TOAST_ANIMATION, (String) objValue);
            mToastAnimation.setSummary(mToastAnimation.getEntries()[index]);
            Toast.makeText(getActivity(), "Toast animation test!!!",
                    Toast.LENGTH_SHORT).show();
        } else if (preference == mFontStyle) {
            int val = Integer.parseInt((String) objValue);
            int index = mFontStyle.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_CLOCK_FONT_STYLE, val);
            mFontStyle.setSummary(mFontStyle.getEntries()[index]);
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
}
