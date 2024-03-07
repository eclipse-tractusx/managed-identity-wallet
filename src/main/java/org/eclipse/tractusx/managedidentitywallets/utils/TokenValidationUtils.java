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
import org.eclipse.tractusx.managedidentitywallets.constant.TokenValidationErrors;
import org.eclipse.tractusx.managedidentitywallets.dto.ValidationResult;
import org.eclipse.tractusx.managedidentitywallets.service.DidDocumentService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.time.ZoneOffset.UTC;

/**
 * Contains methods for validating token claims.
 */
@Component
@RequiredArgsConstructor
public class TokenValidationUtils {

    private final DidDocumentService service;

    public static final String DID_FORMAT = "did:";
    private static final int IAT_LEEWAY = 5;

    public ValidationResult checkIfIssuerEqualsSubject(JWTClaimsSet claims) {
        String iss = claims.getIssuer();
        String sub = claims.getSubject();
        return (iss != null && Objects.equals(iss, sub)) ?
                getValidResult() : getInvalidResult(TokenValidationErrors.ISS_AND_SUB_NOT_EQUAL);
    }

    public ValidationResult getValidResult() {
        return ValidationResult.builder().isValid(true).build();
    }

    public ValidationResult getInvalidResult(TokenValidationErrors error) {
        return ValidationResult.builder().isValid(false).errors(List.of(error)).build();
    }

    public ValidationResult checkIfSubjectValidAndEqualsDid(JWTClaimsSet claims) {
        String sub = claims.getSubject();
        return checkIfSubPresent(sub)
                ? checkIfDidPresent(sub)
                ? getValidResult()
                : getInvalidResult(TokenValidationErrors.SUB_NOT_MATCH_ANY_DID)
                : getInvalidResult(TokenValidationErrors.SUB_NOT_DID);
    }

    private boolean checkIfSubPresent(String sub) {
        return sub != null && sub.startsWith(DID_FORMAT);
    }

    private boolean checkIfDidPresent(String sub) {
        URI id = service.getDidDocument(sub).getId();
        return id != null && Objects.equals(id.toString(), sub);
    }

    public ValidationResult checkTokenExpiry(JWTClaimsSet claims) {
        return !checkIfExpirationIsPresent(claims.getExpirationTime())
                ? getInvalidResult(TokenValidationErrors.EXP_MISSING)
                : checkIfTokenIsExpired(claims.getExpirationTime())
                ? getInvalidResult(TokenValidationErrors.TOKEN_ALREADY_EXPIRED)
                : checkIssAt(claims);
    }

    private boolean checkIfExpirationIsPresent(Date expirationTime) {
        return null != expirationTime;
    }

    private boolean checkIfTokenIsExpired(Date expirationTime) {
        return Instant.now().isAfter(convertDateToUtcTime(expirationTime));
    }

    private Instant convertDateToUtcTime(Date date) {
        return date.toInstant().atOffset(UTC).toInstant();
    }

    private ValidationResult checkIssAt(JWTClaimsSet claimsSet) {
        return !checkIfIssuedAtIsPresent(claimsSet.getIssueTime())
                ? getInvalidResult(TokenValidationErrors.IAT_MISSING)
                : checkIfIssuedAtIsAfterExpires(claimsSet)
                ? getInvalidResult(TokenValidationErrors.IAT_AFTER_EXPIRATION)
                : checkIssuedAtIsAfterCurrentDateTime(claimsSet.getIssueTime())
                ? getInvalidResult(TokenValidationErrors.CURRENT_TIME_BEFORE_IAT)
                : getValidResult();
    }

    private boolean checkIfIssuedAtIsPresent(Date issueTime) {
        return null != issueTime;
    }

    private boolean checkIfIssuedAtIsAfterExpires(JWTClaimsSet claims) {
        Date expires = claims.getExpirationTime();
        Date issuedAt = claims.getIssueTime();
        Instant issuedAtInst = convertDateToUtcTime(issuedAt);
        return issuedAtInst.isAfter(convertDateToUtcTime(expires));
    }

    private boolean checkIssuedAtIsAfterCurrentDateTime(Date issuedAt) {
        Instant issuedAtInst = convertDateToUtcTime(issuedAt);
        Instant now = Instant.now();
        return now.plusSeconds(IAT_LEEWAY).isBefore(issuedAtInst);
    }

    public ValidationResult checkIfAudienceClaimsAreEqual(List<String> audienceSI, List<String> audienceAccess) {
        return checkIfAudsAreMissing(audienceSI, audienceAccess)
                ? getInvalidResult(TokenValidationErrors.AUD_MISSING)
                : checkAudEquality(audienceSI, audienceAccess)
                ? checkAudFormat(audienceAccess)
                ? getValidResult()
                : getInvalidResult(TokenValidationErrors.AUD_NOT_DID)
                : getInvalidResult(TokenValidationErrors.AUD_CLAIMS_NOT_EQUAL);
    }

    private boolean checkAudFormat(List<String> audienceAccess) {
        return audienceAccess.get(0).startsWith(DID_FORMAT);
    }

    private boolean checkAudEquality(List<String> audienceSI, List<String> audienceAccess) {
        return audienceSI.contains(audienceAccess.get(0));
    }

    private boolean checkIfAudsAreMissing(List<String> audienceSI, List<String> audienceAccess) {
        return audienceSI.isEmpty() || audienceAccess.isEmpty();
    }

    public ValidationResult checkIfNonceClaimsAreEqual(String nonceSI, String nonceAccess) {
        return checkIfNoncesAreMissing(nonceSI, nonceAccess)
                ? getInvalidResult(TokenValidationErrors.NONCE_MISSING)
                : nonceSI.equals(nonceAccess)
                ? getValidResult()
                : getInvalidResult(TokenValidationErrors.NONCE_CLAIMS_NOT_EQUAL);
    }

    private boolean checkIfNoncesAreMissing(String nonceSI, String nonceAccess) {
        return nonceSI == null || nonceAccess == null;
    }
}
