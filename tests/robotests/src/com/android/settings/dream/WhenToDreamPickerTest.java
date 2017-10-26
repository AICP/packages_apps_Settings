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

package com.android.settings.dream;

import android.app.Activity;
import android.content.Context;
import android.os.UserManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.dream.DreamBackend;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WhenToDreamPickerTest {
    private WhenToDreamPicker mPicker;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DreamBackend mBackend;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        FakeFeatureFactory.setupForTest(mActivity);

        mPicker = new WhenToDreamPicker();
        mPicker.onAttach((Context)mActivity);

        ReflectionHelpers.setField(mPicker, "mBackend", mBackend);
    }

    @Test
    public void getDefaultKeyReturnsCurrentWhenToDreamSetting() {
        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.WHILE_CHARGING);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(DreamSettings.getKeyFromSetting(DreamBackend.WHILE_CHARGING));

        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.WHILE_DOCKED);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(DreamSettings.getKeyFromSetting(DreamBackend.WHILE_DOCKED));

        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.EITHER);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(DreamSettings.getKeyFromSetting(DreamBackend.EITHER));

        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.NEVER);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(DreamSettings.getKeyFromSetting(DreamBackend.NEVER));
    }

    @Test
    public void setDreamWhileCharging() {
        String key = DreamSettings.getKeyFromSetting(DreamBackend.WHILE_CHARGING);
        mPicker.setDefaultKey(key);
        verify(mBackend).setWhenToDream(DreamBackend.WHILE_CHARGING);
    }

    @Test
    public void setDreamWhileDocked() {
        String key = DreamSettings.getKeyFromSetting(DreamBackend.WHILE_DOCKED);
        mPicker.setDefaultKey(key);
        verify(mBackend).setWhenToDream(DreamBackend.WHILE_DOCKED);
    }

    @Test
    public void setDreamWhileChargingOrDocked() {
        String key = DreamSettings.getKeyFromSetting(DreamBackend.EITHER);
        mPicker.setDefaultKey(key);
        verify(mBackend).setWhenToDream(DreamBackend.EITHER);
    }

    @Test
    public void setDreamNever() {
        String key = DreamSettings.getKeyFromSetting(DreamBackend.NEVER);
        mPicker.setDefaultKey(key);
        verify(mBackend).setWhenToDream(DreamBackend.NEVER);
    }
}
