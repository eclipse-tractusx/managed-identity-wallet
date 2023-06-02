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

package org.eclipse.tractusx.managedidentitywallets.service;

import com.smartsensesolutions.java.commons.FilterRequest;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.sort.Sort;
import com.smartsensesolutions.java.commons.sort.SortType;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.ApplicationConstant;
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateWalletProblem;
import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.exception.WalletNotFoundProblem;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.base.MultibaseFactory;
import org.eclipse.tractusx.ssi.lib.crypt.ed25519.Ed25519KeySet;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.MultibaseString;
import org.eclipse.tractusx.ssi.lib.model.did.*;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Wallet service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService extends BaseService<Wallet, Long> {

    private final WalletRepository walletRepository;

    private final MIWSettings miwSettings;

    private final EncryptionUtils encryptionUtils;

    private final WalletKeyService walletKeyService;

    private final CredentialRepository credentialRepository;

    private final SpecificationUtil<Wallet> walletSpecificationUtil;

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
     * @return the map
     */
    public Map<String, String> storeCredential(Map<String, Object> data, String identifier) {
        VerifiableCredential verifiableCredential = new VerifiableCredential(data);
        Wallet wallet = getWalletByIdentifier(identifier);
        Validate.isNull(wallet).launch(new WalletNotFoundProblem("Can not find wallet with identifier " + identifier));
        String did = wallet.getDid();
        String holderDid = verifiableCredential.getCredentialSubject().get(0).get("id").toString();

        //check ownership of credentials
        Validate.isFalse(did.equals(holderDid)).launch(new ForbiddenException(String.format("The target wallet %s is not holder of provided credentials", identifier)));

        //check type
        Validate.isTrue(verifiableCredential.getTypes().isEmpty()).launch(new BadDataException("Invalid types provided in credentials"));

        if (verifiableCredential.getTypes().size() > 1) {
            verifiableCredential.getTypes().remove("VerifiableCredential");
        }
        credentialRepository.save(Credential.builder()
                .holderDid(wallet.getDid())
                .issuerDid(URLDecoder.decode(verifiableCredential.getIssuer().toString(), Charset.defaultCharset()))
                .type(verifiableCredential.getTypes().get(0))
                .data(verifiableCredential)
                .build());
        return Map.of("message", String.format("Credential with id %s has been successfully stored", verifiableCredential.getId()));
    }


    /**
     * Gets wallet by identifier.
     *
     * @param identifier      the identifier
     * @param withCredentials the with credentials
     * @return the wallet by identifier
     */
    public Wallet getWalletByIdentifier(String identifier, boolean withCredentials) {
        Wallet wallet = getWalletByIdentifier(identifier);
        if (withCredentials) {
            wallet.setVerifiableCredentials(credentialRepository.getCredentialsByHolder(wallet.getDid()));
        }
        return wallet;
    }

    /**
     * Gets wallet by identifier.
     *
     * @param identifier the identifier
     * @return the wallet by identifier
     */
    public Wallet getWalletByIdentifier(String identifier) {
        Wallet wallet;
        if (CommonUtils.getIdentifierType(identifier).equals(ApplicationConstant.BPN)) {
            wallet = walletRepository.getByBpn(identifier);
        } else {
            wallet = walletRepository.getByDid(identifier);
        }
        Validate.isNull(wallet).launch(new WalletNotFoundProblem("Wallet not found for identifier " + identifier));
        return wallet;
    }

    /**
     * Gets wallets.
     *
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
     * Create wallet.
     *
     * @param request the request
     * @return the wallet
     */
    @SneakyThrows
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public Wallet createWallet(CreateWalletRequest request) {
        validateCreateWallet(request);


        //create private key pair
        Ed25519KeySet keyPair = createKeyPair();

        //create did json
        Did did = DidWebFactory.fromHostname(miwSettings.host() + ":" + request.getBpn());

        //Extracting keys 
        Ed25519KeySet keySet = new Ed25519KeySet(keyPair.getPrivateKey(), keyPair.getPublicKey());
        MultibaseString publicKeyBase = MultibaseFactory.create(keySet.getPublicKey());

        //Building Verification Methods:
        List<VerificationMethod> verificationMethods = new ArrayList<>();
        Ed25519VerificationKey2020Builder builder = new Ed25519VerificationKey2020Builder();
        Ed25519VerificationKey2020 key =
                builder
                        .id(URI.create(did.toUri() + "#key-" + 1))
                        .controller(did.toUri())
                        .publicKeyMultiBase(publicKeyBase)
                        .build();
        verificationMethods.add(key);

        DidDocumentBuilder didDocumentBuilder = new DidDocumentBuilder();
        didDocumentBuilder.id(did.toUri());
        didDocumentBuilder.verificationMethods(verificationMethods);
        DidDocument didDocument = didDocumentBuilder.build();

        log.debug("did document created for bpn ->{}", request.getBpn());

        //Save wallet
        Wallet wallet = create(Wallet.builder()
                .didDocument(didDocument)
                .bpn(request.getBpn())
                .name(request.getName())
                .did(URLDecoder.decode(did.toUri().toString(), Charset.defaultCharset()))
                .algorithm("ED25519")
                .build());

        //Save key
        walletKeyService.getRepository().save(WalletKey.builder()
                .walletId(wallet.getId())
                .referenceKey("dummy ref key")  //TODO removed once vault setup is ready
                .vaultAccessToken("dummy vault access token") ////TODO removed once vault setup is ready
                .privateKey(encryptionUtils.encrypt(getPrivateKeyString(keyPair.getPrivateKey())))
                .publicKey(encryptionUtils.encrypt(getPublicKeyString(keyPair.getPublicKey())))
                .build());
        log.debug("Wallet created for bpn ->{}", request.getBpn());

        //issue BPN credentials``
        Wallet baseWallet = getWalletByIdentifier(miwSettings.authorityWalletBpn());
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifier(baseWallet.getId());

        Credential credential = CommonUtils.getCredential(Map.of("type", MIWVerifiableCredentialType.BPN_CREDENTIAL,
                "id", wallet.getDid(),
                "bpn", wallet.getBpn()), MIWVerifiableCredentialType.BPN_CREDENTIAL_CX, miwSettings.authorityWalletDid(), privateKeyBytes, wallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate());

        //Store Credential
        credentialRepository.save(credential);
        log.debug("BPN credential issued for bpn -{}", request.getBpn());

        return wallet;
    }

    /**
     * Create authority wallet on application start up, skip if already created.
     */
    @PostConstruct
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public void createAuthorityWallet() {
        if (!walletRepository.existsByBpn(miwSettings.authorityWalletBpn())) {
            CreateWalletRequest request = CreateWalletRequest.builder()
                    .name(miwSettings.authorityWalletName())
                    .bpn(miwSettings.authorityWalletBpn())
                    .build();
            createWallet(request);
            log.info("Authority wallet created with bpn {}", miwSettings.authorityWalletBpn());
        } else {
            log.info("Authority wallet exists with bpn {}", miwSettings.authorityWalletBpn());
        }
    }

    private void validateCreateWallet(CreateWalletRequest request) {
        boolean exist = walletRepository.existsByBpn(request.getBpn());
        if (exist) {
            throw new DuplicateWalletProblem("Wallet is already exists for bpn " + request.getBpn());
        }

    }

    @SneakyThrows
    private Ed25519KeySet createKeyPair() {
        SecureRandom secureRandom = new SecureRandom();

        Ed25519KeyPairGenerator keyPairGenerator = new Ed25519KeyPairGenerator();
        keyPairGenerator.init(new Ed25519KeyGenerationParameters(secureRandom));

        AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();
        Ed25519PrivateKeyParameters privateKey = (Ed25519PrivateKeyParameters) keyPair.getPrivate();
        Ed25519PublicKeyParameters publicKey = (Ed25519PublicKeyParameters) keyPair.getPublic();

        byte[] privateKeyBytes = privateKey.getEncoded();
        byte[] publicKeyBytes = publicKey.getEncoded();
        return new Ed25519KeySet(privateKeyBytes, publicKeyBytes);
    }


    @SneakyThrows
    private String getPrivateKeyString(byte[] privateKeyBytes) {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        pemWriter.writeObject(PrivateKeyInfoFactory.createPrivateKeyInfo(new Ed25519PrivateKeyParameters(privateKeyBytes, 0)));
        pemWriter.close();
        return stringWriter.toString();
    }

    @SneakyThrows
    private String getPublicKeyString(byte[] publicKeyBytes) {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        pemWriter.writeObject(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(new Ed25519PublicKeyParameters(publicKeyBytes, 0)));
        pemWriter.close();
        return stringWriter.toString();
    }

}