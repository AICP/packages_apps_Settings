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

package com.android.settings.ui

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.assertHasTexts
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatterySettingsTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        device.startMainActivityFromHomeScreen(Intent.ACTION_POWER_USAGE_SUMMARY)
    }

    @Test
    fun hasTexts() {
        device.assertHasTexts(ON_SCREEN_TEXTS)
    }

    private companion object {
        // Items we really want to always show
        val ON_SCREEN_TEXTS = listOf(
            "Battery usage",
            "Battery Saver",
            "Battery percentage",
            "Remaining battery life is approximate and can change based on usage"
        )
    }
}
