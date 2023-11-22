package org.eclipse.tractusx.managedidentitywallets.apidocs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


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
            }),
    })
    @Operation(description = "Permission: **view_wallets** (The BPN of holderIdentifier must equal BPN of caller)\n\n Search verifiable credentials with filter criteria", summary = "Query Verifiable Credentials")
    public @interface GetCredentialsApiDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                   "bpn": "BPNL000000000000"
                                 }
                    """))
    })
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
            @ApiResponse(responseCode = "409", description = "The request could not be completed due to a conflict.", content = {
                    @Content(examples = {
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
            }) })
    @Operation(summary = "Issue a Membership Verifiable Credential with base wallet issuer", description = "Permission: **update_wallets** (The BPN of base wallet must equal BPN of caller)\n\n Issue a verifiable credential by base wallet")
    public @interface IssueMembershipCredentialApiDoc {
    }

}
