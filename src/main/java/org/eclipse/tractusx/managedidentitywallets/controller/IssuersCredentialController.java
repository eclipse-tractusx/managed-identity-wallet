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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.tractusx.managedidentitywallets.apidocs.IssuersCredentialControllerApiDocs.GetCredentialsApiDocs;
import org.eclipse.tractusx.managedidentitywallets.apidocs.IssuersCredentialControllerApiDocs.IssueDismantlerCredentialApiDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.IssuersCredentialControllerApiDocs.IssueMembershipCredentialApiDoc;
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
@Slf4j
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
    @GetCredentialsApiDocs
    @GetMapping(path = RestURI.ISSUERS_CREDENTIALS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageImpl<VerifiableCredential>> getCredentials(@Parameter(name = "credentialId", description = "Credential Id", examples = {@ExampleObject(name = "Credential Id", value = "did:web:localhost:BPNL000000000000#12528899-160a-48bd-ba15-f396c3959ae9")}) @RequestParam(required = false) String credentialId,
                                                                         @Parameter(name = "holderIdentifier", description = "Holder identifier(did of BPN)", examples = {@ExampleObject(name = "bpn", value = "BPNL000000000001", description = "bpn"), @ExampleObject(description = "did", name = "did", value = "did:web:localhost:BPNL000000000001")}) @RequestParam(required = false) String holderIdentifier,
                                                                         @Parameter(name = "type", description = "Type of VC", examples = {@ExampleObject(name = "SummaryCredential", value = "SummaryCredential", description = "SummaryCredential"), @ExampleObject(description = "BpnCredential", name = "BpnCredential", value = "BpnCredential")}) @RequestParam(required = false) List<String> type,
                                                                         @Min(0) @Max(Integer.MAX_VALUE) @Parameter(description = "Page number, Page number start with zero") @RequestParam(required = false, defaultValue = "0") int pageNumber,
                                                                         @Min(0) @Max(Integer.MAX_VALUE) @Parameter(description = "Number of records per page") @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") int size,
                                                                         @Parameter(name = "sortColumn", description = "Sort column name",
                                                                                 examples = {
                                                                                         @ExampleObject(value = "createdAt", name = "creation date"),
                                                                                         @ExampleObject(value = "holderDid", name = "Holder did"),
                                                                                         @ExampleObject(value = "type", name = "Credential type"),
                                                                                         @ExampleObject(value = "credentialId", name = "Credential id")
                                                                                 }
                                                                         ) @RequestParam(required = false, defaultValue = "createdAt") String sortColumn,
                                                                         @Parameter(name = "sortTpe", description = "Sort order", examples = {@ExampleObject(value = "desc", name = "Descending order"), @ExampleObject(value = "asc", name = "Ascending order")}) @RequestParam(required = false, defaultValue = "desc") String sortTpe, Principal principal) {
        log.debug("Received request to get credentials. BPN: {}", getBPNFromToken(principal));
        return ResponseEntity.status(HttpStatus.OK).body(issuersCredentialService.getCredentials(credentialId, holderIdentifier, type, sortColumn, sortTpe, pageNumber, size, getBPNFromToken(principal)));
    }

    /**
     * Issue membership credential response entity.
     *
     * @param issueMembershipCredentialRequest the issue membership credential request
     * @param principal                        the principal
     * @return the response entity
     */
    @IssueMembershipCredentialApiDoc
    @PostMapping(path = RestURI.CREDENTIALS_ISSUER_MEMBERSHIP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueMembershipCredential(@Valid @RequestBody IssueMembershipCredentialRequest issueMembershipCredentialRequest, Principal principal) {
        log.debug("Received request to issue membership credential. BPN: {}", getBPNFromToken(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(issuersCredentialService.issueMembershipCredential(issueMembershipCredentialRequest, getBPNFromToken(principal)));
    }

    /**
     * Issue dismantler credential response entity.
     *
     * @param request   the request
     * @param principal the principal
     * @return the response entity
     */
    @IssueDismantlerCredentialApiDoc
    @PostMapping(path = RestURI.CREDENTIALS_ISSUER_DISMANTLER, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueDismantlerCredential(@Valid @RequestBody IssueDismantlerCredentialRequest request, Principal principal) {
        log.debug("Received request to issue dismantler credential. BPN: {}", getBPNFromToken(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(issuersCredentialService.issueDismantlerCredential(request, getBPNFromToken(principal)));
    }

    /**
     * Issue framework credential response entity.
     *
     * @param request   the request
     * @param principal the principal
     * @return the response entity
     */

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = {
                    @Content(examples = {
                            @ExampleObject(name = "BehaviorTwinCredential", value = """
                                                                    {
                                                                      "holderIdentifier": "BPNL000000000000",
                                                                      "type": "BehaviorTwinCredential",
                                                                      "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                                                      "contract-version": "1.0.0"
                                                                    }
                                    """),
                            @ExampleObject(name = "PcfCredential", value = """
                                                                    {
                                                                      "holderIdentifier": "BPNL000000000000",
                                                                      "type": "PcfCredential",
                                                                      "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                                                      "contract-version": "1.0.0"
                                                                    }
                                    """),
                            @ExampleObject(name = "SustainabilityCredential", value = """
                                                                    {
                                                                      "holderIdentifier": "BPNL000000000000",
                                                                      "type": "SustainabilityCredential",
                                                                      "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                                                      "contract-version": "1.0.0"
                                                                    }
                                    """),
                            @ExampleObject(name = "QualityCredential", value = """
                                                                    {
                                                                      "holderIdentifier": "BPNL000000000000",
                                                                      "type": "QualityCredential",
                                                                      "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                                                      "contract-version": "1.0.0"
                                                                    }
                                    """),
                            @ExampleObject(name = "TraceabilityCredential", value = """
                                                                    {
                                                                      "holderIdentifier": "BPNL000000000000",
                                                                      "type": "TraceabilityCredential",
                                                                      "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                                                      "contract-version": "1.0.0"
                                                                    }
                                    """),
                            @ExampleObject(name = "BehaviorTwinCredential", value = """
                                                                    {
                                                                      "holderIdentifier": "BPNL000000000000",
                                                                      "type": "BehaviorTwinCredential",
                                                                      "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                                                      "contract-version": "1.0.0"
                                                                    }
                                    """),
                            @ExampleObject(name = "ResiliencyCredential", value = """
                                                                    {
                                                                      "holderIdentifier": "BPNL000000000000",
                                                                      "type": "ResiliencyCredential",
                                                                      "contract-template": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                                                      "contract-version": "1.0.0"
                                                                    }
                                    """)

                    })
            }
    )
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_ISSUER)
    @Operation(summary = "Issue a Use Case Verifiable Credential with base wallet issuer", description = "Permission: **update_wallets** (The BPN of base wallet must equal BPN of caller)\n\n Issue a verifiable credential by base wallet")
    @ApiResponse(responseCode = "401", description = "The request could not be completed due to a failed authorization.", content = {@Content(examples = {})})
    @ApiResponse(responseCode = "403", description = "The request could not be completed due to a forbidden access", content = {@Content(examples = {})})
    @ApiResponse(responseCode = "500", description = "Any other internal server error", content = {@Content(examples = {
            @ExampleObject(name = "Internal server error", value = """
                    {
                      "type": "about:blank",
                      "title": "Error Title",
                      "status": 500,
                      "detail": "Error Details",
                      "instance": "API endpoint",
                      "properties": {
                        "timestamp": 1689762476720
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "404", description = "Wallet not found with provided identifier", content = {@Content(examples = {
            @ExampleObject(name = "Wallet not found with provided identifier", value = """
                    {
                      "type": "about:blank",
                      "title": "Error Title",
                      "status": 404,
                      "detail": "Error Details",
                      "instance": "API endpoint",
                      "properties": {
                        "timestamp": 1689762476720
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "400", description = "The input does not comply to the syntax requirements", content = {
            @Content(examples = {
                    @ExampleObject(name = "Response in case of invalid data provided", value = """
                             {
                                 "type": "about:blank",
                                 "title": "Invalid data provided",
                                 "status": 400,
                                 "detail": "details",
                                 "instance": "API endpoint",
                                 "properties":
                                 {
                                     "timestamp": 1689760833962,
                                     "errors":
                                     {
                                         "filed": "filed error message"
                                     }
                                 }
                             }
                            """)
            })
    })
    @ApiResponse(responseCode = "201", description = "Framework credential", content = {
            @Content(examples = {
                    @ExampleObject(name = "BehaviorTwin credential", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/2018/credentials/v1",
                                 "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                 "https://w3id.org/security/suites/jws-2020/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000#46a8c5e6-b195-4ec9-85cd-665c57d296ab",
                               "type": [
                                 "VerifiableCredential",
                                 "UseCaseFrameworkCondition"
                               ],
                               "issuer": "did:web:localhost:BPNL000000000000",
                               "issuanceDate": "2023-07-19T13:49:58Z",
                               "expirationDate": "2024-12-31T18:30:00Z",
                               "credentialSubject": [
                                 {
                                   "holderIdentifier": "BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "type": "BehaviorTwinCredential",
                                   "contractTemplate": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                   "contractVersion": "1.0.0"
                                 }
                               ],
                               "proof": {
                                 "proofPurpose": "proofPurpose",
                                 "type": "JsonWebSignature2020",
                                 "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                 "created": "2023-07-19T13:50:02Z",
                                 "jws": "eyJhbGciOiJFZERTQSJ9..IkfgC6Gn9sOT1uu1zMiDIIqw6pV4Z8axkKvphegsCVWT9uo0HZp4J9L1ILxR-huINGR5QlGIKiVuLGB5kKDOAQ"
                               }
                             }
                            """),
                    @ExampleObject(name = "Pcf Credential", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/2018/credentials/v1",
                                 "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                 "https://w3id.org/security/suites/jws-2020/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000#46a8c5e6-b195-4ec9-85cd-665c57d296ab",
                               "type": [
                                 "VerifiableCredential",
                                 "UseCaseFrameworkCondition"
                               ],
                               "issuer": "did:web:localhost:BPNL000000000000",
                               "issuanceDate": "2023-07-19T13:49:58Z",
                               "expirationDate": "2024-12-31T18:30:00Z",
                               "credentialSubject": [
                                 {
                                   "holderIdentifier": "BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "type": "PcfCredential",
                                   "contractTemplate": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                   "contractVersion": "1.0.0"
                                 }
                               ],
                               "proof": {
                                 "proofPurpose": "proofPurpose",
                                 "type": "JsonWebSignature2020",
                                 "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                 "created": "2023-07-19T13:50:02Z",
                                 "jws": "eyJhbGciOiJFZERTQSJ9..IkfgC6Gn9sOT1uu1zMiDIIqw6pV4Z8axkKvphegsCVWT9uo0HZp4J9L1ILxR-huINGR5QlGIKiVuLGB5kKDOAQ"
                               }
                             }
                            """),
                    @ExampleObject(name = "Sustainability Credential", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/2018/credentials/v1",
                                 "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                 "https://w3id.org/security/suites/jws-2020/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000#46a8c5e6-b195-4ec9-85cd-665c57d296ab",
                               "type": [
                                 "VerifiableCredential",
                                 "UseCaseFrameworkCondition"
                               ],
                               "issuer": "did:web:localhost:BPNL000000000000",
                               "issuanceDate": "2023-07-19T13:49:58Z",
                               "expirationDate": "2024-12-31T18:30:00Z",
                               "credentialSubject": [
                                 {
                                   "holderIdentifier": "BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "type": "SustainabilityCredential",
                                   "contractTemplate": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                   "contractVersion": "1.0.0"
                                 }
                               ],
                               "proof": {
                                 "proofPurpose": "proofPurpose",
                                 "type": "JsonWebSignature2020",
                                 "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                 "created": "2023-07-19T13:50:02Z",
                                 "jws": "eyJhbGciOiJFZERTQSJ9..IkfgC6Gn9sOT1uu1zMiDIIqw6pV4Z8axkKvphegsCVWT9uo0HZp4J9L1ILxR-huINGR5QlGIKiVuLGB5kKDOAQ"
                               }
                             }
                            """),
                    @ExampleObject(name = "Quality Credential", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/2018/credentials/v1",
                                 "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                 "https://w3id.org/security/suites/jws-2020/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000#46a8c5e6-b195-4ec9-85cd-665c57d296ab",
                               "type": [
                                 "VerifiableCredential",
                                 "UseCaseFrameworkCondition"
                               ],
                               "issuer": "did:web:localhost:BPNL000000000000",
                               "issuanceDate": "2023-07-19T13:49:58Z",
                               "expirationDate": "2024-12-31T18:30:00Z",
                               "credentialSubject": [
                                 {
                                   "holderIdentifier": "BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "type": "QualityCredential",
                                   "contractTemplate": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                   "contractVersion": "1.0.0"
                                 }
                               ],
                               "proof": {
                                 "proofPurpose": "proofPurpose",
                                 "type": "JsonWebSignature2020",
                                 "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                 "created": "2023-07-19T13:50:02Z",
                                 "jws": "eyJhbGciOiJFZERTQSJ9..IkfgC6Gn9sOT1uu1zMiDIIqw6pV4Z8axkKvphegsCVWT9uo0HZp4J9L1ILxR-huINGR5QlGIKiVuLGB5kKDOAQ"
                               }
                             }
                            """),
                    @ExampleObject(name = "Traceability Credential", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/2018/credentials/v1",
                                 "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                 "https://w3id.org/security/suites/jws-2020/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000#46a8c5e6-b195-4ec9-85cd-665c57d296ab",
                               "type": [
                                 "VerifiableCredential",
                                 "UseCaseFrameworkCondition"
                               ],
                               "issuer": "did:web:localhost:BPNL000000000000",
                               "issuanceDate": "2023-07-19T13:49:58Z",
                               "expirationDate": "2024-12-31T18:30:00Z",
                               "credentialSubject": [
                                 {
                                   "holderIdentifier": "BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "type": "TraceabilityCredential",
                                   "contractTemplate": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                   "contractVersion": "1.0.0"
                                 }
                               ],
                               "proof": {
                                 "proofPurpose": "proofPurpose",
                                 "type": "JsonWebSignature2020",
                                 "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                 "created": "2023-07-19T13:50:02Z",
                                 "jws": "eyJhbGciOiJFZERTQSJ9..IkfgC6Gn9sOT1uu1zMiDIIqw6pV4Z8axkKvphegsCVWT9uo0HZp4J9L1ILxR-huINGR5QlGIKiVuLGB5kKDOAQ"
                               }
                             }
                            """),
                    @ExampleObject(name = "Resiliency Credential", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/2018/credentials/v1",
                                 "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                 "https://w3id.org/security/suites/jws-2020/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000#46a8c5e6-b195-4ec9-85cd-665c57d296ab",
                               "type": [
                                 "VerifiableCredential",
                                 "UseCaseFrameworkCondition"
                               ],
                               "issuer": "did:web:localhost:BPNL000000000000",
                               "issuanceDate": "2023-07-19T13:49:58Z",
                               "expirationDate": "2024-12-31T18:30:00Z",
                               "credentialSubject": [
                                 {
                                   "holderIdentifier": "BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "type": "ResiliencyCredential",
                                   "contractTemplate": "https://public.catena-x.org/contracts/traceabilty.v1.pdf",
                                   "contractVersion": "1.0.0"
                                 }
                               ],
                               "proof": {
                                 "proofPurpose": "proofPurpose",
                                 "type": "JsonWebSignature2020",
                                 "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                 "created": "2023-07-19T13:50:02Z",
                                 "jws": "eyJhbGciOiJFZERTQSJ9..IkfgC6Gn9sOT1uu1zMiDIIqw6pV4Z8axkKvphegsCVWT9uo0HZp4J9L1ILxR-huINGR5QlGIKiVuLGB5kKDOAQ"
                               }
                             }
                            """)
            })
    })
    @PostMapping(path = RestURI.API_CREDENTIALS_ISSUER_FRAMEWORK, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifiableCredential> issueFrameworkCredential(@Valid @RequestBody IssueFrameworkCredentialRequest request, Principal principal) {
        log.debug("Received request to issue framework credential. BPN: {}", getBPNFromToken(principal));
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
    @ApiResponse(responseCode = "401", description = "The request could not be completed due to a failed authorization.", content = {@Content(examples = {})})
    @ApiResponse(responseCode = "403", description = "The request could not be completed due to a forbidden access", content = {@Content(examples = {})})
    @ApiResponse(responseCode = "500", description = "Any other internal server error", content = {@Content(examples = {
            @ExampleObject(name = "Internal server error", value = """
                    {
                      "type": "about:blank",
                      "title": "Error Title",
                      "status": 500,
                      "detail": "Error Details",
                      "instance": "API endpoint",
                      "properties": {
                        "timestamp": 1689762476720
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "200", description = "Validate Verifiable Credentials", content = {
            @Content(examples = {
                    @ExampleObject(name = "Verifiable Credentials without check expiry", value = """
                             {
                               "valid": true,
                               "vc": {
                                 "issuanceDate": "2023-07-19T09:11:34Z",
                                 "credentialSubject": [
                                   {
                                     "bpn": "BPNL000000000000",
                                     "id": "did:web:localhost:BPNL000000000000",
                                     "type": "BpnCredential"
                                   }
                                 ],
                                 "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                 "proof": {
                                   "created": "2023-07-19T09:11:39Z",
                                   "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhfDw",
                                   "proofPurpose": "proofPurpose",
                                   "type": "JsonWebSignature2020",
                                   "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                 },
                                 "type": [
                                   "VerifiableCredential",
                                   "BpnCredential"
                                 ],
                                 "@context": [
                                   "https://www.w3.org/2018/credentials/v1",
                                   "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                   "https://w3id.org/security/suites/jws-2020/v1"
                                 ],
                                 "issuer": "did:web:localhost:BPNL000000000000",
                                 "expirationDate": "2024-12-31T18:30:00Z"
                               }
                             }
                            """),
                    @ExampleObject(name = "Verifiable Credentials with check expiry", value = """
                             {
                               "valid": true,
                               "validateExpiryDate": true,
                               "vc": {
                                 "issuanceDate": "2023-07-19T09:11:34Z",
                                 "credentialSubject": [
                                   {
                                     "bpn": "BPNL000000000000",
                                     "id": "did:web:localhost:BPNL000000000000",
                                     "type": "BpnCredential"
                                   }
                                 ],
                                 "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                 "proof": {
                                   "created": "2023-07-19T09:11:39Z",
                                   "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhfDw",
                                   "proofPurpose": "proofPurpose",
                                   "type": "JsonWebSignature2020",
                                   "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                 },
                                 "type": [
                                   "VerifiableCredential",
                                   "BpnCredential"
                                 ],
                                 "@context": [
                                   "https://www.w3.org/2018/credentials/v1",
                                   "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                   "https://w3id.org/security/suites/jws-2020/v1"
                                 ],
                                 "issuer": "did:web:localhost:BPNL000000000000",
                                 "expirationDate": "2024-12-31T18:30:00Z"
                               }
                             }
                            """),
                    @ExampleObject(name = "Verifiable expired credentials with check expiry ", value = """
                             {
                               "valid": false,
                               "validateExpiryDate": false,
                               "vc": {
                                 "issuanceDate": "2023-07-19T09:11:34Z",
                                 "credentialSubject": [
                                   {
                                     "bpn": "BPNL000000000000",
                                     "id": "did:web:localhost:BPNL000000000000",
                                     "type": "BpnCredential"
                                   }
                                 ],
                                 "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                 "proof": {
                                   "created": "2023-07-19T09:11:39Z",
                                   "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhfDw",
                                   "proofPurpose": "proofPurpose",
                                   "type": "JsonWebSignature2020",
                                   "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                 },
                                 "type": [
                                   "VerifiableCredential",
                                   "BpnCredential"
                                 ],
                                 "@context": [
                                   "https://www.w3.org/2018/credentials/v1",
                                   "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                   "https://w3id.org/security/suites/jws-2020/v1"
                                 ],
                                 "issuer": "did:web:localhost:BPNL000000000000",
                                 "expirationDate": "2022-12-31T18:30:00Z"
                               }
                             }
                            """),
                    @ExampleObject(name = "Verifiable Credentials with invalid signature", value = """
                             {
                               "valid": false,
                               "vc": {
                               "@context": [
                                   "https://www.w3.org/2018/credentials/v1",
                                   "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                   "https://w3id.org/security/suites/jws-2020/v1"
                                 ],
                                 "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                 "type": [
                                   "VerifiableCredential",
                                   "BpnCredential"
                                 ],
                                 "issuer": "did:web:localhost:BPNL000000000000",
                                 "expirationDate": "2024-12-31T18:30:00Z"
                                 "issuanceDate": "2023-07-19T09:11:34Z",
                                 "credentialSubject": [
                                   {
                                     "bpn": "BPNL000000000000",
                                     "id": "did:web:localhost:BPNL000000000000",
                                     "type": "BpnCredential"
                                   }
                                 ],
                                 "proof": {
                                   "created": "2023-07-19T09:11:39Z",
                                   "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhf",
                                   "proofPurpose": "proofPurpose",
                                   "type": "JsonWebSignature2020",
                                   "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                 },
                               }
                             }
                            """)
            })
    })
    @Operation(summary = "Validate Verifiable Credentials", description = "Permission: **view_wallets** OR **view_wallet** \n\n Validate Verifiable Credentials")
    @PostMapping(path = RestURI.CREDENTIALS_VALIDATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                  "@context": [
                                    "https://www.w3.org/2018/credentials/v1",
                                    "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                    "https://w3id.org/security/suites/jws-2020/v1"
                                  ],
                                  "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                  "type": [
                                    "VerifiableCredential",
                                    "BpnCredential"
                                  ],
                                  "issuer": "did:web:localhost:BPNL000000000000",
                                  "issuanceDate": "2023-07-19T09:11:34Z",
                                  "expirationDate": "2024-12-31T18:30:00Z",
                                  "credentialSubject": [
                                    {
                                      "bpn": "BPNL000000000000",
                                      "id": "did:web:localhost:BPNL000000000000",
                                      "type": "BpnCredential"
                                    }
                                  ],
                                  "proof": {
                                    "created": "2023-07-19T09:11:39Z",
                                    "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhfDw",
                                    "proofPurpose": "proofPurpose",
                                    "type": "JsonWebSignature2020",
                                    "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                  }
                                }
                    """))
    })
    public ResponseEntity<Map<String, Object>> credentialsValidation(@RequestBody Map<String, Object> data,
                                                                     @Parameter(description = "Check expiry of VC") @RequestParam(name = "withCredentialExpiryDate", defaultValue = "false", required = false) boolean withCredentialExpiryDate) {
        log.debug("Received request to validate verifiable credentials");
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
    @ApiResponse(responseCode = "401", description = "The request could not be completed due to a failed authorization.", content = {@Content(examples = {})})
    @ApiResponse(responseCode = "403", description = "The request could not be completed due to a forbidden access", content = {@Content(examples = {})})
    @ApiResponse(responseCode = "500", description = "Any other internal server error", content = {@Content(examples = {
            @ExampleObject(name = "Internal server error", value = """
                    {
                      "type": "about:blank",
                      "title": "Error Title",
                      "status": 500,
                      "detail": "Error Details",
                      "instance": "API endpoint",
                      "properties": {
                        "timestamp": 1689762476720
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "404", description = "Wallet not found with provided identifier", content = {@Content(examples = {
            @ExampleObject(name = "Wallet not found with provided identifier", value = """
                    {
                      "type": "about:blank",
                      "title": "Error Title",
                      "status": 404,
                      "detail": "Error Details",
                      "instance": "API endpoint",
                      "properties": {
                        "timestamp": 1689762476720
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "400", description = "The input does not comply to the syntax requirements", content = {
            @Content(examples = {
                    @ExampleObject(name = "Response in case of invalid data provided", value = """
                             {
                                 "type": "about:blank",
                                 "title": "Invalid data provided",
                                 "status": 400,
                                 "detail": "details",
                                 "instance": "API endpoint",
                                 "properties":
                                 {
                                     "timestamp": 1689760833962,
                                     "errors":
                                     {
                                         "filed": "filed error message"
                                     }
                                 }
                             }
                            """)
            })
    })
    @ApiResponse(responseCode = "201", description = "Issuer credential", content = {
            @Content(examples = {
                    @ExampleObject(name = "Issuer credential", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/2018/credentials/v1",
                                 "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                 "https://w3id.org/security/suites/jws-2020/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000#ff084e7a-1b46-4a2f-a78d-3d701a0bd6e4",
                               "type": [
                                 "VerifiableCredential",
                                 "BpnCredential"
                               ],
                               "issuer": "did:web:localhost:BPNL000000000000",
                               "issuanceDate": "2023-07-19T12:18:30Z",
                               "expirationDate": "2024-12-31T18:30:00Z",
                               "credentialSubject": [
                                 {
                                   "bpn": "BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "type": "BpnCredential"
                                 }
                               ],
                               "proof": {
                                 "proofPurpose": "proofPurpose",
                                 "type": "JsonWebSignature2020",
                                 "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                 "created": "2023-07-19T12:18:34Z",
                                 "jws": "eyJhbGciOiJFZERTQSJ9..0Ua1vcTQAYwQY3PPuHr4RQxqW6iIngrHQQx1oPgk2uzqUpcbfY2YUxXAnbNA333-lSuvNhiV_1NLfBnCEcI2DQ"
                               }
                             }
                            """)
            })
    })
    @Operation(summary = "Issue Verifiable Credential", description = "Permission: **update_wallets** (The BPN of the base wallet must equal BPN of caller)\nIssue a verifiable credential with a given issuer DID")
    @PostMapping(path = RestURI.ISSUERS_CREDENTIALS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                  "@context": [
                                    "https://www.w3.org/2018/credentials/v1",
                                    "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                    "https://w3id.org/security/suites/jws-2020/v1"
                                  ],
                                  "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                  "type": [
                                    "VerifiableCredential",
                                    "BpnCredential"
                                  ],
                                  "issuer": "did:web:localhost:BPNL000000000000",
                                  "issuanceDate": "2023-07-19T09:11:34Z",
                                  "expirationDate": "2024-12-31T18:30:00Z",
                                  "credentialSubject": [
                                    {
                                      "bpn": "BPNL000000000000",
                                      "id": "did:web:localhost:BPNL000000000000",
                                      "type": "BpnCredential"
                                    }
                                  ]
                                }
                    """))
    })
    public ResponseEntity<VerifiableCredential> issueCredentialUsingBaseWallet(@Parameter(description = "Holder DID", examples = {@ExampleObject(description = "did", name = "did", value = "did:web:localhost:BPNL000000000000")}) @RequestParam(name = "holderDid") String holderDid, @RequestBody Map<String, Object> data, Principal principal) {
        log.debug("Received request to issue verifiable credential. BPN: {}", getBPNFromToken(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(issuersCredentialService.issueCredentialUsingBaseWallet(holderDid, data, getBPNFromToken(principal)));
    }
}
