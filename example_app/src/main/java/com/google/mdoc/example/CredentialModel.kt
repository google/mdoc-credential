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

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.mdoc.GetMdocCredentialOption
import com.google.android.mdoc.MdocCredential
import com.google.android.mdoc.MdocCredentialElement
import com.google.android.mdoc.MdocHandover
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

class CredentialModel(private val context: Context) {
    val values = mutableStateOf<Map<String, String>?>(null)

    private val keyPair = generateKeyPair()

    private fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(
            ECGenParameterSpec("secp256r1"), SecureRandom()
        )
        return kpg.generateKeyPair()
    }

    suspend fun requestCredential(
        criticalElements: Set<MdocCredentialElement>,
        requestedElements: Set<MdocCredentialElement>,
        origin: String? = null
    ) {
        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)

        val publicKey = keyPair.public
        val hpke = MdocHpke(publicKey, keyPair.private)
        val handover = if (origin != null) MdocHandover.BROWSER else MdocHandover.ANDROID

        val option = GetMdocCredentialOption(
            handover = handover,
            nonce = nonce,
            publicKey = publicKey,
            documentType = MdocCredential.DOCUMENT_TYPE_MDL,
            requestedElements = requestedElements,
            criticalElements = criticalElements,
            retentionInDays = GetMdocCredentialOption.RETENTION_NONE,
        )

        val request = GetCredentialRequest(listOf(option))
        val credman = CredentialManager.create(context)

        values.value = null

        val cred = credman.getCredential(context, request).credential
        cred as CustomCredential

        val handoverBytes = when(handover) {
            MdocHandover.ANDROID ->
                hpke.generateAndroidSessionTranscript(
                    nonce = nonce,
                    publicKey = publicKey,
                    packageName = context.opPackageName
                )
            MdocHandover.BROWSER ->
                hpke.generateBrowserSessionTranscript(
                    nonce = nonce,
                    publicKey = publicKey,
                    origin = origin!!
                )
        }

        val mdocCred = MdocCredential.createFrom(cred)
        val cborBytes =
            hpke.decrypt(mdocCred.encryptedData, mdocCred.encapsulatedKey, handoverBytes)
        values.value = CborUtils.extractIssuerNamespacedElements(cborBytes)
    }
}
