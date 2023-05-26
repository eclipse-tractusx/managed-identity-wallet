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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ActiveProfiles("test")
@ContextConfiguration(initializers = {TestContextInitializer.class})
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
    void encryptionTest() {
        String originalMassage = "Dummy test message";
        String encrypt = encryptionUtils.encrypt(originalMassage);
        String decrypt = encryptionUtils.decrypt(encrypt);
        Assertions.assertEquals(originalMassage, decrypt);
    }


    @Test
    @Order(1)
    void createWalletTest403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, Wallet.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    @Order(2)
    void createWalletTest201() throws JsonProcessingException, JSONException {

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, String.class);
        String body = response.getBody();

        Wallet wallet1 = getWalletFromString(body);

        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());

        Assertions.assertEquals(wallet1.getBpn(), bpn);
        Assertions.assertEquals(wallet1.getName(), name);

        Wallet wallet = walletRepository.getByBpn(bpn);
        Assertions.assertEquals(wallet.getBpn(), bpn);
        Assertions.assertEquals(wallet.getName(), name);
        Assertions.assertNotNull(wallet);
        WalletKey walletKey = walletKeyRepository.getByWalletId(wallet.getId());
        Assertions.assertNotNull(walletKey);

        Assertions.assertEquals(wallet.getBpn(), bpn);

    }

    @NotNull
    private static Wallet getWalletFromString(String body) throws JsonProcessingException {
        JSONObject jsonObject = new JSONObject(body);
        JSONObject didDocument = jsonObject.getJSONObject("didDocument");
        jsonObject.remove("didDocument");
        ObjectMapper objectMapper = new ObjectMapper();
        Wallet wallet1 = objectMapper.readValue(jsonObject.toString(), Wallet.class);
        wallet1.setDidDocument(DidDocument.fromJson(didDocument.toString()));
        System.out.println("wallet -- >" + wallet1.getBpn());
        return wallet1;
    }

    @Test
    @Order(3)
    void createWalletWithDuplicateBpn409() {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();


        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, Wallet.class);
        Assertions.assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
    }

    @Test
    @Order(4)
    void getWalletByBpnTest403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS_BY_BPN, HttpMethod.GET, entity, Wallet.class, bpn);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    @Order(5)
    void getWalletByBpnTest200() throws JsonProcessingException {

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.WALLETS_BY_BPN, HttpMethod.GET, entity, String.class, bpn);

        Wallet body = getWalletFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(body.getBpn(), bpn);
    }

    @Test
    @Order(6)
    void getWalletInvalidBpn404() {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS_BY_BPN, HttpMethod.GET, entity, Wallet.class, UUID.randomUUID().toString());

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    @Order(7)
    void getWallets403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Wallet>> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {
                });
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    @Order(8)
    void getWallets200() throws JsonProcessingException {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.GET, entity, String.class);
        List<Wallet> body = getWalletsFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(body).size() > 0);
    }

    private static List<Wallet> getWalletsFromString(String body) throws JsonProcessingException {
        List<Wallet> walletList = new ArrayList<>();
        JSONArray array = new JSONArray(body);
        if (array.length() == 0) {
            return walletList;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject wallet = array.getJSONObject(i);
            walletList.add(getWalletFromString(wallet.toString()));
        }
        return walletList;
    }

}
