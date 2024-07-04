/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.Utils;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants;

/**
 * The controller of the hearing device call routing list preference.
 */
public class HearingDeviceCallRoutingPreferenceController extends
        HearingDeviceAudioRoutingBasePreferenceController {
    public HearingDeviceCallRoutingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Utils.isVoiceCapable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    protected int[] getSupportedAttributeList() {
        return HearingAidAudioRoutingConstants.CALL_ROUTING_ATTRIBUTES;
    }

    @Override
    protected void saveRoutingValue(Context context, int routingValue) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.HEARING_AID_CALL_ROUTING, routingValue);
    }

    @Override
    protected int restoreRoutingValue(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.HEARING_AID_CALL_ROUTING,
                HearingAidAudioRoutingConstants.RoutingValue.AUTO);
    }
}
