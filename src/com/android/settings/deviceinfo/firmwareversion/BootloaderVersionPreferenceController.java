package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;

public class BootloaderVersionPreferenceController extends BasePreferenceController {

    static final String BOOTLOADER_PROPERTY = "ro.bootloader";

    public BootloaderVersionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        CharSequence mBootloaderProp = getBootloaderProp();
        if (TextUtils.isEmpty(mBootloaderProp) || TextUtils.equals(mBootloaderProp, "unknown")) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return getBootloaderProp();
    }

    private CharSequence getBootloaderProp() {
        return SystemProperties.get(BOOTLOADER_PROPERTY);

    }
}
