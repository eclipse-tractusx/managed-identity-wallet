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

package org.eclipse.tractusx.managedidentitywallets.apidocs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class HoldersCredentialControllerApiDocs {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "401", description = "The request could not be completed due to a failed authorization.", content = {
                    @Content(examples = {}) }),
            @ApiResponse(responseCode = "403", description = "The request could not be completed due to a forbidden access", content = {
                    @Content(examples = {}) }),
            @ApiResponse(responseCode = "500", description = "Any other internal server error", content = {
                    @Content(examples = {
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
                    }) }),
            @ApiResponse(responseCode = "404", description = "Wallet not found with caller BPN", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Wallet not found with caller BPN", value = """
                                    {
                                        "type": "about:blank",
                                        "title": "Wallet not found for identifier did:web:localhost:BPNL0000000",
                                        "status": 404,
                                        "detail": "Wallet not found for identifier did:web:localhost:BPNL0000000",
                                        "instance": "/api/wallets/did%3Aweb%3Alocalhost%3ABPNL0000000/credentials",
                                        "properties": {
                                          "timestamp": 1689765541959
                                        }
                                      }
                                    """)
                    }) }),
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
            }),
            @ApiResponse(responseCode = "200", description = "Credential list", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Credential list", value = """
                                                                {
                                                                    "content":
                                                                    [
                                                                        {
                                                                            "@context":
                                                                            [
                                                                                "https://www.w3.org/2018/credentials/v1",
                                                                                "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
                                                                                "https://w3id.org/security/suites/jws-2020/v1"
                                                                            ],
                                                                            "id": "did:web:localhost:BPNL000000000000#954d43de-ebed-481d-9e35-e3bbb311b8f5",
                                                                            "type":
                                                                            [
                                                                                "VerifiableCredential",
                                                                                "SummaryCredential"
                                                                            ],
                                                                            "issuer": "did:web:localhost:BPNL000000000000",
                                                                            "issuanceDate": "2023-07-14T11:05:48Z",
                                                                            "expirationDate": "2023-09-30T18:30:00Z",
                                                                            "credentialSubject":
                                                                            [
                                                                                {
                                                                                    "contractTemplate": "https://public.catena-x.org/contracts/",
                                                                                    "holderIdentifier": "BPNL000000000000",
                                                                                    "id": "did:web:localhost:BPNL000000000000",
                                                                                    "items":
                                                                                    [
                                                                                        "BpnCredential"
                                                                                    ],
                                                                                    "type": "SummaryCredential"
                                                                                }
                                                                            ],
                                                                            "proof":
                                                                            {
                                                                                "created": "2023-07-14T11:05:50Z",
                                                                                "jws": "eyJhbGciOiJFZERTQSJ9..4xwFUCtP0xXVEo5_lXd90Vv-TWO2FijZut-HZ5cozAQseexj8EpTkK1erhFbf2Ua1kb8pi_H5At5HiPkTxSIAQ",
                                                                                "proofPurpose": "proofPurpose",
                                                                                "type": "JsonWebSignature2020",
                                                                                "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                                                            }
                                                                        }
                                                                    ],
                                                                    "pageable":
                                                                    {
                                                                        "sort":
                                                                        {
                                                                            "empty": false,
                                                                            "sorted": true,
                                                                            "unsorted": false
                                                                        },
                                                                        "offset": 0,
                                                                        "pageNumber": 0,
                                                                        "pageSize": 2147483647,
                                                                        "paged": true,
                                                                        "unpaged": false
                                                                    },
                                                                    "totalElements": 1,
                                                                    "totalPages": 1,
                                                                    "last": true,
                                                                    "size": 2147483647,
                                                                    "number": 0,
                                                                    "sort":
                                                                    {
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
            }) })
    @Operation(description = "Permission: **view_wallets** OR **view_wallet** (The BPN of holderIdentifier must equal BPN of caller)\n\n Search verifiable credentials with filter criteria", summary = "Query Verifiable Credentials", security = { @SecurityRequirement(name = "Authenticate using access_token") })
    public @interface GetCredentialsApiDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Success Response", content = { @Content(examples = {
                    @ExampleObject(name = "Success Response", value = """
                                                {
                                                    "@context":
                                                    [
                                                        "https://www.w3.org/2018/credentials/v1",
                                                        "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
                                                        "https://w3id.org/security/suites/jws-2020/v1"
                                                    ],
                                                    "id": "did:web:localhost:BPNL000000000000#319a2641-9407-4c39-bf51-a4a109b59604",
                                                    "type":
                                                    [
                                                        "VerifiableCredential",
                                                        "BankDetails"
                                                    ],
                                                    "issuer": "did:web:localhost:BPNL000000000000",
                                                    "issuanceDate": "2023-07-19T13:41:52Z",
                                                    "expirationDate": "2024-12-31T18:30:00Z",
                                                    "credentialSubject":
                                                    [
                                                        {
                                                            "bpn": "BPNL000000000000",
                                                            "bankName": "Dummy Bank",
                                                            "id": "did:web:localhost:BPNL000000000000",
                                                            "type": "BankDetails",
                                                            "accountNumber": "123456789"
                                                        }
                                                    ],
                                                    "proof":
                                                    {
                                                        "proofPurpose": "proofPurpose",
                                                        "verificationMethod": "did:web:localhost:BPNL000000000000#",
                                                        "type": "JsonWebSignature2020",
                                                        "created": "2023-07-19T13:41:54Z",
                                                        "jws": "eyJhbGciOiJFZERTQSJ9..fdqaAsPhQ5xZhQiRvWliDVXX-R9NzCvFXGUAOyQ8yE1hmf_4cvxS7JFuEojjsi3V-n66iiRCUFEXsnv56XPgDA"
                                                    }
                                                }
                            """)
            }) }),
            @ApiResponse(responseCode = "404", description = "Wallet not found with caller BPN", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Wallet not found with caller BPN", value = """
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
                    }) }),
            @ApiResponse(responseCode = "401", description = "The request could not be completed due to a failed authorization.", content = {
                    @Content(examples = {}) }),
            @ApiResponse(responseCode = "403", description = "The request could not be completed due to a forbidden access", content = {
                    @Content(examples = {}) }),
            @ApiResponse(responseCode = "500", description = "Any other internal server error", content = {
                    @Content(examples = {
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
                    }) }),
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
    })
    @RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                  "@context": [
                                    "https://www.w3.org/2018/credentials/v1",
                                    "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
                                    "https://w3id.org/security/suites/jws-2020/v1"
                                  ],
                                  "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                  "type": [
                                    "VerifiableCredential",
                                    "BankDetails"
                                  ],
                                  "issuer": "did:web:localhost:BPNL000000000000",
                                  "expirationDate": "2024-12-31T18:30:00Z",
                                  "issuanceDate": "2023-07-19T09:11:34Z",
                                  "credentialSubject": [
                                    {
                                      "bpn": "BPNL000000000000",
                                      "id": "did:web:localhost:BPNL000000000000",
                                      "type": "BankDetails",
                                      "accountNumber": "123456789",
                                      "bankName":"Dummy Bank"
                                    }
                                  ]
                                }
                    """))
    })
    @Operation(summary = "Issue Verifiable Credential", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of the issuer of the Verifiable Credential must equal BPN of caller)\nIssue a verifiable credential with a given issuer DID", security = { @SecurityRequirement(name = "Authenticate using access_token") })
    public @interface IssueCredentialApiDoc {
    }

}
