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

import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.model.DataItem
import co.nstant.`in`.cbor.model.MajorType
import java.io.ByteArrayInputStream
import kotlin.collections.Map
import kotlin.collections.set
import co.nstant.`in`.cbor.model.Array as CborArray
import co.nstant.`in`.cbor.model.ByteString as CborByteString
import co.nstant.`in`.cbor.model.Map as CborMap
import co.nstant.`in`.cbor.model.UnicodeString as CborUnicodeString

class CborUtils {

    companion object {

        fun extractIssuerNamespacedElements(cborBytes: ByteArray): Map<String, String> {
            val result = mutableMapOf<String, String>()

            val cbors = CborDecoder(ByteArrayInputStream(cborBytes)).decode()

            val elements =
                cbors[0]["documents"][0]["issuerSigned"]["nameSpaces"]["org.iso.18013.5.1"] as CborArray

            for (item in elements.dataItems) {
                val decoded =
                    CborDecoder(ByteArrayInputStream((item as CborByteString).bytes)).decode()

                val identifier = decoded[0]["elementIdentifier"].toString()
                val value = decoded[0]["elementValue"]

                if (value.majorType == MajorType.BYTE_STRING) {
                    result[identifier] = "<bytes>";
                } else {
                    result[identifier] = value.toString()
                }
            }

            return result
        }
    }
}

operator fun DataItem.get(name: String): DataItem {
    check(this.majorType == MajorType.MAP)
    this as CborMap
    return this.get(CborUnicodeString(name))
}

operator fun DataItem.get(index: Int): DataItem {
    check(this.majorType == MajorType.ARRAY)
    this as CborArray
    return this.dataItems[index]
}