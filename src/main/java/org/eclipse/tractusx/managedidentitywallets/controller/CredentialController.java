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

package org.eclipse.tractusx.managedidentitywallets.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueDismantlerCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.service.CredentialService;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The type Credential controller.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "VerifiableCredentials")
public class CredentialController {

    private final CredentialService service;

    /**
     * Gets credentials.
     *
     * @param holderIdentifier the holder identifier
     * @param id               the id
     * @param issuerIdentifier the issuer identifier
     * @param type             the type
     * @return the credentials
     */
    @Operation(description = "Permission: **view_wallets** OR **view_wallet** (The BPN of holderIdentifier must equal BPN of caller)\n\n Search verifiable credentials with filter criteria", summary = "Query Verifiable Credentials")
    @GetMapping(path = RestURI.CREDENTIALS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Credential>> getCredentials(@RequestParam(required = false) String holderIdentifier, @RequestParam(required = false) String id, @RequestParam(required = false) String issuerIdentifier, @RequestParam(required = false) List<String> type) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getCredentials(holderIdentifier, id, issuerIdentifier, type));
    }

    /**
     * Issue membership credential response entity.
     *
     * @param issueMembershipCredentialRequest the issue membership credential request
     * @return the response entity
     */
    @Operation(summary = "Issue a Membership Verifiable Credential with base wallet issuer", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of base wallet must equal BPN of caller)\n\n Issue a verifiable credential by base wallet")
    @PostMapping(path = RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueMembershipCredential(@Valid @RequestBody IssueMembershipCredentialRequest issueMembershipCredentialRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.issueMembershipCredential(issueMembershipCredentialRequest));
    }

    /**
     * Issue dismantler credential response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @Operation(summary = "Issue a Dismantler Verifiable Credential with base wallet issuer", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of base wallet must equal BPN of caller)\n\n Issue a verifiable credential by base wallet")
    @PostMapping(path = RestURI.CREDENTIALS_ISSUER_DISMANTLER, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueDismantlerCredential(@Valid @RequestBody IssueDismantlerCredentialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.issueDismantlerCredential(request));
    }

    /**
     * Issue framework credential response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @Operation(summary = "Issue a Use Case Verifiable Credential with base wallet issuer", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of base wallet must equal BPN of caller)\n\n Issue a verifiable credential by base wallet")
    @PostMapping(path = RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueFrameworkCredential(@Valid @RequestBody IssueFrameworkCredentialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.issueFrameworkCredential(request));
    }
}
