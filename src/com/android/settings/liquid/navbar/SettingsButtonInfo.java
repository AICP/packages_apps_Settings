/*
 * Copyright (C) 2015 The Fusion Project
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

package com.android.settings.liquid.navbar;

import android.text.TextUtils;

public class SettingsButtonInfo {
    String singleAction, doubleTapAction, longPressAction, iconUri;

    public SettingsButtonInfo(String singleTap, String doubleTap, String longPress, String uri) {
        this.singleAction = singleTap;
        this.doubleTapAction = doubleTap;
        this.longPressAction = longPress;
        this.iconUri = uri;

        if (singleAction == null) singleAction = "";
        if (doubleTapAction == null) doubleTapAction = "";
        if (longPressAction == null) longPressAction = "";
        if (iconUri == null) iconUri = "";
    }

    @Override
    public String toString() {
        return singleAction + "," + doubleTapAction + "," + longPressAction + "," + iconUri;
    }
}
