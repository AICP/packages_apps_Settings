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

package com.android.settings.spa.system

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.SettingsIcon

object LanguageAndInputPageProvider : SettingsPageProvider {
    override val name = "LanguageAndInput"

    override fun isEnabled(arguments: Bundle?) = false

    @Composable
    override fun Page(arguments: Bundle?) {
        LanguageAndInput()
    }

    @Composable
    fun EntryItem() {
        val summary = stringResource(R.string.language_settings)
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.language_settings)
            override val summary = { summary }
            override val onClick = navigator(name)
            override val icon = @Composable {
                SettingsIcon(imageVector = Icons.Outlined.Language)
            }
        })
    }

}

@Composable
private fun LanguageAndInput() {
    RegularScaffold(title = stringResource(R.string.language_settings)) {
        AppLanguagesPageProvider.EntryItem()
    }
}