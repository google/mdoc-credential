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

import java.util.Objects

class MdocCredentialElement @JvmOverloads constructor(
    val name: String, val namespace: String? = null
) {
    companion object {
        const val NAMESPACE_MDL = "org.iso.18013.5.1"
        const val NAMESPACE_AAMVA = "org.iso.18013.5.1.aamva"
    }

    override fun hashCode(): Int {
        return Objects.hash(namespace, name)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MdocCredentialElement

        return other.namespace == namespace && other.name == name
    }

    override fun toString(): String {
        namespace?.let {
            return "$namespace:$name"
        }

        return name
    }
}