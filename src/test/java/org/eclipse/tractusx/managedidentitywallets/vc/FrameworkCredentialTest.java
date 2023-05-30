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
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ActiveProfiles("test")
@ContextConfiguration(initializers = {TestContextInitializer.class})
public class FrameworkCredentialTest {
    @Autowired
    private CredentialRepository credentialRepository;
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletKeyRepository walletKeyRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MIWSettings miwSettings;


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
    void issueBehaviorTwinCredentialTest201() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);

        String type ="cx-behavior-twin";
        String value ="Behavior Twin";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        IssueFrameworkCredentialRequest twinRequest = getIssueFrameworkCredentialRequest(bpn, type, value);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        validate(wallet, type, value, response, miwSettings);
    }


    @Test
    void issuePCFCredentialTest201() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);

        String type ="cx-pcf";
        String value ="PCF";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        IssueFrameworkCredentialRequest twinRequest = getIssueFrameworkCredentialRequest(bpn, type, value);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        validate(wallet, type, value, response, miwSettings);
    }

    @Test
    void issueQualityCredentialTest201() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);

        String type ="cx-quality";
        String value ="Quality";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        IssueFrameworkCredentialRequest twinRequest = getIssueFrameworkCredentialRequest(bpn, type, value);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        validate(wallet, type, value, response, miwSettings);
    }

    @Test
    void issueResiliencyCredentialTest201() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);

        String type ="cx-resiliency";
        String value ="Resiliency";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        IssueFrameworkCredentialRequest twinRequest = getIssueFrameworkCredentialRequest(bpn, type, value);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        validate(wallet, type, value, response, miwSettings);
    }

    @Test
    void issueSustainabilityCredentialTest201() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);

        String type ="cx-sustainability";
        String value ="Sustainability";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        IssueFrameworkCredentialRequest twinRequest = getIssueFrameworkCredentialRequest(bpn, type, value);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        validate(wallet, type, value, response, miwSettings);
    }


    @Test
    void issueTraceabilityCredentialTest201() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);

        String type ="cx-traceability";
        String value ="ID_3.0_Trace";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        IssueFrameworkCredentialRequest twinRequest = getIssueFrameworkCredentialRequest(bpn, type, value);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        validate(wallet, type, value, response, miwSettings);
    }

    @Test
    void issueFrameworkCredentialTest400() throws JsonProcessingException, JSONException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        Wallet wallet = TestUtils.createWallet(bpn, did, walletRepository);


        String type ="cx-traceability1";
        String value ="ID_3.0_Trace1";

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        IssueFrameworkCredentialRequest twinRequest = getIssueFrameworkCredentialRequest(bpn, type, value);

        HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(twinRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());

    }

    private void validate(Wallet wallet, String type, String value, ResponseEntity<String> response, MIWSettings miwSettings) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(response.getBody(), Map.class);
        VerifiableCredential verifiableCredential = new VerifiableCredential(map);
        Assertions.assertTrue(verifiableCredential.getTypes().contains(MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX));

        TestUtils.checkVC(verifiableCredential, miwSettings);

        Assertions.assertTrue(verifiableCredential.getCredentialSubject().get(0).get("type").equals(type));
        Assertions.assertTrue(verifiableCredential.getCredentialSubject().get(0).get("value").equals(value));
        Assertions.assertTrue(verifiableCredential.getCredentialSubject().get(0).get("id").equals(wallet.getDid()));

        Credential credential = credentialRepository.getByHolderDidAndType(wallet.getDid(), MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX);
        Assertions.assertNotNull(credential);

        VerifiableCredential vcFromDB = credential.getData();
        TestUtils.checkVC(vcFromDB, miwSettings);

        Assertions.assertTrue(vcFromDB.getCredentialSubject().get(0).get("type").equals(type));
        Assertions.assertTrue(vcFromDB.getCredentialSubject().get(0).get("value").equals(value));
        Assertions.assertTrue(vcFromDB.getCredentialSubject().get(0).get("id").equals(wallet.getDid()));
    }

    private static IssueFrameworkCredentialRequest getIssueFrameworkCredentialRequest(String bpn, String type, String value) {
        IssueFrameworkCredentialRequest twinRequest = IssueFrameworkCredentialRequest.builder()
                .contractTemplate("http://localhost")
                .contractVersion("v1")
                .type(type)
                .value(value)
                .bpn(bpn)
                .build();
        return twinRequest;
    }
}
