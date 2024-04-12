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

package org.eclipse.tractusx.managedidentitywallets.signing;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.domain.CredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.PresentationCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.domain.VerifiableEncoding;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.service.JwtPresentationES256KService;
import org.eclipse.tractusx.ssi.lib.crypt.IKeyGenerator;
import org.eclipse.tractusx.ssi.lib.crypt.IPrivateKey;
import org.eclipse.tractusx.ssi.lib.crypt.IPublicKey;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.crypt.octet.OctetKeyPairFactory;
import org.eclipse.tractusx.ssi.lib.crypt.x21559.x21559Generator;
import org.eclipse.tractusx.ssi.lib.crypt.x21559.x21559PrivateKey;
import org.eclipse.tractusx.ssi.lib.exception.InvalidePrivateKeyFormat;
import org.eclipse.tractusx.ssi.lib.exception.KeyGenerationException;
import org.eclipse.tractusx.ssi.lib.exception.UnsupportedSignatureTypeException;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtFactory;
import org.eclipse.tractusx.ssi.lib.model.JsonLdObject;
import org.eclipse.tractusx.ssi.lib.model.base.EncodeType;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.proof.Proof;
import org.eclipse.tractusx.ssi.lib.model.proof.jws.JWSSignature2020;
import org.eclipse.tractusx.ssi.lib.model.verifiable.Verifiable;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;
import org.eclipse.tractusx.ssi.lib.proof.SignatureType;
import org.eclipse.tractusx.ssi.lib.serialization.jsonLd.JsonLdSerializerImpl;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactory;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactoryImpl;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class LocalSigningServiceImpl implements LocalSigningService {

    private KeyProvider keyProvider;

    @Override
    public SignerResult createCredential(CredentialCreationConfig config) {
        byte[] privateKeyBytes = keyProvider.getPrivateKey(config.getKeyName(), config.getAlgorithm());
        VerifiableEncoding encoding = Objects.requireNonNull(config.getEncoding());
        SignerResult.SignerResultBuilder resultBuilder = SignerResult.builder().encoding(encoding);
        switch (encoding) {
            case JSON_LD -> {
                return resultBuilder.jsonLd(createVerifiableCredential(config, privateKeyBytes)).build();
            }
            case JWT -> throw new NotImplementedException("not implemented yet");
            default ->
                    throw new IllegalArgumentException("encoding %s is not supported".formatted(config.getEncoding()));

        }
    }

    @Override
    public Map<KeyType, KeyPair> getKeys(KeyCreationConfig config) throws KeyGenerationException {
        List<KeyType> keyTypes = Objects.requireNonNull(config.getKeyTypes());
        Map<KeyType, KeyPair> result = new HashMap<>();
        for (KeyType keyType : keyTypes) {
            switch (keyType.getValue().toUpperCase()) {
                case "EC" -> {
                    try {
                        ECKey ecKey = new ECKeyGenerator(Curve.SECP256K1)
                                .provider(BouncyCastleProviderSingleton.getInstance())
                                .generate();
                        ECPrivateKey ecPrivateKey = ecKey.toECPrivateKey();
                        ECPublicKey ecPublicKey = ecKey.toECPublicKey();

                        result.put(keyType, new KeyPair(new IPublicKey() {
                            @Override
                            public int getKeyLength() {
                                return 0;
                            }

                            @Override
                            public String asStringForStoring() throws IOException {
                                return null;
                            }

                            @Override
                            public String asStringForExchange(EncodeType encodeType) throws IOException {
                                return null;
                            }

                            @Override
                            public byte[] asByte() {
                                return ecPublicKey.getEncoded();
                            }
                        }, new IPrivateKey() {
                            @Override
                            public int getKeyLength() {
                                return 0;
                            }

                            @Override
                            public String asStringForStoring() throws IOException {
                                return null;
                            }

                            @Override
                            public String asStringForExchange(EncodeType encodeType) throws IOException {
                                return null;
                            }

                            @Override
                            public byte[] asByte() {
                                return ecPrivateKey.getEncoded();
                            }
                        }));
                    } catch (JOSEException e) {
                        throw new BadDataException("Could not generate EC Jwk", e);
                    }
                }
                case "RSA" ->
                        throw new NotImplementedException("%s is not implemented yet".formatted(keyType.toString()));
                case "OCT" -> {
                    IKeyGenerator keyGenerator = new x21559Generator();
                    result.put(keyType, keyGenerator.generateKey());
                }
                default -> throw new IllegalArgumentException("%s is not supported".formatted(keyType.toString()));
            }
        }
        return result;
    }

    @Override
    public SigningServiceType getSupportedServiceType() {
        return SigningServiceType.LOCAL;
    }

    @Override
    public SignerResult createPresentation(PresentationCreationConfig config) {
        byte[] privateKeyBytes = keyProvider.getPrivateKey(config.getKeyName(), config.getAlgorithm());
        VerifiableEncoding encoding = Objects.requireNonNull(config.getEncoding());
        SignerResult.SignerResultBuilder resultBuilder = SignerResult.builder().encoding(encoding);
        switch (config.getEncoding()) {
            case JWT -> {
                return resultBuilder.jwt(generateJwtPresentation(config, privateKeyBytes).serialize()).build();
            }
            case JSON_LD -> {
                try {
                    return resultBuilder.jsonLd(generateJsonLdPresentation(config, privateKeyBytes)).build();
                } catch (UnsupportedSignatureTypeException | InvalidePrivateKeyFormat e) {
                    throw new IllegalStateException(e);
                }
            }
            default ->
                    throw new IllegalArgumentException("encoding %s is not supported".formatted(config.getEncoding()));
        }
    }

    @Override
    public void setKeyProvider(KeyProvider keyProvider) {
        this.keyProvider = Objects.requireNonNull(keyProvider);
    }

    @Override
    public void saveKeys(List<WalletKey> key) {
        keyProvider.saveKeys(key);
    }

    private SignedJWT generateJwtPresentation(PresentationCreationConfig config, byte[] privateKeyBytes) {

        if (config.getAlgorithm() == SupportedAlgorithms.ES256K) {
            return buildES256K(config, privateKeyBytes);
        } else if (config.getAlgorithm() == SupportedAlgorithms.ED25519) {
            return buildED25519(config, privateKeyBytes);
        }

        throw new IllegalArgumentException("algorithm %s is not supported for VP JWT".formatted(config.getAlgorithm().name()));
    }

    private VerifiablePresentation generateJsonLdPresentation(PresentationCreationConfig config, byte[] privateKeyBytes) throws UnsupportedSignatureTypeException, InvalidePrivateKeyFormat {
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

        x21559PrivateKey privateKey = new x21559PrivateKey(privateKeyBytes);

        Proof proof = generator.createProof(verifiablePresentation, config.getVerificationMethod(),
                privateKey);
        verifiablePresentation.put(Verifiable.PROOF, proof);
        return verifiablePresentation;
    }

    @SneakyThrows({ UnsupportedSignatureTypeException.class, InvalidePrivateKeyFormat.class })
    private static VerifiableCredential createVerifiableCredential(CredentialCreationConfig config, byte[] privateKeyBytes) {
        //VC Builder

        // if the credential does not contain the JWS proof-context add it
        URI jwsUri = URI.create("https://w3id.org/security/suites/jws-2020/v1");
        if (!config.getContexts().contains(jwsUri)) {
            config.getContexts().add(jwsUri);
        }

        // check if the expiryDate is set
        // if its null then it will be ignored from the SSI Lib (VerifiableCredentialBuilder) and will not be added to the VC
        Instant expiryInstant = config.getExpiryDate().toInstant();


        URI id = URI.create(UUID.randomUUID().toString());
        VerifiableCredentialBuilder builder = new VerifiableCredentialBuilder()
                .context(config.getContexts())
                .id(URI.create(config.getIssuerDoc().getId() + "#" + id))
                .type(config.getTypes())
                .issuer(config.getIssuerDoc().getId())
                .expirationDate(expiryInstant)
                .issuanceDate(Instant.now())
                .credentialSubject(config.getSubject());


        LinkedDataProofGenerator generator = LinkedDataProofGenerator.newInstance(SignatureType.JWS);
        URI verificationMethod = config.getIssuerDoc().getVerificationMethods().get(0).getId();

        JWSSignature2020 proof =
                (JWSSignature2020) generator.createProof(builder.build(), verificationMethod, new x21559PrivateKey(privateKeyBytes));


        //Adding Proof to VC
        builder.proof(proof);

        //Create Credential
        return builder.build();
    }


    private SignedJWT buildED25519(PresentationCreationConfig config, byte[] privateKeyBytes) {
        SerializedJwtPresentationFactory presentationFactory = new SerializedJwtPresentationFactoryImpl(
                new SignedJwtFactory(new OctetKeyPairFactory()), new JsonLdSerializerImpl(), config.getVpIssuerDid());

        x21559PrivateKey privateKey = null;
        try {
            privateKey = new x21559PrivateKey(privateKeyBytes);
        } catch (InvalidePrivateKeyFormat e) {
            throw new IllegalArgumentException(e);
        }
        return presentationFactory.createPresentation(config.getVpIssuerDid()
                , config.getVerifiableCredentials(), config.getAudience(), privateKey);
    }

    @SneakyThrows
    private SignedJWT buildES256K(PresentationCreationConfig config, byte[] privateKeyBytes) {
        var kf = KeyFactory.getInstance("EC");
        var privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        ECPrivateKey ecPrivateKey = (ECPrivateKey) kf.generatePrivate(privateKeySpec);

        JwtPresentationES256KService presentationFactory = new JwtPresentationES256KService(config.getVpIssuerDid(), new JsonLdSerializerImpl());
        return presentationFactory.createPresentation(config.getVpIssuerDid()
                , config.getVerifiableCredentials(), config.getAudience(), ecPrivateKey);
    }


}
