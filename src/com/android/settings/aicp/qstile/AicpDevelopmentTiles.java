/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2017-2018 AICP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.settings.aicp.qstile;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.android.settings.qstile.DevelopmentTiles;

public abstract class AicpDevelopmentTiles extends DevelopmentTiles {

    /**
     * Tile to control the "Show touches" developer setting
     */
    public static class ShowTouches extends AicpDevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            return Settings.System.getInt(getContentResolver(),
                        Settings.System.SHOW_TOUCHES, 0) != 0;
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SHOW_TOUCHES,
                    isEnabled ? 1 : 0);
        }
    }
}
