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

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyPair;
import org.eclipse.tractusx.managedidentitywallets.domain.SecureTokenIssuer;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureTokenIssuerAdapter implements SecureTokenIssuer {

    private static final String ACCESS_TOKEN = "access_token";

    private final SecureTokenConfigurationProperties properties;

    @Override
    public JWT issueIdToken(DID self, DID partner, KeyPair keyPair, Set<String> scopes) {
        log.info("Requested ID token for us: '{}' and partner '{}'", self, partner);
        Instant expirationTime = Instant.now().plus(properties.tokenDuration());

        return createAccessToken(keyPair, self, partner, expirationTime, scopes)
                .map(accessToken -> createIdToken(keyPair, self, partner, expirationTime, accessToken))
                .orElseGet(() -> createIdToken(keyPair, self, partner, expirationTime));
    }

    @Override
    public JWT issueIdToken(DID self, DID partner, KeyPair keyPair, JWT accessToken) {
        log.info("Requested ID token for us: '{}' and partner '{}' with existing access token.", self, partner);
        Instant expirationTime = Instant.now().plus(properties.tokenDuration());
        return createIdToken(keyPair, self, partner, expirationTime, accessToken);
    }

    private JWT createIdToken(KeyPair keyPair, DID self, DID partner, Instant expirationTime) {
        return createToken(keyPair, new JWTClaimsSet.Builder()
                .issuer(self.toString())
                .audience(partner.toString())
                .subject(self.toString())
                .expirationTime(Date.from(expirationTime)));
    }

    private JWT createIdToken(KeyPair keyPair, DID self, DID partner, Instant expirationTime, JWT accessToken) {
        return createToken(keyPair, new JWTClaimsSet.Builder()
                .issuer(self.toString())
                .audience(partner.toString())
                .subject(self.toString())
                .expirationTime(Date.from(expirationTime))
                .claim(ACCESS_TOKEN, accessToken));
    }

    private Optional<JWT> createAccessToken(KeyPair keyPair, DID self, DID partner, Instant expirationTime,
                                            Set<String> scopes) {
        log.info("Access token created for us: '{}' expiring at '{}'", self, expirationTime);
        if (scopes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(createToken(keyPair, new JWTClaimsSet.Builder()
                .issuer(self.toString())
                .audience(partner.toString())
                .subject(partner.toString())
                .claim("", scopes) // TODO how to map scopes to claims??
                .expirationTime(Date.from(expirationTime))));
    }

    @SneakyThrows
    private JWT createToken(KeyPair keyPair, JWTClaimsSet.Builder builder) {
        log.debug("Creating JWS header for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
                builder.getClaims().get("sub"));
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .type(JOSEObjectType.JWT)
                .keyID(keyPair.keyId())
                .build();

        log.debug("Creating JWT body for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
                builder.getClaims().get("sub"));
        JWTClaimsSet body = builder
                .issueTime(Date.from(Instant.now()))
                .jwtID(UUID.randomUUID().toString())
                .build();

        log.debug("Creating JWS signature for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
                builder.getClaims().get("sub"));
        SignedJWT signedJWT = new SignedJWT(header, body);
        OctetKeyPair jwk = new OctetKeyPair.Builder(Curve.Ed25519, new Base64URL(keyPair.publicKey()))
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyPair.keyId())
                .d(new Base64URL(keyPair.privateKey()))
                .build();

        signedJWT.sign(new Ed25519Signer(jwk));
        log.debug("JWT signed for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
                builder.getClaims().get("sub"));

        return signedJWT;
    }

}
