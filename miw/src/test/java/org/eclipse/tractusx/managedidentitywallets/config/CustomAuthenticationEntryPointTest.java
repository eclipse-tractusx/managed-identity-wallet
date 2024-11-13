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

package org.eclipse.tractusx.managedidentitywallets.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.config.security.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

class CustomAuthenticationEntryPointTest {

    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        customAuthenticationEntryPoint = new CustomAuthenticationEntryPoint();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("Commence should set unauthorized status and headers when OAuth2 authentication exception")
    void commenceShouldSetUnauthorizedStatusAndHeadersWhenOAuth2AuthenticationException() {
        OAuth2Error error = new OAuth2Error("invalid_token", "The token is invalid", "https://example.com");
        OAuth2AuthenticationException authException = new OAuth2AuthenticationException(error);

        customAuthenticationEntryPoint.commence(request, response, authException);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.WWW_AUTHENTICATE), headerCaptor.capture());
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());

        String wwwAuthenticate = headerCaptor.getValue();
        assertEquals("Bearer error=\"invalid_token\", error_description=\"The token is invalid\", error_uri=\"https://example.com\"", wwwAuthenticate);
    }

    @Test
    @DisplayName("Commence should set forbidden status when bpn not found exception")
    void commence_ShouldSetForbiddenStatus_WhenBpnNotFoundException() {
        AuthenticationException authException = new AuthenticationException(StringPool.BPN_NOT_FOUND) {
        };

        customAuthenticationEntryPoint.commence(request, response, authException);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Commence should set custom realm when realm name is set")
    void commence_ShouldSetCustomRealm_WhenRealmNameIsSet() {
        customAuthenticationEntryPoint.setRealmName("custom-realm");

        OAuth2Error error = new OAuth2Error("invalid_token", "The token is invalid", "https://example.com");
        OAuth2AuthenticationException authException = new OAuth2AuthenticationException(error);

        customAuthenticationEntryPoint.commence(request, response, authException);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.WWW_AUTHENTICATE), headerCaptor.capture());

        String wwwAuthenticate = headerCaptor.getValue();
        assertEquals("Bearer realm=\"custom-realm\", error=\"invalid_token\", error_description=\"The token is invalid\", error_uri=\"https://example.com\"", wwwAuthenticate);
    }

    @Test
    @DisplayName("Commence should set scope when bearer token error has scope")
    void commence_ShouldSetScope_WhenBearerTokenErrorHasScope() {
        BearerTokenError error = new BearerTokenError("insufficient_scope", HttpStatus.UNAUTHORIZED, "Insufficient scope", "https://example.com", "scope1 scope2");
        OAuth2AuthenticationException authException = new OAuth2AuthenticationException(error);

        customAuthenticationEntryPoint.commence(request, response, authException);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.WWW_AUTHENTICATE), headerCaptor.capture());

        String wwwAuthenticate = headerCaptor.getValue();
        assertEquals("Bearer error=\"insufficient_scope\", error_description=\"Insufficient scope\", error_uri=\"https://example.com\", scope=\"scope1 scope2\"", wwwAuthenticate);
    }
}
