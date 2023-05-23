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

package org.eclipse.tractusx.managedidentitywallets;

import org.eclipse.tractusx.managedidentitywallets.config.PostgresSQLContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ActiveProfiles("test")
@ContextConfiguration(initializers = {PostgresSQLContextInitializer.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WalletTest {

    private final String bpn = "123456789";

    private final String name = "smartSense";

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletKeyRepository walletKeyRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EncryptionUtils encryptionUtils;


    @Test
    void encryptionTest(){
        String originalMassage = "Dummy test message";
        String encrypt = encryptionUtils.encrypt(originalMassage);
        String decrypt = encryptionUtils.decrypt(encrypt);
        Assertions.assertEquals(originalMassage, decrypt);
    }

    @Test
    @Order(1)
    void createWalletTest201(){
        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, new HttpEntity<>(request), Wallet.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());

        Wallet wallet =  walletRepository.getByBpn(bpn);
        Assertions.assertNotNull(wallet);
        WalletKey walletKey = walletKeyRepository.getByWalletId(wallet.getId());
        Assertions.assertNotNull(walletKey);
    }

    @Test
    @Order(2)
    void createWalletWithDuplicateBpn409(){
        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();
        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, new HttpEntity<>(request), Wallet.class);
        Assertions.assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
    }

    @Test
    @Order(3)
    void getWalletByBpnTest200(){
        ResponseEntity<Wallet> response = restTemplate.getForEntity(RestURI.WALLETS_BY_BPN, Wallet.class, bpn);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Wallet body = response.getBody();
        Assertions.assertEquals(body.getBpn(), bpn);
    }

    @Test
    @Order(4)
    void getWalletInvalidBpn404() {
        ResponseEntity<Wallet> response = restTemplate.getForEntity(RestURI.WALLETS_BY_BPN, Wallet.class, UUID.randomUUID().toString());
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    @Order(5)
    void getWallets200() {
        ResponseEntity<List<Wallet>> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {
                });
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertTrue(response.getBody().size() == 1);
    }
}
