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

import com.smartsensesolutions.commons.dao.base.BaseRepository;
import com.smartsensesolutions.commons.dao.base.BaseService;
import com.smartsensesolutions.commons.dao.filter.FilterRequest;
import com.smartsensesolutions.commons.dao.filter.sort.Sort;
import com.smartsensesolutions.commons.dao.filter.sort.SortType;
import com.smartsensesolutions.commons.dao.operator.Operator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.tractusx.managedidentitywallets.command.GetCredentialsCommand;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.Validate;
import org.eclipse.tractusx.managedidentitywallets.config.RevocationSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.CredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.domain.VerifiableEncoding;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialsResponse;
import org.eclipse.tractusx.managedidentitywallets.service.revocation.RevocationService;
import org.eclipse.tractusx.managedidentitywallets.signing.SignerResult;
import org.eclipse.tractusx.managedidentitywallets.signing.SigningService;
import org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialStatusList2021Entry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
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

    private final Map<SigningServiceType, SigningService> availableSigningServices;

    private final RevocationService revocationService;

    private final RevocationSettings revocationSettings;


    @Override
    protected BaseRepository<HoldersCredential, Long> getRepository() {
        return holdersCredentialRepository;
    }


    /**
     * Gets credentials.
     *
     * @param command the command
     * @return the credentials
     */
    public PageImpl<CredentialsResponse> getCredentials(GetCredentialsCommand command) {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setPage(command.getPageNumber());
        filterRequest.setSize(command.getSize());

        //Holder must be caller of API
        Wallet holderWallet = commonService.getWalletByIdentifier(command.getCallerBPN());
        filterRequest.appendCriteria(StringPool.HOLDER_DID, Operator.EQUALS, holderWallet.getDid());

        if (StringUtils.hasText(command.getIdentifier())) {
            Wallet issuerWallet = commonService.getWalletByIdentifier(command.getIdentifier());
            filterRequest.appendCriteria(StringPool.ISSUER_DID, Operator.EQUALS, issuerWallet.getDid());
        }

        if (StringUtils.hasText(command.getCredentialId())) {
            filterRequest.appendCriteria(StringPool.CREDENTIAL_ID, Operator.EQUALS, command.getCredentialId());
        }
        if (!CollectionUtils.isEmpty(command.getType())) {
            for (String str : command.getType()) {
                filterRequest.appendOrCriteria(StringPool.TYPE, Operator.CONTAIN, str);
            }
        }

        Sort sort = new Sort();
        sort.setColumn(command.getSortColumn());
        sort.setSortType(SortType.valueOf(command.getSortType().toUpperCase()));
        filterRequest.setSort(List.of(sort));
        Page<HoldersCredential> filter = filter(filterRequest);

        List<CredentialsResponse> list = new ArrayList<>(filter.getContent().size());

        Wallet issuerWallet = command.getIdentifier() != null ? commonService.getWalletByIdentifier(command.getIdentifier()) : holderWallet;

        for (HoldersCredential credential : filter.getContent()) {
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
     * Issue credential verifiable credential.
     *
     * @param data      the data
     * @param callerBpn the caller bpn
     * @param asJwt     the as jwt
     * @return the verifiable credential
     */
    public CredentialsResponse issueCredential(Map<String, Object> data, String callerBpn, boolean asJwt, boolean revocable, String token) {
        VerifiableCredential verifiableCredential = new VerifiableCredential(data);
        Wallet issuerWallet = commonService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        //validate BPN access, Holder must be caller of API
        Validate.isFalse(callerBpn.equals(issuerWallet.getBpn())).launch(new ForbiddenException(BASE_WALLET_BPN_IS_NOT_MATCHING_WITH_REQUEST_BPN_FROM_TOKEN));

        // check if the expiryDate is set
        Date expiryDate = null;
        if (verifiableCredential.getExpirationDate() != null) {
            expiryDate = Date.from(verifiableCredential.getExpirationDate());
        }

        CredentialCreationConfig.CredentialCreationConfigBuilder builder = CredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .subject(verifiableCredential.getCredentialSubject().get(0))
                .types(verifiableCredential.getTypes())
                .issuerDoc(issuerWallet.getDidDocument())
                .holderDid(issuerWallet.getDid())
                .contexts(verifiableCredential.getContext())
                .expiryDate(expiryDate)
                .selfIssued(true)
                .revocable(revocable)
                .keyName(issuerWallet.getBpn())
                .algorithm(SupportedAlgorithms.valueOf(issuerWallet.getAlgorithm()));

        if (revocable) {
            //get credential status in case of revocation
            VerifiableCredentialStatusList2021Entry statusListEntry = revocationService.getStatusListEntry(issuerWallet.getBpn(), token);
            builder.verifiableCredentialStatus(statusListEntry);

            //add revocation context if missing
            List<URI> uris = verifiableCredential.getContext();
            if (!uris.contains(revocationSettings.bitStringStatusListContext())) {
                uris.add(revocationSettings.bitStringStatusListContext());
                builder.contexts(uris);
            }

        }

        CredentialCreationConfig holdersCredentialCreationConfig = builder.build();

        // Create Credential
        SignerResult signerResult = availableSigningServices.get(issuerWallet.getSigningServiceType()).createCredential(holdersCredentialCreationConfig);
        VerifiableCredential vc = (VerifiableCredential) signerResult.getJsonLd();
        HoldersCredential credential = CommonUtils.convertVerifiableCredential(vc, holdersCredentialCreationConfig);


        //Store Credential in holder table
        credential = create(credential);

        final CredentialsResponse cr = new CredentialsResponse();

        // Return VC
        if (asJwt) {
            holdersCredentialCreationConfig.setVerifiableCredential(credential.getData());
            holdersCredentialCreationConfig.setEncoding(VerifiableEncoding.JWT);
            SignerResult signerJwtResult = availableSigningServices.get(issuerWallet.getSigningServiceType()).createCredential(holdersCredentialCreationConfig);
            cr.setJwt(signerJwtResult.getJwt());
        } else {
            cr.setVc(credential.getData());
        }

        log.debug("VC type of {} issued to bpn ->{}", StringEscapeUtils.escapeJava(verifiableCredential.getTypes().toString()), StringEscapeUtils.escapeJava(callerBpn));

        return cr;
    }
}
