/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.Fragment;
import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.deviceinfo.SecurityDialogFragment;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SecurityDialogPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String TAG = "SecurityDialogTag";
    private static final String KEY_SECURITY_DIALOG = "security_dialog";

    public static String mHonorPatch;

    private final Fragment mHost;

    public SecurityDialogPreferenceController(Context context, Fragment host) {
        super(context);
        mHost = host;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SECURITY_DIALOG;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_SECURITY_DIALOG);
        mHonorPatch = SystemProperties.get("ro.vendor.override.security_patch", "");
        String deviceCheck = SystemProperties.get("ro.du.version");
        if (pref == null) return;
        pref.setSummary(deviceCheck.contains("berkeley") ? getSecurityPatch() : DeviceInfoUtils.getSecurityPatch());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), KEY_SECURITY_DIALOG)) {
            SecurityDialogFragment fragment = SecurityDialogFragment.newInstance();
            fragment.show(mHost.getFragmentManager(), SecurityDialogFragment.TAG);
            return true;
        }
        return false;
    }

    public static String getSecurityPatch() {
        if (!"".equals(mHonorPatch)) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(mHonorPatch);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                mHonorPatch = DateFormat.format(format, patchDate).toString();
            } catch (ParseException e) {
                // broken parse; fall through and use the raw string
            }
            return mHonorPatch;
        } else {
            return null;
        }
    }
}
