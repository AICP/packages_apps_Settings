/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.aicp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.cyanogenmod.PackageListAdapter;
import com.android.settings.cyanogenmod.PackageListAdapter.PackageItem;
import com.android.settings.util.Helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class HeadsUpSettings extends SettingsPreferenceFragment
        implements AdapterView.OnItemLongClickListener, Preference.OnPreferenceClickListener,
        OnPreferenceChangeListener {

    private static final int DIALOG_DND_APPS = 0;
    private static final int DIALOG_BLACKLIST_APPS = 1;

    private static final String PREF_HEADS_UP_TIME_OUT = "heads_up_time_out";
    private static final String PREF_HEADS_UP_EXPANDED = "heads_up_expanded";
    private static final String PREF_SHOW_HEADS_UP_BOTTOM = "show_heads_up_bottom";
    private static final String PREF_HEADS_UP_EXCLUDE_FROM_LOCK_SCREEN = "heads_up_exclude_from_lock_screen";
    private static final String PREF_HEADS_UP_BG_COLOR = "heads_up_bg_color";
    private static final String PREF_HEADS_UP_TEXT_COLOR = "heads_up_text_color";

    private PackageListAdapter mPackageAdapter;
    private PackageManager mPackageManager;
    private PreferenceGroup mDndPrefList;
    private PreferenceGroup mBlacklistPrefList;
    private Preference mAddDndPref;
    private Preference mAddBlacklistPref;
    private ListPreference mHeadsUpTimeOut;
    private CheckBoxPreference mHeadsUpExpanded;
    private CheckBoxPreference mHeadsExcludeFromLockscreen;
    private CheckBoxPreference mShowHeadsUpBottom;

    private ColorPickerPreference mHeadsUpBgColor;
    private ColorPickerPreference mHeadsUpTextColor;

    private String mDndPackageList;
    private String mBlacklistPackageList;
    private Map<String, Package> mDndPackages;
    private Map<String, Package> mBlacklistPackages;

    private Switch mActionBarSwitch;
    private HeadsUpEnabler mHeadsUpEnabler;

    private ViewGroup mPrefsContainer;
    private View mDisabledText;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x00ffffff;
    private static final int DEFAULT_TEXT_COLOR = 0xffffffff;

    private ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateEnabledState();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get launch-able applications
        addPreferencesFromResource(R.xml.heads_up_settings);
        mPackageManager = getPackageManager();
        mPackageAdapter = new PackageListAdapter(getActivity());
        PackageManager pm = getPackageManager();

        mDndPrefList = (PreferenceGroup) findPreference("dnd_applications_list");
        mDndPrefList.setOrderingAsAdded(false);

        mBlacklistPrefList = (PreferenceGroup) findPreference("blacklist_applications");
        mBlacklistPrefList.setOrderingAsAdded(false);

        mDndPackages = new HashMap<String, Package>();
        mBlacklistPackages = new HashMap<String, Package>();

        mAddDndPref = findPreference("add_dnd_packages");
        mAddBlacklistPref = findPreference("add_blacklist_packages");

        mAddDndPref.setOnPreferenceClickListener(this);
        mAddBlacklistPref.setOnPreferenceClickListener(this);

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            return;
        }

        int defaultTimeOut = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_notification_decay", null, null));
        mHeadsUpTimeOut = (ListPreference) findPreference(PREF_HEADS_UP_TIME_OUT);
        mHeadsUpTimeOut.setOnPreferenceChangeListener(this);
        int headsUpTimeOut = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_NOTIFCATION_DECAY, defaultTimeOut);
        mHeadsUpTimeOut.setValue(String.valueOf(headsUpTimeOut));
        updateHeadsUpTimeOutSummary(headsUpTimeOut);

        mHeadsUpExpanded = (CheckBoxPreference) findPreference(PREF_HEADS_UP_EXPANDED);
        mHeadsUpExpanded.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HEADS_UP_EXPANDED, 0, UserHandle.USER_CURRENT) == 1);
        mHeadsUpExpanded.setOnPreferenceChangeListener(this);

        mShowHeadsUpBottom = (CheckBoxPreference) findPreference(PREF_SHOW_HEADS_UP_BOTTOM);
        mShowHeadsUpBottom.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.SHOW_HEADS_UP_BOTTOM, 0, UserHandle.USER_CURRENT) == 1);
        mShowHeadsUpBottom.setOnPreferenceChangeListener(this);

        mHeadsExcludeFromLockscreen = (CheckBoxPreference) findPreference(PREF_HEADS_UP_EXCLUDE_FROM_LOCK_SCREEN);
        mHeadsExcludeFromLockscreen.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HEADS_UP_EXCLUDE_FROM_LOCK_SCREEN, 0, UserHandle.USER_CURRENT) == 1);
        mHeadsExcludeFromLockscreen.setOnPreferenceChangeListener(this);

        // Heads Up background color
        mHeadsUpBgColor =
                (ColorPickerPreference) findPreference(PREF_HEADS_UP_BG_COLOR);
        mHeadsUpBgColor.setOnPreferenceChangeListener(this);
        final int intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_BG_COLOR, 0x00ffffff);
        String hexColor = String.format("#%08x", (0x00ffffff & intColor));
        if (hexColor.equals("#00ffffff")) {
            mHeadsUpBgColor.setSummary(R.string.trds_default_color);
        } else {
            mHeadsUpBgColor.setSummary(hexColor);
        }
        mHeadsUpBgColor.setNewPreviewColor(intColor);

        // Heads Up text color
        mHeadsUpTextColor =
                (ColorPickerPreference) findPreference(PREF_HEADS_UP_TEXT_COLOR);
        mHeadsUpTextColor.setOnPreferenceChangeListener(this);
        final int intTextColor = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_TEXT_COLOR, 0x00000000);
        String hexTextColor = String.format("#%08x", (0x00000000 & intTextColor));
        if (hexTextColor.equals("#00000000")) {
            mHeadsUpTextColor.setSummary(R.string.trds_default_color);
        } else {
            mHeadsUpTextColor.setSummary(hexTextColor);
        }
        mHeadsUpTextColor.setNewPreviewColor(intTextColor);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        // We don't call super.onActivityCreated() here, since it assumes we already set up
        // Preference (probably in onCreate()), while ProfilesSettings exceptionally set it up in
        // this method.
        // On/off switch
        Activity activity = getActivity();
        //Switch
        mActionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            final int padding = activity.getResources().getDimensionPixelSize(
                    R.dimen.action_bar_switch_padding);
            mActionBarSwitch.setPaddingRelative(0, 0, padding, 0);
            activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(mActionBarSwitch, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.END));
        }

        mHeadsUpEnabler = new HeadsUpEnabler(activity, mActionBarSwitch);
        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(icicle);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.headsup_fragment, container, false);
        mPrefsContainer = (ViewGroup) v.findViewById(R.id.prefs_container);
        mDisabledText = v.findViewById(R.id.disabled_text);

        View prefs = super.onCreateView(inflater, mPrefsContainer, savedInstanceState);
        mPrefsContainer.addView(prefs);

        return v;
    }

    @Override
    public void onDestroyView() {
        getActivity().getActionBar().setCustomView(null);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mHeadsUpEnabler != null) {
            mHeadsUpEnabler.resume();
        }
        refreshCustomApplicationPrefs();
        getListView().setOnItemLongClickListener(this);
        getActivity().invalidateOptionsMenu();

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.HEADS_UP_NOTIFICATION),
                true, mSettingsObserver);
        updateEnabledState();

        // If running on a phone, remove padding around container
        // and the preference listview
        if (!Utils.isTablet(getActivity())) {
            mPrefsContainer.setPadding(0, 0, 0, 0);
            getListView().setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mHeadsUpEnabler != null) {
            mHeadsUpEnabler.pause();
        }
        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHeadsUpTimeOut) {
            int headsUpTimeOut = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_NOTIFCATION_DECAY,
                    headsUpTimeOut);
            updateHeadsUpTimeOutSummary(headsUpTimeOut);
            return true;
        } else if (preference == mHeadsUpExpanded) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HEADS_UP_EXPANDED,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mShowHeadsUpBottom) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.SHOW_HEADS_UP_BOTTOM,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mHeadsExcludeFromLockscreen) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HEADS_UP_EXCLUDE_FROM_LOCK_SCREEN,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHeadsUpBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.trds_default_color);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mHeadsUpTextColor) {
            String hexText = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hexText.equals("#00000000")) {
                preference.setSummary(R.string.trds_default_color);
            } else {
                preference.setSummary(hexText);
            }
            int intHexText = ColorPickerPreference.convertToColorInt(hexText);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_TEXT_COLOR,
                    intHexText);
            return true;
        }
        return false;
    }

    private void updateHeadsUpTimeOutSummary(int value) {
        String summary = getResources().getString(R.string.heads_up_time_out_summary,
                value / 1000);
        if (value == 0) {
            mHeadsUpTimeOut.setSummary(
                    getResources().getString(R.string.heads_up_time_out_never_summary));
        } else {
            mHeadsUpTimeOut.setSummary(summary);
        }
    }

    /**
     * Utility classes and supporting methods
     */
    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Dialog dialog;
        final ListView list = new ListView(getActivity());
        list.setAdapter(mPackageAdapter);

        builder.setTitle(R.string.profile_choose_app);
        builder.setView(list);
        dialog = builder.create();

        switch (id) {
            case DIALOG_DND_APPS:
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent,
                            View view, int position, long id) {
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        addCustomApplicationPref(info.packageName, mDndPackages);
                        dialog.cancel();
                    }
                });
                break;
            case DIALOG_BLACKLIST_APPS:
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent,
                            View view, int position, long id) {
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        addCustomApplicationPref(info.packageName, mBlacklistPackages);
                        dialog.cancel();
                    }
                });
        }
        return dialog;
    }


    /**
     * Application class
     */
    private static class Package {
        public String name;
        /**
         * Stores all the application values in one call
         * @param name
         */
        public Package(String name) {
            this.name = name;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            return builder.toString();
        }

        public static Package fromString(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }

            try {
                Package item = new Package(value);
                return item;
            } catch (NumberFormatException e) {
                return null;
            }
        }

    };

    private void refreshCustomApplicationPrefs() {
        if (!parsePackageList()) {
            return;
        }

        // Add the Application Preferences
        if (mDndPrefList != null && mBlacklistPrefList != null) {
            mDndPrefList.removeAll();
            mBlacklistPrefList.removeAll();

            for (Package pkg : mDndPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mDndPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }

            for (Package pkg : mBlacklistPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mBlacklistPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }
        }

        // Keep these at the top
        mAddDndPref.setOrder(0);
        mAddBlacklistPref.setOrder(0);
        // Add 'add' options
        mDndPrefList.addPreference(mAddDndPref);
        mBlacklistPrefList.addPreference(mAddBlacklistPref);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAddDndPref) {
            showDialog(DIALOG_DND_APPS);
        }

        if (preference == mAddBlacklistPref) {
            showDialog(DIALOG_BLACKLIST_APPS);
        }
        return true;
    }

    private void addCustomApplicationPref(String packageName, Map<String,Package> map) {
        Package pkg = map.get(packageName);
        if (pkg == null) {
            pkg = new Package(packageName);
            map.put(packageName, pkg);
            savePackageList(false, map);
            refreshCustomApplicationPrefs();
        }
    }

    private Preference createPreferenceFromInfo(Package pkg)
            throws PackageManager.NameNotFoundException {
        PackageInfo info = mPackageManager.getPackageInfo(pkg.name,
                PackageManager.GET_META_DATA);
        Preference pref = new Preference(getActivity());

        pref.setKey(pkg.name);
        pref.setTitle(info.applicationInfo.loadLabel(mPackageManager));
        pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
        pref.setPersistent(false);
        return pref;
    }

    private void removeApplicationPref(String packageName, Map<String,Package> map) {
        if (map.remove(packageName) != null) {
            savePackageList(false, map);
            refreshCustomApplicationPrefs();
        }
    }

    private boolean parsePackageList() {
        boolean parsed = false;

        final String dndString = Settings.System.getString(getContentResolver(),
                Settings.System.HEADS_UP_CUSTOM_VALUES);
        final String blacklistString = Settings.System.getString(getContentResolver(),
                Settings.System.HEADS_UP_BLACKLIST_VALUES);

        if (!TextUtils.equals(mDndPackageList, dndString)) {
            mDndPackageList = dndString;
            mDndPackages.clear();
            parseAndAddToMap(dndString, mDndPackages);
            parsed = true;
        }

        if (!TextUtils.equals(mBlacklistPackageList, blacklistString)) {
            mBlacklistPackageList = blacklistString;
            mBlacklistPackages.clear();
            parseAndAddToMap(blacklistString, mBlacklistPackages);
            parsed = true;
        }

        return parsed;
    }

    private void parseAndAddToMap(String baseString, Map<String,Package> map) {
        if (baseString == null) {
            return;
        }

        final String[] array = TextUtils.split(baseString, "\\|");
        for (String item : array) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            Package pkg = Package.fromString(item);
            map.put(pkg.name, pkg);
        }
    }

    private void savePackageList(boolean preferencesUpdated, Map<String,Package> map) {
        String setting = map == mDndPackages
                ? Settings.System.HEADS_UP_CUSTOM_VALUES
                : Settings.System.HEADS_UP_BLACKLIST_VALUES;

        List<String> settings = new ArrayList<String>();
        for (Package app : map.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            if (TextUtils.equals(setting, Settings.System.HEADS_UP_CUSTOM_VALUES)) {
                mDndPackageList = value;
            } else {
                mBlacklistPackageList = value;
            }
        }
        Settings.System.putString(getContentResolver(), setting, value);
    }

    private void updateEnabledState() {
        boolean enabled = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_NOTIFICATION, 0) != 0;
        mPrefsContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        mDisabledText.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Preference pref =
                (Preference) getPreferenceScreen().getRootAdapter().getItem(position);

        if ((mBlacklistPrefList.findPreference(pref.getKey()) != pref)
                && (mDndPrefList.findPreference(pref.getKey()) != pref)) {
            return false;
        }

        if (mAddDndPref == pref || mAddBlacklistPref == pref) {
            return false;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mBlacklistPrefList.findPreference(pref.getKey()) == pref) {
                            removeApplicationPref(pref.getKey(), mBlacklistPackages);
                        } else if (mDndPrefList.findPreference(pref.getKey()) == pref) {
                            removeApplicationPref(pref.getKey(), mDndPackages);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        builder.show();
        return true;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset_default_message)
                .setIcon(R.drawable.ic_settings_backup)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.reset);
        alertDialog.setMessage(R.string.qs_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.HEADS_UP_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
        mHeadsUpBgColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
        mHeadsUpBgColor.setSummary(R.string.trds_default_color);
        Settings.System.putInt(getContentResolver(),
                Settings.System.HEADS_UP_TEXT_COLOR, 0);
        mHeadsUpTextColor.setNewPreviewColor(DEFAULT_TEXT_COLOR);
        mHeadsUpTextColor.setSummary(R.string.trds_default_color);
    }
}
