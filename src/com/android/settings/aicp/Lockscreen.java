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

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

public class Lockscreen extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "LockscreenSettings";

    private static final String CATEGORY_HWBUTTONS = "hwbutton_category";
    private static final String SCREEN_HWBUTTONS = "lockscreen_screen";
    private static final String KEY_BLUR_BEHIND = "blur_behind";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
    private static final String KEY_ALLOW_ROTATION = "allow_rotation";
    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";
    private static final String PREF_LOCKSCREEN_TORCH = "lockscreen_torch";
    private static final String KEY_DISABLE_FRAME = "lockscreen_disable_frame";

    private CheckBoxPreference mAllowRotation;
    private CheckBoxPreference mBlurBehind;
    private CheckBoxPreference mEnableCameraWidget;
    private CheckBoxPreference mDisableFrame;
    private CheckBoxPreference mGlowpadTorch;
    private CheckBoxPreference mLockRingBattery;
    private SeekBarPreference mBlurRadius;

    private PreferenceScreen mLockscreenScreen;
    private PreferenceCategory mHwButtonsCategory;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockUtils;
    private DevicePolicyManager mDPM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.aicp_lockscreen);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        PreferenceScreen prefSet = getPreferenceScreen();

        mLockscreenScreen = (PreferenceScreen) findPreference("lockscreen_screen");
        mHwButtonsCategory = (PreferenceCategory) findPreference("hwbutton_category");

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

        // Lockscreen widget frame
        mDisableFrame = (CheckBoxPreference) prefSet.findPreference(KEY_DISABLE_FRAME);
        mDisableFrame.setChecked(Settings.System.getInt(resolver,
                    Settings.System.LOCKSCREEN_WIDGET_FRAME_ENABLED, 0) == 1);
        mDisableFrame.setOnPreferenceChangeListener(this);

        // Remove lockscreen button actions if device doesn't have hardware keys
        if (!hasButtons()) {
            mLockscreenScreen.removePreference(mHwButtonsCategory);
        }

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
        } else if (preference == mLockRingBattery) {
            Settings.System.putInt(resolver,
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING,
                    mLockRingBattery.isChecked() ? 1 : 0);
        } else if (preference == mEnableCameraWidget) {
            mLockUtils.setCameraEnabled(mEnableCameraWidget.isChecked());
        } else if (preference == mGlowpadTorch) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_GLOWPAD_TORCH, mGlowpadTorch.isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (preference == mBlurRadius) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer)objValue);
        } else if (preference == mDisableFrame) {
            Settings.System.putInt(resolver,
                    Settings.System.LOCKSCREEN_WIDGET_FRAME_ENABLED,
                    (Boolean) objValue ? 1 : 0);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    /**
     * Checks if the device has hardware buttons.
     * @return has Buttons
     */
    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }
}
