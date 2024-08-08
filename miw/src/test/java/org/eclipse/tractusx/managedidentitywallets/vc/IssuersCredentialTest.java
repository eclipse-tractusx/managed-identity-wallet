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


package org.eclipse.tractusx.managedidentitywallets.vc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teketik.test.mockinbean.MockInBean;
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.CredentialStatus;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.RevocationSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.IssuersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.revocation.RevocationClient;
import org.eclipse.tractusx.managedidentitywallets.service.revocation.RevocationService;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.serialization.SerializeUtil;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool.COLON_SEPARATOR;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
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

    @Autowired
    private RevocationSettings revocationSettings;


    @MockInBean(RevocationService.class)
    private RevocationClient revocationClient;

    @SneakyThrows
    @BeforeEach
    void beforeEach() {
        TestUtils.mockGetStatusListEntry(revocationClient);
        TestUtils.mockGetStatusListVC(revocationClient, objectMapper, TestUtils.createEncodedList());
        TestUtils.mockRevocationVerification(revocationClient, CredentialStatus.ACTIVE);
    }

    @AfterEach
    void afterEach() {
        Mockito.reset(revocationClient);
    }

    @Test
    void getCredentials200() throws com.fasterxml.jackson.core.JsonProcessingException, JSONException {
        String baseBPN = miwSettings.authorityWalletBpn();
        String holderBpn = TestUtils.getRandomBpmNumber();
        String holderDID = "did:web:localhost:" + holderBpn;
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(baseBPN);
        //save wallet
        Wallet wallet = TestUtils.createWallet(holderBpn, holderDID, walletRepository);

        //issue some VCs
        List<String> typesOfVcs = List.of("Type1", "Type2", "Type3");
        typesOfVcs.forEach(type -> {
            TestUtils.issueCustomVCUsingBaseWallet(holderBpn, wallet.getDid(), miwSettings.authorityWalletDid(), type, AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn()), miwSettings, objectMapper, restTemplate);
        });


        HttpEntity<Map> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?holderIdentifier={did}"
                , HttpMethod.GET, entity, String.class, holderDID);

        List<VerifiableCredential> credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);

        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(typesOfVcs.size(), Objects.requireNonNull(credentialList).size());


        response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?credentialId={id}"
                , HttpMethod.GET, entity, String.class, credentialList.get(0).getId());
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(1, Objects.requireNonNull(credentialList).size());


        response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?type={list}&holderIdentifier={holderIdentifier}"
                , HttpMethod.GET, entity, String.class, String.join(",", typesOfVcs), wallet.getBpn());
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        //here we at getting VCs from issuer table, it will have double entry
        Assertions.assertEquals(typesOfVcs.size(), Objects.requireNonNull(credentialList).size());


        response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?type={list}&holderIdentifier={holderIdentifier}"
                , HttpMethod.GET, entity, String.class, typesOfVcs.get(0), wallet.getBpn());
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(1, Objects.requireNonNull(credentialList).size());

    }

    @Test
    @DisplayName("Get Credentials as JWT")
    void getCredentialsAsJWT200() throws JSONException {
        String baseBPN = miwSettings.authorityWalletBpn();
        String holderBpn = TestUtils.getRandomBpmNumber();
        String holderDID = "did:web:localhost:" + holderBpn;
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(baseBPN);
        //save wallet
        Wallet wallet = TestUtils.createWallet(holderBpn, holderDID, walletRepository);

        //create test data
        List<String> typesOfVcs = List.of("Type1", "Type2", "Type3");
        typesOfVcs.forEach(type -> {
            TestUtils.issueCustomVCUsingBaseWallet(holderBpn, wallet.getDid(), miwSettings.authorityWalletDid(), type, AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn()), miwSettings, objectMapper, restTemplate);
        });

        HttpEntity<Map> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?holderIdentifier={did}&asJwt=true"
                , HttpMethod.GET, entity, String.class, holderDID);

        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Map<String, Object> responseMap = SerializeUtil.fromJson(response.getBody());
        List<Map<String, Object>> vcsAsJwt = (ArrayList<Map<String, Object>>) responseMap.get("content");
        //5 framework CV + 1 membership + 6 Summary VC
        Assertions.assertEquals(typesOfVcs.size(), vcsAsJwt.size());
        vcsAsJwt.forEach(vc -> {
            Assertions.assertNotNull(vc.get(StringPool.VC_JWT_KEY));

        });
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

        String baseBpn = miwSettings.authorityWalletBpn();
        //save wallet
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        TestUtils.createWallet(bpn, holderDid, restTemplate, baseBpn, defaultLocation);


        Assertions.assertThrows(ForbiddenException.class, () -> {
            TestUtils.issueCustomVCUsingBaseWallet(bpn, holderDid, holderDid, type, headers, miwSettings, objectMapper, restTemplate);
        });
    }

    @Test
    void issueCredentialsToBaseWallet200() throws JsonProcessingException {
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());

        String baseBpn = miwSettings.authorityWalletBpn();
        String bpn = TestUtils.getRandomBpmNumber();
        //save wallet
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        TestUtils.createWallet(bpn, bpn, restTemplate, baseBpn, defaultLocation);

        VerifiableCredential verifiableCredential = TestUtils.issueCustomVCUsingBaseWallet(baseBpn, miwSettings.authorityWalletDid(), miwSettings.authorityWalletDid(), type, headers, miwSettings, objectMapper, restTemplate);

        Assertions.assertNotNull(verifiableCredential.getProof());
        Assertions.assertNotNull(verifiableCredential.getVerifiableCredentialStatus());

        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(miwSettings.authorityWalletDid(), type);
        Assertions.assertFalse(credentials.isEmpty());
        Assertions.assertFalse(credentials.get(0).isStored());  //stored must be false
        Assertions.assertTrue(credentials.get(0).isSelfIssued());  //stored must be true
    }

    @Test
    void issueCredentials200() throws com.fasterxml.jackson.core.JsonProcessingException {

        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());

        String baseBpn = miwSettings.authorityWalletBpn();
        //save wallet
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        TestUtils.createWallet(bpn, bpn, restTemplate, baseBpn, defaultLocation);

        VerifiableCredential verifiableCredential = TestUtils.issueCustomVCUsingBaseWallet(bpn, did, miwSettings.authorityWalletDid(), type, headers, miwSettings, objectMapper, restTemplate);

        Assertions.assertNotNull(verifiableCredential.getProof());

        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(did, type);
        Assertions.assertFalse(credentials.isEmpty());
        TestUtils.checkVC(credentials.get(0).getData(), miwSettings, revocationSettings);
        Assertions.assertFalse(credentials.get(0).isStored());  //stored must be false
        Assertions.assertFalse(credentials.get(0).isSelfIssued());  //stored must be false

        //check is it is stored in issuer wallet
        List<IssuersCredential> issuersCredentials = issuersCredentialRepository.getByIssuerDidAndHolderDidAndType(miwSettings.authorityWalletDid(), did, type);
        Assertions.assertEquals(1, issuersCredentials.size());
        Assertions.assertEquals(type, issuersCredentials.get(0).getType());
    }


}
