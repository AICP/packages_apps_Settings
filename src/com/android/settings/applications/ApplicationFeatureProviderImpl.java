/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.applications;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.icu.text.MeasureFormat;
import android.icu.text.MeasureFormat.FormatWidth;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserManager;
import android.service.euicc.EuiccService;
import android.telecom.DefaultDialerManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.internal.telephony.SmsApplication;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ApplicationFeatureProviderImpl implements ApplicationFeatureProvider {
    private static final String TAG = "AppFeatureProviderImpl";
    private static final boolean DEBUG = false;

    protected final Context mContext;
    private final PackageManager mPm;
    private final IPackageManager mPms;
    private final DevicePolicyManager mDpm;
    private final UserManager mUm;
    /** Flags to use when querying PackageManager for Euicc component implementations. */
    private static final int EUICC_QUERY_FLAGS =
            PackageManager.MATCH_SYSTEM_ONLY | PackageManager.MATCH_DEBUG_TRIAGED_MISSING
                | PackageManager.GET_RESOLVED_FILTER;

    public ApplicationFeatureProviderImpl(Context context, PackageManager pm,
            IPackageManager pms, DevicePolicyManager dpm) {
        mContext = context.getApplicationContext();
        mPm = pm;
        mPms = pms;
        mDpm = dpm;
        mUm = UserManager.get(mContext);
    }

    @Override
    public void calculateNumberOfPolicyInstalledApps(boolean async, NumberOfAppsCallback callback) {
        final CurrentUserAndManagedProfilePolicyInstalledAppCounter counter =
                new CurrentUserAndManagedProfilePolicyInstalledAppCounter(mContext, mPm, callback);
        if (async) {
            counter.execute();
        } else {
            counter.executeInForeground();
        }
    }

    @Override
    public void listPolicyInstalledApps(ListOfAppsCallback callback) {
        final CurrentUserPolicyInstalledAppLister lister =
                new CurrentUserPolicyInstalledAppLister(mPm, mUm, callback);
        lister.execute();
    }

    @Override
    public void calculateNumberOfAppsWithAdminGrantedPermissions(String[] permissions,
            boolean async, NumberOfAppsCallback callback) {
        final CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter counter =
                new CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter(mContext,
                        permissions, mPm, mPms, mDpm, callback);
        if (async) {
            counter.execute();
        } else {
            counter.executeInForeground();
        }
    }

    @Override
    public void listAppsWithAdminGrantedPermissions(String[] permissions,
            ListOfAppsCallback callback) {
        final CurrentUserAppWithAdminGrantedPermissionsLister lister =
                new CurrentUserAppWithAdminGrantedPermissionsLister(permissions, mPm, mPms, mDpm,
                        mUm, callback);
        lister.execute();
    }

    @Override
    public List<UserAppInfo> findPersistentPreferredActivities(int userId, Intent[] intents) {
        final List<UserAppInfo> preferredActivities = new ArrayList<>();
        final Set<UserAppInfo> uniqueApps = new ArraySet<>();
        final UserInfo userInfo = mUm.getUserInfo(userId);
        for (final Intent intent : intents) {
            try {
                final ResolveInfo resolveInfo =
                        mPms.findPersistentPreferredActivity(intent, userId);
                if (resolveInfo != null) {
                    ComponentInfo componentInfo = null;
                    if (resolveInfo.activityInfo != null) {
                        componentInfo = resolveInfo.activityInfo;
                    } else if (resolveInfo.serviceInfo != null) {
                        componentInfo = resolveInfo.serviceInfo;
                    } else if (resolveInfo.providerInfo != null) {
                        componentInfo = resolveInfo.providerInfo;
                    }
                    if (componentInfo != null) {
                        UserAppInfo info = new UserAppInfo(userInfo, componentInfo.applicationInfo);
                        if (uniqueApps.add(info)) {
                            preferredActivities.add(info);
                        }
                    }
                }
            } catch (RemoteException exception) {
            }
        }
        return preferredActivities;
    }

    @Override
    public Set<String> getKeepEnabledPackages() {
        // Find current default phone/sms app. We should keep them enabled.
        final Set<String> keepEnabledPackages = new ArraySet<>();
        final String defaultDialer = DefaultDialerManager.getDefaultDialerApplication(mContext);
        if (!TextUtils.isEmpty(defaultDialer)) {
            keepEnabledPackages.add(defaultDialer);
        }
        final ComponentName defaultSms = SmsApplication.getDefaultSmsApplication(
                mContext, true /* updateIfNeeded */);
        if (defaultSms != null) {
            keepEnabledPackages.add(defaultSms.getPackageName());
        }

        // Keep Euicc Service enabled.
        final ComponentInfo euicc = findEuiccService(mPm);
        if (euicc != null) {
            keepEnabledPackages.add(euicc.packageName);
        }

        keepEnabledPackages.addAll(getEnabledPackageWhitelist());

        final LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        final String locationHistoryPackage = locationManager.getExtraLocationControllerPackage();
        if (locationHistoryPackage != null) {
            keepEnabledPackages.add(locationHistoryPackage);
        }

        // Keep Google Applications enabled.
        keepEnabledPackages.add("com.google.android.inputmethod.latin");
        keepEnabledPackages.add("com.google.android.dialer");
        keepEnabledPackages.add("com.google.android.apps.wellbeing");
        keepEnabledPackages.add("com.google.android.settings.intelligence");
        keepEnabledPackages.add("com.google.android.ims");
        keepEnabledPackages.add("com.google.android.packageinstaller");
        keepEnabledPackages.add("com.google.android.euicc");
        return keepEnabledPackages;
    }

    private Set<String> getEnabledPackageWhitelist() {
        final Set<String> keepEnabledPackages = new ArraySet<>();

        // Keep Settings intelligence enabled, otherwise search feature will be disabled.
        keepEnabledPackages.add(
                mContext.getString(R.string.config_settingsintelligence_package_name));

        // Keep Package Installer enabled.
        keepEnabledPackages.add(mContext.getString(R.string.config_package_installer_package_name));

        return keepEnabledPackages;
    }

    private static class CurrentUserAndManagedProfilePolicyInstalledAppCounter
            extends InstalledAppCounter {
        private NumberOfAppsCallback mCallback;

        CurrentUserAndManagedProfilePolicyInstalledAppCounter(Context context,
                PackageManager packageManager, NumberOfAppsCallback callback) {
            super(context, PackageManager.INSTALL_REASON_POLICY, packageManager);
            mCallback = callback;
        }

        @Override
        protected void onCountComplete(int num) {
            mCallback.onNumberOfAppsResult(num);
        }
    }

    private static class CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter
            extends AppWithAdminGrantedPermissionsCounter {
        private NumberOfAppsCallback mCallback;

        CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter(Context context,
                String[] permissions, PackageManager packageManager,
                IPackageManager packageManagerService,
                DevicePolicyManager devicePolicyManager, NumberOfAppsCallback callback) {
            super(context, permissions, packageManager, packageManagerService, devicePolicyManager);
            mCallback = callback;
        }

        @Override
        protected void onCountComplete(int num) {
            mCallback.onNumberOfAppsResult(num);
        }
    }

    private static class CurrentUserPolicyInstalledAppLister extends InstalledAppLister {
        private ListOfAppsCallback mCallback;

        CurrentUserPolicyInstalledAppLister(PackageManager packageManager,
                UserManager userManager, ListOfAppsCallback callback) {
            super(packageManager, userManager);
            mCallback = callback;
        }

        @Override
        protected void onAppListBuilt(List<UserAppInfo> list) {
            mCallback.onListOfAppsResult(list);
        }
    }

    private static class CurrentUserAppWithAdminGrantedPermissionsLister extends
            AppWithAdminGrantedPermissionsLister {
        private ListOfAppsCallback mCallback;

        CurrentUserAppWithAdminGrantedPermissionsLister(String[] permissions,
                PackageManager packageManager, IPackageManager packageManagerService,
                DevicePolicyManager devicePolicyManager, UserManager userManager,
                ListOfAppsCallback callback) {
            super(permissions, packageManager, packageManagerService, devicePolicyManager,
                    userManager);
            mCallback = callback;
        }

        @Override
        protected void onAppListBuilt(List<UserAppInfo> list) {
            mCallback.onListOfAppsResult(list);
        }
    }

    /**
     * Return the component info of the EuiccService to bind to, or null if none were found.
     */
    @VisibleForTesting
    ComponentInfo findEuiccService(PackageManager packageManager) {
        final Intent intent = new Intent(EuiccService.EUICC_SERVICE_INTERFACE);
        final List<ResolveInfo> resolveInfoList =
                packageManager.queryIntentServices(intent, EUICC_QUERY_FLAGS);
        final ComponentInfo bestComponent = findEuiccService(packageManager, resolveInfoList);
        if (bestComponent == null) {
            Log.w(TAG, "No valid EuiccService implementation found");
        }
        return bestComponent;
    }

    private ComponentInfo findEuiccService(
            PackageManager packageManager, List<ResolveInfo> resolveInfoList) {
        int bestPriority = Integer.MIN_VALUE;
        ComponentInfo bestComponent = null;
        if (resolveInfoList != null) {
            for (ResolveInfo resolveInfo : resolveInfoList) {
                if (!isValidEuiccComponent(packageManager, resolveInfo)) {
                    continue;
                }

                if (resolveInfo.filter.getPriority() > bestPriority) {
                    bestPriority = resolveInfo.filter.getPriority();
                    bestComponent = getComponentInfo(resolveInfo);
                }
            }
        }

        return bestComponent;
    }

    private boolean isValidEuiccComponent(
            PackageManager packageManager, ResolveInfo resolveInfo) {
        final ComponentInfo componentInfo = getComponentInfo(resolveInfo);
        final String packageName = componentInfo.packageName;

        // Verify that the app is privileged (via granting of a privileged permission).
        if (packageManager.checkPermission(
                Manifest.permission.WRITE_EMBEDDED_SUBSCRIPTIONS, packageName)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Package " + packageName
                    + " does not declare WRITE_EMBEDDED_SUBSCRIPTIONS");
            return false;
        }

        // Verify that only the system can access the component.
        final String permission;
        if (componentInfo instanceof ServiceInfo) {
            permission = ((ServiceInfo) componentInfo).permission;
        } else if (componentInfo instanceof ActivityInfo) {
            permission = ((ActivityInfo) componentInfo).permission;
        } else {
            throw new IllegalArgumentException("Can only verify services/activities");
        }
        if (!TextUtils.equals(permission, Manifest.permission.BIND_EUICC_SERVICE)) {
            Log.e(TAG, "Package " + packageName
                    + " does not require the BIND_EUICC_SERVICE permission");
            return false;
        }

        // Verify that the component declares a priority.
        if (resolveInfo.filter == null || resolveInfo.filter.getPriority() == 0) {
            Log.e(TAG, "Package " + packageName + " does not specify a priority");
            return false;
        }
        return true;
    }

    private ComponentInfo getComponentInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo;
        }
        if (resolveInfo.serviceInfo != null) {
            return resolveInfo.serviceInfo;
        }
        if (resolveInfo.providerInfo != null) {
            return resolveInfo.providerInfo;
        }
        throw new IllegalStateException("Missing ComponentInfo!");
    }

    @Override
    public CharSequence getTimeSpentInApp(String packageName) {
        try {
            if (!isPrivilegedApp("com.google.android.apps.wellbeing.api")) {
                if (DEBUG) {
                    Log.d("ApplicationFeatureProviderImpl", "Not a privileged app.");
                }
                return "";
            }
            Bundle bundle = new Bundle();
            bundle.putString("packageName", packageName);
            Bundle call = mContext.getContentResolver().call(
                                   "com.google.android.apps.wellbeing.api",
                                   "get_app_usage_millis", null, bundle);
            if (call != null) {
                if (call.getBoolean("success")) {
                    Bundle bundle2 = call.getBundle("data");
                    if (bundle2 == null) {
                        if (DEBUG) {
                            Log.d("ApplicationFeatureProviderImpl", "data bundle is null.");
                        }
                        return "";
                    }
                    String readableDuration = getReadableDuration(
                        Long.valueOf(bundle2.getLong("total_time_millis")),
                        FormatWidth.NARROW, R.string.duration_less_than_one_minute, false);
                    return mContext.getString(
                            R.string.screen_time_summary_usage_today, readableDuration);
                }
            }
            if (DEBUG) {
                Log.d("ApplicationFeatureProviderImpl", "Provider call unsuccessful");
            }
            return "";
        } catch (Exception e) {
            Log.w("ApplicationFeatureProviderImpl", "Error getting time spent for app " + packageName, e);
            return "";
        }
    }

    String getReadableDuration(Long totalTime, FormatWidth formatWidth, int defaultString, boolean landscape) {
        long j;
        long j2;
        long longValue = totalTime.longValue();
        if (longValue >= 3600000) {
            j = longValue / 3600000;
            longValue -= 3600000 * j;
        } else {
            j = 0;
        }
        if (longValue >= 60000) {
            j2 = longValue / 60000;
            longValue -= 60000 * j2;
        } else {
            j2 = 0;
        }
        int i2 = (j > 0 ? 1 : (j == 0 ? 0 : -1));
        if (i2 > 0 && j2 > 0) {
            return MeasureFormat.getInstance(Locale.getDefault(), formatWidth).formatMeasures(
                                             new Measure(Long.valueOf(j), MeasureUnit.HOUR),
                                             new Measure(Long.valueOf(j2), MeasureUnit.MINUTE));
        } else if (i2 > 0) {
            Locale locale = Locale.getDefault();
            if (!landscape) {
                formatWidth = FormatWidth.WIDE;
            }
            return MeasureFormat.getInstance(locale, formatWidth).formatMeasures(
                                             new Measure(Long.valueOf(j), MeasureUnit.HOUR));
        } else if (j2 > 0) {
            Locale locale2 = Locale.getDefault();
            if (!landscape) {
                formatWidth = FormatWidth.WIDE;
            }
            return MeasureFormat.getInstance(locale2, formatWidth).formatMeasures(
                                             new Measure(Long.valueOf(j2), MeasureUnit.MINUTE));
        } else if (longValue > 0) {
            return mContext.getResources().getString(defaultString);
        } else {
            Locale locale3 = Locale.getDefault();
            if (!landscape) {
                formatWidth = FormatWidth.WIDE;
            }
            return MeasureFormat.getInstance(locale3, formatWidth).formatMeasures(
                                             new Measure(0, MeasureUnit.MINUTE));
        }
    }

    boolean isPrivilegedApp(String packageName) {
        ProviderInfo resolveContentProvider = mContext.getPackageManager().resolveContentProvider(packageName, 0);
        if (resolveContentProvider == null)
            return false;
        return resolveContentProvider.applicationInfo.isPrivilegedApp();
    }
}
