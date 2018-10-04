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
 * limitations under the License
 */

package com.android.settings.gzosp.buttons;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.System.KEY_HOME_LONG_PRESS_ACTION;

public class LongPressHomePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private Context mContext;
    private ListPreference mPref;
    private final String mKey;

    public LongPressHomePreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mKey = key;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPref = (ListPreference) screen.findPreference(getPreferenceKey());
        if (mPref == null) return;
        int defaultBehavior = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_longPressOnHomeKeyBehavior);
        int value = Settings.System.getInt(mContext.getContentResolver(), KEY_HOME_LONG_PRESS_ACTION, defaultBehavior);
        mPref.setValue(Integer.toString(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(), KEY_HOME_LONG_PRESS_ACTION, value);
        refreshSummary(preference);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        int value = Settings.System.getInt(mContext.getContentResolver(), KEY_HOME_LONG_PRESS_ACTION, 0);
        int index = mPref.findIndexOfValue(Integer.toString(value));
        return mPref.getEntries()[index];
    }
}
