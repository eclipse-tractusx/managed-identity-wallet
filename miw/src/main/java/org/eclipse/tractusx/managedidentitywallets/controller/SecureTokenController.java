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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.BadRequestException;
import org.eclipse.tractusx.managedidentitywallets.apidocs.SecureTokenControllerApiDoc;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.managedidentitywallets.domain.StsTokenErrorResponse;
import org.eclipse.tractusx.managedidentitywallets.domain.StsTokenResponse;
import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequestScope;
import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequestToken;
import org.eclipse.tractusx.managedidentitywallets.exception.InvalidIdpTokenResponseException;
import org.eclipse.tractusx.managedidentitywallets.exception.InvalidSecureTokenRequestException;
import org.eclipse.tractusx.managedidentitywallets.exception.UnknownBusinessPartnerNumberException;
import org.eclipse.tractusx.managedidentitywallets.exception.UnsupportedGrantTypeException;
import org.eclipse.tractusx.managedidentitywallets.signing.SigningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "STS")
public class SecureTokenController extends BaseController {

    public static final String BASE_PATH = "/api/token";


    private final WalletRepository walletRepo;

    private final Map<SigningServiceType, SigningService> availableSigningServices;

    @SneakyThrows
    @PostMapping(path = BASE_PATH, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    @SecureTokenControllerApiDoc.PostSecureTokenDocJson
    public ResponseEntity<StsTokenResponse> tokenJson(
            @RequestBody String data,
            Principal principal
    ) {
        var request = new ObjectMapper().readValue(data, SecureTokenRequest.class);
        return processRequest(request, principal);
    }

    private ResponseEntity<StsTokenResponse> processRequest(SecureTokenRequest secureTokenRequest,
                                                            Principal principal) throws ParseException, BadRequestException {

        var bpn = getBPNFromToken(principal);

        /* If Token is Present */
        if (secureTokenRequest.getSecureTokenRequestToken().isPresent()) {
            return processWithToken(secureTokenRequest.getSecureTokenRequestToken().get(), bpn);
            /* Else If Scope is Present */
        } else if (secureTokenRequest.getSecureTokenRequestScope().isPresent()) {
            return processWithScope(secureTokenRequest.getSecureTokenRequestScope().get(), bpn);
            /* Else Throw */
        } else {
            throw new BadRequestException("The provided data could not be used to create and sign a token.");
        }
    }


    private ResponseEntity<StsTokenResponse> processWithToken(SecureTokenRequestToken secureTokenRequest, String bpn) throws ParseException {
        Wallet selfWallet = walletRepo.getByBpn(bpn);
        DID selfDid = new DID(selfWallet.getDid());
        DID partnerDid;
        if (Pattern.compile(StringPool.BPN_NUMBER_REGEX).matcher(secureTokenRequest.getAudience()).matches()) {
            partnerDid = new DID(walletRepo.getByBpn(secureTokenRequest.getAudience()).getDid());
        } else if (StringUtils.startsWith(secureTokenRequest.getAudience(), "did:")) {
            partnerDid = new DID(secureTokenRequest.getAudience());
        } else {
            throw new InvalidSecureTokenRequestException("You must provide an audience either as a BPN or DID.");
        }

        SigningServiceType signingServiceType = selfWallet.getSigningServiceType();
        SigningService signingService = availableSigningServices.get(signingServiceType);

        // create the SI token and put/create the access_token inside
        JWT responseJwt;
        log.debug("Signing si token.");
        responseJwt = signingService.issueToken(
                selfDid,
                partnerDid,
                JWTParser.parse(secureTokenRequest.getToken())
        );

        // create the response
        log.debug("Preparing StsTokenResponse.");
        StsTokenResponse response = StsTokenResponse.builder()
                .token(responseJwt.serialize())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    private ResponseEntity<StsTokenResponse> processWithScope(SecureTokenRequestScope secureTokenRequest, String bpn) {
        Wallet selfWallet = walletRepo.getByBpn(bpn);
        DID selfDid = new DID(selfWallet.getDid());
        DID partnerDid;
        if (Pattern.compile(StringPool.BPN_NUMBER_REGEX).matcher(secureTokenRequest.getConsumerDid()).matches()) {
            partnerDid = new DID(walletRepo.getByBpn(secureTokenRequest.getConsumerDid()).getDid());
        } else if (StringUtils.startsWith(secureTokenRequest.getConsumerDid(), "did:")) {
            partnerDid = new DID(secureTokenRequest.getConsumerDid());
        } else {
            throw new InvalidSecureTokenRequestException("You must provide an audience either as a BPN or DID.");
        }

        SigningServiceType signingServiceType = selfWallet.getSigningServiceType();
        SigningService signingService = availableSigningServices.get(signingServiceType);

        // create the SI token and put/create the access_token inside
        JWT responseJwt;
        log.debug("Creating access token and signing si token.");

        var scope = secureTokenRequest.getScope();
        if (!scope.equalsIgnoreCase("read")) {
            throw new UnsupportedOperationException("Only read scope is supported.");
        }

        var scopes = secureTokenRequest.getCredentialTypes()
                .stream()
                // Why this strange scopes? Doesn't make sense, but done as defined here
                // https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/verifiable.presentation.protocol.md#3-security
                .map("hereCouldBeYourText:%s:read"::formatted)
                .collect(Collectors.toSet());

        responseJwt = signingService.issueToken(
                selfDid,
                partnerDid,
                scopes
        );

        // create the response
        log.debug("Preparing StsTokenResponse.");
        StsTokenResponse response = StsTokenResponse.builder()
                .token(responseJwt.serialize())
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
