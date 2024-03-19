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

package org.eclipse.tractusx.managedidentitywallets;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.domain.HoldersCredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyStorageType;
import org.eclipse.tractusx.managedidentitywallets.domain.PresentationCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.service.WalletKeyService;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.ssi.lib.crypt.IKeyGenerator;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.crypt.octet.OctetKeyPairFactory;
import org.eclipse.tractusx.ssi.lib.crypt.x25519.X25519Generator;
import org.eclipse.tractusx.ssi.lib.crypt.x25519.X25519PrivateKey;
import org.eclipse.tractusx.ssi.lib.exception.json.TransformJsonLdException;
import org.eclipse.tractusx.ssi.lib.exception.key.InvalidPrivateKeyFormatException;
import org.eclipse.tractusx.ssi.lib.exception.key.KeyGenerationException;
import org.eclipse.tractusx.ssi.lib.exception.proof.SignatureGenerateFailedException;
import org.eclipse.tractusx.ssi.lib.exception.proof.UnsupportedSignatureTypeException;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtFactory;
import org.eclipse.tractusx.ssi.lib.model.JsonLdObject;
import org.eclipse.tractusx.ssi.lib.model.proof.Proof;
import org.eclipse.tractusx.ssi.lib.model.verifiable.Verifiable;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;
import org.eclipse.tractusx.ssi.lib.proof.SignatureType;
import org.eclipse.tractusx.ssi.lib.serialization.jsonld.JsonLdSerializerImpl;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactory;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactoryImpl;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class DBKeyStorageService implements KeyStorageService {

    private final WalletKeyService walletKeyService;

    @Override
    public HoldersCredential createHoldersCredential(HoldersCredentialCreationConfig config) {
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdAsBytes(config.getWalletId(), SupportedAlgorithms.ED25519.name());
        return CommonUtils.getHoldersCredential(config.getSubject(),
                config.getTypes(), config.getIssuerDoc(), privateKeyBytes, config.getHolderDid(), config.getContexts(), config.getExpiryDate(), config.isSelfIssued());
    }

    @Override
    public KeyPair getKey() throws KeyGenerationException {
        IKeyGenerator keyGenerator = new X25519Generator();
        return keyGenerator.generateKey();
    }

    @Override
    public KeyStorageType getSupportedStorageType() {
        return KeyStorageType.DB;
    }

    @Override
    public String createPresentation(PresentationCreationConfig config) {
        Objects.requireNonNull(config);
        switch (config.getType()) {
            case JWT -> {
                return generateJwtPresentation(config).serialize();
            }
            case JSON_LD -> {
                try {
                    return generateJsonLdPresentation(config).toJson();
                } catch (UnsupportedSignatureTypeException | SignatureGenerateFailedException |
                         TransformJsonLdException | InvalidPrivateKeyFormatException e) {
                    throw new IllegalStateException(e);
                }
            }
            default ->
                    throw new IllegalArgumentException("config type %s is not supported".formatted(config.getType()));
        }
    }

    private SignedJWT generateJwtPresentation(PresentationCreationConfig config) {
        SerializedJwtPresentationFactory presentationFactory = new SerializedJwtPresentationFactoryImpl(
                new SignedJwtFactory(new OctetKeyPairFactory()), new JsonLdSerializerImpl(), config.getVpIssuerDid());

        //Build JWT
        X25519PrivateKey privateKey = null;
        try {
            privateKey = new X25519PrivateKey(walletKeyService.getPrivateKeyByWalletIdAsBytes(config.getWalletId(), SupportedAlgorithms.ED25519.name()));
        } catch (InvalidPrivateKeyFormatException e) {
            throw new IllegalArgumentException(e);
        }
        String keyId = walletKeyService.getWalletKeyIdByWalletId(config.getWalletId(), SupportedAlgorithms.ED25519);
        return presentationFactory.createPresentation(config.getVpIssuerDid(), config.getVerifiableCredentials(), config.getAudience(), privateKey, keyId);
    }

    private VerifiablePresentation generateJsonLdPresentation(PresentationCreationConfig config) throws UnsupportedSignatureTypeException, SignatureGenerateFailedException, TransformJsonLdException, InvalidPrivateKeyFormatException {
        VerifiablePresentationBuilder verifiablePresentationBuilder =
                new VerifiablePresentationBuilder().id(URI.create(config.getVpIssuerDid() + "#" + UUID.randomUUID().toString()))
                        .type(List.of(VerifiablePresentationType.VERIFIABLE_PRESENTATION))
                        .verifiableCredentials(config.getVerifiableCredentials());


        VerifiablePresentation verifiablePresentation = verifiablePresentationBuilder.build();
        List<String> contexts = verifiablePresentation.getContext().stream().map(URI::toString).collect(Collectors.toList());
        if (!contexts.contains(StringPool.W3_ID_JWS_2020_V1_CONTEXT_URL)) {
            contexts.add(StringPool.W3_ID_JWS_2020_V1_CONTEXT_URL);
        }
        verifiablePresentation.put(JsonLdObject.CONTEXT, contexts);
        LinkedDataProofGenerator generator = LinkedDataProofGenerator.newInstance(SignatureType.JWS);

        X25519PrivateKey privateKey = new X25519PrivateKey(walletKeyService.getPrivateKeyByWalletIdAsBytes(config.getWalletId(), SupportedAlgorithms.ED25519.name()));

        Proof proof = generator.createProof(verifiablePresentation, config.getVerificationMethod(),
                privateKey);
        verifiablePresentation.put(Verifiable.PROOF, proof);
        return verifiablePresentation;
    }
}
