/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.settings.core.instrumentation.ElapsedTimeUtils;

import java.time.Duration;

/** Receives broadcasts to start or stop the periodic fetching job. */
public final class BootBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BootBroadcastReceiver";
    private static final long RESCHEDULE_FOR_BOOT_ACTION = Duration.ofSeconds(6).toMillis();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public static final String ACTION_PERIODIC_JOB_RECHECK =
            "com.android.settings.battery.action.PERIODIC_JOB_RECHECK";
    public static final String ACTION_SETUP_WIZARD_FINISHED =
            "com.google.android.setupwizard.SETUP_WIZARD_FINISHED";

    /** Invokes periodic job rechecking process. */
    public static void invokeJobRecheck(Context context) {
        context = context.getApplicationContext();
        final Intent intent = new Intent(ACTION_PERIODIC_JOB_RECHECK);
        intent.setClass(context, BootBroadcastReceiver.class);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent == null ? "" : intent.getAction();
        if (DatabaseUtils.isWorkProfile(context)) {
            Log.w(TAG, "do not start job for work profile action=" + action);
            return;
        }

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
            case Intent.ACTION_MY_PACKAGE_UNSUSPENDED:
            case ACTION_SETUP_WIZARD_FINISHED:
            case ACTION_PERIODIC_JOB_RECHECK:
                Log.d(TAG, "refresh periodic job from action=" + action);
                refreshJobs(context);
                break;
            case Intent.ACTION_TIME_CHANGED:
                Log.d(TAG, "refresh job and clear all data from action=" + action);
                DatabaseUtils.clearAll(context);
                PeriodicJobManager.getInstance(context).refreshJob();
                break;
            default:
                Log.w(TAG, "receive unsupported action=" + action);
        }

        // Waits a while to recheck the scheduler to avoid AlarmManager is not ready.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            final Intent recheckIntent = new Intent(ACTION_PERIODIC_JOB_RECHECK);
            recheckIntent.setClass(context, BootBroadcastReceiver.class);
            mHandler.postDelayed(() -> context.sendBroadcast(recheckIntent),
                    RESCHEDULE_FOR_BOOT_ACTION);
        } else if (ACTION_SETUP_WIZARD_FINISHED.equals(action)) {
            ElapsedTimeUtils.storeSuwFinishedTimestamp(context, System.currentTimeMillis());
        }
    }

    private static void refreshJobs(Context context) {
        // Clears useless data from battery usage database if needed.
        DatabaseUtils.clearExpiredDataIfNeeded(context);
        PeriodicJobManager.getInstance(context).refreshJob();
    }
}
