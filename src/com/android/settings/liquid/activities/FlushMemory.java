/*
 * Copyright 2015 The Fusion Project
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

package com.android.settings.liquid.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.util.CMDProcessor;
import com.android.internal.util.CMDProcessor.CommandResult;
import com.android.internal.util.Helpers;

public class FlushMemory extends Activity {

  private static final String TAG = "MemFlusher";

  public FlushMemory() {
    super();
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    Flush();

    this.finish();
  }

  public static void Flush() {
    // flush page cache, inodes, and dentries, finalize, and suggest a
    // garbage collection to the VM.  Useful to run before/after something
    // memory intensive like a game.  This will keep all your apps in memory
    // Log.v(TAG, "(1/3) Diddling drop_caches");

    //drop_caches 1 drops stuff that is dropped as part of drop_caches 3, so 3 makes 1 useless
    // CommandResult cr = new CMDProcessor().su.runWaitFor("echo 3 > /proc/sys/vm/drop_caches; sync");
    // if (!cr.success())
    //    Log.w(TAG, "Failed: echo 3 > /proc/sys/vm/drop_caches; sync\n\t"+cr.toString());

    Log.v(TAG, "(1/2) Running finalization");
    System.runFinalization();
    Log.v(TAG, "(2/2) Suggesting garbage collection to the VM");
    System.gc();
    Log.i(TAG, "Flush complete");
  }
}
