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
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ContextConfiguration(initializers = {TestContextInitializer.class})
class IssuersCredentialTest {

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
    private IssuersCredentialRepository issuersCredentialRepository;


    @Test
    void getCredentials200() throws com.fasterxml.jackson.core.JsonProcessingException {
        String baseBPN = miwSettings.authorityWalletBpn();
        String holderBpn = TestUtils.getRandomBpmNumber();
        String holderDID = "did:web:localhost:" + holderBpn;
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(baseBPN);
        //save wallet
        TestUtils.createWallet(holderBpn, holderDID, walletRepository);
        TestUtils.issueMembershipVC(restTemplate, holderBpn, baseBPN);
        String vcList = """
                [
                {"type":"TraceabilityCredential"},
                {"type":"SustainabilityCredential"},
                {"type":"ResiliencyCredential"},
                {"type":"QualityCredential"},
                {"type":"PcfCredential"}
                ]
                """;
        JSONArray jsonArray = new JSONArray(vcList);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            IssueFrameworkCredentialRequest request = TestUtils.getIssueFrameworkCredentialRequest(holderBpn, jsonObject.get(StringPool.TYPE).toString());
            HttpEntity<IssueFrameworkCredentialRequest> entity = new HttpEntity<>(request, AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn())); //ony base wallet can issue VC
            ResponseEntity<String> exchange = restTemplate.exchange(RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, HttpMethod.POST, entity, String.class);
            Assertions.assertEquals(exchange.getStatusCode().value(), HttpStatus.CREATED.value());
        }

        HttpEntity<Map> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?holderIdentifier={did}"
                , HttpMethod.GET, entity, String.class, holderDID);

        List<VerifiableCredential> credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);

        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(12, Objects.requireNonNull(credentialList).size());  //5 framework CV + 1 membership + 6 Summary VC


        response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?credentialId={id}"
                , HttpMethod.GET, entity, String.class, credentialList.get(0).getId());
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(1, Objects.requireNonNull(credentialList).size());

        List<String> list = new ArrayList<>();
        list.add(MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL);
        response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?type={list}"
                , HttpMethod.GET, entity, String.class, String.join(",", list));
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        //all VC must be type of MEMBERSHIP_CREDENTIAL_CX
        credentialList.forEach(vc -> {
            Assertions.assertTrue(vc.getTypes().contains(MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL));
        });

        list = new ArrayList<>();
        list.add(MIWVerifiableCredentialType.SUMMARY_CREDENTIAL);
        response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?type={list}&holderIdentifier={did}"
                , HttpMethod.GET, entity, String.class, String.join(",", list), holderDID);
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(6, Objects.requireNonNull(credentialList).size()); //5 framework CV + 1 membership

        for (VerifiableCredential vc : credentialList) {
            Assertions.assertEquals(3, vc.getContext().size(), "Each credential requires 3 contexts");
        }
    }


    @Test
    void issueCredentialsTestWithInvalidRole403() {

        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS, HttpMethod.POST, entity, Map.class);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void issueCredentialsWithoutBaseWalletBPN403() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String holderDid = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        ResponseEntity<String> response = issueVC(bpn, holderDid, holderDid, type, headers);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void issueCredentialsToBaseWallet200() throws JsonProcessingException {
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());

        ResponseEntity<String> response = issueVC(miwSettings.authorityWalletBpn(), miwSettings.authorityWalletDid(), miwSettings.authorityWalletDid(), type, headers);

        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        VerifiableCredential verifiableCredential = new VerifiableCredential(new ObjectMapper().readValue(response.getBody(), Map.class));
        Assertions.assertNotNull(verifiableCredential.getProof());

        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(miwSettings.authorityWalletDid(), type);
        Assertions.assertFalse(credentials.isEmpty());
        Assertions.assertFalse(credentials.get(0).isStored());  //stored must be false
        Assertions.assertTrue(credentials.get(0).isSelfIssued());  //stored must be true
    }


    @Test
    void issueSummaryCredentials400() throws com.fasterxml.jackson.core.JsonProcessingException {

        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());

        ResponseEntity<String> response = issueVC(bpn, did, miwSettings.authorityWalletDid(), MIWVerifiableCredentialType.SUMMARY_CREDENTIAL, headers);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
    }

    @Test
    void issueCredentials200() throws com.fasterxml.jackson.core.JsonProcessingException {

        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());

        ResponseEntity<String> response = issueVC(bpn, did, miwSettings.authorityWalletDid(), type, headers);

        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        VerifiableCredential verifiableCredential = new VerifiableCredential(new ObjectMapper().readValue(response.getBody(), Map.class));
        Assertions.assertNotNull(verifiableCredential.getProof());

        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(did, type);
        Assertions.assertFalse(credentials.isEmpty());
        TestUtils.checkVC(credentials.get(0).getData(), miwSettings);
        Assertions.assertFalse(credentials.get(0).isStored());  //stored must be false
        Assertions.assertFalse(credentials.get(0).isSelfIssued());  //stored must be false

        //check is it is stored in issuer wallet
        List<IssuersCredential> issuersCredentials = issuersCredentialRepository.getByIssuerDidAndHolderDidAndType(miwSettings.authorityWalletDid(), did, type);
        Assertions.assertEquals(1, issuersCredentials.size());
        Assertions.assertEquals(type, issuersCredentials.get(0).getType());
    }


    private ResponseEntity<String> issueVC(String bpn, String holderDid, String issuerDid, String type, HttpHeaders headers) throws JsonProcessingException {
        String baseBpn = miwSettings.authorityWalletBpn();
        //save wallet
        TestUtils.createWallet(bpn, holderDid, restTemplate, baseBpn);

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
                        .id(URI.create(issuerDid + "#" + UUID.randomUUID()))
                        .context(miwSettings.vcContexts())
                        .type(List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, type))
                        .issuer(URI.create(issuerDid)) //issuer must be base wallet
                        .expirationDate(miwSettings.vcExpiryDate().toInstant())
                        .issuanceDate(Instant.now())
                        .credentialSubject(verifiableCredentialSubject)
                        .build();

        Map<String, Objects> map = objectMapper.readValue(credentialWithoutProof.toJson(), Map.class);
        HttpEntity<Map> entity = new HttpEntity<>(map, headers);
        return restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?holderDid={did}", HttpMethod.POST, entity, String.class, holderDid);
    }

}
