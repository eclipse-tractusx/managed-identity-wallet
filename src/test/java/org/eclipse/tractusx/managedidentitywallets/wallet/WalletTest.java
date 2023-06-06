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
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ActiveProfiles("test")
@ContextConfiguration(initializers = {TestContextInitializer.class})
class WalletTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(WalletTest.class);

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


    @Test
    void authorityWalletExistTest() {
        Wallet wallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        Assertions.assertNotNull(wallet);
        Assertions.assertEquals(wallet.getBpn(), miwSettings.authorityWalletBpn());
        Assertions.assertEquals(wallet.getName(), miwSettings.authorityWalletName());
        Assertions.assertNotNull(wallet.getDidDocument());
    }


    @Test
    void createWalletTest403() {
        String bpn = UUID.randomUUID().toString();
        String name = "Sample Wallet";
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, Wallet.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    void createWalletTest201() throws JsonProcessingException, JSONException {

        String bpn = UUID.randomUUID().toString();
        String name = "Sample Wallet";

        ResponseEntity<String> response = TestUtils.createWallet(bpn, name, restTemplate);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        Wallet wallet = TestUtils.getWalletFromString(response.getBody());

        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(wallet.getDidDocument());
        Assertions.assertEquals(wallet.getBpn(), bpn);
        Assertions.assertEquals(wallet.getName(), name);

        Wallet walletFromDB = walletRepository.getByBpn(bpn);
        Assertions.assertEquals(walletFromDB.getBpn(), bpn);
        Assertions.assertEquals(walletFromDB.getName(), name);
        Assertions.assertNotNull(walletFromDB);
        WalletKey walletKey = walletKeyRepository.getByWalletId(walletFromDB.getId());
        Assertions.assertNotNull(wallet.getDidDocument());
        Assertions.assertNotNull(walletKey);

        Assertions.assertEquals(walletFromDB.getBpn(), bpn);

        //check if BPN credentials is issued
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<String> getWalletResponse = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER + "?withCredentials={withCredentials}", HttpMethod.GET, entity, String.class, bpn, "true");
        Assertions.assertEquals(getWalletResponse.getStatusCode().value(), HttpStatus.OK.value());
        Wallet body = TestUtils.getWalletFromString(getWalletResponse.getBody());
        Assertions.assertEquals(1, body.getVerifiableCredentials().size());
        VerifiableCredential verifiableCredential = body.getVerifiableCredentials().get(0);

        Assertions.assertEquals(verifiableCredential.getCredentialSubject().get(0).get("id"), wallet.getDid());

        Assertions.assertEquals(verifiableCredential.getCredentialSubject().get(0).get("bpn"), wallet.getBpn());
        Assertions.assertEquals(MIWVerifiableCredentialType.BPN_CREDENTIAL, verifiableCredential.getCredentialSubject().get(0).get("type"));

    }


    @Test
    void storeCredentialsTest201() throws JsonProcessingException {


        //make sure authority wallet is created
        authorityWalletExistTest();
        String did = "did:web:localhost:" + miwSettings.authorityWalletBpn();

        ResponseEntity<Map> response = storeCredential(miwSettings.authorityWalletBpn(), did);

        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        Wallet byBpn = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        List<HoldersCredential> byHolder = holdersCredentialRepository.getByHolderDid(byBpn.getDid());
        Assertions.assertEquals(2, byHolder.size());

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
        //make sure authority wallet is created
        authorityWalletExistTest();
        String did = "did:web:localhost:" + miwSettings.authorityWalletBpn();

        ResponseEntity<Map> response = storeCredential(miwSettings.authorityWalletBpn(), "Some Random bpn");
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());

    }

    @Test
    void createWalletWithDuplicateBpn409() throws JsonProcessingException {

        String bpn = UUID.randomUUID().toString();
        String name = "Sample Wallet";

        //save wallet
        ResponseEntity<String> response = TestUtils.createWallet(bpn, name, restTemplate);
        TestUtils.getWalletFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        //try with again with same BPN
        ResponseEntity<String> response1 = TestUtils.createWallet(bpn, name, restTemplate);
        Assertions.assertEquals(HttpStatus.CONFLICT.value(), response1.getStatusCode().value());
    }

    @Test
    void getWalletByIdentifierTest403() {
        String bpn = UUID.randomUUID().toString();
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, Wallet.class, bpn);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void getWalletByIdentifierWithInvalidBPNTest403() {
        String bpn = UUID.randomUUID().toString();

        TestUtils.createWallet(bpn, "sample name", restTemplate);

        //create token with different BPN
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders("invalid BPN");
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, Wallet.class, bpn);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void getWalletByIdentifierBPNTest200() throws JsonProcessingException {
        String bpn = UUID.randomUUID().toString();
        String name = "Sample Name";

        //Create entry
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, name, restTemplate).getBody());

        //get Wallet
        ///get wallet with credentials
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
        String bpn = UUID.randomUUID().toString();
        String name = "Sample Name";
        String did = "did:web:localhost:" + bpn;
        //Create entry
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, name, restTemplate).getBody());

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
        Assertions.assertEquals(2, body.getVerifiableCredentials().size());
        Assertions.assertEquals(body.getBpn(), bpn);
    }

    @Test
    void getWalletByIdentifierDidTest200() throws JsonProcessingException {

        String bpn = UUID.randomUUID().toString();
        String name = "Sample Name";
        String did = "did:web:localhost:" + bpn;

        //Create entry
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, name, restTemplate).getBody());

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

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, Wallet.class, UUID.randomUUID().toString());

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

        String bpn = UUID.randomUUID().toString();
        String name = "Sample Name";
        String did = "did:web:localhost:" + bpn;
        //Create entry
        TestUtils.createWallet(bpn, name, restTemplate);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.GET, entity, String.class);
        List<Wallet> body = getWalletsFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(body).size() > 0);
    }



    private ResponseEntity<Map> storeCredential(String bpn, String did) throws JsonProcessingException {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);
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
