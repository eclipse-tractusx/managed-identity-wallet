/*
 * *******************************************************************************
 *  Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ******************************************************************************
 */

package org.eclipse.tractusx.managedidentitywallets.revocation.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitSetManagerTest {

    @Test
    void initializeEncodedListString_ReturnsValidBase64() {
        String encoded = BitSetManager.initializeEncodedListString();
        assertNotNull(encoded);
        // Attempt to decode to verify it's valid Base64.
        assertDoesNotThrow(() -> BitSetManager.decodeFromString(encoded));
    }

    @Test
    void compress_And_Decompress_ReturnsOriginalBitSet() {
        BitSet originalBitSet = new BitSet(BitSetManager.BITSET_SIZE);
        originalBitSet.set(100);
        originalBitSet.set(10000);

        byte[] compressedBytes = BitSetManager.compress(originalBitSet);
        assertNotNull(compressedBytes);

        BitSet decompressedBitSet = BitSetManager.decompress(compressedBytes);
        assertNotNull(decompressedBitSet);
        assertEquals(originalBitSet, decompressedBitSet);
    }

    @Test
    void revokeCredential_SetsBitAndReturnsUpdatedEncodedList() throws Exception {
        String encodedList = BitSetManager.initializeEncodedListString();
        int indexToRevoke = 99;

        String updatedEncodedList = BitSetManager.revokeCredential(encodedList, indexToRevoke);
        assertNotNull(updatedEncodedList);

        BitSet updatedBitSet =
                BitSetManager.decompress(BitSetManager.decodeFromString(updatedEncodedList));
        assertTrue(updatedBitSet.get(indexToRevoke));
    }

    @Test
    @Disabled("should this even be checked here?")
    void revokeCredential_AlreadyRevoked_ThrowsException() {
        String encodedList = BitSetManager.initializeEncodedListString();
        int indexToRevoke = 99;

        // First time revoking should be fine
        assertDoesNotThrow(() -> BitSetManager.revokeCredential(encodedList, indexToRevoke));

        // Second time revoking should throw Exception
        assertThrows(Exception.class, () -> BitSetManager.revokeCredential(encodedList, indexToRevoke));
    }

    @Test
    void suspendCredential_FlipsBitAndReturnsUpdatedEncodedList() {
        String encodedList = BitSetManager.initializeEncodedListString();
        int indexToSuspend = 99;

        String updatedEncodedList = BitSetManager.suspendCredential(encodedList, indexToSuspend);
        assertNotNull(updatedEncodedList);

        BitSet updatedBitSet =
                BitSetManager.decompress(BitSetManager.decodeFromString(updatedEncodedList));
        assertTrue(updatedBitSet.get(indexToSuspend)); // Suspend should flip the bit to true
    }

    @Test
    void encodeToStringAndDecodeFromString_AreReversible() {
        byte[] originalData = new byte[]{ 1, 2, 3, 4, 5 };
        String encoded = BitSetManager.encodeToString(originalData);
        assertNotNull(encoded);

        byte[] decodedData = BitSetManager.decodeFromString(encoded);
        assertNotNull(decodedData);
        assertArrayEquals(originalData, decodedData);
    }
}
