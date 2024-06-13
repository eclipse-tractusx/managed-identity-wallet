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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.tractusx.managedidentitywallets.revocation.config.security.SecurityConfigProperties;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.TokenResponse;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.mockEmptyEncodedList;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.mockStatusListCredential;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.mockStatusListVC;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class HttpClientServiceTest {

    @RegisterExtension
    static WireMockExtension wm1 =
            WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static SecurityConfigProperties securityConfigProperties;

    private static HttpClientService httpClientService; // The service to test

    @Mock
    private RestClient.Builder webClientBuilder; // Assuming RestClient is using WebClient.Builder

    @Mock
    private RestClient webClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @BeforeAll
    public static void beforeAll() {
        securityConfigProperties = Mockito.mock(SecurityConfigProperties.class);
        when(securityConfigProperties.publicClientId()).thenReturn("public-client-id");
        when(securityConfigProperties.clientId()).thenReturn("client-id");
        when(securityConfigProperties.tokenUrl()).thenReturn(wm1.baseUrl() + "/token");
        httpClientService = new HttpClientService(securityConfigProperties);
        ReflectionTestUtils.setField(httpClientService, "miwUrl", wm1.baseUrl());
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetBearerToken_Success() {
        String expectedToken = "mockToken";
        TokenResponse mockTokenResponse = new TokenResponse();
        mockTokenResponse.setAccessToken(expectedToken);
        wm1.stubFor(post("/token").willReturn(jsonResponse(mockTokenResponse, 200)));
        String token = httpClientService.getBearerToken();
        assertEquals(expectedToken, token);
    }

    // 4XX HttpClientErrorException
    // 5XX HttpServerErrorException
    @ParameterizedTest
    @ValueSource(ints = { 500, 400 })
    void testGetBearerToken_Error(int code) {
        String expectedToken = "mockToken";
        TokenResponse mockTokenResponse = new TokenResponse();
        mockTokenResponse.setAccessToken(expectedToken);
        wm1.stubFor(post("/token").willReturn(jsonResponse(mockTokenResponse, code)));
        if (code == 400)
            assertThrows(HttpClientErrorException.class, () -> httpClientService.getBearerToken());
        else assertThrows(HttpServerErrorException.class, () -> httpClientService.getBearerToken());
    }

    @Test
    void testSignStatusListVC_Success() {
        final var issuer = "did:web:localhost:BPNL345345345345";
        var fragment = UUID.randomUUID().toString();
        var encodedList = mockEmptyEncodedList();
        var credentialBuilder = mockStatusListVC(issuer, fragment, encodedList);
        var unsignedCredential = credentialBuilder.build();
        var statusListCredential = mockStatusListCredential(issuer, credentialBuilder);
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("123456");
        wm1.stubFor(post("/token").willReturn(jsonResponse(tokenResponse, 200)));
        wm1.stubFor(
                post("/api/credentials?isRevocable=false")
                        .willReturn(jsonResponse(statusListCredential.getCredential(), 200)));
        VerifiableCredential signedCredential =
                assertDoesNotThrow(
                        () ->
                                httpClientService.signStatusListVC(
                                        unsignedCredential, httpClientService.getBearerToken()));
        assertThat(signedCredential).hasFieldOrProperty("proof");
    }

    @Test
    void testSignStatusListVC_Error() {
        wm1.stubFor(post("/api/credentials").willReturn(aResponse().withStatus(400)));

        final var issuer = "did:web:localhost:BPNL345345345345";
        var fragment = UUID.randomUUID().toString();
        var encodedList = mockEmptyEncodedList();
        var credentialBuilder = mockStatusListVC(issuer, fragment, encodedList);
        var unsignedCredential = credentialBuilder.build();

        // HttpClientErrorException extends RestClientException
        assertThrows(
                HttpClientErrorException.class,
                () -> httpClientService.signStatusListVC(unsignedCredential, "dummy"));
    }
}
