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
import android.support.v7.preference.Preference;


import com.android.settings.core.PreferenceController;

public class CustomDozePreferenceController extends PreferenceController {

    private static final String KEY_CUSTOM_DOZE = "custom_doze";

    public CustomDozePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CUSTOM_DOZE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public boolean isAvailable() {
        if (mContext.getApplicationContext().getResources().getBoolean(com.android.internal.R.bool.config_alt_ambient_display)) {
            return true;
        } else {
            return false;
        }
    }
}
