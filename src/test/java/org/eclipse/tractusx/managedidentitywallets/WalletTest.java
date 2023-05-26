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

package org.eclipse.tractusx.managedidentitywallets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WalletTest {

    private final String bpn = "123456789";

    private final String name = "smartSense";


    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletKeyRepository walletKeyRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EncryptionUtils encryptionUtils;

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CredentialRepository credentialRepository;


    @Test
    void authorityWalletExistTest() {
        Wallet wallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        Assertions.assertNotNull(wallet);
        Assertions.assertEquals(wallet.getBpn(), miwSettings.authorityWalletBpn());
        Assertions.assertEquals(wallet.getName(), miwSettings.authorityWalletName());
        Assertions.assertNotNull(wallet.getDidDocument());
    }

    @Test
    void encryptionTest() {
        String originalMassage = "Dummy test message";
        String encrypt = encryptionUtils.encrypt(originalMassage);
        String decrypt = encryptionUtils.decrypt(encrypt);
        Assertions.assertEquals(originalMassage, decrypt);
    }


    @Test
    @Order(1)
    void createWalletTest403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, Wallet.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    @Order(2)
    void createWalletTest201() throws JsonProcessingException, JSONException {

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, String.class);
        String body = response.getBody();

        Wallet wallet1 = getWalletFromString(body);

        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(wallet1.getDidDocument());
        Assertions.assertEquals(wallet1.getBpn(), bpn);
        Assertions.assertEquals(wallet1.getName(), name);

        Wallet wallet = walletRepository.getByBpn(bpn);
        Assertions.assertEquals(wallet.getBpn(), bpn);
        Assertions.assertEquals(wallet.getName(), name);
        Assertions.assertNotNull(wallet);
        WalletKey walletKey = walletKeyRepository.getByWalletId(wallet.getId());
        Assertions.assertNotNull(wallet1.getDidDocument());
        Assertions.assertNotNull(walletKey);

        Assertions.assertEquals(wallet.getBpn(), bpn);

    }


    @Test
    @Order(3)
    void storeCredentialsTest201() throws JsonProcessingException {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

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
                                            "id": "did:web:localhost:BPNL000000000000",
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

        Map<String, Objects> map = objectMapper.readValue(vc, Map.class);
        HttpEntity<Map> entity = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, HttpMethod.POST, entity, Map.class, miwSettings.authorityWalletBpn());
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        Wallet byBpn = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        List<Credential> byHolder = credentialRepository.getByHolder(byBpn.getId());
        Assertions.assertEquals(1, byHolder.size());

    }

    @Test
    @Order(4)
    void createWalletWithDuplicateBpn409() {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();


        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, Wallet.class);
        Assertions.assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
    }

    @Test
    @Order(5)
    void getWalletByIdentifierTest403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, Wallet.class, bpn);

        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    @Order(6)
    void getWalletByIdentifierBPNTest200() throws JsonProcessingException {

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, String.class, bpn);

        Wallet body = getWalletFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(body.getBpn(), bpn);
    }


    @Test
    @Order(7)
    void getWalletByIdentifierBPNWithCredentialsTest200() throws JsonProcessingException {

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER + "?withCredentials={withCredentials}", HttpMethod.GET, entity, String.class, miwSettings.authorityWalletBpn(), "true");

        Wallet body = getWalletFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(1, body.getVerifiableCredentials().size());
        Assertions.assertEquals(body.getBpn(), miwSettings.authorityWalletBpn());
    }


    @Test
    @Order(8)
    void getWalletByIdentifierDidTest200() throws JsonProcessingException {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        Wallet newWallet = walletRepository.getByBpn(bpn);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, String.class, newWallet.getDid());

        Wallet body = getWalletFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(body.getBpn(), bpn);
    }


    @Test
    @Order(9)
    void getWalletInvalidBpn404() {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<Wallet> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER, HttpMethod.GET, entity, Wallet.class, UUID.randomUUID().toString());

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    @Order(10)
    void getWallets403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Wallet>> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {
                });
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }


    @Test
    @Order(11)
    void getWallets200() throws JsonProcessingException {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();
        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.WALLETS, HttpMethod.GET, entity, String.class);
        List<Wallet> body = getWalletsFromString(response.getBody());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(body).size() > 0);
    }

    private static List<Wallet> getWalletsFromString(String body) throws JsonProcessingException {
        List<Wallet> walletList = new ArrayList<>();
        JSONArray array = new JSONArray(body);
        if (array.length() == 0) {
            return walletList;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject wallet = array.getJSONObject(i);
            walletList.add(getWalletFromString(wallet.toString()));
        }
        return walletList;
    }


    @NotNull
    private static Wallet getWalletFromString(String body) throws JsonProcessingException {
        JSONObject jsonObject = new JSONObject(body);

        //convert DidDocument
        JSONObject didDocument = jsonObject.getJSONObject("didDocument");
        jsonObject.remove("didDocument");

        JSONArray credentialArray = null;
        if (!jsonObject.isNull("verifiableCredentials")) {
            credentialArray = jsonObject.getJSONArray("verifiableCredentials");
            jsonObject.remove("verifiableCredentials");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Wallet wallet1 = objectMapper.readValue(jsonObject.toString(), Wallet.class);
        wallet1.setDidDocument(DidDocument.fromJson(didDocument.toString()));

        //convert VC
        if (credentialArray != null) {
            List<VerifiableCredential> verifiableCredentials = new ArrayList<>(credentialArray.length());
            for (int i = 0; i < credentialArray.length(); i++) {
                JSONObject object = credentialArray.getJSONObject(i);
                verifiableCredentials.add(new VerifiableCredential(objectMapper.readValue(object.toString(), Map.class)));
            }
            wallet1.setVerifiableCredentials(verifiableCredentials);
        }
        System.out.println("wallet -- >" + wallet1.getBpn());
        return wallet1;
    }
}
