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
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.config.security.SecurityConfigProperties;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

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
        info.setTitle("Managed Identity Wallets API");
        info.setDescription("Managed Identity Wallets API");
        info.termsOfService("https://www.eclipse.org/legal/termsofuse.php");
        info.setVersion("0.0.1");

        Contact contact = new Contact();
        contact.name("Eclipse Tractus-X");
        contact.email("tractusx-dev@eclipse.org");
        contact.url("https://projects.eclipse.org/projects/automotive.tractusx");
        info.contact(contact);

        License license = new License();
        license.name("Apache 2.0");
        license.url("https://github.com/eclipse-tractusx/managed-identity-wallets/blob/develop/LICENSE");
        info.license(license);

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
        Components components = new Components();

        //Auth using access_token
        String accessTokenAuth = "Authenticate using access_token";
        components.addSecuritySchemes(accessTokenAuth,
                new SecurityScheme().name(accessTokenAuth)
                        .description("""
                                **Bearer (apiKey)**
                                JWT Authorization header using the Bearer scheme.
                                Enter **Bearer** [space] and then your token in the text input below:
                                Example: Bearer 12345abcdef
                                """)
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(HttpHeaders.AUTHORIZATION));

        //Auth using sts_token
        String stsTokenAuth = "sts_token";
        components.addSecuritySchemes(stsTokenAuth,
                new SecurityScheme().name(stsTokenAuth)
                        .description("""
                                **STS token**
                                JWT Authorization header.
                                Enter your token in the text input below:
                                Example: 12345abcdef
                                """)
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(HttpHeaders.AUTHORIZATION));

        return openAPI.components(components)
                .addSecurityItem(new SecurityRequirement()
                        .addList(accessTokenAuth, Collections.emptyList()))
                .addSecurityItem(new SecurityRequirement()
                        .addList(stsTokenAuth, Collections.emptyList()));
    }
}
