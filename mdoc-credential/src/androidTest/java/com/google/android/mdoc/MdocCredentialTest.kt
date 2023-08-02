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
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Arrays

@RunWith(AndroidJUnit4::class)
@SmallTest
class MdocCredentialTest {

    private val randomBytes = Random(0).nextBytes(256)
    private val encapsulatedKey: ECPublicKey

    init {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(
            ECGenParameterSpec("secp256r1"), SecureRandom()
        )
        encapsulatedKey = kpg.generateKeyPair().public as ECPublicKey
    }

    @Test
    fun constructor_success() {
        MdocCredential(randomBytes, encapsulatedKey)
    }

    @Test
    fun getter_mdocData_success() {
        val mdocCredential = MdocCredential(randomBytes, encapsulatedKey)
        assertThat(mdocCredential.encryptedData).isEqualTo(randomBytes)
        assertThat(mdocCredential.encapsulatedKey).isEqualTo(encapsulatedKey)
    }

    @Test
    fun getter_frameworkProperties() {
        val expectedEncryptedData = randomBytes
        val expectedEncapsulatedKey = encapsulatedKey
        val expectedData = Bundle()
        expectedData.putByteArray(
            MdocCredential.BUNDLE_KEY_CREDENTIAL_DATA, expectedEncryptedData
        )
        expectedData.putSerializable(
            MdocCredential.BUNDLE_KEY_ENCAPSULATED_KEY, expectedEncapsulatedKey
        )

        val mdocCredential = MdocCredential(expectedEncryptedData, encapsulatedKey)

        assertThat(mdocCredential.type).isEqualTo(
            MdocCredential.TYPE_MDOC_CREDENTIAL
        )
        assertEquals(mdocCredential.data, expectedData)
    }

    @Test
    fun createFrom_success() {
        val credential = MdocCredential(randomBytes, encapsulatedKey)

        val convertedCredential = MdocCredential.createFrom(credential)
        assertThat(convertedCredential.encryptedData)
            .isEqualTo(credential.encryptedData)
        assertThat(convertedCredential.encapsulatedKey)
            .isEqualTo(credential.encapsulatedKey)
    }

    @Test
    fun staticProperty_hasCorrectTypeConstantValue() {
        val typeExpected = "com.google.android.mdoc.TYPE_MDOC_CREDENTIAL"
        val typeActual = MdocCredential.TYPE_MDOC_CREDENTIAL
        assertThat(typeActual).isEqualTo(typeExpected)
    }
}