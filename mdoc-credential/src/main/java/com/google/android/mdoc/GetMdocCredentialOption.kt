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
import androidx.annotation.RestrictTo
import androidx.credentials.GetCustomCredentialOption
import java.security.PublicKey
import java.security.cert.Certificate

class GetMdocCredentialOption @JvmOverloads constructor(
    val nonce: ByteArray,
    val publicKey: PublicKey,
    val documentType: String,
    val requestedElements: Set<MdocCredentialElement> = emptySet(),
    val criticalElements: Set<MdocCredentialElement> = emptySet(),
    val handover: MdocHandover = MdocHandover.ANDROID,
    val retentionInDays: Int = RETENTION_FOREVER,
    val clientCertificate: Certificate? = null,
) : GetCustomCredentialOption(
    type = MdocCredential.TYPE_MDOC_CREDENTIAL,
    requestData = toRequestDataBundle(
        nonce,
        publicKey,
        requestedElements,
        documentType,
        handover,
        retentionInDays,
        clientCertificate
    ),
    candidateQueryData = toRequestDataBundle(
        nonce,
        publicKey,
        criticalElements,
        documentType,
        handover,
        retentionInDays,
        clientCertificate
    ),
    isSystemProviderRequired = false,
    isAutoSelectAllowed = true,
    allowedProviders = emptySet()
) {
    companion object {
        const val RETENTION_FOREVER = -1
        const val RETENTION_NONE = 0

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val BUNDLE_KEY_SUPPORTED_ELEMENT_KEYS =
            "android.credentials.GetCredentialOption.SUPPORTED_ELEMENT_KEYS"

        internal const val BUNDLE_KEY_NONCE = "com.google.android.mdoc.BUNDLE_KEY_MDOC_NONCE"
        internal const val BUNDLE_KEY_PUBLIC_KEY =
            "com.google.android.mdoc.BUNDLE_KEY_MDOC_PUBLIC_KEY"
        internal const val BUNDLE_KEY_DOCUMENT_TYPE =
            "com.google.android.mdoc.BUNDLE_KEY_MDOC_DOC_TYPE"
        internal const val BUNDLE_KEY_HANDOVER_TYPE =
            "com.google.android.mdoc.BUNDLE_KEY_HANDOVER_TYPE"
        internal const val BUNDLE_KEY_RETENTION =
            "com.google.android.mdoc.BUNDLE_KEY_MDOC_RETENTION"
        internal const val BUNDLE_KEY_CLIENT_CERT =
            "com.google.android.mdoc.BUNDLE_KEY_MDOC_CLIENT_CERT"

        private const val ELEMENT_DELIMITER = ":"

        internal fun toRequestDataBundle(
            nonce: ByteArray,
            publicKey: PublicKey,
            elementKeys: Set<MdocCredentialElement>,
            documentType: String,
            handover: MdocHandover,
            retentionInDays: Int,
            clientCertificate: Certificate?
        ): Bundle {
            val bundle = Bundle()

            val elements = flattenElements(documentType, elementKeys)

            bundle.putByteArray(BUNDLE_KEY_NONCE, nonce)
            bundle.putSerializable(BUNDLE_KEY_PUBLIC_KEY, publicKey)

            bundle.putStringArrayList(
                BUNDLE_KEY_SUPPORTED_ELEMENT_KEYS, elements
            )

            bundle.putString(BUNDLE_KEY_DOCUMENT_TYPE, documentType)
            bundle.putString(BUNDLE_KEY_HANDOVER_TYPE, handover.name)
            bundle.putInt(BUNDLE_KEY_RETENTION, retentionInDays)
            bundle.putSerializable(BUNDLE_KEY_CLIENT_CERT, clientCertificate)

            return bundle
        }

        fun flattenElements(documentType: String, elements: Set<MdocCredentialElement>): ArrayList<String> {
            val list = elements.mapTo(ArrayList()) {
                it.namespace + ELEMENT_DELIMITER + it.name
            }

            list.add(documentType)
            return list
        }

        private fun unflattenElements(keys: List<String>): Set<MdocCredentialElement> {
            val result = HashSet<MdocCredentialElement>()
            keys.forEach {
                val splitElement = it.split(ELEMENT_DELIMITER)
                if (splitElement.size > 1) {
                    result.add(MdocCredentialElement(
                        name = splitElement[1],
                        namespace = splitElement[0]
                    ))
                }
            }

            return result
        }

        fun createFrom(customCredentialOption: GetCustomCredentialOption): GetMdocCredentialOption {
            check(customCredentialOption.type == MdocCredential.TYPE_MDOC_CREDENTIAL) {
                "Not an mdoc request"
            }

            return createFrom(customCredentialOption.requestData, customCredentialOption.candidateQueryData)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Suppress("Deprecation")
        fun createFrom(requestBundle: Bundle, candidateBundle: Bundle): GetMdocCredentialOption {
            val nonce = requestBundle.getByteArray(BUNDLE_KEY_NONCE)
            checkNotNull(nonce) { "nonce cannot be null" }

            val publicKey = requestBundle.getSerializable(BUNDLE_KEY_PUBLIC_KEY) as PublicKey?
            checkNotNull(publicKey) { "public key cannot be null" }

            val requestedElements = unflattenElements(
                checkNotNull(
                    requestBundle.getStringArrayList(BUNDLE_KEY_SUPPORTED_ELEMENT_KEYS)
                )
            )

            val criticalElements = unflattenElements(
                checkNotNull(
                    candidateBundle.getStringArrayList(BUNDLE_KEY_SUPPORTED_ELEMENT_KEYS)
                )
            )

            val documentType = requestBundle.getString(BUNDLE_KEY_DOCUMENT_TYPE)
            checkNotNull(documentType) { "documentType must not be null" }

            val handoverName = requestBundle.getString(BUNDLE_KEY_HANDOVER_TYPE)
            checkNotNull(handoverName) { "handoverName must not be null" }
            val handover = try {
                MdocHandover.valueOf(handoverName)
            } catch (e: IllegalArgumentException) {
                // We don't understand this value, default to MdocHandover.ANDROID
                MdocHandover.ANDROID
            }

            val retentionInDays = requestBundle.getInt(BUNDLE_KEY_RETENTION)

            val clientCertificate = requestBundle.getSerializable(
                BUNDLE_KEY_CLIENT_CERT
            ) as Certificate?

            return GetMdocCredentialOption(
                nonce = nonce,
                publicKey = publicKey,
                requestedElements = requestedElements,
                criticalElements = criticalElements,
                documentType = documentType,
                handover = handover,
                retentionInDays = retentionInDays,
                clientCertificate = clientCertificate
            )
        }
    }
}
