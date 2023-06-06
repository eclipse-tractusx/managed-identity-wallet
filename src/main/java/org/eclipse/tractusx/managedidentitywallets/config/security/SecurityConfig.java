/*
 * *******************************************************************************
 *  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.constant.ApplicationConstant;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/**
 * The type Security config.
 */
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
    @ConditionalOnProperty(value = "miw.security.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and()
                .csrf().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeHttpRequests()
                .requestMatchers(new AntPathRequestMatcher("/")).permitAll() // forwards to swagger
                .requestMatchers(new AntPathRequestMatcher("/docs/api-docs/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/ui/swagger-ui/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/health/**")).permitAll()

                //did document resolve APIs
                .requestMatchers(new AntPathRequestMatcher(RestURI.DID_RESOLVE, GET.name())).permitAll() //Get did document
                .requestMatchers(new AntPathRequestMatcher(RestURI.DID_DOCUMENTS, GET.name())).permitAll() //Get did document

                //wallet APIS
                .requestMatchers(new AntPathRequestMatcher(RestURI.WALLETS, POST.name())).hasRole(ApplicationConstant.ROLE_ADD_WALLETS) //Create wallet
                .requestMatchers(new AntPathRequestMatcher(RestURI.WALLETS, GET.name())).hasAnyRole(ApplicationConstant.ROLE_VIEW_WALLETS) //Get all wallet
                .requestMatchers(new AntPathRequestMatcher(RestURI.API_WALLETS_IDENTIFIER, GET.name())).hasAnyRole(ApplicationConstant.ROLE_VIEW_WALLET, ApplicationConstant.ROLE_VIEW_WALLETS) //get wallet by identifier
                .requestMatchers(new AntPathRequestMatcher(RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, POST.name())).hasAnyRole(ApplicationConstant.ROLE_UPDATE_WALLETS, ApplicationConstant.ROLE_UPDATE_WALLET) //Store credential

                //VP-Generation
                .requestMatchers(new AntPathRequestMatcher(RestURI.API_PRESENTATIONS, POST.name())).hasAnyRole(ApplicationConstant.ROLE_UPDATE_WALLETS, ApplicationConstant.ROLE_UPDATE_WALLET) //Create VP

                //VP - Validation
                .requestMatchers(new AntPathRequestMatcher(RestURI.API_PRESENTATIONS_VALIDATION, POST.name())).hasAnyRole(ApplicationConstant.ROLE_VIEW_WALLETS, ApplicationConstant.ROLE_VIEW_WALLET) //validate VP

                //VC - Holder
                .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS, GET.name())).hasAnyRole(ApplicationConstant.ROLE_VIEW_WALLET, ApplicationConstant.ROLE_VIEW_WALLETS) //get credentials
                .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS, POST.name())).hasAnyRole(ApplicationConstant.ROLE_UPDATE_WALLET, ApplicationConstant.ROLE_UPDATE_WALLETS) //issue credentials

                //VC - validation
                .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS_VALIDATION, POST.name())).hasAnyRole(ApplicationConstant.ROLE_VIEW_WALLET, ApplicationConstant.ROLE_VIEW_WALLETS) //validate credentials

                //VC - Issuer
                .requestMatchers(new AntPathRequestMatcher(RestURI.ISSUERS_CREDENTIALS, GET.name())).hasAnyRole(ApplicationConstant.ROLE_VIEW_WALLET, ApplicationConstant.ROLE_UPDATE_WALLETS) //Lis of issuer VC
                .requestMatchers(new AntPathRequestMatcher(RestURI.ISSUERS_CREDENTIALS, POST.name())).hasAnyRole(ApplicationConstant.ROLE_UPDATE_WALLET, ApplicationConstant.ROLE_UPDATE_WALLETS) //Issue VC
                .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, POST.name())).hasAnyRole(ApplicationConstant.ROLE_UPDATE_WALLETS, ApplicationConstant.ROLE_UPDATE_WALLET) //issue Membership Credential
                .requestMatchers(new AntPathRequestMatcher(RestURI.CREDENTIALS_ISSUER_DISMANTLER, POST.name())).hasAnyRole(ApplicationConstant.ROLE_UPDATE_WALLETS, ApplicationConstant.ROLE_UPDATE_WALLET) //issue dismantler Credential
                .requestMatchers(new AntPathRequestMatcher(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, POST.name())).hasAnyRole(ApplicationConstant.ROLE_UPDATE_WALLETS, ApplicationConstant.ROLE_UPDATE_WALLET) //issue dismantler Credential

                //error
                .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
                .and().oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(new CustomAuthenticationConverter(securityConfigProperties.clientId()));
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
}
