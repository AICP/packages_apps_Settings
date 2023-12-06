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

package com.android.settings.fuelgauge.batteryusage.bugreport;

import android.content.Context;
import android.util.Log;

import com.android.settings.fuelgauge.batteryusage.ConvertUtils;
import com.android.settings.fuelgauge.batteryusage.DatabaseUtils;
import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventDao;
import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/** A utility class to aggregate and provide required log data. */
public final class LogUtils {
    private static final String TAG = "LogUtils";
    private static final Duration DUMP_TIME_OFFSET = Duration.ofHours(24);
    private static final Duration DUMP_TIME_OFFSET_FOR_ENTRY = Duration.ofHours(4);

    static void dumpBatteryUsageDatabaseHist(Context context, PrintWriter writer) {
        // Dumps periodic job events.
        writer.println("\nBattery PeriodicJob History:");
        BatteryUsageLogUtils.printHistoricalLog(context, writer);
        writer.flush();

        // Dumps phenotype environments.
        DatabaseUtils.dump(context, writer);
        writer.flush();
        final BatteryStateDao dao =
                BatteryStateDatabase
                        .getInstance(context.getApplicationContext())
                        .batteryStateDao();
        final long timeOffset =
                Clock.systemUTC().millis() - DUMP_TIME_OFFSET.toMillis();

        // Gets all distinct timestamps.
        final List<Long> timestamps = dao.getDistinctTimestamps(timeOffset);
        final int distinctCount = timestamps.size();
        writer.println("\n\tBattery DatabaseHistory:");
        writer.println("distinct timestamp count:" + distinctCount);
        Log.w(TAG, "distinct timestamp count:" + distinctCount);
        if (distinctCount == 0) {
            return;
        }
        // Dumps all distinct timestamps.
        timestamps.forEach(timestamp -> {
            final String formattedTimestamp = ConvertUtils.utcToLocalTimeForLogging(timestamp);
            writer.println("\t" + formattedTimestamp);
            Log.w(TAG, "\t" + formattedTimestamp);
        });
        writer.flush();

        final List<BatteryState> stateList = dao.getAllAfter(
                Clock.systemUTC().millis() - DUMP_TIME_OFFSET_FOR_ENTRY.toMillis());
        stateList.stream().forEach(state -> writer.println(state));
    }

    static void dumpAppUsageDatabaseHist(Context context, PrintWriter writer) {
        final AppUsageEventDao dao =
                BatteryStateDatabase
                        .getInstance(context.getApplicationContext())
                        .appUsageEventDao();
        writer.println("\n\tApp DatabaseHistory:");
        final List<AppUsageEventEntity> eventList = dao.getAllAfter(
                Clock.systemUTC().millis() - DUMP_TIME_OFFSET_FOR_ENTRY.toMillis());
        eventList.stream().forEach(event -> writer.println(event));
        writer.flush();
    }

    private LogUtils() {}
}
