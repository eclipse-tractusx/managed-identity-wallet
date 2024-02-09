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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.utils.TokenValidationUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class STSTokenValidationService {

    private final TokenValidationUtils tokenValidationUtils;
    private static final String ACCESS_TOKEN = "access_token";

    /**
     * Validates SI token and Access token.
     *
     * @param token token in a String format
     * @return boolean result of validation
     */
    public boolean validateToken(String token) {
        List<String> errors = new ArrayList<>();

        JWTClaimsSet claimsSI = getClaimsSet(token);

        tokenValidationUtils.checkIfIssuerEqualsSubject(claimsSI).ifPresent(errors::add);
        tokenValidationUtils.checkTokenExpiry(claimsSI).ifPresent(errors::add);
        tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSI).ifPresent(errors::add);

        Optional<String> accessToken = getAccessToken(claimsSI);
        if (accessToken.isPresent()) {
            String accessTokenValue = accessToken.get();
            JWTClaimsSet claimsAT = getClaimsSet(accessTokenValue);
            tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSI, claimsAT).ifPresent(errors::add);
            tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSI, claimsAT).ifPresent(errors::add);
        } else {
            errors.add("The '%s' claim must not be null.".formatted(ACCESS_TOKEN));
        }

        if (errors.isEmpty()) {
            return true;
        } else {
            log.error(errors.toString());
            return false;
        }
    }

    /**
     * Parses the token and gets claim set from it.
     *
     * @param token token in a String format
     * @return the set of JWT claims
     */
    private JWTClaimsSet getClaimsSet(String token) {
        try {
            SignedJWT tokenParsed = SignedJWT.parse(token);
            return tokenParsed.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new BadDataException("Could not parse jwt token", e);
        }
    }

    /**
     * Gets access token from SI token.
     *
     * @param claims set of claims of SI token
     * @return the value of token
     */
    private Optional<String> getAccessToken(JWTClaimsSet claims) {
        try {
            String accessTokenValue = claims.getStringClaim(ACCESS_TOKEN);
            return accessTokenValue == null ? Optional.empty() : Optional.of(accessTokenValue);
        } catch (ParseException e) {
            throw new BadDataException("Could not parse jwt token", e);
        }
    }
}
