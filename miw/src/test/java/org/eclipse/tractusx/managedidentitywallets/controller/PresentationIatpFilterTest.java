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
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dto.ValidationResult;
import org.eclipse.tractusx.managedidentitywallets.service.STSTokenValidationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;

import static org.eclipse.tractusx.managedidentitywallets.constant.TokenValidationErrors.NONCE_MISSING;
import static org.eclipse.tractusx.managedidentitywallets.constant.TokenValidationErrors.TOKEN_ALREADY_EXPIRED;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
class PresentationIatpFilterTest {

    private static final String TOKEN = "eyJraWQiOiI1OGNiNGIzMi1jMmU0LTQ2ZjAtYTNhZC0zMjg2ZTM0NzY1ZWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.eyJhY2Nlc3NfdG9rZW4iOiJleUpyYVdRaU9pSTFPR05pTkdJek1pMWpNbVUwTFRRMlpqQXRZVE5oWkMwek1qZzJaVE0wTnpZMVpXUWlMQ0owZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGWkVSVFFTSjkuZXlKaGRXUWlPaUprYVdRNmQyVmlPbXh2WTJGc2FHOXpkRHBDVUU1TU1EQXdNREF3TURBd01EQXdJaXdpYzNWaUlqb2laR2xrT25kbFlqcHNiMk5oYkdodmMzUTZRbEJPVERBd01EQXdNREF3TURBd01DSXNJbk5qYjNCbElqb2liM0puTG1WamJHbHdjMlV1ZEhKaFkzUjFjM2d1ZG1NdWRIbHdaVHBXWVd4cFpFTnlaV1JsYm5ScFlXeFVlWEJsT25KbFlXUWlMQ0pwYzNNaU9pSmthV1E2ZDJWaU9teHZZMkZzYUc5emREcENVRTVNTURBd01EQXdNREF3TURBd0lpd2laWGh3SWpveE56QTNNak13TkRVNUxDSnBZWFFpT2pFM01EY3lNekF6T1Rrc0ltcDBhU0k2SW1FNU16YzJNakk0TFRreVpUSXROR1pqT0MwNVpUZ3pMVGMxWlRneFpEVm1OR1V3TXlKOS40WHBKVTl0VlQ5QU4zT2JYdHZOX2hGcTNqY2Z0QjMwR2tJOXJHUWhBbFA2MnB5eFNZeDZTRENEVkJTbmpQTUE0MVB3cXIzaC1OVVVtcmFVU2dvUXNBZyIsImF1ZCI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJzdWIiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwiaXNzIjoiZGlkOndlYjpsb2NhbGhvc3Q6QlBOTDAwMDAwMDAwMDAwMCIsImV4cCI6MTcwNzIzMDQ1OSwiaWF0IjoxNzA3MjMwMzk5LCJqdGkiOiJhYWQ4OTUzMS04YjE4LTQzN2EtOGZmNS1lZDc2OThjMmFlYTAifQ.HXPtWRDh6rIlYdhQq40zLmeLhWgQnj_EwHYZ014AuTJhSgEmTep756nNyTcMXqa-cloNxoKrA323VLcaOAezBQ";

    @MockBean
    private STSTokenValidationService validationService;

    @Autowired
    private TestRestTemplate testTemplate;

    @Test
    void createPresentationFailure401Test() {
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON_VALUE));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = testTemplate.exchange(
                RestURI.API_PRESENTATIONS_IATP,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void createPresentationFailure401WithErrorsTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON_VALUE));
        headers.put(HttpHeaders.AUTHORIZATION, List.of(TOKEN));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ValidationResult validationResult = ValidationResult.builder()
                .isValid(false)
                .errors(List.of(TOKEN_ALREADY_EXPIRED, NONCE_MISSING))
                .build();

        Mockito.when(validationService.validateToken(TOKEN)).thenReturn(validationResult);

        ResponseEntity<String> response = testTemplate.exchange(
                RestURI.API_PRESENTATIONS_IATP,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        String expectedBody = TOKEN_ALREADY_EXPIRED.name() + StringPool.COMA_SEPARATOR + NONCE_MISSING.name();

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Assertions.assertEquals(expectedBody, response.getBody());
    }
}
