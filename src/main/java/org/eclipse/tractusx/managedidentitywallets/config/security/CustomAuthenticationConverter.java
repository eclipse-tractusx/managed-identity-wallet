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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * The type Custom authentication converter.
 */
public class CustomAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter;
    private final String resourceId;

    /**
     * Instantiates a new Custom authentication converter.
     *
     * @param resourceId the resource id
     */
    public CustomAuthenticationConverter(String resourceId) {
        this.resourceId = resourceId;
        grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    }
    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        Collection<GrantedAuthority> authorities = (grantedAuthoritiesConverter.convert(source))
                .stream()
                .collect(Collectors.toSet());
        authorities.addAll(extractResourceRoles(source, resourceId));
        extractResourceRoles(source, resourceId);
        return new JwtAuthenticationToken(source, authorities);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt, String resourceId) {
        return Optional.ofNullable(jwt.getClaim("resource_access"))
        .filter(resourceAccess -> resourceAccess instanceof Map)
            .map(resourceAccess -> ((Map<String, Object>) resourceAccess).get(resourceId))
            .filter(resource -> resource instanceof Map)
            .map(resource -> ((Map<String, Object>) resource).get("roles"))
            .filter(resourceRoles -> resourceRoles instanceof Collection)
            .map(resourceRoles -> ((Collection<String>) resourceRoles).stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .collect(Collectors.toSet()))
            .orElse(Set.of());
    }
}