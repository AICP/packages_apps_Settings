/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.aicp.stats;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import com.android.settings.R;

public class PreviewActivity extends PreferenceActivity {

	private static final String UNIQUE_ID = "preview_id";
	private static final String DEVICE = "preview_device";
	private static final String VERSION = "preview_version";
	private static final String COUNTRY = "preview_country";
	private static final String CARRIER = "preview_carrier";
	private static final String ROMVERSION = "preview_romversion";

	private Preference mId;
	private Preference mDevice;
	private Preference mVersion;
	private Preference mCountry;
	private Preference mCarrier;
	private Preference mRomVersion;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preview_data);
		PreferenceScreen prefSet = getPreferenceScreen();
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mId = prefSet.findPreference(UNIQUE_ID);
		mDevice = prefSet.findPreference(DEVICE);
		mVersion = prefSet.findPreference(VERSION);
		mCountry = prefSet.findPreference(COUNTRY);
		mCarrier = prefSet.findPreference(CARRIER);
		mRomVersion = prefSet.findPreference(ROMVERSION);

		mId.setSummary(Utilities.getUniqueID(getApplicationContext()));
		mDevice.setSummary(Utilities.getDevice());
		mVersion.setSummary(Utilities.getModVersion());
		mCountry.setSummary(Utilities.getCountryCode(getApplicationContext()));
		mCarrier.setSummary(Utilities.getCarrier(getApplicationContext()));
		mRomVersion.setSummary(Utilities.getRomVersion());
	}

}
