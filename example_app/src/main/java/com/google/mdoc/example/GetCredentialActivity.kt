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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetCustomCredentialOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.google.android.mdoc.GetMdocCredentialOption
import com.google.android.mdoc.MdocCredential
import com.google.android.mdoc.MdocHandover

class GetCredentialActivity : ComponentActivity() {

    private lateinit var request: ProviderGetCredentialRequest
    private lateinit var mdocOption: GetMdocCredentialOption

    companion object {
        private const val TAG = "GetCredentialActivity"
        private val ALLOWED_BROWSER_PACKAGES = listOf(
            "com.google.android.apps.chrome"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        request = PendingIntentHandler.retrieveProviderGetCredentialRequest(this.intent)!!
        mdocOption = GetMdocCredentialOption.createFrom(request.credentialOptions[0] as GetCustomCredentialOption)

        if (!checkBrowserRequest()) {
            respondWithError(GetCredentialUnknownException())
            return
        }

        setContent { GetCredential() }
    }

    private fun checkBrowserRequest(): Boolean {
        if (mdocOption.handover != MdocHandover.BROWSER) {
            // Not a browser request
            return true
        }

        val browserPackage = request.callingAppInfo.packageName
        if (!ALLOWED_BROWSER_PACKAGES.contains(browserPackage)) {
            Log.d(TAG, "Browser $browserPackage is not allowed to request credentials from this service")
            return false
        }

        // TODO: check package signatures against allowed values

        return true
    }

    fun respondWithError(exception: GetCredentialException) {
        val result = Intent()
        PendingIntentHandler.setGetCredentialException(result, exception)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    fun respondWithCredential() {
        val hpke = MdocHpke(mdocOption.publicKey)
        val sessionTranscriptBytes = when (mdocOption.handover) {
            MdocHandover.ANDROID -> hpke.generateAndroidSessionTranscript(
                nonce = mdocOption.nonce,
                publicKey = mdocOption.publicKey,
                packageName = request.callingAppInfo.packageName
            )
            MdocHandover.BROWSER -> hpke.generateBrowserSessionTranscript(
                nonce = mdocOption.nonce,
                publicKey = mdocOption.publicKey,
                origin = request.callingAppInfo.origin!!
            )
        }

        val (encryptedData, encapKey) = hpke.encrypt(TestVectors.ISO_18013_5_ANNEX_D_DEVICE_RESPONSE, sessionTranscriptBytes)
        val credential =
            MdocCredential(encryptedData, encapKey)
        val response = GetCredentialResponse(credential)

        val result = Intent()
        PendingIntentHandler.setGetCredentialResponse(result, response)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
fun GetCredential() {
    val activity = LocalContext.current as GetCredentialActivity
    ModalBottomSheet(
        onDismissRequest = {
            activity.respondWithError(GetCredentialCancellationException())
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { activity.respondWithCredential() }) {
                Text(text = "Share Test Credential")
            }
        }
    }
}