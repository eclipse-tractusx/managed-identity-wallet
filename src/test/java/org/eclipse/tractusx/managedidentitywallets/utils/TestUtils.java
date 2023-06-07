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

package org.eclipse.tractusx.managedidentitywallets.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestUtils {

    public static ResponseEntity<String> createWallet(String bpn, String name, TestRestTemplate testTemplate) {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        CreateWalletRequest request = CreateWalletRequest.builder().bpn(bpn).name(name).build();

        HttpEntity<CreateWalletRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> exchange = testTemplate.exchange(RestURI.WALLETS, HttpMethod.POST, entity, String.class);
        return exchange;

    }
    
    public static Wallet createWallet(String bpn, String did, WalletRepository walletRepository) {
        String didDocument = """
                {
                  "id": "did:web:localhost:bpn123124",
                  "verificationMethod": [
                    {
                      "publicKeyMultibase": "z9mo3TUPvEntiBQtHYVXXy5DfxLGgaHa84ZT6Er2qWs4y",
                      "controller": "did:web:localhost%3Abpn123124",
                      "id": "did:web:localhost%3Abpn123124#key-1",
                      "type": "Ed25519VerificationKey2020"
                    }
                  ],
                  "@context": "https://www.w3.org/ns/did/v1"
                }
                """;

        Wallet wallet = Wallet.builder()
                .bpn(bpn)
                .did(did)
                .didDocument(DidDocument.fromJson(didDocument))
                .algorithm(StringPool.ED_25519)
                .name(bpn)
                .build();
        return walletRepository.save(wallet);
    }

    public static void checkVC(VerifiableCredential verifiableCredential, MIWSettings miwSettings) {
        //text context URL
        Assertions.assertEquals(verifiableCredential.getContext().size(), miwSettings.vcContexts().size());
        for (String link : verifiableCredential.getContext()) {
            Assertions.assertTrue(miwSettings.vcContexts().contains(link));
        }

        //check expiry date
        Assertions.assertEquals(0, verifiableCredential.getExpirationDate().compareTo(miwSettings.vcExpiryDate().toInstant()));
    }

    public static ResponseEntity<String> issueMembershipVC(TestRestTemplate restTemplate, String bpn, String baseWalletBpn) {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(baseWalletBpn);
        IssueMembershipCredentialRequest request = IssueMembershipCredentialRequest.builder().bpn(bpn).build();
        HttpEntity<IssueMembershipCredentialRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, HttpMethod.POST, entity, String.class);
    }

    public static IssueFrameworkCredentialRequest getIssueFrameworkCredentialRequest(String bpn, String type, String value) {
        IssueFrameworkCredentialRequest twinRequest = IssueFrameworkCredentialRequest.builder()
                .contractTemplate("http://localhost")
                .contractVersion("v1")
                .type(type)
                .value(value)
                .bpn(bpn)
                .build();
        return twinRequest;
    }


    public static Wallet getWalletFromString(String body) throws JsonProcessingException {
        JSONObject jsonObject = new JSONObject(body);
        //convert DidDocument
        JSONObject didDocument = jsonObject.getJSONObject(StringPool.DID_DOCUMENT);
        jsonObject.remove(StringPool.DID_DOCUMENT);

        JSONArray credentialArray = null;
        if (!jsonObject.isNull(StringPool.VERIFIABLE_CREDENTIALS)) {
            credentialArray = jsonObject.getJSONArray(StringPool.VERIFIABLE_CREDENTIALS);
            jsonObject.remove(StringPool.VERIFIABLE_CREDENTIALS);
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

    public static List<VerifiableCredential> getCredentialsFromString(String body) throws com.fasterxml.jackson.core.JsonProcessingException {
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
}
