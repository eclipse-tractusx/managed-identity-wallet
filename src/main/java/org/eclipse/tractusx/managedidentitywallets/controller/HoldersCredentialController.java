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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.service.HoldersCredentialService;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * The type Holders credential controller.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Verifiable Credential - Holder")
public class HoldersCredentialController extends BaseController {

    private final HoldersCredentialService holdersCredentialService;


    /**
     * Gets credentials.
     *
     * @param credentialId     the credential id
     * @param issuerIdentifier the issuer identifier
     * @param type             the type
     * @param sortColumn       the sort column
     * @param sortTpe          the sort tpe
     * @param principal        the principal
     * @return the credentials
     */
    @Operation(description = "Permission: **view_wallets** OR **view_wallet** (The BPN of holderIdentifier must equal BPN of caller)\n\n Search verifiable credentials with filter criteria", summary = "Query Verifiable Credentials")
    @GetMapping(path = RestURI.CREDENTIALS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageImpl<VerifiableCredential>> getCredentials(@RequestParam(required = false) String credentialId,
                                                                         @RequestParam(required = false) String issuerIdentifier,
                                                                         @RequestParam(required = false) List<String> type,
                                                                         @RequestParam(required = false, defaultValue = "createdAt") String sortColumn,
                                                                         @RequestParam(required = false, defaultValue = "desc") String sortTpe,
                                                                         @Min(0) @Max(Integer.MAX_VALUE) @Parameter(description = "Page number, Page number start with zero") @RequestParam(required = false, defaultValue = "0") int pageNumber,
                                                                         @Min(0) @Max(Integer.MAX_VALUE) @Parameter(description = "Number of records per page") @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") int size,
                                                                         Principal principal) {
        return ResponseEntity.status(HttpStatus.OK).body(holdersCredentialService.getCredentials(credentialId, issuerIdentifier, type, sortColumn, sortTpe, pageNumber, size, getBPNFromToken(principal)));
    }


    /**
     * Issue credential response entity.
     *
     * @param data      the data
     * @param principal the principal
     * @return the response entity
     */
    @Operation(summary = "Issue Verifiable Credential", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of the issuer of the Verifiable Credential must equal BPN of caller)\nIssue a verifiable credential with a given issuer DID")
    @PostMapping(path = RestURI.CREDENTIALS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                      "id": "http://example.edu/credentials/333",
                                      "@context": [
                                        "https://www.w3.org/2018/credentials/v1",
                                        "https://www.w3.org/2018/credentials/examples/v1"
                                      ],
                                      "type": [
                                        "University-Degree-Credential","VerifiableCredential"
                                      ],
                                      "issuer": "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                      "issuanceDate": "2019-06-16T18:56:59Z",
                                      "expirationDate": "2019-06-17T18:56:59Z",
                                      "credentialSubject": [{
                                        "college": "Test-University"
                                      }]
                                }
                    """))
    })
    public ResponseEntity<VerifiableCredential> issueCredential(@RequestBody Map<String, Object> data, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holdersCredentialService.issueCredential(data, getBPNFromToken(principal)));
    }

    /**
     * Delete credential response entity.
     *
     * @param credentialId the credential id
     * @param principal    the principal
     * @return the response entity
     */
    @Operation(description = "Permission: **update_wallet** (The BPN of holderIdentifier must equal BPN of caller)\n\n Delete a verifiable credential by its ID", summary = "Delete a verifiable credential by its ID")
    @DeleteMapping(path = RestURI.CREDENTIALS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deleteCredential(@RequestParam(name = "id") String credentialId, Principal principal) {
        holdersCredentialService.deleteCredential(credentialId, getBPNFromToken(principal));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
