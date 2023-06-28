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
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateWalletProblem;
import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.crypt.IKeyGenerator;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.crypt.jwk.JsonWebKey;
import org.eclipse.tractusx.ssi.lib.crypt.x21559.x21559Generator;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.did.*;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
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

    private final HoldersCredentialRepository holdersCredentialRepository;

    private final SpecificationUtil<Wallet> walletSpecificationUtil;

    private final IssuersCredentialService issuersCredentialService;

    private final CommonService commonService;


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

        //validate BPN access
        Validate.isFalse(callerBpn.equalsIgnoreCase(wallet.getBpn())).launch(new ForbiddenException("Wallet BPN is not matching with request BPN(from the token)"));

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
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public Wallet createWallet(CreateWalletRequest request) {
        return createWallet(request, false);
    }

    /**
     * Create wallet.
     *
     * @param request the request
     * @return the wallet
     */
    @SneakyThrows
    private Wallet createWallet(CreateWalletRequest request, boolean authority) {
        validateCreateWallet(request);

        //create private key pair
        IKeyGenerator keyGenerator = new x21559Generator();
        KeyPair keyPair = keyGenerator.generateKey();

        //create did json
        Did did = DidWebFactory.fromHostnameAndPath(miwSettings.host(), request.getBpn());

        JsonWebKey jwk = new JsonWebKey("", keyPair.getPublicKey(), keyPair.getPrivateKey());
        JWKVerificationMethod jwkVerificationMethod =
                new JWKVerificationMethodBuilder().did(did).jwk(jwk).build();

        DidDocumentBuilder didDocumentBuilder = new DidDocumentBuilder();
        didDocumentBuilder.id(did.toUri());
        didDocumentBuilder.verificationMethods(List.of(jwkVerificationMethod));
        DidDocument didDocument = didDocumentBuilder.build();
        didDocument = DidDocument.fromJson(didDocument.toJson());
        log.debug("did document created for bpn ->{}", request.getBpn());

        //Save wallet
        Wallet wallet = create(Wallet.builder()
                .didDocument(didDocument)
                .bpn(request.getBpn())
                .name(request.getName())
                .did(did.toUri().toString())
                .algorithm(StringPool.ED_25519)
                .build());


        //Save key
        walletKeyService.getRepository().save(WalletKey.builder()
                .walletId(wallet.getId())
                .referenceKey("dummy ref key")  //TODO removed once vault setup is ready
                .vaultAccessToken("dummy vault access token") ////TODO removed once vault setup is ready
                .privateKey(encryptionUtils.encrypt(getPrivateKeyString(keyPair.getPrivateKey().asByte())))
                .publicKey(encryptionUtils.encrypt(getPublicKeyString(keyPair.getPublicKey().asByte())))
                .build());
        log.debug("Wallet created for bpn ->{}", request.getBpn());

        Wallet issuerWallet = walletRepository.getByBpn(miwSettings.authorityWalletBpn());

        //issue BPN credentials
        issuersCredentialService.issueBpnCredential(issuerWallet, wallet, authority);

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
            createWallet(request, true);
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
/*
    @SneakyThrows
    private Ed25519KeySet createKeyPair() {
        KeyPairGeneratorSpi.Ed25519 ed25519 = new KeyPairGeneratorSpi.Ed25519();
        ed25519.initialize(256, new SecureRandom());
        KeyPair keyPair = ed25519.generateKeyPair();
        PublicKey PubKey = keyPair.getPublic();
        PrivateKey PivKey = keyPair.getPrivate();
        Ed25519PrivateKeyParameters ed25519PrivateKeyParameters =
                (Ed25519PrivateKeyParameters) PrivateKeyFactory.createKey(PivKey.getEncoded());
        Ed25519PublicKeyParameters publicKeyParameters =
                (Ed25519PublicKeyParameters) PublicKeyFactory.createKey(PubKey.getEncoded());

        byte[] privateKeyBytes = ed25519PrivateKeyParameters.getEncoded();
        byte[] publicKeyBytes = publicKeyParameters.getEncoded();
        return new Ed25519KeySet(privateKeyBytes, publicKeyBytes);
    }*/

    @SneakyThrows
    private String getPrivateKeyString(byte[] privateKeyBytes) {
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(new PemObject("PRIVATE KEY", privateKeyBytes));
        pemWriter.flush();
        pemWriter.close();
        return stringWriter.toString();
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

}
