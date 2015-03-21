/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.WirelessSettings;
import com.android.settings.dashboard.GenericSwitchToggle;
import com.android.settings.search.Index;
import com.android.settings.widget.SwitchBar;

import java.util.concurrent.atomic.AtomicBoolean;

public class WifiEnabler extends GenericSwitchToggle  {
    private AtomicBoolean mConnected = new AtomicBoolean(false);

    private WifiManager mWifiManager;
    private IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                if (!mConnected.get()) {
                    handleStateChanged(WifiInfo.getDetailedStateOf((SupplicantState)
                            intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                        WifiManager.EXTRA_NETWORK_INFO);
                mConnected.set(info.isConnected());
                handleStateChanged(info.getDetailedState());
            }
        }
    };

    private static final String EVENT_DATA_IS_WIFI_ON = "is_wifi_on";
    private static final int EVENT_UPDATE_INDEX = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_INDEX:
                    final boolean isWiFiOn = msg.getData().getBoolean(EVENT_DATA_IS_WIFI_ON);
                    Index.getInstance(mContext).updateFromClassNameResource(
                            WifiSettings.class.getName(), true, isWiFiOn);
                    break;
            }
        }
    };

    public WifiEnabler(Context context, SwitchBar switchBar) {
        super(context, switchBar);

        init();
        setupSwitches();
    }

    public WifiEnabler(Context context, Switch switch_) {
        super(context, switch_);

        init();
        setupSwitches();
    }

    private void init() {
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // The order matters! We really should not depend on this. :(
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    private void setupSwitches() {
        final int state = mWifiManager.getWifiState();
        handleWifiStateChanged(state);
        if (mSwitchBar != null) {
            mSwitchBar.show();
        }
    }


    @Override
    public void resume(Context context) {
        super.resume(context);
        // Wi-Fi state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void pause() {
        super.pause();
        mContext.unregisterReceiver(mReceiver);
    }

    private void handleWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                setChecked(true);
                setEnabled(true);
                updateSearchIndex(true);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                setChecked(false);
                setEnabled(true);
                updateSearchIndex(false);
                break;
            default:
                setChecked(false);
                setEnabled(false);
                updateSearchIndex(false);
        }
    }

    private void updateSearchIndex(boolean isWiFiOn) {
        mHandler.removeMessages(EVENT_UPDATE_INDEX);

        Message msg = new Message();
        msg.what = EVENT_UPDATE_INDEX;
        msg.getData().putBoolean(EVENT_DATA_IS_WIFI_ON, isWiFiOn);
        mHandler.sendMessage(msg);
    }


    private void handleStateChanged(@SuppressWarnings("unused") NetworkInfo.DetailedState state) {
        // After the refactoring from a CheckBoxPreference to a Switch, this method is useless since
        // there is nowhere to display a summary.
        // This code is kept in case a future change re-introduces an associated text.
        /*
        // WifiInfo is valid if and only if Wi-Fi is enabled.
        // Here we use the state of the switch as an optimization.
        if (state != null && mSwitch.isChecked()) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            if (info != null) {
                //setSummary(Summary.get(mContext, info.getSSID(), state));
            }
        }
        */
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }
        // Show toast message if Wi-Fi is not allowed in airplane mode
        if (isChecked && !WirelessSettings.isRadioAllowed(mContext, Settings.Global.RADIO_WIFI)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            setChecked(false);
            return;
        }

        // Disable tethering if enabling Wifi
        int wifiApState = mWifiManager.getWifiApState();
        if (isChecked && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
            mWifiManager.setWifiApEnabled(null, false);
        }

        if (!mWifiManager.setWifiEnabled(isChecked)) {
            // Error
            setEnabled(true);

            Toast.makeText(mContext, R.string.wifi_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        super.onCheckedChanged(buttonView, isChecked);
    }
}
