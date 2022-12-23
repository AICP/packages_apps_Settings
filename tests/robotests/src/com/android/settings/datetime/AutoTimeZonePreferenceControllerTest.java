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

package com.android.settings.datetime;

import static android.app.time.DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_RUNNING;
import static android.app.time.DetectorStatusTypes.DETECTOR_STATUS_RUNNING;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_PRESENT;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_READY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.time.Capabilities;
import android.app.time.LocationTimeZoneAlgorithmStatus;
import android.app.time.TelephonyTimeZoneAlgorithmStatus;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.app.time.TimeZoneDetectorStatus;
import android.content.Context;
import android.os.UserHandle;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutoTimeZonePreferenceControllerTest {

    @Mock
    private UpdateTimeAndDateCallback mCallback;
    private Context mContext;
    private Preference mPreference;
    @Mock
    private TimeManager mTimeManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mPreference = new Preference(mContext);

        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
    }

    @Test
    public void isFromSUW_notAvailable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, true /* isFromSUW */);

        assertThat(controller.isAvailable()).isFalse();
    }

    @Test
    public void notFromSUW_isAvailable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* isFromSUW */);

        assertThat(controller.isAvailable()).isTrue();
    }

    @Test
    public void autoTimeZoneNotSupported_notAvailable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* fromSUW */);

        assertThat(controller.isAvailable()).isFalse();
    }

    @Test
    public void isFromSUW_notEnable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, true /* fromSUW */);

        assertThat(controller.isEnabled()).isFalse();
    }

    @Test
    public void isFromSUW_isEnable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, true /* fromSUW */);

        assertThat(controller.isEnabled()).isTrue();
    }

    @Test
    public void autoTimeZoneNotSupported_notEnable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* fromSUW */);

        assertThat(controller.isEnabled()).isFalse();
    }

    @Test
    public void testIsEnabled_shouldReadFromTimeManagerConfig() {
        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* fromSUW */);

        {
            // Disabled
            TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                    /* autoSupported= */true, /* autoEnabled= */false);
            when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

            assertThat(controller.isEnabled()).isFalse();
        }

        {
            // Enabled
            TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                    /* autoSupported= */true, /* autoEnabled= */true);
            when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

            assertThat(controller.isEnabled()).isTrue();
        }
    }

    @Test
    public void updatePreferenceChange_prefIsChecked_shouldUpdatePreferenceAndNotifyCallback() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeZoneConfiguration(Mockito.any())).thenReturn(true);

        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, mCallback, false /* fromSUW */);

        assertThat(controller.onPreferenceChange(mPreference, true)).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);

        // Check the service was asked to change the configuration correctly.
        TimeZoneConfiguration timeZoneConfiguration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(true)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(timeZoneConfiguration);

        // Update the mTimeManager mock so that it now returns the expected updated config.
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfigAfterUpdate =
                createCapabilitiesAndConfig(/* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig())
                .thenReturn(capabilitiesAndConfigAfterUpdate);

        assertThat(controller.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsUnchecked_shouldUpdatePreferenceAndNotifyCallback() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeZoneConfiguration(Mockito.any())).thenReturn(true);

        AutoTimeZonePreferenceController controller = new AutoTimeZonePreferenceController(
                mContext, mCallback, false /* fromSUW */);

        assertThat(controller.onPreferenceChange(mPreference, false)).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);

        // Check the service was asked to change the configuration correctly.
        TimeZoneConfiguration timeZoneConfiguration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(false)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(timeZoneConfiguration);

        // Update the mTimeManager mock so that it now returns the expected updated config.
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfigAfterUpdate =
                createCapabilitiesAndConfig(/* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig())
                .thenReturn(capabilitiesAndConfigAfterUpdate);

        assertThat(controller.isEnabled()).isFalse();
    }

    private static TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig(
            boolean autoSupported, boolean autoEnabled) {
        TimeZoneDetectorStatus status = new TimeZoneDetectorStatus(DETECTOR_STATUS_RUNNING,
                new TelephonyTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING),
                new LocationTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING,
                        PROVIDER_STATUS_NOT_READY, null,
                        PROVIDER_STATUS_NOT_PRESENT, null));
        int configureAutoDetectionEnabledCapability =
                autoSupported ? Capabilities.CAPABILITY_POSSESSED
                        : Capabilities.CAPABILITY_NOT_SUPPORTED;
        TimeZoneCapabilities capabilities = new TimeZoneCapabilities.Builder(UserHandle.SYSTEM)
                .setConfigureAutoDetectionEnabledCapability(configureAutoDetectionEnabledCapability)
                .setConfigureGeoDetectionEnabledCapability(Capabilities.CAPABILITY_NOT_SUPPORTED)
                .setSetManualTimeZoneCapability(Capabilities.CAPABILITY_POSSESSED)
                .build();
        TimeZoneConfiguration config = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(autoEnabled)
                .setGeoDetectionEnabled(false)
                .build();
        return new TimeZoneCapabilitiesAndConfig(status, capabilities, config);
    }
}
