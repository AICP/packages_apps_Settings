/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;

import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class CustomLightsPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_CUSTOM_LIGHT = "custom_light";

    private int mLedColor = 0;

    public CustomLightsPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CUSTOM_LIGHT;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mChannel == null) {
            return false;
        }
        return checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT)
                && canPulseLight();
    }

    public void updateState(Preference preference) {
        if (mChannel != null) {
             //light color pref
            ColorPickerPreference mCustomLight = (ColorPickerPreference) preference;
            int defaultLightColor = mContext.getResources().getColor(com.android.internal.R.color.config_defaultNotificationColor);
            mCustomLight.setDefaultValue(defaultLightColor);
            mLedColor = (mChannel.getLightColor() != 0 ? mChannel.getLightColor() : defaultLightColor);
            mCustomLight.setAlphaSliderEnabled(false);
            mCustomLight.setNewPreviewColor(mLedColor);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel != null) {
            mLedColor = ((Integer) newValue).intValue();
            mChannel.setLightColor(mLedColor);
            saveChannel();
            showLedPreview();
        }
        return true;
    }

    boolean canPulseLight() {
        if (!mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            return false;
        }
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 1) == 1;
    }

    private void showLedPreview() {
        if (mChannel.shouldShowLights()) {
            if (mLedColor == 0xFFFFFFFF) {
                // i've no idea why atm but this is needed
                mLedColor = 0xffffff;
            }
            mNm.forcePulseLedLight(
                    mLedColor, mChannel.getLightOnTime(), mChannel.getLightOffTime());
        }
    }

}
