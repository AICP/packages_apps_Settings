/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.Nullable;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.icu.text.NumberFormat;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.format.Formatter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.preference.Preference;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.LayoutPreference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was consumed
 * since the last time it was unplugged.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class PowerUsageSummary extends PowerUsageBase implements OnLongClickListener,
        BatteryTipPreferenceController.BatteryTipListener {

    static final String TAG = "PowerUsageSummary";

    private static final String KEY_BATTERY_HEADER = "battery_header";
    private static final int BATTERY_ANIMATION_DURATION_MS_PER_LEVEL = 30;

    @VisibleForTesting
    static final String ARG_BATTERY_LEVEL = "key_battery_level";

    private static final String KEY_SCREEN_USAGE = "screen_usage";
    private static final String KEY_BATTERY_TEMP = "battery_temp";
    private static final String KEY_TIME_SINCE_LAST_FULL_CHARGE = "last_full_charge";
    private static final String KEY_CURRENT_BATTERY_CAPACITY = "current_battery_capacity";
    private static final String KEY_DESIGNED_BATTERY_CAPACITY = "designed_battery_capacity";
    private static final String KEY_BATTERY_CHARGE_CYCLES = "battery_charge_cycles";
    private static final String KEY_BATTERY_HEALTH = "battery_health";
    private static final String KEY_BATTERY_TYPE = "battery_type";

    @VisibleForTesting
    static final int BATTERY_INFO_LOADER = 1;
    @VisibleForTesting
    static final int BATTERY_TIP_LOADER = 2;
    @VisibleForTesting
    static final int MENU_ADVANCED_BATTERY = Menu.FIRST + 1;
    static final int MENU_STATS_RESET = Menu.FIRST + 2;
    public static final int DEBUG_INFO_LOADER = 3;

    @VisibleForTesting
    int mBatteryLevel;
    @VisibleForTesting
    PowerGaugePreference mScreenUsagePref;
    @VisibleForTesting
    PowerGaugePreference mBatteryTempPref;
    @VisibleForTesting
    PowerGaugePreference mLastFullChargePref;
    @VisibleForTesting
    PowerUsageFeatureProvider mPowerFeatureProvider;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    LayoutPreference mBatteryLayoutPref;
    @VisibleForTesting
    BatteryInfo mBatteryInfo;

    ArrayList<PowerGaugePreference> pGPList;

    @VisibleForTesting
    PowerManager mPowerManager;
    @VisibleForTesting
    boolean mNeedUpdateBatteryTip;
    @VisibleForTesting
    BatteryTipPreferenceController mBatteryTipPreferenceController;

    @VisibleForTesting
    final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            restartBatteryInfoLoader();
        }
    };

    @VisibleForTesting
    LoaderManager.LoaderCallbacks<BatteryInfo> mBatteryInfoLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<BatteryInfo>() {

                @Override
                public Loader<BatteryInfo> onCreateLoader(int i, Bundle bundle) {
                    return new BatteryInfoLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<BatteryInfo> loader, BatteryInfo batteryInfo) {
                    updateHeaderPreference(batteryInfo);
                    mBatteryInfo = batteryInfo;
                    updateLastFullChargePreference();
                }

                @Override
                public void onLoaderReset(Loader<BatteryInfo> loader) {
                    // do nothing
                }
            };

    LoaderManager.LoaderCallbacks<List<BatteryInfo>> mBatteryInfoDebugLoaderCallbacks =
            new LoaderCallbacks<List<BatteryInfo>>() {
                @Override
                public Loader<List<BatteryInfo>> onCreateLoader(int i, Bundle bundle) {
                    return new DebugEstimatesLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<List<BatteryInfo>> loader,
                        List<BatteryInfo> batteryInfos) {
                    updateViews(batteryInfos);
                }

                @Override
                public void onLoaderReset(Loader<List<BatteryInfo>> loader) {
                }
            };

    protected void updateViews(List<BatteryInfo> batteryInfos) {
        final BatteryMeterView batteryView = mBatteryLayoutPref
                .findViewById(R.id.battery_header_icon);
        final TextView percentRemaining =
                mBatteryLayoutPref.findViewById(R.id.battery_percent);
        final TextView summary1 = mBatteryLayoutPref.findViewById(R.id.summary1);
        BatteryInfo oldInfo = batteryInfos.get(0);
        BatteryInfo newInfo = batteryInfos.get(1);
        percentRemaining.setText(Utils.formatPercentage(oldInfo.batteryLevel));

        // set the text to the old estimate (copied from battery info). Note that this
        // can sometimes say 0 time remaining because battery stats requires the phone
        // be unplugged for a period of time before being willing ot make an estimate.
        final String OldEstimateString = mPowerFeatureProvider.getOldEstimateDebugString(
                Formatter.formatShortElapsedTime(getContext(),
                        PowerUtil.convertUsToMs(oldInfo.remainingTimeUs)));
        final String NewEstimateString = mPowerFeatureProvider.getEnhancedEstimateDebugString(
                Formatter.formatShortElapsedTime(getContext(),
                        PowerUtil.convertUsToMs(newInfo.remainingTimeUs)));
        summary1.setText(OldEstimateString + "\n" + NewEstimateString);

        batteryView.setBatteryLevel(oldInfo.batteryLevel);
        batteryView.setCharging(!oldInfo.discharging);
    }

    private LoaderManager.LoaderCallbacks<List<BatteryTip>> mBatteryTipsCallbacks =
            new LoaderManager.LoaderCallbacks<List<BatteryTip>>() {

                @Override
                public Loader<List<BatteryTip>> onCreateLoader(int id, Bundle args) {
                    return new BatteryTipLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<List<BatteryTip>> loader,
                        List<BatteryTip> data) {
                    mBatteryTipPreferenceController.updateBatteryTips(data);
                }

                @Override
                public void onLoaderReset(Loader<List<BatteryTip>> loader) {

                }
            };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final SettingsActivity activity = (SettingsActivity) getActivity();

        mPowerManager = context.getSystemService(PowerManager.class);

        mBatteryTipPreferenceController = use(BatteryTipPreferenceController.class);
        mBatteryTipPreferenceController.setActivity(activity);
        mBatteryTipPreferenceController.setFragment(this);
        mBatteryTipPreferenceController.setBatteryTipListener(this::onBatteryTipHandled);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);

        pGPList = new ArrayList<PowerGaugePreference>();

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            if (getPreferenceScreen().getPreference(i) instanceof PowerGaugePreference) {
                  if (checkKey(getPreferenceScreen().getPreference(i).getKey())) {
                      pGPList.add((PowerGaugePreference) getPreferenceScreen().getPreference(i));
                  }
            }
        }

        initFeatureProvider();
        mBatteryLayoutPref = (LayoutPreference) findPreference(KEY_BATTERY_HEADER);

        mBatteryLevel = getContext().getResources().getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel) + 1;

        mScreenUsagePref = (PowerGaugePreference) findPreference(KEY_SCREEN_USAGE);
        mBatteryTempPref = (PowerGaugePreference) findPreference(KEY_BATTERY_TEMP);
        mLastFullChargePref = (PowerGaugePreference) findPreference(
                KEY_TIME_SINCE_LAST_FULL_CHARGE);
        mBatteryUtils = BatteryUtils.getInstance(getContext());

        if (Utils.isBatteryPresent(getContext())) {
            restartBatteryInfoLoader();
        }
        mBatteryTipPreferenceController.restoreInstanceState(icicle);
        updateBatteryTipFlag(icicle);

        // Check availability of Battery Health
        updateAvailability();
    }

    private boolean checkKey(String prefKey) {
        switch (prefKey) {
            case KEY_DESIGNED_BATTERY_CAPACITY:
            case KEY_CURRENT_BATTERY_CAPACITY:
            case KEY_BATTERY_CHARGE_CYCLES:
            case KEY_BATTERY_TYPE:
            case KEY_BATTERY_HEALTH:
                return true;
            default:
                return false;
        }
    }

    private void updateAvailability() {
        final Resources res = getContext().getResources();
        if (!res.getBoolean(R.bool.config_supportBatteryHealth)) {
            for (int i = 0; i < pGPList.size(); i++) {
                getPreferenceScreen().removePreference(pGPList.get(i));
            }
        } else {
            String configPath = null;
            for (int i = 0; i < pGPList.size(); i++) {
                configPath = getPrefResources(pGPList.get(i).getKey(), true);
                if (configPath == null || configPath.isEmpty()) {
                    //remove from preference screen if no config path is available.
                    getPreferenceScreen().removePreference(pGPList.get(i));
                } else {
                    //for available configPath, check if we can access it and read from it.
                    if (checkPreference(configPath, pGPList.get(i))) {
                        getPreferenceScreen().removePreference(pGPList.get(i));
                    }
                }
            }
        }
    }

    private boolean checkPreference(String file, PowerGaugePreference preference) {
        //If we are having issues accessing the file or reading from it.
        final boolean isAccessBad = (preference == (PowerGaugePreference) findPreference(KEY_BATTERY_TYPE)) ?
                getResources().getString(R.string.status_unavailable).equals(parseLine(file)) : parseInteger(file) == -1;
        return isAccessBad;
    }

    private String getPrefResources(String prefKey, boolean returnPath) {
        final Resources res = getResources();
        switch (prefKey) {
            case KEY_DESIGNED_BATTERY_CAPACITY:
                return returnPath ? res.getString(R.string.config_batDesCap)
                                  : parseInteger(res.getString(R.string.config_batDesCap)) /
                                      res.getInteger(R.integer.config_batDesCapDivider) + " mAh";
            case KEY_CURRENT_BATTERY_CAPACITY:
                return returnPath ? res.getString(R.string.config_batCurCap)
                                  : parseInteger(res.getString(R.string.config_batCurCap)) /
                                      res.getInteger(R.integer.config_batCurCapDivider) + " mAh";
            case KEY_BATTERY_CHARGE_CYCLES:
                return returnPath ? res.getString(R.string.config_batChargeCycle)
                                  : parseInteger(res.getString(R.string.config_batChargeCycle)) + " Cycles";
            case KEY_BATTERY_TYPE:
                return returnPath ? res.getString(R.string.config_batType)
                                  : parseLine(res.getString(R.string.config_batType));
            case KEY_BATTERY_HEALTH:
                return returnPath ? res.getString(R.string.config_batHealth)
                                  : parseInteger(res.getString(R.string.config_batHealth)) + " %";
        }
        return null;
    }

    private void updateBatteryInfoPreference(PowerGaugePreference preference) {
        //If preference exists
        if (preference != null) {
            preference.setSummary(getPrefResources(preference.getKey(), false));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_BATTERY_HEADER.equals(preference.getKey())) {
            new SubSettingLauncher(getContext())
                        .setDestination(PowerUsageAdvanced.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setTitleRes(R.string.advanced_battery_title)
                        .launch();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mBatteryLevel = savedInstanceState.getInt(ARG_BATTERY_LEVEL);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initHeaderPreference();
        getContentResolver().registerContentObserver(
                Global.getUriFor(Global.BATTERY_ESTIMATES_LAST_UPDATE_TIME),
                false,
                mSettingsObserver);
    }

    @Override
    public void onPause() {
        getContentResolver().unregisterContentObserver(mSettingsObserver);
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_POWER_USAGE_SUMMARY_V2;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_summary;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_ADVANCED_BATTERY, Menu.NONE, R.string.advanced_battery_title);

        MenuItem reset = menu.add(0, MENU_STATS_RESET, 0, R.string.battery_stats_reset)
                .setIcon(R.drawable.ic_delete)
                .setAlphabeticShortcut('d');
        reset.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void resetStats() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.battery_stats_reset)
            .setMessage(R.string.battery_stats_message)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mStatsHelper.resetStatistics();
                    refreshUi(BatteryUpdateType.MANUAL);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();
        dialog.show();
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_battery;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS_RESET:
                resetStats();
                return true;
            case MENU_ADVANCED_BATTERY:
                new SubSettingLauncher(getContext())
                        .setDestination(PowerUsageAdvanced.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setTitleRes(R.string.advanced_battery_title)
                        .launch();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void refreshUi(@BatteryUpdateType int refreshType) {
        final Context context = getContext();
        final Resources res = getResources();
        if (context == null) {
            return;
        }
        // Skip refreshing UI if battery is not present.
        if (!mIsBatteryPresent) {
            return;
        }

        // Skip BatteryTipLoader if device is rotated or only battery level change
        if (mNeedUpdateBatteryTip
                && refreshType != BatteryUpdateType.BATTERY_LEVEL) {
            restartBatteryTipLoader();
        } else {
            mNeedUpdateBatteryTip = true;
        }
        // reload BatteryInfo and updateUI
        restartBatteryInfoLoader();
        updateLastFullChargePreference();
        mScreenUsagePref.setSummary(StringUtil.formatElapsedTime(context,
                mBatteryUtils.calculateScreenUsageTime(mStatsHelper), false));
        final long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        for (int i = 0; i < pGPList.size(); i++) {
             updateBatteryInfoPreference((PowerGaugePreference) pGPList.get(i));
        }

        Intent batteryBroadcast = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryInfo batteryInfo = BatteryInfo.getBatteryInfoOld(context, batteryBroadcast,
                mStatsHelper.getStats(), elapsedRealtimeUs, false);
        mBatteryTempPref.setSummary(BatteryInfo.batteryTemp + " \u2103");
        updateHeaderPreference(batteryInfo);
    }

    @VisibleForTesting
    void restartBatteryTipLoader() {
        getLoaderManager().restartLoader(BATTERY_TIP_LOADER, Bundle.EMPTY, mBatteryTipsCallbacks);
    }

    @VisibleForTesting
    void setBatteryLayoutPreference(LayoutPreference layoutPreference) {
        mBatteryLayoutPref = layoutPreference;
    }

    @VisibleForTesting
    void updateLastFullChargePreference() {
        if (mBatteryInfo != null && mBatteryInfo.averageTimeToDischarge
                != EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN) {
            mLastFullChargePref.setTitle(R.string.battery_full_charge_last);
            mLastFullChargePref.setSummary(
                    StringUtil.formatElapsedTime(getContext(), mBatteryInfo.averageTimeToDischarge,
                            false /* withSeconds */));
        } else {
            final long lastFullChargeTime = mBatteryUtils.calculateLastFullChargeTime(mStatsHelper,
                    System.currentTimeMillis());
            mLastFullChargePref.setTitle(R.string.battery_last_full_charge);
            mLastFullChargePref.setSummary(
                    StringUtil.formatRelativeTime(getContext(), lastFullChargeTime,
                            false /* withSeconds */));
        }
    }

    @VisibleForTesting
    void showBothEstimates() {
        final Context context = getContext();
        if (context == null
                || !mPowerFeatureProvider.isEnhancedBatteryPredictionEnabled(context)) {
            return;
        }
        getLoaderManager().restartLoader(DEBUG_INFO_LOADER, Bundle.EMPTY,
                mBatteryInfoDebugLoaderCallbacks);
    }

    @VisibleForTesting
    void updateHeaderPreference(BatteryInfo info) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final BatteryMeterView batteryView = (BatteryMeterView) mBatteryLayoutPref
                .findViewById(R.id.battery_header_icon);
        final TextView timeText = (TextView) mBatteryLayoutPref.findViewById(R.id.battery_percent);
        final TextView summary1 = (TextView) mBatteryLayoutPref.findViewById(R.id.summary1);
        if (info.remainingLabel == null ) {
            summary1.setText(info.statusLabel);
        } else {
            summary1.setText(info.remainingLabel);
        }
        batteryView.setCharging(!info.discharging);
        batteryView.setPowerSave(mPowerManager.isPowerSaveMode());
        startBatteryHeaderAnimationIfNecessary(batteryView, timeText, mBatteryLevel,
                info.batteryLevel);
    }

    @VisibleForTesting
    void initHeaderPreference() {
        if (getContext() != null) {
            final BatteryMeterView batteryView = (BatteryMeterView) mBatteryLayoutPref
                  .findViewById(R.id.battery_header_icon);
            final TextView timeText = (TextView) mBatteryLayoutPref.findViewById(R.id.battery_percent);

            batteryView.setBatteryLevel(mBatteryLevel);
            batteryView.setPowerSave(mPowerManager.isPowerSaveMode());
            timeText.setText(formatBatteryPercentageText(mBatteryLevel));
        }
    }

    @VisibleForTesting
    void startBatteryHeaderAnimationIfNecessary(BatteryMeterView batteryView, TextView timeTextView,
                int prevLevel, int currentLevel) {
        if (getContext() != null) {
        mBatteryLevel = currentLevel;
        final int diff = Math.abs(prevLevel - currentLevel);
        if (diff != 0) {
            final ValueAnimator animator = ValueAnimator.ofInt(prevLevel, currentLevel);
            animator.setDuration(BATTERY_ANIMATION_DURATION_MS_PER_LEVEL * diff);
            animator.setInterpolator(AnimationUtils.loadInterpolator(getContext(),
            android.R.interpolator.fast_out_slow_in));
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    final Integer level = (Integer) animation.getAnimatedValue();
                    batteryView.setBatteryLevel(level);
                    batteryView.setPowerSave(mPowerManager.isPowerSaveMode());
                    timeTextView.setText(formatBatteryPercentageText(level));
                }
            });
            animator.start();
        }
      }
    }

    @VisibleForTesting
    void initFeatureProvider() {
        final Context context = getContext();
        mPowerFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    @VisibleForTesting
    void restartBatteryInfoLoader() {
        if (getContext() == null) {
            return;
        }
        // Skip restartBatteryInfoLoader if battery is not present.
        if (!mIsBatteryPresent) {
            return;
        }
        getLoaderManager().restartLoader(BATTERY_INFO_LOADER, Bundle.EMPTY,
                mBatteryInfoLoaderCallbacks);
        if (mPowerFeatureProvider.isEstimateDebugEnabled()) {
            // Set long click action for summary to show debug info
            View header = mBatteryLayoutPref.findViewById(R.id.summary1);
            header.setOnLongClickListener(this);
        }
    }

    @VisibleForTesting
    void updateBatteryTipFlag(Bundle icicle) {
        mNeedUpdateBatteryTip = icicle == null || mBatteryTipPreferenceController.needUpdate();
    }

    @Override
    public boolean onLongClick(View view) {
        showBothEstimates();
        view.setOnLongClickListener(null);
        return true;
    }

    @Override
    protected void restartBatteryStatsLoader(@BatteryUpdateType int refreshType) {
        super.restartBatteryStatsLoader(refreshType);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mBatteryTipPreferenceController.saveInstanceState(outState);
    }

    @Override
    public void onBatteryTipHandled(BatteryTip batteryTip) {
        restartBatteryTipLoader();
    }

    private int parseInteger(String file) {
        try {
            return Integer.parseInt(readLine(file));
        } catch (IOException ioe) {
            Log.e(TAG, "Cannot read from "
                    + file, ioe);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Read a badly formatted data from "
                    + file, nfe);
        }
        return Integer.parseInt("-1");
    }

    private String parseLine(String file) {
        try {
            return readLine(file);
        } catch (IOException ioe) {
            Log.e(TAG, "Cannot read line from "
                    + file, ioe);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Read a badly formatted line from "
                    + file, nfe);
        }
        return getResources().getString(R.string.status_unavailable);
    }

    /**
    * Reads a line from the specified file.
    *
    * @param filename The file to read from.
    * @return The first line up to 256 characters, or <code>null</code> if file is empty.
    * @throws IOException If the file couldn't be read.
    */
    @Nullable
    private String readLine(String filename) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    private CharSequence formatBatteryPercentageText(int batteryLevel) {
        try {
            return TextUtils.expandTemplate(getContext().getText(R.string.battery_header_title_alternate),
                  NumberFormat.getIntegerInstance().format(batteryLevel));
        }
        catch (Exception e) {
            return null;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.power_usage_summary);
}
