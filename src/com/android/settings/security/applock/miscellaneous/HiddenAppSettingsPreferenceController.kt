/*
 * Copyright (C) 2022 AOSP-Krypton Project
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.UserHandle

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.Preference

import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settings.password.ConfirmDeviceCredentialActivity
import com.android.settingslib.transition.SettingsTransitionHelper.TransitionType

class HiddenAppSettingsPreferenceController(
    context: Context,
    private val host: MiscellaneousSettingsFragment?,
) : BasePreferenceController(context, KEY) {

    private val lockPatternUtils = LockPatternUtils(context)
    private val securityPromptLauncher: ActivityResultLauncher<Intent>?

    init {
        securityPromptLauncher = host?.registerForActivityResult(
            StartActivityForResult()
        ) {
            if (it?.resultCode == Activity.RESULT_OK) {
                SubSettingLauncher(mContext)
                    .setDestination(HiddenAppSettingsFragment::class.qualifiedName)
                    .setSourceMetricsCategory(host.metricsCategory)
                    .setTransitionType(TransitionType.TRANSITION_SLIDE)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                    .launch()
            }
        }
    }

    override fun getAvailabilityStatus() = AVAILABLE

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == KEY &&
                securityPromptLauncher != null &&
                lockPatternUtils.isSecure(UserHandle.myUserId())
        ) {
            securityPromptLauncher.launch(
                ConfirmDeviceCredentialActivity.createIntent(
                    mContext.getString(R.string.hidden_app_authentication_dialog_title),
                    null /* details */,
                )
            )
            return true
        }
        return super.handlePreferenceTreeClick(preference)
    }

    companion object {
        private const val KEY = "hidden_apps"
    }
}
