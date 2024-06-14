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

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.experimental.UtilityClass;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.BadDataException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.SCOPE;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.NONCE;
import static org.springframework.security.oauth2.jwt.JwtClaimNames.JTI;

/**
 * The type Token parsing utils.
 */
@UtilityClass
public class TokenParsingUtils {

    /**
     * The constant PARSING_TOKEN_ERROR.
     */
    public static final String PARSING_TOKEN_ERROR = "Could not parse jwt token";
    /**
     * The constant BEARER_ACCESS_SCOPE.
     */
    public static final String BEARER_ACCESS_SCOPE = "bearer_access_scope";
    /**
     * The constant ACCESS_TOKEN_ERROR.
     */
    public static final String ACCESS_TOKEN_ERROR = "Access token not present";

    /**
     * Gets claims set.
     *
     * @param tokenParsed the token parsed
     * @return the claims set
     */
    public static JWTClaimsSet getClaimsSet(SignedJWT tokenParsed) {
        try {
            return tokenParsed.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new BadDataException(PARSING_TOKEN_ERROR, e);
        }
    }

    /**
     * Parse token signed jwt.
     *
     * @param token the token
     * @return the signed jwt
     */
    public static SignedJWT parseToken(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new BadDataException(PARSING_TOKEN_ERROR, e);
        }
    }

    /**
     * Gets string claim.
     *
     * @param claimsSet the claims set
     * @param name      the name
     * @return the string claim
     */
    public static String getStringClaim(JWTClaimsSet claimsSet, String name) {
        try {
            return claimsSet.getStringClaim(name);
        } catch (ParseException e) {
            throw new BadDataException(PARSING_TOKEN_ERROR, e);
        }
    }

    /**
     * Gets access token.
     *
     * @param claims the claims
     * @return the access token
     */
    public static Optional<String> getAccessToken(JWTClaimsSet claims) {
        try {
            String accessTokenValue = claims.getStringClaim(ACCESS_TOKEN);
            return accessTokenValue == null ? Optional.empty() : Optional.of(accessTokenValue);
        } catch (ParseException e) {
            throw new BadDataException(PARSING_TOKEN_ERROR, e);
        }
    }

    /**
     * Gets access token.
     *
     * @param outerToken the outer token
     * @return the access token
     */
    public static SignedJWT getAccessToken(String outerToken) {
        SignedJWT jwtOuter = parseToken(outerToken);
        JWTClaimsSet claimsSet = getClaimsSet(jwtOuter);
        Optional<String> accessToken = getAccessToken(claimsSet);
        return accessToken.map(TokenParsingUtils::parseToken).orElseThrow(() -> new BadDataException(ACCESS_TOKEN_ERROR));
    }

    /**
     * Gets scope.
     *
     * @param jwtClaimsSet the jwt claims set
     * @return the scope
     */
    public static String getScope(JWTClaimsSet jwtClaimsSet) {
        try {
            String scopes = jwtClaimsSet.getStringClaim(SCOPE);
            if (scopes == null) {
                scopes = jwtClaimsSet.getStringClaim(BEARER_ACCESS_SCOPE);
            }
            return scopes;
        } catch (ParseException e) {
            throw new BadDataException("Token does not contain scope claim");
        }
    }

    /**
     * Gets jti access token.
     *
     * @param accessToken the access token
     * @return the jti access token
     */
    public static String getJtiAccessToken(JWT accessToken) {
        try {
            return getStringClaim(accessToken.getJWTClaimsSet(), JTI);
        } catch (ParseException e) {
            throw new BadDataException(PARSING_TOKEN_ERROR, e);
        }
    }

    /**
     * Gets nonce access token.
     *
     * @param accessToken the access token
     * @return the nonce access token
     */
    public static String getNonceAccessToken(JWT accessToken) {
        try {
            return accessToken.getJWTClaimsSet().getStringClaim(NONCE);
        } catch (ParseException e) {
            throw new BadDataException(PARSING_TOKEN_ERROR, e);
        }
    }

    /**
     * Gets bpn from token.
     *
     * @param authentication the authentication
     * @return the bpn from token
     */
    public static String getBPNFromToken(Authentication authentication) {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        // this will misbehave if we have more then one claims with different case
        // ie. BPN=123456 and bpn=789456
        Map<String, Object> claims = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        claims.putAll(jwt.getClaims());
        return claims.get(StringPool.BPN).toString();
    }
}
