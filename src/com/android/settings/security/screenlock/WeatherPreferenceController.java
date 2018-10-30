/*
 * Copyright (C) 2018 The PixelExperience Project
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

package com.android.settings.security.screenlock;

import android.content.Context;
import com.android.settings.core.BasePreferenceController;

import com.android.internal.util.aicp.weather.WeatherClient;

public class WeatherPreferenceController extends BasePreferenceController {

    public WeatherPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return WeatherClient.isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

}
