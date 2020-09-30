/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.backup;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment showing the items to launch different backup settings screens.
 */
@SearchIndexable
public class BackupSettingsFragment extends DashboardFragment {
    private static final String TAG = "BackupSettings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        // update information when we navigate back from TransportActivity
        displayResourceTilesToScreen(getPreferenceScreen());
    }

    /**
     * Get the tag string for logging.
     */
    @Override
    protected String getLogTag() {
        return TAG;
    }

    /**
     * Get the res id for static preference xml for this fragment.
     */
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.backup_settings;
    }

    /**
     * Get a list of {@link AbstractPreferenceController} for this fragment.
     */
    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new BackupSettingsPreferenceController(context));
        return controllers;
    }

    // The intention is to index {@link UserBackupSettingsActivity} instead of the fragments,
    // therefore leaving this index provider empty.
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BACKUP_SETTINGS;
    }
}
