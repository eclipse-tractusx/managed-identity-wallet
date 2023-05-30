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
import com.smartsensesolutions.java.commons.operator.Operator;
import com.smartsensesolutions.java.commons.sort.Sort;
import com.smartsensesolutions.java.commons.sort.SortType;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemReader;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueDismantlerCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateCredentialProblem;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.model.Ed25519Signature2020;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Credential service.
 */
@Service
@Slf4j
public class CredentialService extends BaseService<Credential, Long> {

    private final CredentialRepository credentialRepository;
    private final MIWSettings miwSettings;
    private final WalletKeyRepository walletKeyRepository;
    private final EncryptionUtils encryptionUtils;
    private final WalletService walletService;

    private final SpecificationUtil<Credential> credentialSpecificationUtil;

    public CredentialService(CredentialRepository credentialRepository, MIWSettings miwSettings, WalletKeyRepository walletKeyRepository, EncryptionUtils encryptionUtils, @Lazy WalletService walletService, SpecificationUtil<Credential> credentialSpecificationUtil) {
        this.credentialRepository = credentialRepository;
        this.miwSettings = miwSettings;
        this.walletKeyRepository = walletKeyRepository;
        this.encryptionUtils = encryptionUtils;
        this.walletService = walletService;
        this.credentialSpecificationUtil = credentialSpecificationUtil;
    }


    @Override
    protected BaseRepository<Credential, Long> getRepository() {
        return credentialRepository;
    }

    @Override
    protected SpecificationUtil<Credential> getSpecificationUtil() {
        return credentialSpecificationUtil;
    }

    /**
     * Gets credentials.
     *
     * @param holderIdentifier the holder identifier
     * @param id               the id
     * @param issuerIdentifier the issuer identifier
     * @param type             the type
     * @return the credentials
     */
    public Page<Credential> getCredentials(String holderIdentifier, String id, String issuerIdentifier, List<String> type, int pageNumber, int size, String sortColumn, String sortType) {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setPage(pageNumber);
        filterRequest.setSize(size);
        if (StringUtils.hasText(holderIdentifier)) {
            Wallet holderWallet = walletService.getWalletByIdentifier(holderIdentifier);
            filterRequest.appendNewCriteria("holderDid", Operator.EQUALS, holderWallet.getDid());
        }

        if (StringUtils.hasText(issuerIdentifier)) {
            Wallet issuerWallet = walletService.getWalletByIdentifier(issuerIdentifier);
            filterRequest.appendNewCriteria("issuerDid", Operator.EQUALS, issuerWallet.getDid());
        }

        if (!CollectionUtils.isEmpty(type)) {
            filterRequest.appendNewCriteria("type", Operator.IN, type);
        }

        Sort sort = new Sort();
        sort.setColumn(sortColumn);
        sort.setSortType(SortType.valueOf(sortType.toUpperCase()));
        filterRequest.setSort(sort);
        return filter(filterRequest);
    }

    /**
     * Issue framework credential verifiable credential.
     *
     * @param request the request
     * @return the verifiable credential
     */
    public VerifiableCredential issueFrameworkCredential(IssueFrameworkCredentialRequest request) {
        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(request.getBpn());

        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());
        byte[] privateKeyBytes = getPrivateKeyById(baseWallet.getId());

        Map<String, Object> subject = Map.of("type", request.getType(),
                "id", holderWallet.getDid(),
                "value", request.getValue(),
                "contract-template", request.getContractTemplate(),
                "contract-version", request.getContractVersion());
        Credential credential = getCredential(subject, MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX, baseWallet, privateKeyBytes, holderWallet);

        //Store Credential
        credential = create(credential);

        // Return VC
        return credential.getData();
    }

    /**
     * Issue dismantler credential verifiable credential.
     *
     * @param request the request
     * @return the verifiable credential
     */
    public VerifiableCredential issueDismantlerCredential(IssueDismantlerCredentialRequest request) {

        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(request.getBpn());

        //check duplicate
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX);

        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());
        byte[] privateKeyBytes = getPrivateKeyById(baseWallet.getId());

        Map<String, Object> subject = Map.of("type", MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL,
                "id", holderWallet.getDid(),
                "holderIdentifier", holderWallet.getBpn(),
                "activityType", request.getActivityType(),
                "allowedVehicleBrands", request.getAllowedVehicleBrands());
        Credential credential = getCredential(subject, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX, baseWallet, privateKeyBytes, holderWallet);

        //Store Credential
        credential = create(credential);

        // Return VC
        return credential.getData();
    }

    /**
     * Issue membership credential verifiable credential.
     *
     * @param issueMembershipCredentialRequest the issue membership credential request
     * @return the verifiable credential
     */
    @SneakyThrows
    public VerifiableCredential issueMembershipCredential(IssueMembershipCredentialRequest issueMembershipCredentialRequest) {

        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(issueMembershipCredentialRequest.getBpn());

        //check duplicate
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX);

        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());
        byte[] privateKeyBytes = getPrivateKeyById(baseWallet.getId());

        //VC Subject
        Credential credential = getCredential(Map.of("type", MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL,
                "id", holderWallet.getDid(),
                "holderIdentifier", holderWallet.getBpn(),
                "memberOf", baseWallet.getName(),
                "status", "Active",
                "startTime", Instant.now().toString()), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX, baseWallet, privateKeyBytes, holderWallet);

        //Store Credential
        credential = create(credential);

        // Return VC
        return credential.getData();
    }

    private VerifiableCredential createVerifiableCredential(String issuerDid, List<String> verifiableCredentialType, VerifiableCredentialSubject verifiableCredentialSubject, byte[] privateKey) {
        //VC Builder
        VerifiableCredentialBuilder builder =
                new VerifiableCredentialBuilder()
                        .context(miwSettings.vcContexts())
                        .id(URI.create(UUID.randomUUID().toString()))
                        .type(verifiableCredentialType)
                        .issuer(URI.create(issuerDid))
                        .expirationDate(miwSettings.vcExpiryDate().toInstant())
                        .issuanceDate(Instant.now())
                        .credentialSubject(verifiableCredentialSubject);


        //Ed25519 Proof Builder
        LinkedDataProofGenerator generator = LinkedDataProofGenerator.create();
        Ed25519Signature2020 proof = generator.createEd25519Signature2020(builder.build(), URI.create(issuerDid), privateKey);

        //Adding Proof to VC
        builder.proof(proof);

        //Create Credential
        return builder.build();
    }


    public Credential getCredential(Map<String, Object> subject, String type, Wallet baseWallet, byte[] privateKeyBytes, Wallet holderWallet) {
        //VC Subject
        VerifiableCredentialSubject verifiableCredentialSubject =
                new VerifiableCredentialSubject(subject);

        // VC Type
        List<String> verifiableCredentialType = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, type);

        // Create VC
        VerifiableCredential verifiableCredential = createVerifiableCredential(baseWallet.getDid(), verifiableCredentialType, verifiableCredentialSubject, privateKeyBytes);

        // Create Credential
        return Credential.builder()
                .holderDid(holderWallet.getDid())
                .issuerDid(baseWallet.getDid())
                .type(type)
                .data(verifiableCredential)
                .build();
    }


    @SneakyThrows
    public byte[] getPrivateKeyById(Long id) {
        WalletKey baseWalletKey = walletKeyRepository.getByWalletId(id);
        String privateKey = encryptionUtils.decrypt(baseWalletKey.getPrivateKey());
        return new PemReader(new StringReader(privateKey)).readPemObject().getContent();
    }

    private void isCredentialExit(String holderDid, String credentialType) {
        Validate.isTrue(credentialRepository.existsByHolderDidAndType(holderDid, credentialType)).launch(new DuplicateCredentialProblem("Credential of type " + credentialType + " is already exists "));
    }

}
