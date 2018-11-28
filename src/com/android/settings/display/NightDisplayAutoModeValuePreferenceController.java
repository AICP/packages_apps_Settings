/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class NightDisplayAutoModeValuePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private DropDownPreference mPreference;

    public NightDisplayAutoModeValuePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayController.isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (DropDownPreference) screen.findPreference(getPreferenceKey());

        mPreference.setEntries(new CharSequence[]{
                mContext.getString(R.string.night_auto_value_disabled),
                mContext.getString(R.string.night_auto_value_min),
                mContext.getString(R.string.night_bright_low),
                mContext.getString(R.string.night_bright_midlow),
        });
        mPreference.setEntryValues(new CharSequence[]{
                String.valueOf(0),
                String.valueOf(1),
                String.valueOf(2),
                String.valueOf(3),
        });
    }

    @Override
    public final void updateState(Preference preference) {
        mPreference.setValue(String.valueOf(
                Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NIGHT_BRIGHTNESS_VALUE, 2,
                UserHandle.USER_CURRENT)));
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.NIGHT_BRIGHTNESS_VALUE,
                Integer.parseInt((String) newValue),
                UserHandle.USER_CURRENT);

        return true;
    }
}
