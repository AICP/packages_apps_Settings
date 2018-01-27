/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.notification;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.AbstractPreferenceController;

public class LessNotificationSoundsController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_LESS_NOTIF_SOUNDS = "less_notification_sounds";

    public LessNotificationSoundsController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LESS_NOTIF_SOUNDS;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ListPreference pref = (ListPreference) preference;
        int lessNotifSoundsValue = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, -1);

        int valueIndex = pref.findIndexOfValue(String.valueOf(lessNotifSoundsValue));
        pref.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        // show user-configured value instead of default summary
        if (valueIndex >= 0) {
            pref.setSummary(pref.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, Integer.valueOf(value));
        updateState(preference);
        return true;
    }
}
