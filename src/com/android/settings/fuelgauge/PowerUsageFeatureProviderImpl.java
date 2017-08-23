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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Process;
import com.android.internal.os.BatterySipper;
import com.android.internal.util.ArrayUtils;

public class PowerUsageFeatureProviderImpl implements PowerUsageFeatureProvider {

    private static final String ADDITIONAL_BATTERY_INFO_ACTION = "com.google.android.apps.turbo.SHOW_ADDITIONAL_BATTERY_INFO";
    private static final String ADDITIONAL_BATTERY_INFO_PACKAGE = "com.google.android.apps.turbo";
    private static final String PACKAGE_CALENDAR_PROVIDER = "com.android.providers.calendar";
    private static final String PACKAGE_MEDIA_PROVIDER = "com.android.providers.media";
    private static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    private static final String[] PACKAGES_SYSTEM = {PACKAGE_MEDIA_PROVIDER,
            PACKAGE_CALENDAR_PROVIDER, PACKAGE_SYSTEMUI};

    private Context mContext;
    protected PackageManager mPackageManager;

    public PowerUsageFeatureProviderImpl(Context context) {
        mPackageManager = context.getPackageManager();
        mContext = context;
    }

    @Override
    public boolean isTypeService(BatterySipper sipper) {
        return false;
    }

    @Override
    public boolean isTypeSystem(BatterySipper sipper) {
        final int uid = sipper.uidObj == null ? -1 : sipper.getUid();
        sipper.mPackages = mPackageManager.getPackagesForUid(uid);
        // Classify all the sippers to type system if the range of uid is 0...FIRST_APPLICATION_UID
        if (uid >= Process.ROOT_UID && uid < Process.FIRST_APPLICATION_UID) {
            return true;
        } else if (sipper.mPackages != null) {
            for (final String packageName : sipper.mPackages) {
                if (ArrayUtils.contains(PACKAGES_SYSTEM, packageName)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isLocationSettingEnabled(String[] packages) {
        return false;
    }

    @Override
    public boolean isAdditionalBatteryInfoEnabled() {
        Intent intent = getAdditionalBatteryInfoIntent();
        return !mContext.getPackageManager().queryIntentActivities(intent, 0).isEmpty();
    }

    @Override
    public Intent getAdditionalBatteryInfoIntent() {
        Intent intent = new Intent(ADDITIONAL_BATTERY_INFO_ACTION);
        return intent.setPackage(ADDITIONAL_BATTERY_INFO_PACKAGE);
    }

    @Override
    public boolean isAdvancedUiEnabled() {
        return true;
    }

    @Override
    public boolean isPowerAccountingToggleEnabled() {
        return true;
    }
}
