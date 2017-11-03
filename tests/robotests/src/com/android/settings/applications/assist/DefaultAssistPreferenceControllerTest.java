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

package com.android.settings.applications.assist;

import android.Manifest;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.defaultapps.DefaultAppInfo;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultAssistPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private SearchManager mSearchManager;
    @Mock
    private PackageManager mPackageManager;
    private DefaultAssistPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DefaultAssistPreferenceController(mContext);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(shadows = {ShadowSecureSettings.class})
    public void getDefaultAppInfo_hasDefaultAssist_shouldReturnKey() {
        final String flattenKey = "com.android.settings/assist";
        ShadowSecureSettings.putString(null, Settings.Secure.ASSISTANT, flattenKey);
        DefaultAppInfo appInfo = mController.getDefaultAppInfo();

        assertThat(appInfo.getKey()).isEqualTo(flattenKey);
    }

    @Test
    public void getSettingIntent_noSettingsActivity_shouldNotCrash() {
        final String flattenKey = "com.android.settings/assist";
        ShadowSecureSettings.putString(null, Settings.Secure.ASSISTANT, flattenKey);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        DefaultAssistPreferenceController controller =
            spy(new DefaultAssistPreferenceController(mContext));
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.name = "assist";
        resolveInfo.activityInfo.applicationInfo = new ApplicationInfo();
        resolveInfo.activityInfo.applicationInfo.packageName = "com.android.settings";
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
            .thenReturn(resolveInfo);
        when(mContext.getSystemService(Context.SEARCH_SERVICE)).thenReturn(mSearchManager);
        when(mSearchManager.getAssistIntent(anyBoolean())).thenReturn(mock(Intent.class));
        final ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.permission = Manifest.permission.BIND_VOICE_INTERACTION;
        resolveInfo.serviceInfo = serviceInfo;
        final List<ResolveInfo> services = new ArrayList<>();
        services.add(resolveInfo);
        when(mPackageManager.queryIntentServices(any(Intent.class), anyInt())).thenReturn(services);
        doReturn(null).when(controller).getAssistSettingsActivity(
            ComponentName.unflattenFromString(flattenKey), resolveInfo, mPackageManager);

        controller.getSettingIntent(null);
        // should not crash
    }
}
