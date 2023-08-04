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

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.mdoc.MdocCredential
import com.google.android.mdoc.MdocCredentialElement
import com.google.android.mdoc.provider.MdocRegistry
import com.google.mdoc.example.ui.theme.MdoccredentialTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val LOGTAG = "MainActivity"
        val SUPPORTED_ELEMENTS = setOf(
            MdocCredentialElement("family_name", MdocCredentialElement.NAMESPACE_MDL),
            MdocCredentialElement("issue_date", MdocCredentialElement.NAMESPACE_MDL),
            MdocCredentialElement("expiry_date", MdocCredentialElement.NAMESPACE_MDL),
            MdocCredentialElement("document_number", MdocCredentialElement.NAMESPACE_MDL),
            MdocCredentialElement("driving_privileges", MdocCredentialElement.NAMESPACE_MDL),
            MdocCredentialElement("portrait", MdocCredentialElement.NAMESPACE_MDL),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerCredentials()
        setContent { Main() }
    }

    private fun registerCredentials() {
        val registry = MdocRegistry(this)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, GetCredentialActivity::class.java),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        try {
            registry.registerCredential(
                title = "Jane Doe's Driver's License",
                icon = Icon.createWithResource(this, android.R.drawable.ic_secure),
                documentType = MdocCredential.DOCUMENT_TYPE_MDL,
                supportedElements = SUPPORTED_ELEMENTS,
                pendingIntent = pendingIntent
            )
            Toast.makeText(this, "Successfully registered credential", Toast.LENGTH_SHORT).show()
        } catch (e: UnsupportedOperationException) {
            Log.e(LOGTAG, "Failed to register credential", e)
            Toast.makeText(this, "Failed to register credential, see logs", Toast.LENGTH_SHORT)
                .show()
        }
    }
}

@Composable
@Preview(showBackground = true)
fun Main() {
    val model = CredentialModel(LocalContext.current)
    MdoccredentialTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RequestCredButton(model)
                CredentialFields(model)
            }
        }
    }
}

@Composable
fun RequestCredButton(model: CredentialModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Request all of the elements that we have registered
    val elements = MainActivity.SUPPORTED_ELEMENTS

    Button(onClick = {
        scope.launch {
            try {
                model.requestCredential(elements)
            } catch (e: Exception) {
                Log.e(MainActivity.LOGTAG, "Failed to request credential", e)
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }) {
        Text(text = "Request Credential", fontSize = 24.sp)
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float
) {
    Text(
        text = text,
        Modifier
            .border(2.dp, Color.LightGray)
            .weight(weight)
            .padding(8.dp)
    )
}

@Composable
fun CredentialFields(model: CredentialModel) {
    if (model.values.value == null) {
        return
    }

    val entries = model.values.value!!.entries.toList()

    LazyColumn(
        Modifier.fillMaxSize(fraction = 0.85f),
        verticalArrangement = Arrangement.spacedBy((-2).dp)
    ) {
        items(entries) {
            val (id, text) = it
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((-2).dp)) {
                TableCell(text = id.toString(), weight = 0.5f)
                TableCell(text = text, weight = 0.5f)
            }
        }
    }

}