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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
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
import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebResolver;
import org.eclipse.tractusx.ssi.lib.did.web.util.DidWebParser;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.eclipse.tractusx.ssi.lib.proof.SignatureType;
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
     * @param pageNumber       the page number
     * @param size             the size
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
     * @param authority    the authority
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public VerifiableCredential issueBpnCredential(Wallet baseWallet, Wallet holderWallet, boolean authority) {
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(baseWallet.getId(), baseWallet.getAlgorithm());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.BPN_CREDENTIAL);
        VerifiableCredentialSubject verifiableCredentialSubject = new VerifiableCredentialSubject(Map.of(StringPool.TYPE, MIWVerifiableCredentialType.BPN_CREDENTIAL,
                StringPool.ID, holderWallet.getDid(),
                StringPool.BPN, holderWallet.getBpn()));
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(verifiableCredentialSubject,
                types, baseWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), authority);

        //Store Credential in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredentialRepository.save(issuersCredential);

        //update summery VC
        updateSummeryCredentials(baseWallet.getDidDocument(), privateKeyBytes, baseWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), MIWVerifiableCredentialType.BPN_CREDENTIAL);

        log.debug("BPN credential issued for bpn -{}", StringEscapeUtils.escapeJava(holderWallet.getBpn()));

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
        Validate.isFalse(miwSettings.supportedFrameworkVCTypes().contains(request.getType())).launch(new BadDataException("Framework credential of type " + request.getType() + " is not supported, supported values are " + miwSettings.supportedFrameworkVCTypes()));

        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(request.getHolderIdentifier());

        Wallet baseWallet = commonService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        validateAccess(callerBPN, baseWallet);
        // get Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(baseWallet.getId(), baseWallet.getAlgorithm());

        //if base wallet issue credentials to itself
        boolean isSelfIssued = isSelfIssued(holderWallet.getBpn());

        VerifiableCredentialSubject subject = new VerifiableCredentialSubject(Map.of(
                StringPool.TYPE, request.getType(),
                StringPool.ID, holderWallet.getDid(),
                StringPool.HOLDER_IDENTIFIER, holderWallet.getBpn(),
                StringPool.CONTRACT_TEMPLATE, request.getContractTemplate(),
                StringPool.CONTRACT_VERSION, request.getContractVersion()));
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(subject, types, baseWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), isSelfIssued);

        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        //update summery cred
        updateSummeryCredentials(baseWallet.getDidDocument(), privateKeyBytes, baseWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), request.getType());

        log.debug("Framework VC of type ->{} issued to bpn ->{}", StringEscapeUtils.escapeJava(request.getType()), StringEscapeUtils.escapeJava(holderWallet.getBpn()));

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
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL);

        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId(), issuerWallet.getAlgorithm());

        //if base wallet issue credentials to itself
        boolean isSelfIssued = isSelfIssued(request.getBpn());

        VerifiableCredentialSubject subject = new VerifiableCredentialSubject(Map.of(StringPool.TYPE, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL,
                StringPool.ID, holderWallet.getDid(),
                StringPool.HOLDER_IDENTIFIER, holderWallet.getBpn(),
                StringPool.ACTIVITY_TYPE, request.getActivityType(),
                StringPool.ALLOWED_VEHICLE_BRANDS, request.getAllowedVehicleBrands() == null ? Collections.emptySet() : request.getAllowedVehicleBrands()));
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(subject, types, issuerWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), isSelfIssued);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        //update summery VC
        updateSummeryCredentials(issuerWallet.getDidDocument(), privateKeyBytes, issuerWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL);

        log.debug("Dismantler VC issued to bpn -> {}", StringEscapeUtils.escapeJava(request.getBpn()));

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
        isCredentialExit(holderWallet.getDid(), VerifiableCredentialType.MEMBERSHIP_CREDENTIAL);

        // Fetch Issuer Wallet
        Wallet issuerWallet = commonService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        validateAccess(callerBPN, issuerWallet);

        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId(), issuerWallet.getAlgorithm());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, VerifiableCredentialType.MEMBERSHIP_CREDENTIAL);

        //if base wallet issue credentials to itself
        boolean isSelfIssued = isSelfIssued(issueMembershipCredentialRequest.getBpn());

        //VC Subject
        VerifiableCredentialSubject verifiableCredentialSubject = new VerifiableCredentialSubject(Map.of(StringPool.TYPE, VerifiableCredentialType.MEMBERSHIP_CREDENTIAL,
                StringPool.ID, holderWallet.getDid(),
                StringPool.HOLDER_IDENTIFIER, holderWallet.getBpn(),
                StringPool.MEMBER_OF, issuerWallet.getName(),
                StringPool.STATUS, "Active",
                StringPool.START_TIME, Instant.now().toString()));
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(verifiableCredentialSubject, types, issuerWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), isSelfIssued);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);

        //Store Credential in issuer table
        issuersCredential = create(issuersCredential);

        //update summery VC
        updateSummeryCredentials(issuerWallet.getDidDocument(), privateKeyBytes, issuerWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), VerifiableCredentialType.MEMBERSHIP_CREDENTIAL);

        log.debug("Membership VC issued to bpn ->{}", StringEscapeUtils.escapeJava(issueMembershipCredentialRequest.getBpn()));

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

        //Summary VC can not be issued using API, as summary VC is issuing at runtime
        verifiableCredential.getTypes().forEach(type -> Validate.isTrue(type.equals(MIWVerifiableCredentialType.SUMMARY_CREDENTIAL)).launch(new BadDataException("Can not issue " + MIWVerifiableCredentialType.SUMMARY_CREDENTIAL + " type VC using API")));

        Wallet issuerWallet = commonService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        validateAccess(callerBpn, issuerWallet);

        // get issuer Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId(), issuerWallet.getAlgorithm());

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

        log.debug("VC type of {} issued to bpn ->{}", StringEscapeUtils.escapeJava(verifiableCredential.getTypes().toString()), StringEscapeUtils.escapeJava(holderWallet.getBpn()));

        // Return VC
        return issuersCredential.getData();
    }

    /**
     * Credentials validation map.
     *
     * @param data                     the data
     * @param withCredentialExpiryDate the with credential expiry date
     * @return the map
     */
    @SneakyThrows
    public Map<String, Object> credentialsValidation(Map<String, Object> data, boolean withCredentialExpiryDate) {
        VerifiableCredential verifiableCredential = new VerifiableCredential(data);

        DidResolver didResolver = new DidWebResolver(HttpClient.newHttpClient(), new DidWebParser(), miwSettings.enforceHttps());

        LinkedDataProofValidation proofValidation = LinkedDataProofValidation.newInstance(didResolver);

        boolean valid = proofValidation.verify(verifiableCredential);

        Map<String, Object> response = new TreeMap<>();

        //check expiry
        boolean dateValidation = CommonService.validateExpiry(withCredentialExpiryDate, verifiableCredential, response);

        response.put(StringPool.VALID, valid && dateValidation);
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

        //get last issued summary vc to holder to update items
        Page<IssuersCredential> filter = getLastIssuedSummaryCredential(issuerDid, holderDid);
        List<String> items;
        if (!filter.getContent().isEmpty()) {
            IssuersCredential issuersCredential = filter.getContent().get(0);

            //check if summery VC has subject
            Validate.isTrue(issuersCredential.getData().getCredentialSubject().isEmpty()).launch(new BadDataException("VC subject not found in existing su,,ery VC"));

            //Check if we have only one subject in summery VC
            Validate.isTrue(issuersCredential.getData().getCredentialSubject().size() > 1).launch(new BadDataException("VC subjects can more then 1 in case of summery VC"));

            VerifiableCredentialSubject subject = issuersCredential.getData().getCredentialSubject().get(0);
            if (subject.containsKey(StringPool.ITEMS)) {
                items = (List<String>) subject.get(StringPool.ITEMS);
                if (!items.contains(type)) {
                    items.add(type);
                }
            } else {
                items = List.of(type);

            }
        } else {
            items = List.of(type);
        }
        log.debug("Issuing summary VC with items ->{}", StringEscapeUtils.escapeJava(items.toString()));

        //get summery VC of holder
        List<HoldersCredential> vcs = holdersCredentialRepository.getByHolderDidAndIssuerDidAndTypeAndStored(holderDid, issuerDid, MIWVerifiableCredentialType.SUMMARY_CREDENTIAL, false); //deleted only not stored VC
        if (CollectionUtils.isEmpty(vcs)) {
            log.debug("No summery VC found for did ->{}, checking in issuer", StringEscapeUtils.escapeJava(holderDid));
        } else {
            //delete old summery VC from holder table, delete only not stored VC
            log.debug("Deleting older summary VC fir bpn -{}", holderBpn);
            holdersCredentialRepository.deleteAll(vcs);
        }

        //issue new summery VC
        boolean isSelfIssued = isSelfIssued(holderBpn);

        VerifiableCredentialSubject subject = new VerifiableCredentialSubject(Map.of(StringPool.ID, holderDid,
                StringPool.HOLDER_IDENTIFIER, holderBpn,
                StringPool.ITEMS, items,
                StringPool.TYPE, MIWVerifiableCredentialType.SUMMARY_CREDENTIAL,
                StringPool.CONTRACT_TEMPLATE, miwSettings.contractTemplatesUrl()));

        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.SUMMARY_CREDENTIAL);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(subject, types,
                issuerDidDocument,
                issuerPrivateKey,
                holderDid, miwSettings.summaryVcContexts(), miwSettings.vcExpiryDate(), isSelfIssued);


        //save in holder wallet
        holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        issuersCredentialRepository.save(IssuersCredential.of(holdersCredential));

        log.info("Summery VC updated for holder did -> {}", StringEscapeUtils.escapeJava(holderDid));
    }

    private Page<IssuersCredential> getLastIssuedSummaryCredential(String issuerDid, String holderDid) {
        FilterRequest filterRequest = new FilterRequest();

        //we need latest one record
        filterRequest.setPage(0);
        filterRequest.setSize(1);
        Sort sort = new Sort();
        sort.setColumn(StringPool.CREATED_AT);
        sort.setSortType(SortType.valueOf("desc".toUpperCase()));
        filterRequest.setSort(sort);

        filterRequest.appendCriteria(StringPool.HOLDER_DID, Operator.EQUALS, holderDid);
        filterRequest.appendCriteria(StringPool.ISSUER_DID, Operator.EQUALS, issuerDid);
        filterRequest.appendCriteria(StringPool.TYPE, Operator.EQUALS, MIWVerifiableCredentialType.SUMMARY_CREDENTIAL);

        return filter(filterRequest);
    }
}
