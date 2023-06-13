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
import java.util.Arrays

class TestUtils {

    companion object {
        fun assertEquals(a: Bundle, b: Bundle) {
            if (a.keySet().size != b.keySet().size) {
                throw AssertionError("Expected ${a.keySet().size} elements, but have ${b.keySet().size}")
            }

            assertContains(a, b)
        }

        @Suppress("DEPRECATION")
        fun assertContains(a: Bundle, b: Bundle) {
            for (key in a.keySet()) {
                if (!b.keySet().contains(key)) {
                    throw AssertionError("Does not contain $key")
                }

                val valA = a.get(key)
                val valB = b.get(key)
                if (valA is Bundle && valB is Bundle) {
                    assertEquals(valA, valB)
                } else if (valA is ByteArray && valB is ByteArray) {
                    if (!Arrays.equals(valA, valB)) {
                        throw AssertionError("ByteArrays are not equal for key $key")
                    }
                } else {
                    val isEqual = (valA?.equals(valB) ?: (valB == null))
                    if (!isEqual) {
                        throw AssertionError("Values $a and $b are not equal for key $key")
                    }
                }
            }
        }
    }
}