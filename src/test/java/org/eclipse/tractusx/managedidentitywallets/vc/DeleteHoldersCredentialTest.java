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

package org.eclipse.tractusx.managedidentitywallets.vc;

import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.IssuersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ActiveProfiles("test")
@ContextConfiguration(initializers = {TestContextInitializer.class})
class DeleteHoldersCredentialTest {
    @Autowired
    private HoldersCredentialRepository holdersCredentialRepository;
    @Autowired
    private IssuersCredentialRepository issuersCredentialRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MIWSettings miwSettings;


    @Test
    void deleteCredentialTestWithInvalidRole403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();
        HttpEntity<Map> entity = new HttpEntity<>(headers);
        ResponseEntity<Object> response = restTemplate.exchange(RestURI.CREDENTIALS, HttpMethod.DELETE, entity, Object.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void deleteCredentialTest204() {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        //create new wallet
        TestUtils.createWallet(bpn, did, restTemplate);

        //Fetch bpn credential which is auto generated while create wallet
        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDid(did);
        String type = credentials.get(0).getType();
        Assertions.assertFalse(credentials.isEmpty());

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        HttpEntity<Map> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS + "?id={id}", HttpMethod.DELETE, entity, String.class, credentials.get(0).getCredentialId());

        Assertions.assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatusCode().value());

        credentials = holdersCredentialRepository.getByHolderDid(did);
        Assertions.assertTrue(credentials.isEmpty());

        //check, VC should not be deleted from issuer table
        List<IssuersCredential> vcs = issuersCredentialRepository.getByIssuerDidAndHolderDidAndType(miwSettings.authorityWalletDid(), did, type);
        Assertions.assertFalse(vcs.isEmpty());
    }

    @Test
    void deleteCredentialTest404() {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());
        HttpEntity<Map> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS + "?id={id}", HttpMethod.DELETE, entity, String.class, "");
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

}
