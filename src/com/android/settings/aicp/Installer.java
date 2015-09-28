/*
 * Copyright (C) 2015 AICP
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.SlimSeekBarPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.CommandResult;
import com.android.settings.util.Helpers;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Installer extends SettingsPreferenceFragment implements
        Indexable, OnPreferenceChangeListener {

    private static final String TAG = "Installer";

    private static final String CONF_FILE = "/system/etc/persist.conf";

    public static final String ETC_HOSTS = "etc/hosts";
    public static final String BIN_APP_PROCESS = "bin/app_process";
    public static final String PERSIST_ENABLE = "persist_enable";
    public static final String PERSIST_HOSTS = "persist_file_hosts";
    public static final String PERSIST_XPOSED = "persist_file_xposed";
    public static final String PERSIST_PROPS = "persist_props";
    public static final String PERSIST_FILES = "persist_files";
    public static final String PREF_PERSIST_FILE_XPOSED = "persist_file_xposed";

    SwitchPreference mPrefPersistEnable;
    SwitchPreference mPrefPersistHosts;
    SwitchPreference mPrefPersistXposed;

    boolean mPersistEnable;
    ArrayList<String> mPersistProps;
    ArrayList<String> mPersistFiles;
    ArrayList<String> mPersistTrailer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.aicp_installer_settings);

        final PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mPrefPersistEnable = (SwitchPreference) prefScreen.findPreference(PERSIST_ENABLE);
        mPrefPersistEnable.setChecked(Settings.System.getIntForUser(resolver,
                Settings.System.INSTALLER_PERSISTANCE, 0, UserHandle.USER_CURRENT) == 1);
        mPrefPersistEnable.setOnPreferenceChangeListener(this);

        mPrefPersistHosts = (SwitchPreference) prefScreen.findPreference(PERSIST_HOSTS);
        mPrefPersistEnable.setChecked(Settings.System.getIntForUser(resolver,
                Settings.System.INSTALLER_PERSISTANCE_HOSTS, 0, UserHandle.USER_CURRENT) == 1);
        mPrefPersistHosts.setOnPreferenceChangeListener(this);

        mPrefPersistXposed = (SwitchPreference) prefScreen.findPreference(PERSIST_XPOSED);
        mPrefPersistEnable.setChecked(Settings.System.getIntForUser(resolver,
                Settings.System.INSTALLER_PERSISTANCE_XPOSED, 0, UserHandle.USER_CURRENT) == 1);
        mPrefPersistXposed.setOnPreferenceChangeListener(this);

        if(!isAppInstalled("de.robv.android.xposed.installer")) {
            prefScreen.removePreference(mPrefPersistXposed);
        }

        loadPrefs();

    }


    @Override
    public void onResume() {
        super.onResume();

        loadPrefs();
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mPrefPersistEnable) {
            Settings.System.putIntForUser(resolver,
                Settings.System.INSTALLER_PERSISTANCE,
                    ((Boolean) newValue) ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mPrefPersistHosts) {
            if (newValue.toString().equals("true")) {
                if (!mPersistFiles.contains(ETC_HOSTS)) {
                    mPersistFiles.add(ETC_HOSTS);
                }
            } else if (newValue.toString().equals("false")) {
                mPersistFiles.remove(ETC_HOSTS);
            }
            Settings.System.putIntForUser(resolver,
                Settings.System.INSTALLER_PERSISTANCE_HOSTS,
                    ((Boolean) newValue) ? 1 : 0, UserHandle.USER_CURRENT);
            savePrefs();
            loadPrefs(); // refresh
            return true;
        } else if (preference == mPrefPersistXposed) {
            if (newValue.toString().equals("true")) {
                if (!mPersistFiles.contains(BIN_APP_PROCESS)) {
                    mPersistFiles.add(BIN_APP_PROCESS);
                }
            } else if (newValue.toString().equals("false")) {
                mPersistFiles.remove(BIN_APP_PROCESS);
            }
            Settings.System.putIntForUser(resolver,
                Settings.System.INSTALLER_PERSISTANCE_XPOSED,
                    ((Boolean) newValue) ? 1 : 0, UserHandle.USER_CURRENT);
            savePrefs();
            loadPrefs(); // refresh
            return true;
        }
        return false;
    }

    boolean loadPrefs() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                mPersistEnable = true;
                mPersistProps = new ArrayList<String>();
                mPersistFiles = new ArrayList<String>();
                mPersistTrailer = new ArrayList<String>();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(CONF_FILE), 1024);
                    boolean inTrailer = false;
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("# END REPLACE")) {
                            inTrailer = true;
                        }
                        if (!inTrailer) {
                            if (line.startsWith("persist_")) {
                                String[] fields = line.split("=", 2);
                                if (fields[0].equals(PERSIST_ENABLE)) {
                                    mPersistEnable = stringToBool(fields[1]);
                                }
                                if (fields[0].equals(PERSIST_PROPS)) {
                                    mPersistProps = stringToStringArray(fields[1]);
                                }
                                if (fields[0].equals(PERSIST_FILES)) {
                                    mPersistFiles = stringToStringArray(fields[1]);
                                }
                            }
                        } else {
                            mPersistTrailer.add(line);
                        }
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Config file not found");
                } catch (IOException e) {
                    Log.e(TAG, "Exception reading config file: " + e.getMessage());
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            // Igonre
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (mPrefPersistEnable != null) {
                    mPrefPersistEnable.setChecked(mPersistEnable);
                }
                if (mPrefPersistHosts != null) {
                    mPrefPersistHosts.setChecked(mPersistFiles.contains(ETC_HOSTS));
                }
                if (mPrefPersistXposed != null) {
                    mPrefPersistXposed.setChecked(mPersistFiles.contains(BIN_APP_PROCESS));
                }
            }
        }.execute((Void) null);

        return true;
    }

    boolean savePrefs() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BufferedWriter bw = null;
                Helpers.getMount("rw");
                String[] cmdarray = new String[3];
                cmdarray[0] = "su";
                cmdarray[1] = "-c";
                cmdarray[2] = "cat > " + CONF_FILE;
                StringBuffer childStdin = new StringBuffer();
                childStdin.append("# /system/etc/persist.conf\n");
                childStdin.append("persist_enable=" + boolToString(mPersistEnable) + "\n");
                childStdin.append("persist_props=" + stringArrayToString(mPersistProps) + "\n");
                childStdin.append("persist_files=" + stringArrayToString(mPersistFiles) + "\n");
                for (String line : mPersistTrailer) {
                    childStdin.append(line + "\n");
                }
                CommandResult cr = CMDProcessor.runSysCmd(cmdarray, childStdin.toString());
                Log.i(TAG, "savePrefs: result=" + cr.getExitValue());
                Log.i(TAG, "savePrefs: stdout=" + cr.getStdout());
                Log.i(TAG, "savePrefs: stderr=" + cr.getStderr());
                Helpers.getMount("ro");
            }
        });
        return true;
    }

    private static boolean stringToBool(String val) {
        if (val.equals("0") ||
                val.equals("false") ||
                val.equals("False")) {
            return false;
        }
        return true;
    }

    private static String boolToString(boolean val) {
        return (val ? "true" : "false");
    }

    private static ArrayList<String> stringToStringArray(String val) {
        ArrayList<String> ret = new ArrayList<String>();
        int p1 = val.indexOf("\"");
        int p2 = val.lastIndexOf("\"");
        if (p1 >= 0 && p2 > p1 + 1) {
            String dqval = val.substring(p1 + 1, p2);
            for (String s : dqval.split(" +")) {
                ret.add(s);
            }
        }
        return ret;
    }

    private static String stringArrayToString(ArrayList<String> val) {
        String ret = "";
        boolean first = true;
        ret += "\"";
        for (String s : val) {
            if (!first) {
                ret += " ";
            }
            ret += s;
            first = false;
        }
        ret += "\"";
        return ret;
    }

    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getActivity().getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                    boolean enabled) {
            ArrayList<SearchIndexableResource> result =
                new ArrayList<SearchIndexableResource>();

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.aicp_installer_settings;
            result.add(sir);

            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList<String>();
            return result;
        }
    };
}
