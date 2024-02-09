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
import java.util.Optional;

import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_2;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_JSON_STRING_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_JSON_STRING_2;

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
        Optional<String> result = tokenValidationUtils.checkIfIssuerEqualsSubject(claimsSet);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void checkIfIssuerEqualsSubjectFailureTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().issuer(DID_BPN_1).subject(DID_BPN_2).build();
        Optional<String> result = tokenValidationUtils.checkIfIssuerEqualsSubject(claimsSet);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("The 'iss' and 'sub' claims must be non-null and identical.", result.get());
    }

    //checkIfSubjectValidAndEqualsDid
    @Test
    void checkIfSubjectValidAndEqualsDidSuccessTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1).build();
        DidDocument doc = DidDocument.fromJson(DID_JSON_STRING_1);
        Mockito.when(didDocumentService.getDidDocument(DID_BPN_1)).thenReturn(doc);
        Optional<String> result = tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSet);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void checkIfSubjectValidAndEqualsDidFailureWrongDidTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1).build();
        DidDocument doc = DidDocument.fromJson(DID_JSON_STRING_2);
        Mockito.when(didDocumentService.getDidDocument(DID_BPN_1)).thenReturn(doc);
        Optional<String> result = tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSet);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("The 'sub' claim must be identical to the id of existing DID document.", result.get());
    }

    @Test
    void checkIfSubjectValidAndEqualsDidFailureWrongFormatTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("BPNL001").build();
        Optional<String> result = tokenValidationUtils.checkIfSubjectValidAndEqualsDid(claimsSet);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("The 'sub' claim must be in did format.", result.get());
    }

    //checkTokenExpiry
    @Test
    void checkTokenExpirySuccessTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1)
                .expirationTime(new Date(Long.parseLong("2559397136000")))
                .issueTime(new Date(Long.parseLong("1707317488000")))
                .build();
        Optional<String> result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void checkTokenExpiryFailureNoExpClaimTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1).build();
        Optional<String> result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Required expiration time 'exp' claim is missing in token", result.get());
    }

    @Test
    void checkTokenExpiryFailureAlreadyExpiredTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1)
                .expirationTime(new Date(Long.parseLong("1707320002664"))).build();
        Optional<String> result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Token has expired 'exp'", result.get());
    }

    @Test
    void checkTokenExpiryFailureIatIsAfterExpTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1)
                .expirationTime(new Date(Long.parseLong("2527861136000")))
                .issueTime(new Date(Long.parseLong("2559397136000")))
                .build();
        Optional<String> result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Issued at 'iat' claim is after expiration time 'exp' claim in token", result.get());
    }

    @Test
    void checkTokenExpiryFailureIatInTheFutureTest() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(DID_BPN_1)
                .expirationTime(new Date(Long.parseLong("2559397136000")))
                .issueTime(new Date(Long.parseLong("2527861136000")))
                .build();
        Optional<String> result = tokenValidationUtils.checkTokenExpiry(claimsSet);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Current date/time is before issued at 'iat' claim in token", result.get());
    }

    //checkIfAudienceClaimsAreEqual
    @Test
    void checkIfAudienceClaimsAreEqualSuccessTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().audience(List.of(DID_BPN_1, DID_BPN_2)).build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        Optional<String> result = tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSetSI, claimsSetAccess);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void checkIfAudienceClaimsAreEqualFailureNoAudClaimTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().subject(DID_BPN_1).build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        Optional<String> result = tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSetSI, claimsSetAccess);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("The 'aud' claim must not be empty.", result.get());
    }

    @Test
    void checkIfAudienceClaimsAreEqualFailureMismatchTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience(DID_BPN_2).build();
        Optional<String> result = tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSetSI, claimsSetAccess);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("The 'aud' claims must be equal in SI and Access tokens.", result.get());
    }

    @Test
    void checkIfAudienceClaimsAreEqualFailureWrongformatTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().audience("localhost:BPNL001").build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience("localhost:BPNL001").build();
        Optional<String> result = tokenValidationUtils.checkIfAudienceClaimsAreEqual(claimsSetSI, claimsSetAccess);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("The 'aud' claims must have did format.", result.get());
    }

    //checkIfNonceClaimsAreEqual
    @Test
    void checkIfNonceClaimsAreEqualSuccessTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().claim("nonce", "123456").build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().claim("nonce", "123456").build();
        Optional<String> result = tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSetSI, claimsSetAccess);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void checkIfNonceClaimsAreEqualFailureNoNonceClaimTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().audience(DID_BPN_1).build();
        Optional<String> result = tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSetSI, claimsSetAccess);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("The 'nonce' claim must not be empty.", result.get());
    }

    @Test
    void checkIfNonceClaimsAreEqualFailureMismatchTest() {
        JWTClaimsSet claimsSetSI = new JWTClaimsSet.Builder().claim("nonce", "123456").build();
        JWTClaimsSet claimsSetAccess = new JWTClaimsSet.Builder().claim("nonce", "123456789").build();
        Optional<String> result = tokenValidationUtils.checkIfNonceClaimsAreEqual(claimsSetSI, claimsSetAccess);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("The 'nonce' claims must be equal in SI and Access tokens.", result.get());
    }
}
