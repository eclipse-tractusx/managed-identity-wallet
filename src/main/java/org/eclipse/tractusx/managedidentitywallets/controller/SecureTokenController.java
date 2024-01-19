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

package org.eclipse.tractusx.managedidentitywallets.controller;

import java.util.Optional;
import java.util.Set;

import org.eclipse.tractusx.managedidentitywallets.UnsupportedGrantTypeException;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.service.SecureTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RestController
@RequiredArgsConstructor
public class SecureTokenController implements TokenApi {

    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final SecureTokenService tokenService;

    @Override
    @SneakyThrows
    public ResponseEntity<StsTokenResponse> token(
            @Valid String audience, @Valid String clientId, @Valid String clientSecret, @Valid String grantType,
            @Valid String accessToken, @Valid String bearerAccessAlias, @Valid String bearerAccessScope) {
        if (!grantType.equals(CLIENT_CREDENTIALS)) {
            throw new UnsupportedGrantTypeException("Selected GrantType is not supported.");
        }

        JWT jwt;
        if (accessToken != null && !accessToken.isBlank()) {
            jwt = tokenService.issueToken(new DID(clientId), new DID(audience), JWTParser.parse(accessToken));
        } else {
            jwt = tokenService.issueToken(new DID(clientId), new DID(audience),
                    Optional.of(bearerAccessScope).map(scopes -> Set.of(scopes.split(" "))).orElse(Set.of()));
        }

        StsTokenResponse response = new StsTokenResponse();
        response.setAccessToken(jwt.toString());
        response.setExpiresIn(jwt.getJWTClaimsSet().getExpirationTime().getTime());
        response.setTokenType(TOKEN_TYPE_BEARER);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(UnsupportedGrantTypeException.class)
    public ResponseEntity<StsTokenErrorResponse> getErrorResponse(RuntimeException e) {
        StsTokenErrorResponse response = new StsTokenErrorResponse();
        response.setError("client_metadata_value_not_supported");
        response.setErrorDescription(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

}
