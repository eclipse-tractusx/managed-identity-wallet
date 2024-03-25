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

package org.eclipse.tractusx.managedidentitywallets.vp;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.JtiRecord;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.JtiRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.exception.MissingVcTypesException;
import org.eclipse.tractusx.managedidentitywallets.exception.PermissionViolationException;
import org.eclipse.tractusx.managedidentitywallets.service.IssuersCredentialService;
import org.eclipse.tractusx.managedidentitywallets.service.PresentationService;
import org.eclipse.tractusx.managedidentitywallets.utils.TestConstants;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COLON_SEPARATOR;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.BPN_CREDENTIAL_READ;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.BPN_CREDENTIAL_WRITE;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.EXP_VALID_DATE;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.IAT_VALID_DATE;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.INVALID_CREDENTIAL_READ;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.JWK_INNER;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.VERIFIABLE_PRESENTATION;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.buildClaimsSet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.buildJWTToken;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.createWallet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.generateUuid;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {
        ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
public class PresentationServiceTest {

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private PresentationService presentationService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JtiRepository jtiRepository;

    @Autowired
    private IssuersCredentialService issuersCredentialService;

    @Autowired
    private WalletRepository walletRepository;

    @SneakyThrows
    @Test
    void createPresentation200ResponseAsJWT() {
        boolean asJwt = true;
        String bpn = TestUtils.getRandomBpmNumber();
        String did = generateWalletAndBpnCredentialAndGetDid(bpn);
        String jtiValue = generateUuid();
        String accessToken = generateAccessToken(did, did, did, BPN_CREDENTIAL_READ, jtiValue);
        JtiRecord jtiRecord = buildJti(jtiValue, false);
        jtiRepository.save(jtiRecord);

        Map<String, Object> presentation = presentationService.createVpWithRequiredScopes(SignedJWT.parse(accessToken), asJwt);
        String vpAsJwt = String.valueOf(presentation.get(VERIFIABLE_PRESENTATION));
        JWT jwt = JWTParser.parse(vpAsJwt);

        Assertions.assertNotNull(presentation);
        Assertions.assertEquals(did, jwt.getJWTClaimsSet().getSubject());
        Assertions.assertEquals(did, jwt.getJWTClaimsSet().getIssuer());
    }

    @SneakyThrows
    @Test
    void createPresentation200ResponseAsJsonLD() {
        boolean asJwt = false;
        String bpn = TestUtils.getRandomBpmNumber();
        String did = generateWalletAndBpnCredentialAndGetDid(bpn);
        String jtiValue = generateUuid();
        String accessToken = generateAccessToken(did, did, did, BPN_CREDENTIAL_READ, jtiValue);
        JtiRecord jtiRecord = buildJti(jtiValue, false);
        jtiRepository.save(jtiRecord);

        Map<String, Object> presentation = presentationService.createVpWithRequiredScopes(SignedJWT.parse(accessToken), asJwt);
        Assertions.assertNotNull(presentation);

        VerifiablePresentation vp = (VerifiablePresentation) presentation.get(VERIFIABLE_PRESENTATION);
        Assertions.assertNotNull(vp.getVerifiableCredentials());
        VerifiableCredential verifiableCredential = vp.getVerifiableCredentials().get(0);
        VerifiableCredentialSubject verifiableCredentialSubject = verifiableCredential.getCredentialSubject().get(0);
        Assertions.assertNotNull(verifiableCredentialSubject);
        Assertions.assertEquals(bpn, verifiableCredentialSubject.get("bpn"));
        Assertions.assertEquals(did, verifiableCredentialSubject.get("id"));
    }

    @SneakyThrows
    @Test
    void createPresentation200ResponseNoJtiRecord() {
        boolean asJwt = true;
        String bpn = TestUtils.getRandomBpmNumber();
        String did = generateWalletAndBpnCredentialAndGetDid(bpn);
        String jtiValue = generateUuid();
        String accessToken = generateAccessToken(did, did, did, BPN_CREDENTIAL_READ, jtiValue);

        Map<String, Object> presentation = presentationService.createVpWithRequiredScopes(SignedJWT.parse(accessToken), asJwt);
        String vpAsJwt = String.valueOf(presentation.get(VERIFIABLE_PRESENTATION));
        JWT jwt = JWTParser.parse(vpAsJwt);

        Assertions.assertNotNull(presentation);
        Assertions.assertEquals(did, jwt.getJWTClaimsSet().getSubject());
        Assertions.assertEquals(did, jwt.getJWTClaimsSet().getIssuer());
    }

    @Test
    void createPresentationIncorrectVcTypeResponse() {
        boolean asJwt = true;
        String bpn = TestUtils.getRandomBpmNumber();
        String did = generateWalletAndBpnCredentialAndGetDid(bpn);
        String jtiValue = generateUuid();
        String accessToken = generateAccessToken(did, did, did, INVALID_CREDENTIAL_READ, jtiValue);
        JtiRecord jtiRecord = buildJti(jtiValue, false);
        jtiRepository.save(jtiRecord);

        Assertions.assertThrows(MissingVcTypesException.class, () ->
                presentationService.createVpWithRequiredScopes(SignedJWT.parse(accessToken), asJwt));
    }

    @Test
    void createPresentationIncorrectRightsRequested() {
        boolean asJwt = true;
        String jtiValue = generateUuid();
        String accessToken = generateAccessToken(DID_BPN_1, DID_BPN_1, DID_BPN_1, BPN_CREDENTIAL_WRITE, jtiValue);
        JtiRecord jtiRecord = buildJti(jtiValue, false);
        jtiRepository.save(jtiRecord);

        Assertions.assertThrows(PermissionViolationException.class, () ->
                presentationService.createVpWithRequiredScopes(SignedJWT.parse(accessToken), asJwt));
    }

    @Test
    void createPresentationIncorrectJtiAlreadyUsed() {
        boolean asJwt = false;
        String bpn = TestUtils.getRandomBpmNumber();
        String did = generateWalletAndBpnCredentialAndGetDid(bpn);
        String jtiValue = generateUuid();
        String accessToken = generateAccessToken(did, did, did, BPN_CREDENTIAL_READ, jtiValue);
        JtiRecord jtiRecord = buildJti(jtiValue, true);
        jtiRepository.save(jtiRecord);

        BadDataException ex = Assertions.assertThrows(BadDataException.class, () -> presentationService.createVpWithRequiredScopes(SignedJWT.parse(accessToken), asJwt));
        Assertions.assertEquals("The token was already used", ex.getMessage());
    }

    @SneakyThrows
    private String generateWalletAndBpnCredentialAndGetDid(String bpn) {
        String baseBpn = miwSettings.authorityWalletBpn();
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn;
        ResponseEntity<String> createWalletResponse = createWallet(bpn, "name", restTemplate, baseBpn, defaultLocation);
        Wallet wallet = TestUtils.getWalletFromString(createWalletResponse.getBody());
        Wallet issuerWallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());
        issuersCredentialService.issueBpnCredential(issuerWallet, wallet, false);
        return wallet.getDid();
    }

    private JtiRecord buildJti(String value, boolean isUsed) {
        return JtiRecord.builder().jti(UUID.fromString(value)).isUsedStatus(isUsed).build();
    }

    @SneakyThrows
    private String generateAccessToken(String issUrl, String sub, String aud, String scope, String jwt) {
        JWTClaimsSet innerSet = buildClaimsSet(issUrl, sub, aud, TestConstants.NONCE, scope, EXP_VALID_DATE, IAT_VALID_DATE, jwt);
        return buildJWTToken(JWK_INNER, innerSet);
    }
}
