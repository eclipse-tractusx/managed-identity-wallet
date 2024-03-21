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
import com.smartsensesolutions.java.commons.criteria.CriteriaOperator;
import com.smartsensesolutions.java.commons.operator.Operator;
import com.smartsensesolutions.java.commons.sort.Sort;
import com.smartsensesolutions.java.commons.sort.SortType;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.CredentialNotFoundProblem;
import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The type Credential service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HoldersCredentialService extends BaseService<HoldersCredential, Long> {

    /**
     * The constant BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN.
     */
    public static final String BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN = "Base wallet BPN is not matching with request BPN(from token)";

    private final HoldersCredentialRepository holdersCredentialRepository;

    private final CommonService commonService;

    private final SpecificationUtil<HoldersCredential> credentialSpecificationUtil;

    private final WalletKeyService walletKeyService;

    @Override
    protected BaseRepository<HoldersCredential, Long> getRepository() {
        return holdersCredentialRepository;
    }

    @Override
    protected SpecificationUtil<HoldersCredential> getSpecificationUtil() {
        return credentialSpecificationUtil;
    }


    /**
     * Gets list of holder's credentials
     *
     * @param credentialId     the credentialId
     * @param issuerIdentifier the issuer identifier
     * @param type             the type
     * @param sortColumn       the sort column
     * @param sortType         the sort type
     * @param callerBPN        the caller bpn
     * @return the credentials
     */
    public PageImpl<VerifiableCredential> getCredentials(String credentialId, String issuerIdentifier, List<String> type, String sortColumn, String sortType, int pageNumber, int size, String callerBPN) {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setPage(pageNumber);
        filterRequest.setSize(size);

        //Holder must be caller of API
        Wallet holderWallet = commonService.getWalletByIdentifier(callerBPN);
        filterRequest.appendCriteria(StringPool.HOLDER_DID, Operator.EQUALS, holderWallet.getDid());

        if (StringUtils.hasText(issuerIdentifier)) {
            Wallet issuerWallet = commonService.getWalletByIdentifier(issuerIdentifier);
            filterRequest.appendCriteria(StringPool.ISSUER_DID, Operator.EQUALS, issuerWallet.getDid());
        }

        if (StringUtils.hasText(credentialId)) {
            filterRequest.appendCriteria(StringPool.CREDENTIAL_ID, Operator.EQUALS, credentialId);
        }
        FilterRequest request = new FilterRequest();
        if (!CollectionUtils.isEmpty(type)) {
            request.setPage(filterRequest.getPage());
            request.setSize(filterRequest.getSize());
            request.setCriteriaOperator(CriteriaOperator.OR);
            for (String str : type) {
                request.appendCriteria(StringPool.TYPE, Operator.CONTAIN, str);
            }
        }

        Sort sort = new Sort();
        sort.setColumn(sortColumn);
        sort.setSortType(SortType.valueOf(sortType.toUpperCase()));
        filterRequest.setSort(sort);
        Page<HoldersCredential> filter = filter(filterRequest, request, CriteriaOperator.AND);

        List<VerifiableCredential> list = new ArrayList<>(filter.getContent().size());
        for (HoldersCredential credential : filter.getContent()) {
            list.add(credential.getData());
        }

        return new PageImpl<>(list, filter.getPageable(), filter.getTotalElements());
    }

    /**
     * Issue credential verifiable credential.
     *
     * @param data      the data
     * @param callerBpn the caller bpn
     * @return the verifiable credential
     */
    public VerifiableCredential issueCredential(Map<String, Object> data, String callerBpn) {
        VerifiableCredential verifiableCredential = new VerifiableCredential(data);
        Wallet issuerWallet = commonService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        //validate BPN access, Holder must be caller of API
        Validate.isFalse(callerBpn.equals(issuerWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        // get Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId(), issuerWallet.getAlgorithm());

        // check if the expiryDate is set
        Date expiryDate = null;
        if (verifiableCredential.getExpirationDate() != null) {
            expiryDate = Date.from(verifiableCredential.getExpirationDate());
        }
        // Create Credential
        HoldersCredential credential = CommonUtils.getHoldersCredential(verifiableCredential.getCredentialSubject().get(0),
                verifiableCredential.getTypes(), issuerWallet.getDidDocument(),
                privateKeyBytes, issuerWallet.getDid(),
                verifiableCredential.getContext(), expiryDate, true);

        //Store Credential in holder table
        credential = create(credential);

        log.debug("VC type of {} issued to bpn ->{}", StringEscapeUtils.escapeJava(verifiableCredential.getTypes().toString()), StringEscapeUtils.escapeJava(callerBpn));
        // Return VC
        return credential.getData();
    }

    private void isCredentialExistWithId(String holderDid, String credentialId) {
        Validate.isFalse(holdersCredentialRepository.existsByHolderDidAndCredentialId(holderDid, credentialId)).launch(new CredentialNotFoundProblem("Credential ID: " + credentialId + " is not exists "));
    }
}
