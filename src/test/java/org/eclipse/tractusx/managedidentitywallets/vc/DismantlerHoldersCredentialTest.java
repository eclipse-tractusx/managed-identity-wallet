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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.IssuersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueDismantlerCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ContextConfiguration(initializers = {TestContextInitializer.class})
class DismantlerHoldersCredentialTest {
    @Autowired
    private HoldersCredentialRepository holdersCredentialRepository;
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletKeyRepository walletKeyRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private IssuersCredentialRepository issuersCredentialRepository;


    @Test
    void issueDismantlerCredentialTest403() {
        String bpn = TestUtils.getRandomBpmNumber();

        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        IssueMembershipCredentialRequest request = IssueMembershipCredentialRequest.builder().bpn(bpn).build();

        HttpEntity<IssueMembershipCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<VerifiableCredential> response = restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_DISMANTLER, HttpMethod.POST, entity, VerifiableCredential.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    void issueDismantlerCredentialToBaseWalletTest201() throws JSONException {
        Wallet wallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        String oldSummaryCredentialId = TestUtils.getSummaryCredentialId(wallet.getDid(), holdersCredentialRepository);
        ResponseEntity<String> response = issueDismantlerCredential(miwSettings.authorityWalletBpn(), miwSettings.authorityWalletBpn());
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(miwSettings.authorityWalletDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL);
        Assertions.assertFalse(credentials.isEmpty());
        Assertions.assertTrue(credentials.get(0).isSelfIssued()); //self issued must be false
        Assertions.assertFalse(credentials.get(0).isStored()); //stored must be false
        //check summary credential
        TestUtils.checkSummaryCredential(miwSettings.authorityWalletDid(), wallet.getDid(), holdersCredentialRepository, issuersCredentialRepository, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL, oldSummaryCredentialId);
    }


    @Test
    void issueDismantlerCredentialTest201() throws JsonProcessingException, JSONException {

        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String baseBpn = miwSettings.authorityWalletBpn();

        //create wallet
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, bpn, restTemplate, baseBpn).getBody());
        String oldSummaryCredentialId = TestUtils.getSummaryCredentialId(wallet.getDid(), holdersCredentialRepository);

        ResponseEntity<String> response = issueDismantlerCredential(bpn, did);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(response.getBody(), Map.class);
        VerifiableCredential verifiableCredential = new VerifiableCredential(map);
        Assertions.assertTrue(verifiableCredential.getTypes().contains(MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL));

        TestUtils.checkVC(verifiableCredential, miwSettings);


        Assertions.assertEquals(StringPool.VEHICLE_DISMANTLE, verifiableCredential.getCredentialSubject().get(0).get(StringPool.ACTIVITY_TYPE).toString());

        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(wallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL);
        Assertions.assertFalse(credentials.isEmpty());
        TestUtils.checkVC(credentials.get(0).getData(), miwSettings);
        Assertions.assertFalse(credentials.get(0).isSelfIssued()); //self issued must be false
        Assertions.assertFalse(credentials.get(0).isStored()); //stored must be false

        VerifiableCredential data = credentials.get(0).getData();

        Assertions.assertEquals(StringPool.VEHICLE_DISMANTLE, data.getCredentialSubject().get(0).get(StringPool.ACTIVITY_TYPE).toString());

        //check in issuer wallet
        List<IssuersCredential> issuerVCs = issuersCredentialRepository.getByIssuerDidAndHolderDidAndType(miwSettings.authorityWalletDid(), wallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL);
        Assertions.assertEquals(1, issuerVCs.size());
        TestUtils.checkVC(issuerVCs.get(0).getData(), miwSettings);
        Assertions.assertEquals(StringPool.VEHICLE_DISMANTLE, issuerVCs.get(0).getData().getCredentialSubject().get(0).get(StringPool.ACTIVITY_TYPE).toString());

        //check summary credential
        TestUtils.checkSummaryCredential(miwSettings.authorityWalletDid(), wallet.getDid(), holdersCredentialRepository, issuersCredentialRepository, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL, oldSummaryCredentialId);
    }

    @Test
    void issueDismantlerCredentialWithInvalidBpnAccess409() {
        String bpn = TestUtils.getRandomBpmNumber();

        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();

        //create entry
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn); //token must contain base wallet BPN

        IssueDismantlerCredentialRequest request = IssueDismantlerCredentialRequest.builder()
                .activityType(StringPool.VEHICLE_DISMANTLE)
                .bpn(bpn)
                .allowedVehicleBrands(Set.of("BMW"))
                .build();


        HttpEntity<IssueDismantlerCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_DISMANTLER, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());

    }

    @Test
    void issueDismantlerCredentialWithoutAllowedVehicleBrands() {
        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn()); //token must contain base wallet BPN

        IssueDismantlerCredentialRequest request = IssueDismantlerCredentialRequest.builder()
                .activityType(StringPool.VEHICLE_DISMANTLE)
                .bpn(bpn)
                .allowedVehicleBrands(null)
                .build();

        HttpEntity<IssueDismantlerCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_DISMANTLER, HttpMethod.POST, entity, String.class);

        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
    }

    @Test
    void issueDismantlerCredentialWithDuplicateBpn409() {

        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();

        //create entry
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);
        ResponseEntity<String> response = issueDismantlerCredential(bpn, did);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        //issue duplicate
        ResponseEntity<String> duplicateResponse = issueDismantlerCredential(bpn, did);
        Assertions.assertEquals(HttpStatus.CONFLICT.value(), duplicateResponse.getStatusCode().value());
    }


    private ResponseEntity<String> issueDismantlerCredential(String bpn, String did) {


        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn()); //token must contain base wallet BPN

        IssueDismantlerCredentialRequest request = IssueDismantlerCredentialRequest.builder()
                .activityType(StringPool.VEHICLE_DISMANTLE)
                .bpn(bpn)
                .allowedVehicleBrands(Set.of("BMW"))
                .build();


        HttpEntity<IssueDismantlerCredentialRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_DISMANTLER, HttpMethod.POST, entity, String.class);
    }
}