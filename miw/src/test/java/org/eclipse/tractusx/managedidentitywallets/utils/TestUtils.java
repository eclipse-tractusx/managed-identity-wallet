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

package org.eclipse.tractusx.managedidentitywallets.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.curiousoddman.rgxgen.RgxGen;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.CredentialStatus;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.RevocationSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.StatusListRequest;
import org.eclipse.tractusx.managedidentitywallets.revocation.RevocationClient;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.Verifiable;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialStatusList2021Entry;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.SCOPE;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.NONCE;
import static org.springframework.security.oauth2.jwt.JwtClaimNames.JTI;

public class TestUtils {

    public static ResponseEntity<String> createWallet(String bpn, String name, TestRestTemplate testTemplate, String baseBPN, String didUrl) {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(baseBPN);

        CreateWalletRequest request = CreateWalletRequest.builder()
                .businessPartnerNumber(bpn)
                .companyName(name)
                .didUrl(didUrl)
                .build();

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
                .signingServiceType(SigningServiceType.LOCAL)
                .build();
        return walletRepository.save(wallet);
    }

    public static void checkVC(VerifiableCredential verifiableCredential, MIWSettings miwSettings, RevocationSettings revocationSettings) {
        for (URI link : miwSettings.vcContexts()) {
            Assertions.assertTrue(verifiableCredential.getContext().contains(link));
        }

        if (verifiableCredential.getVerifiableCredentialStatus() != null) {
            Assertions.assertTrue(verifiableCredential.getContext().contains(revocationSettings.bitStringStatusListContext()));
        }
        //check expiry date
        Assertions.assertEquals(0, verifiableCredential.getExpirationDate().compareTo(miwSettings.vcExpiryDate().toInstant()));
    }


    public static Wallet getWalletFromString(String body) throws JsonProcessingException, JSONException {
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
        return wallet1;
    }


    @NotNull
    public static List<VerifiableCredential> getVerifiableCredentials(ResponseEntity<String> response, ObjectMapper objectMapper) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(response.getBody(), Map.class);

        List<Map<String, Object>> vcs = (List<Map<String, Object>>) map.get("content");

        List<VerifiableCredential> credentialList = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : vcs) {
            credentialList.add(new VerifiableCredential(stringObjectMap));
        }
        return credentialList;
    }

    public static String getRandomBpmNumber() {
        RgxGen rgxGen = new RgxGen(StringPool.BPN_NUMBER_REGEX);
        return rgxGen.generate();
    }

    public static String buildJWTToken(OctetKeyPair jwk, JWTClaimsSet claimsSet) throws JOSEException {
        JWSSigner signer = new Ed25519Signer(jwk);
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.EdDSA).keyID(jwk.getKeyID()).build(),
                claimsSet);

        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    public static JWTClaimsSet buildClaimsSet(String issuer, String subject, String audience, String nonce, String scope, Date expiration, Date issuance, String jti) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(audience)
                .expirationTime(expiration)
                .issueTime(issuance)
                .claim(NONCE, nonce)
                .claim(SCOPE, scope)
                .claim(JTI, jti)
                .build();
    }

    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    public static JWTClaimsSet addAccessTokenToClaimsSet(String accessToken, JWTClaimsSet initialSet) {
        return new JWTClaimsSet.Builder(initialSet).claim(ACCESS_TOKEN, accessToken).build();
    }

    public static Wallet buildWallet(String bpn, String did, String didJson) {
        return Wallet.builder()
                .bpn(bpn)
                .did(did)
                .didDocument(DidDocument.fromJson(didJson))
                .algorithm(StringPool.ED_25519)
                .name(bpn)
                .signingServiceType(SigningServiceType.LOCAL)
                .build();
    }

    @SneakyThrows
    public static VerifiableCredential issueCustomVCUsingBaseWallet(String holderBPn, String holderDid, String issuerDid, String type, HttpHeaders headers,
                                                                    MIWSettings miwSettings, ObjectMapper objectMapper, TestRestTemplate restTemplate) {

        Map<String, Object> map = getCredentialAsMap(holderBPn, holderDid, issuerDid, type, miwSettings, objectMapper);
        HttpEntity<Map> entity = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?holderDid={did}", HttpMethod.POST, entity, String.class, holderDid);
        if (response.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
            throw new ForbiddenException();
        }
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        return new VerifiableCredential(new ObjectMapper().readValue(response.getBody(), Map.class));
    }

    public static Map<String, Object> getCredentialAsMap(String holderBpn, String holderDid, String issuerDid, String type, MIWSettings miwSettings, ObjectMapper objectMapper) throws JsonProcessingException {
        // Create VC without proof
        //VC Builder
        VerifiableCredentialBuilder verifiableCredentialBuilder =
                new VerifiableCredentialBuilder();

        Map<String, Object> subjectData;
        if (Objects.equals(type, StringPool.BPN_CREDENTIAL)) {
            subjectData = Map.of(Verifiable.ID, holderDid, StringPool.BPN, holderBpn);
        } else {
            subjectData = Map.of(Verifiable.ID, "test");
        }
        //VC Subject
        VerifiableCredentialSubject verifiableCredentialSubject =
                new VerifiableCredentialSubject(subjectData);
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

        return objectMapper.readValue(credentialWithoutProof.toJson(), Map.class);
    }


    public static VerifiableCredentialStatusList2021Entry getStatusListEntry(int index) {
        return new VerifiableCredentialStatusList2021Entry(Map.of(
                "id", "https://example.com/credentials/bpn123456789000/revocation/3#" + index,
                "type", "BitstringStatusListEntry",
                "statusPurpose", "revocation",
                "statusListIndex", String.valueOf(index),
                "statusListCredential", "https://example.com/credentials/bpn123456789000/revocation/3"
        ));
    }

    public static VerifiableCredentialStatusList2021Entry getStatusListEntry() {
        int index = RandomUtils.nextInt(1, 100);
        return new VerifiableCredentialStatusList2021Entry(Map.of(
                "id", "https://example.com/credentials/bpn123456789000/revocation/3#" + index,
                "type", "BitstringStatusListEntry",
                "statusPurpose", "revocation",
                "statusListIndex", String.valueOf(index),
                "statusListCredential", "https://example.com/credentials/bpn123456789000/revocation/3"
        ));
    }

    public static void mockGetStatusListEntry(RevocationClient revocationClient, int statusIndex) {
        //mock revocation service
        Mockito.when(revocationClient.getStatusListEntry(Mockito.any(StatusListRequest.class), Mockito.any(String.class))).thenReturn(TestUtils.getStatusListEntry(statusIndex));
    }

    public static void mockGetStatusListEntry(RevocationClient revocationClient) {
        //mock revocation service
        Mockito.when(revocationClient.getStatusListEntry(Mockito.any(StatusListRequest.class), Mockito.any(String.class))).thenReturn(TestUtils.getStatusListEntry());
    }


    public static void mockRevocationVerification(RevocationClient revocationClient, CredentialStatus credentialStatus) {
        Mockito.when(revocationClient.verifyCredentialStatus(Mockito.any(), Mockito.anyString())).thenReturn(Map.of("status", credentialStatus.getName().toLowerCase()));
    }

    @SneakyThrows
    public static void mockGetStatusListVC(RevocationClient revocationClient, ObjectMapper objectMapper, String encodedList) {
        String vcString = """
                {
                  "type": [
                    "VerifiableCredential"
                  ],
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "https://w3id.org/vc/status-list/2021/v1",
                    "https://w3id.org/security/suites/jws-2020/v1"
                  ],
                  "id": "http://localhost:8085/api/v1/revocations/credentials/did:web:BPNL01-revocation",
                  "issuer": "did:key:z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce",
                  "issuanceDate": "2023-11-30T11:29:17Z",
                  "issued": "2023-11-30T11:29:17Z",
                  "validFrom": "2023-11-30T11:29:17Z",
                  "proof": {
                    "type": "JsonWebSignature2020",
                    "created": "2023-11-30T11:29:17Z",
                    "verificationMethod": "did:key:z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce#z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce",
                    "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..Iv6H_e4kfLj9dr0COsB2D_ZPpkMoFj3BVXW2iyKFC3q5QtvPWraWfzEDJ5fxtfd5bARJQIP6YhaXdfSRgJpACQ"
                  },
                  "credentialSubject": {
                    "id": "did:key:z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce",
                    "type": "BitstringStatusList",
                    "statusPurpose": "revocation",
                    "encodedList": "##encodedList"
                  }
                }
                """;
        vcString = vcString.replace("##encodedList", encodedList);

        VerifiableCredential verifiableCredential = new VerifiableCredential(objectMapper.readValue(vcString, Map.class));
        Mockito.when(revocationClient.getStatusListCredential(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class))).thenReturn(verifiableCredential);
    }

    @SneakyThrows
    public static void mockGetStatusListVC(RevocationClient revocationClient, ObjectMapper objectMapper) {
        String vcString = """
                {
                  "type": [
                    "VerifiableCredential",
                    "StatusList2021Credential"
                  ],
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "https://w3id.org/vc/status-list/2021/v1",
                    "https://w3id.org/security/suites/jws-2020/v1"
                  ],
                  "id": "http://localhost:8085/api/v1/revocations/credentials/did:web:BPNL01-revocation",
                  "issuer": "did:key:z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce",
                  "issuanceDate": "2023-11-30T11:29:17Z",
                  "issued": "2023-11-30T11:29:17Z",
                  "validFrom": "2023-11-30T11:29:17Z",
                  "proof": {
                    "type": "JsonWebSignature2020",
                    "creator": "did:key:z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce",
                    "created": "2023-11-30T11:29:17Z",
                    "verificationMethod": "did:key:z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce#z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce",
                    "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..Iv6H_e4kfLj9dr0COsB2D_ZPpkMoFj3BVXW2iyKFC3q5QtvPWraWfzEDJ5fxtfd5bARJQIP6YhaXdfSRgJpACQ"
                  },
                  "credentialSubject": {
                    "id": "did:key:z6MkhGTzcvb8BXh5aeoaFvb3XJ3MBmfLRamdYdXyV1pxJBce",
                    "type": "StatusList2021Credential",
                    "statusPurpose": "revocation",
                    "encodedList": "H4sIAAAAAAAA/+3BMQEAAAjAoEqzfzk/SwjUmQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIDXFiqoX4AAAAIA"
                  }
                }
                """;

        VerifiableCredential verifiableCredential = new VerifiableCredential(objectMapper.readValue(vcString, Map.class));
        Mockito.when(revocationClient.getStatusListCredential(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class))).thenReturn(verifiableCredential);
    }

    public static String createEncodedList() throws IOException {
        BitSet bitSet = new BitSet(16 * 1024 * 8);

        byte[] bitstringBytes = bitSet.toByteArray();
        // Perform GZIP compression
        ByteArrayOutputStream gzipOutput = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(gzipOutput)) {
            gzipStream.write(bitstringBytes);
        }


        // Base64 encode the compressed byte array
        byte[] compressedBytes = gzipOutput.toByteArray();
        String encodedList = Base64.getEncoder().encodeToString(compressedBytes);


        return encodedList;
    }
}
