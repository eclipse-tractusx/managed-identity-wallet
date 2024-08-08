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


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.apidocs.HoldersCredentialControllerApiDocs.GetCredentialsApiDocs;
import org.eclipse.tractusx.managedidentitywallets.apidocs.HoldersCredentialControllerApiDocs.IssueCredentialApiDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.IssuersCredentialControllerApiDocs.AsJwtParam;
import org.eclipse.tractusx.managedidentitywallets.apidocs.RevocationAPIDoc;
import org.eclipse.tractusx.managedidentitywallets.command.GetCredentialsCommand;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.TokenParsingUtils;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialsResponse;
import org.eclipse.tractusx.managedidentitywallets.service.HoldersCredentialService;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The type Holders credential controller.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Verifiable Credential - Holder")
public class HoldersCredentialController {

    private final HoldersCredentialService holdersCredentialService;


    /**
     * Gets credentials.
     *
     * @param credentialId     the credential id
     * @param issuerIdentifier the issuer identifier
     * @param type             the type
     * @param sortColumn       the sort column
     * @param sortTpe          the sort tpe
     * @param authentication   the authentication
     * @return the credentials
     */
    @GetCredentialsApiDocs
    @GetMapping(path = RestURI.CREDENTIALS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageImpl<CredentialsResponse>> getCredentials(@Parameter(name = "credentialId", description = "Credential Id", examples = { @ExampleObject(name = "Credential Id", value = "did:web:localhost:BPNL000000000000#12528899-160a-48bd-ba15-f396c3959ae9") }) @RequestParam(required = false) String credentialId,
                                                                        @Parameter(name = "issuerIdentifier", description = "Issuer identifier(did of BPN)", examples = { @ExampleObject(name = "bpn", value = "BPNL000000000000", description = "bpn"), @ExampleObject(description = "did", name = "did", value = "did:web:localhost:BPNL000000000000") }) @RequestParam(required = false) String issuerIdentifier,
                                                                        @Parameter(name = "type", description = "Type of VC", examples = { @ExampleObject(name = "SummaryCredential", value = "SummaryCredential", description = "SummaryCredential"), @ExampleObject(description = "BpnCredential", name = "BpnCredential", value = "BpnCredential") }) @RequestParam(required = false) List<String> type,
                                                                        @Parameter(name = "sortColumn", description = "Sort column name",
                                                                                examples = {
                                                                                        @ExampleObject(value = "createdAt", name = "creation date"),
                                                                                        @ExampleObject(value = "issuerDid", name = "Issuer did"),
                                                                                        @ExampleObject(value = "type", name = "Credential type"),
                                                                                        @ExampleObject(value = "credentialId", name = "Credential id"),
                                                                                        @ExampleObject(value = "selfIssued", name = "Self issued credential"),
                                                                                        @ExampleObject(value = "stored", name = "Stored credential")
                                                                                }
                                                                        ) @RequestParam(required = false, defaultValue = "createdAt") String sortColumn,
                                                                        @Parameter(name = "sortTpe", description = "Sort order", examples = { @ExampleObject(value = "desc", name = "Descending order"), @ExampleObject(value = "asc", name = "Ascending order") }) @RequestParam(required = false, defaultValue = "desc") String sortTpe,
                                                                        @Min(0) @Max(Integer.MAX_VALUE) @Parameter(description = "Page number, Page number start with zero") @RequestParam(required = false, defaultValue = "0") int pageNumber,
                                                                        @Min(0) @Max(Integer.MAX_VALUE) @Parameter(description = "Number of records per page") @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") int size,
                                                                        @AsJwtParam @RequestParam(name = StringPool.AS_JWT, defaultValue = "false") boolean asJwt,

                                                                        Authentication authentication) {
        log.debug("Received request to get credentials. BPN: {}", TokenParsingUtils.getBPNFromToken(authentication));
        final GetCredentialsCommand command;
        command = GetCredentialsCommand.builder()
                .credentialId(credentialId)
                .identifier(issuerIdentifier)
                .type(type)
                .sortColumn(sortColumn)
                .sortType(sortTpe)
                .pageNumber(pageNumber)
                .size(size)
                .asJwt(asJwt)
                .callerBPN(TokenParsingUtils.getBPNFromToken(authentication))
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(holdersCredentialService.getCredentials(command));
    }


    /**
     * Issue credential response entity.
     *
     * @param data           the data
     * @param authentication the authentication
     * @return the response entity
     */

    @IssueCredentialApiDoc
    @PostMapping(path = RestURI.CREDENTIALS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity<CredentialsResponse> issueCredential(@RequestBody Map<String, Object> data, Authentication authentication,
                                                               @AsJwtParam @RequestParam(name = "asJwt", defaultValue = "false") boolean asJwt,
                                                               @RevocationAPIDoc.Revocable @RequestParam(name = StringPool.REVOCABLE, defaultValue = "true") boolean revocable,
                                                               @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token
    ) {
        log.debug("Received request to issue credential. BPN: {}", TokenParsingUtils.getBPNFromToken(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(holdersCredentialService.issueCredential(data, TokenParsingUtils.getBPNFromToken(authentication), asJwt, revocable, token));
    }
}
