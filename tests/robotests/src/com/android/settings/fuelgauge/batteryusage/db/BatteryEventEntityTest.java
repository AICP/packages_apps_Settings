/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage.db;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BatteryEventEntity}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryEventEntityTest {

    @Test
    public void testBuilder_returnsExpectedResult() {
        final long timestamp = 10001L;
        final int batteryEventType = 1;
        final int batteryLevel = 66;

        BatteryEventEntity entity =
                BatteryEventEntity.newBuilder()
                        .setTimestamp(timestamp)
                        .setBatteryEventType(batteryEventType)
                        .setBatteryLevel(batteryLevel)
                        .build();

        // Verifies the app relative information.
        assertThat(entity.timestamp).isEqualTo(timestamp);
        assertThat(entity.batteryEventType).isEqualTo(batteryEventType);
        assertThat(entity.batteryLevel).isEqualTo(batteryLevel);
    }
}
