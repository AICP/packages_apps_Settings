/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.cyanogenmod;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import android.provider.SearchIndexableResource;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.qs.QSTiles;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.util.Helpers;

import java.util.ArrayList;
import java.util.List;

import java.util.Locale;

public class NotificationDrawerSettings extends SettingsPreferenceFragment implements Indexable,
            OnPreferenceChangeListener {

    public static final String TAG = "QsSettings";

    private static final String PREF_QUICK_PULLDOWN = "quick_pulldown";
    private static final String PREF_SMART_PULLDOWN = "smart_pulldown";
    private static final String PREF_BLOCK_ON_SECURE_KEYGUARD = "block_on_secure_keyguard";
    private static final String PREF_QS_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";
    private static final String ENABLE_TASK_MANAGER = "enable_task_manager";

    private Preference mQSTiles;
    private ListPreference mQuickPulldown;
    private ListPreference mSmartPulldown;
    private SwitchPreference mBlockOnSecureKeyguard;
    private SwitchPreference mBrightnessSlider;
    private SwitchPreference mEnableTaskManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.notification_drawer_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mQSTiles = findPreference("qs_order");

        mQuickPulldown = (ListPreference) findPreference(PREF_QUICK_PULLDOWN);
        mSmartPulldown = (ListPreference) findPreference(PREF_SMART_PULLDOWN);

        // Quick Pulldown
        mQuickPulldown.setOnPreferenceChangeListener(this);
        int statusQuickPulldown = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 1);
        mQuickPulldown.setValue(String.valueOf(statusQuickPulldown));
        updateQuickPulldownSummary(statusQuickPulldown);

        // Smart Pulldown
        mSmartPulldown.setOnPreferenceChangeListener(this);
        int smartPulldown = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_SMART_PULLDOWN, 0);
        mSmartPulldown.setValue(String.valueOf(smartPulldown));
        updateSmartPulldownSummary(smartPulldown);

        final LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
        mBlockOnSecureKeyguard = (SwitchPreference) findPreference(PREF_BLOCK_ON_SECURE_KEYGUARD);
        if (lockPatternUtils.isSecure()) {
            mBlockOnSecureKeyguard.setChecked(Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.STATUS_BAR_LOCKED_ON_SECURE_KEYGUARD, 1) == 1);
            mBlockOnSecureKeyguard.setOnPreferenceChangeListener(this);
        } else {
            prefs.removePreference(mBlockOnSecureKeyguard);
        }

        // Brightness slider
        mBrightnessSlider = (SwitchPreference) prefs.findPreference(PREF_QS_SHOW_BRIGHTNESS_SLIDER);
        mBrightnessSlider.setChecked(Settings.Secure.getInt(getActivity().getContentResolver(),
            Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1) == 1);
        mBrightnessSlider.setOnPreferenceChangeListener(this);
        int brightnessSlider = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1);
        updateBrightnessSliderSummary(brightnessSlider);

        // Task manager
        mEnableTaskManager = (SwitchPreference) prefs.findPreference(ENABLE_TASK_MANAGER);
        mEnableTaskManager.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ENABLE_TASK_MANAGER, 0) == 1));

    }

    @Override
    public void onResume() {
        super.onResume();

        int qsTileCount = QSTiles.determineTileCount(getActivity());
        mQSTiles.setSummary(getResources().getQuantityString(R.plurals.qs_tiles_summary,
                    qsTileCount, qsTileCount));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQuickPulldown) {
            int statusQuickPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                    statusQuickPulldown);
            updateQuickPulldownSummary(statusQuickPulldown);
            return true;
        } else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.QS_SMART_PULLDOWN,
                    smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
            return true;
        } else if (preference == mBlockOnSecureKeyguard) {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.STATUS_BAR_LOCKED_ON_SECURE_KEYGUARD,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mBrightnessSlider) {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER,
                    (Boolean) newValue ? 1 : 0);
            int brightnessSlider = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1);
            updateBrightnessSliderSummary(brightnessSlider);
            return true;
        } else if  (preference == mEnableTaskManager) {
            boolean checked = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_TASK_MANAGER, checked ? 1:0);
            Helpers.restartSystemUI();
            return true;
        }
        return false;
    }

    private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = null;
            switch (value) {
                case 1:
                    type = res.getString(R.string.smart_pulldown_dismissable);
                    break;
                case 2:
                    type = res.getString(R.string.smart_pulldown_persistent);
                    break;
                default:
                    type = res.getString(R.string.smart_pulldown_all);
                    break;
            }
            // Remove title capitalized formatting
            type = type.toLowerCase();
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }

    private void updateQuickPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else {
            Locale l = Locale.getDefault();
            boolean isRtl = TextUtils.getLayoutDirectionFromLocale(l) == View.LAYOUT_DIRECTION_RTL;
            String direction = res.getString(value == 2
                    ? (isRtl ? R.string.quick_pulldown_right : R.string.quick_pulldown_left)
                    : (isRtl ? R.string.quick_pulldown_left : R.string.quick_pulldown_right));
            mQuickPulldown.setSummary(res.getString(R.string.summary_quick_pulldown, direction));
        }
    }

    private void updateBrightnessSliderSummary(int value) {
        String summary = value != 0
                ? getResources().getString(R.string.qs_brightness_slider_enabled)
                : getResources().getString(R.string.qs_brightness_slider_disabled);
        mBrightnessSlider.setSummary(summary);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.notification_drawer_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return new ArrayList<String>();
                }
            };
}
