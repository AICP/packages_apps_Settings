/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import java.util.UUID;

import android.app.Profile;
import android.app.ProfileGroup;
import android.app.ProfileGroup.Mode;
import android.app.ProfileManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.media.RingtoneManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.content.Context;
import android.provider.MediaStore;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ProfileGroupConfig extends SettingsPreferenceFragment implements
    OnPreferenceChangeListener {

    public static final String PROFILE_SERVICE = "profile";

    private static final CharSequence KEY_SOUNDMODE = "sound_mode";

    private static final CharSequence KEY_VIBRATEMODE = "vibrate_mode";

    private static final CharSequence KEY_LIGHTSMODE = "lights_mode";

    private static final CharSequence KEY_RINGERMODE = "ringer_mode";

    private static final CharSequence KEY_SOUNDTONE = "soundtone";

    private static final CharSequence KEY_RINGTONE = "ringtone";

    Profile mProfile;

    ProfileGroup mProfileGroup;

    private ListPreference mSoundMode;

    private ListPreference mRingerMode;

    private ListPreference mVibrateMode;

    private ListPreference mLightsMode;

    private ProfileRingtonePreference mRingTone;

    private ProfileRingtonePreference mSoundTone;

    private ProfileManager mProfileManager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.profile_settings);

        final Bundle args = getArguments();
        if (args != null) {
            mProfile = (Profile) args.getParcelable("Profile");
            UUID uuid = UUID.fromString(args.getString("ProfileGroup"));

            mProfileManager = (ProfileManager) getSystemService(PROFILE_SERVICE);
            mProfileGroup = mProfile.getProfileGroup(uuid);

            mRingerMode = (ListPreference) findPreference(KEY_RINGERMODE);
            mSoundMode = (ListPreference) findPreference(KEY_SOUNDMODE);
            mVibrateMode = (ListPreference) findPreference(KEY_VIBRATEMODE);
            mLightsMode = (ListPreference) findPreference(KEY_LIGHTSMODE);
            mRingTone = (ProfileRingtonePreference) findPreference(KEY_RINGTONE);
            mSoundTone = (ProfileRingtonePreference) findPreference(KEY_SOUNDTONE);

            mRingTone.setShowSilent(false);
            mSoundTone.setShowSilent(false);

            mSoundMode.setOnPreferenceChangeListener(this);
            mRingerMode.setOnPreferenceChangeListener(this);
            mVibrateMode.setOnPreferenceChangeListener(this);
            mLightsMode.setOnPreferenceChangeListener(this);
            mSoundTone.setOnPreferenceChangeListener(this);
            mRingTone.setOnPreferenceChangeListener(this);

            updateState();
        }
    }
    
    private void updateState() {

        mVibrateMode.setValue(mProfileGroup.getVibrateMode().name());
        mSoundMode.setValue(mProfileGroup.getSoundMode().name());
        mRingerMode.setValue(mProfileGroup.getRingerMode().name());
        mLightsMode.setValue(mProfileGroup.getLightsMode().name());

        mVibrateMode.setSummary(mVibrateMode.getEntry());
        mSoundMode.setSummary(mSoundMode.getEntry());
        mRingerMode.setSummary(mRingerMode.getEntry());
        mLightsMode.setSummary(mLightsMode.getEntry());

        if (mProfileGroup.getSoundOverride() != null) {
            mSoundTone.setRingtone(mProfileGroup.getSoundOverride());
            updateRingtoneName(mProfileGroup.getSoundOverride(), RingtoneManager.TYPE_NOTIFICATION, mSoundTone);
        }

        if (mProfileGroup.getRingerOverride() != null) {
            mRingTone.setRingtone(mProfileGroup.getRingerOverride());
            updateRingtoneName(mProfileGroup.getRingerOverride(), RingtoneManager.TYPE_RINGTONE, mRingTone);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVibrateMode) {
            mProfileGroup.setVibrateMode(Mode.valueOf((String) newValue));
        } else if (preference == mSoundMode) {
            mProfileGroup.setSoundMode(Mode.valueOf((String) newValue));
        } else if (preference == mRingerMode) {
            mProfileGroup.setRingerMode(Mode.valueOf((String) newValue));
        } else if (preference == mLightsMode) {
            mProfileGroup.setLightsMode(Mode.valueOf((String) newValue));
        } else if (preference == mRingTone) {
            Uri uri = Uri.parse((String) newValue);
            mProfileGroup.setRingerOverride(uri);
        } else if (preference == mSoundTone) {
            Uri uri = Uri.parse((String) newValue);
            mProfileGroup.setSoundOverride(uri);
        }

        mProfileManager.updateProfile(mProfile);

        updateState();
        return true;
    }

    private void updateRingtoneName(Uri ringtoneUri, int type, ProfileRingtonePreference preference) {
        if (preference == null) return;
        Context context = getActivity();
        if (context == null) return;
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        if (type == RingtoneManager.TYPE_NOTIFICATION){
            if (ringtoneUri.equals(Settings.System.DEFAULT_NOTIFICATION_URI)){
                ringtoneUri = defaultRingtoneUri;
            }
        } else if (type == RingtoneManager.TYPE_RINGTONE){
            if (ringtoneUri.equals(Settings.System.DEFAULT_RINGTONE_URI)){
                ringtoneUri = defaultRingtoneUri;
            }
        }
        CharSequence summary = context.getString(com.android.internal.R.string.ringtone_unknown);
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = context.getString(com.android.internal.R.string.ringtone_silent);
        } else {
            // Fetch the ringtone title from the media provider
            try {
                Cursor cursor = context.getContentResolver().query(ringtoneUri,
                        new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        summary = cursor.getString(0);
                    }
                    cursor.close();
                }
            } catch (SQLiteException sqle) {
                // Unknown title for the ringtone
            }
        }
        preference.setSummary(summary);
    }
}
