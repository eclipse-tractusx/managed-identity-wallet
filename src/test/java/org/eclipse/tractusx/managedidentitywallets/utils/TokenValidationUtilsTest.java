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
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.constant.TokenValidationErrors;
import org.eclipse.tractusx.managedidentitywallets.dto.ValidationResult;
import org.eclipse.tractusx.managedidentitywallets.service.DidDocumentService;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_2;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_JSON_STRING_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_JSON_STRING_2;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.NONCE;

@ExtendWith(MockitoExtension.class)
class TokenValidationUtilsTest {

    @Mock
    DidDocumentService didDocumentService;

    @InjectMocks
    private TokenValidationUtils tokenValidationUtils;

    //checkIfIssuerEqualsSubject
    @Test
    void checkIfIssuerEqualsSubjectSuccessTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().issuer(DID_BPN_1).subject(DID_BPN_1).build();
        ValidationResult result = tokenValidationUtils.checkIfIssuerEqualsSubject(claimsSet);
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void checkIfIssuerEqualsSubjectFailureTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().issuer(DID_BPN_1).subject(DID_BPN_2).build();
        ValidationResult result = tokenValidationUtils.checkIfIssuerEqualsSubject(claimsSet);
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.ISS_AND_SUB_NOT_EQUAL, result.getErrors().get(0));
    }

    //checkIfSubjectValidAndEqualsDid
    @Test
    void checkIfSubjectValidAndEqualsDidSuccessTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1).build();
        DidDocument doc = DidDocument.fromJson(DID_JSON_STRING_1);
        Mockito.when(didDocumentService.getDidDocument(DID_BPN_1)).thenReturn(doc);
        ValidationResult result = tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSet);
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void checkIfSubjectValidAndEqualsDidFailureWrongDidTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1).build();
        DidDocument doc = DidDocument.fromJson(DID_JSON_STRING_2);
        Mockito.when(didDocumentService.getDidDocument(DID_BPN_1)).thenReturn(doc);
        ValidationResult result = tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSet);
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.SUB_NOT_MATCH_ANY_DID, result.getErrors().get(0));
    }

    @Test
    void checkIfSubjectValidAndEqualsDidFailureWrongFormatTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("BPNL001").build();
        ValidationResult result = tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSet);
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.SUB_NOT_DID, result.getErrors().get(0));
    }

    //checkTokenExpiry
    @Test
    void checkTokenExpirySuccessTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1)
                .expirationTime(new Date(Long.parseLong("2559397136000")))
                .issueTime(new Date(Long.parseLong("1707317488000")))
                .build();
        ValidationResult result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void checkTokenExpiryFailureNoExpClaimTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1).build();
        ValidationResult result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.EXP_MISSING, result.getErrors().get(0));
    }

    @Test
    void checkTokenExpiryFailureAlreadyExpiredTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1)
                .expirationTime(new Date(Long.parseLong("1707320002664"))).build();
        ValidationResult result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.TOKEN_ALREADY_EXPIRED, result.getErrors().get(0));
    }

    @Test
    void checkTokenExpiryFailureIatIsAfterExpTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1)
                .expirationTime(new Date(Long.parseLong("2527861136000")))
                .issueTime(new Date(Long.parseLong("2559397136000")))
                .build();
        ValidationResult result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.IAT_AFTER_EXPIRATION, result.getErrors().get(0));
    }

    @Test
    void checkTokenExpiryFailureIatInTheFutureTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1)
                .expirationTime(new Date(Long.parseLong("2559397136000")))
                .issueTime(new Date(Long.parseLong("2527861136000")))
                .build();
        ValidationResult result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.CURRENT_TIME_BEFORE_IAT, result.getErrors().get(0));
    }

    //checkIfAudienceClaimsAreEqual
    @Test
    void checkIfAudienceClaimsAreEqualSuccessTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().audience(List.of(DID_BPN_1, DID_BPN_2)).build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        ValidationResult result = tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSetSI.getAudience(),
                claimsSetAccess.getAudience());
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void checkIfAudienceClaimsAreEqualFailureNoAudClaimTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().subject(DID_BPN_1).build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        ValidationResult result = tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSetSI.getAudience(),
                claimsSetAccess.getAudience());
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.AUD_MISSING, result.getErrors().get(0));
    }

    @Test
    void checkIfAudienceClaimsAreEqualFailureMismatchTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience(DID_BPN_2).build();
        ValidationResult result = tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSetSI.getAudience(),
                claimsSetAccess.getAudience());
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.AUD_CLAIMS_NOT_EQUAL, result.getErrors().get(0));
    }

    @Test
    void checkIfAudienceClaimsAreEqualFailureWrongFormatTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().audience("localhost:BPNL001").build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience("localhost:BPNL001").build();
        ValidationResult result = tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSetSI.getAudience(),
                claimsSetAccess.getAudience());
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.AUD_NOT_DID, result.getErrors().get(0));
    }

    //checkIfNonceClaimsAreEqual
    @SneakyThrows
    @Test
    void checkIfNonceClaimsAreEqualSuccessTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().claim("nonce", "123456").build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().claim("nonce", "123456").build();
        ValidationResult result = tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSetSI.getStringClaim(NONCE),
                claimsSetAccess.getStringClaim(NONCE));
        Assertions.assertTrue(result.isValid());
    }

    @SneakyThrows
    @Test
    void checkIfNonceClaimsAreEqualFailureNoNonceClaimTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        ValidationResult result = tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSetSI.getStringClaim(NONCE),
                claimsSetAccess.getStringClaim(NONCE));
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.NONCE_MISSING, result.getErrors().get(0));
    }

    @SneakyThrows
    @Test
    void checkIfNonceClaimsAreEqualFailureMismatchTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().claim("nonce", "123456").build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().claim("nonce", "123456789").build();
        ValidationResult result = tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSetSI.getStringClaim(NONCE),
                claimsSetAccess.getStringClaim(NONCE));
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.NONCE_CLAIMS_NOT_EQUAL, result.getErrors().get(0));
    }
}
