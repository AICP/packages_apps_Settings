/*
 * Copyright (C) 2018 gzosp
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
package com.android.settings.gzosp.buttons;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.System.BUTTON_BRIGHTNESS_ENABLED;

public class ButtonBrightnessPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private String mKey;
    private int mDeviceHardwareKeys;

    public ButtonBrightnessPreferenceController(Context context, String key) {
        super(context);
        mKey = key;
        mDeviceHardwareKeys = context.getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
    }

    @Override
    public boolean isAvailable() {
        return mDeviceHardwareKeys != 0;
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public void updateState(Preference preference) {
        int setting = Settings.System.getInt(mContext.getContentResolver(),
                BUTTON_BRIGHTNESS_ENABLED, 1);
        ((SwitchPreference) preference).setChecked(setting != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), BUTTON_BRIGHTNESS_ENABLED,
                enabled ? 1 : 0);
        return true;
    }
}
