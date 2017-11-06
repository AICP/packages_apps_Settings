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
 *
 */

package com.android.settings.search;

import android.content.Context;
import com.android.settings.search.DatabaseResultLoader;
import com.android.settings.search.SearchResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Mock loader to subvert the requirements of returning data while also driving the Loader
 * lifecycle.
 */
class MockDBLoader extends DatabaseResultLoader {

    public MockDBLoader(Context context) {
        super(context, "test", null);
    }

    @Override
    public Set<? extends SearchResult> loadInBackground() {
        return new HashSet<>();
    }

    @Override
    protected void onDiscardResult(Set<? extends SearchResult> result) {

    }
}
