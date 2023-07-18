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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueDismantlerCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.service.IssuersCredentialService;
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
 * The type Issuers credential controller.
 */
@RestController
@RequiredArgsConstructor
public class IssuersCredentialController extends BaseController {

    /**
     * The constant API_TAG_VERIFIABLE_CREDENTIAL_ISSUER.
     */
    public static final String API_TAG_VERIFIABLE_CREDENTIAL_ISSUER = "Verifiable Credential - Issuer";
    /**
     * The constant API_TAG_VERIFIABLE_CREDENTIAL_VALIDATION.
     */
    public static final String API_TAG_VERIFIABLE_CREDENTIAL_VALIDATION = "Verifiable Credential - Validation";

    private final IssuersCredentialService issuersCredentialService;


    /**
     * Gets credentials.
     *
     * @param credentialId     the credential id
     * @param holderIdentifier the holder identifier
     * @param type             the type
     * @param pageNumber       the page number
     * @param size             the size
     * @param sortColumn       the sort column
     * @param sortTpe          the sort tpe
     * @param principal        the principal
     * @return the credentials
     */
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_ISSUER)
    @Operation(description = "Permission: **view_wallets** (The BPN of holderIdentifier must equal BPN of caller)\n\n Search verifiable credentials with filter criteria", summary = "Query Verifiable Credentials")
    @GetMapping(path = RestURI.ISSUERS_CREDENTIALS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageImpl<VerifiableCredential>> getCredentials(@RequestParam(required = false) String credentialId,
                                                                         @RequestParam(required = false) String holderIdentifier,
                                                                         @RequestParam(required = false) List<String> type,
                                                                         @Min(0) @Max(Integer.MAX_VALUE) @Parameter(description = "Page number, Page number start with zero") @RequestParam(required = false, defaultValue = "0") int pageNumber,
                                                                         @Min(0) @Max(Integer.MAX_VALUE) @Parameter(description = "Number of records per page") @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") int size,
                                                                         @RequestParam(required = false, defaultValue = "createdAt") String sortColumn,
                                                                         @RequestParam(required = false, defaultValue = "desc") String sortTpe, Principal principal) {
        return ResponseEntity.status(HttpStatus.OK).body(issuersCredentialService.getCredentials(credentialId, holderIdentifier, type, sortColumn, sortTpe, pageNumber, size, getBPNFromToken(principal)));
    }

    /**
     * Issue membership credential response entity.
     *
     * @param issueMembershipCredentialRequest the issue membership credential request
     * @param principal                        the principal
     * @return the response entity
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                   "bpn": "BPNL000000000000"
                                 }
                    """))
    })
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_ISSUER)
    @Operation(summary = "Issue a Membership Verifiable Credential with base wallet issuer", description = "Permission: **update_wallets** (The BPN of base wallet must equal BPN of caller)\n\n Issue a verifiable credential by base wallet")
    @PostMapping(path = RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueMembershipCredential(@Valid @RequestBody IssueMembershipCredentialRequest issueMembershipCredentialRequest, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issuersCredentialService.issueMembershipCredential(issueMembershipCredentialRequest, getBPNFromToken(principal)));
    }

    /**
     * Issue dismantler credential response entity.
     *
     * @param request   the request
     * @param principal the principal
     * @return the response entity
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                   "bpn": "BPNL000000000000",
                                   "activityType": "vehicleDismantle",
                                   "allowedVehicleBrands": [
                                     "Audi", "Abarth", "Alfa Romeo", "Chrysler"
                                   ]
                                 }
                    """))
    })
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_ISSUER)
    @Operation(summary = "Issue a Dismantler Verifiable Credential with base wallet issuer", description = "Permission: **update_wallets** (The BPN of base wallet must equal BPN of caller)\n\n Issue a verifiable credential by base wallet")
    @PostMapping(path = RestURI.CREDENTIALS_ISSUER_DISMANTLER, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueDismantlerCredential(@Valid @RequestBody IssueDismantlerCredentialRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issuersCredentialService.issueDismantlerCredential(request, getBPNFromToken(principal)));
    }

    /**
     * Issue framework credential response entity.
     *
     * @param request   the request
     * @param principal the principal
     * @return the response entity
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                  "holderIdentifier": "BPNL000000000000",
                                  "type": "BehaviorTwinCredential",
                                  "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                  "contract-version": "1.0.0"
                                }
                    """))
    })
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_ISSUER)
    @Operation(summary = "Issue a Use Case Verifiable Credential with base wallet issuer", description = "Permission: **update_wallets** (The BPN of base wallet must equal BPN of caller)\n\n Issue a verifiable credential by base wallet")
    @PostMapping(path = RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueFrameworkCredential(@Valid @RequestBody IssueFrameworkCredentialRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issuersCredentialService.issueFrameworkCredential(request, getBPNFromToken(principal)));
    }

    /**
     * Credentials validation response entity.
     *
     * @param data                     the data
     * @param withCredentialExpiryDate the with credential expiry date
     * @return the response entity
     */
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_VALIDATION)
    @Operation(summary = "Validate Verifiable Credentials", description = "Permission: **view_wallets** OR **view_wallet** \n\n Validate Verifiable Credentials")
    @PostMapping(path = RestURI.CREDENTIALS_VALIDATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                     "credentialSubject":
                                     [
                                         {
                                             "bpn": "BPNL000000000000",
                                             "id": "did:web:localhost:BPNL000000000000",
                                             "type": "BpnCredential"
                                         }
                                     ],
                                     "issuanceDate": "2023-07-14T11:05:44Z",
                                     "id": "did:web:localhost:BPNL000000000000#f177b3e9-bbf9-45db-bc3d-80152abcb419",
                                     "proof":
                                     {
                                         "created": "2023-07-14T11:05:48Z",
                                         "jws": "eyJhbGciOiJFZERTQSJ9..C4oYBfTh11OKG0yV0qoCQxF6zZWZLb9dPXJCP6oCtpyB_sSc8o6cPhByKwf-0o7ElsUr0mh6AGPwGxdoOijfDw",
                                         "proofPurpose": "proofPurpose",
                                         "type": "JsonWebSignature2020",
                                         "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                     },
                                     "type":
                                     [
                                         "VerifiableCredential",
                                         "BpnCredential"
                                     ],
                                     "@context":
                                     [
                                         "https://www.w3.org/2018/credentials/v1",
                                         "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                         "https://w3id.org/security/suites/jws-2020/v1"
                                     ],
                                     "issuer": "did:web:localhost:BPNL000000000000",
                                     "expirationDate": "2023-09-30T18:30:00Z"
                                 }
                    """))
    })
    public ResponseEntity<Map<String, Object>> credentialsValidation(@RequestBody Map<String, Object> data,
                                                                     @Parameter(description = "Check expiry of VC") @RequestParam(name = "withCredentialExpiryDate", defaultValue = "false", required = false) boolean withCredentialExpiryDate) {
        return ResponseEntity.status(HttpStatus.OK).body(issuersCredentialService.credentialsValidation(data, withCredentialExpiryDate));
    }

    /**
     * Issue credential response entity.
     *
     * @param holderDid the holder did
     * @param data      the data
     * @param principal the principal
     * @return the response entity
     */
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_ISSUER)
    @Operation(summary = "Issue Verifiable Credential", description = "Permission: **update_wallets** (The BPN of the base wallet must equal BPN of caller)\nIssue a verifiable credential with a given issuer DID")
    @PostMapping(path = RestURI.ISSUERS_CREDENTIALS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                      "id": "http://example.edu/credentials/333",
                                      "@context": [
                                        "https://www.w3.org/2018/credentials/v1",
                                        "https://www.w3.org/2018/credentials/examples/v1"
                                      ],
                                      "type": [
                                        "VerifiableCredential", "University-Degree-Credential"
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
    public ResponseEntity<VerifiableCredential> issueCredentialUsingBaseWallet(@RequestParam String holderDid, @RequestBody Map<String, Object> data, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issuersCredentialService.issueCredentialUsingBaseWallet(holderDid, data, getBPNFromToken(principal)));
    }
}
