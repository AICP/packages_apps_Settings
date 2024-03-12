/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.network.apn

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ApnRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mContentResolver = mock<ContentResolver> {}
    private val uri = mock<Uri> {}

    @Test
    fun getApnDataFromUri() {
        // mock out resources and the feature provider
        val cursor = MatrixCursor(sProjection)
        cursor.addRow(
            arrayOf<Any?>(
                0,
                "name",
                "apn",
                "proxy",
                "port",
                "userName",
                "server",
                "passWord",
                "mmsc",
                "mmsProxy",
                "mmsPort",
                0,
                "apnType",
                "apnProtocol",
                0,
                0,
                "apnRoaming",
                0,
                1,
                0
            )
        )
        val context = Mockito.spy(context)
        whenever(context.contentResolver).thenReturn(mContentResolver)
        whenever(mContentResolver.query(uri, sProjection, null, null, null)).thenReturn(cursor)
        assert(getApnDataFromUri(uri, context).name == "name")
    }
}