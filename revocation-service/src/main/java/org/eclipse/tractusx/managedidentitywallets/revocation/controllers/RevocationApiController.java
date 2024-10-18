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

package org.eclipse.tractusx.managedidentitywallets.revocation.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.Validate;
import org.eclipse.tractusx.managedidentitywallets.revocation.apidocs.RevocationApiControllerApiDocs;
import org.eclipse.tractusx.managedidentitywallets.revocation.constant.RevocationApiEndpoints;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.CredentialStatusDto;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.StatusEntryDto;
import org.eclipse.tractusx.managedidentitywallets.revocation.exception.RevocationServiceException;
import org.eclipse.tractusx.managedidentitywallets.revocation.services.RevocationService;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * The RevocationApiController class is a REST controller that handles revocation-related API
 * endpoints.
 */
@RestController
@RequestMapping(RevocationApiEndpoints.REVOCATION_API)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Revocation Service", description = "Revocation Service API")
public class RevocationApiController extends BaseController {

    private final RevocationService revocationService;

    /**
     * The above function is a Java POST endpoint that creates a status list for a credential using
     * the provided DTO.
     *
     * @param dto   The parameter "dto" is of type "StatusEntryDto" and is annotated with
     *              "@RequestBody". It is used to receive the request body data from the client. The "@Valid"
     *              annotation is used to perform validation on the "dto" object based on the validation
     *              constraints defined in the "StatusEntry
     * @param token The authentication token
     * @return The method is returning a CredentialStatusDto object.
     */
    @RevocationApiControllerApiDocs.StatusEntryApiDocs
    @PostMapping(
            value = RevocationApiEndpoints.STATUS_ENTRY,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CredentialStatusDto createStatusListVC(
            @Valid @RequestBody StatusEntryDto dto,
            @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token,
            Principal principal) {
        Validate.isFalse(
                        getBPNFromToken(principal).equals(revocationService.extractBpnFromDid(dto.issuerId())))
                .launch(new ForbiddenException("invalid caller"));
        return revocationService.createStatusList(dto, token);
    }

    /**
     * The above function is a Java POST endpoint that revokes a credential and returns an HTTP status
     * code.
     *
     * @param dto   The `dto` parameter is of type `CredentialStatusDto` and is annotated with
     *              `@RequestBody`. This means that it is expected to be the request body of the HTTP POST
     *              request. The `@Valid` annotation indicates that the `dto` object should be validated before
     *              being processed further.
     * @param token The authentication token
     * @return The method is returning a ResponseEntity object with a HttpStatus of OK.
     */
    @RevocationApiControllerApiDocs.revokeCredentialDocs
    @PostMapping(RevocationApiEndpoints.REVOKE)
    public ResponseEntity<HttpStatus> revokeCredential(
            @Valid @RequestBody CredentialStatusDto dto,
            @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token,
            Principal principal)
            throws RevocationServiceException {
        Validate.isFalse(
                        getBPNFromToken(principal).equals(revocationService.extractBpnFromURL(dto.id())))
                .launch(new ForbiddenException("Invalid caller"));
        revocationService.revoke(dto, token);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RevocationApiControllerApiDocs.verifyCredentialDocs
    @PostMapping(RevocationApiEndpoints.VERIFY)
    public ResponseEntity<Map<String, String>> verifyRevocation(
            @Valid @RequestBody CredentialStatusDto dto,
            @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token,
            Principal principal) {
        Validate.isFalse(
                        getBPNFromToken(principal).equals(revocationService.extractBpnFromURL(dto.id())))
                .launch(new ForbiddenException("Invalid caller"));


        return ResponseEntity.ofNullable(revocationService.verifyStatus(dto));
    }

    /**
     * The function `getCredentialsByIssuerId` retrieves a list of credentials by their issuer ID.
     *
     * @param issuerBPN The `issuerBPN` parameter is a string that represents the BPn of the
     *                 issuer.
     * @return The method is returning a ResponseEntity object that wraps a VerifiableCredential
     * object.
     */
    @RevocationApiControllerApiDocs.GetStatusListCredentialDocs
    @GetMapping(
            path = RevocationApiEndpoints.CREDENTIALS_STATUS_INDEX,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> getStatusListCredential(
            @RevocationApiControllerApiDocs.IssuerBPNPathParamDoc @PathVariable(name = "issuerBPN") String issuerBPN,
            @RevocationApiControllerApiDocs.StatusPathParamDoc @PathVariable(name = "status") String status,
            @RevocationApiControllerApiDocs.IndexPathParamDoc @PathVariable(name = "index") String index) {
        log.debug("received get list for {}", issuerBPN);
        return ResponseEntity.ofNullable(
                revocationService.getStatusListCredential(
                        issuerBPN.toUpperCase(), status.toLowerCase(), index));
    }
}
