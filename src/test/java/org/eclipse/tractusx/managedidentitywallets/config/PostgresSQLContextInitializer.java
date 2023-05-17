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

package org.eclipse.tractusx.managedidentitywallets.config;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresSQLContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:15.2");

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        this.postgreSQLContainer.start();
        TestPropertyValues.of(
                "spring.datasource.url=" + this.postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username=" + this.postgreSQLContainer.getUsername(),
                "spring.datasource.password=" + this.postgreSQLContainer.getPassword()
        ).applyTo(applicationContext.getEnvironment());
    }
}