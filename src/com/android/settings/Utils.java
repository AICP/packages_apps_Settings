/**
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.Dialog;
import android.app.Fragment;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.usb.IUsbManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.preference.Preference;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceGroup;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.provider.Settings;
import android.service.persistentdata.PersistentDataBlockManager;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.TabWidget;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;

import com.android.internal.util.UserIcons;
import com.android.settings.UserAdapter.UserDetails;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.dashboard.DashboardTile;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.drawable.CircleFramedDrawable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import static android.content.Intent.EXTRA_USER;

public final class Utils {
    private static final String TAG = "Settings";

    /**
     * Set the preference's title to the matching activity's label.
     */
    public static final int UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY = 1;

    /**
     * The opacity level of a disabled icon.
     */
    public static final float DISABLED_ALPHA = 0.4f;

    /**
     * Color spectrum to use to indicate badness.  0 is completely transparent (no data),
     * 1 is most bad (red), the last value is least bad (green).
     */
    public static final int[] BADNESS_COLORS = new int[] {
            0x00000000, 0xffc43828, 0xffe54918, 0xfff47b00,
            0xfffabf2c, 0xff679e37, 0xff0a7f42
    };

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the icon that should be displayed for the preference.
     */
    public static final String META_DATA_PREFERENCE_ICON = "com.android.settings.icon";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the title that should be displayed for the preference.
     */
    public static final String META_DATA_PREFERENCE_TITLE = "com.android.settings.title";

    /**
     * Name of the meta-data item that should be set in the AndroidManifest.xml
     * to specify the summary text that should be displayed for the preference.
     */
    public static final String META_DATA_PREFERENCE_SUMMARY = "com.android.settings.summary";

    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    public static final String OS_PKG = "os";

    private static final long GB_IN_BYTES = 1024 * 1024 * 1024;

    private static SparseArray<Bitmap> sDarkDefaultUserBitmapCache = new SparseArray<Bitmap>();

    /**
     * Finds a matching activity for a preference's intent. If a matching
     * activity is not found, it will remove the preference.
     *
     * @param context The context.
     * @param parentPreferenceGroup The preference group that contains the
     *            preference whose intent is being resolved.
     * @param preferenceKey The key of the preference whose intent is being
     *            resolved.
     * @param flags 0 or one or more of
     *            {@link #UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY}
     *            .
     * @return Whether an activity was found. If false, the preference was
     *         removed.
     */
    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {

        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {

                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));

                    if ((flags & UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY) != 0) {
                        // Set the preference title to the activity's label
                        preference.setTitle(resolveInfo.loadLabel(pm));
                    }

                    return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);

        return false;
    }

    /**
     * Finds a matching activity for a preference's intent. If a matching
     * activity is not found, it will remove the preference. The icon, title and
     * summary of the preference will also be updated with the values retrieved
     * from the activity's meta-data elements. If no meta-data elements are
     * specified then the preference title will be set to match the label of the
     * activity, an icon and summary text will not be displayed.
     *
     * @param context The context.
     * @param parentPreferenceGroup The preference group that contains the
     *            preference whose intent is being resolved.
     * @param preferenceKey The key of the preference whose intent is being
     *            resolved.
     *
     * @return Whether an activity was found. If false, the preference was
     *         removed.
     *
     * @see {@link #META_DATA_PREFERENCE_ICON}
     *      {@link #META_DATA_PREFERENCE_TITLE}
     *      {@link #META_DATA_PREFERENCE_SUMMARY}
     */
    public static boolean updatePreferenceToSpecificActivityFromMetaDataOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey) {

        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags
                        & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    Drawable icon = null;
                    String title = null;
                    String summary = null;

                    // Get the activity's meta-data
                    try {
                        Resources res = pm
                                .getResourcesForApplication(resolveInfo.activityInfo.packageName);
                        Bundle metaData = resolveInfo.activityInfo.metaData;

                        if (res != null && metaData != null) {
                            if (preference instanceof IconPreferenceScreen) {
                                icon = res.getDrawable(metaData.getInt(META_DATA_PREFERENCE_ICON));
                            }
                            title = res.getString(metaData.getInt(META_DATA_PREFERENCE_TITLE));
                            summary = res.getString(metaData.getInt(META_DATA_PREFERENCE_SUMMARY));
                        }
                    } catch (NameNotFoundException e) {
                        // Ignore
                    } catch (NotFoundException e) {
                        // Ignore
                    }

                    // Set the preference title to the activity's label if no
                    // meta-data is found
                    if (TextUtils.isEmpty(title)) {
                        title = resolveInfo.loadLabel(pm).toString();
                    }

                    // Set icon, title and summary for the preference
                    preference.setTitle(title);
                    preference.setSummary(summary);
                    if (preference instanceof IconPreferenceScreen) {
                        IconPreferenceScreen iconPreference = (IconPreferenceScreen) preference;
                        iconPreference.setIcon(icon);
                    }

                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));

                   return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);

        return false;
    }

    public static boolean updateTileToSpecificActivityFromMetaDataOrRemove(Context context,
            DashboardTile tile) {

        Intent intent = tile.intent;
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = tile.userHandle.size() != 0
                    ? pm.queryIntentActivitiesAsUser(intent, PackageManager.GET_META_DATA,
                            tile.userHandle.get(0).getIdentifier())
                    : pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {
                    int icon = 0;
                    CharSequence title = null;
                    String summary = null;

                    // Get the activity's meta-data
                    try {
                        Resources res = pm.getResourcesForApplication(
                                resolveInfo.activityInfo.packageName);
                        Bundle metaData = resolveInfo.activityInfo.metaData;

                        if (res != null && metaData != null) {
                            if (metaData.containsKey(META_DATA_PREFERENCE_ICON)) {
                                icon = metaData.getInt(META_DATA_PREFERENCE_ICON);
                            }
                            if (metaData.containsKey(META_DATA_PREFERENCE_TITLE)) {
                                title = res.getString(metaData.getInt(META_DATA_PREFERENCE_TITLE));
                            }
                            if (metaData.containsKey(META_DATA_PREFERENCE_SUMMARY)) {
                                summary = res.getString(
                                        metaData.getInt(META_DATA_PREFERENCE_SUMMARY));
                            }
                        }
                    } catch (NameNotFoundException | NotFoundException e) {
                        // Ignore
                    }

                    // Set the preference title to the activity's label if no
                    // meta-data is found
                    if (TextUtils.isEmpty(title)) {
                        title = resolveInfo.loadLabel(pm).toString();
                    }
                    if (icon == 0) {
                        icon = resolveInfo.activityInfo.icon;
                    }

                    // Set icon, title and summary for the preference
                    if (icon != 0) {
                        tile.iconRes = icon;
                        tile.iconPkg = resolveInfo.activityInfo.packageName;
                    }
                    tile.title = title;
                    tile.summary = summary;
                    // Replace the intent with this specific activity
                    tile.intent = new Intent().setClassName(resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns true if Monkey is running.
     */
    public static boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    /**
     * Returns the WIFI IP Addresses, if any, taking into account IPv4 and IPv6 style addresses.
     * @param context the application context
     * @return the formatted and newline-separated IP addresses, or null if none.
     */
    public static String getWifiIpAddresses(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties prop = cm.getLinkProperties(ConnectivityManager.TYPE_WIFI);
        return formatIpAddresses(prop);
    }

    /**
     * Returns the default link's IP addresses, if any, taking into account IPv4 and IPv6 style
     * addresses.
     * @param context the application context
     * @return the formatted and newline-separated IP addresses, or null if none.
     */
    public static String getDefaultIpAddresses(ConnectivityManager cm) {
        LinkProperties prop = cm.getActiveLinkProperties();
        return formatIpAddresses(prop);
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) return null;
        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        // If there are no entries, return null
        if (!iter.hasNext()) return null;
        // Concatenate all available addresses, comma separated
        String addresses = "";
        while (iter.hasNext()) {
            addresses += iter.next().getHostAddress();
            if (iter.hasNext()) addresses += "\n";
        }
        return addresses;
    }

    public static Locale createLocaleFromString(String localeStr) {
        // TODO: is there a better way to actually construct a locale that will match?
        // The main problem is, on top of Java specs, locale.toString() and
        // new Locale(locale.toString()).toString() do not return equal() strings in
        // many cases, because the constructor takes the only string as the language
        // code. So : new Locale("en", "US").toString() => "en_US"
        // And : new Locale("en_US").toString() => "en_us"
        if (null == localeStr)
            return Locale.getDefault();
        String[] brokenDownLocale = localeStr.split("_", 3);
        // split may not return a 0-length array.
        if (1 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0]);
        } else if (2 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1]);
        } else {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1], brokenDownLocale[2]);
        }
    }

    /** Formats the ratio of amount/total as a percentage. */
    public static String formatPercentage(long amount, long total) {
        return formatPercentage(((double) amount) / total);
    }

    /** Formats an integer from 0..100 as a percentage. */
    public static String formatPercentage(int percentage) {
        return formatPercentage(((double) percentage) / 100.0);
    }

    /** Formats a double from 0.0..1.0 as a percentage. */
    private static String formatPercentage(double percentage) {
      return NumberFormat.getPercentInstance().format(percentage);
    }

    public static boolean isBatteryPresent(Intent batteryChangedIntent) {
        return batteryChangedIntent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
    }

    public static String getBatteryPercentage(Intent batteryChangedIntent) {
        return formatPercentage(getBatteryLevel(batteryChangedIntent));
    }

    public static int getBatteryLevel(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return (level * 100) / scale;
    }

    public static boolean isDockBatteryPresent(Intent batteryChangedIntent) {
        return batteryChangedIntent.getBooleanExtra(BatteryManager.EXTRA_DOCK_PRESENT, true);
    }

    public static String getDockBatteryPercentage(Intent batteryChangedIntent) {
        return formatPercentage(getDockBatteryLevel(batteryChangedIntent));
    }

    public static int getDockBatteryLevel(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_DOCK_LEVEL, 0);
        int scale = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_DOCK_SCALE, 100);
        return (level * 100) / scale;
    }

    public static String getBatteryStatus(Resources res, Intent batteryChangedIntent) {
        final Intent intent = batteryChangedIntent;

        int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        String statusString;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            int resId;
            if (plugType == BatteryManager.BATTERY_PLUGGED_AC) {
                resId = R.string.battery_info_status_charging_ac;
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_USB) {
                resId = R.string.battery_info_status_charging_usb;
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                resId = R.string.battery_info_status_charging_wireless;
            } else {
                resId = R.string.battery_info_status_charging;
            }
            statusString = res.getString(resId);
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            statusString = res.getString(R.string.battery_info_status_discharging);
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            statusString = res.getString(R.string.battery_info_status_not_charging);
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            statusString = res.getString(R.string.battery_info_status_full);
        } else {
            statusString = res.getString(R.string.battery_info_status_unknown);
        }

        return statusString;
    }

    public static String getDockBatteryStatus(Resources res, Intent batteryChangedIntent) {
        final Intent intent = batteryChangedIntent;

        int plugType = intent.getIntExtra(BatteryManager.EXTRA_DOCK_PLUGGED, 0);
        int status = intent.getIntExtra(BatteryManager.EXTRA_DOCK_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        String statusString;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            int resId;
            if (plugType == BatteryManager.BATTERY_DOCK_PLUGGED_AC) {
                resId = R.string.battery_info_status_charging_dock_ac;
            } else if (plugType == BatteryManager.BATTERY_DOCK_PLUGGED_USB) {
                resId = R.string.battery_info_status_charging_dock_usb;
            } else {
                resId = R.string.battery_info_status_charging;
            }
            statusString = res.getString(resId);
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            statusString = res.getString(R.string.battery_info_status_discharging);
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            statusString = res.getString(R.string.battery_info_status_not_charging);
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            statusString = res.getString(R.string.battery_info_status_full);
        } else {
            statusString = res.getString(R.string.battery_info_status_unknown);
        }

        return statusString;
    }

    public static void forcePrepareCustomPreferencesList(
            ViewGroup parent, View child, ListView list, boolean ignoreSidePadding) {
        list.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        list.setClipToPadding(false);
        prepareCustomPreferencesList(parent, child, list, ignoreSidePadding);
    }

    /**
     * Prepare a custom preferences layout, moving padding to {@link ListView}
     * when outside scrollbars are requested. Usually used to display
     * {@link ListView} and {@link TabWidget} with correct padding.
     */
    public static void prepareCustomPreferencesList(
            ViewGroup parent, View child, View list, boolean ignoreSidePadding) {
        final boolean movePadding = list.getScrollBarStyle() == View.SCROLLBARS_OUTSIDE_OVERLAY;
        if (movePadding) {
            final Resources res = list.getResources();
            final int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);
            final int paddingBottom = res.getDimensionPixelSize(
                    com.android.internal.R.dimen.preference_fragment_padding_bottom);

            if (parent instanceof PreferenceFrameLayout) {
                ((PreferenceFrameLayout.LayoutParams) child.getLayoutParams()).removeBorders = true;

                final int effectivePaddingSide = ignoreSidePadding ? 0 : paddingSide;
                list.setPaddingRelative(effectivePaddingSide, 0, effectivePaddingSide, paddingBottom);
            } else {
                list.setPaddingRelative(paddingSide, 0, paddingSide, paddingBottom);
            }
        }
    }

    public static void forceCustomPadding(View view, boolean additive) {
        final Resources res = view.getResources();
        final int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);

        final int paddingStart = paddingSide + (additive ? view.getPaddingStart() : 0);
        final int paddingEnd = paddingSide + (additive ? view.getPaddingEnd() : 0);
        final int paddingBottom = res.getDimensionPixelSize(
                com.android.internal.R.dimen.preference_fragment_padding_bottom);

        view.setPaddingRelative(paddingStart, 0, paddingEnd, paddingBottom);
    }

    /* Used by UserSettings as well. Call this on a non-ui thread. */
    public static boolean copyMeProfilePhoto(Context context, UserInfo user) {
        Uri contactUri = Profile.CONTENT_URI;

        InputStream avatarDataStream = Contacts.openContactPhotoInputStream(
                    context.getContentResolver(),
                    contactUri, true);
        // If there's no profile photo, assign a default avatar
        if (avatarDataStream == null) {
            return false;
        }
        int userId = user != null ? user.id : UserHandle.myUserId();
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bitmap icon = BitmapFactory.decodeStream(avatarDataStream);
        um.setUserIcon(userId, icon);
        try {
            avatarDataStream.close();
        } catch (IOException ioe) { }
        return true;
    }

    public static String getMeProfileName(Context context, boolean full) {
        if (full) {
            return getProfileDisplayName(context);
        } else {
            return getShorterNameIfPossible(context);
        }
    }

    private static String getShorterNameIfPossible(Context context) {
        final String given = getLocalProfileGivenName(context);
        return !TextUtils.isEmpty(given) ? given : getProfileDisplayName(context);
    }

    private static String getLocalProfileGivenName(Context context) {
        final ContentResolver cr = context.getContentResolver();

        // Find the raw contact ID for the local ME profile raw contact.
        final long localRowProfileId;
        final Cursor localRawProfile = cr.query(
                Profile.CONTENT_RAW_CONTACTS_URI,
                new String[] {RawContacts._ID},
                RawContacts.ACCOUNT_TYPE + " IS NULL AND " +
                        RawContacts.ACCOUNT_NAME + " IS NULL",
                null, null);
        if (localRawProfile == null) return null;

        try {
            if (!localRawProfile.moveToFirst()) {
                return null;
            }
            localRowProfileId = localRawProfile.getLong(0);
        } finally {
            localRawProfile.close();
        }

        // Find the structured name for the raw contact.
        final Cursor structuredName = cr.query(
                Profile.CONTENT_URI.buildUpon().appendPath(Contacts.Data.CONTENT_DIRECTORY).build(),
                new String[] {CommonDataKinds.StructuredName.GIVEN_NAME,
                    CommonDataKinds.StructuredName.FAMILY_NAME},
                Data.RAW_CONTACT_ID + "=" + localRowProfileId,
                null, null);
        if (structuredName == null) return null;

        try {
            if (!structuredName.moveToFirst()) {
                return null;
            }
            String partialName = structuredName.getString(0);
            if (TextUtils.isEmpty(partialName)) {
                partialName = structuredName.getString(1);
            }
            return partialName;
        } finally {
            structuredName.close();
        }
    }

    private static final String getProfileDisplayName(Context context) {
        final ContentResolver cr = context.getContentResolver();
        final Cursor profile = cr.query(Profile.CONTENT_URI,
                new String[] {Profile.DISPLAY_NAME}, null, null, null);
        if (profile == null) return null;

        try {
            if (!profile.moveToFirst()) {
                return null;
            }
            return profile.getString(0);
        } finally {
            profile.close();
        }
    }

    /** Not global warming, it's global change warning. */
    public static Dialog buildGlobalChangeWarningDialog(final Context context, int titleResId,
            final Runnable positiveAction) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleResId);
        builder.setMessage(R.string.global_change_warning);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                positiveAction.run();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    public static boolean hasMultipleUsers(Context context) {
        return ((UserManager) context.getSystemService(Context.USER_SERVICE))
                .getUsers().size() > 1;
    }

    public static boolean isRestrictedProfile(Context context) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        return um.getUserInfo(um.getUserHandle()).isRestricted();
    }

    /* returns whether the device has volume rocker or not. */
    public static boolean hasVolumeRocker(Context context) {
        final int deviceKeys = context.getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        return (deviceKeys & ButtonSettings.KEY_MASK_VOLUME) != 0;
    }

    public static boolean isPackageInstalled(Context context, String pkg, boolean ignoreState) {
        if (pkg != null) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled && !ignoreState) {
                    return false;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        return isPackageInstalled(context, pkg, true);
    }

    /**
     * Start a new instance of the activity, showing only the given fragment.
     * When launched in this mode, the given preference fragment will be instantiated and fill the
     * entire activity.
     *
     * @param context The context.
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param resultTo Option fragment that should receive the result of the activity launch.
     * @param resultRequestCode If resultTo is non-null, this is the request code in which
     *                          to report the result.
     * @param titleResId resource id for the String to display for the title of this set
     *                   of preferences.
     * @param title String to display for the title of this set of preferences.
     */
    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, int titleResId,
            CharSequence title) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode,
                null /* titleResPackageName */, titleResId, title, false /* not a shortcut */);
    }

    /**
     * Start a new instance of the activity, showing only the given fragment.
     * When launched in this mode, the given preference fragment will be instantiated and fill the
     * entire activity.
     *
     * @param context The context.
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param resultTo Option fragment that should receive the result of the activity launch.
     * @param resultRequestCode If resultTo is non-null, this is the request code in which
     *                          to report the result.
     * @param titleResPackageName Optional package name for the resource id of the title.
     * @param titleResId resource id for the String to display for the title of this set
     *                   of preferences.
     * @param title String to display for the title of this set of preferences.
     */
    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, String titleResPackageName, int titleResId,
            CharSequence title) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode,
                titleResPackageName, titleResId, title, false /* not a shortcut */);
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, int titleResId,
            CharSequence title, boolean isShortcut) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args,
                null /* titleResPackageName */, titleResId, title, isShortcut);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, String titleResPackageName, int titleResId,
            CharSequence title, boolean isShortcut) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, titleResPackageName,
                titleResId, title, isShortcut);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public static void startWithFragmentAsUser(Context context, String fragmentName, Bundle args,
            int titleResId, CharSequence title, boolean isShortcut,
            UserHandle userHandle) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args,
                null /* titleResPackageName */, titleResId, title, isShortcut);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivityAsUser(intent, userHandle);
    }

    public static void startWithFragmentAsUser(Context context, String fragmentName, Bundle args,
            String titleResPackageName, int titleResId, CharSequence title, boolean isShortcut,
            UserHandle userHandle) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, titleResPackageName,
                titleResId, title, isShortcut);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivityAsUser(intent, userHandle);
    }

    /**
     * Build an Intent to launch a new activity showing the selected fragment.
     * The implementation constructs an Intent that re-launches the current activity with the
     * appropriate arguments to display the fragment.
     *
     *
     * @param context The Context.
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param titleResPackageName Optional package name for the resource id of the title.
     * @param titleResId Optional title resource id to show for this item.
     * @param title Optional title to show for this item.
     * @param isShortcut  tell if this is a Launcher Shortcut or not
     * @return Returns an Intent that can be launched to display the given
     * fragment.
     */
    public static Intent onBuildStartFragmentIntent(Context context, String fragmentName,
            Bundle args, String titleResPackageName, int titleResId, CharSequence title,
            boolean isShortcut) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        if (BluetoothSettings.class.getName().equals(fragmentName)) {
            intent.setClass(context, SubSettings.BluetoothSubSettings.class);
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
         } else if (SecuritySettings.class.getName().equals(fragmentName)) {
            intent.setClass(context, SubSettings.SecuritySubSettings.class);
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
         } else {
             intent.setClass(context, SubSettings.class);
         }
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME,
                titleResPackageName);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, titleResId);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE, title);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SHORTCUT, isShortcut);
        return intent;
    }

    /**
     * Returns the managed profile of the current user or null if none found.
     */
    public static UserHandle getManagedProfile(UserManager userManager) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        final int count = userProfiles.size();
        for (int i = 0; i < count; i++) {
            final UserHandle profile = userProfiles.get(i);
            if (profile.getIdentifier() == userManager.getUserHandle()) {
                continue;
            }
            final UserInfo userInfo = userManager.getUserInfo(profile.getIdentifier());
            if (userInfo.isManagedProfile()) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Returns true if the current profile is a managed one.
     */
    public static boolean isManagedProfile(UserManager userManager) {
        UserInfo currentUser = userManager.getUserInfo(userManager.getUserHandle());
        return currentUser.isManagedProfile();
    }

    /**
     * Creates a {@link UserAdapter} if there is more than one profile on the device.
     *
     * <p> The adapter can be used to populate a spinner that switches between the Settings
     * app on the different profiles.
     *
     * @return a {@link UserAdapter} or null if there is only one profile.
     */
    public static UserAdapter createUserSpinnerAdapter(UserManager userManager,
            Context context) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        if (userProfiles.size() < 2) {
            return null;
        }

        UserHandle myUserHandle = new UserHandle(UserHandle.myUserId());
        // The first option should be the current profile
        userProfiles.remove(myUserHandle);
        userProfiles.add(0, myUserHandle);

        return createUserAdapter(userManager, context, userProfiles);
    }

    public static UserAdapter createUserAdapter(UserManager userManager,
            Context context, List<UserHandle> userProfiles) {
        ArrayList<UserDetails> userDetails = new ArrayList<UserDetails>(userProfiles.size());
        final int count = userProfiles.size();
        for (int i = 0; i < count; i++) {
            userDetails.add(new UserDetails(userProfiles.get(i), userManager, context));
        }
        return new UserAdapter(context, userDetails);
    }

    /**
     * Returns the target user for a Settings activity.
     *
     * The target user can be either the current user, the user that launched this activity or
     * the user contained as an extra in the arguments or intent extras.
     *
     * Note: This is secure in the sense that it only returns a target user different to the current
     * one if the app launching this activity is the Settings app itself, running in the same user
     * or in one that is in the same profile group, or if the user id is provided by the system.
     */
    public static UserHandle getSecureTargetUser(IBinder activityToken,
           UserManager um, @Nullable Bundle arguments, @Nullable Bundle intentExtras) {
        UserHandle currentUser = new UserHandle(UserHandle.myUserId());
        IActivityManager am = ActivityManagerNative.getDefault();
        try {
            String launchedFromPackage = am.getLaunchedFromPackage(activityToken);
            boolean launchedFromSettingsApp = SETTINGS_PACKAGE_NAME.equals(launchedFromPackage);

            UserHandle launchedFromUser = new UserHandle(UserHandle.getUserId(
                    am.getLaunchedFromUid(activityToken)));
            if (launchedFromUser != null && !launchedFromUser.equals(currentUser)) {
                // Check it's secure
                if (isProfileOf(um, launchedFromUser)) {
                    return launchedFromUser;
                }
            }
            UserHandle extrasUser = intentExtras != null
                    ? (UserHandle) intentExtras.getParcelable(EXTRA_USER) : null;
            if (extrasUser != null && !extrasUser.equals(currentUser)) {
                // Check it's secure
                if (launchedFromSettingsApp && isProfileOf(um, extrasUser)) {
                    return extrasUser;
                }
            }
            UserHandle argumentsUser = arguments != null
                    ? (UserHandle) arguments.getParcelable(EXTRA_USER) : null;
            if (argumentsUser != null && !argumentsUser.equals(currentUser)) {
                // Check it's secure
                if (launchedFromSettingsApp && isProfileOf(um, argumentsUser)) {
                    return argumentsUser;
                }
            }
        } catch (RemoteException e) {
            // Should not happen
            Log.v(TAG, "Could not talk to activity manager.", e);
        }
        return currentUser;
   }

    /**
     * Returns the target user for a Settings activity.
     *
     * The target user can be either the current user, the user that launched this activity or
     * the user contained as an extra in the arguments or intent extras.
     *
     * You should use {@link #getSecureTargetUser(IBinder, UserManager, Bundle, Bundle)} if
     * possible.
     *
     * @see #getInsecureTargetUser(IBinder, Bundle, Bundle)
     */
   public static UserHandle getInsecureTargetUser(IBinder activityToken, @Nullable Bundle arguments,
           @Nullable Bundle intentExtras) {
       UserHandle currentUser = new UserHandle(UserHandle.myUserId());
       IActivityManager am = ActivityManagerNative.getDefault();
       try {
           UserHandle launchedFromUser = new UserHandle(UserHandle.getUserId(
                   am.getLaunchedFromUid(activityToken)));
           if (launchedFromUser != null && !launchedFromUser.equals(currentUser)) {
               return launchedFromUser;
           }
           UserHandle extrasUser = intentExtras != null
                   ? (UserHandle) intentExtras.getParcelable(EXTRA_USER) : null;
           if (extrasUser != null && !extrasUser.equals(currentUser)) {
               return extrasUser;
           }
           UserHandle argumentsUser = arguments != null
                   ? (UserHandle) arguments.getParcelable(EXTRA_USER) : null;
           if (argumentsUser != null && !argumentsUser.equals(currentUser)) {
               return argumentsUser;
           }
       } catch (RemoteException e) {
           // Should not happen
           Log.v(TAG, "Could not talk to activity manager.", e);
           return null;
       }
       return currentUser;
   }

   /**
    * Returns true if the user provided is in the same profiles group as the current user.
    */
   private static boolean isProfileOf(UserManager um, UserHandle otherUser) {
       if (um == null || otherUser == null) return false;
       return (UserHandle.myUserId() == otherUser.getIdentifier())
               || um.getUserProfiles().contains(otherUser);
   }


    /**
     * Returns whether or not this device is able to be OEM unlocked.
     */
    static boolean isOemUnlockEnabled(Context context) {
        PersistentDataBlockManager manager =(PersistentDataBlockManager)
                context.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
        return manager.getOemUnlockEnabled();
    }

    /**
     * Allows enabling or disabling OEM unlock on this device. OEM unlocked
     * devices allow users to flash other OSes to them.
     */
    static void setOemUnlockEnabled(Context context, boolean enabled) {
        PersistentDataBlockManager manager =(PersistentDataBlockManager)
                context.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
        manager.setOemUnlockEnabled(enabled);
    }

    /**
     * Return whether or not the user should have a SIM Cards option in Settings.
     * TODO: Change back to returning true if count is greater than one after testing.
     * TODO: See bug 16533525.
     */
    public static boolean showSimCardTile(Context context) {
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final boolean isPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        return isPrimary && tm.getSimCount() > 1;
    }

    /**
     * Determine whether a package is a "system package", in which case certain things (like
     * disabling notifications or disabling the package altogether) should be disallowed.
     */
    public static boolean isSystemPackage(PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{ getSystemSignature(pm) };
        }
        return sSystemSignature[0] != null && sSystemSignature[0].equals(getFirstSignature(pkg));
    }

    private static Signature[] sSystemSignature;

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg != null && pkg.signatures != null && pkg.signatures.length > 0) {
            return pkg.signatures[0];
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            final PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return getFirstSignature(sys);
        } catch (NameNotFoundException e) {
        }
        return null;
    }

    /**
     * Returns elapsed time for the given millis, in the following format:
     * 2d 5h 40m 29s
     * @param context the application context
     * @param millis the elapsed time in milli seconds
     * @param withSeconds include seconds?
     * @return the formatted elapsed time
     */
    public static String formatElapsedTime(Context context, double millis, boolean withSeconds) {
        StringBuilder sb = new StringBuilder();
        int seconds = (int) Math.floor(millis / 1000);
        if (!withSeconds) {
            // Round up.
            seconds += 30;
        }

        int days = 0, hours = 0, minutes = 0;
        if (seconds >= SECONDS_PER_DAY) {
            days = seconds / SECONDS_PER_DAY;
            seconds -= days * SECONDS_PER_DAY;
        }
        if (seconds >= SECONDS_PER_HOUR) {
            hours = seconds / SECONDS_PER_HOUR;
            seconds -= hours * SECONDS_PER_HOUR;
        }
        if (seconds >= SECONDS_PER_MINUTE) {
            minutes = seconds / SECONDS_PER_MINUTE;
            seconds -= minutes * SECONDS_PER_MINUTE;
        }
        if (withSeconds) {
            if (days > 0) {
                sb.append(context.getString(R.string.battery_history_days,
                        days, hours, minutes, seconds));
            } else if (hours > 0) {
                sb.append(context.getString(R.string.battery_history_hours,
                        hours, minutes, seconds));
            } else if (minutes > 0) {
                sb.append(context.getString(R.string.battery_history_minutes, minutes, seconds));
            } else {
                sb.append(context.getString(R.string.battery_history_seconds, seconds));
            }
        } else {
            if (days > 0) {
                sb.append(context.getString(R.string.battery_history_days_no_seconds,
                        days, hours, minutes));
            } else if (hours > 0) {
                sb.append(context.getString(R.string.battery_history_hours_no_seconds,
                        hours, minutes));
            } else {
                sb.append(context.getString(R.string.battery_history_minutes_no_seconds, minutes));
            }
        }
        return sb.toString();
    }

    /**
     * Queries for the UserInfo of a user. Returns null if the user doesn't exist (was removed).
     * @param userManager Instance of UserManager
     * @param checkUser The user to check the existence of.
     * @return UserInfo of the user or null for non-existent user.
     */
    public static UserInfo getExistingUser(UserManager userManager, UserHandle checkUser) {
        final List<UserInfo> users = userManager.getUsers(true /* excludeDying */);
        final int checkUserId = checkUser.getIdentifier();
        for (UserInfo user : users) {
            if (user.id == checkUserId) {
                return user;
            }
        }
        return null;
    }

    public static View inflateCategoryHeader(LayoutInflater inflater, ViewGroup parent) {
        final TypedArray a = inflater.getContext().obtainStyledAttributes(null,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.attr.preferenceCategoryStyle, 0);
        final int resId = a.getResourceId(com.android.internal.R.styleable.Preference_layout,
                0);
        a.recycle();
        return inflater.inflate(resId, parent, false);
    }

    /**
     * Return if we are running low on storage space or not.
     *
     * @param context The context
     * @return true if we are running low on storage space
     */
    public static boolean isLowStorage(Context context) {
        final StorageManager sm = StorageManager.from(context);
        return (sm.getStorageBytesUntilLow(context.getFilesDir()) < 0);
    }

    /**
     * Returns a default user icon (as a {@link Bitmap}) for the given user.
     *
     * Note that for guest users, you should pass in {@code UserHandle.USER_NULL}.
     * @param userId the user id or {@code UserHandle.USER_NULL} for a non-user specific icon
     */
    public static Bitmap getDefaultUserIconAsBitmap(int userId) {
        Bitmap bitmap = null;
        // Try finding the corresponding bitmap in the dark bitmap cache
        bitmap = sDarkDefaultUserBitmapCache.get(userId);
        if (bitmap == null) {
            bitmap = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(userId, false));
            // Save it to cache
            sDarkDefaultUserBitmapCache.put(userId, bitmap);
        }
        return bitmap;
    }

    public static boolean hasUsbDefaults(IUsbManager usbManager, String packageName) {
        try {
            if (usbManager != null) {
                return usbManager.hasDefaults(packageName, UserHandle.myUserId());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "mUsbManager.hasDefaults", e);
        }
        return false;
    }

    public static boolean hasPreferredActivities(PackageManager pm, String packageName) {
        // Get list of preferred activities
        List<ComponentName> prefActList = new ArrayList<>();
        // Intent list cannot be null. so pass empty list
        List<IntentFilter> intentList = new ArrayList<>();
        pm.getPreferredActivities(intentList, prefActList, packageName);
        Log.d(TAG, "Have " + prefActList.size() + " number of activities in preferred list");
        return prefActList.size() > 0;
    }

    public static ArraySet<String> getHandledDomains(PackageManager pm, String packageName) {
        List<IntentFilterVerificationInfo> iviList = pm.getIntentFilterVerifications(packageName);
        List<IntentFilter> filters = pm.getAllIntentFilters(packageName);

        ArraySet<String> result = new ArraySet<>();
        if (iviList.size() > 0) {
            for (IntentFilterVerificationInfo ivi : iviList) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
        }
        if (filters != null && filters.size() > 0) {
            for (IntentFilter filter : filters) {
                if (filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                        && (filter.hasDataScheme(IntentFilter.SCHEME_HTTP) ||
                                filter.hasDataScheme(IntentFilter.SCHEME_HTTPS))) {
                    result.addAll(filter.getHostsList());
                }
            }
        }
        return result;
    }

    public static CharSequence getLaunchByDeafaultSummary(ApplicationsState.AppEntry appEntry,
            IUsbManager usbManager, PackageManager pm, Context context) {
        String packageName = appEntry.info.packageName;
        boolean hasPreferred = hasPreferredActivities(pm, packageName)
                || hasUsbDefaults(usbManager, packageName);
        int status = pm.getIntentVerificationStatus(packageName, UserHandle.myUserId());
        // consider a visible current link-handling state to be any explicitly designated behavior
        boolean hasDomainURLsPreference =
                status != PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED;
        return context.getString(hasPreferred || hasDomainURLsPreference
                ? R.string.launch_defaults_some
                : R.string.launch_defaults_none);
    }

    public static void handleLoadingContainer(View loading, View doneLoading, boolean done,
            boolean animate) {
        setViewShown(loading, !done, animate);
        setViewShown(doneLoading, done, animate);
    }

    private static void setViewShown(final View view, boolean shown, boolean animate) {
        if (animate) {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(),
                    shown ? android.R.anim.fade_in : android.R.anim.fade_out);
            if (shown) {
                view.setVisibility(View.VISIBLE);
            } else {
                animation.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(View.INVISIBLE);
                    }
                });
            }
            view.startAnimation(animation);
        } else {
            view.clearAnimation();
            view.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * Returns the application info of the currently installed MDM package.
     */
    public static ApplicationInfo getAdminApplicationInfo(Context context, int profileId) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName mdmPackage = dpm.getProfileOwnerAsUser(profileId);
        if (mdmPackage == null) {
            return null;
        }
        String mdmPackageName = mdmPackage.getPackageName();
        try {
            IPackageManager ipm = AppGlobals.getPackageManager();
            ApplicationInfo mdmApplicationInfo =
                    ipm.getApplicationInfo(mdmPackageName, 0, profileId);
            return mdmApplicationInfo;
        } catch (RemoteException e) {
            Log.e(TAG, "Error while retrieving application info for package " + mdmPackageName
                    + ", userId " + profileId, e);
            return null;
        }
    }

    public static boolean isBandwidthControlEnabled() {
        final INetworkManagementService netManager = INetworkManagementService.Stub
                .asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        try {
            return netManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Returns an accessible SpannableString.
     * @param displayText the text to display
     * @param accessibileText the text text-to-speech engines should read
     */
    public static SpannableString createAccessibleSequence(CharSequence displayText,
            String accessibileText) {
        SpannableString str = new SpannableString(displayText);
        str.setSpan(new TtsSpan.TextBuilder(accessibileText).build(), 0,
                displayText.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return str;
    }

    public static int getEffectiveUserId(Context context) {
        UserManager um = UserManager.get(context);
        if (um != null) {
            return um.getCredentialOwnerProfile(UserHandle.myUserId());
        } else {
            Log.e(TAG, "Unable to acquire UserManager");
            return UserHandle.myUserId();
        }
    }

    public static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    public static String getServiceStateString(int state, Resources res) {
        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                return res.getString(R.string.radioInfo_service_in);
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                return res.getString(R.string.radioInfo_service_out);
            case ServiceState.STATE_POWER_OFF:
                return res.getString(R.string.radioInfo_service_off);
            default:
                return res.getString(R.string.radioInfo_unknown);
        }
    }

    /**
     * Locks the activity orientation to the current device orientation
     * @param activity
     */
    public static void lockCurrentOrientation(Activity activity) {
        int currentRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = activity.getResources().getConfiguration().orientation;
        int frozenRotation = 0;
        switch (currentRotation) {
            case Surface.ROTATION_0:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case Surface.ROTATION_90:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_180:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case Surface.ROTATION_270:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
        }
        activity.setRequestedOrientation(frozenRotation);
    }

    public static boolean isUserOwner() {
        return UserHandle.myUserId() == UserHandle.USER_OWNER;
    }

    public static boolean canUserMakeCallsSms(Context context) {
        UserManager userManager = UserManager.get(context);
        UserHandle userHandle = new UserHandle(UserHandle.myUserId());
        boolean callSmsNotAllowed = userManager.hasUserRestriction(
                userManager.DISALLOW_OUTGOING_CALLS, userHandle);
        callSmsNotAllowed &= userManager.hasUserRestriction(
                UserManager.DISALLOW_SMS, userHandle);
        return !callSmsNotAllowed;
    }

    public static String join(Resources res, List<String> items) {
        final int count = items.size();
        if (items.isEmpty()) {
            return null;
        } else if (count == 1) {
            return items.get(0);
        } else if (count == 2) {
            return res.getString(R.string.join_two_items, items.get(0), items.get(1));
        } else {
            String middle = items.get(count - 2);
            for (int i = count - 3; i > 0; i--) {
                middle = res.getString(R.string.join_many_items_middle,
                        items.get(i), middle);
            }
            final String allButLast = res.getString(R.string.join_many_items_first,
                    items.get(0), middle);
            return res.getString(R.string.join_many_items_last, allButLast,
                    items.get(count - 1));
        }
    }

    // Snackbar with action button
    public static void showSnackbar(final String message, Snackbar.SnackbarDuration duration,
            final String label, final Intent intent, final Context context) {
        Activity realActivity = ((Activity)context).getParent();
        if (realActivity == null) {
            realActivity = (Activity)context;
        }
        final Activity activity = realActivity;
        SnackbarManager.show(
            Snackbar.with(context)
                .type(SnackbarType.MULTI_LINE)
                .text(message)
                .color(context.getResources()
                        .getColor(R.color.theme_primary_dark))
                .actionLabel(label)
                .actionListener(new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        activity.startActivity(intent);
                    }
                })
                , activity);
    }

    public static boolean isAirplaneModeEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public static long getSystemTotalSpace() {
        File system = Environment.getRootDirectory();
        return system.getTotalSpace();
    }

    public static long estimateTotalSpace(Context context, long approximateTotalSpace) {
        int[] possibleSizeBases = context.getResources()
                .getIntArray(R.array.config_storageSizes);

        SortedSet<Long> possibleSizes = new TreeSet<Long>();
        for (int possibleSizeBase : possibleSizeBases) {
            possibleSizes.add(possibleSizeBase * GB_IN_BYTES);
        }

        long estimatedTotal = approximateTotalSpace;
        for (long possibleSize : possibleSizes) {
            if (possibleSize > approximateTotalSpace) {
                estimatedTotal = possibleSize;
                break;
            }
        }
        return estimatedTotal;
    }
}
