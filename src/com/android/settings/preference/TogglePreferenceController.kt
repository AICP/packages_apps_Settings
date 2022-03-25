/*
 * Copyright (C) 2022 FlamingoOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.preference

import android.content.Context
import android.widget.Switch

import androidx.preference.Preference
import androidx.preference.PreferenceScreen

import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.OnMainSwitchChangeListener

abstract class TogglePreferenceController(
    context: Context,
    key: String,
) : TogglePreferenceController(context, key),
    OnMainSwitchChangeListener {

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val preference = screen.findPreference<Preference>(preferenceKey) ?: return
        if (preference is MainSwitchPreference) {
            preference.addOnSwitchChangeListener(this)
        }
    }

    override fun onSwitchChanged(switchView: Switch, isChecked: Boolean) {
        setChecked(isChecked)
    }

    override fun getSliceHighlightMenuRes() = R.string.menu_key_rice
}
