/*
 * Copyright (C) 2018 gzosp
 * Copyright (C) 2018 AICP
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
 * limitations under the License
 */

package com.android.settings.gzosp.buttons;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.System.CAMERA_WAKE_SCREEN;

public class CameraButtonWakePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final int KEY_MASK_CAMERA = 0x20;

    private Context mContext;
    private SwitchPreference mPref;

    private int mDeviceHardwareWakeKeys;
    private final String mKey;

    public CameraButtonWakePreferenceController(Context context, String key) {
        super(context);
        mContext = context;
        mKey = key;
        mDeviceHardwareWakeKeys = context.getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareWakeKeys);
    }

    @Override
    public boolean isAvailable() {
        return (mDeviceHardwareWakeKeys & KEY_MASK_CAMERA) != 0;
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public void updateState(Preference preference) {
        boolean defaultBehavior = (mDeviceHardwareWakeKeys & KEY_MASK_CAMERA) != 0;
        int setting = Settings.System.getInt(mContext.getContentResolver(),
                CAMERA_WAKE_SCREEN, defaultBehavior ? 1 : 0);
        ((SwitchPreference) preference).setChecked(setting != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), CAMERA_WAKE_SCREEN,
                enabled ? 1 : 0);
        return true;
    }
}
