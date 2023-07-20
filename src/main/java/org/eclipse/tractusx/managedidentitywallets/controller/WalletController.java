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
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * The type Wallet controller.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Wallets")
public class WalletController extends BaseController {

    private final WalletService service;

    /**
     * Create wallet response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = {

                    @ExampleObject(name = "Create wallet with BPN", value = """
                                                        {
                                                          "bpn": "BPNL000000000001",
                                                          "name": "companyA"
                                                        }
                            """)
            })
    })
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
    @ApiResponse(responseCode = "401", description = "The request could not be completed due to a failed authorization.", content = {@Content(examples = {})})
    @ApiResponse(responseCode = "403", description = "The request could not be completed due to a forbidden access", content = {@Content(examples = {})})
    @ApiResponse(responseCode = "409", description = "The request could not be completed due to a conflict.", content = {@Content(examples = {
            @ExampleObject(name = "Wallet already exist", value = """
                    {
                      "type": "about:blank",
                      "title": "Wallet is already exists for bpn BPNL000000000001",
                      "status": 409,
                      "detail": "Wallet is already exists for bpn BPNL000000000001",
                      "instance": "/api/wallets",
                      "properties": {
                        "timestamp": 1689762639948
                      }
                    }
                    """)
    })})
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
    @ApiResponse(responseCode = "201", content = {
            @Content(examples = {
                    @ExampleObject(name = "Success response", value = """
                                     {
                                         "name": "companyA",
                                         "did": "did:web:localhost:BPNL000000000001",
                                         "bpn": "BPNL000000000501",
                                         "algorithm": "ED25519",
                                         "didDocument":
                                         {
                                             "@context":
                                             [
                                                 "https://www.w3.org/ns/did/v1",
                                                 "https://w3c.github.io/vc-jws-2020/contexts/v1"
                                             ],
                                             "id": "did:web:localhost:BPNL000000000001",
                                             "verificationMethod":
                                             [
                                                 {
                                                     "controller": "did:web:localhost:BPNL000000000001",
                                                     "id": "did:web:localhost:BPNL000000000001#",
                                                     "publicKeyJwk":
                                                     {
                                                         "crv": "Ed25519",
                                                         "kty": "OKP",
                                                         "x": "0Ap6FsX5UuRBIoOzxWtcFA2ymnqXw0U08Ino_mIuYM4"
                                                     },
                                                     "type": "JsonWebKey2020"
                                                 }
                                             ]
                                         }
                                     }              
                            """)
            })
    })
    @Operation(summary = "Create Wallet", description = "Permission: **add_wallets** \n\n Create a wallet and store it")
    @PostMapping(path = RestURI.WALLETS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createWallet(request));
    }

    /**
     * Store credential response entity.
     *
     * @param data       the data
     * @param identifier the identifier
     * @return the response entity
     */
    @Operation(summary = "Store Verifiable Credential", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of wallet to extract credentials from must equal BPN of caller) \n\n Store a verifiable credential in the wallet of the given identifier")
    @PostMapping(path = RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                 {
                                     "@context":
                                     [
                                         "https://www.w3.org/2018/credentials/v1",
                                         "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"
                                     ],
                                     "id": "did:web:localhost.in#123456789",
                                     "type":
                                     [
                                         "VerifiableCredential", "LegalParticipant"
                                     ],
                                     "issuer": "did:web:localhost.in",
                                     "issuanceDate": "2023-05-04T07:36:03.633Z",
                                     "credentialSubject":
                                     {
                                         "id": "https://localhost/.well-known/participant.json",
                                         "type": "gx:LegalParticipant",
                                         "gx:legalName": "Sample Company",
                                         "gx:legalRegistrationNumber":
                                         {
                                             "gx:taxID": "113123123"
                                         },
                                         "gx:headquarterAddress":
                                         {
                                             "gx:countrySubdivisionCode": "BE-BRU"
                                         },
                                         "gx:legalAddress":
                                         {
                                             "gx:countrySubdivisionCode": "BE-BRU"
                                         },
                                         "gx-terms-and-conditions:gaiaxTermsAndConditions": "70c1d713215f95191a11d38fe2341faed27d19e083917bc8732ca4fea4976700"
                                     },
                                     "proof":
                                     {
                                         "type": "JsonWebSignature2020",
                                         "created": "2023-05-04T07:36:04.079Z",
                                         "proofPurpose": "assertionMethod",
                                         "verificationMethod": "did:web:localhost",
                                         "jws": "eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..iHki8WC3nPfcSRkC_AV4tXh0ikfT7BLPTGc_0ecI8zontTmJLqwcpPfAt0PFsoo3SkZgc6j636z55jj5tagBc-OKoiDu7diWryNAnL9ASsmWJyrPhOKVARs6x6PxVaTFBuyCfAHZeipxmkcYfNB_jooIXO2HuRcL2odhsQHELkGc5IDD-aBMWyNpfVAaYQ-cCzvDflZQlsowziUKfMkBfwpwgMdXFIgKWYdDIRvzA-U-XiC11-6QV7tPeKsMguEU0F5bh8cCEm2rooqXtENcsM_7cqFdQoOyblJyM-agoz2LUTj9QIdn9_gnNkGN-2U7_qBJWmHkK1Hm_mHqcNeeQw"
                                     }
                                 }
                    """))
    })
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
    @ApiResponse(responseCode = "404", description = "Wallet not found with provided identifier", content = {@Content(examples = {
            @ExampleObject(name = "Wallet not found with provided identifier", value = """
                    {
                        "type": "about:blank",
                        "title": "Wallet not found for identifier did:web:localhost:BPNL000000044001",
                        "status": 404,
                        "detail": "Wallet not found for identifier did:web:localhost:BPNL000000044001",
                        "instance": "/api/wallets/did%3Aweb%3Alocalhost%3ABPNL0000000/credentials",
                        "properties": {
                          "timestamp": 1689765541959
                        }
                      }
                    """)
    })})
    @ApiResponse(responseCode = "201", description = "Success Response", content = {@Content(examples = {
            @ExampleObject(name = "Success Response", value = """
                     {
                        "message": "Credential with id did:web:localhost#123456789 has been successfully stored"
                      }
                    """)
    })})
    public ResponseEntity<Map<String, String>> storeCredential(@RequestBody Map<String, Object> data,
                                                               @Parameter(description = "Did or BPN", examples = {@ExampleObject(name = "bpn", value = "BPNL000000000001", description = "bpn"), @ExampleObject(description = "did", name = "did", value = "did:web:localhost:BPNL000000000001")}) @PathVariable(name = "identifier") String identifier, Principal principal) {

        return ResponseEntity.status(HttpStatus.CREATED).body(service.storeCredential(data, identifier, getBPNFromToken(principal)));
    }

    /**
     * Gets wallet by bpn.
     *
     * @param identifier      the identifier
     * @param withCredentials the with credentials
     * @return the wallet by bpn
     */
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
    @ApiResponse(responseCode = "404", description = "Wallet not found with provided identifier", content = {@Content(examples = {
            @ExampleObject(name = "Wallet not found with provided identifier", value = """
                    {
                       "type": "about:blank",
                       "title": "Wallet not found for identifier did:web:localhost:BPNL0000000501",
                       "status": 404,
                       "detail": "Wallet not found for identifier did:web:localhost:BPNL0000000501",
                       "instance": "/api/wallets/did%3Aweb%3Alocalhost%3ABPNL0000000501",
                       "properties": {
                         "timestamp": 1689764377224
                       }
                     }
                    """)
    })})
    @ApiResponse(responseCode = "200", description = "Wallet Details", content = {@Content(examples = {
            @ExampleObject(name = "Wallet details without with credentials false", value = """
                    {
                        "name": "companyA",
                        "did": "did:web:localhost:BPNL000000000001",
                        "bpn": "BPNL000000000001",
                        "algorithm": "ED25519",
                        "didDocument":
                        {
                            "@context":
                            [
                                "https://www.w3.org/ns/did/v1",
                                "https://w3c.github.io/vc-jws-2020/contexts/v1"
                            ],
                            "id": "did:web:localhost:BPNL000000000001",
                            "verificationMethod":
                            [
                                {
                                    "controller": "did:web:localhost:BPNL000000000001",
                                    "id": "did:web:localhost:BPNL000000000001#",
                                    "publicKeyJwk":
                                    {
                                        "crv": "Ed25519",
                                        "kty": "OKP",
                                        "x": "mhph0ZSVk7cDVmazbaaC3jBDpphW4eNygAK9gHPlMow"
                                    },
                                    "type": "JsonWebKey2020"
                                }
                            ]
                        }
                    }
                    """), @ExampleObject(name = "Wallet details without with credentials true", value = """
                        {
                            "name": "companyA",
                            "did": "did:web:localhost:BPNL000000000001",
                            "bpn": "BPNL000000000001",
                            "algorithm": "ED25519",
                            "didDocument":
                            {
                                "@context":
                                [
                                    "https://www.w3.org/ns/did/v1",
                                    "https://w3c.github.io/vc-jws-2020/contexts/v1"
                                ],
                                "id": "did:web:localhost:BPNL000000000001",
                                "verificationMethod":
                                [
                                    {
                                        "controller": "did:web:localhost:BPNL000000000001",
                                        "id": "did:web:localhost:BPNL000000000001#",
                                        "publicKeyJwk":
                                        {
                                            "crv": "Ed25519",
                                            "kty": "OKP",
                                            "x": "mhph0ZSVk7cDVmazbaaC3jBDpphW4eNygAK9gHPlMow"
                                        },
                                        "type": "JsonWebKey2020"
                                    }
                                ]
                            },
                            "verifiableCredentials":
                            [
                                {
                                    "@context":
                                    [
                                        "https://www.w3.org/2018/credentials/v1",
                                        "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                        "https://w3id.org/security/suites/jws-2020/v1"
                                    ],
                                    "id": "did:web:localhost:BPNL000000000000#a1f8ae36-9919-4ed8-8546-535280acc5bf",
                                    "type":
                                    [
                                        "VerifiableCredential",
                                        "BpnCredential"
                                    ],
                                    "issuer": "did:web:localhost:BPNL000000000000",
                                    "issuanceDate": "2023-07-19T09:14:45Z",
                                    "expirationDate": "2023-09-30T18:30:00Z",
                                    "credentialSubject":
                                    [
                                        {
                                            "bpn": "BPNL000000000001",
                                            "id": "did:web:localhost:BPNL000000000001",
                                            "type": "BpnCredential"
                                        }
                                    ],
                                    "proof":
                                    {
                                        "created": "2023-07-19T09:14:47Z",
                                        "jws": "eyJhbGciOiJFZERTQSJ9..O69dLGMDVgZQJ7chFx3aUbkJFvibH8WWunw634rIDC77_pdiUHvQpQ0hq15_7OgFMy3dp-9H-pNgxTZ-i4UXCw",
                                        "proofPurpose": "proofPurpose",
                                        "type": "JsonWebSignature2020",
                                        "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                    }
                                }
                            ]
                        }
            """)
    })})
    @Operation(summary = "Retrieve wallet by identifier", description = "Permission: **view_wallets** OR **view_wallet** (The BPN of Wallet to retrieve must equal the BPN of caller or Base wallet, authority wallet can see all wallets) \n\n Retrieve single wallet by identifier, with or without its credentials")
    @GetMapping(path = RestURI.API_WALLETS_IDENTIFIER, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> getWalletByIdentifier(@Parameter(description = "Did or BPN", examples = {@ExampleObject(name = "bpn", value = "BPNL000000000001", description = "bpn"), @ExampleObject(description = "did", name = "did", value = "did:web:localhost:BPNL000000000001")}) @PathVariable(name = "identifier") String identifier,
                                                        @RequestParam(name = "withCredentials", defaultValue = "false") boolean withCredentials,
                                                        Principal principal) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getWalletByIdentifier(identifier, withCredentials, getBPNFromToken(principal)));
    }

    /**
     * Gets wallets.
     *
     * @return the wallets
     */
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
    @ApiResponse(responseCode = "200", description = "Wallet list", content = {
            @Content(examples = {
                    @ExampleObject(name = "Wallet list", value = """
                             {
                               "content": [
                                 {
                                   "name": "companyA",
                                   "did": "did:web:localhost:BPNL000000000001",
                                   "bpn": "BPNL000000000001",
                                   "algorithm": "ED25519",
                                   "didDocument": {
                                   "@context": [
                                       "https://www.w3.org/ns/did/v1",
                                       "https://w3c.github.io/vc-jws-2020/contexts/v1"
                                     ],
                                     "id": "did:web:localhost:BPNL000000000001",
                                     "verificationMethod": [
                                       {
                                         "controller": "did:web:localhost:BPNL000000000001",
                                         "id": "did:web:localhost:BPNL000000000001#",
                                         "publicKeyJwk": {
                                           "crv": "Ed25519",
                                           "kty": "OKP",
                                           "x": "mhph0ZSVk7cDVmazbaaC3jBDpphW4eNygAK9gHPlMow"
                                         },
                                         "type": "JsonWebKey2020"
                                       }
                                     ]
                                   }
                                 }
                               ],
                               "pageable": {
                                 "sort": {
                                   "empty": false,
                                   "sorted": true,
                                   "unsorted": false
                                 },
                                 "offset": 0,
                                 "pageNumber": 0,
                                 "pageSize": 1,
                                 "paged": true,
                                 "unpaged": false
                               },
                               "totalElements": 3,
                               "totalPages": 3,
                               "last": false,
                               "size": 1,
                               "number": 0,
                               "sort": {
                                 "empty": false,
                                 "sorted": true,
                                 "unsorted": false
                               },
                               "first": true,
                               "numberOfElements": 1,
                               "empty": false
                             }
                            """)
            })
    })
    @Operation(summary = "List of wallets", description = "Permission: **view_wallets** \n\n Retrieve list of registered wallets")
    @GetMapping(path = RestURI.WALLETS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Wallet>> getWallets(@Parameter(name = "pageNumber", description = "Page number, Page number start with zero") @RequestParam(required = false, defaultValue = "0") int pageNumber,
                                                   @Parameter(name = "size", description = "Number of records per page") @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") int size,
                                                   @Parameter(name = "sortColumn", description = "Sort column name", examples = {
                                                           @ExampleObject(value = "createdAt", name = "Creation date"),
                                                           @ExampleObject(value = "name", name = "Wallet name"),
                                                           @ExampleObject(value = "did", name = "Wallet did"),
                                                           @ExampleObject(value = "bpn", name = "Wallet BPN")
                                                   }
                                                   )
                                                   @RequestParam(required = false, defaultValue = "createdAt") String sortColumn,
                                                   @Parameter(name = "sortTpe", description = "Sort order", examples = {@ExampleObject(value = "desc", name = "Descending order"), @ExampleObject(value = "asc", name = "Ascending order")}) @RequestParam(required = false, defaultValue = "desc") String sortTpe) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getWallets(pageNumber, size, sortColumn, sortTpe));
    }
}