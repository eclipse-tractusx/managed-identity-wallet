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

package org.eclipse.tractusx.managedidentitywallets.controller;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.managedidentitywallets.apidocs.SecureTokenControllerApiDoc;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.BusinessPartnerNumber;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.IdpTokenResponse;
import org.eclipse.tractusx.managedidentitywallets.domain.StsTokenErrorResponse;
import org.eclipse.tractusx.managedidentitywallets.domain.StsTokenResponse;
import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.InvalidIdpTokenResponseException;
import org.eclipse.tractusx.managedidentitywallets.exception.InvalidSecureTokenRequestException;
import org.eclipse.tractusx.managedidentitywallets.exception.UnknownBusinessPartnerNumberException;
import org.eclipse.tractusx.managedidentitywallets.exception.UnsupportedGrantTypeException;
import org.eclipse.tractusx.managedidentitywallets.interfaces.SecureTokenService;
import org.eclipse.tractusx.managedidentitywallets.service.IdpAuthorization;
import org.eclipse.tractusx.managedidentitywallets.validator.SecureTokenRequestValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.Set;
import java.util.regex.Pattern;

import static org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils.getSecureTokenRequest;


@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "STS")
public class SecureTokenController {

    private final SecureTokenService tokenService;

    private final IdpAuthorization idpAuthorization;

    private final WalletRepository walletRepo;

    @InitBinder
    void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(new SecureTokenRequestValidator());
    }

    @SneakyThrows
    @PostMapping(path = "/api/token", consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    @SecureTokenControllerApiDoc.PostSecureTokenDoc
    public ResponseEntity<StsTokenResponse> token(
            @Valid @RequestBody SecureTokenRequest secureTokenRequest
    ) {
        return processTokenRequest(secureTokenRequest);
    }

    @SneakyThrows
    @PostMapping(path = "/api/token", consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    @SecureTokenControllerApiDoc.PostSecureTokenDoc
    public ResponseEntity<StsTokenResponse> token(
            @Valid @RequestBody MultiValueMap<String, String> requestParameters
    ) {
        final SecureTokenRequest secureTokenRequest = getSecureTokenRequest(requestParameters);
        return processTokenRequest(secureTokenRequest);
    }

    private ResponseEntity<StsTokenResponse> processTokenRequest(SecureTokenRequest secureTokenRequest) throws ParseException {
        // handle idp authorization
        IdpTokenResponse idpResponse = idpAuthorization.fromSecureTokenRequest(secureTokenRequest);
        BusinessPartnerNumber bpn = idpResponse.bpn();
        DID selfDid = new DID(walletRepo.getByBpn(bpn.toString()).getDid());
        DID partnerDid;
        if (Pattern.compile(StringPool.BPN_NUMBER_REGEX).matcher(secureTokenRequest.getAudience()).matches()) {
            partnerDid = new DID(walletRepo.getByBpn(secureTokenRequest.getAudience()).getDid());
        } else if (StringUtils.startsWith(secureTokenRequest.getAudience(), "did:")) {
            partnerDid = new DID(secureTokenRequest.getAudience());
        } else {
            throw new InvalidSecureTokenRequestException("You must provide an audience either as a BPN or DID.");
        }

        // create the SI token and put/create the access_token inside
        JWT responseJwt;
        if (secureTokenRequest.assertValidWithAccessToken()) {
            log.debug("Signing si token.");
            responseJwt = tokenService.issueToken(
                    selfDid,
                    partnerDid,
                    JWTParser.parse(secureTokenRequest.getAccessToken())
            );
        } else if (secureTokenRequest.assertValidWithScopes()) {
            log.debug("Creating access token and signing si token.");
            responseJwt = tokenService.issueToken(
                    selfDid,
                    partnerDid,
                    Set.of(secureTokenRequest.getBearerAccessScope())
            );
        } else {
            throw new InvalidSecureTokenRequestException("The provided data could not be used to create and sign a token.");
        }

        // create the response
        log.debug("Preparing StsTokenResponse.");
        StsTokenResponse response = StsTokenResponse.builder()
                .token(responseJwt.serialize())
                .expiresAt(responseJwt.getJWTClaimsSet().getExpirationTime().getTime())
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @ExceptionHandler({ UnsupportedGrantTypeException.class, InvalidSecureTokenRequestException.class, UnknownBusinessPartnerNumberException.class, InvalidIdpTokenResponseException.class })
    public ResponseEntity<StsTokenErrorResponse> getErrorResponse(RuntimeException e) {
        StsTokenErrorResponse response = new StsTokenErrorResponse();
        response.setError(e.getClass().getSimpleName());
        response.setErrorDescription(e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
