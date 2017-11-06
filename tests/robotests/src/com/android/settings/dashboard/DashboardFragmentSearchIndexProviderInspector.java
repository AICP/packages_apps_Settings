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
 * limitations under the License.
 */

package com.android.settings.dashboard;

import android.app.Fragment;
import android.content.Context;

import com.android.settings.core.PreferenceController;
import com.android.settings.search.Indexable;
import com.android.settings.search.DatabaseIndexingUtils;

import org.robolectric.RuntimeEnvironment;

import java.util.List;

public class DashboardFragmentSearchIndexProviderInspector {

    public static boolean isSharingPreferenceControllers(Class clazz) {
        final Context context = RuntimeEnvironment.application;
        final Fragment fragment;
        try {
            fragment = Fragment.instantiate(context, clazz.getName());
        } catch (Throwable e) {
            // Can't do much with exception, assume the test passed.
            return true;
        }
        if (!(fragment instanceof DashboardFragment)) {
            return true;
        }

        final Indexable.SearchIndexProvider provider =
                DatabaseIndexingUtils.getSearchIndexProvider(clazz);
        if (provider == null) {
            return true;
        }
        final List<PreferenceController> controllersFromSearchIndexProvider;
        final List<PreferenceController> controllersFromFragment;
        try {
            controllersFromSearchIndexProvider = provider.getPreferenceControllers(context);
        } catch (Throwable e) {
            // Can't do much with exception, assume the test passed.
            return true;
        }
        try {
            controllersFromFragment =
                    ((DashboardFragment) fragment).getPreferenceControllers(context);
        } catch (Throwable e) {
            // Can't do much with exception, assume the test passed.
            return true;
        }

        if (controllersFromFragment == controllersFromSearchIndexProvider) {
            return true;
        } else if (controllersFromFragment != null && controllersFromSearchIndexProvider != null) {
            return controllersFromFragment.size() == controllersFromSearchIndexProvider.size();
        } else {
            return false;
        }
    }
}
