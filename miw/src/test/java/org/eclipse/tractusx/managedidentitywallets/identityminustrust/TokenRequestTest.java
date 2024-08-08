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

package org.eclipse.tractusx.managedidentitywallets.identityminustrust;

import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.service.IssuersCredentialService;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.ResourceUtil;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;


@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
public class TokenRequestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PRESENTATION_QUERY_REQUEST = "identityminustrust/messages/presentation_query.json";

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestRestTemplate testTemplate;

    @Autowired
    private IssuersCredentialService issuersCredentialService;

    private String bpn;

    private String clientId;

    private String clientSecret;

    @BeforeEach
    @SneakyThrows
    public void initWallets() {
        // given
        bpn = TestUtils.getRandomBpmNumber();
        String partnerBpn = TestUtils.getRandomBpmNumber();
        clientId = bpn;
        clientSecret = bpn;
        AuthenticationUtils.setupKeycloakClient(clientId, clientSecret, bpn);
        AuthenticationUtils.setupKeycloakClient("partner", "partner", partnerBpn);
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String didPartner = DidWebFactory.fromHostnameAndPath(miwSettings.host(), partnerBpn).toString();
        String defaultLocation = miwSettings.host() + StringPool.COLON_SEPARATOR + bpn;
        TestUtils.createWallet(bpn, did, testTemplate, miwSettings.authorityWalletBpn(), defaultLocation);
        String defaultLocationPartner = miwSettings.host() + StringPool.COLON_SEPARATOR + partnerBpn;
        TestUtils.createWallet(partnerBpn, didPartner, testTemplate, miwSettings.authorityWalletBpn(), defaultLocationPartner);

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
                "        \"holderIdentifier\": \"" + did + "\",\n" +
                "        \"memberOf\":\"Catena-X\",\n" +
                "        \"status\":\"Active\",\n" +
                "        \"startTime656\":\"2021-06-16T18:56:59Z\"\n" +
                "    }\n" +
                "}";

        issuersCredentialService.issueCredentialUsingBaseWallet(
                did,
                MAPPER.readValue(vc, Map.class),
                false, false,
                miwSettings.authorityWalletBpn(), "token"
        );
    }

    @Test
    @SneakyThrows
    public void testPresentationQueryWithToken() {
        // when
        String body = "audience=%s&client_id=%s&client_secret=%s&grant_type=client_credentials&bearer_access_scope=org.eclipse.tractusx.vc.type:MembershipCredential:read";
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

        var jwt = (String) response.getBody().get("access_token");

        final String message2 = ResourceUtil.loadResource(PRESENTATION_QUERY_REQUEST);
        final Map<String, Object> data2 = MAPPER.readValue(message2, Map.class);

        final HttpHeaders headers2 = new HttpHeaders();
        headers2.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        final HttpEntity<Map<String, Object>> entity2 = new HttpEntity<>(data2, headers2);
        var result2 = restTemplate
                .postForEntity(RestURI.API_PRESENTATIONS_IATP, entity2, String.class);

        System.out.println("RESULT:\n" + result2.toString());

        Assertions.assertTrue(result2.getStatusCode().is2xxSuccessful());
    }

}
