/*
 * Copyright (C) 2013 The ChameleonOS Project
 * Copyright (C) 2016 The Dirty Unicorns project
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

package com.android.settings.aicp;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.utils.du.ActionHandler;
import com.android.internal.utils.du.Config;
import com.android.internal.utils.du.Config.ActionConfig;
import com.android.internal.utils.du.Config.ButtonConfig;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.aicp.ActionPreference;

public class ThreeFingerGesture extends ActionFragment implements OnPreferenceChangeListener {

    private static final String TAG = "ThreeFingerGesture";

    private static final String THREE_FINGER_GESTURE = "three_finger_gesture_action";

    private ActionPreference mThreeFingerSwipeGestures;

    private CharSequence mPreviousTitle;

    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.aicp_three_finger_gesture);

        PreferenceScreen prefSet = getPreferenceScreen();
        mContext = (Context) getActivity();

        mThreeFingerSwipeGestures = (ActionPreference) findPreference(THREE_FINGER_GESTURE);
        mThreeFingerSwipeGestures.setTag(THREE_FINGER_GESTURE);
        mThreeFingerSwipeGestures.setActionConfig(getSwipeThreeFingerGestures());
        mThreeFingerSwipeGestures.setDefaultActionConfig(new ActionConfig(getActivity()));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        final ActionBar bar = getActivity().getActionBar();
        mPreviousTitle = bar.getTitle();
        bar.setTitle(R.string.gestures_category);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().getActionBar().setTitle(mPreviousTitle);
    }

    @Override
    protected void findAndUpdatePreference(ActionConfig action, String tag) {
        if (TextUtils.equals(THREE_FINGER_GESTURE, tag)) {
            ActionConfig newAction;
            if (action == null) {
                newAction = mThreeFingerSwipeGestures.getDefaultActionConfig();
            } else {
                newAction = action;
            }
            mThreeFingerSwipeGestures.setActionConfig(newAction);
            setSwipeThreeFingerGestures(newAction);
        } else {
            super.findAndUpdatePreference(action, tag);
        }
    }

    private ActionConfig getSwipeThreeFingerGestures() {
        ButtonConfig config = ButtonConfig.getButton(mContext,
                Settings.Secure.THREE_FINGER_GESTURE, true);
        ActionConfig action;
        if (config == null) {
            action = new ActionConfig(getActivity());
        } else {
            action = config.getActionConfig(ActionConfig.PRIMARY);
        }
        return action;
    }

    private void setSwipeThreeFingerGestures(ActionConfig action) {
        ButtonConfig config = new ButtonConfig(getActivity());
        config.setActionConfig(action, ActionConfig.PRIMARY);
        ButtonConfig.setButton(getActivity(), config, Settings.Secure.THREE_FINGER_GESTURE, true);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }
}
