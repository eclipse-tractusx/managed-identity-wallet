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
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.exception.SignatureFailureException;
import org.eclipse.tractusx.managedidentitywallets.exception.UnsupportedAlgorithmException;
import org.eclipse.tractusx.ssi.lib.model.JsonLdObject;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocumentBuilder;
import org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.did.VerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.verifiable.Verifiable;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.serialization.jsonld.JsonLdSerializer;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedVerifiablePresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private MIWSettings miwSettings;

    @Autowired
    public JwtPresentationES256KService(MIWSettings miwSettings) {
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


    public JWKVerificationMethod getJwkVerificationMethod(ECKey ecKey, Did did) {
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
        didDocument.put(JsonLdObject.CONTEXT, mutableContext);
        //add assertionMethod
        List<URI> ids = new ArrayList<>();
        jwkVerificationMethods.forEach((verificationMethod) -> {
            ids.add(verificationMethod.getId());
        });
        didDocument.put(StringPool.ASSERTION_METHOD, ids);
        //add service
        Map<String, Object> tokenServiceData = Map.of(Verifiable.ID, did.toUri() + "#" + StringPool.SECURITY_TOKEN_SERVICE, Verifiable.TYPE, StringPool.SECURITY_TOKEN_SERVICE,
                StringPool.SERVICE_ENDPOINT, StringPool.HTTPS_SCHEME + miwSettings.host() + "/api/token");
        org.eclipse.tractusx.ssi.lib.model.did.Service tokenService = new org.eclipse.tractusx.ssi.lib.model.did.Service(tokenServiceData);
        Map<String, Object> credentialServiceData = Map.of(Verifiable.ID, did.toUri() + "#" + StringPool.CREDENTIAL_SERVICE, Verifiable.TYPE, StringPool.CREDENTIAL_SERVICE,
                StringPool.SERVICE_ENDPOINT, StringPool.HTTPS_SCHEME + miwSettings.host());
        org.eclipse.tractusx.ssi.lib.model.did.Service credentialService = new org.eclipse.tractusx.ssi.lib.model.did.Service(credentialServiceData);
        didDocument.put(StringPool.SERVICE, List.of(tokenService, credentialService));

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
