/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.Application;

import com.android.settings.activityembedding.ActivityEmbeddingRulesController;

import java.lang.ref.WeakReference;

/** Settings application which sets up activity embedding rules for the large screen device. */
public class SettingsApplication extends Application {

    private WeakReference<Activity> mHomeActivity = new WeakReference<>(null);

    @Override
    public void onCreate() {
        super.onCreate();

        final ActivityEmbeddingRulesController controller =
                new ActivityEmbeddingRulesController(this);
        controller.initRules();
    }

    public void setHomeActivity(Activity homeActivity) {
        mHomeActivity = new WeakReference<>(homeActivity);
    }

    public Activity getHomeActivity() {
        return mHomeActivity.get();
    }
}
