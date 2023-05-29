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
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.json.JSONException;
import org.junit.jupiter.api.*;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MembershipCredentialTest {
    @Autowired
    private CredentialRepository credentialRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private TestRestTemplate restTemplate;

    private final String bpn = UUID.randomUUID().toString();

    private final String did = "did:web:localhost" + bpn;

    @Test
    @Order(1)
    void issueMembershipCredentialTest403() {
        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        IssueMembershipCredentialRequest request = IssueMembershipCredentialRequest.builder().bpn(bpn).build();

        HttpEntity<IssueMembershipCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<VerifiableCredential> response = restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, HttpMethod.POST, entity, VerifiableCredential.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    @Order(2)
    void issueMembershipCredentialTest201() throws JsonProcessingException, JSONException {

        String didDocument = """
                {
                  "id": "did:web:localhost%3Abpn123124",
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
                .algorithm("ED25519")
                .name(bpn)
                .build();
        wallet = walletRepository.save(wallet);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();
        IssueMembershipCredentialRequest request = IssueMembershipCredentialRequest.builder().bpn(bpn).build();
        HttpEntity<IssueMembershipCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(response.getBody(), Map.class);
        VerifiableCredential verifiableCredential = new VerifiableCredential(map);
        Assertions.assertTrue(verifiableCredential.getTypes().contains(VerifiableCredentialType.MEMBERSHIP_CREDENTIAL));
        Assertions.assertEquals(verifiableCredential.getCredentialSubject().get(0).get("holderIdentifier"), bpn);

        Credential credential = credentialRepository.getByHolderAndType(wallet.getId(), VerifiableCredentialType.MEMBERSHIP_CREDENTIAL);
        Assertions.assertNotNull(credential);

    }

    @Test
    @Order(3)
    void issueMembershipCredentialWithDuplicateBpn409() {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders();
        IssueMembershipCredentialRequest request = IssueMembershipCredentialRequest.builder().bpn(bpn).build();
        HttpEntity<IssueMembershipCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
    }

}
