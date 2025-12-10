/*
 * SPDX-FileCopyrightText: 2025 The LibreMobileOS Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.spa.network

import android.provider.Settings
import android.sysprop.TelephonyProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel

@Composable
fun Smart5gPreference() {
    // TODO: find a better way to check device-side 5G support
    val is5gSupported = TelephonyProperties.default_network()
        .any { it > 22 /* NETWORK_MODE_NR_LTE */ }
    if (!is5gSupported) return

    val context = LocalContext.current
    val summaryText = stringResource(R.string.smart_5g_summary)
    var isChecked by rememberSaveable {
        mutableStateOf(
            Settings.System.getInt(context.contentResolver, Settings.System.SMART_5G, 1) == 1
        )
    }
    SwitchPreference(
        object : SwitchPreferenceModel {
            override val title = stringResource(id = R.string.smart_5g_title)
            override val summary = { summaryText }
            override val checked = { isChecked }
            override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                if (
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SMART_5G,
                        if (newChecked) 1 else 0
                    )
                ) {
                    isChecked = newChecked
                }
            }
        }
    )
}
