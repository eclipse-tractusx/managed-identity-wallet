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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
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
import org.eclipse.tractusx.ssi.lib.did.web.DidWebDocumentResolver;
import org.eclipse.tractusx.ssi.lib.did.web.util.DidWebParser;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.eclipse.tractusx.ssi.lib.resolver.DidDocumentResolverRegistry;
import org.eclipse.tractusx.ssi.lib.resolver.DidDocumentResolverRegistryImpl;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
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
    private final WalletService walletService;

    private final SpecificationUtil<IssuersCredential> credentialSpecificationUtil;

    private final WalletKeyService walletKeyService;

    private final Map<String, String> supportedFrameworkVCTypes;

    private final HoldersCredentialRepository holdersCredentialRepository;

    /**
     * Instantiates a new Issuers credential service.
     *
     * @param issuersCredentialRepository the issuers credential repository
     * @param miwSettings                 the miw settings
     * @param walletService               the wallet service
     * @param credentialSpecificationUtil the credential specification util
     * @param walletKeyService            the wallet key service
     * @param holdersCredentialRepository the holders credential repository
     */
    public IssuersCredentialService(IssuersCredentialRepository issuersCredentialRepository, MIWSettings miwSettings, WalletService walletService, SpecificationUtil<IssuersCredential> credentialSpecificationUtil, WalletKeyService walletKeyService, HoldersCredentialRepository holdersCredentialRepository) {
        this.issuersCredentialRepository = issuersCredentialRepository;
        this.miwSettings = miwSettings;
        this.walletService = walletService;
        this.credentialSpecificationUtil = credentialSpecificationUtil;
        this.walletKeyService = walletKeyService;
        this.holdersCredentialRepository = holdersCredentialRepository;
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
    public List<VerifiableCredential> getCredentials(String credentialId, String holderIdentifier, List<String> type, String sortColumn, String sortType, String callerBPN) {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(1000);

        //Issuer must be caller of API
        Wallet issuerWallet = walletService.getWalletByIdentifier(callerBPN);
        filterRequest.appendCriteria("issuerDid", Operator.EQUALS, issuerWallet.getDid());

        if (StringUtils.hasText(holderIdentifier)) {
            Wallet holderWallet = walletService.getWalletByIdentifier(holderIdentifier);
            filterRequest.appendCriteria("holderDid", Operator.EQUALS, holderWallet.getDid());
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
        Page<IssuersCredential> filter = filter(filterRequest, request, CriteriaOperator.AND);

        List<VerifiableCredential> list = new ArrayList<>(filter.getContent().size());
        for (IssuersCredential credential : filter.getContent()) {
            list.add(credential.getData());
        }

        return list;
    }


    /**
     * Issue framework credential verifiable credential.
     *
     * @param request   the request
     * @param callerBPN the caller bpn
     * @return the verifiable credential
     */
    public VerifiableCredential issueFrameworkCredential(IssueFrameworkCredentialRequest request, String callerBPN) {

        //validate type
        Validate.isFalse(supportedFrameworkVCTypes.containsKey(request.getType())).launch(new BadDataException("Framework credential of type " + request.getType() + " is not supported"));

        //validate value
        Validate.isFalse(request.getValue().equals(supportedFrameworkVCTypes.get(request.getType()))).launch(new BadDataException("Invalid value of credential type " + request.getType()));

        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(request.getBpn());

        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        //validate BPN access
        Validate.isFalse(callerBPN.equals(baseWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        // get Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(baseWallet.getId());

        Map<String, Object> subject = Map.of("type", request.getType(),
                "id", holderWallet.getDid(),
                "value", request.getValue(),
                "contract-template", request.getContractTemplate(),
                "contract-version", request.getContractVersion());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(subject, types, baseWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), false);

        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

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
    public VerifiableCredential issueDismantlerCredential(IssueDismantlerCredentialRequest request, String callerBPN) {

        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(request.getBpn());

        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        //check BPN access
        Validate.isFalse(callerBPN.equals(baseWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        //check duplicate
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX);

        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(baseWallet.getId());

        Map<String, Object> subject = Map.of("type", MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL,
                "id", holderWallet.getDid(),
                "holderIdentifier", holderWallet.getBpn(),
                "activityType", request.getActivityType(),
                "allowedVehicleBrands", request.getAllowedVehicleBrands());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX);
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(subject, types, baseWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), false);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

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
    @SneakyThrows
    public VerifiableCredential issueMembershipCredential(IssueMembershipCredentialRequest issueMembershipCredentialRequest, String callerBPN) {

        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(issueMembershipCredentialRequest.getBpn());

        //check duplicate
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX);

        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        //validate BPN access
        Validate.isFalse(callerBPN.equals(baseWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(baseWallet.getId());
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.MEMBERSHIP_CREDENTIAL_CX);
        //VC Subject
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(Map.of("type", VerifiableCredentialType.MEMBERSHIP_CREDENTIAL,
                "id", holderWallet.getDid(),
                "holderIdentifier", holderWallet.getBpn(),
                "memberOf", baseWallet.getName(),
                "status", "Active",
                "startTime", Instant.now().toString()), types, baseWallet.getDidDocument(), privateKeyBytes, holderWallet.getDid(), miwSettings.vcContexts(), miwSettings.vcExpiryDate(), false);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);

        //Store Credential in issuer table
        issuersCredential = create(issuersCredential);

        // Return VC
        return issuersCredential.getData();
    }


    /**
     * Issue credential using base wallet
     *
     * @param data      the data
     * @param callerBpn the caller bpn
     * @return the verifiable credential
     */
    public VerifiableCredential issueCredentialUsingBaseWallet(Map<String, Object> data, String callerBpn) {
        VerifiableCredential verifiableCredential = new VerifiableCredential(data);

        Wallet issuerWallet = walletService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        //validate BPN access, VC must be issued by base wallet
        Validate.isFalse(callerBpn.equals(issuerWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        //issuer must be base wallet
        Validate.isFalse(issuerWallet.getBpn().equals(miwSettings.authorityWalletBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        // get Key
        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(issuerWallet.getId());

        // Create Credential
        HoldersCredential holdersCredential = CommonUtils.getHoldersCredential(verifiableCredential.getCredentialSubject().get(0),
                verifiableCredential.getTypes(), issuerWallet.getDidDocument(),
                privateKeyBytes,
                issuerWallet.getDid(), //TODO need to check, how we can identify holder of VC, need to m
                verifiableCredential.getContext(), Date.from(verifiableCredential.getExpirationDate()), false);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        // Return VC
        return issuersCredential.getData();
    }


    private void isCredentialExit(String holderDid, String credentialType) {
        Validate.isTrue(holdersCredentialRepository.existsByHolderDidAndType(holderDid, credentialType)).launch(new DuplicateCredentialProblem("Credential of type " + credentialType + " is already exists "));
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
        response.put("valid", valid);
        response.put("vc", verifiableCredential);

        return response;
    }
}
