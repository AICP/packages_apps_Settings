/*
* Copyright (C) 2015 The Fusion Project
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

package com.android.settings.liquid;

import android.app.LauncherActivity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.android.settings.R;

// Liquid actions and utilities setup

public class CreateShortcut extends LauncherActivity {

    @Override
    protected Intent getTargetIntent() {
        Intent targetIntent = new Intent(Intent.ACTION_MAIN, null);
        targetIntent.addCategory("com.android.systemui.SHORTCUT");
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return targetIntent;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent shortcutIntent = intentForPosition(position);

        String intentClass = shortcutIntent.getComponent().getClassName();
        String intentAction = shortcutIntent.getAction();

        shortcutIntent = new Intent();
        shortcutIntent.setClassName(this, intentClass);
        shortcutIntent.setAction(intentAction);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
        Intent.ShortcutIconResource.fromContext(this, getProperShortcutIcon(intentClass)));
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, itemForPosition(position).label);
        setResult(RESULT_OK, intent);
        finish();
    }

    private int getProperShortcutIcon(String className) {
        String c = className.substring(className.lastIndexOf(".") + 1);

        if (c.equals ("FlushMemory")) {
            return R.drawable.liquid_flush_memory;

        } else if (c.equals ("RingVibToggle")) {
            return R.drawable.ic_lockscreen_vib;

        } else if (c.equals ("RingSilentToggle")) {
            return R.drawable.ic_lockscreen_silent;

        } else if (c.equals ("RingVibSilentToggle")) {
            return R.drawable.ic_lockscreen_soundon;

        } else if (c.equals ("StartDreamActivity")) {
            return R.drawable.ic_sysbar_dream;

        } else if (c.equals ("ProfileActivity")) {
            return R.drawable.ic_sysbar_profile;

        } else {
            return R.drawable.ic_sysbar_null;
        }
    }

    @Override
    protected boolean onEvaluateShowIcons() {
        return false;
    }
}

