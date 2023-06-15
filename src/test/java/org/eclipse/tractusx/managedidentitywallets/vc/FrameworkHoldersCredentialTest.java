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
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ActiveProfiles("test")
@ContextConfiguration(initializers = {TestContextInitializer.class})
class FrameworkHoldersCredentialTest {
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

    private static int count = 0;


    @Test
    void issueFrameworkCredentialTest403() {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        IssueMembershipCredentialRequest request = IssueMembershipCredentialRequest.builder().bpn(bpn).build();

        HttpEntity<IssueMembershipCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<VerifiableCredential> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, VerifiableCredential.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    void issueFrameworkCredentialWithInvalidBpnAccessTest403() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        TestUtils.createWallet(bpn, did, walletRepository);

        String type = "BehaviorTwinCredential";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        IssueFrameworkCredentialRequest twinRequest = TestUtils.getIssueFrameworkCredentialRequest(bpn, type);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void issueFrameWorkVCToBaseWalletTest201() throws JSONException, JsonProcessingException {
        String bpn = miwSettings.authorityWalletBpn();
        String type = "PcfCredential";
        //create wallet
        Wallet wallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        String oldSummaryCredentialId = TestUtils.getSummaryCredentialId(wallet.getDid(), holdersCredentialRepository);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());

        IssueFrameworkCredentialRequest twinRequest = TestUtils.getIssueFrameworkCredentialRequest(bpn, type);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(miwSettings.authorityWalletDid(), MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX);
        Assertions.assertFalse(credentials.isEmpty());

        VerifiableCredential vcFromDB = credentials.get(0).getData();
        TestUtils.checkVC(vcFromDB, miwSettings);

        Assertions.assertFalse(credentials.get(0).isStored()); //stored must be false
        Assertions.assertTrue(credentials.get(0).isSelfIssued()); //self issue must be false

        //check summary credential
        TestUtils.checkSummaryCredential(miwSettings.authorityWalletDid(), wallet.getDid(), holdersCredentialRepository, issuersCredentialRepository, type, oldSummaryCredentialId);
    }

    @ParameterizedTest
    @MethodSource("getTypes")
    void issueFrameWorkVCTest201(IssueFrameworkCredentialRequest request) throws JsonProcessingException, JSONException {
        String bpn = request.getHolderIdentifier();
        String did = "did:web:localhost:" + bpn;

        String type = request.getType();

        createAndValidateVC(bpn, did, type);
        //check in issuer tables
        List<IssuersCredential> issuerVCs = issuersCredentialRepository.getByIssuerDidAndHolderDidAndType(miwSettings.authorityWalletDid(), did, MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX);
        Assertions.assertEquals(1, issuerVCs.size());
    }

    static Stream<IssueFrameworkCredentialRequest> getTypes() {
        return Stream.of(
                IssueFrameworkCredentialRequest.builder().holderIdentifier(UUID.randomUUID().toString()).type("BehaviorTwinCredential").build(),
                IssueFrameworkCredentialRequest.builder().holderIdentifier(UUID.randomUUID().toString()).type("PcfCredential").build(),
                IssueFrameworkCredentialRequest.builder().holderIdentifier(UUID.randomUUID().toString()).type("QualityCredential").build(),
                IssueFrameworkCredentialRequest.builder().holderIdentifier(UUID.randomUUID().toString()).type("ResiliencyCredential").build(),
                IssueFrameworkCredentialRequest.builder().holderIdentifier(UUID.randomUUID().toString()).type("SustainabilityCredential").build(),
                IssueFrameworkCredentialRequest.builder().holderIdentifier(UUID.randomUUID().toString()).type("TraceabilityCredential").build()
        );
    }


    @Test
    @DisplayName("Issue framework with invalid type")
    void issueFrameworkCredentialTest400() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);


        String type = "cx-traceability1";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());

        IssueFrameworkCredentialRequest twinRequest = TestUtils.getIssueFrameworkCredentialRequest(bpn, type);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());

    }

    private void createAndValidateVC(String bpn, String did, String type) throws JsonProcessingException {
        //create wallet
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, bpn, restTemplate).getBody());
        String oldSummaryCredentialId = TestUtils.getSummaryCredentialId(wallet.getDid(), holdersCredentialRepository);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());

        IssueFrameworkCredentialRequest twinRequest = TestUtils.getIssueFrameworkCredentialRequest(bpn, type);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        validate(wallet, type, response, miwSettings, oldSummaryCredentialId);

    }

    private void validate(Wallet wallet, String type, ResponseEntity<String> response, MIWSettings miwSettings, String oldSummaryCredentialId) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(response.getBody(), Map.class);
        VerifiableCredential verifiableCredential = new VerifiableCredential(map);
        Assertions.assertTrue(verifiableCredential.getTypes().contains(MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX));

        TestUtils.checkVC(verifiableCredential, miwSettings);

        Assertions.assertEquals(verifiableCredential.getCredentialSubject().get(0).get(StringPool.TYPE), type);
        Assertions.assertEquals(verifiableCredential.getCredentialSubject().get(0).get(StringPool.ID), wallet.getDid());

        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(wallet.getDid(), MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX);
        Assertions.assertFalse(credentials.isEmpty());

        VerifiableCredential vcFromDB = credentials.get(0).getData();
        TestUtils.checkVC(vcFromDB, miwSettings);

        Assertions.assertFalse(credentials.get(0).isStored()); //stored must be false
        Assertions.assertFalse(credentials.get(0).isSelfIssued()); //self issue must be false
        Assertions.assertEquals(vcFromDB.getCredentialSubject().get(0).get(StringPool.TYPE), type);
        Assertions.assertEquals(vcFromDB.getCredentialSubject().get(0).get(StringPool.ID), wallet.getDid());

        //check summary credential
        TestUtils.checkSummaryCredential(miwSettings.authorityWalletDid(), wallet.getDid(), holdersCredentialRepository, issuersCredentialRepository, type, oldSummaryCredentialId);
    }
}