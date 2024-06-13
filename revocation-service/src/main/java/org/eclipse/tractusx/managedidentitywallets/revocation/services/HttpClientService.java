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

package org.eclipse.tractusx.managedidentitywallets.revocation.services;

import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.revocation.config.security.SecurityConfigProperties;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.TokenResponse;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class HttpClientService {

    private final RestClient restClient = RestClient.create();

    private final SecurityConfigProperties securityConfigProperties;

    @Value("${revocation.domain.url}")
    public String domainUrl;

    @Value("${revocation.miw.url}")
    private String miwUrl;

    public String getBearerToken() {
        MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
        data.add("client_id", securityConfigProperties.publicClientId());
        data.add("client_secret", securityConfigProperties.clientId());
        data.add("grant_type", "client_credentials");
        var result =
                restClient
                        .post()
                        .uri(securityConfigProperties.tokenUrl())
                        .accept(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(data)
                        .retrieve();
        TokenResponse tokenResponse = result.toEntity(TokenResponse.class).getBody();
        if (tokenResponse == null) {
            return null;
        }
        return tokenResponse.getAccessToken();

    }

    public VerifiableCredential signStatusListVC(VerifiableCredential vc, String token) {
        String uri =
                UriComponentsBuilder.fromHttpUrl(miwUrl)
                        .path("/api/credentials")
                        .queryParam("isRevocable", "false")
                        .build()
                        .toUriString();

        var result =
                restClient
                        .post()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .body(vc.toJson())
                        .retrieve();
        return result.toEntity(VerifiableCredential.class).getBody();
    }
}
