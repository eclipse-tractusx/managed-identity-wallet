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

package org.eclipse.tractusx.managedidentitywallets.config.openapi;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.config.security.SecurityConfigProperties;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * OpenApiConfig is used for managing the swagger with basic security setup if security is enabled.
 */
@Configuration
@AllArgsConstructor
public class OpenApiConfig {

    private final SecurityConfigProperties properties;

    /**
     * Open api open api.
     *
     * @return the open api
     */
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info();
        info.setTitle("MIW API");
        info.setDescription("MIW API");
        info.setVersion("0.0.1");
        OpenAPI openAPI = new OpenAPI();
        if (Boolean.TRUE.equals(properties.enabled())) {
            openAPI = enableSecurity(openAPI);
        }
        return openAPI.info(info);
    }

    /**
     * Open api definition grouped open api.
     *
     * @return the grouped open api
     */
    @Bean
    public GroupedOpenApi openApiDefinition() {
        return GroupedOpenApi.builder()
                .group("docs")
                .pathsToMatch("/**")
                .displayName("Docs")
                .build();
    }

    private OpenAPI enableSecurity(OpenAPI openAPI) {
        String authorization = "Authorization";
        Components components = new Components();
        components.addSecuritySchemes(
                "open_id_scheme",
                new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows()
                                .authorizationCode(new OAuthFlow()
                                        .authorizationUrl(properties.authUrl())
                                        .tokenUrl(properties.tokenUrl())
                                        .refreshUrl(properties.refreshTokenUrl()
                                        )
                                )
                        )
        );

        components.addSecuritySchemes(authorization,
                new SecurityScheme().name(authorization)
                        .type(SecurityScheme.Type.HTTP).scheme("Bearer"));
        return openAPI.components(components)
                .addSecurityItem(new SecurityRequirement()
                        .addList(authorization, Collections.emptyList())
                        .addList("open_id_scheme", Collections.emptyList()));
    }
}