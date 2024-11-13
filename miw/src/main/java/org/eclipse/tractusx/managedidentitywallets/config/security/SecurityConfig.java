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

package org.eclipse.tractusx.managedidentitywallets.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.ApplicationRole;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.service.STSTokenValidationService;
import org.eclipse.tractusx.managedidentitywallets.utils.BpnValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * The type Security config.
 */
@Slf4j
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final STSTokenValidationService validationService;

    private final SecurityConfigProperties securityConfigProperties;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Filter chain security filter chain.
     *
     * @param http the http
     * @return the security filter chain
     * @throws Exception the exception
     */
    @Bean
    @ConditionalOnProperty(value = "miw.security.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer
                        .xssProtection(Customizer.withDefaults())
                        .contentSecurityPolicy(contentSecurityPolicyConfig -> contentSecurityPolicyConfig.policyDirectives("script-src 'self'")))
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(new AntPathRequestMatcher("/")).permitAll() // forwards to swagger
                        .requestMatchers(new AntPathRequestMatcher("/docs/api-docs/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/ui/swagger-ui/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/actuator/health/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/token", POST.name())).permitAll()
                        .requestMatchers(new AntPathRequestMatcher(RestURI.API_PRESENTATIONS_IATP, POST.name())).permitAll()
                        .requestMatchers(new AntPathRequestMatcher(RestURI.API_PRESENTATIONS_IATP_WORKAROUND, POST.name())).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/actuator/loggers/**")).hasRole(ApplicationRole.ROLE_MANAGE_APP)

                        //did document resolve APIs
                        .requestMatchers(new AntPathRequestMatcher(RestURI.DID_RESOLVE, GET.name())).permitAll() //Get did document
                        .requestMatchers(new AntPathRequestMatcher(RestURI.DID_DOCUMENTS, GET.name())).permitAll() //Get did document

                        //wallet APIS
                        .requestMatchers(new AntPathRequestMatcher(RestURI.WALLETS, POST.name())).hasRole(ApplicationRole.ROLE_ADD_WALLETS) //Create wallet
                        .requestMatchers(new AntPathRequestMatcher(RestURI.WALLETS, GET.name())).hasAnyRole(ApplicationRole.ROLE_VIEW_WALLETS) //Get all wallet
                        .requestMatchers(new AntPathRequestMatcher(RestURI.API_WALLETS_IDENTIFIER, GET.name())).hasAnyRole(ApplicationRole.ROLE_VIEW_WALLET, ApplicationRole.ROLE_VIEW_WALLETS) //get wallet by identifier
                        .requestMatchers(new AntPathRequestMatcher(RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, POST.name())).hasAnyRole(ApplicationRole.ROLE_UPDATE_WALLETS, ApplicationRole.ROLE_UPDATE_WALLET) //Store credential

                        //VP-Generation
                        .requestMatchers(new AntPathRequestMatcher(RestURI.API_PRESENTATIONS, POST.name())).hasAnyRole(ApplicationRole.ROLE_UPDATE_WALLETS, ApplicationRole.ROLE_UPDATE_WALLET, ApplicationRole.ROLE_VIEW_WALLETS, ApplicationRole.ROLE_VIEW_WALLET) //Create VP

                        //VP - Validation
                        .requestMatchers(new AntPathRequestMatcher(RestURI.API_PRESENTATIONS_VALIDATION, POST.name())).hasAnyRole(ApplicationRole.ROLE_VIEW_WALLETS, ApplicationRole.ROLE_VIEW_WALLET) //validate VP

                        //VC - revoke
                        .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS_REVOKE, PUT.name())).hasAnyRole(ApplicationRole.ROLE_UPDATE_WALLET, ApplicationRole.ROLE_UPDATE_WALLETS) //revoke credentials

                        //VC - Holder
                        .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS, GET.name())).hasAnyRole(ApplicationRole.ROLE_VIEW_WALLET, ApplicationRole.ROLE_VIEW_WALLETS) //get credentials
                        .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS, POST.name())).hasAnyRole(ApplicationRole.ROLE_UPDATE_WALLET, ApplicationRole.ROLE_UPDATE_WALLETS) //issue credential

                        //VC - validation
                        .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS_VALIDATION, POST.name())).hasAnyRole(ApplicationRole.ROLE_VIEW_WALLET, ApplicationRole.ROLE_VIEW_WALLETS) //validate credentials

                        //VC - Issuer
                        .requestMatchers(new AntPathRequestMatcher(RestURI.ISSUERS_CREDENTIALS, GET.name())).hasAnyRole(ApplicationRole.ROLE_UPDATE_WALLETS) //Lis of issuer VC
                        .requestMatchers(new AntPathRequestMatcher(RestURI.ISSUERS_CREDENTIALS, POST.name())).hasAnyRole(ApplicationRole.ROLE_UPDATE_WALLETS) //Issue VC

                        //error
                        .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
                ).oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(new CustomAuthenticationConverter(securityConfigProperties.clientId())))
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint()))
                .securityMatcher(new NegatedRequestMatcher(new OrRequestMatcher(
                        List.of(
                                new AntPathRequestMatcher(RestURI.API_PRESENTATIONS_IATP),
                                new AntPathRequestMatcher(RestURI.API_PRESENTATIONS_IATP_WORKAROUND)))));

        return http.build();
    }

    /**
     * Security customizer web security customizer.
     *
     * @return the web security customizer
     */
    @Bean
    @ConditionalOnProperty(value = "miw.security.enabled", havingValue = "false")
    public WebSecurityCustomizer securityCustomizer() {
        log.warn("Disable security : This is not recommended to use in production environments.");
        return web -> web.ignoring().requestMatchers(new AntPathRequestMatcher("**"));
    }

    /**
     * Needed to enable an event-listener for failed login attempts.
     */
    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher
    (ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);
        OAuth2TokenValidator<Jwt> bpnValidator = bpnValidator();
        OAuth2TokenValidator<Jwt> withBpn = new DelegatingOAuth2TokenValidator<>(bpnValidator);
        jwtDecoder.setJwtValidator(withBpn);
        return jwtDecoder;
    }

    OAuth2TokenValidator<Jwt> bpnValidator() {
        return new BpnValidator();
    }
}
