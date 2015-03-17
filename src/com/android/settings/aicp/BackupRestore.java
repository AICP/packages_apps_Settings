/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
 * Copyright (C) 2014 The Android Ice Cold Project
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

package com.android.settings.aicp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import com.android.settings.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BackupRestore extends SettingsPreferenceFragment
        implements OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "BackupRestore";


    private static final int REQUEST_PICK_RESTORE_FILE = 201;

    private static final String PREF_BACKUP = "backup";
    private static final String PREF_RESTORE = "restore";
    private static final String SETTINGS_DB_PATH = "/data/user/0/com.android.providers.settings/databases/settings.db";
    private static final String BACKUP_PATH = new File(Environment
            .getExternalStorageDirectory(), "/AICP_ota/").getAbsolutePath();

    private Preference mBackup;
    private Preference mRestore;

    private AlertDialog mRestoreDialog;
    private String mSettingsDbPath;

    private Context mContext;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.aicp_backup_restore);

        ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getPackageManager();
        Resources res = getResources();
        mContext = getActivity();

        mBackup = findPreference(PREF_BACKUP);
        mRestore = findPreference(PREF_RESTORE);

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mBackup) {
            runBackup();
            return true;
        } else if (preference == mRestore) {
            openRestoreDialog();
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

      //  return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_RESTORE_FILE) {
                if (data == null) {
                    //Nothing returned by user, probably pressed back button in file manager
                    return;
                }
                mSettingsDbPath = data.getData().getPath();
                openRestoreDialog();
            }
        }
    }

    private void openRestoreDialog() {
        if (mRestoreDialog != null) {
            mRestoreDialog.cancel();
            mRestoreDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.backup_title);
        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                copyRestoreFile(dialog, mSettingsDbPath);
                // wait till the file gets copied, than restart UI
                try
                {
                    Thread.sleep(1500);
                } catch (InterruptedException ie)
                {
                    ie.printStackTrace();
                }
                Helpers.restartSystem();
            }
        });
        builder.setNeutralButton(R.string.choose_restore_file,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PackageManager packageManager = getActivity().getPackageManager();
                        Intent test = new Intent(Intent.ACTION_GET_CONTENT);
                        test.setType("file/*");
                        List<ResolveInfo> list = packageManager.queryIntentActivities(test,
                                PackageManager.GET_ACTIVITIES);
                        if (!list.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                            intent.setType("file/*");
                            startActivityForResult(intent, REQUEST_PICK_RESTORE_FILE);
                        } else {
                            //No app installed to handle the intent - file explorer required
                            Toast.makeText(mContext, R.string.install_file_manager_error,
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
        builder.setNegativeButton(com.android.internal.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        mRestoreDialog = builder.create();
        mRestoreDialog.setOwnerActivity(getActivity());
        mRestoreDialog.show();
    }

    private void copyRestoreFile(DialogInterface dialog, String settingsDbPath) {
        new AbstractAsyncSuCMDProcessor() {
            @Override
            protected void onPostExecute(String result) {
            }
        }.execute("mount -o rw,remount /system",
                "cp -f " + settingsDbPath + " " + SETTINGS_DB_PATH,
                "chmod 660 " + SETTINGS_DB_PATH,
                "mount -o ro,remount /system");
    }

    private void runBackup() {
        DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
        Date date = new Date();
        String current = (dateFormat.format(date));
        new AbstractAsyncSuCMDProcessor() {
            @Override
            protected void onPostExecute(String result) {
            }
        }.execute("cp -f " + SETTINGS_DB_PATH + " " + BACKUP_PATH + "/settings.db_" + current);
    }

}
