/*
 * Copyright (C) 2014 Dokdo-Project
 *               2014 AICP
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

package com.android.settings.aicp;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class OverscrollEffects extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener {

    private static final String OVERSCROLL_GLOW_COLOR = "overscroll_glow_color";
    private static final String OVERSCROLL_PREF = "overscroll_effect";
    private static final String OVERSCROLL_WEIGHT_PREF = "overscroll_weight";

    ColorPickerPreference mOverScrollGlowColor;
    private ListPreference mOverscrollPref;
    private ListPreference mOverscrollWeightPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.aicp_overscroll_effects);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mOverScrollGlowColor = (ColorPickerPreference) findPreference(OVERSCROLL_GLOW_COLOR);
        mOverScrollGlowColor.setOnPreferenceChangeListener(this);
        int defaultColor = Color.rgb(255, 255, 255);
        int intColor = Settings.System.getInt(resolver,
                Settings.System.OVERSCROLL_GLOW_COLOR, defaultColor);
        mOverScrollGlowColor.setNewPreviewColor(intColor);

        mOverscrollPref = (ListPreference) findPreference(OVERSCROLL_PREF);
        int overscrollEffect = Settings.System.getInt(resolver,
                Settings.System.OVERSCROLL_EFFECT, 1);
        mOverscrollPref.setValue(String.valueOf(overscrollEffect));
        mOverscrollPref.setOnPreferenceChangeListener(this);

        mOverscrollWeightPref = (ListPreference) findPreference(OVERSCROLL_WEIGHT_PREF);
        int overscrollWeight = Settings.System.getInt(resolver,
                Settings.System.OVERSCROLL_WEIGHT, 5);
        mOverscrollWeightPref.setValue(String.valueOf(overscrollWeight));
        mOverscrollWeightPref.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (preference == mOverScrollGlowColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.OVERSCROLL_GLOW_COLOR, intHex);
            return true;
        } else if (preference == mOverscrollPref) {
            int overscrollEffect = Integer.valueOf((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.OVERSCROLL_EFFECT, overscrollEffect);
            return true;
        } else if (preference == mOverscrollWeightPref) {
            int overscrollWeight = Integer.valueOf((String)newValue);
            Settings.System.putInt(resolver,
                    Settings.System.OVERSCROLL_WEIGHT, overscrollWeight);
            return true;
        }
        return false;
    }
}
