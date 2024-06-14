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

package org.eclipse.tractusx.managedidentitywallets.revocation.config.security;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.ApplicationRole;
import org.eclipse.tractusx.managedidentitywallets.revocation.constant.RevocationApiEndpoints;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAnyRole;

@Slf4j
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@Configuration
@AllArgsConstructor
public class SecurityConfig {

    private final SecurityConfigProperties securityConfigProperties;

    /**
     * Filter chain security filter chain.
     *
     * @param http the http
     * @return the security filter chain
     * @throws Exception the exception
     */
    @Bean
    @ConditionalOnProperty(
            value = "service.security.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(
                        httpSecurityHeadersConfigurer ->
                                httpSecurityHeadersConfigurer
                                        .xssProtection(Customizer.withDefaults())
                                        .contentSecurityPolicy(
                                                contentSecurityPolicyConfig ->
                                                        contentSecurityPolicyConfig.policyDirectives("script-src 'self'")))
                .sessionManagement(
                        sessionManagement ->
                                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorizeHttpRequests ->
                                authorizeHttpRequests
                                        .requestMatchers("/")
                                        .permitAll() // forwards to swagger
                                        .requestMatchers("/docs/api-docs/**")
                                        .permitAll()
                                        .requestMatchers("/ui/swagger-ui/**")
                                        .permitAll()
                                        .requestMatchers("/error")
                                        .permitAll()
                                        .requestMatchers(new AntPathRequestMatcher("/actuator/health/**"))
                                        .permitAll()
                                        .requestMatchers(new AntPathRequestMatcher("/actuator/loggers/**"))
                                        .hasRole(ApplicationRole.ROLE_MANAGE_APP)
                                        .requestMatchers(
                                                HttpMethod.GET, RevocationApiEndpoints.REVOCATION_API + "/credentials/**")
                                        .permitAll()
                                        .requestMatchers(RevocationApiEndpoints.REVOCATION_API + "/**")
                                        .access(hasAnyRole(ApplicationRole.ROLE_UPDATE_WALLET)))
                .oauth2ResourceServer(
                        resourceServer ->
                                resourceServer.jwt(
                                        jwt ->
                                                jwt.jwtAuthenticationConverter(
                                                        new CustomAuthenticationConverter(
                                                                securityConfigProperties.clientId()))));
        return http.build();
    }

    /**
     * Security customizer web security customizer.
     *
     * @return the web security customizer
     */
    @Bean
    @ConditionalOnProperty(value = "service.security.enabled", havingValue = "false")
    public WebSecurityCustomizer securityCustomizer() {
        log.warn("Disable security : This is not recommended to use in production environments.");
        return web -> web.ignoring().requestMatchers(new AntPathRequestMatcher("**"));
    }
}
