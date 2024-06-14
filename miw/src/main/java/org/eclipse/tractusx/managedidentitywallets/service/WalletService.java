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

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.smartsensesolutions.java.commons.FilterRequest;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.sort.Sort;
import com.smartsensesolutions.java.commons.sort.SortType;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.Validate;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateWalletProblem;
import org.eclipse.tractusx.managedidentitywallets.signing.SigningService;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.crypt.jwk.JsonWebKey;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethodBuilder;
import org.eclipse.tractusx.ssi.lib.model.did.VerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Wallet service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService extends BaseService<Wallet, Long> {


    /**
     * The constant BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN.
     */
    public static final String BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN = "Base wallet BPN is not matching with request BPN(from token)";
    private final WalletRepository walletRepository;

    private final MIWSettings miwSettings;

    private final EncryptionUtils encryptionUtils;

    private final HoldersCredentialRepository holdersCredentialRepository;

    private final SpecificationUtil<Wallet> walletSpecificationUtil;

    private final CommonService commonService;

    private final Map<SigningServiceType, SigningService> availableSigningServices;

    @Qualifier("transactionManager")
    private final PlatformTransactionManager transactionManager;

    private final JwtPresentationES256KService jwtPresentationES256KService;


    @Override
    protected BaseRepository<Wallet, Long> getRepository() {
        return walletRepository;
    }

    @Override
    protected SpecificationUtil<Wallet> getSpecificationUtil() {
        return walletSpecificationUtil;
    }

    /**
     * Store credential map.
     *
     * @param data       the data
     * @param identifier the identifier
     * @param callerBpn  the caller bpn
     * @return the map
     */
    public Map<String, String> storeCredential(Map<String, Object> data, String identifier, String callerBpn) {
        VerifiableCredential verifiableCredential = new VerifiableCredential(data);
        Wallet wallet = getWalletByIdentifier(identifier);

        //validate BPN access
        Validate.isFalse(callerBpn.equalsIgnoreCase(wallet.getBpn())).launch(new ForbiddenException("Wallet BPN is not matching with request BPN(from the token)"));

        //check type
        Validate.isTrue(verifiableCredential.getTypes().isEmpty()).launch(new BadDataException("Invalid types provided in credentials"));

        List<String> cloneTypes = new ArrayList<>(verifiableCredential.getTypes());
        cloneTypes.remove(VerifiableCredentialType.VERIFIABLE_CREDENTIAL);

        holdersCredentialRepository.save(HoldersCredential.builder()
                .holderDid(wallet.getDid())
                .issuerDid(verifiableCredential.getIssuer().toString())
                .type(String.join(",", cloneTypes))
                .data(verifiableCredential)
                .selfIssued(false)
                .stored(true)  //credential is stored(not issued by MIW)
                .credentialId(verifiableCredential.getId().toString())
                .build());
        log.debug("VC type of {} stored for bpn ->{} with id-{}", cloneTypes, callerBpn, verifiableCredential.getId());
        return Map.of("message", String.format("Credential with id %s has been successfully stored", verifiableCredential.getId()));
    }


    private Wallet getWalletByIdentifier(String identifier) {
        return commonService.getWalletByIdentifier(identifier);
    }

    /**
     * Gets wallet by identifier.
     *
     * @param identifier      the identifier
     * @param withCredentials the with credentials
     * @param callerBpn       the caller bpn
     * @return the wallet by identifier
     */
    public Wallet getWalletByIdentifier(String identifier, boolean withCredentials, String callerBpn) {
        Wallet wallet = getWalletByIdentifier(identifier);

        // authority wallet can see all wallets
        if (!miwSettings.authorityWalletBpn().equals(callerBpn)) {
            //validate BPN access
            Validate.isFalse(callerBpn.equalsIgnoreCase(wallet.getBpn())).launch(new ForbiddenException("Wallet BPN is not matching with request BPN(from the token)"));
        }

        if (withCredentials) {
            wallet.setVerifiableCredentials(holdersCredentialRepository.getCredentialsByHolder(wallet.getDid()));
        }
        return wallet;
    }


    /**
     * Gets wallets.
     *
     * @param pageNumber the page number
     * @param size       the size
     * @param sortColumn the sort column
     * @param sortType   the sort type
     * @return the wallets
     */
    public Page<Wallet> getWallets(int pageNumber, int size, String sortColumn, String sortType) {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setSize(size);
        filterRequest.setPage(pageNumber);

        Sort sort = new Sort();
        sort.setColumn(sortColumn);
        sort.setSortType(SortType.valueOf(sortType.toUpperCase()));
        filterRequest.setSort(sort);
        return filter(filterRequest);
    }

    /**
     * Create wallet wallet.
     *
     * @param request the request
     * @return the wallet
     */
    @SneakyThrows
    @Transactional
    public Wallet createWallet(CreateWalletRequest request, String callerBpn) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        final Wallet[] wallets = new Wallet[1];
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                wallets[0] = createWallet(request, false, callerBpn);
            }
        });
        return wallets[0];
    }

    /**
     * Create wallet.
     *
     * @param request the request
     * @return the wallet
     */
    @SneakyThrows
    private Wallet createWallet(CreateWalletRequest request, boolean authority, String callerBpn) {
        validateCreateWallet(request, callerBpn);

        //create private key pair
        final SigningServiceType signingServiceType;
        if (authority) {
            signingServiceType = miwSettings.authoritySigningServiceType();
        } else {
            signingServiceType = request.getSigningServiceType();
        }

        KeyCreationConfig keyCreationConfig = KeyCreationConfig.builder()
                .keyName(request.getBusinessPartnerNumber())
                .keyTypes(List.of(KeyType.OCT, KeyType.EC))
                .curve(Curve.SECP256K1)
                .build();
        SigningService signingService = availableSigningServices.get(signingServiceType);

        Map<KeyType, KeyPair> keyPairs = signingService.getKeys(keyCreationConfig);

        //create did json
        Did did = createDidJson(request.getDidUrl());
        List<WalletKeyInfo> walletKeyInfos = new ArrayList<>();
        keyPairs.forEach((k, keyPair) -> {
            String keyId = UUID.randomUUID().toString();
            JWKVerificationMethod jwkVerificationMethod;
            SupportedAlgorithms algorithm;
            if (k == KeyType.OCT) {
                algorithm = SupportedAlgorithms.ED25519;
                JsonWebKey jwk = new JsonWebKey(keyId, keyPair.getPublicKey(), keyPair.getPrivateKey());
                jwkVerificationMethod =
                        new JWKVerificationMethodBuilder().did(did).jwk(jwk).build();
            } else if (k == KeyType.EC) {
                algorithm = SupportedAlgorithms.ES256K;
                ECKey ecKey = new ECKey.Builder(Curve.SECP256K1, CommonUtils.ecPublicFrom(keyPair.getPublicKey().asByte()))
                        .privateKey(CommonUtils.ecPrivateFrom(keyPair.getPrivateKey().asByte()))
                        .keyID(keyId)
                        .keyUse(KeyUse.SIGNATURE)
                        .build();
                jwkVerificationMethod = jwtPresentationES256KService.getJwkVerificationMethod(ecKey, did);
            } else {
                throw new IllegalArgumentException("unsupported keyType %s".formatted(k.getValue()));
            }

            walletKeyInfos.add(new WalletKeyInfo(keyId, keyPair, algorithm, jwkVerificationMethod));
        });


        DidDocument didDocument = jwtPresentationES256KService.buildDidDocument(request.getBusinessPartnerNumber(), did, walletKeyInfos.stream().map(wki -> wki.verificationMethod).toList());

        //Save wallet
        Wallet wallet = create(Wallet.builder()
                .didDocument(didDocument)
                .bpn(request.getBusinessPartnerNumber())
                .name(request.getCompanyName())
                .did(did.toUri().toString())
                .algorithm(StringPool.ED_25519)
                .signingServiceType(signingServiceType)
                .build());

        var walletsKeys = walletKeyInfos.stream().map(e ->
                WalletKey.builder()
                        .wallet(wallet)
                        .keyId(e.keyId)
                        .referenceKey(StringPool.REFERENCE_KEY)
                        .vaultAccessToken(StringPool.VAULT_ACCESS_TOKEN)
                        .privateKey(encryptionUtils.encrypt(CommonUtils.getKeyString(e.keyPair.getPrivateKey().asByte(), StringPool.PRIVATE_KEY)))
                        .publicKey(encryptionUtils.encrypt(CommonUtils.getKeyString(e.keyPair.getPublicKey().asByte(), StringPool.PUBLIC_KEY)))
                        .algorithm(e.algorithm.name())
                        .build()
        ).toList();


        signingService.saveKeys(walletsKeys);

        log.debug("Wallet created for bpn ->{}", StringEscapeUtils.escapeJava(request.getBusinessPartnerNumber()));

        return wallet;
    }

    private Did createDidJson(String didUrl) {
        String[] split = didUrl.split(StringPool.COLON_SEPARATOR);
        if (split.length == 1) {
            return DidWebFactory.fromHostname(didUrl);
        } else if (split.length == 2) {
            return DidWebFactory.fromHostnameAndPath(split[0], split[1]);
        } else {
            int i = didUrl.lastIndexOf(StringPool.COLON_SEPARATOR);
            String[] splitByLast = { didUrl.substring(0, i), didUrl.substring(i + 1) };
            return DidWebFactory.fromHostnameAndPath(splitByLast[0], splitByLast[1]);
        }
    }

    /**
     * Create authority wallet on application start up, skip if already created.
     */
    @PostConstruct
    public void createAuthorityWallet() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        final Wallet[] wallets = new Wallet[1];
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                if (!walletRepository.existsByBpn(miwSettings.authorityWalletBpn())) {
                    CreateWalletRequest request = CreateWalletRequest.builder()
                            .companyName(miwSettings.authorityWalletName())
                            .businessPartnerNumber(miwSettings.authorityWalletBpn())
                            .didUrl(miwSettings.host() + StringPool.COLON_SEPARATOR + miwSettings.authorityWalletBpn())
                            .build();
                    wallets[0] = createWallet(request, true, miwSettings.authorityWalletBpn());
                    log.info("Authority wallet created with bpn {}", StringEscapeUtils.escapeJava(miwSettings.authorityWalletBpn()));
                } else {
                    log.info("Authority wallet exists with bpn {}", StringEscapeUtils.escapeJava(miwSettings.authorityWalletBpn()));
                }
            }
        });
    }


    private void validateCreateWallet(CreateWalletRequest request, String callerBpn) {
        // check base wallet
        Validate.isFalse(callerBpn.equalsIgnoreCase(miwSettings.authorityWalletBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        // check wallet already exists
        boolean exist = walletRepository.existsByBpn(request.getBusinessPartnerNumber());
        if (exist) {
            throw new DuplicateWalletProblem("Wallet is already exists for bpn " + request.getBusinessPartnerNumber());
        }
    }


    private record WalletKeyInfo(String keyId, KeyPair keyPair, SupportedAlgorithms algorithm,
                                 VerificationMethod verificationMethod) {
    }
}
