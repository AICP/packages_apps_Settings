/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PickupGestureSettings extends DashboardFragment {

    private static final String TAG = "PickupGestureSettings";
    private static final String KEY_PICK_UP = "gesture_pick_up";

    public static final String PREF_KEY_SUGGESTION_COMPLETE =
            "pref_pickup_gesture_suggestion_complete";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SuggestionFeatureProvider suggestionFeatureProvider = FeatureFactory.getFactory(context)
                .getSuggestionFeatureProvider(context);
        SharedPreferences prefs = suggestionFeatureProvider.getSharedPrefs(context);
        prefs.edit().putBoolean(PREF_KEY_SUGGESTION_COMPLETE, true).apply();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_GESTURE_PICKUP;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.pick_up_gesture_settings;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_pickup_gesture;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<PreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new PickupGesturePreferenceController(context, lifecycle,
                new AmbientDisplayConfiguration(context), UserHandle.myUserId(), KEY_PICK_UP));
        return controllers;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.pick_up_gesture_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<PreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null /* lifecycle */);
                }
            };

}
