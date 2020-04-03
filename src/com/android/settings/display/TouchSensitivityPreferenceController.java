/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.display;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;

import androidx.fragment.app.Fragment;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.R;

public class TouchSensitivityPreferenceController extends TogglePreferenceController {

    public static int OFF = 0;
    public static int ON = 1;

    public static final String TOUCH_SENSITIVITY_ENABLED = "touch_sensitivity_enabled";
    public static final String TOUCH_SENSITIVITY_PROP = "debug.touch_sensitivity_mode";

    private Fragment mParent;

    public TouchSensitivityPreferenceController(Context context, String str) {
        super(context, str);
    }

    public void init(Fragment fragment) {
        mParent = fragment;
    }

    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_touch_sensitivity) ? 0 : 3;
    }

    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), TOUCH_SENSITIVITY_ENABLED, 0) == 1;
    }

    public boolean setChecked(boolean isPresentandEnabled) {
        Settings.Secure.putInt(mContext.getContentResolver(), TOUCH_SENSITIVITY_ENABLED, isPresentandEnabled ? 1 : 0);
        SystemProperties.set(TOUCH_SENSITIVITY_PROP, isPresentandEnabled ? "1" : "0");
        return true;
    }
}
