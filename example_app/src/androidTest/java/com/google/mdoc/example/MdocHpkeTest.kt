/*
 *  Copyright 2023 Google LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.google.mdoc.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

@RunWith(AndroidJUnit4::class)
@SmallTest
class MdocHpkeTest {

    private lateinit var keyPair: KeyPair

    @Before
    fun setup() {
        val nonce = ByteArray(12)

        val random = SecureRandom()
        random.nextBytes(nonce)

        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(
            ECGenParameterSpec("secp256r1"), random
        )
        keyPair = kpg.generateKeyPair()
    }

    @Test
    fun `test happy path for encryption and decryption`() {
        val plainText = "This is the plain text".toByteArray()
        val aad = "This is the associated data".toByteArray()

        val hpke = MdocHpke(keyPair)

        assertThat(hpke.decrypt(hpke.encrypt(plainText, aad), aad)).isEqualTo(plainText)
    }
}
