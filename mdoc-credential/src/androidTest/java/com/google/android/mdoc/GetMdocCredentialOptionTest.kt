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

import android.os.Build
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.android.mdoc.TestUtils.Companion.assertContains
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetMdocCredentialOptionTest {

    companion object {
        private val certificatePem: String = """
            -----BEGIN CERTIFICATE-----
            MIIHSjCCBjKgAwIBAgIQDB/LGEUYx+OGZ0EjbWtz8TANBgkqhkiG9w0BAQsFADBP
            MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMSkwJwYDVQQDEyBE
            aWdpQ2VydCBUTFMgUlNBIFNIQTI1NiAyMDIwIENBMTAeFw0yMzAxMTMwMDAwMDBa
            Fw0yNDAyMTMyMzU5NTlaMIGWMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZv
            cm5pYTEUMBIGA1UEBxMLTG9zIEFuZ2VsZXMxQjBABgNVBAoMOUludGVybmV0wqBD
            b3Jwb3JhdGlvbsKgZm9ywqBBc3NpZ25lZMKgTmFtZXPCoGFuZMKgTnVtYmVyczEY
            MBYGA1UEAxMPd3d3LmV4YW1wbGUub3JnMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A
            MIIBCgKCAQEAwoB3iVm4RW+6StkR+nutx1fQevu2+t0Fu6KBcbvhfyHSXy7w0nJO
            dTT4jWLjStpRkNQBPZwMwHH35i+21gdnJtDe/xfO8IX9McFmyodlBUcqX8CruIzD
            v9AXf2OjXPBG+4aq+03XKl5/muATl32++301Vw1dXoGYNeoWQqLTsHT3WS3tOOf+
            ehuzNuZ+rj+ephaD3lMBToEArrtC9R91KTTN6YSAOK48NxTA8CfOMFK5itxfIqB5
            +E9OSQTidXyqLyoeA+xxTKMqYfxvypEek1oueAhY9u67NCBdmuavxtfyvwp7+o6S
            d+NsewxAhmRKFexw13KOYzDhC+9aMJcuJQIDAQABo4ID2DCCA9QwHwYDVR0jBBgw
            FoAUt2ui6qiqhIx56rTaD5iyxZV2ufQwHQYDVR0OBBYEFLCTP+gXgv1ssrYXh8vj
            gP6CmwGeMIGBBgNVHREEejB4gg93d3cuZXhhbXBsZS5vcmeCC2V4YW1wbGUubmV0
            ggtleGFtcGxlLmVkdYILZXhhbXBsZS5jb22CC2V4YW1wbGUub3Jngg93d3cuZXhh
            bXBsZS5jb22CD3d3dy5leGFtcGxlLmVkdYIPd3d3LmV4YW1wbGUubmV0MA4GA1Ud
            DwEB/wQEAwIFoDAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgY8GA1Ud
            HwSBhzCBhDBAoD6gPIY6aHR0cDovL2NybDMuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0
            VExTUlNBU0hBMjU2MjAyMENBMS00LmNybDBAoD6gPIY6aHR0cDovL2NybDQuZGln
            aWNlcnQuY29tL0RpZ2lDZXJ0VExTUlNBU0hBMjU2MjAyMENBMS00LmNybDA+BgNV
            HSAENzA1MDMGBmeBDAECAjApMCcGCCsGAQUFBwIBFhtodHRwOi8vd3d3LmRpZ2lj
            ZXJ0LmNvbS9DUFMwfwYIKwYBBQUHAQEEczBxMCQGCCsGAQUFBzABhhhodHRwOi8v
            b2NzcC5kaWdpY2VydC5jb20wSQYIKwYBBQUHMAKGPWh0dHA6Ly9jYWNlcnRzLmRp
            Z2ljZXJ0LmNvbS9EaWdpQ2VydFRMU1JTQVNIQTI1NjIwMjBDQTEtMS5jcnQwCQYD
            VR0TBAIwADCCAX8GCisGAQQB1nkCBAIEggFvBIIBawFpAHYA7s3QZNXbGs7FXLed
            tM0TojKHRny87N7DUUhZRnEftZsAAAGFq0gFIwAABAMARzBFAiEAqt+fK6jFdGA6
            tv0EWt9rax0WYBV4re9jgZgq0zi42QUCIEBh1yKpPvgX1BreE0wBUmriOVUhJS77
            KgF193fT2877AHcAc9meiRtMlnigIH1HneayxhzQUV5xGSqMa4AQesF3crUAAAGF
            q0gFnwAABAMASDBGAiEA12SUFK5rgLqRzvgcr7ZzV4nl+Zt9lloAzRLfPc7vSPAC
            IQCXPbwScx1rE+BjFawZlVjLj/1PsM0KQQcsfHDZJUTLwAB2AEiw42vapkc0D+Vq
            AvqdMOscUgHLVt0sgdm7v6s52IRzAAABhatIBV4AAAQDAEcwRQIhAN5bhHthoyWM
            J3CQB/1iYFEhMgUVkFhHDM/nlE9ThCwhAiAPvPJXyp7a2kzwJX3P7fqH5Xko3rPh
            CzRoXYd6W+QkCjANBgkqhkiG9w0BAQsFAAOCAQEAWeRK2KmCuppK8WMMbXYmdbM8
            dL7F9z2nkZL4zwYtWBDt87jW/Gz/E5YyzU/phySFC3SiwvYP9afYfXaKrunJWCtu
            AG+5zSTuxELFTBaFnTRhOSO/xo6VyYSpsuVBD0R415W5z9l0v1hP5xb/fEAwxGxO
            Ik3Lg2c6k78rxcWcGvJDoSU7hPb3U26oha7eFHSRMAYN8gfUxAi6Q2TF4j/arMVB
            r6Q36EJ2dPcTu0p9NlmBm8dE34lzuTNC6GDCTWFdEloQ9u//M4kUUOjWn8a5XCs1
            263t3Ta2JfKViqxpP5r+GvgVKG3qGFrC0mIYr0B4tfpeCY9T+cz4I6GDMSP0xg==
            -----END CERTIFICATE-----
        """.trimIndent()
    }

    private val nonce: ByteArray
    private val publicKey: ECPublicKey
    private val criticalElements = setOf(
        MdocCredentialElement("given_name", MdocCredentialElement.NAMESPACE_MDL),
        MdocCredentialElement("family_name", MdocCredentialElement.NAMESPACE_MDL),
        MdocCredentialElement("driving_privileges", MdocCredentialElement.NAMESPACE_MDL),
    )
    private val requestedElements = criticalElements + setOf(
        MdocCredentialElement("issue_date", MdocCredentialElement.NAMESPACE_MDL),
        MdocCredentialElement("organ_donor", MdocCredentialElement.NAMESPACE_AAMVA)
    )
    private val clientCertificate: Certificate
    private val retentionInDays = 42

    init {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(
            ECGenParameterSpec("secp256r1"), SecureRandom()
        )
        val keyPair = kpg.generateKeyPair()
        publicKey = keyPair.public as ECPublicKey
        nonce = Random(0).nextBytes(4)

        val cf = CertificateFactory.getInstance("X.509")
        clientCertificate =
            cf.generateCertificate(ByteArrayInputStream(certificatePem.toByteArray()))
    }

    @Test
    fun constructor_success_onlyRequiredArguments() {
        val option = GetMdocCredentialOption(
            nonce = nonce,
            publicKey = publicKey,
            documentType = MdocCredential.DOCUMENT_TYPE_MDL,
        )
        assertThat(option.nonce).isSameInstanceAs(nonce)
        assertThat(option.publicKey).isSameInstanceAs(publicKey)
        assertThat(option.documentType).isEqualTo(MdocCredential.DOCUMENT_TYPE_MDL)
    }

    @Test
    fun constructor_success_allArguments() {
        val option = GetMdocCredentialOption(
            nonce = nonce,
            publicKey = publicKey,
            documentType = MdocCredential.DOCUMENT_TYPE_MDL,
            requestedElements = requestedElements,
            criticalElements = criticalElements,
            handover = MdocHandover.ANDROID,
            retentionInDays = retentionInDays,
            clientCertificate = clientCertificate
        )
        assertThat(option.nonce).isSameInstanceAs(nonce)
        assertThat(option.publicKey).isSameInstanceAs(publicKey)
        assertThat(option.documentType).isEqualTo(MdocCredential.DOCUMENT_TYPE_MDL)
        assertThat(option.criticalElements).isSameInstanceAs(criticalElements)
        assertThat(option.requestedElements).isSameInstanceAs(requestedElements)
        assertThat(option.handover).isEqualTo(MdocHandover.ANDROID)
        assertThat(option.retentionInDays).isEqualTo(retentionInDays)
        assertThat(option.clientCertificate).isSameInstanceAs(clientCertificate)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun getter_frameworkProperties_success() {
        val expectedCriticalElements = criticalElements.map {
            it.namespace + ":" + it.name
        }.toMutableSet()
        val expectedRequestElements = requestedElements.map {
            it.namespace + ":" + it.name
        }.toMutableSet()

        expectedCriticalElements.add(MdocCredential.DOCUMENT_TYPE_MDL)
        expectedRequestElements.add(MdocCredential.DOCUMENT_TYPE_MDL)

        val expectedCandidateData = Bundle()
        expectedCandidateData.putByteArray(GetMdocCredentialOption.BUNDLE_KEY_NONCE, nonce)
        expectedCandidateData.putSerializable(
            GetMdocCredentialOption.BUNDLE_KEY_PUBLIC_KEY, publicKey
        )
        expectedCandidateData.putStringArrayList(
            GetMdocCredentialOption.BUNDLE_KEY_SUPPORTED_ELEMENT_KEYS,
            expectedCriticalElements.toCollection(ArrayList())
        )
        expectedCandidateData.putString(
            GetMdocCredentialOption.BUNDLE_KEY_DOCUMENT_TYPE,
            MdocCredential.DOCUMENT_TYPE_MDL
        )
        expectedCandidateData.putString(
            GetMdocCredentialOption.BUNDLE_KEY_HANDOVER_TYPE, MdocHandover.ANDROID.name
        )
        expectedCandidateData.putInt(
            GetMdocCredentialOption.BUNDLE_KEY_RETENTION, retentionInDays
        )
        expectedCandidateData.putSerializable(
            GetMdocCredentialOption.BUNDLE_KEY_CLIENT_CERT, clientCertificate
        )
        expectedCandidateData.putStringArrayList(
            GetMdocCredentialOption.BUNDLE_KEY_SUPPORTED_ELEMENT_KEYS,
            expectedCriticalElements.toCollection(ArrayList())
        )

        val expectedRequestData = expectedCandidateData.deepCopy()
        expectedRequestData.putStringArrayList(
            GetMdocCredentialOption.BUNDLE_KEY_SUPPORTED_ELEMENT_KEYS,
            expectedRequestElements.toCollection(ArrayList())
        )

        val option = GetMdocCredentialOption(
            nonce = nonce,
            publicKey = publicKey,
            documentType = MdocCredential.DOCUMENT_TYPE_MDL,
            requestedElements = requestedElements,
            criticalElements = criticalElements,
            handover = MdocHandover.ANDROID,
            retentionInDays = retentionInDays,
            clientCertificate = clientCertificate
        )

        assertThat(option.type).isEqualTo(MdocCredential.TYPE_MDOC_CREDENTIAL)
        assertContains(expectedRequestData, option.requestData)
        assertContains(expectedCandidateData, option.candidateQueryData)
        assertThat(option.isSystemProviderRequired).isFalse()
        assertThat(option.isAutoSelectAllowed).isTrue()
    }

    @Test
    fun createFrom_success() {
        val expectedCriticalElements = criticalElements
        val expectedRequestedElements = requestedElements

        val option = GetMdocCredentialOption(
            nonce = nonce,
            publicKey = publicKey,
            documentType = MdocCredential.DOCUMENT_TYPE_MDL,
            requestedElements = requestedElements,
            criticalElements = criticalElements,
            handover = MdocHandover.ANDROID,
            retentionInDays = retentionInDays,
            clientCertificate = clientCertificate
        )

        val convertedOption = GetMdocCredentialOption.createFrom(option)
        assertThat(convertedOption.nonce).isEqualTo(option.nonce)
        assertThat(convertedOption.publicKey.encoded).isEqualTo(option.publicKey.encoded)
        assertThat(convertedOption.criticalElements).isEqualTo(expectedCriticalElements)
        assertThat(convertedOption.requestedElements).isEqualTo(expectedRequestedElements)
        assertThat(convertedOption.documentType).isEqualTo(option.documentType)
        assertThat(convertedOption.handover).isEqualTo(option.handover)
        assertThat(convertedOption.retentionInDays).isEqualTo(option.retentionInDays)
        assertThat(convertedOption.clientCertificate).isEqualTo(option.clientCertificate)
    }
}
