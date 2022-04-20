/*
 * Copyright (C) 2021-2022 AOSP-Krypton Project
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

package com.android.settings.security.applock.miscellaneous

import android.content.Context

import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.search.SearchIndexable
import com.android.settings.security.applock.AicpDashboardFragment

@SearchIndexable
class MiscellaneousSettingsFragment : AicpDashboardFragment() {

    override protected fun getPreferenceScreenResId() = R.xml.app_lock_settings

    override protected fun getLogTag() = TAG

    override protected fun createPreferenceControllers(
        context: Context
    ): List<AbstractPreferenceController> = buildPreferenceControllers(
        context,
        this /* host */,
    )

    companion object {
        private const val TAG = "MiscellaneousSettingsFragment"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.app_lock_settings) {
            override fun createPreferenceControllers(
                context: Context
            ): List<AbstractPreferenceController> = buildPreferenceControllers(
                context,
                null /* host */,
            )
        }

        private fun buildPreferenceControllers(
            context: Context,
            host: MiscellaneousSettingsFragment?,
        ): List<AbstractPreferenceController> = listOf(
            HiddenAppSettingsPreferenceController(context, host)
        )
    }
}
