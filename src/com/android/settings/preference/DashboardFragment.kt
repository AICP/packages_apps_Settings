/*
 * Copyright (C) 2022 FlamingoOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.preference

import androidx.preference.Preference

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.dashboard.DashboardFragment

abstract class DashboardFragment: DashboardFragment() {
    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.AICP_METRICS

    override fun onDisplayPreferenceDialog(preference: Preference) {
        super.onDisplayPreferenceDialog(preference)
    }

    companion object {
        const val REQUEST_KEY = "DashboardFragment#RequestKey"
    }
}
