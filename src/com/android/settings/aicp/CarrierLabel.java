/*
 * Copyright (C) 2012 Gummy
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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.Spannable;
import android.text.TextUtils;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class CarrierLabel extends SettingsPreferenceFragment  implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "CarrierLabel";

    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String PREF_NOTIFICATION_SHOW_WIFI_SSID = "notification_show_wifi_ssid";
    private static final String NOTIFICATION_SHORTCUTS_HIDE_CARRIER =
            "pref_notification_shortcuts_hide_carrier";
    private static final String STATUS_BAR_CARRIER = "status_bar_carrier";
    private static final String STATUS_BAR_CARRIER_COLOR = "status_bar_carrier_color";
    private static final String NO_KEYGUARD_CARRIER = "no_carrier_label";

    static final int DEFAULT_STATUS_CARRIER_COLOR = 0xffffffff;

    private ContentResolver mCr;
    private PreferenceScreen mPrefSet;

    private CheckBoxPreference mHideCarrier;
    private CheckBoxPreference mStatusBarCarrier;
    private ColorPickerPreference mCarrierColorPicker;
    private CheckBoxPreference mNoKeyguardCarrier;

    Preference mCustomLabel;
    String mCustomLabelText = null;
    Context mContext;
    CheckBoxPreference mShowWifiName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.aicp_carrier_label_prefs);
        PreferenceScreen prefScreen = getPreferenceScreen();

        mCr = getContentResolver();
        mPrefSet = getPreferenceScreen();

        int intColor;
        String hexColor;

        // Custom Carrier Label Text
        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        // Show Wifi Network Name
        mShowWifiName = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_SHOW_WIFI_SSID);
            mShowWifiName.setOnPreferenceChangeListener(this);

        // MIUI-like carrier Label
        mStatusBarCarrier = (CheckBoxPreference) findPreference(STATUS_BAR_CARRIER);
        mStatusBarCarrier.setChecked((Settings.System.getInt(mCr,
                Settings.System.STATUS_BAR_CARRIER, 0) == 1));

        // MIUI-like carrier Label color
        mCarrierColorPicker = (ColorPickerPreference) mPrefSet.findPreference(STATUS_BAR_CARRIER_COLOR);
        mCarrierColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CARRIER_COLOR, DEFAULT_STATUS_CARRIER_COLOR);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mCarrierColorPicker.setSummary(hexColor);
        mCarrierColorPicker.setNewPreviewColor(intColor);

        // Hide Carrier Label
        mHideCarrier = (CheckBoxPreference) mPrefSet.findPreference(
                NOTIFICATION_SHORTCUTS_HIDE_CARRIER);
        mHideCarrier.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NOTIFICATION_SHORTCUTS_HIDE_CARRIER, 0, UserHandle.USER_CURRENT_OR_SELF) == 1);
        mHideCarrier.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                Settings.System.putInt(mCr, Settings.System.NOTIFICATION_SHORTCUTS_HIDE_CARRIER, (Boolean) newValue ? 1 : 0);
                return true;
            }
        });

        // Hide Carrier Label in keyguard
        mNoKeyguardCarrier = (CheckBoxPreference) mPrefSet.findPreference(
                NO_KEYGUARD_CARRIER);
        mNoKeyguardCarrier.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NO_CARRIER_LABEL, 0, UserHandle.USER_CURRENT_OR_SELF) == 1);
        mNoKeyguardCarrier.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                Settings.System.putInt(mCr, Settings.System.NO_CARRIER_LABEL, (Boolean) newValue ? 1 : 0);
                return true;
            }
        });
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
       if (preference == mStatusBarCarrier) {
           Settings.System.putInt(mCr, Settings.System.STATUS_BAR_CARRIER, mStatusBarCarrier.isChecked() ? 1 : 0);
           return true;
       } else if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);

            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settins.LABEL_CHANGED");
                    getActivity().sendBroadcast(i);
                    Helpers.restartSystemUI();
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mCarrierColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_CARRIER_COLOR, intHex);
            return true;
        } else if (preference == mShowWifiName) {
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.NOTIFICATION_SHOW_WIFI_SSID,
                    ((CheckBoxPreference)preference).isChecked() ? 0 : 1);
            return true;
        }
        return false;
    }

}
