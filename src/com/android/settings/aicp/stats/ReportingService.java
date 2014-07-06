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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.android.settings.R;

public class ReportingService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getBooleanExtra("firstBoot", false)) {
			promptUser();
			Log.d(Utilities.TAG, "Prompting user for opt-in.");
		} else {
			Log.d(Utilities.TAG, "User has opted in -- reporting.");
			Thread thread = new Thread() {
				@Override
				public void run() {
					report();
				}
			};
			thread.start();
		}
		return Service.START_REDELIVER_INTENT;
	}

	private void report() {
		Log.d(Utilities.TAG, "Reporting stats");

		String deviceId = Utilities.getUniqueID(getApplicationContext());
		String deviceName = Utilities.getDevice();
		String deviceVersion = Utilities.getModVersion();
		String deviceCountry = Utilities
				.getCountryCode(getApplicationContext());
		String deviceCarrier = Utilities.getCarrier(getApplicationContext());
		String deviceCarrierId = Utilities
				.getCarrierId(getApplicationContext());
		String RomName = Utilities.getRomName();
		String RomVersion = Utilities.getRomVersion();

		String RomStatsUrl = Utilities.getStatsUrl();

		if (RomStatsUrl == null || RomStatsUrl.isEmpty()) {
			Log.e(Utilities.TAG,
					"This ROM is not configured for ROM Statistics.");
			return;
		}

		Log.d(Utilities.TAG, "SERVICE: Report URL=" + RomStatsUrl);
		Log.d(Utilities.TAG, "SERVICE: Device ID=" + deviceId);
		Log.d(Utilities.TAG, "SERVICE: Device Name=" + deviceName);
		Log.d(Utilities.TAG, "SERVICE: Device Version=" + deviceVersion);
		Log.d(Utilities.TAG, "SERVICE: Country=" + deviceCountry);
		Log.d(Utilities.TAG, "SERVICE: Carrier=" + deviceCarrier);
		Log.d(Utilities.TAG, "SERVICE: Carrier ID=" + deviceCarrierId);
		Log.d(Utilities.TAG, "SERVICE: ROM Name=" + RomName);
		Log.d(Utilities.TAG, "SERVICE: ROM Version=" + RomVersion);

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(
				"http://stats.aicp-rom.com/submit.php");
		try {
			List<NameValuePair> kv = new ArrayList<NameValuePair>(5);
			kv.add(new BasicNameValuePair("device_hash", deviceId));
			kv.add(new BasicNameValuePair("device_name", deviceName));
			kv.add(new BasicNameValuePair("device_version", deviceVersion));
			kv.add(new BasicNameValuePair("device_country", deviceCountry));
			kv.add(new BasicNameValuePair("device_carrier", deviceCarrier));
			kv.add(new BasicNameValuePair("device_carrier_id", deviceCarrierId));
			kv.add(new BasicNameValuePair("rom_name", RomName));
			kv.add(new BasicNameValuePair("rom_version", RomVersion));
			httppost.setEntity(new UrlEncodedFormEntity(kv));
			HttpResponse resp = httpclient.execute(httppost);

			Log.d(Utilities.TAG, "Response is: "
					+ resp.getStatusLine().getStatusCode());
			getSharedPreferences(Utilities.SETTINGS_PREF_NAME, 0)
					.edit()
					.putLong(AnonymousStats.ANONYMOUS_LAST_CHECKED,
							System.currentTimeMillis()).apply();
		} catch (Exception e) {
			Log.e(Utilities.TAG, "Got Exception", e);
		}

		ReportingServiceManager.setAlarm(this);
		stopSelf();
	}

	@SuppressWarnings("deprecation")
	private void promptUser() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new Notification(R.drawable.icon,
				getString(R.string.notification_ticker),
				System.currentTimeMillis());
		Intent nI = new Intent(this, AnonymousStats.class);
		PendingIntent pI = PendingIntent.getActivity(getApplicationContext(),
				0, nI, 0);
		n.setLatestEventInfo(getApplicationContext(),
				getString(R.string.notification_title),
				getString(R.string.notification_desc), pI);
		nm.notify(1, n);
	}
}
