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
    @ApiResponse(responseCode = "400", description = "The input does not comply to the syntax requirements", content = {
            @Content(examples = {
                    @ExampleObject(name = "Response in case of invalid data provided", value = """
                             {
                                 "type": "about:blank",
                                 "title": "title",
                                 "status": 400,
                                 "detail": "details",
                                 "instance": "API endpoint",
                                 "properties":
                                 {
                                     "timestamp": 1689760833962,
                                     "errors":
                                     {
                                     }
                                 }
                             }
                            """)
            })
    })
    @ApiResponse(responseCode = "200", description = "Issuer credential list", content = {
            @Content(examples = {
                    @ExampleObject(name = "Issuer credential list", value = """
                             {
                                "content": [
                                  {
                                    "@context": [
                                      "https://www.w3.org/2018/credentials/v1",
                                      "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                      "https://w3id.org/security/suites/jws-2020/v1"
                                    ],
                                    "id": "did:web:localhost:BPNL000000000000#ae364f71-f054-4d91-b579-f001bcb3e59e",
                                    "type": [
                                      "VerifiableCredential",
                                      "BpnCredential"
                                    ],
                                    "issuer": "did:web:localhost:BPNL000000000000",
                                    "issuanceDate": "2023-07-19T09:27:42Z",
                                    "expirationDate": "2024-12-31T18:30:00Z",
                                    "credentialSubject": [
                                      {
                                        "bpn": "BPNL000000000000",
                                        "id": "did:web:localhost:BPNL000000000000",
                                        "type": "BpnCredential"
                                      }
                                    ],
                                    "proof": {
                                      "created": "2023-07-19T09:27:44Z",
                                      "jws": "eyJhbGciOiJFZERTQSJ9..evDHQfW4EzJUt2HnS_WlmO8FFtywTGnwyywtCE7WP41my4Iscpqr4tbuVOqnZg85b4U8L3_ut8_pEONIhbExCQ",
                                      "proofPurpose": "proofPurpose",
                                      "type": "JsonWebSignature2020",
                                      "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                    }
                                  },
                                  {
                                    "type": [
                                      "VerifiableCredential",
                                      "SummaryCredential"
                                    ],
                                    "@context": [
                                      "https://www.w3.org/2018/credentials/v1",
                                      "https://catenax-ng.github.io/product-core-schemas/SummaryVC.json",
                                      "https://w3id.org/security/suites/jws-2020/v1"
                                    ],
                                    "issuer": "did:web:localhost:BPNL000000000000",
                                    "issuanceDate": "2023-07-19T09:11:39Z",
                                    "expirationDate": "2024-12-31T18:30:00Z"
                                    "credentialSubject": [
                                      {
                                        "contractTemplate": "https://public.catena-x.org/contracts/",
                                        "holderIdentifier": "BPNL000000000000",
                                        "id": "did:web:localhost:BPNL000000000000",
                                        "items": [
                                          "BpnCredential"
                                        ],
                                        "type": "SummaryCredential"
                                      }
                                    ],
                                    "proof": {
                                      "created": "2023-07-19T09:11:41Z",
                                      "jws": "eyJhbGciOiJFZERTQSJ9..YvoFhDip3TQAfZUIu0yc843oA4uGTg049dMFt_GoaMmPjiNB_B1EFOL-gDpwjIxTYNlGOO_CLp9qStbzlDTNBg",
                                      "proofPurpose": "proofPurpose",
                                      "type": "JsonWebSignature2020",
                                      "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                    }
                                  },
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
                                ],
                                "pageable": {
                                  "sort": {
                                    "empty": false,
                                    "unsorted": false,
                                    "sorted": true
                                  },
                                  "offset": 0,
                                  "pageNumber": 0,
                                  "pageSize": 2147483647,
                                  "paged": true,
                                  "unpaged": false
                                },
                                "last": true,
                                "totalPages": 1,
                                "totalElements": 3,
                                "first": true,
                                "size": 2147483647,
                                "number": 0,
                                "sort": {
                                  "empty": false,
                                  "unsorted": false,
                                  "sorted": true
                                },
                                "numberOfElements": 3,
                                "empty": false
                              }
                            """)
            })
    })
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
    @ApiResponse(responseCode = "409", description = "The request could not be completed due to a conflict.", content = {@Content(examples = {
            @ExampleObject(name = "MembershipCredential already exist", value = """
                    {
                      "type": "about:blank",
                      "title": "Credential of type MembershipCredential is already exists ",
                      "status": 409,
                      "detail": "Credential of type MembershipCredential is already exists ",
                      "instance": "/api/credentials/issuer/membership",
                      "properties": {
                        "timestamp": 1689772483831
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
                    @ExampleObject(name = "Membership credential", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/2018/credentials/v1",
                                 "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                 "https://w3id.org/security/suites/jws-2020/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000#0d6b6447-99de-4bc5-94f3-3ac0ae8ee188",
                               "type": [
                                 "VerifiableCredential",
                                 "MembershipCredential"
                               ],
                               "issuer": "did:web:localhost:BPNL000000000000",
                               "issuanceDate": "2023-07-19T13:13:53Z",
                               "expirationDate": "2024-12-31T18:30:00Z",
                               "credentialSubject": [
                                 {
                                   "holderIdentifier": "BPNL000000000000",
                                   "startTime": "2023-07-19T13:13:53.581081Z",
                                   "memberOf": "Catena-X",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "type": "MembershipCredential",
                                   "status": "Active"
                                 }
                               ],
                               "proof": {
                                 "proofPurpose": "proofPurpose",
                                 "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                 "type": "JsonWebSignature2020",
                                 "created": "2023-07-19T13:13:57Z",
                                 "jws": "eyJhbGciOiJFZERTQSJ9..zt7SyONY1shO7N6KrabQJr9uNrToM1Bc4eagTQc1LxAfZ1v-SSp9Y-2cpZNDV8AR08r4L8VbtWrR9t2dNoAfDw"
                               }
                             }
                            """)
            })
    })
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
    @ApiResponse(responseCode = "409", description = "The request could not be completed due to a conflict.", content = {@Content(examples = {
            @ExampleObject(name = "DismantlerCredential already exist", value = """
                    {
                      "type": "about:blank",
                      "title": "Credential of type DismantlerCredential is already exists ",
                      "status": 409,
                      "detail": "Credential of type DismantlerCredential is already exists ",
                      "instance": "/api/credentials/issuer/dismantler",
                      "properties": {
                        "timestamp": 1689773804746
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
    @ApiResponse(responseCode = "201", description = "Dismantler Credential", content = {
            @Content(examples = {
                    @ExampleObject(name = "Dismantler Credential", value = """
                             {
                                "@context": [
                                  "https://www.w3.org/2018/credentials/v1",
                                  "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                  "https://w3id.org/security/suites/jws-2020/v1"
                                ],
                                "id": "did:web:localhost:BPNL000000000000#5caac86c-8ef8-4aab-9d2b-fb18c62560a9",
                                "type": [
                                  "VerifiableCredential",
                                  "DismantlerCredential"
                                ],
                                "issuer": "did:web:localhost:BPNL000000000000",
                                "issuanceDate": "2023-07-19T13:35:33Z",
                                "expirationDate": "2024-12-31T18:30:00Z",
                                "credentialSubject": [
                                  {
                                    "holderIdentifier": "BPNL000000000000",
                                    "allowedVehicleBrands": [
                                      "Audi",
                                      "Abarth",
                                      "Alfa Romeo",
                                      "Chrysler"
                                    ],
                                    "id": "did:web:localhost:BPNL000000000000",
                                    "activityType": "vehicleDismantle",
                                    "type": "DismantlerCredential"
                                  }
                                ],
                                "proof": {
                                  "proofPurpose": "proofPurpose",
                                  "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                  "type": "JsonWebSignature2020",
                                  "created": "2023-07-19T13:35:38Z",
                                  "jws": "eyJhbGciOiJFZERTQSJ9..UI82uq6iyqoaKjZIhJiV24v_Bqnj_7EqWiqZ3VWjqkoHLnr7JDtW5KVywWPl27j_baLBxxnM5jqjQdSK4rfbBg"
                                }
                              }
                            """)
            })
    })
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
                               },
                               "validateExpiryDate": true
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
        return ResponseEntity.status(HttpStatus.CREATED).body(issuersCredentialService.issueCredentialUsingBaseWallet(holderDid, data, getBPNFromToken(principal)));
    }
}
