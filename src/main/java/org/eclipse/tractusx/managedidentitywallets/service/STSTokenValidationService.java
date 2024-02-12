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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.utils.CustomSignedJWTVerifier;
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

    private final DidDocumentResolverService didDocumentResolverService;
    private final CustomSignedJWTVerifier customSignedJWTverifier;
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
        SignedJWT jwtSI = parseToken(token);
        JWTClaimsSet claimsSI = getClaimsSet(jwtSI);

        tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSI).ifPresent(errors::add);
        tokenValidationUtils.checkIfIssuerEqualsSubject(claimsSI).ifPresent(errors::add);
        tokenValidationUtils.checkTokenExpiry(claimsSI).ifPresent(errors::add);

        Optional<String> accessToken = getAccessToken(claimsSI);
        if (accessToken.isPresent()) {
            SignedJWT jwtAT = parseToken(accessToken.get());
            JWTClaimsSet claimsAT = getClaimsSet(jwtAT);

            tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSI, claimsAT).ifPresent(errors::add);
            tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSI, claimsAT).ifPresent(errors::add);

            String didForOuter = claimsAT.getAudience().get(0);
            verifySignature(didForOuter, jwtSI).ifPresent(errors::add);

            String didForInner = claimsAT.getIssuer();
            verifySignature(didForInner, jwtAT).ifPresent(errors::add);
        } else {
            errors.add("The '%s' claim must not be null.".formatted(ACCESS_TOKEN));
        }

        if (errors.isEmpty()) {
            return true;
        } else {
            log.debug(errors.toString());
            return false;
        }
    }

    private JWTClaimsSet getClaimsSet(SignedJWT tokenParsed) {
        try {
            return tokenParsed.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new BadDataException("Could not parse jwt token", e);
        }
    }

    private SignedJWT parseToken(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new BadDataException("Could not parse jwt token", e);
        }
    }

    private Optional<String> getAccessToken(JWTClaimsSet claims) {
        try {
            String accessTokenValue = claims.getStringClaim(ACCESS_TOKEN);
            return accessTokenValue == null ? Optional.empty() : Optional.of(accessTokenValue);
        } catch (ParseException e) {
            throw new BadDataException("Could not parse jwt token", e);
        }
    }

    private Optional<String> verifySignature(String did, SignedJWT signedJWT) {
        try {
            customSignedJWTverifier.setDidResolver(didDocumentResolverService.getCompositeDidResolver());
            return customSignedJWTverifier.verify(did, signedJWT) ? Optional.empty()
                    : Optional.of("Signature of jwt is not verified");
        } catch (JOSEException ex) {
            throw new BadDataException("Can not verify signature of jwt", ex);
        }
    }
}
