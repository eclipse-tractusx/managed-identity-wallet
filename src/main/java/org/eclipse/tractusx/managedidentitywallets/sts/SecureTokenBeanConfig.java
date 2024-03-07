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

package org.eclipse.tractusx.managedidentitywallets.sts;

import org.eclipse.tractusx.managedidentitywallets.dao.repository.JtiRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.interfaces.SecureTokenIssuer;
import org.eclipse.tractusx.managedidentitywallets.interfaces.SecureTokenService;
import org.eclipse.tractusx.managedidentitywallets.service.SecureTokenServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecureTokenBeanConfig {

    @Bean
    public SecureTokenService secureTokenService(
            WalletKeyRepository keyRepository,
            WalletRepository walletRepository,
            SecureTokenIssuer issuer,
            SecureTokenConfigurationProperties properties,
            JtiRepository jtiRepository
    ) {
        return new SecureTokenServiceImpl(keyRepository, walletRepository, issuer, properties, jtiRepository);
    }

}
