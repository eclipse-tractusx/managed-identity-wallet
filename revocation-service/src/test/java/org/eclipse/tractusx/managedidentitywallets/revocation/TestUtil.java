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

package org.eclipse.tractusx.managedidentitywallets.revocation;

import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.StatusListCredentialSubject;
import org.eclipse.tractusx.managedidentitywallets.revocation.jpa.StatusListCredential;
import org.eclipse.tractusx.managedidentitywallets.revocation.jpa.StatusListIndex;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.crypt.x25519.X25519Generator;
import org.eclipse.tractusx.ssi.lib.exception.json.TransformJsonLdException;
import org.eclipse.tractusx.ssi.lib.exception.key.InvalidPrivateKeyFormatException;
import org.eclipse.tractusx.ssi.lib.exception.key.KeyGenerationException;
import org.eclipse.tractusx.ssi.lib.exception.proof.SignatureGenerateFailedException;
import org.eclipse.tractusx.ssi.lib.exception.proof.UnsupportedSignatureTypeException;
import org.eclipse.tractusx.ssi.lib.model.proof.Proof;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;
import org.eclipse.tractusx.ssi.lib.proof.SignatureType;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.mockito.Mockito.when;

public class TestUtil {

    public static final int BITSET_SIZE = 131072;

    public static final String STATUS_LIST_ID = "https://example.com/credentials/status/3";

    public static final String BPN = "BPNL123456789000";

    public static final String DID = "did:web:example:BPNL123456789000";

    public static final List<URI> VC_CONTEXTS =
            List.of(URI.create("https://w3id.org/vc/status-list/2021/v1"));

    public static final String STATUS_LIST_CREDENTIAL_SUBJECT_ID =
            "https://example.com/status/3#list";

    public static StatusListIndex mockStatusListIndex(
            String issuerBpnStatus, StatusListCredential statusListCredential, String currentIndex) {
        var statusListIndex = Mockito.mock(StatusListIndex.class);
        when(statusListIndex.getStatusListCredential()).thenReturn(statusListCredential);
        when(statusListIndex.getCurrentIndex()).thenReturn(currentIndex);
        when(statusListIndex.getIssuerBpnStatus()).thenReturn(issuerBpnStatus);
        return statusListIndex;
    }

    public static String mockEmptyEncodedList() {
        BitSet bitSet = new BitSet(BITSET_SIZE);
        return Base64.getEncoder().encodeToString(gzipCompress(bitSet));
    }

    public static VerifiableCredentialBuilder mockStatusListVC(
            String issuer, String index, String encodedList) {
        var builder =
                new VerifiableCredentialBuilder()
                        .context(VC_CONTEXTS)
                        .id(URI.create(issuer + "#" + index))
                        .type(List.of("VerifiableCredential", "BitstringStatusListCredential"))
                        .issuer(URI.create(issuer))
                        .expirationDate(Instant.now().plusSeconds(200000000L))
                        .issuanceDate(Instant.now())
                        .credentialSubject(new VerifiableCredentialSubject(mockStatusList(encodedList)));
        return builder;
    }

    public static StatusListCredential mockStatusListCredential(
            String issuer, VerifiableCredentialBuilder builder) {

        final LinkedDataProofGenerator generator;
        try {
            generator = LinkedDataProofGenerator.newInstance(SignatureType.ED25519);
        } catch (UnsupportedSignatureTypeException e) {
            throw new AssertionError(e);
        }

        final Proof proof;
        try {
            proof =
                    generator.createProof(
                            builder.build(), URI.create(issuer + "#key-1"), generateKeys().getPrivateKey());
        } catch (InvalidPrivateKeyFormatException
                 | SignatureGenerateFailedException
                 | TransformJsonLdException e) {
            throw new AssertionError(e);
        }

        // Adding Proof to VC
        builder.proof(proof);

        StatusListCredential statusListCredential = Mockito.mock(StatusListCredential.class);
        when(statusListCredential.getCredential()).thenReturn(builder.build());
        when(statusListCredential.getId()).thenReturn(STATUS_LIST_ID);
        when(statusListCredential.getIssuerBpn()).thenReturn(issuer);
        when(statusListCredential.getCreatedAt()).thenReturn(LocalDateTime.now());

        return statusListCredential;
    }

    public static KeyPair generateKeys() {
        X25519Generator gen = new X25519Generator();
        KeyPair baseWalletKeys;
        try {
            baseWalletKeys = gen.generateKey();
        } catch (KeyGenerationException e) {
            throw new AssertionError(e);
        }
        return baseWalletKeys;
    }

    public static Map<String, Object> mockStatusList(String encodedList) {
        Map<String, Object> credentialSubjectMap = new HashMap<String, Object>();
        credentialSubjectMap.put(
                StatusListCredentialSubject.SUBJECT_ID, STATUS_LIST_CREDENTIAL_SUBJECT_ID);
        credentialSubjectMap.put(StatusListCredentialSubject.SUBJECT_TYPE, "BitstringStatusList");
        credentialSubjectMap.put(StatusListCredentialSubject.SUBJECT_STATUS_PURPOSE, "revocation");
        credentialSubjectMap.put(StatusListCredentialSubject.SUBJECT_ENCODED_LIST, encodedList);
        return credentialSubjectMap;
    }

    public static byte[] gzipCompress(BitSet bitSet) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(out)) {
            gzipOut.write(bitSet.toByteArray());
        } catch (IOException e) {
            throw new AssertionError("failed.");
        }

        return out.toByteArray();
    }

    public static BitSet decompressGzip(byte[] bytes) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try (GZIPInputStream gzipIn = new GZIPInputStream(in)) {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            BitSet bitSet = BitSet.valueOf(outputStream.toByteArray());
            outputStream.close();
            return bitSet;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String extractBpnFromDid(String did) {
        return did.substring(did.lastIndexOf(":") + 1).toUpperCase();
    }

    @SneakyThrows
    public static void main(String[] args) {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        // use explicit initialization as the platform default might fail
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();
        String s = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        System.out.println(s);
    }
}
