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

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.service.DidDocumentService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;

/**
 * Contains methods for validating token claims.
 */
@Component
@RequiredArgsConstructor
public class TokenValidationUtils {

    private final DidDocumentService service;

    public static final String NONCE = "nonce";
    public static final String DID_FORMAT = "did:";
    private static final int IAT_LEEWAY = 5;

    public Optional<String> checkIfIssuerEqualsSubject(JWTClaimsSet claims) {
        String iss = claims.getIssuer();
        String sub = claims.getSubject();
        return (iss != null && Objects.equals(iss, sub)) ?
                Optional.empty() : Optional.of("The 'iss' and 'sub' claims must be non-null and identical.");
    }

    public Optional<String> checkIfSubjectValidAndEqualsDid(JWTClaimsSet claims) {
        String sub = claims.getSubject();
        if (sub != null && sub.startsWith(DID_FORMAT)) {
            URI id = service.getDidDocument(sub).getId();
            return (id != null && Objects.equals(id.toString(), sub)) ?
                    Optional.empty() : Optional.of("The 'sub' claim must be identical to the id of existing DID document.");
        } else {
            return Optional.of("The 'sub' claim must be in did format.");
        }
    }

    public Optional<String> checkTokenExpiry(JWTClaimsSet claims) {
        Instant now = Instant.now();
        Date expires = claims.getExpirationTime();

        if (expires == null) {
            return Optional.of("Required expiration time 'exp' claim is missing in token");
        } else if (now.isAfter(convertDateToUtcTime(expires))) {
            return Optional.of("Token has expired 'exp'");
        }

        Date issuedAt = claims.getIssueTime();
        if (issuedAt != null) {
            Instant issuedAtInst = convertDateToUtcTime(issuedAt);
            if (issuedAtInst.isAfter(convertDateToUtcTime(expires))) {
                return Optional.of("Issued at 'iat' claim is after expiration time 'exp' claim in token");
            } else if (now.plusSeconds(IAT_LEEWAY).isBefore(issuedAtInst)) {
                return Optional.of("Current date/time is before issued at 'iat' claim in token");
            }
        }
        return Optional.empty();
    }

    private Instant convertDateToUtcTime(Date date) {
        return date.toInstant().atOffset(UTC).toInstant();
    }

    public Optional<String> checkIfAudienceClaimsAreEqual(JWTClaimsSet claimsSI, JWTClaimsSet claimsAT) {
        List<String> audienceSI = claimsSI.getAudience();
        List<String> audienceAccess = claimsAT.getAudience();

        if (audienceSI.isEmpty() || audienceAccess.isEmpty()) {
            return Optional.of("The 'aud' claim must not be empty.");
        } else if (audienceSI.contains(audienceAccess.get(0))) {
            return (audienceAccess.get(0).startsWith(DID_FORMAT)) ?
                    Optional.empty() : Optional.of("The 'aud' claims must have did format.");
        } else {
            return Optional.of("The 'aud' claims must be equal in SI and Access tokens.");
        }
    }

    public Optional<String> checkIfNonceClaimsAreEqual(JWTClaimsSet claimsSI, JWTClaimsSet claimsAT) {
        try {
            String nonceSI = claimsSI.getStringClaim(NONCE);
            String nonceAccess = claimsAT.getStringClaim(NONCE);

            if (nonceSI == null || nonceAccess == null) {
                return Optional.of("The 'nonce' claim must not be empty.");
            } else if (nonceSI.equals(nonceAccess)) {
                return Optional.empty();
            } else {
                return Optional.of("The 'nonce' claims must be equal in SI and Access tokens.");
            }
        } catch (ParseException e) {
            throw new BadDataException("Could not parse 'nonce' claim in token", e);
        }
    }
}
