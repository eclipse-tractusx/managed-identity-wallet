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
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.IssuersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueDismantlerCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateCredentialProblem;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateSummaryCredentialProblem;
import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebDocumentResolver;
import org.eclipse.tractusx.ssi.lib.did.web.util.DidWebParser;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.eclipse.tractusx.ssi.lib.resolver.DidDocumentResolverRegistry;
import org.eclipse.tractusx.ssi.lib.resolver.DidDocumentResolverRegistryImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.*;

/**
 * The type Issuers credential service.
 */
@Service
@Slf4j
public class IssuersCredentialService extends BaseService<IssuersCredential, Long> {

    /**
     * The constant BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN.
     */
    public static final String BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN = "Base wallet BPN is not matching with request BPN(from token)";

    private final IssuersCredentialRepository issuersCredentialRepository;
    private final MIWSettings miwSettings;

    private final SpecificationUtil<IssuersCredential> credentialSpecificationUtil;

    private final WalletKeyService walletKeyService;

    private final Map<String, String> supportedFrameworkVCTypes;

    private final HoldersCredentialRepository holdersCredentialRepository;

    private final CommonService commonService;

    /**
     * Instantiates a new Issuers credential service.
     *
     * @param issuersCredentialRepository the issuers credential repository
     * @param miwSettings                 the miw settings
     * @param credentialSpecificationUtil the credential specification util
     * @param walletKeyService            the wallet key service
     * @param holdersCredentialRepository the holders credential repository
     * @param commonService               the common service
     */
    public IssuersCredentialService(IssuersCredentialRepository issuersCredentialRepository, MIWSettings miwSettings,
                                    SpecificationUtil<IssuersCredential> credentialSpecificationUtil,
                                    WalletKeyService walletKeyService, HoldersCredentialRepository holdersCredentialRepository, CommonService commonService) {
        this.issuersCredentialRepository = issuersCredentialRepository;
        this.miwSettings = miwSettings;
        this.credentialSpecificationUtil = credentialSpecificationUtil;
        this.walletKeyService = walletKeyService;
        this.holdersCredentialRepository = holdersCredentialRepository;
        this.commonService = commonService;
        Map<String, String> tmpMap = new HashMap<>();
        for (String type : org.apache.commons.lang3.StringUtils.split(miwSettings.supportedFrameworkVCTypes(), ",")) {
            tmpMap.put(type.split("=")[0].trim(), type.split("=")[1].trim());
        }
        supportedFrameworkVCTypes = ImmutableMap.copyOf(tmpMap);
    }


    @Override
    protected BaseRepository<IssuersCredential, Long> getRepository() {
        return issuersCredentialRepository;
    }

    @Override
    protected SpecificationUtil<IssuersCredential> getSpecificationUtil() {
        return credentialSpecificationUtil;
    }


    /**
     * Gets credentials.
     *
     * @param credentialId     the credential id
     * @param holderIdentifier the issuer identifier
     * @param type             the type
     * @param sortColumn       the sort column
     * @param sortType         the sort type
     * @param callerBPN        the caller bpn
     * @return the credentials
     */
    public PageImpl<VerifiableCredential> getCredentials(String credentialId, String holderIdentifier, List<String> type, String sortColumn, String sortType, int pageNumber, int size, String callerBPN) {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setSize(size);
        filterRequest.setPage(pageNumber);

        //Issuer must be caller of API
        Wallet issuerWallet = commonService.getWalletByIdentifier(callerBPN);
        filterRequest.appendCriteria(StringPool.ISSUER_DID, Operator.EQUALS, issuerWallet.getDid());

        if (StringUtils.hasText(holderIdentifier)) {
            Wallet holderWallet = commonService.getWalletByIdentifier(holderIdentifier);
            filterRequest.appendCriteria(StringPool.HOLDER_DID, Operator.EQUALS, holderWallet.getDid());
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
        Page<IssuersCredential> filter = filter(filterRequest, request, CriteriaOperator.AND);

        List<VerifiableCredential> list = new ArrayList<>(filter.getContent().size());
        for (IssuersCredential credential : filter.getContent()) {
            list.add(credential.getData());
        }
        return new PageImpl<>(list, filter.getPageable(), filter.getTotalElements());
    }


    /**
     * Issue bpn credential
     *
     * @param baseWallet   the base wallet
     * @param holderWallet the holder wallet
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public VerifiableCredential issueBpnCredential(Wallet baseWallet, Wallet holderWallet, boolean authority) {
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(baseWallet.getId());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.BPN_CREDENTIAL_CX);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(Map.of(StringPool.TYPE, MIWVerifiableCredentialType.BPN_CREDENTIAL,
                StringPool.ID, holderWallet.getDid(),
                StringPool.BPN, holderWallet.getBpn()), types, baseWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), authority);

        //Store Credential in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredentialRepository.save(issuersCredential);

        //update summery VC
        updateSummeryCredentials(baseWallet.getDidDocument(), privateKeyBytes, baseWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), MIWVerifiableCredentialType.BPN_CREDENTIAL_CX);

        log.debug("BPN credential issued for bpn -{}", holderWallet.getBpn());

        return issuersCredential.getData();
    }

    /**
     * Issue framework credential verifiable credential.
     *
     * @param request   the request
     * @param callerBPN the caller bpn
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public VerifiableCredential issueFrameworkCredential(IssueFrameworkCredentialRequest request, String callerBPN) {

        //validate type
        Validate.isFalse(supportedFrameworkVCTypes.containsKey(request.getType())).launch(new BadDataException("Framework credential of type " + request.getType() + " is not supported"));

        //validate value
        Validate.isFalse(request.getValue().equals(supportedFrameworkVCTypes.get(request.getType()))).launch(new BadDataException("Invalid value of credential type " + request.getType()));

        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(request.getBpn());

        Wallet baseWallet = commonService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        validateAccess(callerBPN, baseWallet);
        // get Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(baseWallet.getId());

        //if base wallet issue credentials to itself
        boolean isSelfIssued = isSelfIssued(request.getBpn());

        Map<String, Object> subject = Map.of(StringPool.TYPE, request.getType(),
                StringPool.ID, holderWallet.getDid(),
                StringPool.VALUE, request.getValue(),
                StringPool.CONTRACT_TEMPLATE, request.getContractTemplate(),
                StringPool.CONTRACT_VERSION, request.getContractVersion());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(subject, types, baseWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), isSelfIssued);

        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        //update summery cred
        updateSummeryCredentials(baseWallet.getDidDocument(), privateKeyBytes, baseWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), request.getType());

        log.debug("Framework VC of type ->{} issued to bpn ->{}", request.getType(), request.getBpn());

        // Return VC
        return issuersCredential.getData();
    }

    /**
     * Issue dismantler credential verifiable credential.
     *
     * @param request   the request
     * @param callerBPN the caller bpn
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public VerifiableCredential issueDismantlerCredential(IssueDismantlerCredentialRequest request, String callerBPN) {

        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(request.getBpn());

        // Fetch Issuer Wallet
        Wallet issuerWallet = commonService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        validateAccess(callerBPN, issuerWallet);

        //check duplicate
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX);

        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId());

        //if base wallet issue credentials to itself
        boolean isSelfIssued = isSelfIssued(request.getBpn());

        Map<String, Object> subject = Map.of(StringPool.TYPE, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL,
                StringPool.ID, holderWallet.getDid(),
                StringPool.HOLDER_IDENTIFIER, holderWallet.getBpn(),
                StringPool.ACTIVITY_TYPE, request.getActivityType(),
                StringPool.ALLOWED_VEHICLE_BRANDS, request.getAllowedVehicleBrands());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(subject, types, issuerWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), isSelfIssued);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        //update summery VC
        updateSummeryCredentials(issuerWallet.getDidDocument(), privateKeyBytes, issuerWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX);

        log.debug("Dismantler VC issued to bpn -> {}", request.getBpn());

        // Return VC
        return issuersCredential.getData();
    }

    /**
     * Issue membership credential verifiable credential.
     *
     * @param issueMembershipCredentialRequest the issue membership credential request
     * @param callerBPN                        the caller bpn
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public VerifiableCredential issueMembershipCredential(IssueMembershipCredentialRequest issueMembershipCredentialRequest, String callerBPN) {

        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(issueMembershipCredentialRequest.getBpn());

        //check duplicate
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX);

        // Fetch Issuer Wallet
        Wallet issuerWallet = commonService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        validateAccess(callerBPN, issuerWallet);

        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX);

        //if base wallet issue credentials to itself
        boolean isSelfIssued = isSelfIssued(issueMembershipCredentialRequest.getBpn());

        //VC Subject
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(Map.of(StringPool.TYPE, VerifiableCredentialType.MEMBERSHIP_CREDENTIAL,
                StringPool.ID, holderWallet.getDid(),
                StringPool.HOLDER_IDENTIFIER, holderWallet.getBpn(),
                StringPool.MEMBER_OF, issuerWallet.getName(),
                StringPool.STATUS, "Active",
                StringPool.START_TIME, Instant.now().toString()), types, issuerWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), isSelfIssued);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);

        //Store Credential in issuer table
        issuersCredential = create(issuersCredential);

        //update summery VC
        updateSummeryCredentials(issuerWallet.getDidDocument(), privateKeyBytes, issuerWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX);

        log.debug("Membership VC issued to bpn ->{}", issueMembershipCredentialRequest.getBpn());

        // Return VC
        return issuersCredential.getData();
    }


    /**
     * Issue credential using base wallet
     *
     * @param holderDid the holder did
     * @param data      the data
     * @param callerBpn the caller bpn
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public VerifiableCredential issueCredentialUsingBaseWallet(String holderDid, Map<String, Object> data, String callerBpn) {
        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(holderDid);

        VerifiableCredential verifiableCredential = new VerifiableCredential(data);

        Wallet issuerWallet = commonService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        validateAccess(callerBpn, issuerWallet);

        // get issuer Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId());

        boolean isSelfIssued = isSelfIssued(holderWallet.getBpn());

        // Create Credential
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(verifiableCredential.getCredentialSubject().get(0),
                verifiableCredential.getTypes(), issuerWallet.getDidDocument(),
                privateKeyBytes,
                holderWallet.getDid(),
                verifiableCredential.getContext(), Date.from(verifiableCredential.getExpirationDate()), isSelfIssued);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        log.debug("VC type of {} issued to bpn ->{}", verifiableCredential.getTypes(), holderWallet.getBpn());

        // Return VC
        return issuersCredential.getData();
    }

    /**
     * Credentials validation map.
     *
     * @param data the data
     * @return the map
     */
    public Map<String, Object> credentialsValidation(Map<String, Object> data) {
        VerifiableCredential verifiableCredential = new VerifiableCredential(data);

        // DID Resolver Constracture params
        DidDocumentResolverRegistry didDocumentResolverRegistry = new DidDocumentResolverRegistryImpl();
        didDocumentResolverRegistry.register(
                new DidWebDocumentResolver(HttpClient.newHttpClient(), new DidWebParser(), miwSettings.enforceHttps()));

        LinkedDataProofValidation proofValidation = LinkedDataProofValidation.newInstance(didDocumentResolverRegistry);
        Boolean valid = proofValidation.checkProof(verifiableCredential);
        Map<String, Object> response = new HashMap<>();
        response.put(StringPool.VALID, valid);
        response.put("vc", verifiableCredential);

        return response;
    }


    private void validateAccess(String callerBpn, Wallet issuerWallet) {
        //validate BPN access, VC must be issued by base wallet
        Validate.isFalse(callerBpn.equals(issuerWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        //issuer must be base wallet
        Validate.isFalse(issuerWallet.getBpn().equals(miwSettings.authorityWalletBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));
    }


    private void isCredentialExit(String holderDid, String credentialType) {
        Validate.isTrue(holdersCredentialRepository.existsByHolderDidAndType(holderDid, credentialType)).launch(new DuplicateCredentialProblem("Credential of type " + credentialType + " is already exists "));
    }

    private boolean isSelfIssued(String holderBpn) {
        return holderBpn.equals(miwSettings.authorityWalletBpn());
    }


    /**
     * Update summery credentials.
     *
     * @param issuerDidDocument the issuer did document
     * @param issuerPrivateKey  the issuer private key
     * @param holderBpn         the holder bpn
     * @param holderDid         the holder did
     * @param type              the type
     */
    private void updateSummeryCredentials(DidDocument issuerDidDocument, byte[] issuerPrivateKey, String issuerDid, String holderBpn, String holderDid, String type) {

        //get summery VC of holder
        List<HoldersCredential> vcs = holdersCredentialRepository.getByHolderDidAndIssuerDidAndType(holderDid, issuerDid, MIWVerifiableCredentialType.SUMMARY_CREDENTIAL);
        List<String> items;
        if (CollectionUtils.isEmpty(vcs)) {
            log.debug("No summery VC found for did ->{}", holderDid);
            items = List.of(type);
        } else {
            Validate.isTrue(vcs.size() > 1).launch(new DuplicateSummaryCredentialProblem("Something is not right, there should be only one summery VC of holder at a time"));
            HoldersCredential summeryCredential = vcs.get(0);

            //check if summery VC has subject
            Validate.isTrue(summeryCredential.getData().getCredentialSubject().isEmpty()).launch(new BadDataException("VC subject not found in existing su,,ery VC"));

            //Check if we have only one subject in summery VC
            Validate.isTrue(summeryCredential.getData().getCredentialSubject().size() > 1).launch(new BadDataException("VC subjects can more then 1 in case of summery VC"));

            VerifiableCredentialSubject subject = summeryCredential.getData().getCredentialSubject().get(0);
            if (subject.containsKey(StringPool.ITEMS)) {
                items = (List<String>) subject.get(StringPool.ITEMS);
                if (!items.contains(type)) {
                    items.add(type);
                }
            } else {
                items = List.of(type);
            }
            //delete old summery VC from holder table
            holdersCredentialRepository.deleteAll(vcs);
        }

        //issue new summery VC
        boolean isSelfIssued = isSelfIssued(holderBpn);

        Map<String, Object> subject = Map.of(StringPool.ID, holderDid,
                StringPool.HOLDER_IDENTIFIER, holderBpn,
                StringPool.TYPE, MIWVerifiableCredentialType.SUMMARY_LIST_CREDENTIAL,
                StringPool.NAME, StringPool.CX_CREDENTIALS,
                StringPool.ITEMS, items,
                StringPool.CONTRACT_TEMPLATES, miwSettings.contractTemplatesUrl());


        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.SUMMARY_CREDENTIAL);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(subject, types,
                issuerDidDocument,
                issuerPrivateKey,
                holderDid, miwSettings.vcContexts(), miwSettings.vcExpiryDate(), isSelfIssued);


        //save in holder wallet
        holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        issuersCredentialRepository.save(IssuersCredential.of(holdersCredential));

        log.info("Summery VC updated for holder did -> {}", holderDid);
    }
}