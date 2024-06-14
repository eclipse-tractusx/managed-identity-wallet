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
import org.eclipse.tractusx.managedidentitywallets.command.GetCredentialsCommand;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.CredentialStatus;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.Validate;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.IssuersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.IssuersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.CredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.domain.VerifiableEncoding;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialVerificationRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialsResponse;
import org.eclipse.tractusx.managedidentitywallets.service.revocation.RevocationService;
import org.eclipse.tractusx.managedidentitywallets.signing.SignerResult;
import org.eclipse.tractusx.managedidentitywallets.signing.SigningService;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebResolver;
import org.eclipse.tractusx.ssi.lib.did.web.util.DidWebParser;
import org.eclipse.tractusx.ssi.lib.exception.proof.JwtExpiredException;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtValidator;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtVerifier;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialStatusList2021Entry;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private Map<SigningServiceType, SigningService> availableSigningServices;

    private final RevocationService revocationService;


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

        Wallet holderWallet = command.getIdentifier() != null ? commonService.getWalletByIdentifier(command.getIdentifier()) : issuerWallet;

        for (IssuersCredential credential : filter.getContent()) {
            CredentialsResponse cr = new CredentialsResponse();
            if (command.isAsJwt()) {
                CredentialCreationConfig config = CredentialCreationConfig.builder()
                        .algorithm(SupportedAlgorithms.ED25519)
                        .issuerDoc(issuerWallet.getDidDocument())
                        .holderDid(holderWallet.getDid())
                        .keyName(issuerWallet.getBpn())
                        .verifiableCredential(credential.getData())
                        .subject(credential.getData().getCredentialSubject().get(0))
                        .contexts(credential.getData().getContext())
                        .vcId(credential.getData().getId())
                        .types(credential.getData().getTypes())
                        .encoding(VerifiableEncoding.JWT)
                        .build();

                SignerResult signerResult = availableSigningServices.get(issuerWallet.getSigningServiceType()).createCredential(config);
                cr.setJwt(signerResult.getJwt());
            } else {
                cr.setVc(credential.getData());
            }
            list.add(cr);
        }
        return new PageImpl<>(list, filter.getPageable(), filter.getTotalElements());
    }


    /**
     * Issue credential using base wallet
     *
     * @param holderDid the holder did
     * @param data      the data
     * @param asJwt     the as jwt
     * @param revocable the revocable
     * @param callerBpn the caller bpn
     * @param token     the token
     * @return the verifiable credential
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.REQUIRED)
    public CredentialsResponse issueCredentialUsingBaseWallet(String holderDid, Map<String, Object> data, boolean asJwt, boolean revocable, String callerBpn, String token) {
        //Fetch Holder Wallet
        Wallet holderWallet = commonService.getWalletByIdentifier(holderDid);

        VerifiableCredential verifiableCredential = new VerifiableCredential(data);

        Wallet issuerWallet = commonService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        validateAccess(callerBpn, issuerWallet);

        boolean isSelfIssued = isSelfIssued(holderWallet.getBpn());


        CredentialCreationConfig.CredentialCreationConfigBuilder builder = CredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .subject(verifiableCredential.getCredentialSubject().get(0))
                .types(verifiableCredential.getTypes())
                .issuerDoc(issuerWallet.getDidDocument())
                .keyName(miwSettings.authorityWalletBpn())
                .holderDid(holderWallet.getDid())
                .contexts(verifiableCredential.getContext())
                .expiryDate(Date.from(verifiableCredential.getExpirationDate()))
                .selfIssued(isSelfIssued)
                .revocable(revocable)
                .algorithm(SupportedAlgorithms.valueOf(issuerWallet.getAlgorithm()));

        if (revocable) {
            //get credential status in case of revocation
            VerifiableCredentialStatusList2021Entry statusListEntry = revocationService.getStatusListEntry(issuerWallet.getBpn(), token);
            builder.verifiableCredentialStatus(statusListEntry);
        }

        CredentialCreationConfig holdersCredentialCreationConfig = builder.build();

        // Create Credential
        SignerResult result = availableSigningServices.get(issuerWallet.getSigningServiceType()).createCredential(holdersCredentialCreationConfig);
        VerifiableCredential vc = (VerifiableCredential) result.getJsonLd();
        HoldersCredential holdersCredential = CommonUtils.convertVerifiableCredential(vc, holdersCredentialCreationConfig);


        //save in holder wallet
        holdersCredential = holdersCredentialRepository.save(holdersCredential);

        //Store Credential in issuers table
        IssuersCredential issuersCredential = IssuersCredential.of(holdersCredential);
        issuersCredential = create(issuersCredential);

        final CredentialsResponse cr = new CredentialsResponse();

        // Return VC
        if (asJwt) {
            holdersCredentialCreationConfig.setVerifiableCredential(issuersCredential.getData());
            holdersCredentialCreationConfig.setEncoding(VerifiableEncoding.JWT);
            SignerResult credential = availableSigningServices.get(issuerWallet.getSigningServiceType()).createCredential(holdersCredentialCreationConfig);
            cr.setJwt(credential.getJwt());
        } else {
            cr.setVc(issuersCredential.getData());
        }

        log.debug("VC type of {} issued to bpn ->{}", StringEscapeUtils.escapeJava(verifiableCredential.getTypes().toString()), StringEscapeUtils.escapeJava(holderWallet.getBpn()));

        return cr;
    }


    private JWTVerificationResult verifyVCAsJWT(String jwt, DidResolver didResolver, boolean withCredentialsValidation, boolean withCredentialExpiryDate, String token) throws IOException, ParseException {
        SignedJWT signedJWT = SignedJWT.parse(jwt);
        Map<String, Object> claims = objectMapper.readValue(signedJWT.getPayload().toBytes(), Map.class);
        String vcClaim = objectMapper.writeValueAsString(claims.get("vc"));
        Map<String, Object> map = SerializeUtil.fromJson(vcClaim);
        VerifiableCredential verifiableCredential = new VerifiableCredential(map);

        //took this approach to avoid issues in sonarQube
        return new JWTVerificationResult(validateSignature(withCredentialsValidation, signedJWT, didResolver)
                && validateJWTExpiryDate(withCredentialExpiryDate, signedJWT)
                && !checkRevocationStatus(token, verifiableCredential, new HashMap<>())
                , verifiableCredential);

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
     * @param verificationRequest      the verification request
     * @param withCredentialExpiryDate the with credential expiry date
     * @param token                    the token
     * @return the map
     */
    public Map<String, Object> credentialsValidation(CredentialVerificationRequest verificationRequest, boolean withCredentialExpiryDate, String token) {
        return credentialsValidation(verificationRequest, true, withCredentialExpiryDate, token);
    }

    /**
     * Credentials validation map.
     *
     * @param verificationRequest       the verification request
     * @param withCredentialsValidation the with credentials validation
     * @param withCredentialExpiryDate  the with credential expiry date
     * @param token                     the token
     * @return the map
     */
    @SneakyThrows
    public Map<String, Object> credentialsValidation(CredentialVerificationRequest verificationRequest, boolean withCredentialsValidation,
                                                     boolean withCredentialExpiryDate, String token) {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();


        DidResolver didResolver = new DidWebResolver(httpClient, new DidWebParser(), miwSettings.enforceHttps());
        Map<String, Object> response = new TreeMap<>();
        boolean valid;
        VerifiableCredential verifiableCredential;
        boolean dateValidation = true;

        boolean revoked = false;
        if (verificationRequest.containsKey(StringPool.VC_JWT_KEY)) {
            JWTVerificationResult result = verifyVCAsJWT((String) verificationRequest.get(StringPool.VC_JWT_KEY), didResolver, withCredentialsValidation, withCredentialExpiryDate, token);
            valid = result.valid;
        } else {
            verifiableCredential = new VerifiableCredential(verificationRequest);
            LinkedDataProofValidation proofValidation = LinkedDataProofValidation.newInstance(didResolver);
            if (withCredentialsValidation) {
                valid = proofValidation.verify(verifiableCredential);
            } else {
                valid = true;
            }

            revoked = checkRevocationStatus(token, verifiableCredential, response);

            dateValidation = CommonService.validateExpiry(withCredentialExpiryDate, verifiableCredential,
                    response);
        }

        response.put(StringPool.VALID, valid && dateValidation && !revoked);
        response.put(StringPool.VC, verificationRequest);

        return response;
    }

    private boolean checkRevocationStatus(String token, VerifiableCredential verifiableCredential, Map<String, Object> response) {
        //check revocation
        if (verifiableCredential.getVerifiableCredentialStatus() != null) {
            CredentialStatus credentialStatus = revocationService.checkRevocation(verifiableCredential, token);
            response.put(StringPool.CREDENTIAL_STATUS, credentialStatus.getName());
            return !Objects.equals(credentialStatus.getName(), CredentialStatus.ACTIVE.getName());
        }
        return false;
    }


    private void validateAccess(String callerBpn, Wallet issuerWallet) {
        //validate BPN access, VC must be issued by base wallet
        Validate.isFalse(callerBpn.equals(issuerWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        //issuer must be base wallet
        Validate.isFalse(issuerWallet.getBpn().equals(miwSettings.authorityWalletBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));
    }

    private boolean isSelfIssued(String holderBpn) {
        return holderBpn.equals(miwSettings.authorityWalletBpn());
    }


    /**
     * Sets key service.
     *
     * @param availableKeyStorage the available key storage
     */
    @Autowired
    public void setKeyService(Map<SigningServiceType, SigningService> availableKeyStorage) {
        this.availableSigningServices = availableKeyStorage;
    }

}
