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

package org.eclipse.tractusx.managedidentitywallets.sts;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyPair;
import org.eclipse.tractusx.managedidentitywallets.interfaces.SecureTokenIssuer;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.lib.crypt.octet.OctetKeyPairFactory;
import org.eclipse.tractusx.ssi.lib.crypt.x21559.x21559PrivateKey;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.tractusx.managedidentitywallets.utils.TokenParsingUtils.getNonceAccessToken;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.SCOPE;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.NONCE;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureTokenIssuerImpl implements SecureTokenIssuer {

    private final EncryptionUtils encryptionUtils;

    @Override
    public JWT createIdToken(KeyPair keyPair, DID self, DID partner, Instant expirationTime, JWT accessToken) {
        log.debug("'createIdToken' using a provided access_token.");
        return createSignedJWT(keyPair, new JWTClaimsSet.Builder()
                .issuer(self.toString())
                .audience(partner.toString())
                .subject(self.toString())
                .expirationTime(Date.from(expirationTime))
                .claim(NONCE, getNonceAccessToken(accessToken))
                .claim(ACCESS_TOKEN, accessToken.serialize()));
    }

    @Override
    public JWT createAccessToken(KeyPair keyPair, DID self, DID partner, Instant expirationTime, Set<String> scopes) {
        log.debug("'createAccessToken' using scopes.");
        return createSignedJWT(keyPair, new JWTClaimsSet.Builder()
                .issuer(self.toString())
                .audience(self.toString())
                .subject(partner.toString())
                .expirationTime(Date.from(expirationTime))
                .claim(NONCE, UUID.randomUUID().toString())
                .claim(SCOPE, String.join(" ", scopes)));
    }

    @SneakyThrows
    private JWT createSignedJWT(KeyPair keyPair, JWTClaimsSet.Builder builder) {
        log.debug("Creating JWS header for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
                builder.getClaims().get("sub"));
        // todo bri: we're hard-coding the algorithm for now. Should become dynamic in the future.
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .type(JOSEObjectType.JWT)
                .keyID(keyPair.keyId())
                .build();

        log.debug("Creating JWS body for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
                builder.getClaims().get("sub"));
        JWTClaimsSet body = builder
                .issueTime(Date.from(Instant.now()))
                .jwtID(UUID.randomUUID().toString())
                .build();

        log.debug("Creating JWS signature for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
                builder.getClaims().get("sub"));
        SignedJWT signedJWT = new SignedJWT(header, body);
        String privateKey = encryptionUtils.decrypt(keyPair.privateKey());
        // todo bri: this should become dynamic in the future, as we want to support more key algos.
        OctetKeyPair jwk = new OctetKeyPairFactory().fromPrivateKey(new x21559PrivateKey(privateKey, true));
        Ed25519Signer signer = new Ed25519Signer(jwk);
        signedJWT.sign(signer);
        log.debug("JWT signed for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
                builder.getClaims().get("sub"));

        return signedJWT;
    }
}
