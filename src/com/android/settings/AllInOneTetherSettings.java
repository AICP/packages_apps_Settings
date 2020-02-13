/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings;

import static android.net.ConnectivityManager.ACTION_TETHER_STATE_CHANGED;
import static android.net.ConnectivityManager.TETHERING_WIFI;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_CHANGED_ACTION;

import static com.android.settings.network.TetherEnabler.KEY_ENABLE_WIFI_TETHERING;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.core.FeatureFlags;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.network.BluetoothTetherPreferenceController;
import com.android.settings.network.TetherEnabler;
import com.android.settings.network.UsbTetherPreferenceController;
import com.android.settings.network.WifiTetherDisablePreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.wifi.tether.WifiTetherApBandPreferenceController;
import com.android.settings.wifi.tether.WifiTetherAutoOffPreferenceController;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;
import com.android.settings.wifi.tether.WifiTetherPasswordPreferenceController;
import com.android.settings.wifi.tether.WifiTetherSSIDPreferenceController;
import com.android.settings.wifi.tether.WifiTetherSecurityPreferenceController;
import com.android.settingslib.TetherUtil;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Displays preferences for all Tethering options.
 */
@SearchIndexable
public final class AllInOneTetherSettings extends RestrictedDashboardFragment
        implements DataSaverBackend.Listener,
        WifiTetherBasePreferenceController.OnTetherConfigUpdateListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // TODO(b/148622133): Should clean up the postfix once this fragment replaced TetherSettings.
    public static final String DEDUP_POSTFIX = "_2";

    @VisibleForTesting
    static final String KEY_WIFI_TETHER_NETWORK_NAME = "wifi_tether_network_name" + DEDUP_POSTFIX;
    @VisibleForTesting
    static final String KEY_WIFI_TETHER_NETWORK_PASSWORD =
            "wifi_tether_network_password" + DEDUP_POSTFIX;
    @VisibleForTesting
    static final String KEY_WIFI_TETHER_AUTO_OFF = "wifi_tether_auto_turn_off" + DEDUP_POSTFIX;
    @VisibleForTesting
    static final String KEY_WIFI_TETHER_NETWORK_AP_BAND =
            "wifi_tether_network_ap_band" + DEDUP_POSTFIX;
    @VisibleForTesting
    static final String KEY_WIFI_TETHER_SECURITY = "wifi_tether_security" + DEDUP_POSTFIX;

    private static final String KEY_DATA_SAVER_FOOTER = "disabled_on_data_saver" + DEDUP_POSTFIX;
    private static final String KEY_WIFI_TETHER_GROUP = "wifi_tether_settings_group";
    private static final int EXPANDED_CHILD_COUNT_WITH_SECURITY_NON = 2;
    private static final int EXPANDED_CHILD_COUNT_DEFAULT = 3;
    private static final String TAG = "AllInOneTetherSettings";

    private boolean mUnavailable;

    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    private Preference mDataSaverFooter;

    private WifiManager mWifiManager;
    private boolean mRestartWifiApAfterConfigChange;
    private final AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<>();

    private WifiTetherSSIDPreferenceController mSSIDPreferenceController;
    private WifiTetherPasswordPreferenceController mPasswordPreferenceController;
    private WifiTetherApBandPreferenceController mApBandPreferenceController;
    private WifiTetherSecurityPreferenceController mSecurityPreferenceController;
    private PreferenceGroup mWifiTetherGroup;
    private SharedPreferences mSharedPreferences;
    private boolean mWifiTetherChosen;
    private TetherEnabler mTetherEnabler;

    private final BroadcastReceiver mTetherChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG,
                        "updating display config due to receiving broadcast action " + action);
            }
            updateDisplayWithNewConfig();
            if (TextUtils.equals(action, ACTION_TETHER_STATE_CHANGED)) {
                restartWifiTetherIfNeed(mWifiManager.getWifiApState());
            } else if (TextUtils.equals(action, WIFI_AP_STATE_CHANGED_ACTION)) {
                restartWifiTetherIfNeed(intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, 0));
            }
        }

        private void restartWifiTetherIfNeed(int state) {
            if (state == WifiManager.WIFI_AP_STATE_DISABLED
                    && mWifiTetherChosen
                    && mRestartWifiApAfterConfigChange) {
                mRestartWifiApAfterConfigChange = false;
                mTetherEnabler.startTethering(TETHERING_WIFI);
            }
        }
    };

    private final BluetoothProfile.ServiceListener mProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    mBluetoothPan.set((BluetoothPan) proxy);
                }

                public void onServiceDisconnected(int profile) {
                    mBluetoothPan.set(null);
                }
            };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TETHER;
    }

    public AllInOneTetherSettings() {
        super(UserManager.DISALLOW_CONFIG_TETHERING);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mSharedPreferences =
                context.getSharedPreferences(TetherEnabler.SHARED_PREF, Context.MODE_PRIVATE);

        mSSIDPreferenceController = use(WifiTetherSSIDPreferenceController.class);
        mSecurityPreferenceController = use(WifiTetherSecurityPreferenceController.class);
        mPasswordPreferenceController = use(WifiTetherPasswordPreferenceController.class);
        mApBandPreferenceController = use(WifiTetherApBandPreferenceController.class);
        getSettingsLifecycle().addObserver(use(UsbTetherPreferenceController.class));
        getSettingsLifecycle().addObserver(use(BluetoothTetherPreferenceController.class));
        getSettingsLifecycle().addObserver(use(WifiTetherDisablePreferenceController.class));
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();
        mDataSaverFooter = findPreference(KEY_DATA_SAVER_FOOTER);
        mWifiTetherGroup = findPreference(KEY_WIFI_TETHER_GROUP);

        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted()) {
            mUnavailable = true;
            return;
        }

        mDataSaverBackend.addListener(this);

        // Set initial state based on Data Saver mode.
        onDataSaverChanged(mDataSaverBackend.isDataSaverEnabled());

        // Set initial state based on SharedPreferences value.
        onSharedPreferenceChanged(mSharedPreferences, KEY_ENABLE_WIFI_TETHERING);

        // TODO(b/147325229): Hide advanced settings like security and ap band.
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mUnavailable) {
            return;
        }
        // Assume we are in a SettingsActivity. This is only safe because we currently use
        // SettingsActivity as base for all preference fragments.
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(activity.getApplicationContext(), mProfileServiceListener,
                    BluetoothProfile.PAN);
        }
        final SwitchBar switchBar = activity.getSwitchBar();
        mTetherEnabler = new TetherEnabler(activity,
                new SwitchBarController(switchBar), mBluetoothPan);
        getSettingsLifecycle().addObserver(mTetherEnabler);
        switchBar.show();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.tethering_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        final Context context = getContext();
        if (context != null) {
            IntentFilter filter = new IntentFilter(ACTION_TETHER_STATE_CHANGED);
            filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
            context.registerReceiver(mTetherChangeReceiver, filter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        final Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(mTetherChangeReceiver);
        }
    }

    @Override
    public void onDestroy() {
        mDataSaverBackend.remListener(this);
        super.onDestroy();
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        mDataSaverFooter.setVisible(mDataSaverEnabled);
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
        // Do nothing
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
        // Do nothing
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            WifiTetherBasePreferenceController.OnTetherConfigUpdateListener listener) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(
                new WifiTetherSSIDPreferenceController(context, listener));
        controllers.add(
                new WifiTetherPasswordPreferenceController(context, listener));
        controllers.add(
                new WifiTetherApBandPreferenceController(context, listener));
        controllers.add(
                new WifiTetherSecurityPreferenceController(context, listener));
        controllers.add(
                new WifiTetherAutoOffPreferenceController(context, KEY_WIFI_TETHER_AUTO_OFF));

        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.all_tether_prefs;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onExpandButtonClick() {
        super.onExpandButtonClick();
        // TODO(b/147325229): Display hidden advanced settings like security and ap band.
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    @Override
    public void onTetherConfigUpdated(AbstractPreferenceController controller) {
        final SoftApConfiguration config = buildNewConfig();
        mPasswordPreferenceController.updateVisibility(config.getSecurityType());
        mWifiManager.setSoftApConfiguration(config);

        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Wifi AP config changed while enabled, stop and restart");
            }
            mRestartWifiApAfterConfigChange = true;
            mTetherEnabler.stopTethering(TETHERING_WIFI);
        }

        if (controller instanceof WifiTetherSecurityPreferenceController) {
            reConfigInitialExpandedChildCount();
        }
    }

    private SoftApConfiguration buildNewConfig() {
        final SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder();
        final int securityType = mSecurityPreferenceController.getSecurityType();
        configBuilder.setSsid(mSSIDPreferenceController.getSSID());
        if (securityType == SoftApConfiguration.SECURITY_TYPE_WPA2_PSK) {
            configBuilder.setPassphrase(
                    mPasswordPreferenceController.getPasswordValidated(securityType),
                    SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
        }
        configBuilder.setBand(mApBandPreferenceController.getBandIndex());
        return configBuilder.build();
    }

    private void updateDisplayWithNewConfig() {
        mSSIDPreferenceController.updateDisplay();
        mSecurityPreferenceController.updateDisplay();
        mPasswordPreferenceController.updateDisplay();
        mApBandPreferenceController.updateDisplay();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(key, KEY_ENABLE_WIFI_TETHERING)) {
            mWifiTetherChosen = sharedPreferences.getBoolean(KEY_ENABLE_WIFI_TETHERING, true);
            mWifiTetherGroup.setVisible(mWifiTetherChosen);
            reConfigInitialExpandedChildCount();
        }
    }

    private void reConfigInitialExpandedChildCount() {
        getPreferenceScreen().setInitialExpandedChildrenCount(getInitialExpandedChildCount());
    }

    @Override
    public int getInitialExpandedChildCount() {
        if (!mWifiTetherChosen) {
            // Expand all preferences in the screen.
            return getPreferenceScreen().getPreferenceCount();
        }

        if (mSecurityPreferenceController == null) {
            return EXPANDED_CHILD_COUNT_DEFAULT;
        }

        return (mSecurityPreferenceController.getSecurityType()
                == SoftApConfiguration.SECURITY_TYPE_OPEN)
                ? EXPANDED_CHILD_COUNT_WITH_SECURITY_NON : EXPANDED_CHILD_COUNT_DEFAULT;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.all_tether_prefs) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);

                    if (!TetherUtil.isTetherAvailable(context)) {
                        keys.add(KEY_WIFI_TETHER_NETWORK_NAME);
                        keys.add(KEY_WIFI_TETHER_NETWORK_PASSWORD);
                        keys.add(KEY_WIFI_TETHER_AUTO_OFF);
                        keys.add(KEY_WIFI_TETHER_NETWORK_AP_BAND);
                        keys.add(KEY_WIFI_TETHER_SECURITY);
                    }
                    return keys;
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return FeatureFlagUtils.isEnabled(context, FeatureFlags.TETHER_ALL_IN_ONE);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /*listener*/);
                }
            };
}
