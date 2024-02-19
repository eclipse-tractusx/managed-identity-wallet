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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.config.security.SecurityConfigProperties;
import org.eclipse.tractusx.managedidentitywallets.domain.IdpTokenResponse;
import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.UnsupportedGrantTypeException;
import org.eclipse.tractusx.managedidentitywallets.exception.InvalidIdpTokenResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.CLIENT_CREDENTIALS;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.CLIENT_ID;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.CLIENT_SECRET;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.GRANT_TYPE;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.SCOPE;
import static org.springframework.security.oauth2.core.oidc.OidcScopes.OPENID;

@Service
@Slf4j
public class IdpAuthorization {

    private final RestTemplate rest;

    @Autowired
    public IdpAuthorization(final SecurityConfigProperties properties, final RestTemplateBuilder restTemplateBuilder) {
        String url = UriComponentsBuilder.fromUriString(properties.authServerUrl())
                .pathSegment("realms", properties.realm())
                .build()
                .toString();
        this.rest = restTemplateBuilder
                .rootUri(url)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    public IdpTokenResponse fromSecureTokenRequest(SecureTokenRequest secureTokenRequest) throws UnsupportedGrantTypeException, InvalidIdpTokenResponseException {
        // we're ignoring the input, but the protocol requires us to check.
        if (!secureTokenRequest.getGrantType().equals(CLIENT_CREDENTIALS)) {
            throw new UnsupportedGrantTypeException("The provided 'grant_type' is not valid. Use 'client_credentials'.");
        }
        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add(GRANT_TYPE, CLIENT_CREDENTIALS);
        tokenRequest.add(SCOPE, OPENID);
        tokenRequest.add(CLIENT_ID, secureTokenRequest.getClientId());
        tokenRequest.add(CLIENT_SECRET, secureTokenRequest.getClientSecret());
        log.debug("Doing OAuth token request for '{}' during secure token request flow.", secureTokenRequest.getClientId());
        IdpTokenResponse idpResponse = rest.postForObject(
                "/protocol/openid-connect/token",
                tokenRequest,
                IdpTokenResponse.class
        );
        if (idpResponse == null) {
            throw new InvalidIdpTokenResponseException("The idp response cannot be null. Possible causes for this are: the 'clientId' is invalid, or the 'client' is not enabled.");
        }
        return idpResponse;
    }
}
