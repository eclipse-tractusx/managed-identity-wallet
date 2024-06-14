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
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.config.RevocationSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.domain.BusinessPartnerNumber;
import org.eclipse.tractusx.managedidentitywallets.domain.CredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.PresentationCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.domain.VerifiableEncoding;
import org.eclipse.tractusx.managedidentitywallets.interfaces.SecureTokenService;
import org.eclipse.tractusx.managedidentitywallets.service.JwtPresentationES256KService;
import org.eclipse.tractusx.ssi.lib.crypt.IKeyGenerator;
import org.eclipse.tractusx.ssi.lib.crypt.IPrivateKey;
import org.eclipse.tractusx.ssi.lib.crypt.IPublicKey;
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
import org.eclipse.tractusx.ssi.lib.model.base.EncodeType;
import org.eclipse.tractusx.ssi.lib.model.did.DidParser;
import org.eclipse.tractusx.ssi.lib.model.proof.Proof;
import org.eclipse.tractusx.ssi.lib.model.verifiable.Verifiable;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;
import org.eclipse.tractusx.ssi.lib.proof.SignatureType;
import org.eclipse.tractusx.ssi.lib.serialization.jsonld.JsonLdSerializerImpl;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactory;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactoryImpl;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtVCFactoryImpl;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class LocalSigningServiceImpl implements LocalSigningService {

    // this is not autowired by name, and injected by ApplicationConfig,
    // since keys could be saved in hashicorp (=> HashicorpKeyProvider, e.g.) and retrieved from there
    private KeyProvider keyProvider;

    // Autowired by name!!!
    private final SecureTokenService localSecureTokenService;

    private final RevocationSettings revocationSettings;

    @Override
    public SignerResult createCredential(CredentialCreationConfig config) {

        byte[] privateKeyBytes = getPrivateKeyBytes(config.getKeyName(), config.getAlgorithm());
        VerifiableEncoding encoding = Objects.requireNonNull(config.getEncoding());
        SignerResult.SignerResultBuilder resultBuilder = SignerResult.builder().encoding(encoding);
        switch (encoding) {
            case JSON_LD -> {
                return resultBuilder.jsonLd(createVerifiableCredential(config, privateKeyBytes)).build();
            }
            case JWT -> {

                //TODO maybe this we want, currently in VC as JET, we are putting signed VC(VC with proof) as a JWT claim
                //instead of this we should put VC without proof and utilize JWT signature as a proof
                SignedJWT verifiableCredentialAsJwt = createVerifiableCredentialAsJwt(config);
                return resultBuilder.jwt(verifiableCredentialAsJwt.serialize()).build();
            }
            default ->
                    throw new IllegalArgumentException("encoding %s is not supported".formatted(config.getEncoding()));

        }
    }

    private byte[] getPrivateKeyBytes(String keyName, SupportedAlgorithms supportedAlgorithms) {
        byte[] privateKeyBytes;
        if (supportedAlgorithms.equals(SupportedAlgorithms.ED25519)) {
            privateKeyBytes = ((IPrivateKey) keyProvider.getPrivateKey(keyName, supportedAlgorithms)).asByte();
        } else if (supportedAlgorithms.equals(SupportedAlgorithms.ES256K)) {
            ECPrivateKey ecKey = (ECPrivateKey) keyProvider.getPrivateKey(keyName, supportedAlgorithms);
            privateKeyBytes = ecKey.getEncoded();
        } else {
            throw new IllegalArgumentException("Unknown algorithm " + supportedAlgorithms);
        }
        return privateKeyBytes;
    }

    @SneakyThrows
    private SignedJWT createVerifiableCredentialAsJwt(CredentialCreationConfig config) {
        if (!config.getAlgorithm().equals(SupportedAlgorithms.ED25519)) {
            throw new IllegalArgumentException("VC as JWT is not supported for provided algorithm -> " + config.getAlgorithm());
        }
        // JWT Factory
        SerializedJwtVCFactoryImpl vcFactory = new SerializedJwtVCFactoryImpl(
                new SignedJwtFactory(new OctetKeyPairFactory()));
        IPrivateKey iPrivateKey = ((IPrivateKey) keyProvider.getPrivateKey(config.getKeyName(), config.getAlgorithm()));

        return vcFactory.createVCJwt(DidParser.parse(config.getIssuerDoc().getId()), DidParser.parse(config.getHolderDid()), config.getVerifiableCredential(),
                iPrivateKey,
                keyProvider.getKeyId(config.getKeyName(), config.getAlgorithm()));
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
                            public String asStringForStoring() {
                                return null;
                            }

                            @Override
                            public String asStringForExchange(EncodeType encodeType) {
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
                            public String asStringForStoring() {
                                return null;
                            }

                            @Override
                            public String asStringForExchange(EncodeType encodeType) {
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
                    IKeyGenerator keyGenerator = new X25519Generator();
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
        byte[] privateKeyBytes = getPrivateKeyBytes(config.getKeyName(), config.getAlgorithm());
        VerifiableEncoding encoding = Objects.requireNonNull(config.getEncoding());
        SignerResult.SignerResultBuilder resultBuilder = SignerResult.builder().encoding(encoding);
        switch (config.getEncoding()) {
            case JWT -> {
                return resultBuilder.jwt(generateJwtPresentation(config, privateKeyBytes).serialize()).build();
            }
            case JSON_LD -> {
                try {
                    return resultBuilder.jsonLd(generateJsonLdPresentation(config, privateKeyBytes)).build();
                } catch (UnsupportedSignatureTypeException | InvalidPrivateKeyFormatException |
                         SignatureGenerateFailedException | TransformJsonLdException e) {
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

    private VerifiablePresentation generateJsonLdPresentation(PresentationCreationConfig config, byte[] privateKeyBytes) throws UnsupportedSignatureTypeException, InvalidPrivateKeyFormatException, SignatureGenerateFailedException, TransformJsonLdException {
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

        X25519PrivateKey privateKey = new X25519PrivateKey(privateKeyBytes);

        Proof proof = generator.createProof(verifiablePresentation, config.getVerificationMethod(),
                privateKey);
        verifiablePresentation.put(Verifiable.PROOF, proof);
        return verifiablePresentation;
    }

    @SneakyThrows
    private static VerifiableCredential createVerifiableCredential(CredentialCreationConfig config, byte[] privateKeyBytes) {
        // if the credential does not contain the JWS proof-context add it
        URI jwsUri = URI.create("https://w3id.org/security/suites/jws-2020/v1");
        if (!config.getContexts().contains(jwsUri)) {
            config.getContexts().add(jwsUri);
        }

        URI id = URI.create(UUID.randomUUID().toString());
        VerifiableCredentialBuilder builder = new VerifiableCredentialBuilder()
                .context(config.getContexts())
                .id(URI.create(config.getIssuerDoc().getId() + "#" + id))
                .type(config.getTypes())
                .issuer(config.getIssuerDoc().getId())
                .expirationDate(config.getExpiryDate() != null ? config.getExpiryDate().toInstant() : null)
                .issuanceDate(Instant.now())
                .credentialSubject(config.getSubject());

        //set status list
        if (config.isRevocable()) {
            builder.verifiableCredentialStatus(config.getVerifiableCredentialStatus());
        }

        LinkedDataProofGenerator generator = LinkedDataProofGenerator.newInstance(SignatureType.JWS);
        URI verificationMethod = config.getIssuerDoc().getVerificationMethods().get(0).getId();

        Proof proof = generator.createProof(builder.build(), verificationMethod, new X25519PrivateKey(privateKeyBytes));

        //Adding Proof to VC
        builder.proof(proof);

        //Create Credential
        return builder.build();
    }


    private SignedJWT buildED25519(PresentationCreationConfig config, byte[] privateKeyBytes) {
        SerializedJwtPresentationFactory presentationFactory = new SerializedJwtPresentationFactoryImpl(
                new SignedJwtFactory(new OctetKeyPairFactory()), new JsonLdSerializerImpl(), config.getVpIssuerDid());

        X25519PrivateKey privateKey = null;
        try {
            privateKey = new X25519PrivateKey(privateKeyBytes);
        } catch (InvalidPrivateKeyFormatException e) {
            throw new IllegalArgumentException(e);
        }
        return presentationFactory.createPresentation(config.getVpIssuerDid(), config.getVerifiableCredentials(), config.getAudience(), privateKey, config.getKeyName());
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


    @Override
    public JWT issueToken(DID self, DID partner, Set<String> scopes) {
        return localSecureTokenService.issueToken(self, partner, scopes, keyProvider);
    }

    @Override
    public JWT issueToken(BusinessPartnerNumber self, BusinessPartnerNumber partner, Set<String> scopes) {
        return localSecureTokenService.issueToken(self, partner, scopes, keyProvider);
    }

    @Override
    public JWT issueToken(DID self, DID partner, JWT accessToken) {
        return localSecureTokenService.issueToken(self, partner, accessToken, keyProvider);
    }

    @Override
    public JWT issueToken(BusinessPartnerNumber self, BusinessPartnerNumber partner, JWT accessToken) {
        return localSecureTokenService.issueToken(self, partner, accessToken, keyProvider);
    }
}
