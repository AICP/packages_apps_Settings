package com.android.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.vanir.AppMultiSelectListPreference;

import static android.hardware.Sensor.TYPE_PROXIMITY;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ActiveNotifications extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final String KEY_ENABLED = "ad_enable";
    private static final String KEY_LOCKSCREEN_NOTIFICATIONS = "lock_enable";
    private static final String KEY_POCKET_MODE = "pocket_mode";
    private static final String KEY_HIDE_LOW_PRIORITY = "hide_low_priority";
    private static final String KEY_HIDE_NON_CLEARABLE = "hide_non_clearable";
    private static final String KEY_DISMISS_ALL = "dismiss_all";
    private static final String KEY_PRIVACY_MODE = "privacy_mode";
    private static final String KEY_QUIET_HOURS = "quiet_hours";
    private static final String KEY_ADDITIONAL = "additional_options";
    private static final String KEY_EXCLUDED_APPS = "ad_excluded_apps";
    private static final String KEY_EXCLUDED_NOTIF_APPS = "excluded_apps";

    private Switch mEnabledSwitch;
    private Preference mAdditional;
    private boolean mActiveNotifications;

    private boolean mDialogClicked;
    private Dialog mEnableDialog;

    private CheckBoxPreference mEnabledPref;
    private CheckBoxPreference mLockNotif;
    private ListPreference mPocketModePref;
    private CheckBoxPreference mHideLowPriority;
    private CheckBoxPreference mHideNonClearable;
    private CheckBoxPreference mDismissAll;
    private AppMultiSelectListPreference mExcludedAppsPref;
    private AppMultiSelectListPreference mNotifAppsPref;
    private CheckBoxPreference mPrivacyMode;
    private CheckBoxPreference mQuietHours;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mEnabledSwitch = new Switch(activity);

        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
        mEnabledSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.END));
        mEnabledSwitch.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_NOTIFICATIONS, 0) == 1);
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.active_notifications);
        PreferenceScreen prefs = getPreferenceScreen();
        final ContentResolver cr = getActivity().getContentResolver();

        mEnabledPref = (CheckBoxPreference) prefs.findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(cr,
                Settings.System.ENABLE_ACTIVE_DISPLAY, 0) == 1));

        mLockNotif = (CheckBoxPreference) prefs.findPreference(KEY_LOCKSCREEN_NOTIFICATIONS);
        mLockNotif.setChecked((Settings.System.getInt(cr,
                Settings.System.LOCKSCREEN_NOTIFICATIONS, 0) == 1));

        mPocketModePref = (ListPreference) prefs.findPreference(KEY_POCKET_MODE);
        mPocketModePref.setOnPreferenceChangeListener(this);
        int mode = Settings.System.getInt(cr,
                Settings.System.ACTIVE_NOTIFICATIONS_POCKET_MODE, 0);
        mPocketModePref.setValue(String.valueOf(mode));
        updatePocketModeSummary(mode);
        if (!hasProximitySensor()) {
            getPreferenceScreen().removePreference(mPocketModePref);
        }

        mHideLowPriority = (CheckBoxPreference) prefs.findPreference(KEY_HIDE_LOW_PRIORITY);
        mHideLowPriority.setChecked(Settings.System.getInt(cr,
                    Settings.System.ACTIVE_NOTIFICATIONS_HIDE_LOW_PRIORITY, 0) == 1);

        mHideNonClearable = (CheckBoxPreference) prefs.findPreference(KEY_HIDE_NON_CLEARABLE);
        mHideNonClearable.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_NON_CLEARABLE, 0) == 1);

        mDismissAll = (CheckBoxPreference) prefs.findPreference(KEY_DISMISS_ALL);
        mDismissAll.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_DISMISS_ALL, 1) == 1);

        mPrivacyMode = (CheckBoxPreference) prefs.findPreference(KEY_PRIVACY_MODE);
        mPrivacyMode.setChecked(Settings.System.getInt(cr,
                    Settings.System.ACTIVE_NOTIFICATIONS_PRIVACY_MODE, 0) == 1);

        mQuietHours = (CheckBoxPreference) prefs.findPreference(KEY_QUIET_HOURS);
        mQuietHours.setChecked(Settings.System.getInt(cr,
                    Settings.System.ACTIVE_NOTIFICATIONS_QUIET_HOURS, 0) == 1);

        mExcludedAppsPref = (AppMultiSelectListPreference) findPreference(KEY_EXCLUDED_APPS);
        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) mExcludedAppsPref.setValues(excludedApps);
        mExcludedAppsPref.setOnPreferenceChangeListener(this);

        mNotifAppsPref = (AppMultiSelectListPreference) findPreference(KEY_EXCLUDED_NOTIF_APPS);
        Set<String> excludedNotifApps = getExcludedNotifApps();
        if (excludedNotifApps != null) mNotifAppsPref.setValues(excludedNotifApps);
        mNotifAppsPref.setOnPreferenceChangeListener(this);

        mAdditional = (PreferenceScreen) prefs.findPreference(KEY_ADDITIONAL);

        updateDependency();
    }

    private boolean isKeyguardSecure() {
        LockPatternUtils mLockPatternUtils = new LockPatternUtils(getActivity());
        boolean isSecure = mLockPatternUtils.isSecure();
        return isSecure;
    }

    private void updateDependency() {
        mActiveNotifications = Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_NOTIFICATIONS, 0) == 1;
        mPocketModePref.setEnabled(mActiveNotifications);
        mLockNotif.setEnabled(mActiveNotifications);
        mEnabledPref.setEnabled(mActiveNotifications);
        mHideLowPriority.setEnabled(mActiveNotifications);
        mHideNonClearable.setEnabled(mActiveNotifications);
        mDismissAll.setEnabled(!mHideNonClearable.isChecked() && mActiveNotifications);
        mQuietHours.setEnabled(mActiveNotifications);
        mPrivacyMode.setEnabled(mActiveNotifications);
        mAdditional.setEnabled(mActiveNotifications);
        mNotifAppsPref.setEnabled(mActiveNotifications);
        mExcludedAppsPref.setEnabled(mActiveNotifications);
    }
        
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mEnabledPref) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ENABLE_ACTIVE_DISPLAY, mEnabledPref.isChecked() ? 1 : 0);
        } else if (preference == mLockNotif) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS, mLockNotif.isChecked() ? 1 : 0);
        } else if (preference == mHideLowPriority) {
            Settings.System.putInt(cr, Settings.System.ACTIVE_NOTIFICATIONS_HIDE_LOW_PRIORITY,
                    mHideLowPriority.isChecked() ? 1 : 0);
        } else if (preference == mHideNonClearable) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_NON_CLEARABLE,
                    mHideNonClearable.isChecked() ? 1 : 0);
            mDismissAll.setEnabled(!mHideNonClearable.isChecked());
        } else if (preference == mDismissAll) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_DISMISS_ALL,
                    mDismissAll.isChecked() ? 1 : 0);
        } else if (preference == mPrivacyMode) {
            Settings.System.putInt(cr, Settings.System.ACTIVE_NOTIFICATIONS_PRIVACY_MODE,
                    mPrivacyMode.isChecked() ? 1 : 0);
        } else if (preference == mQuietHours) {
            Settings.System.putInt(cr, Settings.System.ACTIVE_NOTIFICATIONS_QUIET_HOURS,
                    mQuietHours.isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value) {
        if (pref == mPocketModePref) {
            int mode = Integer.valueOf((String) value);
            updatePocketModeSummary(mode);
            return true;
        } else if (pref == mExcludedAppsPref) {
            storeExcludedApps((Set<String>) value);
            return true;
        } else if (pref == mNotifAppsPref) {
			storeExcludedNotifApps((Set<String>) value);
			return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final boolean lesConditionnelles = isKeyguardSecure();
        if (buttonView == mEnabledSwitch) {
            if (isChecked && lesConditionnelles) {
                if (isChecked) {
                    mDialogClicked = false;
                    if (mEnableDialog != null) {
                        dismissDialogs();
                    }
                    mEnableDialog = new AlertDialog.Builder(getActivity()).setMessage(
                            getActivity().getResources().getString(
                                    R.string.lockscreen_notifications_dialog_message))
                            .setTitle(R.string.lockscreen_notifications_dialog_title)
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setPositiveButton(android.R.string.yes, this)
                            .show();
                    mEnableDialog.setOnDismissListener(this);
                }
            }
            boolean value = ((Boolean)isChecked).booleanValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_NOTIFICATIONS,
                    value ? 1 : 0);

            updateDependency();
        }
    }

    private void dismissDialogs() {
        if (mEnableDialog != null) {
            mEnableDialog.dismiss();
            mEnableDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mEnableDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mDialogClicked = true;
            }
        }
    }

    private void updatePocketModeSummary(int value) {
        mPocketModePref.setSummary(
                mPocketModePref.getEntries()[mPocketModePref.findIndexOfValue("" + value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ACTIVE_NOTIFICATIONS_POCKET_MODE, value);
    }

    private boolean hasProximitySensor() {
        SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        return sm.getDefaultSensor(TYPE_PROXIMITY) != null;
    }


    private Set<String> getExcludedApps() {
        String excluded = Settings.System.getString(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excluded))
            return null;

        return new HashSet<String>(Arrays.asList(excluded.split("\\|")));
    }

    private void storeExcludedApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS, builder.toString());
    }

    private Set<String> getExcludedNotifApps() {
        String excludedNotif = Settings.System.getString(getContentResolver(),
        Settings.System.LOCKSCREEN_NOTIFICATIONS_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excludedNotif)) return null;

        return new HashSet<String>(Arrays.asList(excludedNotif.split("\\|")));
    }

    private void storeExcludedNotifApps(Set<String> values) {
        StringBuilder Notifbuilder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
			Notifbuilder.append(delimiter);
			Notifbuilder.append(value);
			delimiter = "|";
        }
        Settings.System.putString(getContentResolver(),
			Settings.System.LOCKSCREEN_NOTIFICATIONS_EXCLUDED_APPS, Notifbuilder.toString());
    }

    public void onDismiss(DialogInterface dialog) {
        // ahh!
    }
}
