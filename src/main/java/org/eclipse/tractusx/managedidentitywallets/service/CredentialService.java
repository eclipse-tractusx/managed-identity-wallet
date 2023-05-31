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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueDismantlerCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateCredentialProblem;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Credential service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CredentialService extends BaseService<Credential, Long> {

    private final CredentialRepository credentialRepository;
    private final MIWSettings miwSettings;
    private final WalletService walletService;

    private final SpecificationUtil<Credential> credentialSpecificationUtil;

    private final WalletKeyService walletKeyService;


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
     * @param pageNumber       the page number
     * @param size             the size
     * @param sortColumn       the sort column
     * @param sortType         the sort type
     * @return the credentials
     */
    public List<VerifiableCredential> getCredentials(String holderIdentifier, String id, String issuerIdentifier, List<String> type, String sortColumn, String sortType) {
        FilterRequest filterRequest = new FilterRequest();
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
        Page<Credential> filter = filter(filterRequest);

        List<VerifiableCredential> list = new ArrayList<>(filter.getContent().size());
        for (Credential credential : filter.getContent()) {
            list.add(credential.getData());
        }

        return list;
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

        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());


        // get Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifier(baseWallet.getId());

        Map<String, Object> subject = Map.of("type", request.getType(),
                "id", holderWallet.getDid(),
                "value", request.getValue(),
                "contract-template", request.getContractTemplate(),
                "contract-version", request.getContractVersion());
        Credential credential = CommonUtils.getCredential(subject, MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX, miwSettings.authorityWalletDid(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate());

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

        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        //check duplicate
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX);

        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifier(baseWallet.getId());

        Map<String, Object> subject = Map.of("type", MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL,
                "id", holderWallet.getDid(),
                "holderIdentifier", holderWallet.getBpn(),
                "activityType", request.getActivityType(),
                "allowedVehicleBrands", request.getAllowedVehicleBrands());
        Credential credential = CommonUtils.getCredential(subject, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX, miwSettings.authorityWalletDid(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate());

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
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifier(baseWallet.getId());

        //VC Subject
        Credential credential = CommonUtils.getCredential(Map.of("type", MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL,
                "id", holderWallet.getDid(),
                "holderIdentifier", holderWallet.getBpn(),
                "memberOf", baseWallet.getName(),
                "status", "Active",
                "startTime", Instant.now().toString()), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX, miwSettings.authorityWalletDid(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate());

        //Store Credential
        credential = create(credential);

        // Return VC
        return credential.getData();
    }




    private void isCredentialExit(String holderDid, String credentialType) {
        Validate.isTrue(credentialRepository.existsByHolderDidAndType(holderDid, credentialType)).launch(new DuplicateCredentialProblem("Credential of type " + credentialType + " is already exists "));
    }

}
