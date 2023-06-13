/*
 * Copyright 2023 The Android Open Source Project
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

package com.google.android.mdoc

import android.os.Bundle
import androidx.credentials.Credential
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.mdoc.TestUtils.Companion.assertEquals
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Arrays

@RunWith(AndroidJUnit4::class)
@SmallTest
class MdocCredentialTest {

    private val randomBytes = Random(0).nextBytes(256)

    @Test
    fun constructor_success() {
        MdocCredential(randomBytes)
    }

    @Test
    fun getter_mdocData_success() {
        val expectedEncryptedData = randomBytes
        val mdocCredential = MdocCredential(expectedEncryptedData)
        assertThat(mdocCredential.encryptedData).isEqualTo(expectedEncryptedData)
    }

    @Test
    fun getter_frameworkProperties() {
        val expectedEncryptedData = randomBytes
        val expectedData = Bundle()
        expectedData.putByteArray(
            MdocCredential.BUNDLE_KEY_CREDENTIAL_DATA, expectedEncryptedData
        )

        val mdocCredential = MdocCredential(expectedEncryptedData)

        assertThat(mdocCredential.type).isEqualTo(
            MdocCredential.TYPE_MDOC_CREDENTIAL
        )
        assertEquals(mdocCredential.data, expectedData)
    }

    @Test
    fun createFrom_success() {
        val credential = MdocCredential(randomBytes)

        val convertedCredential = MdocCredential.createFrom(credential)
        assertThat(convertedCredential.encryptedData)
            .isEqualTo(credential.encryptedData)
    }

    @Test
    fun staticProperty_hasCorrectTypeConstantValue() {
        val typeExpected = "com.google.android.mdoc.TYPE_MDOC_CREDENTIAL"
        val typeActual = MdocCredential.TYPE_MDOC_CREDENTIAL
        assertThat(typeActual).isEqualTo(typeExpected)
    }
}