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

package com.android.settings.bluetooth;


import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import android.util.Pair;

import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * This class adds a header with device name and status (connected/disconnected, etc.).
 */
public class BluetoothDetailsHeaderController extends BluetoothDetailsController {
    private static final String KEY_DEVICE_HEADER = "bluetooth_device_header";

    private EntityHeaderController mHeaderController;

    public BluetoothDetailsHeaderController(Context context, PreferenceFragment fragment,
            CachedBluetoothDevice device, Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
    }

    @Override
    protected void init(PreferenceScreen screen) {
        final LayoutPreference headerPreference =
                (LayoutPreference) screen.findPreference(KEY_DEVICE_HEADER);
        mHeaderController = EntityHeaderController.newInstance(mFragment.getActivity(), mFragment,
                headerPreference.findViewById(R.id.entity_header));
        screen.addPreference(headerPreference);
    }

    protected void setHeaderProperties() {
        Pair<Integer, String> pair = Utils.getBtClassDrawableWithDescription
                (mContext.getResources(), mCachedDevice);
        String summaryText = mCachedDevice.getConnectionSummary();
        mHeaderController.setLabel(mCachedDevice.getName());
        mHeaderController.setIcon(mContext.getDrawable(pair.first));
        mHeaderController.setIconContentDescription(pair.second);
        mHeaderController.setSummary(summaryText);
    }

    @Override
    protected void refresh() {
        setHeaderProperties();
        mHeaderController.done(mFragment.getActivity(), true /* rebindActions */);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEVICE_HEADER;
    }
}
