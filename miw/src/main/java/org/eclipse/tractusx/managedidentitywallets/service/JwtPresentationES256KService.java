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

package org.eclipse.tractusx.managedidentitywallets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.exception.SignatureFailureException;
import org.eclipse.tractusx.managedidentitywallets.exception.UnsupportedAlgorithmException;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocumentBuilder;
import org.eclipse.tractusx.ssi.lib.model.did.DidMethod;
import org.eclipse.tractusx.ssi.lib.model.did.DidMethodIdentifier;
import org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.did.VerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.serialization.jsonLd.JsonLdSerializer;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedVerifiablePresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.security.interfaces.ECPrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COLON_SEPARATOR;
import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.PRIVATE_KEY;
import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.PUBLIC_KEY;
import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.REFERENCE_KEY;
import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.VAULT_ACCESS_TOKEN;
import static org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils.getKeyString;
import static org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethod.JWK_CURVE;
import static org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethod.JWK_KEK_TYPE;
import static org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethod.JWK_X;
import static org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethod.PUBLIC_KEY_JWK;
import static org.eclipse.tractusx.ssi.lib.model.did.VerificationMethod.CONTROLLER;
import static org.eclipse.tractusx.ssi.lib.model.did.VerificationMethod.ID;
import static org.eclipse.tractusx.ssi.lib.model.did.VerificationMethod.TYPE;


@Service
@Slf4j
public class JwtPresentationES256KService {

    public static final String JWK_Y = "y";

    private JsonLdSerializer jsonLdSerializer;
    private Did agentDid;
    private WalletRepository walletRepository;
    private EncryptionUtils encryptionUtils;
    private WalletKeyService walletKeyService;
    private MIWSettings miwSettings;

    @Autowired
    public JwtPresentationES256KService(WalletRepository walletRepository, EncryptionUtils encryptionUtils, WalletKeyService walletKeyService, MIWSettings miwSettings) {
        this.walletRepository = walletRepository;
        this.encryptionUtils = encryptionUtils;
        this.walletKeyService = walletKeyService;
        this.miwSettings = miwSettings;
    }

    public JwtPresentationES256KService(Did agentDid, JsonLdSerializer jsonLdSerializer) {
        this.agentDid = agentDid;
        this.jsonLdSerializer = jsonLdSerializer;
    }

    public SignedJWT createPresentation(Did issuer, List<VerifiableCredential> credentials, String audience, ECPrivateKey ecPrivateKey) {
        VerifiablePresentationBuilder verifiablePresentationBuilder = new VerifiablePresentationBuilder();
        final VerifiablePresentation verifiablePresentation =
                verifiablePresentationBuilder
                        .id(URI.create(agentDid.toUri() + "#" + UUID.randomUUID()))
                        .type(List.of("VerifiablePresentation"))
                        .verifiableCredentials(credentials)
                        .build();
        final SerializedVerifiablePresentation serializedVerifiablePresentation =
                jsonLdSerializer.serializePresentation(verifiablePresentation);
        return createSignedJwt(verifiablePresentation.getId(), issuer, audience, serializedVerifiablePresentation, ecPrivateKey);
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRES_NEW)
    public Wallet storeWalletKeyES256K(Wallet wallet, String keyId) {
        try {
            ECKey ecKey = new ECKeyGenerator(Curve.SECP256K1)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .provider(BouncyCastleProviderSingleton.getInstance())
                    .generate();

            Did did = getDidFromDidString(wallet.getDid());

            JWKVerificationMethod jwkVerificationMethod = getJwkVerificationMethod(ecKey, did);
            DidDocument didDocument = wallet.getDidDocument();
            List<VerificationMethod> verificationMethods = didDocument.getVerificationMethods();
            verificationMethods.add(jwkVerificationMethod);
            DidDocument updatedDidDocument = buildDidDocument(wallet.getBpn(), did, verificationMethods);

            wallet = walletRepository.getByDid(wallet.getDid());
            wallet.setDidDocument(updatedDidDocument);
            walletRepository.save(wallet);

            WalletKey walletKeyES256K = WalletKey.builder()
                    .wallet(wallet)
                    .keyId(keyId)
                    .referenceKey(REFERENCE_KEY)
                    .vaultAccessToken(VAULT_ACCESS_TOKEN)
                    .privateKey(encryptionUtils.encrypt(getKeyString(ecKey.toECPrivateKey().getEncoded(), PRIVATE_KEY)))
                    .publicKey(encryptionUtils.encrypt(getKeyString(ecKey.toECPublicKey().getEncoded(), PUBLIC_KEY)))
                    .algorithm(SupportedAlgorithms.ES256K.toString())
                    .build();
            //Save key ES256K
            walletKeyService.getRepository().save(walletKeyES256K);
        } catch (JOSEException e) {
            throw new BadDataException("Could not generate EC Jwk", e);
        }
        return wallet;
    }

    private Did getDidFromDidString(String didString) {
        int index = StringUtils.ordinalIndexOf(didString, COLON_SEPARATOR, 2);
        String identifier = didString.substring(index + 1);
        DidMethod didMethod = new DidMethod("web");
        DidMethodIdentifier methodIdentifier = new DidMethodIdentifier(identifier);
        return new Did(didMethod, methodIdentifier, null);
    }

    private JWKVerificationMethod getJwkVerificationMethod(ECKey ecKey, Did did) {
        Map<String, Object> verificationMethodJson = new HashMap<>();
        Map<String, String> publicKeyJwk = Map.of(JWK_KEK_TYPE, ecKey.getKeyType().toString(), JWK_CURVE,
                ecKey.getCurve().getName(), JWK_X, ecKey.getX().toString(), JWK_Y, ecKey.getY().toString());
        verificationMethodJson.put(ID, URI.create(did + "#" + ecKey.getKeyID()));
        verificationMethodJson.put(TYPE, JWKVerificationMethod.DEFAULT_TYPE);
        verificationMethodJson.put(CONTROLLER, did.toUri());
        verificationMethodJson.put(PUBLIC_KEY_JWK, publicKeyJwk);
        return new JWKVerificationMethod(verificationMethodJson);
    }

    public DidDocument buildDidDocument(String bpn, Did did, List<VerificationMethod> jwkVerificationMethods) {
        DidDocumentBuilder didDocumentBuilder = new DidDocumentBuilder();
        didDocumentBuilder.id(did.toUri());
        didDocumentBuilder.verificationMethods(jwkVerificationMethods);
        DidDocument didDocument = didDocumentBuilder.build();
        //modify context URLs
        List<URI> context = didDocument.getContext();
        List<URI> mutableContext = new ArrayList<>(context);
        miwSettings.didDocumentContextUrls().forEach(uri -> {
            if (!mutableContext.contains(uri)) {
                mutableContext.add(uri);
            }
        });
        didDocument.put("@context", mutableContext);
        didDocument = DidDocument.fromJson(didDocument.toJson());
        log.debug("did document created for bpn ->{}", StringEscapeUtils.escapeJava(bpn));
        return didDocument;
    }

    public SignedJWT createSignedJwt(URI id, Did didIssuer, String audience, SerializedVerifiablePresentation serializedPresentation, ECPrivateKey ecPrivateKey) {
        String issuer = didIssuer.toString();
        String subject = didIssuer.toString();
        try {
            Map<String, Object> vp = new ObjectMapper().readValue(serializedPresentation.getJson(), HashMap.class);
            JWTClaimsSet claimsSet = (new JWTClaimsSet.Builder())
                    .issuer(issuer)
                    .subject(subject)
                    .audience(audience)
                    .claim("vp", vp)
                    .expirationTime(new Date((new Date()).getTime() + 60000L))
                    .jwtID(id.toString())
                    .build();
            return createSignedES256KJwt(ecPrivateKey, claimsSet, issuer);
        } catch (IOException e) {
            throw new BadDataException("Incorrect VP serialization");
        }
    }

    private static SignedJWT createSignedES256KJwt(ECPrivateKey ecPrivateKey, JWTClaimsSet claimsSet, String issuer) {
        try {
            JWSSigner signer = new ECDSASigner(ecPrivateKey);
            signer.getJCAContext().setProvider(BouncyCastleProviderSingleton.getInstance());
            if (!signer.supportedJWSAlgorithms().contains(JWSAlgorithm.ES256K)) {
                throw new UnsupportedAlgorithmException(String.format("Invalid signing method. Supported signing methods: %s",
                        signer.supportedJWSAlgorithms().stream().map(Algorithm::getName).collect(Collectors.joining(", "))));
            } else {
                JWSAlgorithm algorithm = JWSAlgorithm.ES256K;
                JOSEObjectType type = JOSEObjectType.JWT;
                JWSHeader header = new JWSHeader.Builder(algorithm).type(type).keyID(issuer).base64URLEncodePayload(true).build();
                SignedJWT vc = new SignedJWT(header, claimsSet);
                vc.sign(signer);
                return vc;
            }
        } catch (JOSEException e) {
            throw new SignatureFailureException("Creating signature failed", e);
        }
    }
}
