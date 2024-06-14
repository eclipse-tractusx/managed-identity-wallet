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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class IssuersCredentialControllerApiDocs {
    /**
     * The constant API_TAG_VERIFIABLE_CREDENTIAL_ISSUER.
     */
    public static final String API_TAG_VERIFIABLE_CREDENTIAL_ISSUER = "Verifiable Credential - Issuer";
    /**
     * The constant API_TAG_VERIFIABLE_CREDENTIAL_VALIDATION.
     */
    public static final String API_TAG_VERIFIABLE_CREDENTIAL_VALIDATION = "Verifiable Credential - Validation";

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_ISSUER)
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
            @ApiResponse(responseCode = "200", description = "Issuer credential list", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Issuer credential list", value = """
                                     {
                                        "content": [
                                          {
                                            "@context": [
                                              "https://www.w3.org/2018/credentials/v1",
                                              "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
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
                                              "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
                                              "https://w3id.org/security/suites/jws-2020/v1"
                                            ],
                                            "issuer": "did:web:localhost:BPNL000000000000",
                                            "issuanceDate": "2023-07-19T09:11:39Z",
                                            "expirationDate": "2024-12-31T18:30:00Z",
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
                                              "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
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
            }),
    })
    @Operation(description = "Permission: **view_wallets** (The BPN of holderIdentifier must equal BPN of caller)\n\n Search verifiable credentials with filter criteria", summary = "Query Verifiable Credentials", security = { @SecurityRequirement(name = "Authenticate using access_token") })
    public @interface GetCredentialsApiDocs {
    }


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_VALIDATION)
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
                                           "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
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
                                           "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
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
                                           "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
                                           "https://w3id.org/security/suites/jws-2020/v1"
                                         ],
                                         "issuer": "did:web:localhost:BPNL000000000000",
                                         "expirationDate": "2022-12-31T18:30:00Z"
                                       }
                                     }
                                    """),
                            @ExampleObject(name = "Revocable Verifiable credentials with check expiry ", value = """
                                     {
                                        "credentialStatus": "active",
                                        "valid": true,
                                        "validateExpiryDate": true,
                                        "vc": {
                                          "credentialSubject": [
                                            {
                                              "holderIdentifier": "BPNL000000000001",
                                              "allowedVehicleBrands": [
                                                "Audi",
                                                "Abarth",
                                                "Alfa Romeo",
                                                "Chrysler"
                                              ],
                                              "id": "did:web:6e3e-203-129-213-107.ngrok-free.app:BPNL000000000001",
                                              "activityType": "vehicleDismantle",
                                              "type": "DismantlerCredential"
                                            }
                                          ],
                                          "issuanceDate": "2024-01-05T05:42:53Z",
                                          "id": "did:web:6e3e-203-129-213-107.ngrok-free.app:BPNL000000000000#8507aa50-b2a4-4532-8e45-f50e7654b23b",
                                          "proof": {
                                            "proofPurpose": "assertionMethod",
                                            "verificationMethod": "did:web:6e3e-203-129-213-107.ngrok-free.app:BPNL000000000000#a39d8ccf-2a66-488d-bfec-916768082e91",
                                            "type": "JsonWebSignature2020",
                                            "created": "2024-01-05T05:42:53Z",
                                            "jws": "eyJhbGciOiJFZERTQSJ9..15NdxA8L_Iw7Igxevm7YGMAQA-Kt6PMOpix6p0jaYHCtfQnTy3q61SDvsnsltGT6fzM90JOubOuig2WFy-GPDg"
                                          },
                                          "type": [
                                            "VerifiableCredential",
                                            "DismantlerCredential"
                                          ],
                                          "@context": [
                                            "https://www.w3.org/2018/credentials/v1",
                                            "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
                                            "https://w3id.org/security/suites/jws-2020/v1",
                                            "https://w3id.org/vc/status-list/2021/v1"
                                          ],
                                          "issuer": "did:web:6e3e-203-129-213-107.ngrok-free.app:BPNL000000000000",
                                          "credentialStatus": {
                                            "id": "did:web:6e3e-203-129-213-107.ngrok-free.app:BPNL000000000000#0",
                                            "statusPurpose": "revocation",
                                            "statusListIndex": "0",
                                            "statusListCredential": "https://ae96-203-129-213-107.ngrok-free.app/api/v1/revocations/credentials?issuerId=did:web:6e3e-203-129-213-107.ngrok-free.app:BPNL000000000000",
                                            "type": "StatusList2021Entry"
                                          },
                                          "expirationDate": "2024-12-31T18:30:00Z"
                                        }
                                      }
                                    """),
                            @ExampleObject(name = "Verifiable Credentials with invalid signature", value = """
                                     {
                                         "valid": false,
                                         "vc":
                                         {
                                             "@context":
                                             [
                                                 "https://www.w3.org/2018/credentials/v1",
                                                 "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
                                                 "https://w3id.org/security/suites/jws-2020/v1"
                                             ],
                                             "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                             "type":
                                             [
                                                 "VerifiableCredential",
                                                 "BpnCredential"
                                             ],
                                             "issuer": "did:web:localhost:BPNL000000000000",
                                             "expirationDate": "2024-12-31T18:30:00Z",
                                             "issuanceDate": "2023-07-19T09:11:34Z",
                                             "credentialSubject":
                                             [
                                                 {
                                                     "bpn": "BPNL000000000000",
                                                     "id": "did:web:localhost:BPNL000000000000",
                                                     "type": "BpnCredential"
                                                 }
                                             ],
                                             "proof":
                                             {
                                                 "created": "2023-07-19T09:11:39Z",
                                                 "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhf",
                                                 "proofPurpose": "proofPurpose",
                                                 "type": "JsonWebSignature2020",
                                                 "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                             }
                                         }
                                     }
                                    """)
                    })
            }) })
    @Operation(summary = "Validate Verifiable Credentials", description = "Permission: **view_wallets** OR **view_wallet** \n\n Validate Verifiable Credentials", security = { @SecurityRequirement(name = "Authenticate using access_token") })
    @RequestBody(content = {
            @Content(examples = { @ExampleObject(name = "Validate credential in JSON-LD format", value = """
                                {
                                  "@context": [
                                    "https://www.w3.org/2018/credentials/v1",
                                    "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
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
                    """),
                    @ExampleObject(name = "Validate credential in JWT format", value = """
                                        {
                                            "jwt": "eyJraWQiOiJkaWQ6d2ViOmFmODgtMjAzLTEyOS0yMTMtMTA3Lm5ncm9rLWZyZWUuYXBwOkJQTkwwMDAwMDAwMDAwMDAjOGYyZWU5ZDItYTM2Yy00MTM4LWJlMWYtYjZmZWZiNmY4MDI0IiwidHlwIjoiSldUIiwiYWxnIjoiRWREU0EifQ.eyJpc3MiOiJkaWQ6d2ViOmFmODgtMjAzLTEyOS0yMTMtMTA3Lm5ncm9rLWZyZWUuYXBwOkJQTkwwMDAwMDAwMDAwMDAiLCJzdWIiOiJkaWQ6d2ViOmFmODgtMjAzLTEyOS0yMTMtMTA3Lm5ncm9rLWZyZWUuYXBwOkJQTkwwMDAwMDAwMDAwMTEiLCJleHAiOjE3MzU2Njk4MDAsInZjIjp7IkBjb250ZXh0IjpbImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL3YxIiwiaHR0cHM6Ly9jb2Zpbml0eS14LmdpdGh1Yi5pby9zY2hlbWEtcmVnaXN0cnkvdjEuMS9Vc2VDYXNlVkMuanNvbiIsImh0dHBzOi8vdzNpZC5vcmcvc2VjdXJpdHkvc3VpdGVzL2p3cy0yMDIwL3YxIl0sImlkIjoiZGlkOndlYjphZjg4LTIwMy0xMjktMjEzLTEwNy5uZ3Jvay1mcmVlLmFwcDpCUE5MMDAwMDAwMDAwMDAwI2Q4Y2ZjZDBiLWY0NGQtNDVkMC05OGEzLTA4ZDZkNmU5Y2E5NSIsInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJVc2VDYXNlRnJhbWV3b3JrQ29uZGl0aW9uIl0sImlzc3VlciI6ImRpZDp3ZWI6YWY4OC0yMDMtMTI5LTIxMy0xMDcubmdyb2stZnJlZS5hcHA6QlBOTDAwMDAwMDAwMDAwMCIsImNyZWRlbnRpYWxTdWJqZWN0IjpbeyJob2xkZXJJZGVudGlmaWVyIjoiQlBOTDAwMDAwMDAwMDAxMSIsImlkIjoiZGlkOndlYjphZjg4LTIwMy0xMjktMjEzLTEwNy5uZ3Jvay1mcmVlLmFwcDpCUE5MMDAwMDAwMDAwMDExIiwidHlwZSI6IkJlaGF2aW9yVHdpbkNyZWRlbnRpYWwiLCJjb250cmFjdFRlbXBsYXRlIjoiaHR0cHM6Ly9wdWJsaWMuY2F0ZW5hLXgub3JnL2NvbnRyYWN0cy90cmFjZWFiaWx0eS52MS5wZGYiLCJjb250cmFjdFZlcnNpb24iOiIxLjAuMCJ9XSwiY3JlZGVudGlhbFN0YXR1cyI6bnVsbCwiaXNzdWFuY2VEYXRlIjoiMjAyNC0wMi0wOFQxNDowMjo1M1oiLCJleHBpcmF0aW9uRGF0ZSI6IjIwMjQtMTItMzFUMTg6MzA6MDBaIn0sImp0aSI6IjliYWFhMjIzLTAxMjctNDEyZS05NjZhLTA3ZTJmZGU4NGNlNCJ9.X3rkj8Gv4OD5nEaeFG5pSA-dogbcYA91YEPmHiKT4FhAiIr7QAdSEULGXHYOn8-eK0jSDHNdAxNYIK1UwYRsCA"
                                        }
                            """)
            }
            )
    })
    public @interface ValidateVerifiableCredentialApiDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = API_TAG_VERIFIABLE_CREDENTIAL_ISSUER)
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
            @ApiResponse(responseCode = "404", description = "Wallet not found with provided identifier", content = {
                    @Content(examples = {
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
            }),
            @ApiResponse(responseCode = "201", description = "Issuer credential", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Issuer credential", value = """
                                     {
                                       "@context": [
                                         "https://www.w3.org/2018/credentials/v1",
                                         "https://eclipse-tractusx.github.io/tractusx-profiles/cx/context/credentials.context.json",
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
            }) })
    @Operation(summary = "Issue Verifiable Credential", description = "Permission: **update_wallets** (The BPN of the base wallet must equal BPN of caller)\nIssue a verifiable credential with a given issuer DID", security = { @SecurityRequirement(name = "Authenticate using access_token") })
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
    public @interface IssueVerifiableCredentialUsingBaseWalletApiDocs {
    }

    @Parameter(description = "Specifies whether the VC (Verifiable Credential) should be created as a JWT (JSON Web Token). "
            +
            "If set to true, the VC will be generated in JWT format"
            +
            "Setting this parameter to false will result in the VC being created as JSON-LD " +
            "Defaults to false if not specified.", examples = {
            @ExampleObject(name = "Create VC as JWT", value = "true"),
            @ExampleObject(name = "Do not create VC as JWT", value = "false")
    })
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AsJwtParam {
    }

}
