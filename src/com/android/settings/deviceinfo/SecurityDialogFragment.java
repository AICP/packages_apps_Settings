/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.DeviceInfoUtils;

public class SecurityDialogFragment extends InstrumentedDialogFragment {

    public static final String TAG = "SecurityDialogTag";

    public static SecurityDialogFragment newInstance() {
        return new SecurityDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.SecurityDialog);

        @SuppressLint("InflateParams") final View content = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.security_dialog_layout, null /* parent */);

        TextView mSecurityPatch = content.findViewById(R.id.security_patch_level_text);
        mSecurityPatch.setText(DeviceInfoUtils.getSecurityPatch());

        TextView mVendorPatchText = content.findViewById(R.id.vendor_patch_level_text);
        TextView mVendorPatchSummary = content.findViewById(R.id.vendor_patch_level_summary);
        mVendorPatchSummary.setText(Build.VENDOR_PATCH_LEVEL.replace("_", " "));
        if (mVendorPatchSummary.getText().toString().equals("unknown")) {
            mVendorPatchText.setVisibility(View.GONE);
            mVendorPatchSummary.setVisibility(View.GONE);
        }

        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(getString(R.string.security_dialog_neutral_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.aosp_security_bulletin_url)));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return builder.setView(content).create();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIRTYTWEAKS;
    }
}
