/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterysaver;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            com.android.settings.testutils.shadow.ShadowFragment.class,
        })
public class BatterySaverSettingsTest {
    BatterySaverSettings mFragment;
    @Mock PreferenceScreen mScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new BatterySaverSettings());
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void setupFooter_linkAddedWhenAppropriate() {
        doReturn("").when(mFragment).getText(anyInt());
        doReturn("").when(mFragment).getString(anyInt());
        mFragment.setupFooter();
        verify(mFragment, never()).addHelpLink();

        doReturn("testString").when(mFragment).getText(anyInt());
        doReturn("testString").when(mFragment).getString(anyInt());
        mFragment.setupFooter();
        verify(mFragment, atLeastOnce()).addHelpLink();
    }
}
