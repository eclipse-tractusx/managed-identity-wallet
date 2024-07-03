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

import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COLON_SEPARATOR;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
class SecureTokenControllerTest {

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private TestRestTemplate testTemplate;

    private String bpn;

    private String clientId;

    private String clientSecret;

    @BeforeEach
    public void initWallets() {
        // given
        bpn = TestUtils.getRandomBpmNumber();
        String partnerBpn = TestUtils.getRandomBpmNumber();
        clientId = "main";
        clientSecret = "main";
        AuthenticationUtils.setupKeycloakClient(clientId, clientSecret, bpn);
        AuthenticationUtils.setupKeycloakClient("partner", "partner", partnerBpn);
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String didPartner = DidWebFactory.fromHostnameAndPath(miwSettings.host(), partnerBpn).toString();
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        TestUtils.createWallet(bpn, did, testTemplate, miwSettings.authorityWalletBpn(), defaultLocation);
        String defaultLocationPartner = miwSettings.host() + COLON_SEPARATOR + partnerBpn;
        TestUtils.createWallet(partnerBpn, didPartner, testTemplate, miwSettings.authorityWalletBpn(), defaultLocationPartner);
    }

    @Test
    void tokenJSON() {
        // when
        String body = """
                {
                    "audience": "%s",
                    "client_id": "%s",
                    "client_secret": "%s",
                    "grant_type": "client_credentials",
                    "bearer_access_scope": "org.eclipse.tractusx.vc.type:BpnCredential:read"
                }
                """;
        String requestBody = String.format(body, bpn, clientId, clientSecret);
        // then
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON_VALUE));
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = testTemplate.exchange(
                "/api/token",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getOrDefault("access_token", null));
        Assertions.assertNotNull(response.getBody().getOrDefault("expiresAt", null));
    }

    @Test
    void tokenFormUrlencoded() {
        // when
        String body = "audience=%s&client_id=%s&client_secret=%s&grant_type=client_credentials&bearer_access_scope=org.eclipse.tractusx.vc.type:BpnCredential:read";
        String requestBody = String.format(body, bpn, clientId, clientSecret);
        // then
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_FORM_URLENCODED_VALUE));
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = testTemplate.exchange(
                "/api/token",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(response.getHeaders().getContentType(), MediaType.APPLICATION_JSON);
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getOrDefault("access_token", null));
        Assertions.assertNotNull(response.getBody().getOrDefault("expiresAt", null));
    }
}
