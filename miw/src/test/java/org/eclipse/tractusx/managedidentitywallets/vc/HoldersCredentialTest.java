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
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.RevocationSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.controller.IssuersCredentialController;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialVerificationRequest;
import org.eclipse.tractusx.managedidentitywallets.revocation.RevocationClient;
import org.eclipse.tractusx.managedidentitywallets.service.revocation.RevocationService;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.exception.did.DidParseException;
import org.eclipse.tractusx.ssi.lib.exception.json.TransformJsonLdException;
import org.eclipse.tractusx.ssi.lib.exception.key.InvalidPublicKeyFormatException;
import org.eclipse.tractusx.ssi.lib.exception.proof.NoVerificationKeyFoundException;
import org.eclipse.tractusx.ssi.lib.exception.proof.SignatureParseException;
import org.eclipse.tractusx.ssi.lib.exception.proof.SignatureVerificationFailedException;
import org.eclipse.tractusx.ssi.lib.exception.proof.UnsupportedSignatureTypeException;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.eclipse.tractusx.ssi.lib.serialization.SerializeUtil;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
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
    private RevocationSettings revocationSettings;

    @MockInBean(RevocationService.class)
    private RevocationClient revocationClient;

    @Autowired
    private IssuersCredentialController credentialController;

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
    void issueCredentialTestWithInvalidBPNAccess403() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders("not valid BPN");

        ResponseEntity<String> response = issueVC(bpn, did, type, headers);


        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    void issueCredentialTest200() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String type = "TestCredential";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        ResponseEntity<String> response = issueVC(bpn, did, type, headers);

        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        VerifiableCredential verifiableCredential = new VerifiableCredential(new ObjectMapper().readValue(response.getBody(), Map.class));
        Assertions.assertNotNull(verifiableCredential.getProof());
        Assertions.assertNotNull(verifiableCredential.getVerifiableCredentialStatus());


        List<HoldersCredential> credentials = holdersCredentialRepository.getByHolderDidAndType(did, type);
        Assertions.assertFalse(credentials.isEmpty());
        TestUtils.checkVC(credentials.get(0).getData(), miwSettings, revocationSettings);
        Assertions.assertTrue(credentials.get(0).isSelfIssued());
        Assertions.assertFalse(credentials.get(0).isStored());
    }


    @Test
    void getCredentialsTest403() {

        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(RestURI.CREDENTIALS, HttpMethod.GET, entity, Map.class);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void getCredentials200() throws com.fasterxml.jackson.core.JsonProcessingException, JSONException {


        String baseDID = miwSettings.authorityWalletDid();
        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);
        //save wallet
        TestUtils.createWallet(bpn, did, walletRepository);

        List<VerifiableCredential> vcs = new ArrayList<>();
        List<String> typesOfVcs = List.of("Type1", "Type2", "Type3");

        typesOfVcs.forEach(type -> {
            VerifiableCredential verifiableCredential = TestUtils.issueCustomVCUsingBaseWallet(bpn, did, miwSettings.authorityWalletDid(), type, AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn()), miwSettings, objectMapper, restTemplate);
            vcs.add(verifiableCredential);
        });

        HttpEntity<Map> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS + "?issuerIdentifier={did}"
                , HttpMethod.GET, entity, String.class, baseDID);
        List<VerifiableCredential> credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(typesOfVcs.size(), Objects.requireNonNull(credentialList).size());

        response = restTemplate.exchange(RestURI.CREDENTIALS + "?credentialId={id}"
                , HttpMethod.GET, entity, String.class, credentialList.get(0).getId());
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(1, Objects.requireNonNull(credentialList).size());

        response = restTemplate.exchange(RestURI.CREDENTIALS + "?type={list}"
                , HttpMethod.GET, entity, String.class, String.join(",", typesOfVcs));
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(typesOfVcs.size(), Objects.requireNonNull(credentialList).size());


        //test get by type
        String type = typesOfVcs.get(0);
        response = restTemplate.exchange(RestURI.CREDENTIALS + "?type={list}"
                , HttpMethod.GET, entity, String.class, type);
        credentialList = TestUtils.getVerifiableCredentials(response, objectMapper);
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertEquals(1, Objects.requireNonNull(credentialList).size());
    }


    @Test
    @DisplayName("Get Credentials as JWT")
    void getCredentialsAsJWT200() throws JSONException {

        String baseDID = miwSettings.authorityWalletDid();
        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);
        // save wallet
        TestUtils.createWallet(bpn, did, walletRepository);

        //issue VC : test data
        List<VerifiableCredential> vcs = new ArrayList<>();
        List<String> typesOfVcs = List.of("Type1", "Type2", "Type3");

        typesOfVcs.forEach(type -> {
            VerifiableCredential verifiableCredential = TestUtils.issueCustomVCUsingBaseWallet(bpn, did, miwSettings.authorityWalletDid(), type, AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn()), miwSettings, objectMapper, restTemplate);
            vcs.add(verifiableCredential);
        });


        HttpEntity<Map> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS + "?issuerIdentifier={did}&asJwt=true",
                HttpMethod.GET, entity, String.class, baseDID);

        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Map<String, Object> responseMap = SerializeUtil.fromJson(response.getBody());
        List<Map<String, Object>> vcsAsJwt = (ArrayList<Map<String, Object>>) responseMap.get("content");

        Assertions.assertEquals(vcs.size(), vcsAsJwt.size());
        vcsAsJwt.forEach(vc -> {
            Assertions.assertNotNull(vc.get(StringPool.VC_JWT_KEY));
        });
    }


    @Test
    void validateCredentialsWithInvalidVC() throws com.fasterxml.jackson.core.JsonProcessingException, UnsupportedSignatureTypeException, InvalidPublicKeyFormatException, NoVerificationKeyFoundException, SignatureParseException, DidParseException, SignatureVerificationFailedException, TransformJsonLdException {
        //data setup
        CredentialVerificationRequest request = new CredentialVerificationRequest();
        request.setVc(issueVC());

        //service call
        try (MockedStatic<LinkedDataProofValidation> utils = Mockito.mockStatic(LinkedDataProofValidation.class)) {

            //mock setup
            LinkedDataProofValidation mock = Mockito.mock(LinkedDataProofValidation.class);
            utils.when(() -> {
                LinkedDataProofValidation.newInstance(Mockito.any(DidResolver.class));
            }).thenReturn(mock);
            Mockito.when(mock.verify(Mockito.any(VerifiableCredential.class))).thenReturn(false);

            Map<String, Object> stringObjectMap = credentialController.credentialsValidation(request, false, "dummy token").getBody();
            Assertions.assertFalse(Boolean.parseBoolean(stringObjectMap.get(StringPool.VALID).toString()));
        }
    }


    @SneakyThrows
    @Test
    @DisplayName("validate VC with date check true, it should return true")
    void validateCredentialsWithExpiryCheckTrue() {
        CredentialVerificationRequest request = new CredentialVerificationRequest();
        request.setVc(issueVC());

        //service call
        try (MockedStatic<LinkedDataProofValidation> utils = Mockito.mockStatic(LinkedDataProofValidation.class)) {

            //mock setup
            LinkedDataProofValidation mock = Mockito.mock(LinkedDataProofValidation.class);
            utils.when(() -> {
                LinkedDataProofValidation.newInstance(Mockito.any(DidResolver.class));
            }).thenReturn(mock);
            Mockito.when(mock.verify(Mockito.any(VerifiableCredential.class))).thenReturn(true);

            Map<String, Object> stringObjectMap = credentialController.credentialsValidation(request, true, "dummy token").getBody();
            Assertions.assertTrue(Boolean.parseBoolean(stringObjectMap.get(StringPool.VALID).toString()));
            Assertions.assertTrue(Boolean.parseBoolean(stringObjectMap.get(StringPool.VALIDATE_EXPIRY_DATE).toString()));
        }
    }

    @Test
    @DisplayName("validate expired VC with date check false, it should return true")
    void validateCredentialsWithExpiryCheckFalse() throws com.fasterxml.jackson.core.JsonProcessingException, UnsupportedSignatureTypeException, InvalidPublicKeyFormatException, NoVerificationKeyFoundException, SignatureParseException, DidParseException, SignatureVerificationFailedException, TransformJsonLdException {
        CredentialVerificationRequest request = new CredentialVerificationRequest();
        request.setVc(issueVC());

        //modify expiry date
        Instant instant = Instant.now().minusSeconds(60);
        request.put("expirationDate", instant.toString());


        //service call
        try (MockedStatic<LinkedDataProofValidation> utils = Mockito.mockStatic(LinkedDataProofValidation.class)) {

            //mock setup
            LinkedDataProofValidation mock = Mockito.mock(LinkedDataProofValidation.class);
            utils.when(() -> {
                LinkedDataProofValidation.newInstance(Mockito.any(DidResolver.class));
            }).thenReturn(mock);
            Mockito.when(mock.verify(Mockito.any(VerifiableCredential.class))).thenReturn(true);

            Map<String, Object> stringObjectMap = credentialController.credentialsValidation(request, false, "dummt token").getBody();
            Assertions.assertTrue(Boolean.parseBoolean(stringObjectMap.get(StringPool.VALID).toString()));
        }
    }


    @Test
    @DisplayName("validate expired VC with date check true, it should return false")
    void validateExpiredCredentialsWithExpiryCheckTrue() throws com.fasterxml.jackson.core.JsonProcessingException, UnsupportedSignatureTypeException, InvalidPublicKeyFormatException, NoVerificationKeyFoundException, SignatureParseException, DidParseException, SignatureVerificationFailedException, TransformJsonLdException {

        //data setup
        CredentialVerificationRequest request = new CredentialVerificationRequest();
        request.setVc(issueVC());

        //modify expiry date
        Instant instant = Instant.now().minusSeconds(60);
        request.put("expirationDate", instant.toString());

        //service call
        try (MockedStatic<LinkedDataProofValidation> utils = Mockito.mockStatic(LinkedDataProofValidation.class)) {

            //mock setup
            LinkedDataProofValidation mock = Mockito.mock(LinkedDataProofValidation.class);
            utils.when(() -> {
                LinkedDataProofValidation.newInstance(Mockito.any(DidResolver.class));
            }).thenReturn(mock);
            Mockito.when(mock.verify(Mockito.any(VerifiableCredential.class))).thenReturn(true);

            Map<String, Object> stringObjectMap = credentialController.credentialsValidation(request, true, "dummy token").getBody();
            Assertions.assertFalse(Boolean.parseBoolean(stringObjectMap.get(StringPool.VALID).toString()));
            Assertions.assertFalse(Boolean.parseBoolean(stringObjectMap.get(StringPool.VALIDATE_EXPIRY_DATE).toString()));

        }
    }


    private Map<String, Object> issueVC() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String baseBpn = miwSettings.authorityWalletBpn();
        String defaultLocation = miwSettings.host() + StringPool.COLON_SEPARATOR + bpn;
        ResponseEntity<String> response = TestUtils.createWallet(bpn, "Test Wallet", restTemplate, baseBpn, defaultLocation);
        Wallet wallet = TestUtils.getWalletFromString(response.getBody());
        VerifiableCredential verifiableCredential = TestUtils.issueCustomVCUsingBaseWallet(bpn, wallet.getDid(), miwSettings.authorityWalletDid(), "Type1", AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn()), miwSettings, objectMapper, restTemplate);
        Map<String, Object> map = objectMapper.readValue(verifiableCredential.toJson(), Map.class);
        return map;
    }


    private ResponseEntity<String> issueVC(String bpn, String did, String type, HttpHeaders headers) throws JsonProcessingException {
        String baseBpn = miwSettings.authorityWalletBpn();
        //save wallet
        String defaultLocation = miwSettings.host() + StringPool.COLON_SEPARATOR + bpn;
        TestUtils.createWallet(bpn, did, restTemplate, baseBpn, defaultLocation);

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
                        .id(URI.create(did + "#" + UUID.randomUUID()))
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
