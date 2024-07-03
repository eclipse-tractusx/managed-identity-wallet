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

package org.eclipse.tractusx.managedidentitywallets.did;

import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.service.WalletService;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COLON_SEPARATOR;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ContextConfiguration(initializers = {TestContextInitializer.class})
class DidDocumentsTest {

    @Autowired
    private WalletService walletService;
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MIWSettings miwSettings;

    @Test
    void getDidDocumentInvalidBpn404() {
        ResponseEntity<String> response = restTemplate.getForEntity(RestURI.DID_DOCUMENTS, String.class, TestUtils.getRandomBpmNumber());
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    void getDidDocumentWithBpn200() {

        String bpn = TestUtils.getRandomBpmNumber();
        createWallet(bpn);

        ResponseEntity<String> response = restTemplate.getForEntity(RestURI.DID_DOCUMENTS, String.class, bpn);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
    }

    @Test
    void getDidResolveInvalidBpn404() {
        ResponseEntity<String> response = restTemplate.getForEntity(RestURI.DID_RESOLVE, String.class, TestUtils.getRandomBpmNumber());
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    void getDidResolveWithBpn200() {

        String bpn = TestUtils.getRandomBpmNumber();

        createWallet(bpn);
        ResponseEntity<String> response = restTemplate.getForEntity(RestURI.DID_RESOLVE, String.class, bpn);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
    }

    private Wallet createWallet(String bpn) {
        CreateWalletRequest createWalletRequest = new CreateWalletRequest();
        createWalletRequest.setBusinessPartnerNumber(bpn);
        createWalletRequest.setCompanyName("wallet_" + bpn);
        createWalletRequest.setDidUrl(miwSettings.host() + COLON_SEPARATOR + bpn);
        return walletService.createWallet(createWalletRequest, miwSettings.authorityWalletBpn());
    }
}
