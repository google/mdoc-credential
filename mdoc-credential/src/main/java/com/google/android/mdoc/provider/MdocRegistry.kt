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

package com.google.android.mdoc.provider

import android.app.PendingIntent
import android.content.Context
import android.credentials.CredentialDescription
import android.credentials.CredentialManager
import android.credentials.RegisterCredentialDescriptionRequest
import android.credentials.UnregisterCredentialDescriptionRequest
import android.graphics.drawable.Icon
import android.service.credentials.CredentialEntry
import androidx.annotation.RequiresApi
import com.google.android.mdoc.GetMdocCredentialOption
import com.google.android.mdoc.MdocCredential
import com.google.android.mdoc.MdocCredentialElement

@RequiresApi(34)
class MdocRegistry(private val context: Context) {

    private val credman: CredentialManager = context.getSystemService(CredentialManager::class.java)

    private fun createCredentialEntry(
            title: String, icon: Icon, pendingIntent: PendingIntent,
    ): CredentialEntry {
        val slice = SliceHelper.createSlice(
                id = "id", // unused with registry
                type = MdocCredential.TYPE_MDOC_CREDENTIAL,
                title = title,
                pendingIntent = pendingIntent,
                icon = icon,
        )

        return CredentialEntry("id", MdocCredential.TYPE_MDOC_CREDENTIAL, slice)
    }

    private fun createDescription(
            documentType: String,
            supportedElements: Set<MdocCredentialElement>,
            entry: CredentialEntry? = null,
    ): CredentialDescription {
        val flattenedElements = GetMdocCredentialOption.flattenElements(documentType, supportedElements).toMutableSet()

        val entries = if (entry != null) listOf(entry) else emptyList()
        return CredentialDescription(MdocCredential.TYPE_MDOC_CREDENTIAL, flattenedElements, entries)
    }

    fun unregisterCredential(documentType: String, supportedElements: Set<MdocCredentialElement>) {
        credman.unregisterCredentialDescription(UnregisterCredentialDescriptionRequest(createDescription(documentType, supportedElements)))
    }

    fun registerCredential(
            title: String,
            icon: Icon,
            documentType: String,
            supportedElements: Set<MdocCredentialElement>,
            pendingIntent: PendingIntent,
    ) {
        // Work around credman issue where the CredentialEntry is not updated
        // for existing CredentialDescription
        unregisterCredential(documentType, supportedElements)

        val entry = createCredentialEntry(title, icon, pendingIntent)
        credman.registerCredentialDescription(RegisterCredentialDescriptionRequest(createDescription(documentType, supportedElements, entry)))
    }
}