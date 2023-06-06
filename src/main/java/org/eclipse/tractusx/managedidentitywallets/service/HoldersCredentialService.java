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

import com.google.common.collect.ImmutableMap;
import com.smartsensesolutions.java.commons.FilterRequest;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.criteria.CriteriaOperator;
import com.smartsensesolutions.java.commons.operator.Operator;
import com.smartsensesolutions.java.commons.sort.Sort;
import com.smartsensesolutions.java.commons.sort.SortType;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.CredentialNotFoundProblem;
import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebDocumentResolver;
import org.eclipse.tractusx.ssi.lib.did.web.util.DidWebParser;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.eclipse.tractusx.ssi.lib.resolver.DidDocumentResolverRegistry;
import org.eclipse.tractusx.ssi.lib.resolver.DidDocumentResolverRegistryImpl;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.http.HttpClient;
import java.util.*;

/**
 * The type Credential service.
 */
@Service
@Slf4j
public class HoldersCredentialService extends BaseService<HoldersCredential, Long> {

    /**
     * The constant BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN.
     */
    public static final String BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN = "Base wallet BPN is not matching with request BPN(from token)";
    private final HoldersCredentialRepository holdersCredentialRepository;
    private final MIWSettings miwSettings;
    private final WalletService walletService;

    private final SpecificationUtil<HoldersCredential> credentialSpecificationUtil;

    private final WalletKeyService walletKeyService;

    private final Map<String, String> supportedFrameworkVCTypes;

    /**
     * Instantiates a new Holders credential service.
     *
     * @param holdersCredentialRepository the holders credential repository
     * @param miwSettings                 the miw settings
     * @param walletService               the wallet service
     * @param credentialSpecificationUtil the credential specification util
     * @param walletKeyService            the wallet key service
     */
    public HoldersCredentialService(HoldersCredentialRepository holdersCredentialRepository, MIWSettings miwSettings, WalletService walletService, SpecificationUtil<HoldersCredential> credentialSpecificationUtil, WalletKeyService walletKeyService) {
        this.holdersCredentialRepository = holdersCredentialRepository;
        this.miwSettings = miwSettings;
        this.walletService = walletService;
        this.credentialSpecificationUtil = credentialSpecificationUtil;
        this.walletKeyService = walletKeyService;

        Map<String, String> tmpMap = new HashMap<>();
        for (String type : org.apache.commons.lang3.StringUtils.split(miwSettings.supportedFrameworkVCTypes(), ",")) {
            tmpMap.put(type.split("=")[0].trim(), type.split("=")[1].trim());
        }
        supportedFrameworkVCTypes = ImmutableMap.copyOf(tmpMap);

    }


    @Override
    protected BaseRepository<HoldersCredential, Long> getRepository() {
        return holdersCredentialRepository;
    }

    @Override
    protected SpecificationUtil<HoldersCredential> getSpecificationUtil() {
        return credentialSpecificationUtil;
    }


    /**
     * Gets credentials.
     *
     * @param credentialId     the credentialId
     * @param issuerIdentifier the issuer identifier
     * @param type             the type
     * @param sortColumn       the sort column
     * @param sortType         the sort type
     * @param callerBPN        the caller bpn
     * @return the credentials
     */
    public List<VerifiableCredential> getCredentials(String credentialId, String issuerIdentifier, List<String> type, String sortColumn, String sortType, String callerBPN) {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(1000);

        //Holder must be caller of API
        Wallet holderWallet = walletService.getWalletByIdentifier(callerBPN);
        filterRequest.appendCriteria("holderDid", Operator.EQUALS, holderWallet.getDid());

        if (StringUtils.hasText(issuerIdentifier)) {
            Wallet issuerWallet = walletService.getWalletByIdentifier(issuerIdentifier);
            filterRequest.appendCriteria("issuerDid", Operator.EQUALS, issuerWallet.getDid());
        }

        if (StringUtils.hasText(credentialId)) {
            filterRequest.appendCriteria("credentialId", Operator.EQUALS, credentialId);
        }
        FilterRequest request = new FilterRequest();
        if (!CollectionUtils.isEmpty(type)) {
            request.setPage(filterRequest.getPage());
            request.setSize(filterRequest.getSize());
            request.setCriteriaOperator(CriteriaOperator.OR);
            for (String str : type) {
                request.appendCriteria("type", Operator.CONTAIN, str);
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

        return list;
    }

    /**
     * Credentials validation map.
     *
     * @param data the data
     * @return the map
     */
    public Map<String, Object> credentialsValidation(Map<String, Object> data) {
        VerifiableCredential verifiableCredential = new VerifiableCredential(data);

        DidDocumentResolverRegistry didDocumentResolverRegistry = new DidDocumentResolverRegistryImpl();
        didDocumentResolverRegistry.register(
                new DidWebDocumentResolver(HttpClient.newHttpClient(), new DidWebParser(), miwSettings.enforceHttps()));

        LinkedDataProofValidation proofValidation = LinkedDataProofValidation.newInstance(didDocumentResolverRegistry);
        Boolean valid = proofValidation.checkProof(verifiableCredential);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        response.put("vc", verifiableCredential);

        return response;
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
        Wallet issuerWallet = walletService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        //validate BPN access, Holder must be caller of API
        Validate.isFalse(callerBpn.equals(issuerWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        // get Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId());

        // Create Credential
        HoldersCredential credential = CommonUtils.getHoldersCredential(verifiableCredential.getCredentialSubject().get(0),
                verifiableCredential.getTypes(), issuerWallet.getDidDocument(),
                privateKeyBytes, issuerWallet.getDid(),
                verifiableCredential.getContext(), Date.from(verifiableCredential.getExpirationDate()), true);

        //Store Credential in holder table
        credential = create(credential);

        // Return VC
        return credential.getData();
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public void deleteCredential(String credentialId, String bpnFromToken) {
        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(bpnFromToken);

        //check credential exp
        isCredentialExistWithId(holderWallet.getDid(), credentialId);

        //remove credential
        holdersCredentialRepository.deleteByCredentialId(credentialId);
    }

    private void isCredentialExistWithId(String holderDid, String credentialId) {
        Validate.isFalse(holdersCredentialRepository.existsByHolderDidAndCredentialId(holderDid, credentialId)).launch(new CredentialNotFoundProblem("Credential ID: " + credentialId + " is not exists "));
    }
}
