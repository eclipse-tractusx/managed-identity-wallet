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
package org.eclipse.tractusx.managedidentitywallets.utils;

import com.nimbusds.jose.util.JSONObjectUtils;
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.IssuersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.ssi.lib.crypt.IPublicKey;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.crypt.x25519.X25519Generator;
import org.eclipse.tractusx.ssi.lib.exception.key.InvalidPrivateKeyFormatException;
import org.eclipse.tractusx.ssi.lib.exception.key.KeyGenerationException;
import org.eclipse.tractusx.ssi.lib.exception.proof.UnsupportedSignatureTypeException;
import org.eclipse.tractusx.ssi.lib.model.MultibaseString;
import org.eclipse.tractusx.ssi.lib.model.base.MultibaseFactory;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocumentBuilder;
import org.eclipse.tractusx.ssi.lib.model.did.DidMethod;
import org.eclipse.tractusx.ssi.lib.model.did.DidMethodIdentifier;
import org.eclipse.tractusx.ssi.lib.model.did.Ed25519VerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.did.Ed25519VerificationMethodBuilder;
import org.eclipse.tractusx.ssi.lib.model.did.VerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.proof.Proof;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;
import org.eclipse.tractusx.ssi.lib.proof.SignatureType;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockUtil {

    public static VerifiableCredential mockCredential(
            List<String> types,
            List<VerifiableCredentialSubject> credentialSubjects,
            KeyPair keyPair,
            String host,
            Instant expirationDate
    ) {
        return mockCredential(types, credentialSubjects, keyPair, host, expirationDate, false);
    }

    @SneakyThrows
    public static VerifiableCredential mockCredential(
            List<String> types,
            List<VerifiableCredentialSubject> credentialSubjects,
            KeyPair keyPair,
            String host,
            Instant expirationDate,
            boolean jws
    ) {
        Did issuer = new Did(new DidMethod("web"), new DidMethodIdentifier(host), null);
        VerifiableCredentialBuilder builder = MockUtil.getCredentialBuilder(
                types,
                credentialSubjects,
                expirationDate,
                issuer
        );

        // Ed25519 Proof Builder
        LinkedDataProofGenerator generator;
        try {
            generator = LinkedDataProofGenerator.newInstance(jws ? SignatureType.JWS : SignatureType.ED25519);
        } catch (UnsupportedSignatureTypeException e) {
            throw new AssertionError(e);
        }

        Proof proof;
        try {
            proof =
                    generator.createProof(builder.build(), URI.create(issuer + "#key-1"), keyPair.getPrivateKey());
        } catch (InvalidPrivateKeyFormatException e) {
            throw new AssertionError(e);
        }

        // Adding Proof to VC
        builder.proof(proof);

        return builder.build();
    }

    public static VerifiableCredentialBuilder getCredentialBuilder(
            List<String> types,
            List<VerifiableCredentialSubject> credentialSubjects,
            Instant expirationDate,
            Did issuer
    ) {

        VerifiableCredentialBuilder builder =
                new VerifiableCredentialBuilder()
                        .context(List.of(
                                        URI.create("https://www.w3.org/2018/credentials/v1"),
                                        URI.create("https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json"),
                                        URI.create("https://w3id.org/security/suites/jws-2020/v1"),
                                        URI.create("https://catenax-ng.github.io/product-core-schemas/SummaryVC.json"),
                                        URI.create("https://w3id.org/security/suites/ed25519-2020/v1"),
                                        URI.create("https://w3id.org/vc/status-list/2021/v1")
                                )
                        )
                        .id(URI.create(issuer + "#key-1"))
                        .issuer(issuer.toUri())
                        .issuanceDate(Instant.now().minus(Duration.ofDays(5)))
                        .credentialSubject(credentialSubjects)
                        .expirationDate(expirationDate)
                        .type(types);

        try {
            System.out.println(new ObjectMapper().writeValueAsString(builder.build()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return builder;
    }

    public static Did generateDid(String host) {
        return new Did(
                new DidMethod("web"),
                new DidMethodIdentifier(host),
                null
        );
    }

    @SneakyThrows
    public static DidDocument buildDidDocument(Did did, KeyPair keyPair) {
        IPublicKey publicKey = keyPair.getPublicKey();
        MultibaseString publicKeyBase = MultibaseFactory.create(publicKey.asByte());

        // Building Verification Methods:
        List<VerificationMethod> verificationMethods = new ArrayList<>();
        Ed25519VerificationMethodBuilder builder = new Ed25519VerificationMethodBuilder();
        Ed25519VerificationMethod key = builder.id(URI.create(did.toUri() + "#key-" + 1)).controller(did.toUri()).publicKeyMultiBase(publicKeyBase).build();
        verificationMethods.add(key);
        DidDocumentBuilder didDocumentBuilder = new DidDocumentBuilder();
        didDocumentBuilder.id(did.toUri());
        didDocumentBuilder.verificationMethods(verificationMethods);

        return didDocumentBuilder.build();
    }

    public static Wallet mockWallet(String bpn, Did did, KeyPair keyPair) {
        Wallet wallet = mock(Wallet.class);
        when(wallet.getId()).thenReturn(new Random().nextLong());
        when(wallet.getName()).thenReturn("WalletName");
        when(wallet.getBpn()).thenReturn(bpn);
        when(wallet.getDid()).thenReturn(did.toUri().toString());
        when(wallet.getDidDocument()).thenReturn(buildDidDocument(did, keyPair));
        when(wallet.getAlgorithm()).thenReturn("Ed25519");
        when(wallet.getCreatedAt()).thenReturn(new Date());
        when(wallet.getModifiedAt()).thenReturn(new Date());
        when(wallet.getModifiedFrom()).thenReturn(null);
        return wallet;
    }

    public static void makeFilterWorkForHolder(HoldersCredentialRepository holdersCredentialRepository) {
        KeyPair keyPair = generateEDKeys();
        VerifiableCredential verifiableCredential = mockCredential(
                List.of("VerifiableCredential", "SummaryCredential"),
                List.of(mockCredentialSubject()),
                keyPair,
                "localhost",
                Instant.now().plus(Duration.ofDays(5))
        );
        HoldersCredential holdersCredential = mockHolderCredential(verifiableCredential);
        when(holdersCredentialRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(
                new PageImpl<>(List.of(holdersCredential))
        );
    }

    public static void makeFilterWorkForIssuer(IssuersCredentialRepository holdersCredentialRepository) {
        KeyPair keyPair = generateEDKeys();
        VerifiableCredential verifiableCredential = mockCredential(
                List.of("VerifiableCredential", "SummaryCredential"),
                List.of(mockCredentialSubject()),
                keyPair,
                "localhost",
                Instant.now().plus(Duration.ofDays(5))
        );
        IssuersCredential holdersCredential = mockIssuerCredential(verifiableCredential);
        when(holdersCredentialRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(
                new PageImpl<>(List.of(holdersCredential))
        );
    }

    public static void makeCreateWorkForHolder(HoldersCredentialRepository holdersCredentialRepository) {
        when(holdersCredentialRepository.save(any(HoldersCredential.class)))
                .thenAnswer(new Answer<HoldersCredential>() {
                                @Override
                                public HoldersCredential answer(InvocationOnMock invocation) {
                                    HoldersCredential argument = invocation.getArgument(0, HoldersCredential.class);
                                    argument.setId(42L);
                                    return argument;
                                }
                            }
                );
    }

    public static void makeCreateWorkForIssuer(IssuersCredentialRepository issuersCredentialRepository) {
        when(issuersCredentialRepository.save(any(IssuersCredential.class)))
                .thenAnswer(new Answer<IssuersCredential>() {
                                @Override
                                public IssuersCredential answer(InvocationOnMock invocation) {
                                    IssuersCredential argument = invocation.getArgument(0, IssuersCredential.class);
                                    argument.setId(42L);
                                    return argument;
                                }
                            }
                );
    }

    public static KeyPair generateEDKeys() {
        X25519Generator gen = new X25519Generator();
        KeyPair baseWalletKeys;
        try {
            baseWalletKeys = gen.generateKey();
        } catch (KeyGenerationException e) {
            throw new AssertionError(e);
        }
        return baseWalletKeys;
    }

    public static HoldersCredential mockHolderCredential(VerifiableCredential verifiableCredential) {


        HoldersCredential cred = mock(HoldersCredential.class);
        when(cred.getCredentialId()).thenReturn("credentialId");
        when(cred.getData()).thenReturn(verifiableCredential);
        return cred;
    }

    public static IssuersCredential mockIssuerCredential(VerifiableCredential verifiableCredential) {
        IssuersCredential cred = mock(IssuersCredential.class);
        when(cred.getCredentialId()).thenReturn("credentialId");
        when(cred.getData()).thenReturn(verifiableCredential);
        return cred;
    }

    public static VerifiableCredentialSubject mockCredentialSubject() {
        Map<String, Object> subj;
        try (InputStream in = MockUtil.class.getResourceAsStream("/credential-subject.json")) {
            subj = JSONObjectUtils.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }


        return new VerifiableCredentialSubject(subj);
    }

    public static VerifiableCredentialSubject mockCredentialSubject2() {
        Map<String, Object> subj;
        try (InputStream in = MockUtil.class.getResourceAsStream("/credential-subject-2.json")) {
            subj = JSONObjectUtils.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }


        return new VerifiableCredentialSubject(subj);
    }
}
