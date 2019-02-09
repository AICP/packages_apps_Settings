/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2017-2018 The LineageOS Project
 * Copyright (C) 2019 AICP
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

package com.android.settings.deviceinfo.aboutphone;

import android.content.Context;
import android.os.SystemProperties;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AicpVendorSecurityPatchView extends TextView {
    private static final String TAG = "AicpVendorSecurityPatchView";

    private static final String KEY_AOSP_VENDOR_SECURITY_PATCH =
            "ro.vendor.build.security_patch";

    private static final String KEY_AICP_VENDOR_SECURITY_PATCH =
            "ro.aicp.build.vendor_security_patch";

    public AicpVendorSecurityPatchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setText(getVendorSecurityPatchLevel());
    }

    private String getVendorSecurityPatchLevel() {
        String patchLevel = SystemProperties.get(KEY_AOSP_VENDOR_SECURITY_PATCH);

        if (patchLevel.isEmpty()) {
            patchLevel = SystemProperties.get(KEY_AICP_VENDOR_SECURITY_PATCH);
        }

        if (!patchLevel.isEmpty()) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchLevelDate = template.parse(patchLevel);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                patchLevel = DateFormat.format(format, patchLevelDate).toString();
            } catch (ParseException e) {
                // parsing failed, use raw string
            }
        }
        return patchLevel;
    }

}
