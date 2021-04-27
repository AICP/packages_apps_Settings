/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.utils.StringUtil;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controls the update for chart graph and the list items. */
public class BatteryChartPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnDestroy,
                BatteryChartView.OnSelectListener, ExpandDividerPreference.OnExpandListener {
    private static final String TAG = "BatteryChartPreferenceController";
    private static final int CHART_KEY_ARRAY_SIZE = 25;
    private static final int CHART_LEVEL_ARRAY_SIZE = 13;
    private static final long VALID_USAGE_TIME_DURATION = DateUtils.HOUR_IN_MILLIS * 2;
    private static final long VALID_DIFF_DURATION = DateUtils.MINUTE_IN_MILLIS * 3;

    @VisibleForTesting
    Map<Integer, List<BatteryDiffEntry>> mBatteryIndexedMap;

    @VisibleForTesting Context mPrefContext;
    @VisibleForTesting BatteryUtils mBatteryUtils;
    @VisibleForTesting PreferenceGroup mAppListPrefGroup;
    @VisibleForTesting BatteryChartView mBatteryChartView;
    @VisibleForTesting ExpandDividerPreference mExpandDividerPreference;
    @VisibleForTesting CategoryTitleType mCategoryTitleType =
        CategoryTitleType.TYPE_UNKNOWN;

    @VisibleForTesting int[] mBatteryHistoryLevels;
    @VisibleForTesting long[] mBatteryHistoryKeys;
    @VisibleForTesting int mTrapezoidIndex = BatteryChartView.SELECTED_INDEX_INVALID;

    private final String mPreferenceKey;
    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final CharSequence[] mNotAllowShowSummaryPackages;

    private boolean mIsExpanded = false;

    // Preference cache to avoid create new instance each time.
    @VisibleForTesting
    final Map<String, Preference> mPreferenceCache = new HashMap<>();
    @VisibleForTesting
    final List<BatteryDiffEntry> mSystemEntries = new ArrayList<>();

    /** Which component data will be shown in the screen. */
    enum CategoryTitleType {
        TYPE_UNKNOWN,
        TYPE_APP_COMPONENT,
        TYPE_SYSTEM_COMPONENT,
        TYPE_ALL_COMPONENTS
    }

    public BatteryChartPreferenceController(
            Context context, String preferenceKey,
            Lifecycle lifecycle, SettingsActivity activity,
            InstrumentedPreferenceFragment fragment) {
        super(context);
        mActivity = activity;
        mFragment = fragment;
        mPreferenceKey = preferenceKey;
        mNotAllowShowSummaryPackages = context.getResources()
            .getTextArray(R.array.allowlist_hide_summary_in_battery_usage);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
        if (mActivity.isChangingConfigurations()) {
            BatteryDiffEntry.clearCache();
        }
        mHandler.removeCallbacksAndMessages(/*token=*/ null);
        mPreferenceCache.clear();
        if (mAppListPrefGroup != null) {
            mAppListPrefGroup.removeAll();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
        mAppListPrefGroup = screen.findPreference(mPreferenceKey);
        mAppListPrefGroup.setOrderingAsAdded(false);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof PowerGaugePreference)) {
            return false;
        }
        final PowerGaugePreference powerPref = (PowerGaugePreference) preference;
        final BatteryDiffEntry diffEntry = powerPref.getBatteryDiffEntry();
        final BatteryHistEntry histEntry = diffEntry.mBatteryHistEntry;
        final String packageName = histEntry.mPackageName;
        // Checks whether the package is installed or not.
        boolean isValidPackage = true;
        if (histEntry.isAppEntry()) {
            if (mBatteryUtils == null) {
                mBatteryUtils = BatteryUtils.getInstance(mPrefContext);
            }
            isValidPackage = mBatteryUtils.getPackageUid(packageName)
                != BatteryUtils.UID_NULL;
        }
        Log.d(TAG, String.format("handleClick() label=%s key=%s isValid:%b %s",
            diffEntry.getAppLabel(), histEntry.getKey(), isValidPackage, packageName));
        if (isValidPackage) {
            AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, diffEntry, powerPref.getPercent(),
                isValidToShowSummary(packageName), getSlotInformation());
            return true;
        }
        return false;
    }

    @Override
    public void onSelect(int trapezoidIndex) {
        Log.d(TAG, "onChartSelect:" + trapezoidIndex);
        refreshUi(trapezoidIndex, /*isForce=*/ false);
    }

    @Override
    public void onExpand(boolean isExpanded) {
        mIsExpanded = isExpanded;
        refreshExpandUi();
    }

    void setBatteryHistoryMap(
            final Map<Long, List<BatteryHistEntry>> batteryHistoryMap) {
        mHandler.post(() -> setBatteryHistoryMapInner(batteryHistoryMap));
    }

    private void setBatteryHistoryMapInner(
            final Map<Long, List<BatteryHistEntry>> batteryHistoryMap) {
        // Resets all battery history data relative variables.
        if (batteryHistoryMap == null) {
            mBatteryIndexedMap = null;
            mBatteryHistoryKeys = null;
            mBatteryHistoryLevels = null;
            return;
        }
        // Generates battery history keys.
        final List<Long> batteryHistoryKeyList =
            new ArrayList<Long>(batteryHistoryMap.keySet());
        Collections.sort(batteryHistoryKeyList);
        validateSlotTimestamp(batteryHistoryKeyList);
        mBatteryHistoryKeys = new long[CHART_KEY_ARRAY_SIZE];
        final int listSize = batteryHistoryKeyList.size();
        final int elementSize = Math.min(listSize, CHART_KEY_ARRAY_SIZE);
        for (int index = 0; index < elementSize; index++) {
            mBatteryHistoryKeys[CHART_KEY_ARRAY_SIZE - index - 1] =
                batteryHistoryKeyList.get(listSize - index - 1);
        }

        // Generates the battery history levels.
        mBatteryHistoryLevels = new int[CHART_LEVEL_ARRAY_SIZE];
        for (int index = 0; index < CHART_LEVEL_ARRAY_SIZE; index++) {
            final Long timestamp = Long.valueOf(mBatteryHistoryKeys[index * 2]);
            final List<BatteryHistEntry> entryList = batteryHistoryMap.get(timestamp);
            if (entryList != null && !entryList.isEmpty()) {
                // All battery levels are the same in the same timestamp snapshot.
                mBatteryHistoryLevels[index] = entryList.get(0).mBatteryLevel;
            } else if (entryList != null && entryList.isEmpty()) {
                Log.e(TAG, "abnormal entry list in the timestamp:" +
                    ConvertUtils.utcToLocalTime(timestamp));
            }
        }
        // Generates indexed usage map for chart.
        mBatteryIndexedMap =
            ConvertUtils.getIndexedUsageMap(
                mPrefContext, /*timeSlotSize=*/ CHART_LEVEL_ARRAY_SIZE - 1,
                mBatteryHistoryKeys, batteryHistoryMap,
                /*purgeLowPercentageData=*/ true);
        forceRefreshUi();

        Log.d(TAG, String.format(
            "setBatteryHistoryMap() size=%d\nkeys=%s\nlevels=%s",
            batteryHistoryKeyList.size(),
            utcToLocalTime(mBatteryHistoryKeys),
            Arrays.toString(mBatteryHistoryLevels)));
    }

    void setBatteryChartView(final BatteryChartView batteryChartView) {
        mHandler.post(() -> setBatteryChartViewInner(batteryChartView));
    }

    private void setBatteryChartViewInner(final BatteryChartView batteryChartView) {
        mBatteryChartView = batteryChartView;
        mBatteryChartView.setOnSelectListener(this);
        forceRefreshUi();
    }

    private void forceRefreshUi() {
        final int refreshIndex =
            mTrapezoidIndex == BatteryChartView.SELECTED_INDEX_INVALID
                ? BatteryChartView.SELECTED_INDEX_ALL
                : mTrapezoidIndex;
        if (mBatteryChartView != null) {
            mBatteryChartView.setLevels(mBatteryHistoryLevels);
        }
        refreshUi(refreshIndex, /*isForce=*/ true);
    }

    @VisibleForTesting
    boolean refreshUi(int trapezoidIndex, boolean isForce) {
        // Invalid refresh condition.
        if (mBatteryIndexedMap == null
                || mBatteryChartView == null
                || (mTrapezoidIndex == trapezoidIndex && !isForce)) {
            return false;
        }
        Log.d(TAG, String.format("refreshUi: index=%d size=%d isForce:%b",
            trapezoidIndex, mBatteryIndexedMap.size(), isForce));

        mTrapezoidIndex = trapezoidIndex;
        mHandler.post(() -> {
            removeAndCacheAllPrefs();
            addAllPreferences();
            refreshCategoryTitle();
        });
        return true;
    }

    private void addAllPreferences() {
        mCategoryTitleType = CategoryTitleType.TYPE_UNKNOWN;
        final List<BatteryDiffEntry> entries =
            mBatteryIndexedMap.get(Integer.valueOf(mTrapezoidIndex));
        if (entries == null) {
            Log.w(TAG, "cannot find BatteryDiffEntry for:" + mTrapezoidIndex);
            return;
        }
        // Separates data into two groups and sort them individually.
        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        mSystemEntries.clear();
        entries.forEach(entry -> {
            if (entry.isSystemEntry()) {
                mSystemEntries.add(entry);
            } else {
                appEntries.add(entry);
            }
            // Validates the usage time if users click a specific slot.
            if (mTrapezoidIndex >= 0) {
                validateUsageTime(entry);
            }
        });
        Collections.sort(appEntries, BatteryDiffEntry.COMPARATOR);
        Collections.sort(mSystemEntries, BatteryDiffEntry.COMPARATOR);
        Log.d(TAG, String.format("addAllPreferences() app=%d system=%d",
            appEntries.size(), mSystemEntries.size()));

        // Adds app entries to the list if it is not empty.
        if (!appEntries.isEmpty()) {
            addPreferenceToScreen(appEntries);
            mCategoryTitleType = CategoryTitleType.TYPE_APP_COMPONENT;
        }
        // Adds the expabable divider if we have two sections data.
        if (!appEntries.isEmpty() && !mSystemEntries.isEmpty()) {
            if (mExpandDividerPreference == null) {
                mExpandDividerPreference = new ExpandDividerPreference(mPrefContext);
                mExpandDividerPreference.setOnExpandListener(this);
            }
            mExpandDividerPreference.setOrder(
                mAppListPrefGroup.getPreferenceCount());
            mAppListPrefGroup.addPreference(mExpandDividerPreference);
            mCategoryTitleType = CategoryTitleType.TYPE_ALL_COMPONENTS;
        } else if (appEntries.isEmpty() && !mSystemEntries.isEmpty()) {
            mCategoryTitleType = CategoryTitleType.TYPE_SYSTEM_COMPONENT;
        }
        refreshExpandUi();
    }

    @VisibleForTesting
    void addPreferenceToScreen(List<BatteryDiffEntry> entries) {
        if (mAppListPrefGroup == null || entries.isEmpty()) {
            return;
        }
        int prefIndex = mAppListPrefGroup.getPreferenceCount();
        for (BatteryDiffEntry entry : entries) {
            boolean isAdded = false;
            final String appLabel = entry.getAppLabel();
            final Drawable appIcon = entry.getAppIcon();
            if (TextUtils.isEmpty(appLabel) || appIcon == null) {
                Log.w(TAG, "cannot find app resource for\n" + entry);
                continue;
            }
            final String prefKey = entry.mBatteryHistEntry.getKey();
            PowerGaugePreference pref = mAppListPrefGroup.findPreference(prefKey);
            if (pref != null) {
                isAdded = true;
                Log.w(TAG, "preference should be removed for\n" + entry);
            } else {
                pref = (PowerGaugePreference) mPreferenceCache.get(prefKey);
            }
            // Creates new innstance if cached preference is not found.
            if (pref == null) {
                pref = new PowerGaugePreference(mPrefContext);
                pref.setKey(prefKey);
                mPreferenceCache.put(prefKey, pref);
            }
            pref.setIcon(appIcon);
            pref.setTitle(appLabel);
            pref.setOrder(prefIndex);
            pref.setPercent(entry.getPercentOfTotal());
            pref.setSingleLineTitle(true);
            // Sets the BatteryDiffEntry to preference for launching detailed page.
            pref.setBatteryDiffEntry(entry);
            setPreferenceSummary(pref, entry);
            if (!isAdded) {
                mAppListPrefGroup.addPreference(pref);
            }
            prefIndex++;
        }
    }

    private void removeAndCacheAllPrefs() {
        if (mAppListPrefGroup == null
                || mAppListPrefGroup.getPreferenceCount() == 0) {
            return;
        }
        final int prefsCount = mAppListPrefGroup.getPreferenceCount();
        for (int index = 0; index < prefsCount; index++) {
            final Preference pref = mAppListPrefGroup.getPreference(index);
            if (TextUtils.isEmpty(pref.getKey())) {
                continue;
            }
            mPreferenceCache.put(pref.getKey(), pref);
        }
        mAppListPrefGroup.removeAll();
    }

    private void refreshExpandUi() {
        if (mIsExpanded) {
            addPreferenceToScreen(mSystemEntries);
        } else {
            // Removes and recycles all system entries to hide all of them.
            for (BatteryDiffEntry entry : mSystemEntries) {
                final String prefKey = entry.mBatteryHistEntry.getKey();
                final Preference pref = mAppListPrefGroup.findPreference(prefKey);
                if (pref != null) {
                    mAppListPrefGroup.removePreference(pref);
                    mPreferenceCache.put(pref.getKey(), pref);
                }
            }
        }
    }

    @VisibleForTesting
    void refreshCategoryTitle() {
        final String slotInformation = getSlotInformation();
        Log.d(TAG, String.format("refreshCategoryTitle:%s slotInfo:%s",
            mCategoryTitleType, slotInformation));
        refreshPreferenceCategoryTitle(slotInformation);
        refreshExpandableDividerTitle(slotInformation);
    }

    private void refreshExpandableDividerTitle(String slotInformation) {
        if (mExpandDividerPreference == null) {
            return;
        }
        mExpandDividerPreference.setTitle(
            mCategoryTitleType == CategoryTitleType.TYPE_ALL_COMPONENTS
                ? getSlotInformation(/*isApp=*/ false, slotInformation)
                : null);
    }

    private void refreshPreferenceCategoryTitle(String slotInformation) {
        if (mAppListPrefGroup == null) {
            return;
        }
        switch (mCategoryTitleType) {
            case TYPE_APP_COMPONENT:
            case TYPE_ALL_COMPONENTS:
                mAppListPrefGroup.setTitle(
                    getSlotInformation(/*isApp=*/ true, slotInformation));
                break;
            case TYPE_SYSTEM_COMPONENT:
                mAppListPrefGroup.setTitle(
                    getSlotInformation(/*isApp=*/ false, slotInformation));
                break;
            default:
                mAppListPrefGroup.setTitle(R.string.battery_app_usage_for_past_24);
        }
    }

    private String getSlotInformation(boolean isApp, String slotInformation) {
        // Null means we show all information without a specific time slot.
        if (slotInformation == null) {
            return isApp
                ? mPrefContext.getString(R.string.battery_app_usage_for_past_24)
                : mPrefContext.getString(R.string.battery_system_usage_for_past_24);
        } else {
            return isApp
                ? mPrefContext.getString(R.string.battery_app_usage_for, slotInformation)
                : mPrefContext.getString(R.string.battery_system_usage_for ,slotInformation);
        }
    }

    private String getSlotInformation() {
        if (mTrapezoidIndex < 0) {
            return null;
        }
        final String fromHour = ConvertUtils.utcToLocalTimeHour(
            mBatteryHistoryKeys[mTrapezoidIndex * 2]);
        final String toHour = ConvertUtils.utcToLocalTimeHour(
            mBatteryHistoryKeys[(mTrapezoidIndex + 1) * 2]);
        return String.format("%s-%s", fromHour, toHour);
    }

    @VisibleForTesting
    void setPreferenceSummary(
            PowerGaugePreference preference, BatteryDiffEntry entry) {
        final long foregroundUsageTimeInMs = entry.mForegroundUsageTimeInMs;
        final long backgroundUsageTimeInMs = entry.mBackgroundUsageTimeInMs;
        final long totalUsageTimeInMs = foregroundUsageTimeInMs + backgroundUsageTimeInMs;
        // Checks whether the package is allowed to show summary or not.
        if (!isValidToShowSummary(entry.getPackageName())) {
            preference.setSummary(null);
            return;
        }
        String usageTimeSummary = null;
        // Not shows summary for some system components without usage time.
        if (totalUsageTimeInMs == 0) {
            preference.setSummary(null);
        // Shows background summary only if we don't have foreground usage time.
        } else if (foregroundUsageTimeInMs == 0 && backgroundUsageTimeInMs != 0) {
            usageTimeSummary = buildUsageTimeInfo(backgroundUsageTimeInMs, true);
        // Shows total usage summary only if total usage time is small.
        } else if (totalUsageTimeInMs < DateUtils.MINUTE_IN_MILLIS) {
            usageTimeSummary = buildUsageTimeInfo(totalUsageTimeInMs, false);
        } else {
            usageTimeSummary = buildUsageTimeInfo(totalUsageTimeInMs, false);
            // Shows background usage time if it is larger than a minute.
            if (backgroundUsageTimeInMs > 0) {
                usageTimeSummary +=
                    "\n" + buildUsageTimeInfo(backgroundUsageTimeInMs, true);
            }
        }
        preference.setSummary(usageTimeSummary);
    }

    private String buildUsageTimeInfo(long usageTimeInMs, boolean isBackground) {
        if (usageTimeInMs < DateUtils.MINUTE_IN_MILLIS) {
            return mPrefContext.getString(
                isBackground
                    ? R.string.battery_usage_background_less_than_one_minute
                    : R.string.battery_usage_total_less_than_one_minute);
        }
        final CharSequence timeSequence =
            StringUtil.formatElapsedTime(mPrefContext, usageTimeInMs,
                /*withSeconds=*/ false, /*collapseTimeUnit=*/ false);
        final int resourceId =
            isBackground
                ? R.string.battery_usage_for_background_time
                : R.string.battery_usage_for_total_time;
        return mPrefContext.getString(resourceId, timeSequence);
    }

    private boolean isValidToShowSummary(String packageName) {
        if (mNotAllowShowSummaryPackages != null) {
            for (CharSequence notAllowPackageName : mNotAllowShowSummaryPackages) {
                if (TextUtils.equals(packageName, notAllowPackageName)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String utcToLocalTime(long[] timestamps) {
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < timestamps.length; index++) {
            builder.append(String.format("%s| ",
                  ConvertUtils.utcToLocalTime(timestamps[index])));
        }
        return builder.toString();
    }

    @VisibleForTesting
    static boolean validateUsageTime(BatteryDiffEntry entry) {
        final long foregroundUsageTimeInMs = entry.mForegroundUsageTimeInMs;
        final long backgroundUsageTimeInMs = entry.mBackgroundUsageTimeInMs;
        final long totalUsageTimeInMs = foregroundUsageTimeInMs + backgroundUsageTimeInMs;
        if (foregroundUsageTimeInMs > VALID_USAGE_TIME_DURATION
                || backgroundUsageTimeInMs > VALID_USAGE_TIME_DURATION
                || totalUsageTimeInMs > VALID_USAGE_TIME_DURATION) {
            Log.e(TAG, "validateUsageTime() fail for\n" + entry);
            return false;
        }
        return true;
    }

    @VisibleForTesting
    static boolean validateSlotTimestamp(List<Long> batteryHistoryKeys) {
        // Whether the nearest two slot time diff is valid or not?
        final int size = batteryHistoryKeys.size();
        for (int index = 0; index < size - 1; index++) {
            final long currentTime = batteryHistoryKeys.get(index);
            final long nextTime = batteryHistoryKeys.get(index + 1);
            final long diffTime = Math.abs(
                DateUtils.HOUR_IN_MILLIS - Math.abs(currentTime - nextTime));
            if (currentTime == 0) {
                continue;
            } else if (diffTime > VALID_DIFF_DURATION) {
                Log.e(TAG, String.format("validateSlotTimestamp() %s > %s",
                    ConvertUtils.utcToLocalTime(currentTime),
                    ConvertUtils.utcToLocalTime(nextTime)));
                return false;
            }
        }
        return true;
    }
}
