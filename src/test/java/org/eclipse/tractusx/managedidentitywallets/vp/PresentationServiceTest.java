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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.MissingVcTypesException;
import org.eclipse.tractusx.managedidentitywallets.exception.PermissionViolationException;
import org.eclipse.tractusx.managedidentitywallets.service.PresentationService;
import org.eclipse.tractusx.managedidentitywallets.service.WalletKeyService;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.lib.crypt.IKeyGenerator;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.crypt.x21559.x21559Generator;
import org.eclipse.tractusx.ssi.lib.exception.KeyGenerationException;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.jose.jwk.Curve.Ed25519;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.BPN_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_2;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_JSON_STRING_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.NONCE;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.READ_SCOPE;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.VERIFIABLE_PRESENTATION;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.WRITE_SCOPE;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.addAccessTokenToClaimsSet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.buildClaimsSet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.buildJWTToken;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.buildWallet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TokenParsingUtils.getAccessToken;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {
        ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
public class PresentationServiceTest {
    @Autowired
    private HoldersCredentialRepository holdersCredentialRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletKeyService walletKeyService;

    @Autowired
    private EncryptionUtils encryptionUtils;

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private PresentationService presentationService;

    private static final OctetKeyPair JWK_OUTER = new OctetKeyPair
            .Builder(Ed25519, new Base64URL("4Q5HCXPyutfcj7gLmbAKlYttlJPkykIkRjh7DH2NtZ0"))
            .d(new Base64URL("Ktp0sv9dKr_gnzRxpH5V9qpiTgZ1WbkMSv8WtWodewg"))
            .keyID("58cb4b32-c2e4-46f0-a3ad-3286e34765ed")
            .build();

    private static final OctetKeyPair JWK_INNER = new OctetKeyPair
            .Builder(Ed25519, new Base64URL("Z-8DEkN6pw2E01niDWqrp1kROLF-syIPIpFgmyrVUOU"))
            .d(new Base64URL("MLYxSai_oFzuqEfnB2diA3oDuixLg3kQzZKMyW31-2o"))
            .keyID("58cb4b32-c2e4-46f0-a3ad-3286e34765ty")
            .build();

    private static final Date EXP_VALID_DATE = new Date(Long.parseLong("2559397136000"));

    private static final Date IAT_VALID_DATE = new Date(Long.parseLong("1707496483000"));

    Wallet wallet = buildWallet(BPN_1, DID_BPN_1, DID_JSON_STRING_1);

    @SneakyThrows
    @Test
    void validateParsingFromJWTinStringFormatToSignedJWT() {
        String siToken = generateSiToken(DID_BPN_1, DID_BPN_1, DID_BPN_1, NONCE,
                READ_SCOPE, EXP_VALID_DATE, IAT_VALID_DATE);
        SignedJWT accessToken = getAccessToken(siToken);
        Assertions.assertNotNull(accessToken);
        Assertions.assertEquals(DID_BPN_1, accessToken.getJWTClaimsSet().getIssuer());
    }

    @SneakyThrows
    @Test
    void createPresentation200ResponseAsJWT() {
        boolean asJwt = true;
        String siToken = generateSiToken(DID_BPN_1, DID_BPN_1, DID_BPN_1, NONCE,
                READ_SCOPE, EXP_VALID_DATE, IAT_VALID_DATE);
        SignedJWT accessToken = storeDataAndGetSignedJWT(siToken);
        Map<String, Object> presentation = presentationService.createVpWithRequiredScopes(accessToken, asJwt);
        cleanData();
        String vpAsJwt = String.valueOf(presentation.get(VERIFIABLE_PRESENTATION));
        JWT jwt = JWTParser.parse(vpAsJwt);
        Assertions.assertNotNull(presentation);
        Assertions.assertEquals(DID_BPN_1, jwt.getJWTClaimsSet().getSubject());
        Assertions.assertEquals(DID_BPN_1, jwt.getJWTClaimsSet().getIssuer());
    }

    @SneakyThrows
    @Test
    void createPresentation200ResponseAsJsonLD() {
        boolean asJwt = false;
        String siToken = generateSiToken(DID_BPN_1, DID_BPN_1, DID_BPN_1, NONCE,
                READ_SCOPE, EXP_VALID_DATE, IAT_VALID_DATE);
        SignedJWT accessToken = storeDataAndGetSignedJWT(siToken);
        Map<String, Object> presentation = presentationService.createVpWithRequiredScopes(accessToken, asJwt);
        cleanData();
        Assertions.assertNotNull(presentation);
        VerifiablePresentation vp = (VerifiablePresentation) presentation.get(VERIFIABLE_PRESENTATION);
        Assertions.assertNotNull(vp.getVerifiableCredentials());
        VerifiableCredential verifiableCredential = vp.getVerifiableCredentials().get(0);
        Assertions.assertEquals(DID_BPN_1, verifiableCredential.getIssuer().toString());
        VerifiableCredentialSubject verifiableCredentialSubject = verifiableCredential.getCredentialSubject().get(0);
        Assertions.assertNotNull(verifiableCredentialSubject);
        Assertions.assertEquals(BPN_1, verifiableCredentialSubject.get("bpn"));
    }

    @SneakyThrows
    @Test
    void createPresentationIncorrectVcTypeResponse() {
        boolean asJwt = true;
        String siToken = generateSiToken(DID_BPN_2, DID_BPN_2, DID_BPN_2, "12345",
                READ_SCOPE, EXP_VALID_DATE, IAT_VALID_DATE);
        SignedJWT accessToken = storeDataAndGetSignedJWT(siToken);
        Assertions.assertThrows(MissingVcTypesException.class, () -> presentationService.createVpWithRequiredScopes(accessToken, asJwt));
        cleanData();
    }

    @SneakyThrows
    @Test
    void createPresentationIncorrectRightsRequested() {
        boolean asJwt = true;
        String siToken = generateSiToken(DID_BPN_1, DID_BPN_1, DID_BPN_1, NONCE,
                WRITE_SCOPE, EXP_VALID_DATE, IAT_VALID_DATE);
        SignedJWT accessToken = storeDataAndGetSignedJWT(siToken);
        Assertions.assertThrows(PermissionViolationException.class, () -> presentationService.createVpWithRequiredScopes(accessToken, asJwt));
        cleanData();
    }

    private void cleanData() {
        walletRepository.deleteById(wallet.getId());
        holdersCredentialRepository.deleteAll();
        walletKeyService.delete(2L);
    }

    private SignedJWT storeDataAndGetSignedJWT(String siToken) throws KeyGenerationException {
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.BPN_CREDENTIAL);
        VerifiableCredentialSubject verifiableCredentialSubject = new VerifiableCredentialSubject(Map.of(
                StringPool.TYPE, MIWVerifiableCredentialType.BPN_CREDENTIAL,
                StringPool.ID, DID_BPN_1,
                StringPool.BPN, BPN_1));

        //create private key pair
        IKeyGenerator keyGenerator = new x21559Generator();
        KeyPair keyPair = keyGenerator.generateKey();

        walletRepository.save(wallet);
        WalletKey walletKey = generateWalletKey(keyPair, wallet);
        walletKeyService.create(walletKey);
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(wallet.getId());
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(verifiableCredentialSubject,
                types, wallet.getDidDocument(), privateKeyBytes, DID_BPN_1, miwSettings.vcContexts(), miwSettings.vcExpiryDate(),
                true);

        SignedJWT accessToken = getAccessToken(siToken);
        holdersCredentialRepository.save(holdersCredential);
        return accessToken;
    }

    private WalletKey generateWalletKey(KeyPair keyPair, Wallet wallet) {
        return WalletKey.builder()
                .id(2L)
                .keyId(UUID.randomUUID().toString())
                .privateKey(encryptionUtils.encrypt(getPrivateKeyString(keyPair.getPrivateKey().asByte())))
                .publicKey(encryptionUtils.encrypt(getPublicKeyString(keyPair.getPublicKey().asByte())))
                .referenceKey("dummy ref key")
                .wallet(wallet)
                .vaultAccessToken("dummy vault access token")
                .build();
    }

    @SneakyThrows
    private String getPublicKeyString(byte[] publicKeyBytes) {
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKeyBytes));
        pemWriter.flush();
        pemWriter.close();
        return stringWriter.toString();
    }

    @SneakyThrows
    private String getPrivateKeyString(byte[] privateKeyBytes) {
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(new PemObject("PRIVATE KEY", privateKeyBytes));
        pemWriter.flush();
        pemWriter.close();
        return stringWriter.toString();
    }

    private String generateSiToken(String issUrl, String sub, String aud, String nonce, String scope, Date expDate, Date issDate) throws JOSEException {
        JWTClaimsSet innerSet = buildClaimsSet(issUrl, sub, aud, nonce, scope, expDate, issDate);
        String accessToken = buildJWTToken(JWK_INNER, innerSet);

        JWTClaimsSet outerSet = buildClaimsSet(DID_BPN_1, DID_BPN_1, DID_BPN_1, NONCE, "",
                EXP_VALID_DATE, IAT_VALID_DATE);
        JWTClaimsSet outerSetFull = addAccessTokenToClaimsSet(accessToken, outerSet);
        return buildJWTToken(JWK_OUTER, outerSetFull);
    }
}
