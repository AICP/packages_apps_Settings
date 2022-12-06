/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.IllustrationPreference;

/** Configures the behaviour of long press power illustration. */
public class LongPressPowerIllustrationPreferenceController extends BasePreferenceController
        implements PowerMenuSettingsUtils.SettingsStateCallback, LifecycleObserver {

    private IllustrationPreference mIllustrationPreference;
    private final PowerMenuSettingsUtils mUtils;

    public LongPressPowerIllustrationPreferenceController(Context context, String key) {
        super(context, key);
        mUtils = new PowerMenuSettingsUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mIllustrationPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        ((IllustrationPreference) preference)
                .setLottieAnimationResId(
                        PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext)
                                ? R.raw.lottie_long_press_power_for_assistant
                                : R.raw.lottie_long_press_power_for_power_menu);
    }

    @Override
    public void onChange(Uri uri) {
        if (mIllustrationPreference != null) {
            updateState(mIllustrationPreference);
        }
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_START) */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mUtils.registerObserver(this);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_STOP) */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mUtils.unregisterObserver();
    }
}
