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

package com.android.settings.fuelgauge;

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.graph.BatteryMeterDrawableBase;

public class BatteryMeterView extends ImageView {
    @VisibleForTesting
    BatteryMeterDrawable mDrawable;
    @VisibleForTesting
    ColorFilter mErrorColorFilter;
    @VisibleForTesting
    ColorFilter mAccentColorFilter;

    private int mLevel;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final int frameColor = context.getColor(R.color.meter_background_color);
        mAccentColorFilter = new PorterDuffColorFilter(
                Utils.getColorAttr(context, android.R.attr.colorAccent), PorterDuff.Mode.SRC_IN);
        mErrorColorFilter = new PorterDuffColorFilter(
                context.getColor(R.color.battery_icon_color_error), PorterDuff.Mode.SRC_IN);

        //mDrawable = new BatteryMeterDrawable(context, frameColor);
        //mDrawable.setShowPercent(false);
        int userStyle = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.STATUS_BAR_BATTERY_STYLE,
                BatteryMeterDrawableBase.BATTERY_STYLE_PORTRAIT);
        mDrawable = new BatteryMeterDrawable(context, frameColor, userStyle);
        mDrawable.setBatteryColorFilter(mAccentColorFilter);
        if (userStyle == BatteryMeterDrawableBase.BATTERY_STYLE_PORTRAIT) {
            mDrawable.setWarningColorFilter(
                    new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        }
        setImageDrawable(mDrawable);
    }

    public void setBatteryLevel(int level) {
        mLevel = level;
        mDrawable.setBatteryLevel(level);
        if (level < mDrawable.getCriticalLevel()) {
            mDrawable.setBatteryColorFilter(mErrorColorFilter);
        } else {
            mDrawable.setBatteryColorFilter(mAccentColorFilter);
        }
    }

    public int getBatteryLevel() {
        return mLevel;
    }

    public void setCharging(boolean charging) {
        mDrawable.setCharging(charging);
        postInvalidate();
    }

    public boolean getCharging() {
        return mDrawable.getCharging();
    }

    public static class BatteryMeterDrawable extends BatteryMeterDrawableBase {
        private int mIntrinsicWidth;
        private int mIntrinsicHeight;

        public BatteryMeterDrawable(Context context, int frameColor, int style) {
            super(context, frameColor);

            /*mIntrinsicWidth = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_width);
            mIntrinsicHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_height);*/
            setMeterStyle(style);
            switch (style) {
                case BatteryMeterDrawableBase.BATTERY_STYLE_PORTRAIT:
                    mIntrinsicWidth = mContext.getResources().getDimensionPixelSize(
                                      R.dimen.battery_meter_width);
                    mIntrinsicHeight = mContext.getResources().getDimensionPixelSize(
                                      R.dimen.battery_meter_height);
                    setShowPercent(false);
                    break;
                case BatteryMeterDrawableBase.BATTERY_STYLE_CIRCLE:
                case BatteryMeterDrawableBase.BATTERY_STYLE_BIG_CIRCLE:
                    mIntrinsicWidth = mContext.getResources().getDimensionPixelSize(
                                      R.dimen.battery_meter_height);
                    mIntrinsicHeight = mContext.getResources().getDimensionPixelSize(
                                      R.dimen.battery_meter_height);
                    break;
                case BatteryMeterDrawableBase.BATTERY_STYLE_DOTTED_CIRCLE:
                case BatteryMeterDrawableBase.BATTERY_STYLE_BIG_DOTTED_CIRCLE:
                    mIntrinsicWidth = mContext.getResources().getDimensionPixelSize(
                                      R.dimen.battery_meter_height);
                    mIntrinsicHeight = mContext.getResources().getDimensionPixelSize(
                                      R.dimen.battery_meter_height);
                    setDashEffect(new float[]{18,10}, 0);
                    break;
            }
        }

        @Override
        public int getIntrinsicWidth() {
            return mIntrinsicWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mIntrinsicHeight;
        }

        public void setWarningColorFilter(@Nullable ColorFilter colorFilter) {
            mWarningTextPaint.setColorFilter(colorFilter);
        }

        public void setBatteryColorFilter(@Nullable ColorFilter colorFilter) {
            mFramePaint.setColorFilter(colorFilter);
            mBatteryPaint.setColorFilter(colorFilter);
            mBoltPaint.setColorFilter(colorFilter);
        }
    }

}
