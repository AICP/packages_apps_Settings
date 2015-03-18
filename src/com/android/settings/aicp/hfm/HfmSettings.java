/*
 * Copyright (C) 2014 Android Ice Cold Project
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

package com.android.settings.aicp.hfm;

import java.io.IOException;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.View;

import com.android.settings.aicp.hfm.FetchHosts;
import com.android.settings.aicp.hfm.HfmHelpers;
import com.android.settings.aicp.hfm.HfmWhitelist;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class HfmSettings extends SettingsPreferenceFragment {

    private static final String TAG = "HfmSettings";
    private static final String HFM_DISABLE_ADS = "hfm_disable_ads";
    private static final String KEY_HFM_WHITELIST = "hfm_whitelist";

    public static ProgressDialog pd;

    ConnectivityManager connMgr;

    public static SwitchPreference mHfmDisableAds;
    private Preference mHfmWhitelist;
    Preference mHfmUpdateHosts;

    Context context;

    public static Resources res;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        res = getResources();
        pd = new ProgressDialog(context);
        pd.setCancelable(false);
        pd.setMessage(res.getString(R.string.hfm_dialog_wait));
        connMgr = (ConnectivityManager) getSystemService(context.CONNECTIVITY_SERVICE);

        addPreferencesFromResource(R.xml.aicp_hfm_settings);
        ContentResolver resolver = context.getContentResolver();
        PreferenceScreen prefScreen = getPreferenceScreen();

        mHfmDisableAds = (SwitchPreference) findPreference(HFM_DISABLE_ADS);
        mHfmDisableAds.setChecked((Settings.System.getInt(resolver,
            Settings.System.HFM_DISABLE_ADS, 0) == 1));
        mHfmUpdateHosts = prefScreen.findPreference("hfm_update_hosts");

        mHfmWhitelist = (Preference) findPreference(KEY_HFM_WHITELIST);

        if (mHfmDisableAds.isChecked()) {
            mHfmWhitelist.setEnabled(true);
        } else {
            mHfmWhitelist.setEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if  (preference == mHfmDisableAds) {
            boolean checked = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.HFM_DISABLE_ADS, checked ? 1:0);
            if (checked) {
                mHfmWhitelist.setEnabled(true);
            } else {
                mHfmWhitelist.setEnabled(false);
            }
            HfmHelpers.checkStatus(getActivity());
        } else if (preference == mHfmUpdateHosts) {
            try {
                HfmHelpers.checkConnectivity(context, connMgr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if  (preference == mHfmWhitelist) {
             startActivity(new Intent(getActivity(), HfmWhitelist.class));
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }
}
