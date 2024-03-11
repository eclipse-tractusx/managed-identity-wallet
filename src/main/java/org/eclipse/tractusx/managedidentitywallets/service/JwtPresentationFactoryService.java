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
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.serialization.jsonLd.JsonLdSerializer;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedVerifiablePresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.security.interfaces.ECPrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JwtPresentationFactoryService {
    private static final Logger log = LoggerFactory.getLogger(JwtPresentationFactoryService.class);
    private final JsonLdSerializer jsonLdSerializer;
    private final Did agentDid;

    public JwtPresentationFactoryService(Did agentDid, JsonLdSerializer jsonLdSerializer) {
        this.agentDid = agentDid;
        this.jsonLdSerializer = jsonLdSerializer;
    }

    public SignedJWT createPresentation(Did issuer, List<VerifiableCredential> credentials, String audience, ECPrivateKey ecPrivateKey) throws IOException {
        VerifiablePresentationBuilder verifiablePresentationBuilder = new VerifiablePresentationBuilder();
        URI uri = agentDid.toUri();
        VerifiablePresentation verifiablePresentation = verifiablePresentationBuilder.id(URI.create("" + uri + "#" + UUID.randomUUID())).type(List.of("VerifiablePresentation")).verifiableCredentials(credentials).build();
        SerializedVerifiablePresentation serializedVerifiablePresentation = jsonLdSerializer.serializePresentation(verifiablePresentation);
        return create(issuer, audience, serializedVerifiablePresentation, ecPrivateKey);
    }

    public SignedJWT create(Did didIssuer, String audience, SerializedVerifiablePresentation serializedPresentation, ECPrivateKey ecPrivateKey) throws IOException {
        try {
            String issuer = didIssuer.toString();
            String subject = didIssuer.toString();
            Map<String, Object> vp = (Map)(new ObjectMapper()).readValue(serializedPresentation.getJson(), HashMap.class);
            JWTClaimsSet claimsSet = (new JWTClaimsSet.Builder()).issuer(issuer).subject(subject).audience(audience)
                    .claim("vp", vp).expirationTime(new Date((new Date()).getTime() + 60000L)).jwtID(UUID.randomUUID().toString()).build();
            return createSignedES256KJwt(ecPrivateKey, claimsSet, issuer);
        } catch (Exception ex) {
            log.error(ex.toString());
            throw ex;
        }
    }

    private static SignedJWT createSignedES256KJwt(ECPrivateKey ecPrivateKey, JWTClaimsSet claimsSet, String issuer) {
        try {
            JWSSigner signer = new ECDSASigner(ecPrivateKey);
            if (!signer.supportedJWSAlgorithms().contains(JWSAlgorithm.ES256K)) {
                throw new RuntimeException(String.format("Invalid signing method. Supported signing methods: %s", signer.supportedJWSAlgorithms().stream().map(Algorithm::getName).collect(Collectors.joining(", "))));
            } else {
                JWSAlgorithm algorithm = JWSAlgorithm.ES256K;
                JOSEObjectType type = JOSEObjectType.JWT;
                JWSHeader header = new JWSHeader(algorithm, type, null, null, null, null, null, null, null, null, issuer, true, (Map)null, (Base64URL)null);
                SignedJWT vc = new SignedJWT(header, claimsSet);
                vc.sign(signer);
                return vc;
            }
        } catch (Exception e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        }
    }
}
