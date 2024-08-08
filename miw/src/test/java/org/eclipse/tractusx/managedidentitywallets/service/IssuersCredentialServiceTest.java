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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.RevocationSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialVerificationRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialsResponse;
import org.eclipse.tractusx.managedidentitywallets.interfaces.SecureTokenService;
import org.eclipse.tractusx.managedidentitywallets.service.revocation.RevocationService;
import org.eclipse.tractusx.managedidentitywallets.signing.LocalKeyProvider;
import org.eclipse.tractusx.managedidentitywallets.signing.LocalSigningServiceImpl;
import org.eclipse.tractusx.managedidentitywallets.signing.SigningService;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.MockUtil;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.crypt.octet.OctetKeyPairFactory;
import org.eclipse.tractusx.ssi.lib.crypt.x25519.X25519PrivateKey;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.exception.did.DidParseException;
import org.eclipse.tractusx.ssi.lib.exception.key.InvalidPrivateKeyFormatException;
import org.eclipse.tractusx.ssi.lib.exception.key.KeyTransformationException;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtFactory;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtVerifier;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.did.DidMethod;
import org.eclipse.tractusx.ssi.lib.model.did.DidMethodIdentifier;
import org.eclipse.tractusx.ssi.lib.model.did.DidParser;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtVCFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.security.oauth2.jwt.JwtException;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IssuersCredentialServiceTest {
    public static final String DID_WEB_LOCALHOST = "did:web:localhost";

    public static final Did ISSUER = MockUtil.generateDid("caller");
    public static final String KEY_ID = "key-1";

    private static MIWSettings miwSettings;

    private static WalletKeyService walletKeyService;

    private static WalletKeyRepository walletKeyRepository;

    private static HoldersCredentialRepository holdersCredentialRepository;

    private static CommonService commonService;

    private static IssuersCredentialRepository issuersCredentialRepository;

    private static IssuersCredentialService issuersCredentialService;

    private static SecureTokenService secureTokenService;

    private static EncryptionUtils encryptionUtils;

    private static RevocationService revocationService;

    private static RevocationSettings revocationSettings;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void beforeAll() throws SQLException {

        miwSettings = Mockito.mock(MIWSettings.class);
        walletKeyService = Mockito.mock(WalletKeyService.class);
        holdersCredentialRepository = Mockito.mock(HoldersCredentialRepository.class);
        commonService = Mockito.mock(CommonService.class);
        issuersCredentialRepository = mock(IssuersCredentialRepository.class);
        secureTokenService = mock(SecureTokenService.class);
        walletKeyRepository = mock(WalletKeyRepository.class);
        revocationService = mock(RevocationService.class);
        revocationSettings = mock(RevocationSettings.class);

        Connection connection = mock(Connection.class);

        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);

        when(miwSettings.encryptionKey()).thenReturn("26FlcjRKOEML8YW699CXlg==");
        when(miwSettings.vcExpiryDate()).thenReturn(DateUtils.addMonths(new Date(), 10));
        encryptionUtils = new EncryptionUtils(miwSettings);

        issuersCredentialService = new IssuersCredentialService(
                issuersCredentialRepository,
                miwSettings,
                holdersCredentialRepository, commonService, objectMapper, revocationService, revocationSettings);
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(
                miwSettings,
                walletKeyService,
                holdersCredentialRepository,
                commonService,
                issuersCredentialRepository);
    }

    @Nested
    class issueCredentialUsingBaseWallet {

        @Test
        void shouldIssueCredentialAsJwt() throws InvalidPrivateKeyFormatException,
                KeyTransformationException, JwtException {
            Map<String, Object> wallets = mockBaseAndHolderWallet();
            Wallet baseWallet = (Wallet) wallets.get("base");
            String baseWalletBpn = baseWallet.getBpn();
            Wallet holderWallet = (Wallet) wallets.get("holder");
            String holderWalletBpn = holderWallet.getBpn();
            String walletKeyId = "key-1";

            KeyPair keyPair = MockUtil.generateEDKeys();
            VerifiableCredential verifiableCredential = MockUtil.getCredentialBuilder(
                    List.of("TypeA,TypeB"),
                    List.of(MockUtil.mockCredentialSubject(), MockUtil.mockCredentialSubject2()),
                    Instant.now().plus(Duration.ofDays(5)),
                    MockUtil.generateDid("basewallet")).build();

            MockUtil.makeCreateWorkForIssuer(issuersCredentialRepository);
            when(walletKeyService.getPrivateKeyByWalletIdAsBytes(baseWallet.getId(), "ED25519")).thenReturn(keyPair.getPrivateKey()
                    .asByte());
            when(commonService.getWalletByIdentifier(holderWalletBpn)).thenReturn(holderWallet);
            when(commonService.getWalletByIdentifier(verifiableCredential.getIssuer()
                    .toString())).thenReturn(baseWallet);
            when(miwSettings.authorityWalletBpn()).thenReturn(baseWalletBpn);
            when(holdersCredentialRepository.save(any(HoldersCredential.class)))
                    .thenAnswer(new Answer<HoldersCredential>() {
                        @Override
                        public HoldersCredential answer(InvocationOnMock invocation) {
                            HoldersCredential argument = invocation.getArgument(0, HoldersCredential.class);
                            argument.setId(42L);
                            return argument;
                        }
                    });

            WalletKey walletKey = mock(WalletKey.class);
            when(walletKey.getKeyId()).thenReturn(KEY_ID);
            when(baseWallet.getAlgorithm()).thenReturn("ED25519");
            when(walletKey.getId()).thenReturn(42L);
            when(walletKeyService.getPrivateKeyByWalletIdAndAlgorithm(baseWallet.getId(), SupportedAlgorithms.valueOf(baseWallet.getAlgorithm())))
                    .thenReturn(new X25519PrivateKey(keyPair.getPrivateKey().asStringForStoring(), true));
            when(walletKeyService.getWalletKeyIdByWalletId(baseWallet.getId(), SupportedAlgorithms.ED25519)).thenReturn(walletKeyId);

            when(walletKeyRepository.getByKeyIdAndAlgorithm(anyString(), anyString())).thenReturn(walletKey);
            when(baseWallet.getSigningServiceType()).thenReturn(SigningServiceType.LOCAL);
            when(walletKeyService.getPrivateKeyByKeyId(anyString(), any())).thenReturn(keyPair.getPrivateKey());
            when(walletKeyRepository.getByAlgorithmAndWallet_Bpn(anyString(), anyString())).thenReturn(walletKey);

            LocalSigningServiceImpl localSigningService = new LocalSigningServiceImpl(secureTokenService, revocationSettings);
            localSigningService.setKeyProvider(new LocalKeyProvider(walletKeyService, walletKeyRepository, encryptionUtils));

            Map<SigningServiceType, SigningService> map = new HashMap<>();
            map.put(SigningServiceType.LOCAL, localSigningService);

            issuersCredentialService.setKeyService(map);

            CredentialsResponse credentialsResponse = assertDoesNotThrow(
                    () -> issuersCredentialService.issueCredentialUsingBaseWallet(
                            holderWalletBpn,
                            verifiableCredential,
                            true, false,
                            baseWalletBpn, "dummy token"));

            validateCredentialResponse(credentialsResponse, MockUtil.buildDidDocument(new Did(new DidMethod("web"),
                    new DidMethodIdentifier("basewallet"),
                    null), keyPair));
        }
    }

    @Nested
    class jwtValidationTest {

        @RegisterExtension
        static WireMockExtension issuer = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort()
                        // .notifier(new ConsoleNotifier(true))
                )
                .build();

        @Test
        void shouldValidateAsJWT() throws DidParseException {
            Map<String, Object> wallets = mockBaseAndHolderWallet("localhost%3A" + issuer.getPort());
            Wallet baseWallet = (Wallet) wallets.get("base");
            String baseWalletDid = baseWallet.getDid();

            DidDocument issuerDidDocument = MockUtil.buildDidDocument(
                    DidParser.parse(baseWalletDid),
                    (KeyPair) wallets.get("baseKeys"));
            issuer.stubFor(
                    get("/.well-known/did.json").willReturn(ok(issuerDidDocument.toPrettyJson())));

            Wallet holderWallet = (Wallet) wallets.get("holder");
            String holderWalletDid = holderWallet.getDid();

            VerifiableCredential verifiableCredential = MockUtil.getCredentialBuilder(
                    List.of("TypeA,TypeB"),
                    List.of(MockUtil.mockCredentialSubject(), MockUtil.mockCredentialSubject2()),
                    Instant.now().plus(Duration.ofDays(5)),
                    MockUtil.generateDid("basewallet")).build();

            SerializedJwtVCFactoryImpl vcFactory = new SerializedJwtVCFactoryImpl(
                    new SignedJwtFactory(new OctetKeyPairFactory()));

            SignedJWT vcJWT = vcFactory.createVCJwt(DidParser.parse(baseWalletDid), DidParser.parse(holderWalletDid),
                    verifiableCredential,
                    ((KeyPair) wallets.get("baseKeys")).getPrivateKey(),
                    "key-1");

            String serialized = vcJWT.serialize();

            CredentialVerificationRequest credentialVerificationRequest = new CredentialVerificationRequest();
            credentialVerificationRequest.setJwt(serialized);

            Map<String, Object> stringObjectMap = assertDoesNotThrow(
                    () -> issuersCredentialService.credentialsValidation(credentialVerificationRequest, true, "dummy token"));
            assertTrue((Boolean) stringObjectMap.get(StringPool.VALID));
        }
    }

    private Map<String, Object> mockBaseAndHolderWallet() {
        KeyPair baseKeys = MockUtil.generateEDKeys();
        KeyPair holderKeys = MockUtil.generateEDKeys();
        String baseWalletBpn = TestUtils.getRandomBpmNumber();

        Wallet baseWallet = MockUtil.mockWallet(
                baseWalletBpn,
                MockUtil.generateDid("basewallet"),
                baseKeys);
        String holderWalletBpn = TestUtils.getRandomBpmNumber();
        Wallet holderWallet = MockUtil.mockWallet(
                holderWalletBpn,
                MockUtil.generateDid("holderwallet"),
                holderKeys);

        return Map.of("base", baseWallet, "holder", holderWallet, "baseKeys", baseKeys, "holderKeys", holderKeys);
    }

    private Map<String, Object> mockBaseAndHolderWallet(String baseHost) {
        KeyPair baseKeys = MockUtil.generateEDKeys();
        KeyPair holderKeys = MockUtil.generateEDKeys();
        String baseWalletBpn = TestUtils.getRandomBpmNumber();

        Wallet baseWallet = MockUtil.mockWallet(
                baseWalletBpn,
                MockUtil.generateDid(baseHost),
                baseKeys);
        String holderWalletBpn = TestUtils.getRandomBpmNumber();
        Wallet holderWallet = MockUtil.mockWallet(
                holderWalletBpn,
                MockUtil.generateDid("holderwallet"),
                holderKeys);

        return Map.of("base", baseWallet, "holder", holderWallet, "baseKeys", baseKeys, "holderKeys", holderKeys);
    }

    private void mockCommon(
            String baseWalletBpn,
            String holderWalletBpn,
            KeyPair keyPair,
            Wallet baseWallet,
            Wallet holderWallet) {
        when(miwSettings.authorityWalletBpn()).thenReturn(baseWalletBpn);
        when(commonService.getWalletByIdentifier(baseWalletBpn)).thenReturn(baseWallet);
        when(commonService.getWalletByIdentifier(holderWalletBpn)).thenReturn(holderWallet);
        when(walletKeyService.getPrivateKeyByWalletIdAsBytes(baseWallet.getId(), baseWallet.getAlgorithm()))
                .thenReturn(keyPair.getPrivateKey().asByte());
        when(holdersCredentialRepository.save(any(HoldersCredential.class)))
                .thenAnswer(new Answer<HoldersCredential>() {
                    @Override
                    public HoldersCredential answer(InvocationOnMock invocation) {
                        HoldersCredential argument = invocation.getArgument(0, HoldersCredential.class);
                        argument.setId(42L);
                        return argument;
                    }
                });
    }

    @SneakyThrows
    private void validateCredentialResponse(CredentialsResponse credentialsResponse, DidDocument didDocument) {
        assertTrue(credentialsResponse.containsKey("jwt"));
        JWSObject parsed = JWSObject.parse((String) credentialsResponse.get("jwt"));
        assertEquals("did:web:basewallet#" + KEY_ID, parsed.getHeader().getKeyID());
        assertEquals("JWT", parsed.getHeader().getType().getType());
        assertEquals("EdDSA", parsed.getHeader().getAlgorithm().getName());

        Map<String, Object> payload = parsed.getPayload().toJSONObject();
        assertTrue(payload.containsKey("vc"));

        SignedJwtVerifier jwtVerifier = new SignedJwtVerifier(new DidResolver() {
            @Override
            public DidDocument resolve(Did did) {
                return didDocument;
            }

            @Override
            public boolean isResolvable(Did did) {
                return false;
            }
        });

        SignedJWT signedJwt = SignedJWT.parse((String) credentialsResponse.get("jwt"));
        assertTrue(jwtVerifier.verify(signedJwt));
    }
}
