/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2017 AICP
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
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class DevelopmentTiles {

    /**
     * Tile to control the "Disable HW overlays" developer setting
     */
    public static class DisableHwOverlays extends TileService {
        @Override
        public void onStartListening() {
            super.onStartListening();
            refresh();
        }

        public void refresh() {
            boolean disableOverlaysEnabled = false;
            // Read current state
            try {
                IBinder flinger = ServiceManager.getService("SurfaceFlinger");
                if (flinger != null) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                    flinger.transact(1010, data, reply, 0);
                    @SuppressWarnings("unused")
                    int showCpu = reply.readInt();
                    @SuppressWarnings("unused")
                    int enableGL = reply.readInt();
                    @SuppressWarnings("unused")
                    int showUpdates = reply.readInt();
                    @SuppressWarnings("unused")
                    int showBackground = reply.readInt();
                    int disableOverlays = reply.readInt();
                    disableOverlaysEnabled = disableOverlays != 0;
                    reply.recycle();
                    data.recycle();
                }
            } catch (RemoteException ex) {
            }
            getQsTile().setState(disableOverlaysEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            getQsTile().updateTile();
        }

        @Override
        public void onClick() {
            // Write new state
            try {
                IBinder flinger = ServiceManager.getService("SurfaceFlinger");
                if (flinger != null) {
                    Parcel data = Parcel.obtain();
                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                    final int disableOverlays = getQsTile().getState() == Tile.STATE_INACTIVE ? 1 : 0;
                    data.writeInt(disableOverlays);
                    flinger.transact(1008, data, null, 0);
                    data.recycle();

                    refresh();
                }
            } catch (RemoteException ex) {
            }
        }
    }
}
