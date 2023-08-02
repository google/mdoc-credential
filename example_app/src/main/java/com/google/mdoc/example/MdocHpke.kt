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

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.model.SimpleValue
import com.google.crypto.tink.*
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.proto.*
import com.google.crypto.tink.shaded.protobuf.ByteString
import com.google.crypto.tink.subtle.EllipticCurves
import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey


class MdocHpke(private val publicKey: PublicKey, private val privateKey: PrivateKey? = null) {

    private lateinit var publicKeysetHandle: KeysetHandle
    private var privateKeysetHandle: KeysetHandle? = null

    companion object {
        private const val ANDROID_HANDOVER_V1 = "AndroidHandoverv1"
        private const val BROWSER_HANDOVER_V1 = "BrowserHandoverv1"
        private const val PRIMARY_KEY_ID = 1
    }

    constructor(keyPair: KeyPair) : this(keyPair.public, keyPair.private)

    init {
        TinkConfig.register()
        HybridConfig.register()
        initializeKeysetHandles()
    }

    private fun initializeKeysetHandles() {
        val params = HpkeParams.newBuilder()
            .setAead(HpkeAead.AES_128_GCM)
            .setKdf(HpkeKdf.HKDF_SHA256)
            .setKem(HpkeKem.DHKEM_P256_HKDF_SHA256)
            .build()

        publicKey as ECPublicKey
        val encodedKey = EllipticCurves.pointEncode(
            EllipticCurves.CurveType.NIST_P256,
            EllipticCurves.PointFormatType.UNCOMPRESSED,
            publicKey.w
        )

        val hpkePublicKey = HpkePublicKey.newBuilder()
            .setVersion(0)
            .setPublicKey(ByteString.copyFrom(encodedKey))
            .setParams(params)
            .build()

        val publicKeyData = KeyData.newBuilder()
            .setKeyMaterialType(KeyData.KeyMaterialType.ASYMMETRIC_PUBLIC)
            .setTypeUrl("type.googleapis.com/google.crypto.tink.HpkePublicKey")
            .setValue(hpkePublicKey.toByteString())
            .build()

        val publicKeysetKey = Keyset.Key.newBuilder()
            .setKeyId(PRIMARY_KEY_ID)
            .setKeyData(publicKeyData)
            .setOutputPrefixType(OutputPrefixType.TINK)
            .setStatus(KeyStatusType.ENABLED)
            .build()

        val publicKeyset = Keyset.newBuilder()
            .setPrimaryKeyId(PRIMARY_KEY_ID)
            .addKey(publicKeysetKey)
            .build()

        publicKeysetHandle = TinkProtoKeysetFormat.parseKeyset(
            publicKeyset.toByteArray(),
            InsecureSecretKeyAccess.get()
        )

        if (privateKey != null) {
            privateKey as ECPrivateKey
            val hpkePrivateKey = HpkePrivateKey.newBuilder()
                .setPublicKey(hpkePublicKey)
                .setPrivateKey(ByteString.copyFrom(privateKey.s.toByteArray()))
                .build()

            val privateKeyData = KeyData.newBuilder()
                .setKeyMaterialType(KeyData.KeyMaterialType.ASYMMETRIC_PRIVATE)
                .setTypeUrl("type.googleapis.com/google.crypto.tink.HpkePrivateKey")
                .setValue(hpkePrivateKey.toByteString())
                .build()

            val privateKeysetKey = Keyset.Key.newBuilder()
                .setKeyId(PRIMARY_KEY_ID)
                .setKeyData(privateKeyData)
                .setOutputPrefixType(OutputPrefixType.TINK)
                .setStatus(KeyStatusType.ENABLED)
                .build()
            val privateKeyset = Keyset.newBuilder()
                .setPrimaryKeyId(PRIMARY_KEY_ID)
                .addKey(privateKeysetKey)
                .build()
            privateKeysetHandle = TinkProtoKeysetFormat.parseKeyset(
                privateKeyset.toByteArray(),
                InsecureSecretKeyAccess.get()
            )
        }
    }

    fun encrypt(plainText: ByteArray, aad: ByteArray): ByteArray {
        val encryptor = publicKeysetHandle.getPrimitive(HybridEncrypt::class.java)
        return encryptor.encrypt(plainText, aad)
    }

    fun decrypt(cipherText: ByteArray, aad: ByteArray): ByteArray {
        check(privateKeysetHandle != null)
        val decryptor = privateKeysetHandle!!.getPrimitive(HybridDecrypt::class.java)
        return decryptor.decrypt(cipherText, aad)
    }

    private fun generatePublicKeyHash(publicKey: PublicKey): ByteArray {
        publicKey as ECPublicKey
        val encodedKey = EllipticCurves.pointEncode(
            EllipticCurves.CurveType.NIST_P256,
            EllipticCurves.PointFormatType.UNCOMPRESSED,
            publicKey.w
        )

        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(encodedKey)
    }

//    SessionTranscript = [
//      null, // DeviceEngagementBytes not available
//      null, // EReaderKeyBytes not available
//      AndroidHandover // defined below
//    ]
//
//    AndroidHandover = [
//      "AndroidHandoverv1", // Version number
//      nonce, // nonce that comes from request
//      appId, // RP package name
//      pkRHash, // The SHA256 hash of the recipient public key.
//    ]
    fun generateAndroidSessionTranscript(
        nonce: ByteArray,
        publicKey: PublicKey,
        packageName: String
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        CborEncoder(baos).encode(
            CborBuilder()
                .startArray() // SessionTranscript
                .add(SimpleValue.NULL) // DeviceEngagementBytes
                .add(SimpleValue.NULL) // EReaderKeyBytes
                .startArray() // AndroidHandover
                .add(ANDROID_HANDOVER_V1)
                .add(nonce)
                .add(packageName.toByteArray())
                .add(generatePublicKeyHash(publicKey))
                .end()
                .end()
                .build()
        )
        return baos.toByteArray()
    }

//    SessionTranscript = [
//      null, // DeviceEngagementBytes not available
//      null, // EReaderKeyBytes not available
//      AndroidHandover // defined below
//    ]
//
//    From https://github.com/WICG/mobile-document-request-api
//
//    BrowserHandover = [
//      "BrowserHandoverv1",
//      nonce,
//      OriginInfoBytes, // origin of the request as defined in ISO/IEC 18013-7
//      RequesterIdentity,
//      pkRHash
//    ]
    fun generateBrowserSessionTranscript(nonce: ByteArray, origin: String, publicKey: PublicKey): ByteArray {
        val baos = ByteArrayOutputStream()
        CborEncoder(baos).encode(
            CborBuilder()
                .startArray() // SessionTranscript
                .add(SimpleValue.NULL) // DeviceEngagementBytes
                .add(SimpleValue.NULL) // EReaderKeyBytes
                .startArray() // BrowserHandover
                .add(BROWSER_HANDOVER_V1)
                .add(nonce)
                .add(origin.toByteArray())
                .startMap() // OriginInfo
                .put("cat", 1)
                .put("type", 1)
                .putMap("details")
                .put("baseUrl", origin)
                .end() // OriginInfo Details
                .end() // OriginInfo
                .add(generatePublicKeyHash(publicKey))
                .end()
                .end()
                .build()
        )
        return baos.toByteArray()
    }
}
