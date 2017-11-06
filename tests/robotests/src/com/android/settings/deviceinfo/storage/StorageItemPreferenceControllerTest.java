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
 * limitations under the License
 */
package com.android.settings.deviceinfo.storage;


import static com.android.settings.utils.FileSizeFormatter.MEGABYTE_IN_BYTES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.PreferenceScreen;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.SubSettings;
import com.android.settings.TestConfig;
import com.android.settings.applications.ManageApplications;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.deviceinfo.PrivateVolumeSettings;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class StorageItemPreferenceControllerTest {
    private Context mContext;
    private VolumeInfo mVolume;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock
    private StorageVolumeProvider mSvp;
    private StorageItemPreferenceController mController;
    private StorageItemPreference mPreference;
    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        SettingsShadowResources.overrideResource("android:string/fileSizeSuffix", "%1$s %2$s");
        SettingsShadowResources.overrideResource("android:string/gigabyteShort", "GB");
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        FakeFeatureFactory.setupForTest(mContext);
        mFakeFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        mVolume = spy(new VolumeInfo("id", 0, null, "id"));
        // Note: null is passed as the Lifecycle because we are handling it outside of the normal
        //       Settings fragment lifecycle for test purposes.
        mController = new StorageItemPreferenceController(mContext, mFragment, mVolume, mSvp);
        mPreference = new StorageItemPreference(mContext);

        // Inflate the preference and the widget.
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(
                mPreference.getLayoutResource(), new LinearLayout(mContext), false);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void testUpdateStateWithInitialState() {
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.memory_calculating_size));
    }

    @Test
    public void testClickPhotos() {
        mPreference.setKey("pref_photos_videos");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getType()).isEqualTo("image/*");
        assertThat(intent.getAction()).isEqualTo(android.content.Intent.ACTION_VIEW);
        assertThat(intent.getBooleanExtra(Intent.EXTRA_FROM_STORAGE, false)).isTrue();
    }

    @Test
    public void testClickAudio() {
        mPreference.setKey("pref_music_audio");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));
        Intent intent = argumentCaptor.getValue();

        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ManageApplications.class.getName());
        assertThat(intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS).getInt(
                ManageApplications.EXTRA_STORAGE_TYPE, 0)).isEqualTo(
                ManageApplications.STORAGE_TYPE_MUSIC);
    }

    @Test
    public void handlePreferenceTreeClick_tappingAudioWhileUninitializedDoesntCrash() {
        mController.setVolume(null);

        mPreference.setKey("pref_music_audio");
        mController.handlePreferenceTreeClick(mPreference);
    }

    @Test
    public void testClickApps() {
        mPreference.setKey("pref_other_apps");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ManageApplications.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.apps_storage);
    }

    @Test
    public void handlePreferenceTreeClick_tappingAppsWhileUninitializedDoesntCrash() {
        mController.setVolume(null);

        mPreference.setKey("pref_other_apps");
        mController.handlePreferenceTreeClick(mPreference);
    }

    @Test
    public void testClickFiles() {
        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(mVolume);
        mPreference.setKey("pref_files");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        Intent browseIntent = mVolume.buildBrowseIntent();
        assertThat(intent.getAction()).isEqualTo(browseIntent.getAction());
        assertThat(intent.getData()).isEqualTo(browseIntent.getData());
        verify(mMetricsFeatureProvider, times(1)).action(
                nullable(Context.class), eq(MetricsEvent.STORAGE_FILES));
    }

    @Test
    public void testClickGames() {
        mPreference.setKey("pref_games");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ManageApplications.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.game_storage_settings);
    }

    @Test
    public void testClickMovies() {
        mPreference.setKey("pref_movies");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ManageApplications.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.storage_movies_tv);
    }

    @Test
    public void testClickSystem() {
        mPreference.setKey("pref_system");
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();

        verify(mFragment.getFragmentManager().beginTransaction()).add(
                nullable(PrivateVolumeSettings.SystemInfoFragment.class), nullable(String.class));
    }

    @Test
    public void testMeasurementCompletedUpdatesPreferences() {
        StorageItemPreference audio = new StorageItemPreference(mContext);
        StorageItemPreference image = new StorageItemPreference(mContext);
        StorageItemPreference games = new StorageItemPreference(mContext);
        StorageItemPreference movies = new StorageItemPreference(mContext);
        StorageItemPreference apps = new StorageItemPreference(mContext);
        StorageItemPreference system = new StorageItemPreference(mContext);
        StorageItemPreference files = new StorageItemPreference(mContext);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.AUDIO_KEY))).thenReturn(audio);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.PHOTO_KEY))).thenReturn(image);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.MOVIES_KEY))).thenReturn(movies);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.OTHER_APPS_KEY))).thenReturn(apps);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.SYSTEM_KEY))).thenReturn(system);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.FILES_KEY))).thenReturn(files);
        mController.displayPreference(screen);

        mController.setUsedSize(MEGABYTE_IN_BYTES * 970); // There should 870MB attributed.
        StorageAsyncLoader.AppsStorageResult result = new StorageAsyncLoader.AppsStorageResult();
        result.gamesSize = MEGABYTE_IN_BYTES * 80;
        result.videoAppsSize = MEGABYTE_IN_BYTES * 160;
        result.musicAppsSize = MEGABYTE_IN_BYTES * 40;
        result.otherAppsSize = MEGABYTE_IN_BYTES * 90;
        result.externalStats =
                new StorageStatsSource.ExternalStorageStats(
                        MEGABYTE_IN_BYTES * 500, // total
                        MEGABYTE_IN_BYTES * 100, // audio
                        MEGABYTE_IN_BYTES * 150, // video
                        MEGABYTE_IN_BYTES * 200, 0); // image

        SparseArray<StorageAsyncLoader.AppsStorageResult> results = new SparseArray<>();
        results.put(0, result);
        mController.onLoadFinished(results, 0);

        assertThat(audio.getSummary().toString()).isEqualTo("0.14GB");
        assertThat(image.getSummary().toString()).isEqualTo("0.35GB");
        assertThat(games.getSummary().toString()).isEqualTo("0.08GB");
        assertThat(movies.getSummary().toString()).isEqualTo("0.16GB");
        assertThat(apps.getSummary().toString()).isEqualTo("0.09GB");
        assertThat(files.getSummary().toString()).isEqualTo("0.05GB");
    }

    @Test
    public void settingUserIdAppliesNewIcons() {
        StorageItemPreference audio = spy(new StorageItemPreference(mContext));
        audio.setIcon(R.drawable.ic_media_stream);
        StorageItemPreference video = spy(new StorageItemPreference(mContext));
        video.setIcon(R.drawable.ic_local_movies);
        StorageItemPreference image = spy(new StorageItemPreference(mContext));
        image.setIcon(R.drawable.ic_photo_library);
        StorageItemPreference games = spy(new StorageItemPreference(mContext));
        games.setIcon(R.drawable.ic_videogame_vd_theme_24);
        StorageItemPreference apps = spy(new StorageItemPreference(mContext));
        apps.setIcon(R.drawable.ic_storage_apps);
        StorageItemPreference system = spy(new StorageItemPreference(mContext));
        system.setIcon(R.drawable.ic_system_update_vd_theme_24);
        StorageItemPreference files = spy(new StorageItemPreference(mContext));
        files.setIcon(R.drawable.ic_folder_vd_theme_24);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.AUDIO_KEY))).thenReturn(audio);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.MOVIES_KEY))).thenReturn(video);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.PHOTO_KEY))).thenReturn(image);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.OTHER_APPS_KEY))).thenReturn(apps);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.SYSTEM_KEY))).thenReturn(system);
        when(screen.findPreference(
                eq(StorageItemPreferenceController.FILES_KEY))).thenReturn(files);
        mController.displayPreference(screen);

        mController.setUserId(new UserHandle(10));

        verify(audio, times(2)).setIcon(nullable(Drawable.class));
        verify(video, times(2)).setIcon(nullable(Drawable.class));
        verify(image, times(2)).setIcon(nullable(Drawable.class));
        verify(games, times(2)).setIcon(nullable(Drawable.class));
        verify(apps, times(2)).setIcon(nullable(Drawable.class));
        verify(system, times(2)).setIcon(nullable(Drawable.class));
        verify(files, times(2)).setIcon(nullable(Drawable.class));
    }

    @Test
    public void displayPreference_dontHideFilePreferenceWhenEmulatedInternalStorageUsed() {
        StorageItemPreference audio = new StorageItemPreference(mContext);
        StorageItemPreference image = new StorageItemPreference(mContext);
        StorageItemPreference games = new StorageItemPreference(mContext);
        StorageItemPreference apps = new StorageItemPreference(mContext);
        StorageItemPreference system = new StorageItemPreference(mContext);
        StorageItemPreference files = new StorageItemPreference(mContext);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(eq(StorageItemPreferenceController.AUDIO_KEY)))
                .thenReturn(audio);
        when(screen.findPreference(eq(StorageItemPreferenceController.PHOTO_KEY)))
                .thenReturn(image);
        when(screen.findPreference(eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(eq(StorageItemPreferenceController.OTHER_APPS_KEY)))
                .thenReturn(apps);
        when(screen.findPreference(eq(StorageItemPreferenceController.SYSTEM_KEY)))
                .thenReturn(system);
        when(screen.findPreference(eq(StorageItemPreferenceController.FILES_KEY)))
                .thenReturn(files);

        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(mVolume);
        when(mVolume.isMountedReadable()).thenReturn(true);

        mController.displayPreference(screen);

        verify(screen, times(0)).removePreference(files);
    }

    @Test
    public void displayPreference_hideFilePreferenceWhenEmulatedStorageUnreadable() {
        StorageItemPreference audio = new StorageItemPreference(mContext);
        StorageItemPreference image = new StorageItemPreference(mContext);
        StorageItemPreference games = new StorageItemPreference(mContext);
        StorageItemPreference apps = new StorageItemPreference(mContext);
        StorageItemPreference system = new StorageItemPreference(mContext);
        StorageItemPreference files = new StorageItemPreference(mContext);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(eq(StorageItemPreferenceController.AUDIO_KEY)))
                .thenReturn(audio);
        when(screen.findPreference(eq(StorageItemPreferenceController.PHOTO_KEY)))
                .thenReturn(image);
        when(screen.findPreference(eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(eq(StorageItemPreferenceController.OTHER_APPS_KEY)))
                .thenReturn(apps);
        when(screen.findPreference(eq(StorageItemPreferenceController.SYSTEM_KEY)))
                .thenReturn(system);
        when(screen.findPreference(eq(StorageItemPreferenceController.FILES_KEY)))
                .thenReturn(files);

        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(mVolume);
        when(mVolume.isMountedReadable()).thenReturn(false);

        mController.displayPreference(screen);

        verify(screen).removePreference(files);
    }

    @Test
    public void displayPreference_hideFilePreferenceWhenNoEmulatedInternalStorage() {
        StorageItemPreference audio = new StorageItemPreference(mContext);
        StorageItemPreference image = new StorageItemPreference(mContext);
        StorageItemPreference games = new StorageItemPreference(mContext);
        StorageItemPreference apps = new StorageItemPreference(mContext);
        StorageItemPreference system = new StorageItemPreference(mContext);
        StorageItemPreference files = new StorageItemPreference(mContext);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(eq(StorageItemPreferenceController.AUDIO_KEY)))
                .thenReturn(audio);
        when(screen.findPreference(eq(StorageItemPreferenceController.PHOTO_KEY)))
                .thenReturn(image);
        when(screen.findPreference(eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(eq(StorageItemPreferenceController.OTHER_APPS_KEY)))
                .thenReturn(apps);
        when(screen.findPreference(eq(StorageItemPreferenceController.SYSTEM_KEY)))
                .thenReturn(system);
        when(screen.findPreference(eq(StorageItemPreferenceController.FILES_KEY)))
                .thenReturn(files);

        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(null);

        mController.displayPreference(screen);

        verify(screen).removePreference(files);
    }

    @Test
    public void displayPreference_updateFilePreferenceToHideAfterSettingVolume() {
        StorageItemPreference audio = new StorageItemPreference(mContext);
        StorageItemPreference image = new StorageItemPreference(mContext);
        StorageItemPreference games = new StorageItemPreference(mContext);
        StorageItemPreference apps = new StorageItemPreference(mContext);
        StorageItemPreference system = new StorageItemPreference(mContext);
        StorageItemPreference files = new StorageItemPreference(mContext);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(eq(StorageItemPreferenceController.AUDIO_KEY)))
                .thenReturn(audio);
        when(screen.findPreference(eq(StorageItemPreferenceController.PHOTO_KEY)))
                .thenReturn(image);
        when(screen.findPreference(eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(eq(StorageItemPreferenceController.OTHER_APPS_KEY)))
                .thenReturn(apps);
        when(screen.findPreference(eq(StorageItemPreferenceController.SYSTEM_KEY)))
                .thenReturn(system);
        when(screen.findPreference(eq(StorageItemPreferenceController.FILES_KEY)))
                .thenReturn(files);

        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(mVolume);
        when(mVolume.isMountedReadable()).thenReturn(true);

        mController.displayPreference(screen);
        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(null);
        mController.setVolume(mVolume);

        verify(screen).removePreference(files);
    }


    @Test
    public void displayPreference_updateFilePreferenceToShowAfterSettingVolume() {
        StorageItemPreference audio = new StorageItemPreference(mContext);
        StorageItemPreference image = new StorageItemPreference(mContext);
        StorageItemPreference games = new StorageItemPreference(mContext);
        StorageItemPreference apps = new StorageItemPreference(mContext);
        StorageItemPreference system = new StorageItemPreference(mContext);
        StorageItemPreference files = new StorageItemPreference(mContext);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(eq(StorageItemPreferenceController.AUDIO_KEY)))
                .thenReturn(audio);
        when(screen.findPreference(eq(StorageItemPreferenceController.PHOTO_KEY)))
                .thenReturn(image);
        when(screen.findPreference(eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(eq(StorageItemPreferenceController.OTHER_APPS_KEY)))
                .thenReturn(apps);
        when(screen.findPreference(eq(StorageItemPreferenceController.SYSTEM_KEY)))
                .thenReturn(system);
        when(screen.findPreference(eq(StorageItemPreferenceController.FILES_KEY)))
                .thenReturn(files);

        // This will hide it initially.
        mController.displayPreference(screen);

        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(mVolume);
        when(mVolume.isMountedReadable()).thenReturn(true);

        // And we bring it back.
        mController.setVolume(mVolume);

        verify(screen).addPreference(files);
    }
}
