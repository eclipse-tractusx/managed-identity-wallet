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

package org.eclipse.tractusx.managedidentitywallets.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.TokenValidationErrors;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.ValidationResult;
import org.eclipse.tractusx.managedidentitywallets.utils.TokenValidationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;

import static com.nimbusds.jose.jwk.Curve.Ed25519;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.BPN_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.BPN_2;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_BPN_2;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_JSON_STRING_1;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestConstants.DID_JSON_STRING_2;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.addAccessTokenToClaimsSet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.buildClaimsSet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.buildJWTToken;
import static org.eclipse.tractusx.managedidentitywallets.utils.TestUtils.buildWallet;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
class STSTokenValidationServiceTest {

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

    private static final Date ALREADY_EXP_DATE = new Date(Long.parseLong("1707582883000"));

    private static final Date IAT_VALID_DATE = new Date(Long.parseLong("1707496483000"));

    @Autowired
    private STSTokenValidationService stsTokenValidationService;

    @Autowired
    private TokenValidationUtils tokenValidationUtils;

    @Autowired
    private DidDocumentService didDocumentService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private MIWSettings miwSettings;

    private final Wallet wallet1 = buildWallet(BPN_1, DID_BPN_1, DID_JSON_STRING_1);

    private final Wallet wallet2 = buildWallet(BPN_2, DID_BPN_2, DID_JSON_STRING_2);

    @BeforeEach
    public void initWallets() {
        walletRepository.save(wallet1);
        walletRepository.save(wallet2);
    }

    @AfterEach
    public void cleanWallets() {
        walletRepository.deleteById(wallet1.getId());
        walletRepository.deleteById(wallet2.getId());
    }

    @Test
    void validateTokenFailureAccessTokenMissingTest() throws JOSEException {
        JWTClaimsSet outerSet = buildClaimsSet(DID_BPN_1, DID_BPN_1, DID_BPN_1, "123456", EXP_VALID_DATE, IAT_VALID_DATE);
        String siToken = buildJWTToken(JWK_OUTER, outerSet);

        ValidationResult result = stsTokenValidationService.validateToken(siToken);

        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.ACCESS_TOKEN_MISSING, result.getErrors().get(0));
    }

    @Test
    void validateTokenFailureWrongSignatureInnerTokenTest() throws JOSEException {

        OctetKeyPair jwkRandom = new OctetKeyPairGenerator(Curve.Ed25519)
                .keyID("58cb4b32-c2e4-46f0-a3ad-3286e34765ty")
                .generate();

        JWTClaimsSet innerSet = buildClaimsSet(DID_BPN_2, DID_BPN_1, DID_BPN_1, "123456", EXP_VALID_DATE, IAT_VALID_DATE);
        String accessToken = buildJWTToken(jwkRandom, innerSet);

        JWTClaimsSet outerSet = buildClaimsSet(DID_BPN_1, DID_BPN_1, DID_BPN_1, "123456", EXP_VALID_DATE, ALREADY_EXP_DATE);
        JWTClaimsSet outerSetFull = addAccessTokenToClaimsSet(accessToken, outerSet);
        String siToken = buildJWTToken(JWK_OUTER, outerSetFull);

        ValidationResult result = stsTokenValidationService.validateToken(siToken);

        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(TokenValidationErrors.SIGNATURE_NOT_VERIFIED, result.getErrors().get(0));
    }

    @Test
    void validateTokenFailureExpiredTokenIssNotEqualsSubTest() throws JOSEException {
        JWTClaimsSet innerSet = buildClaimsSet(DID_BPN_2, DID_BPN_1, DID_BPN_1, "123456", EXP_VALID_DATE, IAT_VALID_DATE);
        String accessToken = buildJWTToken(JWK_INNER, innerSet);

        JWTClaimsSet outerSet = buildClaimsSet(DID_BPN_1, DID_BPN_2, DID_BPN_1, "123456", ALREADY_EXP_DATE, IAT_VALID_DATE);
        JWTClaimsSet outerSetFull = addAccessTokenToClaimsSet(accessToken, outerSet);
        String siToken = buildJWTToken(JWK_OUTER, outerSetFull);

        ValidationResult result = stsTokenValidationService.validateToken(siToken);

        Assertions.assertFalse(result.isValid());
        Assertions.assertTrue(result.getErrors().contains(TokenValidationErrors.ISS_AND_SUB_NOT_EQUAL));
        Assertions.assertTrue(result.getErrors().contains(TokenValidationErrors.TOKEN_ALREADY_EXPIRED));
    }

    @Test
    void validateTokenSuccessTest() throws JOSEException {
        JWTClaimsSet innerSet = buildClaimsSet(DID_BPN_2, DID_BPN_1, DID_BPN_1, "123456", EXP_VALID_DATE, IAT_VALID_DATE);
        String accessToken = buildJWTToken(JWK_INNER, innerSet);

        JWTClaimsSet outerSet = buildClaimsSet(DID_BPN_1, DID_BPN_1, DID_BPN_1, "123456", EXP_VALID_DATE, IAT_VALID_DATE);
        JWTClaimsSet outerSetFull = addAccessTokenToClaimsSet(accessToken, outerSet);
        String siToken = buildJWTToken(JWK_OUTER, outerSetFull);

        ValidationResult result = stsTokenValidationService.validateToken(siToken);

        Assertions.assertTrue(result.isValid());
    }
}
