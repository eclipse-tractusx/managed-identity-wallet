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
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.TokenParsingUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class TokenParsingUtilsTest {

    @Test
    void parseTokenShouldReturnSignedJWTWhenTokenIsValid() throws ParseException {
        String token = "valid.token.here";
        SignedJWT signedJWT = mock(SignedJWT.class);

        try (MockedStatic<SignedJWT> mockedSignedJWT = mockStatic(SignedJWT.class)) {
            mockedSignedJWT.when(() -> SignedJWT.parse(token)).thenReturn(signedJWT);

            SignedJWT result = TokenParsingUtils.parseToken(token);

            assertEquals(signedJWT, result);
        }
    }

    @Test
    void parseTokenShouldThrowBadDataExceptionWhenParseExceptionOccurs() throws ParseException {
        String token = "invalid.token.here";

        try (MockedStatic<SignedJWT> mockedSignedJWT = mockStatic(SignedJWT.class)) {
            mockedSignedJWT.when(() -> SignedJWT.parse(token)).thenThrow(ParseException.class);

            BadDataException exception = assertThrows(BadDataException.class, () -> TokenParsingUtils.parseToken(token));

            assertEquals(TokenParsingUtils.PARSING_TOKEN_ERROR, exception.getMessage());
        }
    }

    @Test
    void getAccessTokenShouldReturnInnerSignedJWTWhenAccessTokenIsPresent() throws ParseException {
        String outerToken = "outer.token.here";
        SignedJWT outerSignedJWT = mock(SignedJWT.class);
        JWTClaimsSet outerClaimsSet = new JWTClaimsSet.Builder().claim("access_token", "inner.token.here").build();
        SignedJWT innerSignedJWT = mock(SignedJWT.class);

        try (MockedStatic<SignedJWT> mockedSignedJWT = mockStatic(SignedJWT.class)) {
            mockedSignedJWT.when(() -> SignedJWT.parse(outerToken)).thenReturn(outerSignedJWT);
            mockedSignedJWT.when(() -> SignedJWT.parse("inner.token.here")).thenReturn(innerSignedJWT);
            when(outerSignedJWT.getJWTClaimsSet()).thenReturn(outerClaimsSet);

            SignedJWT result = TokenParsingUtils.getAccessToken(outerToken);

            assertEquals(innerSignedJWT, result);
        }
    }

    @Test
    void getAccessTokenShouldThrowBadDataExceptionWhenAccessTokenIsNotPresent() throws ParseException {
        String outerToken = "outer.token.here";
        SignedJWT outerSignedJWT = mock(SignedJWT.class);
        JWTClaimsSet outerClaimsSet = new JWTClaimsSet.Builder().build();

        try (MockedStatic<SignedJWT> mockedSignedJWT = mockStatic(SignedJWT.class)) {
            mockedSignedJWT.when(() -> SignedJWT.parse(outerToken)).thenReturn(outerSignedJWT);
            when(outerSignedJWT.getJWTClaimsSet()).thenReturn(outerClaimsSet);

            BadDataException exception = assertThrows(BadDataException.class, () -> TokenParsingUtils.getAccessToken(outerToken));

            assertEquals(TokenParsingUtils.ACCESS_TOKEN_ERROR, exception.getMessage());
        }
    }

    @Test
    void getBPNFromTokenShouldReturnBPNWhenBPNClaimIsPresent() {
        Authentication authentication = mock(JwtAuthenticationToken.class);
        Jwt jwt = mock(Jwt.class);
        when(((JwtAuthenticationToken) authentication).getToken()).thenReturn(jwt);
        Map<String, Object> claims = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        claims.put(StringPool.BPN, "123456");
        when(jwt.getClaims()).thenReturn(claims);

        String result = TokenParsingUtils.getBPNFromToken(authentication);

        assertEquals("123456", result);
    }

    // Other test methods for TokenParsingUtils...

    @Test
    void getStringClaimShouldReturnClaimValueWhenClaimIsPresent() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim("claim")).thenReturn("value");

        String result = TokenParsingUtils.getStringClaim(claimsSet, "claim");

        assertEquals("value", result);
    }

    @Test
    void getStringClaimShouldThrowBadDataExceptionWhenParseExceptionOccurs() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim("claim")).thenThrow(ParseException.class);

        BadDataException exception = assertThrows(BadDataException.class, () -> TokenParsingUtils.getStringClaim(claimsSet, "claim"));

        assertEquals(TokenParsingUtils.PARSING_TOKEN_ERROR, exception.getMessage());
    }

    @Test
    void getAccessTokenShouldReturnAccessTokenWhenAccessTokenIsPresent() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim("access_token")).thenReturn("accessToken");

        Optional<String> result = TokenParsingUtils.getAccessToken(claimsSet);

        assertTrue(result.isPresent());
        assertEquals("accessToken", result.get());
    }

    @Test
    void getAccessTokenShouldReturnEmptyOptionalWhenAccessTokenIsNotPresent() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim("access_token")).thenReturn(null);

        Optional<String> result = TokenParsingUtils.getAccessToken(claimsSet);

        assertFalse(result.isPresent());
    }

    @Test
    void getAccessTokenShouldThrowBadDataExceptionWhenParseExceptionOccurs() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim("access_token")).thenThrow(ParseException.class);

        BadDataException exception = assertThrows(BadDataException.class, () -> TokenParsingUtils.getAccessToken(claimsSet));

        assertEquals(TokenParsingUtils.PARSING_TOKEN_ERROR, exception.getMessage());
    }

    @Test
    void getScopeShouldReturnScopeWhenScopeIsPresent() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim("scope")).thenReturn("scope1 scope2");

        String result = TokenParsingUtils.getScope(claimsSet);

        assertEquals("scope1 scope2", result);
    }

    @Test
    void getScopeShouldReturnBearerAccessScopeWhenScopeIsNotPresentButBearerAccessScopeIs() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim("scope")).thenReturn(null);
        when(claimsSet.getStringClaim(TokenParsingUtils.BEARER_ACCESS_SCOPE)).thenReturn("bearerAccessScope");

        String result = TokenParsingUtils.getScope(claimsSet);

        assertEquals("bearerAccessScope", result);
    }

    @Test
    void getScopeShouldThrowBadDataExceptionWhenParseExceptionOccurs() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim("scope")).thenThrow(ParseException.class);

        BadDataException exception = assertThrows(BadDataException.class, () -> TokenParsingUtils.getScope(claimsSet));

        assertEquals("Token does not contain scope claim", exception.getMessage());
    }

    @Test
    void getJtiAccessTokenShouldReturnJtiWhenClaimIsPresent() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim(JwtClaimNames.JTI)).thenReturn("jtiValue");
        SignedJWT signedJWT = mock(SignedJWT.class);
        when(signedJWT.getJWTClaimsSet()).thenReturn(claimsSet);

        String result = TokenParsingUtils.getJtiAccessToken(signedJWT);

        assertEquals("jtiValue", result);
    }

    @Test
    void getJtiAccessTokenShouldThrowBadDataExceptionWhenParseExceptionOccurs() throws ParseException {
        JWTClaimsSet claimsSet = mock(JWTClaimsSet.class);
        when(claimsSet.getStringClaim(JwtClaimNames.JTI)).thenThrow(ParseException.class);
        SignedJWT signedJWT = mock(SignedJWT.class);
        when(signedJWT.getJWTClaimsSet()).thenReturn(claimsSet);

        BadDataException exception = assertThrows(BadDataException.class, () -> TokenParsingUtils.getJtiAccessToken(signedJWT));

        assertEquals(TokenParsingUtils.PARSING_TOKEN_ERROR, exception.getMessage());
    }

}
