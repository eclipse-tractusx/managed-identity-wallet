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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.constant.TokenValidationErrors;
import org.eclipse.tractusx.managedidentitywallets.dto.ValidationResult;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.utils.CustomSignedJWTVerifier;
import org.eclipse.tractusx.managedidentitywallets.utils.TokenValidationUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.eclipse.tractusx.managedidentitywallets.utils.TokenParsingUtils.getAccessToken;
import static org.eclipse.tractusx.managedidentitywallets.utils.TokenParsingUtils.getClaimsSet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TokenParsingUtils.parseToken;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.NONCE;

@Service
@Slf4j
@RequiredArgsConstructor
public class STSTokenValidationService {

    private final DidDocumentResolverService didDocumentResolverService;
    private final CustomSignedJWTVerifier customSignedJWTverifier;
    private final TokenValidationUtils tokenValidationUtils;

    /**
     * Validates SI token and Access token.
     *
     * @param token token in a String format
     * @return boolean result of validation
     */
    public ValidationResult validateToken(String token) {
        List<ValidationResult> validationResults = new ArrayList<>();

        SignedJWT jwtSI = parseToken(token);
        JWTClaimsSet claimsSI = getClaimsSet(jwtSI);

        validationResults.add(tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSI));
        validationResults.add(tokenValidationUtils.checkIfIssuerEqualsSubject(claimsSI));
        validationResults.add(tokenValidationUtils.checkTokenExpiry(claimsSI));

        Optional<String> accessToken = getAccessToken(claimsSI);
        if (accessToken.isPresent()) {
            SignedJWT jwtAT = parseToken(accessToken.get());
            JWTClaimsSet claimsAT = getClaimsSet(jwtAT);

            validationResults.add(tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSI.getAudience(), claimsAT.getAudience()));
            try {
                validationResults.add(tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSI.getStringClaim(NONCE),
                        claimsAT.getStringClaim(NONCE)));
            } catch (ParseException e) {
                throw new BadDataException("Could not parse 'nonce' claim in token", e);
            }

            String didForOuter = claimsAT.getAudience().get(0);
            validationResults.add(verifySignature(didForOuter, jwtSI));

            String didForInner = claimsAT.getIssuer();
            validationResults.add(verifySignature(didForInner, jwtAT));
        } else {
            validationResults.add(tokenValidationUtils.getInvalidResult(TokenValidationErrors.ACCESS_TOKEN_MISSING));
        }
        return combineValidationResults(validationResults);
    }

    private ValidationResult verifySignature(String did, SignedJWT signedJWT) {
        try {
            customSignedJWTverifier.setDidResolver(didDocumentResolverService.getCompositeDidResolver());
            return customSignedJWTverifier.verify(did, signedJWT)
                    ? tokenValidationUtils.getValidResult()
                    : tokenValidationUtils.getInvalidResult(TokenValidationErrors.SIGNATURE_NOT_VERIFIED);
        } catch (JOSEException ex) {
            throw new BadDataException("Could not verify signature of jwt", ex);
        }
    }

    private ValidationResult combineValidationResults(List<ValidationResult> validationResults) {
        List<TokenValidationErrors> errorsList = new ArrayList<>();
        for (ValidationResult result : validationResults) {
            List<TokenValidationErrors> errors = result.getErrors();
            if (null != errors) {
                errorsList.add(errors.get(0));
            }
        }
        ValidationResult finalResult = ValidationResult.builder().errors(errorsList).build();
        finalResult.setValid(errorsList.isEmpty());
        return finalResult;
    }
}
