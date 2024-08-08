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

import org.eclipse.tractusx.managedidentitywallets.revocation.exception.BitSetManagerException;
import org.eclipse.tractusx.managedidentitywallets.revocation.exception.CredentialAlreadyRevokedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.BitSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BitSetManager {

    public static final int BITSET_SIZE = 131072;

    private BitSetManager() {
        // static methods only
    }

    public static String initializeEncodedListString() {
        BitSet bitSet = new BitSet(BITSET_SIZE);
        byte[] compressedBitSet = compress(bitSet);
        return encodeToString(compressedBitSet);
    }

    public static byte[] compress(BitSet bitSet) {
        byte[] byteArray = bitSet.toByteArray();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(byteArray);
            gzipOutputStream.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new BitSetManagerException(e);
        }
    }

    public static String revokeCredential(String encodedList, int index)
            throws BitSetManagerException {
        BitSet bitSet = decompress(decodeFromString(encodedList));
        if (bitSet.get(index)) {
            throw new CredentialAlreadyRevokedException("Credential already revoked");
        }
        bitSet.set(index);
        byte[] compressedBitSet = compress(bitSet);
        return encodeToString(compressedBitSet);
    }

    public static String suspendCredential(String encodedList, int index) {
        BitSet bitSet = decompress(decodeFromString(encodedList));
        bitSet.flip(index);
        byte[] compressedBitSet = compress(bitSet);
        return encodeToString(compressedBitSet);
    }

    public static BitSet decompress(byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int len;
            while ((len = gzipInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            BitSet bitSet = BitSet.valueOf(outputStream.toByteArray());
            outputStream.close();
            return bitSet;

        } catch (IOException e) {
            throw new BitSetManagerException(e);
        }
    }

    public static String encodeToString(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decodeFromString(String data) {
        return Base64.getDecoder().decode(data);
    }
}
