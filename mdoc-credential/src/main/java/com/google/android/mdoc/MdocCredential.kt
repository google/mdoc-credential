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
import androidx.annotation.VisibleForTesting
import androidx.credentials.CustomCredential
import java.security.interfaces.ECPublicKey

class MdocCredential constructor(
    val encryptedData: ByteArray,
    val encapsulatedKey: ECPublicKey
) : CustomCredential(TYPE_MDOC_CREDENTIAL, toBundle(encryptedData, encapsulatedKey)) {
    companion object {
        const val TYPE_MDOC_CREDENTIAL = "com.google.android.mdoc.TYPE_MDOC_CREDENTIAL"

        const val DOCUMENT_TYPE_MDL = "org.iso.18013.5.1.mDL"

        @VisibleForTesting
        internal const val BUNDLE_KEY_CREDENTIAL_DATA =
            "com.google.android.mdoc.BUNDLE_KEY_MDOC_CREDENTIAL_DATA"

        @VisibleForTesting
        internal const val BUNDLE_KEY_ENCAPSULATED_KEY =
            "com.google.android.mdoc.BUNDLE_KEY_ENCAPSULATED_KEY"

        private fun isMdocCredential(customCredential: CustomCredential): Boolean = customCredential.type == TYPE_MDOC_CREDENTIAL

        @JvmStatic
        @Suppress("Deprecated")
        fun createFrom(customCredential: CustomCredential): MdocCredential {
            check(isMdocCredential(customCredential)) { "Not a mdoc credential" }
            return MdocCredential(
                customCredential.data.getByteArray(BUNDLE_KEY_CREDENTIAL_DATA)!!,
                customCredential.data.getSerializable(BUNDLE_KEY_ENCAPSULATED_KEY) as ECPublicKey
            )
        }

        internal fun toBundle(encryptedData: ByteArray, encapsulatedKey: ECPublicKey): Bundle {
            val bundle = Bundle()
            bundle.putByteArray(BUNDLE_KEY_CREDENTIAL_DATA, encryptedData)
            bundle.putSerializable(BUNDLE_KEY_ENCAPSULATED_KEY, encapsulatedKey)
            return bundle
        }
    }
}
