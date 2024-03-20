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
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.service.IssuersCredentialService;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COLON_SEPARATOR;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {ManagedIdentityWalletsApplication.class})
@ContextConfiguration(initializers = {TestContextInitializer.class})
class MembershipHoldersCredentialTest {
    @Autowired
    private HoldersCredentialRepository holdersCredentialRepository;
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private IssuersCredentialRepository issuersCredentialRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IssuersCredentialService issuersCredentialService;


    @Test
    void issueMembershipCredentialTest403() {
        String bpn = TestUtils.getRandomBpmNumber();

        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();

        HttpHeaders headers = AuthenticationUtils.getInvalidUserHttpHeaders();

        IssueMembershipCredentialRequest request = IssueMembershipCredentialRequest.builder().bpn(bpn).build();

        HttpEntity<IssueMembershipCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<VerifiableCredential> response = restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, HttpMethod.POST, entity, VerifiableCredential.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void testIssueSummeryVCAfterDeleteSummaryVCFromHolderWallet() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String baseBpn = miwSettings.authorityWalletBpn();

        // create wallet, in background bpn and summary credential generated
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, bpn, restTemplate, baseBpn, defaultLocation).getBody());
        generateBpnCredential(wallet);
        List<HoldersCredential> byHolderDid = holdersCredentialRepository.getByHolderDid(did);

        //delete all VC
        holdersCredentialRepository.deleteAll(byHolderDid);

        //issue membership
        ResponseEntity<String> response = TestUtils.issueMembershipVC(restTemplate, bpn, miwSettings.authorityWalletBpn());
        Assertions.assertEquals(response.getStatusCode().value(), HttpStatus.CREATED.value());

        //check summary VC in holder wallet
        List<HoldersCredential> summaryVcs = holdersCredentialRepository.getByHolderDidAndIssuerDidAndTypeAndStored(did, miwSettings.authorityWalletDid(), MIWVerifiableCredentialType.SUMMARY_CREDENTIAL, false);
        Assertions.assertFalse(summaryVcs.isEmpty());

        //check items, it should be 2
        List<String> items = (List<String>) summaryVcs.get(0).getData().getCredentialSubject().get(0).get(StringPool.ITEMS);

        Assertions.assertTrue(items.contains(MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL));
        Assertions.assertTrue(items.contains(MIWVerifiableCredentialType.BPN_CREDENTIAL));
    }

    @Test
    void testStoredSummaryVCTest() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();
        String baseBpn = miwSettings.authorityWalletBpn();

        // create wallet, in background bpn and summary credential generated
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, bpn, restTemplate, baseBpn, defaultLocation).getBody());


        String vc = """
                {
                      "@context": [
                        "https://www.w3.org/2018/credentials/v1",
                        "https://www.w3.org/2018/credentials/examples/v1"
                      ],
                      "id": "urn:uuid:12345678-1234-1234-1234-123456789abc",
                      "type": [
                        "VerifiableCredential",
                        "SummaryCredential"
                      ],
                      "issuer": "did:web:localhost:BPNL000000000000",
                      "issuanceDate": "2023-06-02T12:00:00Z",
                      "expirationDate": "2022-06-16T18:56:59Z",
                      "credentialSubject": [{
                        "id": "did:web:localhost:BPNL000000000000",
                        "holderIdentifier": "BPN of holder",
                        "type": "Summary-List",
                        "name": "CX-Credentials",
                        "items": [
                         "MembershipCredential","DismantlerCredential","PcfCredential","SustainabilityCredential","QualityCredential","TraceabilityCredential","BehaviorTwinCredential","BpnCredential"
                        ],
                        "contract-templates": "https://public.catena-x.org/contracts/"
                      },{
                          "name":"test name"
                      }],
                      "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2023-06-02T12:00:00Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:web:example.com#key-1",
                        "jws": "eyJhbGciOiJFZERTQSJ9.eyJpYXQiOjE2MjM1NzA3NDEsImV4cCI6MTYyMzU3NDM0MSwianRpIjoiMTIzNDU2NzgtMTIzNC0xMjM0LTEyMzQtMTIzNDU2Nzg5YWJjIiwicHJvb2YiOnsiaWQiOiJkaWQ6d2ViOmV4YW1wbGUuY29tIiwibmFtZSI6IkJlaXNwaWVsLU9yZ2FuaXNhdGlvbiJ9fQ.SignedExampleSignature"
                      }
                    }
                """;
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);

        Map<String, Objects> map = objectMapper.readValue(vc.replace("##did", did), Map.class);
        HttpEntity<Map> entity = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.exchange(RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, HttpMethod.POST, entity, Map.class, bpn);
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        //issue  membership
        ResponseEntity<String> response1 = TestUtils.issueMembershipVC(restTemplate, bpn, miwSettings.authorityWalletBpn());
        Assertions.assertEquals(HttpStatus.CREATED.value(), response1.getStatusCode().value());

        //stored VC should not be deleted
        List<HoldersCredential> summaryCredential = holdersCredentialRepository.getByHolderDidAndIssuerDidAndTypeAndStored(wallet.getDid(), "did:web:localhost:BPNL000000000000", "SummaryCredential", true);
        Assertions.assertFalse(summaryCredential.isEmpty());

    }

    @Test
    void issueMembershipCredentialToBaseWalletTest400() throws JsonProcessingException {
        String bpn = TestUtils.getRandomBpmNumber();
        String baseBpn = miwSettings.authorityWalletBpn();

        // create wallet, in background bpn and summary credential generated
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, bpn, restTemplate, baseBpn, defaultLocation).getBody());
        generateBpnCredential(wallet);
        //add 2 subject in VC for testing
        List<IssuersCredential> vcs = issuersCredentialRepository.getByIssuerDidAndHolderDidAndType(miwSettings.authorityWalletDid(), wallet.getDid(), MIWVerifiableCredentialType.SUMMARY_CREDENTIAL);

        String vc = """
                {
                      "@context": [
                        "https://www.w3.org/2018/credentials/v1",
                        "https://www.w3.org/2018/credentials/examples/v1"
                      ],
                      "id": "urn:uuid:12345678-1234-1234-1234-123456789abc",
                      "type": [
                        "VerifiableCredential",
                        "SummaryCredential"
                      ],
                      "issuer": "did:web:localhost:BPNL000000000000",
                      "issuanceDate": "2023-06-02T12:00:00Z",
                      "expirationDate": "2022-06-16T18:56:59Z",
                      "credentialSubject": [{
                        "id": "did:web:localhost:BPNL000000000000",
                        "holderIdentifier": "BPN of holder",
                        "type": "Summary-List",
                        "name": "CX-Credentials",
                        "items": [
                         "MembershipCredential","DismantlerCredential","PcfCredential","SustainabilityCredential","QualityCredential","TraceabilityCredential","BehaviorTwinCredential","BpnCredential"
                        ],
                        "contract-templates": "https://public.catena-x.org/contracts/"
                      },{
                          "name":"test name"
                      }],
                      "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2023-06-02T12:00:00Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:web:example.com#key-1",
                        "jws": "eyJhbGciOiJFZERTQSJ9.eyJpYXQiOjE2MjM1NzA3NDEsImV4cCI6MTYyMzU3NDM0MSwianRpIjoiMTIzNDU2NzgtMTIzNC0xMjM0LTEyMzQtMTIzNDU2Nzg5YWJjIiwicHJvb2YiOnsiaWQiOiJkaWQ6d2ViOmV4YW1wbGUuY29tIiwibmFtZSI6IkJlaXNwaWVsLU9yZ2FuaXNhdGlvbiJ9fQ.SignedExampleSignature"
                      }
                    }
                """;
        VerifiableCredential verifiableCredential = new VerifiableCredential(new ObjectMapper().readValue(vc, Map.class));
        vcs.get(0).setData(verifiableCredential);

        issuersCredentialRepository.save(vcs.get(0));

        //Check if we do not have items in subject
        ResponseEntity<String> response = TestUtils.issueMembershipVC(restTemplate, bpn, miwSettings.authorityWalletBpn());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());

        vcs.get(0).getData().getCredentialSubject().remove(1);
        vcs.get(0).getData().getCredentialSubject().get(0).remove(StringPool.ITEMS);
        issuersCredentialRepository.save(vcs.get(0));
    }


    @Test
    void issueMembershipCredentialToBaseWalletTest201() throws JsonProcessingException, JSONException {

        Wallet wallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        issuersCredentialService.issueBpnCredential(wallet, wallet, true);
        String oldSummaryCredentialId = TestUtils.getSummaryCredentialId(wallet.getDid(), holdersCredentialRepository);

        ResponseEntity<String> response = TestUtils.issueMembershipVC(restTemplate, miwSettings.authorityWalletBpn(), miwSettings.authorityWalletBpn());
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        VerifiableCredential verifiableCredential = getVerifiableCredential(response);

        TestUtils.checkVC(verifiableCredential, miwSettings);

        validateTypes(verifiableCredential);

        List<HoldersCredential> holderVCs = holdersCredentialRepository.getByHolderDidAndType(wallet.getDid(), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL);
        Assertions.assertFalse(holderVCs.isEmpty());

        TestUtils.checkVC(holderVCs.get(0).getData(), miwSettings);
        Assertions.assertTrue(holderVCs.get(0).isSelfIssued()); //must be self issued true
        Assertions.assertFalse(holderVCs.get(0).isStored()); //store must be false

        //check in issuer tables
        List<IssuersCredential> issuerVCs = issuersCredentialRepository.getByIssuerDidAndHolderDidAndType(miwSettings.authorityWalletDid(), wallet.getDid(), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL);
        Assertions.assertEquals(1, issuerVCs.size());
        TestUtils.checkVC(issuerVCs.get(0).getData(), miwSettings);
        //check summary credential
        TestUtils.checkSummaryCredential(miwSettings.authorityWalletDid(), wallet.getDid(), holdersCredentialRepository, issuersCredentialRepository, MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL, oldSummaryCredentialId);
    }


    @Test
    void issueMembershipCredentialTest201() throws JsonProcessingException, JSONException {

        String bpn = TestUtils.getRandomBpmNumber();
        String baseBpn = miwSettings.authorityWalletBpn();

        //create wallet
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        Wallet wallet = TestUtils.getWalletFromString(TestUtils.createWallet(bpn, bpn, restTemplate, baseBpn, defaultLocation).getBody());
        generateBpnCredential(wallet);
        String oldSummaryCredentialId = TestUtils.getSummaryCredentialId(wallet.getDid(), holdersCredentialRepository);

        ResponseEntity<String> response = TestUtils.issueMembershipVC(restTemplate, bpn, miwSettings.authorityWalletBpn());
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        VerifiableCredential verifiableCredential = getVerifiableCredential(response);

        TestUtils.checkVC(verifiableCredential, miwSettings);

        validateTypes(verifiableCredential);

        List<HoldersCredential> holderVCs = holdersCredentialRepository.getByHolderDidAndType(wallet.getDid(), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL);
        Assertions.assertFalse(holderVCs.isEmpty());
        Assertions.assertFalse(holderVCs.get(0).isSelfIssued()); //must be self issued false
        Assertions.assertFalse(holderVCs.get(0).isStored()); //store must be false


        TestUtils.checkVC(holderVCs.get(0).getData(), miwSettings);

        //check in issuer tables
        List<IssuersCredential> issuerVCs = issuersCredentialRepository.getByIssuerDidAndHolderDidAndType(miwSettings.authorityWalletDid(), wallet.getDid(), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL);
        Assertions.assertEquals(1, issuerVCs.size());
        TestUtils.checkVC(issuerVCs.get(0).getData(), miwSettings);

        //check summary credential
        TestUtils.checkSummaryCredential(miwSettings.authorityWalletDid(), wallet.getDid(), holdersCredentialRepository, issuersCredentialRepository, MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL, oldSummaryCredentialId);
    }


    @Test
    void issueMembershipCredentialWithInvalidBpnAccess409() {
        String bpn = TestUtils.getRandomBpmNumber();

        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();

        //save wallet
        TestUtils.createWallet(bpn, did, walletRepository);

        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(bpn);
        IssueMembershipCredentialRequest request = IssueMembershipCredentialRequest.builder().bpn(bpn).build();
        HttpEntity<IssueMembershipCredentialRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, HttpMethod.POST, entity, String.class);
        Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void issueMembershipCredentialWithDuplicateBpn409() {

        String bpn = TestUtils.getRandomBpmNumber();

        String did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), bpn).toString();

        //save wallet
        TestUtils.createWallet(bpn, did, walletRepository);

        ResponseEntity<String> response = TestUtils.issueMembershipVC(restTemplate, bpn, miwSettings.authorityWalletBpn());
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        ResponseEntity<String> duplicateResponse = TestUtils.issueMembershipVC(restTemplate, bpn, miwSettings.authorityWalletBpn());

        Assertions.assertEquals(HttpStatus.CONFLICT.value(), duplicateResponse.getStatusCode().value());
    }


    @NotNull
    private VerifiableCredential getVerifiableCredential(ResponseEntity<String> response) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(response.getBody(), Map.class);
        return new VerifiableCredential(map);
    }

    private void validateTypes(VerifiableCredential verifiableCredential) {
        Assertions.assertTrue(verifiableCredential.getTypes().contains(MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL));
        Assertions.assertEquals("Test-X", verifiableCredential.getCredentialSubject().get(0).get(StringPool.MEMBER_OF));
    }

    private void generateBpnCredential(Wallet holderWallet) {
        Wallet issuerWallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        issuersCredentialService.issueBpnCredential(issuerWallet, holderWallet, false);
    }
}
