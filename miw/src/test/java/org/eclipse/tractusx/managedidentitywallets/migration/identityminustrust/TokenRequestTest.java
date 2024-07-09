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

package org.eclipse.tractusx.managedidentitywallets.migration.identityminustrust;

import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.controller.SecureTokenController;
import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequest;
import org.eclipse.tractusx.managedidentitywallets.service.IssuersCredentialService;
import org.eclipse.tractusx.managedidentitywallets.service.WalletService;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.ResourceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;


@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
public class TokenRequestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TOKEN_REQUEST_WITH_SCOPE_REQUEST = "identityminustrust/messages/token_request_with_scope.json";
    private static final String TOKEN_REQUEST_WITH_TOKEN_REQUEST = "identityminustrust/messages/token_request_with_token.json";
    private static final String PRESENTATION_QUERY_REQUEST = "identityminustrust/messages/presentation_query.json";

    @Autowired
    private IssuersCredentialService issuersCredentialService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private TestRestTemplate restTemplate;

    @SneakyThrows
    @BeforeEach
    public void setup() {
        walletService.createAuthorityWallet();

        var vc = "{\n" +
                "    \"id\": \"did:web:foo#f255c392-82aa-483a-90a3-3c8697cd246a\",\n" +
                "    \"@context\": [\n" +
                "        \"https://www.w3.org/2018/credentials/v1\",\n" +
                "        \"https://w3id.org/security/suites/jws-2020/v1\"\n" +
                "    ],\n" +
                "    \"type\": [\"VerifiableCredential\", \"MembershipCredential\"],\n" +
                "    \"issuanceDate\": \"2021-06-16T18:56:59Z\",\n" +
                "    \"expirationDate\": \"2022-06-16T18:56:59Z\",\n" +
                "    \"issuer\": \"" + miwSettings.authorityWalletDid() + "\",\n" +
                "    \"credentialSubject\": {\n" +
                "        \"type\":\"MembershipCredential\",\n" +
                "        \"holderIdentifier\": \"" + miwSettings.authorityWalletDid() + "\",\n" +
                "        \"memberOf\":\"Catena-X\",\n" +
                "        \"status\":\"Active\",\n" +
                "        \"startTime656\":\"2021-06-16T18:56:59Z\"\n" +
                "    }\n" +
                "}";

        issuersCredentialService.issueCredentialUsingBaseWallet(
                miwSettings.authorityWalletDid(),
                MAPPER.readValue(vc, Map.class),
                false,
                miwSettings.authorityWalletBpn()
        );

    }

    @Test
    @SneakyThrows
    public void testTokenRequestWithToken() {

        final String message = getMessage(TOKEN_REQUEST_WITH_TOKEN_REQUEST);
        final SecureTokenRequest data = MAPPER.readValue(message, SecureTokenRequest.class);

        final HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());
        final HttpEntity<SecureTokenRequest> entity = new HttpEntity<>(data, headers);
        var result = restTemplate
                .postForEntity(SecureTokenController.BASE_PATH, entity, String.class);

        System.out.println("Received Response: " + result.toString());

        Assertions.assertTrue(result.getStatusCode().is2xxSuccessful(), "Status code is not 2xx");
        Assertions.assertNotNull(MAPPER.readValue(result.getBody(), Map.class).get("jwt"), "JWT is null");
    }

    @Test
    @SneakyThrows
    public void testTokenRequestWithScope() {

        final String message = getMessage(TOKEN_REQUEST_WITH_SCOPE_REQUEST);
        final SecureTokenRequest data = MAPPER.readValue(message, SecureTokenRequest.class);

        final HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());
        final HttpEntity<SecureTokenRequest> entity = new HttpEntity<>(data, headers);
        var result = restTemplate
                .postForEntity(SecureTokenController.BASE_PATH, entity, String.class);

        System.out.println("Received Response: " + result.toString());

        Assertions.assertTrue(result.getStatusCode().is2xxSuccessful(), "Status code is not 2xx");
        Assertions.assertNotNull(MAPPER.readValue(result.getBody(), Map.class).get("jwt"), "JWT is null");
    }

    @Test
    @SneakyThrows
    public void testPresentationQueryWithToken() {

        final String message = getMessage(TOKEN_REQUEST_WITH_SCOPE_REQUEST);
        final SecureTokenRequest data = MAPPER.readValue(message, SecureTokenRequest.class);

        // set audience to used wallet
        var correctAudience = "did:web:localhost%3A" + TestContextInitializer.port + ":BPNL000000000000";
        data.getSecureTokenRequestScope().get().setProviderDid(correctAudience);
        data.getSecureTokenRequestScope().get().setConsumerDid(correctAudience);

        final HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());
        final HttpEntity<SecureTokenRequest> entity = new HttpEntity<>(data, headers);
        var result = restTemplate
                .postForEntity(SecureTokenController.BASE_PATH, entity, String.class);

        System.out.println("Received Response: " + result.toString());

        var jwt = (String) MAPPER.readValue(result.getBody(), Map.class).get("jwt");
        Assertions.assertTrue(result.getStatusCode().is2xxSuccessful(), "Status code is not 2xx");
        Assertions.assertNotNull(jwt, "JWT is null");

        final String message2 = getMessage(PRESENTATION_QUERY_REQUEST);
        final Map<String, Object> data2 = MAPPER.readValue(message2, Map.class);

        final HttpHeaders headers2 = new HttpHeaders();
        headers2.set(HttpHeaders.AUTHORIZATION, jwt);
        final HttpEntity<Map<String, Object>> entity2 = new HttpEntity<>(data2, headers2);
        var result2 = restTemplate
                .postForEntity(RestURI.API_PRESENTATIONS_IATP, entity2, String.class);

        System.out.println("RESULT:\n" + result2.toString());
    }

    private String getMessage(String resourceName) {
        return ResourceUtil.loadResource(resourceName);
    }
}
