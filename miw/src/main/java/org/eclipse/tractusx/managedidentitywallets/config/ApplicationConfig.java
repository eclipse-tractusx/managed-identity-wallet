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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.signing.KeyProvider;
import org.eclipse.tractusx.managedidentitywallets.signing.LocalSigningService;
import org.eclipse.tractusx.managedidentitywallets.signing.SigningService;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The type Application config.
 */
@Configuration
@Slf4j
public class ApplicationConfig implements WebMvcConfigurer {

    private final SwaggerUiConfigProperties properties;
    private final String resourceBundlePath;
    private final MIWSettings miwSettings;

    @Autowired
    public ApplicationConfig(@Value("${resource.bundle.path:classpath:i18n/language}") String resourceBundlePath, SwaggerUiConfigProperties properties, MIWSettings miwSettings) {
        this.resourceBundlePath = resourceBundlePath;
        this.properties = properties;
        this.miwSettings = miwSettings;
    }

    /**
     * Object mapper object mapper.
     *
     * @return ObjectMapper object mapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        String redirectUri = properties.getPath();
        log.info("Set landing page to path {}", StringEscapeUtils.escapeJava(redirectUri));
        registry.addRedirectViewController("/", redirectUri);
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
        bean.setBasename(resourceBundlePath);
        bean.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return bean;
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean beanValidatorFactory = new LocalValidatorFactoryBean();
        beanValidatorFactory.setValidationMessageSource(messageSource());
        return beanValidatorFactory;
    }

    @Bean
    public Map<SigningServiceType, SigningService> availableKeyStorages(List<SigningService> storages, List<KeyProvider> keyProviders) {
        KeyProvider localSigningKeyProvider = keyProviders.stream().filter(s -> s.getKeyStorageType().equals(miwSettings.localSigningKeyStorageType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no key provider with type %s found".formatted(miwSettings.localSigningKeyStorageType())));

        Map<SigningServiceType, SigningService> available = new EnumMap<>(SigningServiceType.class);
        storages.forEach(
                s -> {
                    if (s instanceof LocalSigningService local) {
                        local.setKeyProvider(localSigningKeyProvider);
                    }
                    available.put(s.getSupportedServiceType(), s);
                }
        );

        return available;
    }
}
