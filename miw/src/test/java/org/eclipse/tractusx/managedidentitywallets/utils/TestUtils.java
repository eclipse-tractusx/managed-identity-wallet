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
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public static void checkVC(VerifiableCredential verifiableCredential, MIWSettings miwSettings) {
        //text context URL
        Assertions.assertEquals(verifiableCredential.getContext().size(), miwSettings.vcContexts().size() + 1);

        for (URI link : miwSettings.vcContexts()) {
            Assertions.assertTrue(verifiableCredential.getContext().contains(link));
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
        System.out.println("wallet -- >" + wallet1.getBpn());
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
    public static VerifiableCredential issueCustomVCUsingBaseWallet(String holderDid, String issuerDid, String type, HttpHeaders headers,
                                                                    MIWSettings miwSettings, ObjectMapper objectMapper, TestRestTemplate restTemplate) {

        Map<String, Object> map = getCredentialAsMap(issuerDid, type, miwSettings, objectMapper);
        HttpEntity<Map> entity = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.exchange(RestURI.ISSUERS_CREDENTIALS + "?holderDid={did}", HttpMethod.POST, entity, String.class, holderDid);
        if (response.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
            throw new ForbiddenException();
        }
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        return new VerifiableCredential(new ObjectMapper().readValue(response.getBody(), Map.class));
    }

    public static Map<String, Object> getCredentialAsMap(String issuerDid, String type, MIWSettings miwSettings, ObjectMapper objectMapper) throws JsonProcessingException {
        // Create VC without proof
        //VC Builder
        VerifiableCredentialBuilder verifiableCredentialBuilder =
                new VerifiableCredentialBuilder();

        //VC Subject
        VerifiableCredentialSubject verifiableCredentialSubject =
                new VerifiableCredentialSubject(Map.of("id", "test"));

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
}
