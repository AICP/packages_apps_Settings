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

package com.android.settings.aicp.stats;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.settings.R;

public class AnonymousStats extends PreferenceActivity implements
		DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
		Preference.OnPreferenceChangeListener {
	private long lastPressedTime;

	private static final int PERIOD = 2000;

	private static final String VIEW_STATS = "pref_view_stats";
	private static final String PREF_UNINSTALL = "pref_uninstall_romstats";

	protected static final String ANONYMOUS_OPT_IN = "pref_anonymous_opt_in";
	protected static final String ANONYMOUS_FIRST_BOOT = "pref_anonymous_first_boot";
	protected static final String ANONYMOUS_LAST_CHECKED = "pref_anonymous_checked_in";
	protected static final String ANONYMOUS_ALARM_SET = "pref_anonymous_alarm_set";

	private CheckBoxPreference mEnableReporting;
	private Preference mViewStats;
	private Preference btnUninstall;
	private Dialog mOkDialog;
	private boolean mOkClicked;
	private SharedPreferences mPrefs;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getPreferenceManager() != null) {
			addPreferencesFromResource(R.xml.anonymous_stats);
			PreferenceScreen prefSet = getPreferenceScreen();

			mPrefs = this.getSharedPreferences(Utilities.SETTINGS_PREF_NAME, 0);
			mEnableReporting = (CheckBoxPreference) prefSet
					.findPreference(ANONYMOUS_OPT_IN);
			mViewStats = prefSet.findPreference(VIEW_STATS);
			btnUninstall = prefSet.findPreference(PREF_UNINSTALL);

			boolean firstBoot = mPrefs.getBoolean(ANONYMOUS_FIRST_BOOT, true);

			if (mEnableReporting.isChecked() && firstBoot) {
				mPrefs.edit().putBoolean(ANONYMOUS_FIRST_BOOT, false).apply();
				ReportingServiceManager.launchService(this);
			}

			try {
				PackageManager pm = getPackageManager();
				ApplicationInfo appInfo = pm.getApplicationInfo(
						getPackageName(), 0);

				// Log.d(Utilities.TAG, "App is installed in: " +
				// appInfo.sourceDir);
				// Log.d(Utilities.TAG, "App is system: " + (appInfo.flags &
				// ApplicationInfo.FLAG_SYSTEM));

				if ((appInfo.sourceDir.startsWith("/data/app/"))
						&& (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
					// it is a User app
					btnUninstall
							.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
								@Override
								public boolean onPreferenceClick(Preference pref) {
									uninstallSelf();
									return true;
								}
							});
				} else {
					prefSet.removePreference(btnUninstall);
				}

			} catch (Exception e) {
				prefSet.removePreference(btnUninstall);
			}

			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(1);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mEnableReporting) {
			if (mEnableReporting.isChecked()) {
				// Display the confirmation dialog
				mOkClicked = false;
				if (mOkDialog != null) {
					mOkDialog.dismiss();
					mOkDialog = null;
				}
				mOkDialog = new AlertDialog.Builder(this)
						.setMessage(
								this.getResources().getString(
										R.string.anonymous_statistics_warning))
						.setTitle(R.string.anonymous_statistics_warning_title)
						.setPositiveButton(android.R.string.yes, this)
						.setNeutralButton(
								getString(R.string.anonymous_learn_more), this)
						.setNegativeButton(android.R.string.no, this).show();
				mOkDialog.setOnDismissListener(this);
			} else {
				// Disable reporting
				mPrefs.edit().putBoolean(ANONYMOUS_OPT_IN, false).apply();
			}
		} else if (preference == mViewStats) {
			// Display the stats page
			Intent intent = new Intent (this, ViewStats.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			// What this does is finish our top application, so when and if the user
			// exits via the ViewStats.java class, it will exit the application, instead
			// of navigate back to this class. However, if the user selects to go back
			// via the ActionBar with the Up navigation I implemented, it will bring the user
			// back to this (Preference)Activity.
			finish();
		} else {
			// If we didn't handle it, let preferences handle it.
			return super.onPreferenceTreeClick(preferenceScreen, preference);
		}
		return true;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			switch (event.getAction()) {
			case KeyEvent.ACTION_DOWN:
				if (event.getDownTime() - lastPressedTime < PERIOD) {
					finish();
				} else {
					Toast.makeText(getApplicationContext(),
							"Press again to exit.", Toast.LENGTH_SHORT).show();
					lastPressedTime = event.getEventTime();
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.exit:
			super.finish();
			break;

		default:

		}
		;

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		if (!mOkClicked) {
			mEnableReporting.setChecked(false);
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			mOkClicked = true;
			mPrefs.edit().putBoolean(ANONYMOUS_OPT_IN, true).apply();
			ReportingServiceManager.launchService(this);
		} else if (which == DialogInterface.BUTTON_NEGATIVE) {
			mEnableReporting.setChecked(false);
		} else {
			Uri uri = Uri
					.parse("http://www.cyanogenmod.com/blog/cmstats-what-it-is-and-why-you-should-opt-in");
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
		}
	}

	public void uninstallSelf() {
		Intent intent = new Intent(Intent.ACTION_DELETE);
		intent.setData(Uri.parse("package:" + getPackageName()));
		startActivity(intent);
	}

}
