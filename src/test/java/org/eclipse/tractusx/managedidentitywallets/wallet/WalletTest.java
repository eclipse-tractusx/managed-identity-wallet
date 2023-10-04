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

package org.eclipse.tractusx.managedidentitywallets.wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.service.WalletService;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ContextConfiguration(initializers = {TestContextInitializer.class})
class WalletTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletKeyRepository walletKeyRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HoldersCredentialRepository holdersCredentialRepository;

    @Autowired
    private WalletService walletService;


    @Test
    void createDuplicateAuthorityWalletTest() {
        walletService.createAuthorityWallet();
        int count = walletRepository.countByBpn(miwSettings.authorityWalletBpn());
        Assertions.assertEquals(1, count);
    }

    @Test
    void authorityWalletExistTest() {
        Wallet wallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        Assertions.assertNotNull(wallet);
        Assertions.assertEquals(wallet.getBpn(), miwSettings.authorityWalletBpn());
        Assertions.assertEquals(wallet.getName(), miwSettings.authorityWalletName());
        Assertions.assertNotNull(wallet.getDidDocument());

        //check BPN credentials issued for authority wallet
        List<HoldersCredential> vcs = holdersCredentialRepository.getByHolderDidAndType(wallet.getDid(), MIWVerifiableCredentialType.BPN_CREDENTIAL);
        Assertions.assertFalse(vcs.isEmpty());
        Assertions.assertTrue(vcs.get(0).isSelfIssued());
    }


    @Test
    void createWalletTest403() {
        String bpn = TestUtils.getRandomBpmNumber();
        String name = "Sample Wallet";
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, Wallet.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void createWalletTestWithUserToken403() {
        String bpn = TestUtils.getRandomBpmNumber();
        String name = "Sample Wallet";
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, Wallet.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    @DisplayName("Create wallet with invalid BPN, it should return 400 ")
    void createWalletWithInvalidBPNTest400() throws JsonProcessingException, JSONException {
        String bpn = "invalid bpn";
        String name = "Sample Wallet";
        String baseBpn = miwSettings.authorityWalletBpn();

        ResponseEntity<String> response = TestUtils.createWallet(bpn, name, restTemplate, baseBpn);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
    }

    @Test
    void createWalletTest201() throws JsonProcessingException, JSONException {

        String bpn = TestUtils.getRandomBpmNumber();
        String name = "Sample Wallet";
        String baseBpn = miwSettings.authorityWalletBpn();

        ResponseEntity<String> response = TestUtils.createWallet(bpn, name, restTemplate, baseBpn);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        Wallet wallet = TestUtils.getWalletFromString(response.getBody());

        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(wallet.getDidDocument());
        List<URI> context = wallet.getDidDocument().getContext();
        miwSettings.didDocumentContextUrls().forEach(uri -> {
            Assertions.assertTrue(context.contains(uri));
        });
        Assertions.assertEquals(wallet.getBpn(), bpn);
        Assertions.assertEquals(wallet.getName(), name);

        Wallet walletFromDB = walletRepository.getByBpn(bpn);
        Assertions.assertEquals(walletFromDB.getBpn(), bpn);
        Assertions.assertEquals(walletFromDB.getName(), name);
        Assertions.assertNotNull(walletFromDB);
        WalletKey walletKey = walletKeyRepository.getByWalletId(walletFromDB.getId());
        Assertions.assertNotNull(walletKey);
        Assertions.assertEquals(walletFromDB.getBpn(), bpn);

        //validate keyId
        String keyId = wallet.getDidDocument().getVerificationMethods().get(0).getId().toString().split("#")[1];
        Assertions.assertNotNull(walletKey.getKeyId());
        Assertions.assertEquals(walletKey.getKeyId(), keyId);

        //check if BPN and Summary credentials is issued
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<String> getWalletResponse = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER + "?withCredentials={withCredentials}", HttpMethod.GET, entity, String.class, bpn, "true");
        Assertions.assertEquals(getWalletResponse.getStatusCode().value(), HttpStatus.OK.value());
        Wallet body = TestUtils.getWalletFromString(getWalletResponse.getBody());
        Assertions.assertEquals(2, body.getVerifiableCredentials().size());

        VerifiableCredential verifiableCredential = body.getVerifiableCredentials().stream()
                .filter(vp -> vp.getTypes().contains(MIWVerifiableCredentialType.BPN_CREDENTIAL))
                .findFirst()
                .orElse(null);
        Assertions.assertEquals(verifiableCredential.getCredentialSubject().get(0).get(StringPool.ID), wallet.getDid());
        Assertions.assertEquals(verifiableCredential.getCredentialSubject().get(0).get(StringPool.BPN), wallet.getBpn());
        Assertions.assertEquals(MIWVerifiableCredentialType.BPN_CREDENTIAL, verifiableCredential.getCredentialSubject().get(0).get(StringPool.TYPE));

        VerifiableCredential summaryVerifiableCredential = body.getVerifiableCredentials().stream()
                .filter(vc -> vc.getTypes().contains(MIWVerifiableCredentialType.SUMMARY_CREDENTIAL)).findFirst()
                .orElse(null);
        VerifiableCredentialSubject subject = summaryVerifiableCredential.getCredentialSubject().get(0);
        List<String> list = (List<String>) subject.get(StringPool.ITEMS);
        Assertions.assertTrue(list.contains(MIWVerifiableCredentialType.BPN_CREDENTIAL));
    }


    @Test
    void storeCredentialsTest201() throws JsonProcessingException {

        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String baseBpn = miwSettings.authorityWalletBpn();

        TestUtils.createWallet(bpn, "name", restTemplate, baseBpn);

        ResponseEntity<Map> response = storeCredential(bpn, did);

        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        Wallet byBpn = walletRepository.getByBpn(bpn);

        List<HoldersCredential> vc = holdersCredentialRepository.getByHolderDidAndType(did, "University-Degree-Credential");
        Assertions.assertFalse(vc.isEmpty());

        Assertions.assertTrue(vc.get(0).isStored());
        Assertions.assertFalse(vc.get(0).isSelfIssued());
    }


    @Test
    void storeCredentialsWithDifferentBPNAccess403() throws JsonProcessingException {
        //make sure authority wallet is created
        authorityWalletExistTest();
        String did = "did:web:localhost:" + miwSettings.authorityWalletBpn();

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders("Invalid BPN");
        String vc = """
                                {
                                    "id": "http://example.edu/credentials/3732",
                                    "@context":
                                    [
                                        "https://www.w3.org/2018/credentials/v1",
                                        "https://www.w3.org/2018/credentials/examples/v1"
                                    ],
                                    "type":
                                    [
                                        "University-Degree-Credential", "VerifiableCredential"
                                    ],
                                    "issuer": "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                    "issuanceDate": "2019-06-16T18:56:59Z",
                                    "expirationDate": "2019-06-17T18:56:59Z",
                                    "credentialSubject":
                                    [
                                        {
                                            "id": "##did",
                                            "college": "Test-University"
                                        }
                                    ],
                                    "credentialStatus":
                                    {
                                        "id": "http://example.edu/api/credentials/status/test#3",
                                        "type": "StatusList2021Entry",
                                        "statusPurpose": "revocation",
                                        "statusListIndex": "3",
                                        "statusListCredential": "http://example.edu/api/credentials/status/test"
                                    },
                                    "proof":
                                    {
                                        "type": "Ed25519Signature2018",
                                        "created": "2021-11-17T22:20:27Z",
                                        "proofPurpose": "assertionMethod",
                                        "verificationMethod": "did:example:76e12ec712ebc6f1c221ebfeb1f#key-1",
                                        "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                                    }
                                }
                """;

        Map<String, Objects> map = objectMapper.readValue(vc.replace("##did", did), Map.class);
        HttpEntity<Map> entity = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, HttpMethod.POST, entity, Map.class, miwSettings.authorityWalletBpn());
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());

    }

    @Test
    void storeCredentialsWithDifferentHolder403() throws JsonProcessingException {

        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String baseBpn = miwSettings.authorityWalletBpn();
        TestUtils.createWallet(bpn, "name", restTemplate, baseBpn);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders("Some random pbn");

        ResponseEntity<Map> response = storeCredential(bpn, did, headers);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());

    }

    @Test
    void createWalletWithDuplicateBpn409() throws JsonProcessingException {

        String bpn = TestUtils.getRandomBpmNumber();
        String name = "Sample Wallet";
        String baseBpn = miwSettings.authorityWalletBpn();

        //save wallet
        ResponseEntity<String> response = TestUtils.createWallet(bpn, name, restTemplate, baseBpn);
        TestUtils.getWalletFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        //try with again with same BPN
        ResponseEntity<String> response1 = TestUtils.createWallet(bpn, name, restTemplate, baseBpn);
        Assertions.assertEquals(HttpStatus.CONFLICT.value(), response1.getStatusCode().value());
    }

    @Test
    void getWalletByIdentifierTest403() {
        String bpn = TestUtils.getRandomBpmNumber();
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, Wallet.class, bpn);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void getWalletByIdentifierWithInvalidBPNTest403() {
        String bpn = TestUtils.getRandomBpmNumber();
        String baseBpn = miwSettings.authorityWalletBpn();

        TestUtils.createWallet(bpn, "sample name", restTemplate, baseBpn);

        //create token with different BPN
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders("invalid BPN");
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, Wallet.class, bpn);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void getWalletByIdentifierBPNTest200() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String name = "Sample Name";
        String baseBpn = miwSettings.authorityWalletBpn();

        //Create entry
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, name, restTemplate, baseBpn).getBody());

        //get wallet without credentials
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<String> getWalletResponse = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER + "?withCredentials={withCredentials}", HttpMethod.GET, entity, String.class, bpn, "false");

        Wallet body = TestUtils.getWalletFromString(getWalletResponse.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), getWalletResponse.getStatusCode().value());
        Assertions.assertNotNull(getWalletResponse.getBody());
        Assertions.assertEquals(body.getBpn(), bpn);
    }


    @Test
    void getWalletByIdentifierBPNWithCredentialsTest200() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String name = "Sample Name";
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String baseBpn = miwSettings.authorityWalletBpn();

        //Create entry
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, name, restTemplate, baseBpn).getBody());

        //store credentials
        ResponseEntity<Map> response = storeCredential(bpn, did);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        ///get wallet with credentials
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<String> getWalletResponse = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER + "?withCredentials={withCredentials}", HttpMethod.GET, entity, String.class, bpn, "true");

        Wallet body = TestUtils.getWalletFromString(getWalletResponse.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), getWalletResponse.getStatusCode().value());
        Assertions.assertNotNull(getWalletResponse.getBody());
        Assertions.assertEquals(3, body.getVerifiableCredentials().size()); //BPN VC + Summery VC + Stored VC
        Assertions.assertEquals(body.getBpn(), bpn);
    }

    @Test
    @Disabled("the endpoint has an issue that prevents resolving did with a port number")
    void getWalletByIdentifierDidTest200() throws JsonProcessingException {

        String bpn = TestUtils.getRandomBpmNumber();
        String name = "Sample Name";
        String baseBpn = miwSettings.authorityWalletBpn();

        //Create entry
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, name, restTemplate, baseBpn).getBody());

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        Wallet newWallet = walletRepository.getByBpn(bpn);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, String.class, newWallet.getDid());

        Wallet body = TestUtils.getWalletFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(body.getBpn(), bpn);
    }

    @Test
    void getWalletInvalidBpn404() {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, Wallet.class, TestUtils.getRandomBpmNumber());

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    void getWallets403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Wallet>> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {
                });
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    void getWallets200() throws JsonProcessingException {

        String bpn = TestUtils.getRandomBpmNumber();
        String name = "Sample Name";
        String baseBpn = miwSettings.authorityWalletBpn();
        //Create entry
        TestUtils.createWallet(bpn, name, restTemplate, baseBpn);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.GET, entity, String.class);
        List<Wallet> body = getWalletsFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(body).size() > 0);
    }


    private ResponseEntity<Map> storeCredential(String bpn, String did, HttpHeaders headers) throws JsonProcessingException {
        String vc = """
                                {
                                    "id": "http://example.edu/credentials/3732",
                                    "@context":
                                    [
                                        "https://www.w3.org/2018/credentials/v1",
                                        "https://www.w3.org/2018/credentials/examples/v1"
                                    ],
                                    "type":
                                    [
                                        "University-Degree-Credential", "VerifiableCredential"
                                    ],
                                    "issuer": "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                    "issuanceDate": "2019-06-16T18:56:59Z",
                                    "expirationDate": "2019-06-17T18:56:59Z",
                                    "credentialSubject":
                                    [
                                        {
                                            "id": "##did",
                                            "college": "Test-University"
                                        }
                                    ],
                                    "credentialStatus":
                                    {
                                        "id": "http://example.edu/api/credentials/status/test#3",
                                        "type": "StatusList2021Entry",
                                        "statusPurpose": "revocation",
                                        "statusListIndex": "3",
                                        "statusListCredential": "http://example.edu/api/credentials/status/test"
                                    },
                                    "proof":
                                    {
                                        "type": "Ed25519Signature2018",
                                        "created": "2021-11-17T22:20:27Z",
                                        "proofPurpose": "assertionMethod",
                                        "verificationMethod": "did:example:76e12ec712ebc6f1c221ebfeb1f#key-1",
                                        "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                                    }
                                }
                """;

        Map<String, Objects> map = objectMapper.readValue(vc.replace("##did", did), Map.class);
        HttpEntity<Map> entity = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, HttpMethod.POST, entity, Map.class, bpn);
        return response;
    }

    private ResponseEntity<Map> storeCredential(String bpn, String did) throws JsonProcessingException {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);
        return storeCredential(bpn, did, headers);
    }


    private List<Wallet> getWalletsFromString(String body) throws JsonProcessingException {
        List<Wallet> walletList = new ArrayList<>();

        JSONArray array = new JSONArray(new JSONObject(body).getJSONArray("content"));
        if (array.length() == 0) {
            return walletList;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject wallet = array.getJSONObject(i);
            walletList.add(TestUtils.getWalletFromString(wallet.toString()));
        }
        return walletList;
    }

}
