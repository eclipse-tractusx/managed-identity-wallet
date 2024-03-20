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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import com.smartsensesolutions.java.commons.FilterRequest;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.criteria.CriteriaOperator;
import com.smartsensesolutions.java.commons.operator.Operator;
import com.smartsensesolutions.java.commons.sort.Sort;
import com.smartsensesolutions.java.commons.sort.SortType;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.tractusx.managedidentitywallets.KeyStorageService;
import org.eclipse.tractusx.managedidentitywallets.command.GetCredentialsCommand;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.IssuersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.HoldersCredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyStorageType;
import org.eclipse.tractusx.managedidentitywallets.domain.VerifiableEncoding;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialVerificationRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialsResponse;
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
import org.eclipse.tractusx.ssi.lib.exception.proof.JwtExpiredException;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtValidator;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtVerifier;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.eclipse.tractusx.ssi.lib.serialization.SerializeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The type Issuers credential service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IssuersCredentialService extends BaseService<IssuersCredential, Long> {

    /**
     * The constant BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN.
     */
    public static final String BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN = "Base wallet BPN is not matching with request BPN(from token)";

    private final IssuersCredentialRepository issuersCredentialRepository;
    private final MIWSettings miwSettings;

    private final SpecificationUtil<IssuersCredential> credentialSpecificationUtil;

    private final HoldersCredentialRepository holdersCredentialRepository;

    private final CommonService commonService;

    private final ObjectMapper objectMapper;

    private final WalletKeyService walletKeyService;
    private Map<KeyStorageType, KeyStorageService> availableKeyStorage;

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
     * @param command the command
     * @return the credentials
     */
    public PageImpl<CredentialsResponse> getCredentials(GetCredentialsCommand command) {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setSize(command.getSize());
        filterRequest.setPage(command.getPageNumber());

        //Issuer must be caller of API
        Wallet issuerWallet = commonService.getWalletByIdentifier(command.getCallerBPN());
        filterRequest.appendCriteria(StringPool.ISSUER_DID, Operator.EQUALS, issuerWallet.getDid());

        if (StringUtils.hasText(command.getIdentifier())) {
            Wallet holderWallet = commonService.getWalletByIdentifier(command.getIdentifier());
            filterRequest.appendCriteria(StringPool.HOLDER_DID, Operator.EQUALS, holderWallet.getDid());
        }

        if (StringUtils.hasText(command.getCredentialId())) {
            filterRequest.appendCriteria(StringPool.CREDENTIAL_ID, Operator.EQUALS, command.getCredentialId());
        }
        FilterRequest request = new FilterRequest();
        if (!CollectionUtils.isEmpty(command.getType())) {
            request.setPage(filterRequest.getPage());
            request.setSize(filterRequest.getSize());
            request.setCriteriaOperator(CriteriaOperator.OR);
            for (String str : command.getType()) {
                request.appendCriteria(StringPool.TYPE, Operator.CONTAIN, str);
            }
        }

        Sort sort = new Sort();
        sort.setColumn(command.getSortColumn());
        sort.setSortType(SortType.valueOf(command.getSortType().toUpperCase()));
        filterRequest.setSort(sort);
        Page<IssuersCredential> filter = filter(filterRequest, request, CriteriaOperator.AND);

        List<CredentialsResponse> list = new ArrayList<>(filter.getContent().size());
        for (IssuersCredential credential : filter.getContent()) {
            CredentialsResponse cr = new CredentialsResponse();
            if (command.isAsJwt()) {
                cr.setJwt(CommonUtils.vcAsJwt(issuerWallet, command.getIdentifier() != null ? commonService.getWalletByIdentifier(command.getIdentifier()) : issuerWallet, credential.getData(), walletKeyService));
            } else {
                cr.setVc(credential.getData());
            }
            list.add(cr);
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

        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.BPN_CREDENTIAL);
        VerifiableCredentialSubject verifiableCredentialSubject = new VerifiableCredentialSubject(Map.of(StringPool.TYPE, MIWVerifiableCredentialType.BPN_CREDENTIAL,
                StringPool.ID, holderWallet.getDid(),
                StringPool.BPN, holderWallet.getBpn()));

        HoldersCredentialCreationConfig holdersCredentialCreationConfig = HoldersCredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .subject(verifiableCredentialSubject)
                .types(types)
                .issuerDoc(baseWallet.getDidDocument())
                .holderDid(holderWallet.getDid())
                .contexts(miwSettings.vcContexts())
                .expiryDate(miwSettings.vcExpiryDate())
                .selfIssued(authority)
                .walletId(baseWallet.getId())
                .build();

        HoldersCredential holdersCredential = availableKeyStorage.get(baseWallet.getKeyStorageType()).createHoldersCredential(holdersCredentialCreationConfig);

        //Store Credential in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredentialRepository.save(issuersCredential);

        //update summery VC
        updateSummeryCredentials(baseWallet.getDidDocument(), baseWallet.getId(), baseWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), MIWVerifiableCredentialType.BPN_CREDENTIAL, baseWallet.getKeyStorageType());

        log.debug("BPN credential issued for bpn -{}", StringEscapeUtils.escapeJava(holderWallet.getBpn()));

        return issuersCredential.getData();
    }

    /**
     * Issue framework credential verifiable credential.
     *
     * @param request   the request
     * @param asJwt     the as jwt
     * @param callerBPN the caller bpn
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public CredentialsResponse issueFrameworkCredential(IssueFrameworkCredentialRequest request, boolean asJwt, String callerBPN) {

        //validate type
        Validate.isFalse(miwSettings.supportedFrameworkVCTypes().contains(request.getType())).launch(new BadDataException("Framework credential of type " + request.getType() + " is not supported, supported values are " + miwSettings.supportedFrameworkVCTypes()));

        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(request.getHolderIdentifier());

        Wallet baseWallet = commonService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        validateAccess(callerBPN, baseWallet);

        //if base wallet issue credentials to itself
        boolean isSelfIssued = isSelfIssued(holderWallet.getBpn());

        VerifiableCredentialSubject subject = new VerifiableCredentialSubject(Map.of(
                StringPool.TYPE, request.getType(),
                StringPool.ID, holderWallet.getDid(),
                StringPool.HOLDER_IDENTIFIER, holderWallet.getBpn(),
                StringPool.CONTRACT_TEMPLATE, request.getContractTemplate(),
                StringPool.CONTRACT_VERSION, request.getContractVersion()));
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION);

        HoldersCredentialCreationConfig holdersCredentialCreationConfig = HoldersCredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .subject(subject)
                .types(types)
                .issuerDoc(baseWallet.getDidDocument())
                .walletId(baseWallet.getId())
                .holderDid(holderWallet.getDid())
                .contexts(miwSettings.vcContexts())
                .expiryDate(miwSettings.vcExpiryDate())
                .selfIssued(isSelfIssued)
                .build();


        HoldersCredential holdersCredential = availableKeyStorage.get(baseWallet.getKeyStorageType()).createHoldersCredential(holdersCredentialCreationConfig);

        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        //update summery cred
        updateSummeryCredentials(baseWallet.getDidDocument(), baseWallet.getId(), baseWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), request.getType(), baseWallet.getKeyStorageType());


        final CredentialsResponse cr = new CredentialsResponse();

        // Return VC
        if (asJwt) {
            cr.setJwt(CommonUtils.vcAsJwt(baseWallet, holderWallet, issuersCredential.getData(), walletKeyService));
        } else {
            cr.setVc(issuersCredential.getData());
        }

        log.debug("Framework VC of type ->{} issued to bpn ->{}", StringEscapeUtils.escapeJava(request.getType()), StringEscapeUtils.escapeJava(holderWallet.getBpn()));

        return cr;
    }

    /**
     * Issue dismantler credential verifiable credential.
     *
     * @param request   the request
     * @param asJwt     the as jwt
     * @param callerBPN the caller bpn
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public CredentialsResponse issueDismantlerCredential(IssueDismantlerCredentialRequest request, boolean asJwt, String callerBPN) {

        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(request.getBpn());

        // Fetch Issuer Wallet
        Wallet issuerWallet = commonService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        validateAccess(callerBPN, issuerWallet);

        //check duplicate
        isCredentialExit(holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL);

        //if base wallet issue credentials to itself
        boolean isSelfIssued = isSelfIssued(request.getBpn());

        VerifiableCredentialSubject subject = new VerifiableCredentialSubject(Map.of(StringPool.TYPE, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL,
                StringPool.ID, holderWallet.getDid(),
                StringPool.HOLDER_IDENTIFIER, holderWallet.getBpn(),
                StringPool.ACTIVITY_TYPE, request.getActivityType(),
                StringPool.ALLOWED_VEHICLE_BRANDS, request.getAllowedVehicleBrands() == null ? Collections.emptySet() : request.getAllowedVehicleBrands()));
        List<String> types = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL);


        HoldersCredentialCreationConfig holdersCredentialCreationConfig = HoldersCredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .subject(subject)
                .types(types)
                .issuerDoc(issuerWallet.getDidDocument())
                .walletId(issuerWallet.getId())
                .holderDid(holderWallet.getDid())
                .contexts(miwSettings.vcContexts())
                .expiryDate(miwSettings.vcExpiryDate())
                .selfIssued(isSelfIssued)
                .build();

        HoldersCredential holdersCredential = availableKeyStorage.get(issuerWallet.getKeyStorageType()).createHoldersCredential(holdersCredentialCreationConfig);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        //update summery VC
        updateSummeryCredentials(issuerWallet.getDidDocument(), issuerWallet.getId(), issuerWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL, issuerWallet.getKeyStorageType());

        final CredentialsResponse cr = new CredentialsResponse();

        // Return VC
        if (asJwt) {
            cr.setJwt(CommonUtils.vcAsJwt(issuerWallet, holderWallet, issuersCredential.getData(), walletKeyService));
        } else {
            cr.setVc(issuersCredential.getData());
        }

        log.debug("Dismantler VC issued to bpn -> {}", StringEscapeUtils.escapeJava(request.getBpn()));

        return cr;
    }

    /**
     * Issue membership credential verifiable credential.
     *
     * @param issueMembershipCredentialRequest the issue membership credential request
     * @param asJwt                            the as jwt
     * @param callerBPN                        the caller bpn
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public CredentialsResponse issueMembershipCredential(IssueMembershipCredentialRequest issueMembershipCredentialRequest, boolean asJwt, String callerBPN) {

        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(issueMembershipCredentialRequest.getBpn());

        //check duplicate
        isCredentialExit(holderWallet.getDid(), VerifiableCredentialType.MEMBERSHIP_CREDENTIAL);

        // Fetch Issuer Wallet
        Wallet issuerWallet = commonService.getWalletByIdentifier(miwSettings.authorityWalletBpn());

        validateAccess(callerBPN, issuerWallet);

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


        HoldersCredentialCreationConfig holdersCredentialCreationConfig = HoldersCredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .subject(verifiableCredentialSubject)
                .types(types)
                .issuerDoc(issuerWallet.getDidDocument())
                .walletId(issuerWallet.getId())
                .holderDid(holderWallet.getDid())
                .contexts(miwSettings.vcContexts())
                .expiryDate(miwSettings.vcExpiryDate())
                .selfIssued(isSelfIssued)
                .build();

        HoldersCredential holdersCredential = availableKeyStorage.get(issuerWallet.getKeyStorageType()).createHoldersCredential(holdersCredentialCreationConfig);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);

        //Store Credential in issuer table
        issuersCredential = create(issuersCredential);

        //update summery VC
        updateSummeryCredentials(issuerWallet.getDidDocument(), issuerWallet.getId(), issuerWallet.getDid(), holderWallet.getBpn(), holderWallet.getDid(), VerifiableCredentialType.MEMBERSHIP_CREDENTIAL, issuerWallet.getKeyStorageType());

        final CredentialsResponse cr = new CredentialsResponse();

        // Return VC
        if (asJwt) {
            cr.setJwt(CommonUtils.vcAsJwt(issuerWallet, holderWallet, issuersCredential.getData(), walletKeyService));
        } else {
            cr.setVc(issuersCredential.getData());
        }

        log.debug("Membership VC issued to bpn ->{}", StringEscapeUtils.escapeJava(issueMembershipCredentialRequest.getBpn()));

        return cr;
    }


    /**
     * Issue credential using base wallet
     *
     * @param holderDid the holder did
     * @param data      the data
     * @param asJwt     the as jwt
     * @param callerBpn the caller bpn
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public CredentialsResponse issueCredentialUsingBaseWallet(String holderDid, Map<String, Object> data, boolean asJwt, String callerBpn) {
        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(holderDid);

        VerifiableCredential verifiableCredential = new VerifiableCredential(data);

        //Summary VC can not be issued using API, as summary VC is issuing at runtime
        verifiableCredential.getTypes().forEach(type -> Validate.isTrue(type.equals(MIWVerifiableCredentialType.SUMMARY_CREDENTIAL)).launch(new BadDataException("Can not issue " + MIWVerifiableCredentialType.SUMMARY_CREDENTIAL + " type VC using API")));

        Wallet issuerWallet = commonService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        validateAccess(callerBpn, issuerWallet);

        // TODO KEYVAULT refactor this into KeyService

        boolean isSelfIssued = isSelfIssued(holderWallet.getBpn());

        HoldersCredentialCreationConfig holdersCredentialCreationConfig = HoldersCredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .subject(verifiableCredential.getCredentialSubject().get(0))
                .types(verifiableCredential.getTypes())
                .issuerDoc(issuerWallet.getDidDocument())
                .walletId(issuerWallet.getId())
                .holderDid(holderWallet.getDid())
                .contexts(verifiableCredential.getContext())
                .expiryDate(Date.from(verifiableCredential.getExpirationDate()))
                .selfIssued(isSelfIssued)
                .build();


        // Create Credential
        HoldersCredential holdersCredential = availableKeyStorage.get(issuerWallet.getKeyStorageType()).createHoldersCredential(holdersCredentialCreationConfig);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        final CredentialsResponse cr = new CredentialsResponse();

        // Return VC
        if (asJwt) {
            cr.setJwt(CommonUtils.vcAsJwt(issuerWallet, holderWallet, issuersCredential.getData(), walletKeyService));
        } else {
            cr.setVc(issuersCredential.getData());
        }

        log.debug("VC type of {} issued to bpn ->{}", StringEscapeUtils.escapeJava(verifiableCredential.getTypes().toString()), StringEscapeUtils.escapeJava(holderWallet.getBpn()));

        return cr;
    }


    private JWTVerificationResult verifyVCAsJWT(String jwt, DidResolver didResolver, boolean withCredentialsValidation, boolean withCredentialExpiryDate) throws IOException, ParseException {
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        Map<String, Object> claims = objectMapper.readValue(signedJWT.getPayload().toBytes(), Map.class);
        String vcClaim = objectMapper.writeValueAsString(claims.get("vc"));
        Map<String, Object> map = SerializeUtil.fromJson(vcClaim);
        VerifiableCredential verifiableCredential = new VerifiableCredential(map);

        //took this approach to avoid issues in sonarQube
        return new JWTVerificationResult(validateSignature(withCredentialsValidation, signedJWT, didResolver) && validateJWTExpiryDate(withCredentialExpiryDate, signedJWT), verifiableCredential);

    }

    private record JWTVerificationResult(boolean valid, VerifiableCredential verifiableCredential) {

    }

    private boolean validateSignature(boolean withValidateSignature, SignedJWT signedJWT, DidResolver didResolver) {
        if (!withValidateSignature) {
            return true;
        }
        //validate jwt signature
        try {
            SignedJwtVerifier jwtVerifier = new SignedJwtVerifier(didResolver);
            return jwtVerifier.verify(signedJWT);
        } catch (Exception e) {
            log.error("Can not verify signature of jwt", e);
            return false;
        }
    }

    private boolean validateJWTExpiryDate(boolean withExpiryDate, SignedJWT signedJWT) {
        if (!withExpiryDate) {
            return true;
        }
        try {
            SignedJwtValidator jwtValidator = new SignedJwtValidator();
            jwtValidator.validateDate(signedJWT);
            return true;
        } catch (Exception e) {
            if (!(e instanceof JwtExpiredException)) {
                log.error("Can not validate jwt expiry date ", e);
            }
            return false;
        }
    }


    /**
     * Credentials validation map.
     *
     * @param withCredentialExpiryDate the with credential expiry date
     * @return the map
     */
    public Map<String, Object> credentialsValidation(CredentialVerificationRequest verificationRequest, boolean withCredentialExpiryDate) {
        return credentialsValidation(verificationRequest, true, withCredentialExpiryDate);
    }

    /**
     * Credentials validation map.
     *
     * @param verificationRequest       the verification request
     * @param withCredentialsValidation the with credentials validation
     * @param withCredentialExpiryDate  the with credential expiry date
     * @return the map
     */
    @SneakyThrows
    public Map<String, Object> credentialsValidation(CredentialVerificationRequest verificationRequest, boolean withCredentialsValidation, boolean withCredentialExpiryDate) {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        DidResolver didResolver = new DidWebResolver(httpClient, new DidWebParser(), miwSettings.enforceHttps());
        Map<String, Object> response = new TreeMap<>();
        boolean valid;
        VerifiableCredential verifiableCredential;
        boolean dateValidation = true;

        if (verificationRequest.containsKey(StringPool.VC_JWT_KEY)) {
            JWTVerificationResult result = verifyVCAsJWT((String) verificationRequest.get(StringPool.VC_JWT_KEY), didResolver, withCredentialsValidation, withCredentialExpiryDate);
            verifiableCredential = result.verifiableCredential;
            valid = result.valid;
        } else {

            verifiableCredential = new VerifiableCredential(verificationRequest);
            LinkedDataProofValidation proofValidation = LinkedDataProofValidation.newInstance(didResolver);


            if (withCredentialsValidation) {
                valid = proofValidation.verify(verifiableCredential);
            } else {
                valid = true;
            }

            dateValidation = CommonService.validateExpiry(withCredentialExpiryDate, verifiableCredential,
                    response);
        }

        response.put(StringPool.VALID, valid && dateValidation);
        response.put(StringPool.VC, verificationRequest);

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
     * @param baseWalletId      the issuer base wallet id
     * @param holderBpn         the holder bpn
     * @param holderDid         the holder did
     * @param type              the type
     */
    private void updateSummeryCredentials(DidDocument issuerDidDocument, long baseWalletId, String issuerDid, String holderBpn, String holderDid, String type, KeyStorageType storageType) {

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

        // TODO KEYVAULT refactor this into KeyService
        HoldersCredentialCreationConfig holdersCredentialCreationConfig = HoldersCredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .subject(subject)
                .types(types)
                .issuerDoc(issuerDidDocument)
                .walletId(baseWalletId)
                .holderDid(holderDid)
                .contexts(miwSettings.summaryVcContexts())
                .expiryDate(miwSettings.vcExpiryDate())
                .selfIssued(isSelfIssued)
                .build();

        HoldersCredential holdersCredential = availableKeyStorage.get(storageType).createHoldersCredential(holdersCredentialCreationConfig);

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

    @Autowired
    public void setKeyService(Map<KeyStorageType, KeyStorageService> availableKeyStorage) {
        this.availableKeyStorage = availableKeyStorage;
    }
}
