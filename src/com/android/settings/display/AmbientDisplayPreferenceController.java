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
package com.android.settings.display;

import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class AmbientDisplayPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final int MY_USER_ID = UserHandle.myUserId();

    private final AmbientDisplayConfiguration mAmbientConfig;
    private final String mKey;

    public AmbientDisplayPreferenceController(Context context, AmbientDisplayConfiguration config,
            String key) {
        super(context);
        mAmbientConfig = config;
        mKey = key;
    }

    @Override
    public boolean isAvailable() {
        return mAmbientConfig.ambientDisplayAvailable();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mAmbientConfig.alwaysOnEnabled(MY_USER_ID)) {
            preference.setSummary(R.string.ambient_display_screen_summary_always_on);
        } else if (mAmbientConfig.pulseOnNotificationEnabled(MY_USER_ID)) {
            preference.setSummary(R.string.ambient_display_screen_summary_notifications);
        } else if (mAmbientConfig.enabled(MY_USER_ID)) {
            preference.setSummary(R.string.switch_on_text);
        } else {
            preference.setSummary(R.string.switch_off_text);
        }
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }
}
