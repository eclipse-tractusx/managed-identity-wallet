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
import org.eclipse.tractusx.managedidentitywallets.controller.IssuersCredentialController;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.eclipse.tractusx.ssi.lib.resolver.DidDocumentResolverRegistryImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ActiveProfiles("test")
@ContextConfiguration(initializers = {TestContextInitializer.class})
@ExtendWith(MockitoExtension.class)
class HoldersCredentialTest {

    @Autowired
    private HoldersCredentialRepository holdersCredentialRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MIWSettings miwSettings;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IssuersCredentialController credentialController;


    @Test
    void issueCredentialTestWithInvalidBPNAccess403() throws JsonProcessingException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders("not valid BPN");

        ResponseEntity<String> response = issueVC(bpn, did, type, headers);


        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    void issueCredentialTest200() throws JsonProcessingException {
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        ResponseEntity<String> response = issueVC(bpn, did, type, headers);


        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        VerifiableCredential verifiableCredential = new VerifiableCredential(new ObjectMapper().readValue(response.getBody(), Map.class));
        Assertions.assertNotNull(verifiableCredential.getProof());

        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(did, type);
        Assertions.assertFalse(credentials.isEmpty());
        TestUtils.checkVC(credentials.get(0).getData(), miwSettings);
        Assertions.assertTrue(credentials.get(0).isSelfIssued());

    }


    @Test
    void getCredentialsTest403() {

        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(RestURI.CREDENTIALS, HttpMethod.GET, entity, Map.class);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void getCredentials200() throws com.fasterxml.jackson.core.JsonProcessingException {


        String baseDID = miwSettings.authorityWalletDid();
        String bpn = UUID.randomUUID().toString();
        String did = "did:web:localhost:" + bpn;
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);
        //save wallet
        TestUtils.createWallet(bpn, did, walletRepository);
        TestUtils.issueMembershipVC(restTemplate, bpn, miwSettings.authorityWalletBpn());
        String vcList = """
                [
                {"type":"cx-traceability","value":"ID_3.0_Trace"},
                {"type":"cx-sustainability","value":"Sustainability"},
                {"type":"cx-resiliency","value":"Resiliency"},
                {"type":"cx-quality","value":"Quality"},
                {"type":"cx-pcf","value":"PCF"}
                ]
                """;
        JSONArray jsonArray = new JSONArray(vcList);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            IssueFrameworkCredentialRequest request = TestUtils.getIssueFrameworkCredentialRequest(bpn, jsonObject.get("type").toString(), jsonObject.get("value").toString());
            HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(request, AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn())); //ony base wallet can issue VC
            ResponseEntity<String> exchange = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
            Assertions.assertEquals(exchange.getStatusCode().value(), HttpStatus.CREATED.value());
        }


        HttpEntity<Map> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS + "?issuerIdentifier={did}"
                , HttpMethod.GET, entity, String.class, baseDID);
        List<VerifiableCredential> credentialList = getCredentialsFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(6, Objects.requireNonNull(credentialList).size());

        response = restTemplate.exchange(RestURI.CREDENTIALS + "?credentialId={id}"
                , HttpMethod.GET, entity, String.class, credentialList.get(0).getId());
        credentialList = getCredentialsFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(1, Objects.requireNonNull(credentialList).size());

        List<String> list = new ArrayList<>();
        list.add(MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX);
        response = restTemplate.exchange(RestURI.CREDENTIALS + "?type={list}"
                , HttpMethod.GET, entity, String.class, String.join(",", list));
        credentialList = getCredentialsFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(1, Objects.requireNonNull(credentialList).size());
    }


    @Test
    void validateCredentialsWithInvalidVC() throws com.fasterxml.jackson.core.JsonProcessingException {
        //data setup
        Map<String, Object> map = issueVC();

        //service call
        try (MockedStatic<LinkedDataProofValidation> utils = Mockito.mockStatic(LinkedDataProofValidation.class)) {

            //mock setup
            LinkedDataProofValidation mock = Mockito.mock(LinkedDataProofValidation.class);
            utils.when(() -> {
                LinkedDataProofValidation.newInstance(Mockito.any(DidDocumentResolverRegistryImpl.class));
            }).thenReturn(mock);
            Mockito.when(mock.checkProof(Mockito.any(VerifiableCredential.class))).thenReturn(false);

            Map<String, Object> stringObjectMap = credentialController.credentialsValidation(map).getBody();
            Assertions.assertFalse(Boolean.parseBoolean(stringObjectMap.get("valid").toString()));
        }
    }


    @Test
    void validateCredentials() throws com.fasterxml.jackson.core.JsonProcessingException {

        //data setup
        Map<String, Object> map = issueVC();


        //service call
        try (MockedStatic<LinkedDataProofValidation> utils = Mockito.mockStatic(LinkedDataProofValidation.class)) {

            //mock setup
            LinkedDataProofValidation mock = Mockito.mock(LinkedDataProofValidation.class);
            utils.when(() -> {
                LinkedDataProofValidation.newInstance(Mockito.any(DidDocumentResolverRegistryImpl.class));
            }).thenReturn(mock);
            Mockito.when(mock.checkProof(Mockito.any(VerifiableCredential.class))).thenReturn(true);

            Map<String, Object> stringObjectMap = credentialController.credentialsValidation(map).getBody();
            Assertions.assertTrue(Boolean.parseBoolean(stringObjectMap.get("valid").toString()));
        }
    }


    private List<VerifiableCredential> getCredentialsFromString(String body) throws com.fasterxml.jackson.core.JsonProcessingException {
        List<VerifiableCredential> credentialList = new ArrayList<>();

        JSONArray array = new JSONArray(body);
        if (array.length() == 0) {
            return credentialList;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            ObjectMapper objectMapper = new ObjectMapper();
            credentialList.add(new VerifiableCredential(objectMapper.readValue(jsonObject.toString(), Map.class)));
        }
        return credentialList;
    }

    private Map<String, Object> issueVC() throws JsonProcessingException {
        String bpn = UUID.randomUUID().toString();
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);
        TestUtils.createWallet(bpn, "Test", restTemplate);
        ResponseEntity<String> vc = TestUtils.issueMembershipVC(restTemplate, bpn, miwSettings.authorityWalletBpn());
        VerifiableCredential verifiableCredential = new VerifiableCredential(new ObjectMapper().readValue(vc.getBody(), Map.class));
        Map<String, Object> map = objectMapper.readValue(verifiableCredential.toJson(), Map.class);
        return map;
    }


    private ResponseEntity<String> issueVC(String bpn, String did, String type, HttpHeaders headers) throws JsonProcessingException {
        //save wallet
        TestUtils.createWallet(bpn, did, restTemplate);

        // Create VC without proof
        //VC Bulider
        VerifiableCredentialBuilder verifiableCredentialBuilder =
                new VerifiableCredentialBuilder();

        //VC Subject
        VerifiableCredentialSubject verifiableCredentialSubject =
                new VerifiableCredentialSubject(Map.of("test", "test"));

        //Using Builder
        VerifiableCredential credentialWithoutProof =
                verifiableCredentialBuilder
                        .id(URI.create(UUID.randomUUID().toString()))
                        .context(miwSettings.vcContexts())
                        .type(List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, type))
                        .issuer(URI.create(did)) //issuer must be base wallet
                        .expirationDate(miwSettings.vcExpiryDate().toInstant())
                        .issuanceDate(Instant.now())
                        .credentialSubject(verifiableCredentialSubject)
                        .build();

        Map<String, Objects> map = objectMapper.readValue(credentialWithoutProof.toJson(), Map.class);
        HttpEntity<Map> entity = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS, HttpMethod.POST, entity, String.class);
        return response;
    }
}
