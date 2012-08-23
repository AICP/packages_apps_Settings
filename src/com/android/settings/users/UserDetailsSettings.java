/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.users;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.HashMap;
import java.util.List;

public class UserDetailsSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, DialogCreatable,
                   Preference.OnPreferenceClickListener {

    private static final String TAG = "UserDetailsSettings";

    private static final int MENU_REMOVE_USER = Menu.FIRST;
    private static final int DIALOG_CONFIRM_REMOVE = 1;

    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PICTURE = "user_picture";

    public static final String EXTRA_USER_ID = "user_id";

    private static final int RESULT_PICK_IMAGE = 1;
    private static final int RESULT_CROP_IMAGE = 2;

    private EditTextPreference mNamePref;
    private Preference mPicturePref;

    private IPackageManager mIPm;
    private PackageManager mPm;
    private UserManager mUm;
    private int mUserId;
    private boolean mNewUser;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.user_details);
        Bundle args = getArguments();
        mNewUser = args == null || args.getInt(EXTRA_USER_ID, -1) == -1;
        mUserId = mNewUser ? -1 : args.getInt(EXTRA_USER_ID, -1);
        mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mUm = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);

        if (mUserId == -1) {
            mUserId = mUm.createUser(getString(R.string.user_new_user_name), 0).id;
        }
        mNamePref = (EditTextPreference) findPreference(KEY_USER_NAME);
        mNamePref.setOnPreferenceChangeListener(this);
        mPicturePref = findPreference(KEY_USER_PICTURE);
        mPicturePref.setOnPreferenceClickListener(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPm = getActivity().getPackageManager();
        if (mUserId >= 0) {
            initExistingUser();
        } else {
            initNewUser();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mUserId == 0) {
            return;
        }
        MenuItem addAccountItem = menu.add(0, MENU_REMOVE_USER, 0,
                mNewUser ? R.string.user_discard_user_menu : R.string.user_remove_user_menu);
        addAccountItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_REMOVE_USER) {
            onRemoveUserClicked();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void initExistingUser() {
        List<UserInfo> users = mUm.getUsers();
        UserInfo foundUser = null;
        for (UserInfo user : users) {
            if (user.id == mUserId) {
                foundUser = user;
                break;
            }
        }
        if (foundUser != null) {
            mNamePref.setSummary(foundUser.name);
            mNamePref.setText(foundUser.name);
            if (foundUser.iconPath != null) {
                setPhotoId(foundUser.iconPath);
            }
        }
    }

    private void initNewUser() {
        // TODO: Check if there's already a "New user" and localize
        mNamePref.setText(getString(R.string.user_new_user_name));
        mNamePref.setSummary(getString(R.string.user_new_user_name));
    }

    private void onRemoveUserClicked() {
        if (mNewUser) {
            removeUserNow();
        } else {
            showDialog(DIALOG_CONFIRM_REMOVE);
        }
    }

    private void removeUserNow() {
        mUm.removeUser(mUserId);
        finish();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof CheckBoxPreference) {
            String packageName = preference.getKey();
            int newState = ((Boolean) newValue) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
            try {
                mIPm.setApplicationEnabledSetting(packageName, newState, 0, mUserId);
            } catch (RemoteException re) {
                Log.e(TAG, "Unable to change enabled state of package " + packageName
                        + " for user " + mUserId);
            }
        } else if (preference == mNamePref) {
            String name = (String) newValue;
            if (TextUtils.isEmpty(name)) {
                return false;
            }
            mUm.setUserName(mUserId, (String) newValue);
            mNamePref.setSummary((String) newValue);
        }
        return true;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE:
                return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.user_confirm_remove_title)
                    .setMessage(R.string.user_confirm_remove_message)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeUserNow();
                            }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            default:
                return null;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mPicturePref) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(intent, RESULT_PICK_IMAGE);
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
        case RESULT_PICK_IMAGE:
            if (data.getData() != null) {
                Uri imageUri = data.getData();
                System.err.println("imageUri = " + imageUri);
                cropImage(imageUri);
            }
            break;
        case RESULT_CROP_IMAGE:
            saveCroppedImage(data);
            break;
        }
    }

    private void cropImage(Uri imageUri) {
        final Uri inputPhotoUri = imageUri;
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(inputPhotoUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 96);
        intent.putExtra("outputY", 96);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, RESULT_CROP_IMAGE);
    }

    private void saveCroppedImage(Intent data) {
        if (data.hasExtra("data")) {
            Bitmap bitmap = (Bitmap) data.getParcelableExtra("data");
            ParcelFileDescriptor fd = mUm.setUserIcon(mUserId);
            if (fd != null) {
                bitmap.compress(CompressFormat.PNG, 100,
                        new ParcelFileDescriptor.AutoCloseOutputStream(fd));
                setPhotoId(mUm.getUserInfo(mUserId).iconPath);
            }
        }
    }

    private void setPhotoId(String realPath) {
        Drawable d = Drawable.createFromPath(realPath);
        if (d == null) return;
        mPicturePref.setIcon(d);
    }
}
