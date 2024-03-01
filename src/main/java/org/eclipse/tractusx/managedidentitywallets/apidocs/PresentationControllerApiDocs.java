package org.eclipse.tractusx.managedidentitywallets.apidocs;

import io.swagger.v3.oas.annotations.Operation;
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

public class PresentationControllerApiDocs {
    public static final String API_TAG_VERIFIABLE_PRESENTATIONS_GENERATION = "Verifiable Presentations - Generation";
    public static final String API_TAG_VERIFIABLE_PRESENTATIONS_VALIDATION = "Verifiable Presentations - Validation";

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = API_TAG_VERIFIABLE_PRESENTATIONS_GENERATION)
    @Operation(summary = "Create Verifiable Presentation", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of the issuer of the Verifiable Presentation must equal to BPN of caller) \n\n Create a verifiable presentation from a list of verifiable credentials, signed by the holder", security = { @SecurityRequirement(name = "Authenticate using access_token") })
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
            @ApiResponse(responseCode = "200", description = "Verifiable Presentation", content = {
                    @Content(examples = {
                            @ExampleObject(name = "VP as Json-LD", value = """
                                     {
                                         "vp": {
                                            "@context": [
                                             "https://www.w3.org/2018/credentials/v1"
                                           ],
                                           "id": "did:web:localhost:BPNL000000000000#b2e69e47-95f3-48ff-af30-eaaab36431d5",
                                           "type": [
                                             "VerifiablePresentation"
                                           ],
                                           "verifiableCredential": [
                                                {
                                                    "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                                    "@context": [
                                                      "https://www.w3.org/2018/credentials/v1",
                                                      "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                                      "https://w3id.org/security/suites/jws-2020/v1"
                                                    ],
                                                    "type": [
                                                      "VerifiableCredential",
                                                      "BpnCredential"
                                                    ],
                                                    "issuer": "did:web:localhost:BPNL000000000000",
                                                    "expirationDate": "2024-12-31T18:30:00Z",
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
                                                      "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhfDw",
                                                      "proofPurpose": "proofPurpose",
                                                      "type": "JsonWebSignature2020",
                                                      "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                                    }
                                                }
                                           ]
                                         }
                                       }
                                    """),
                            @ExampleObject(name = "VP as JWT", value = """
                                     {
                                       "vp": "eyJraWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidHlwIjoiSldUIiwiYWxnIjoiRWREU0EifQ.eyJzdWIiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwiYXVkIjoic21hcnQiLCJpc3MiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidnAiOnsiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIzM4ZTU2ZTg1LTNkODQtNGEyNS1iZjg1LWFiMjRlYzY4MmMwOSIsInR5cGUiOlsiVmVyaWZpYWJsZVByZXNlbnRhdGlvbiJdLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vY2F0ZW5heC1uZy5naXRodWIuaW8vcHJvZHVjdC1jb3JlLXNjaGVtYXMvYnVzaW5lc3NQYXJ0bmVyRGF0YS5qc29uIiwiaHR0cHM6Ly93M2lkLm9yZy9zZWN1cml0eS9zdWl0ZXMvandzLTIwMjAvdjEiXSwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIkJwbkNyZWRlbnRpYWwiXSwiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwI2Y3M2UzNjMxLWJhODctNGEwMy1iZWEzLWIyODcwMDA1Njg3OSIsImlzc3VlciI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJpc3N1YW5jZURhdGUiOiIyMDIzLTA3LTE5VDA5OjExOjM0WiIsImV4cGlyYXRpb25EYXRlIjoiMjAyNC0xMi0zMVQxODozMDowMFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJicG4iOiJCUE5MMDAwMDAwMDAwMDAwIiwidHlwZSI6IkJwbkNyZWRlbnRpYWwifSwicHJvb2YiOnsicHJvb2ZQdXJwb3NlIjoicHJvb2ZQdXJwb3NlIiwidHlwZSI6Ikpzb25XZWJTaWduYXR1cmUyMDIwIiwidmVyaWZpY2F0aW9uTWV0aG9kIjoiZGlkOndlYjpsb2NhbGhvc3Q6QlBOTDAwMDAwMDAwMDAwMCMiLCJjcmVhdGVkIjoiMjAyMy0wNy0xOVQwOToxMTozOVoiLCJqd3MiOiJleUpoYkdjaU9pSkZaRVJUUVNKOS4uZmRuMnFVODVhdU9sdGRIRExkSEk3c0pWVjFaUGRmdHBpWGRfbmRYTjBkRmdTRFdpSXJTY2REMDN3dHZLTHFfSC1zaFFXZmgyUlllTW1ybEV6QWhmRHcifX19LCJleHAiOjE2ODk4MzQ4MDUsImp0aSI6ImIwODYzOWZiLWQ5MWEtNGUwZS1iNmY4LTYzYjdhMzQ1ZTRhZiJ9.80x0AB-OauefdeZfx1cwhitdVKRvCRFeFzYwU73DL7y4w34vu6BdfHWLBGjkwELxkQEoFfiTPOqtuyqhtsyDBg"
                                     }
                                    """)
                    })
            })
    })
    @RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                     "verifiableCredentials":
                                     [
                                         {
                                            "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                            "@context": [
                                              "https://www.w3.org/2018/credentials/v1",
                                              "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                              "https://w3id.org/security/suites/jws-2020/v1"
                                            ],
                                            "type": [
                                              "VerifiableCredential",
                                              "BpnCredential"
                                            ],
                                            "issuer": "did:web:localhost:BPNL000000000000",
                                            "expirationDate": "2024-12-31T18:30:00Z",
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
                                              "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhfDw",
                                              "proofPurpose": "proofPurpose",
                                              "type": "JsonWebSignature2020",
                                              "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                            }
                                          }
                                     ]
                                 }
                    """))
    })
    public @interface PostVerifiablePresentationApiDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = API_TAG_VERIFIABLE_PRESENTATIONS_VALIDATION)
    @Operation(summary = "Validate Verifiable Presentation", description = "Permission: **view_wallets** OR **view_wallet**  \n\n Validate Verifiable Presentation with all included credentials", security = { @SecurityRequirement(name = "Authenticate using access_token") })
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
                            @ExampleObject(name = "Validation of VP in form of JSON-LD is not supported", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Validation of VP in form of JSON-LD is not supported",
                                      "status": 400,
                                      "detail": "Validation of VP in form of JSON-LD is not supported",
                                      "instance": "/api/presentations/validation",
                                      "properties": {
                                        "timestamp": 1689835085703
                                      }
                                    }
                                    """),
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
                    }) }),
            @ApiResponse(responseCode = "200", description = "Verifiable presentation validate", content = {
                    @Content(examples = {
                            @ExampleObject(name = "VP as JWT", value = """
                                     {
                                       "valid": true,
                                       "validateJWTExpiryDate": true,
                                       "validateAudience": true,
                                       "vp": "eyJraWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidHlwIjoiSldUIiwiYWxnIjoiRWREU0EifQ.eyJzdWIiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwiYXVkIjoic21hcnQiLCJpc3MiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidnAiOnsiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIzM4ZTU2ZTg1LTNkODQtNGEyNS1iZjg1LWFiMjRlYzY4MmMwOSIsInR5cGUiOlsiVmVyaWZpYWJsZVByZXNlbnRhdGlvbiJdLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vY2F0ZW5heC1uZy5naXRodWIuaW8vcHJvZHVjdC1jb3JlLXNjaGVtYXMvYnVzaW5lc3NQYXJ0bmVyRGF0YS5qc29uIiwiaHR0cHM6Ly93M2lkLm9yZy9zZWN1cml0eS9zdWl0ZXMvandzLTIwMjAvdjEiXSwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIkJwbkNyZWRlbnRpYWwiXSwiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwI2Y3M2UzNjMxLWJhODctNGEwMy1iZWEzLWIyODcwMDA1Njg3OSIsImlzc3VlciI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJpc3N1YW5jZURhdGUiOiIyMDIzLTA3LTE5VDA5OjExOjM0WiIsImV4cGlyYXRpb25EYXRlIjoiMjAyNC0xMi0zMVQxODozMDowMFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJicG4iOiJCUE5MMDAwMDAwMDAwMDAwIiwidHlwZSI6IkJwbkNyZWRlbnRpYWwifSwicHJvb2YiOnsicHJvb2ZQdXJwb3NlIjoicHJvb2ZQdXJwb3NlIiwidHlwZSI6Ikpzb25XZWJTaWduYXR1cmUyMDIwIiwidmVyaWZpY2F0aW9uTWV0aG9kIjoiZGlkOndlYjpsb2NhbGhvc3Q6QlBOTDAwMDAwMDAwMDAwMCMiLCJjcmVhdGVkIjoiMjAyMy0wNy0xOVQwOToxMTozOVoiLCJqd3MiOiJleUpoYkdjaU9pSkZaRVJUUVNKOS4uZmRuMnFVODVhdU9sdGRIRExkSEk3c0pWVjFaUGRmdHBpWGRfbmRYTjBkRmdTRFdpSXJTY2REMDN3dHZLTHFfSC1zaFFXZmgyUlllTW1ybEV6QWhmRHcifX19LCJleHAiOjE2ODk4MzQ4MDUsImp0aSI6ImIwODYzOWZiLWQ5MWEtNGUwZS1iNmY4LTYzYjdhMzQ1ZTRhZiJ9.80x0AB-OauefdeZfx1cwhitdVKRvCRFeFzYwU73DL7y4w34vu6BdfHWLBGjkwELxkQEoFfiTPOqtuyqhtsyDBg",
                                       "validateExpiryDate": true
                                     }
                                    """)
                    })
            })
    })
    @RequestBody(content = {
            @Content(examples = {

                    @ExampleObject(name = "VP as JWT", value = """
                                                        {
                                                          "vp": "eyJraWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidHlwIjoiSldUIiwiYWxnIjoiRWREU0EifQ.eyJzdWIiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwiYXVkIjoic21hcnQiLCJpc3MiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidnAiOnsiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIzZmZWRmYWRmLWYxNTItNGNjZS05MWQ1LWNjMjhhNzhlMTExMyIsInR5cGUiOlsiVmVyaWZpYWJsZVByZXNlbnRhdGlvbiJdLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vY2F0ZW5heC1uZy5naXRodWIuaW8vcHJvZHVjdC1jb3JlLXNjaGVtYXMvYnVzaW5lc3NQYXJ0bmVyRGF0YS5qc29uIiwiaHR0cHM6Ly93M2lkLm9yZy9zZWN1cml0eS9zdWl0ZXMvandzLTIwMjAvdjEiXSwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIkJwbkNyZWRlbnRpYWwiXSwiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwI2Y3M2UzNjMxLWJhODctNGEwMy1iZWEzLWIyODcwMDA1Njg3OSIsImlzc3VlciI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJpc3N1YW5jZURhdGUiOiIyMDIzLTA3LTE5VDA5OjExOjM0WiIsImV4cGlyYXRpb25EYXRlIjoiMjAyNC0xMi0zMVQxODozMDowMFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJicG4iOiJCUE5MMDAwMDAwMDAwMDAwIiwidHlwZSI6IkJwbkNyZWRlbnRpYWwifSwicHJvb2YiOnsicHJvb2ZQdXJwb3NlIjoicHJvb2ZQdXJwb3NlIiwidHlwZSI6Ikpzb25XZWJTaWduYXR1cmUyMDIwIiwidmVyaWZpY2F0aW9uTWV0aG9kIjoiZGlkOndlYjpsb2NhbGhvc3Q6QlBOTDAwMDAwMDAwMDAwMCMiLCJjcmVhdGVkIjoiMjAyMy0wNy0xOVQwOToxMTozOVoiLCJqd3MiOiJleUpoYkdjaU9pSkZaRVJUUVNKOS4uZmRuMnFVODVhdU9sdGRIRExkSEk3c0pWVjFaUGRmdHBpWGRfbmRYTjBkRmdTRFdpSXJTY2REMDN3dHZLTHFfSC1zaFFXZmgyUlllTW1ybEV6QWhmRHcifX19LCJleHAiOjE2ODk3NTg1NDQsImp0aSI6IjdlNWE4MzQ4LTgwZjUtNGIzMS1iMDNlLTBiOTJmNzc4ZTVjZiJ9.c7FS-CLwm3vxfO9847M5sqcVxv3QbwwSmSsFWcGif7MOesjt1pdnARlQ4pvHzgsFj1UqBEvHwZQvyYyPCQg_Cw"
                                                        }
                            """)

                    ,
                    @ExampleObject(name = "VP as json-ld", value = """
                                                {
                                                    "vp":
                                                    {
                                                        "id": "b9d97cef-758d-4a7c-843d-86f17632b08a",
                                                        "type":
                                                        [
                                                            "VerifiablePresentation"
                                                        ],
                                                        "@context":
                                                        [
                                                            "https://www.w3.org/2018/credentials/v1"
                                                        ],
                                                        "verifiableCredential":
                                                        [
                                                            {
                                                              "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                                              "@context": [
                                                                "https://www.w3.org/2018/credentials/v1",
                                                                "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                                                "https://w3id.org/security/suites/jws-2020/v1"
                                                              ],
                                                              "type": [
                                                                "VerifiableCredential",
                                                                "BpnCredential"
                                                              ],
                                                              "issuer": "did:web:localhost:BPNL000000000000",
                                                              "expirationDate": "2024-12-31T18:30:00Z",
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
                                                                "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhfDw",
                                                                "proofPurpose": "proofPurpose",
                                                                "type": "JsonWebSignature2020",
                                                                "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                                              }
                                                            }
                                                        ]
                                                    }
                                                }
                            """)

            })
    })
    public @interface PostVerifiablePresentationValidationApiDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = API_TAG_VERIFIABLE_PRESENTATIONS_GENERATION)
    @Operation(summary = "Create Verifiable Presentation", description = "Create a verifiable presentation for the verifiable credential types listed in STS token", security = { @SecurityRequirement(name = "sts_token") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "401", description = "The request could not be completed due to a failed authorization.", content = {
                    @Content(examples = {}) }),
            @ApiResponse(responseCode = "403", description = "The request could not be completed due to a forbidden scope value", content = {
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
            @ApiResponse(responseCode = "404", description = "One or more of the requested verifiable credential types were not found", content = {
                    @Content(examples = {
                            @ExampleObject(name = "One or more of the requested verifiable credential types were not found", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Error Title",
                                      "status": 404,
                                      "detail": "Verifiable credential types that were not found",
                                      "instance": "API endpoint",
                                      "properties": {
                                        "timestamp": 1689762476720
                                      }
                                    }
                                    """)
                    }) }),
            @ApiResponse(responseCode = "200", description = "Verifiable Presentation", content = {
                    @Content(examples = {
                            @ExampleObject(name = "VP as Json-LD", value = """
                                     {
                                         "vp": {
                                            "@context": [
                                             "https://www.w3.org/2018/credentials/v1"
                                           ],
                                           "id": "did:web:localhost:BPNL000000000000#b2e69e47-95f3-48ff-af30-eaaab36431d5",
                                           "type": [
                                             "VerifiablePresentation"
                                           ],
                                           "verifiableCredential": [
                                                {
                                                    "id": "did:web:localhost:BPNL000000000000#f73e3631-ba87-4a03-bea3-b28700056879",
                                                    "@context": [
                                                      "https://www.w3.org/2018/credentials/v1",
                                                      "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json",
                                                      "https://w3id.org/security/suites/jws-2020/v1"
                                                    ],
                                                    "type": [
                                                      "VerifiableCredential",
                                                      "BpnCredential"
                                                    ],
                                                    "issuer": "did:web:localhost:BPNL000000000000",
                                                    "expirationDate": "2024-12-31T18:30:00Z",
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
                                                      "jws": "eyJhbGciOiJFZERTQSJ9..fdn2qU85auOltdHDLdHI7sJVV1ZPdftpiXd_ndXN0dFgSDWiIrScdD03wtvKLq_H-shQWfh2RYeMmrlEzAhfDw",
                                                      "proofPurpose": "proofPurpose",
                                                      "type": "JsonWebSignature2020",
                                                      "verificationMethod": "did:web:localhost:BPNL000000000000#"
                                                    }
                                                }
                                           ]
                                         }
                                       }
                                    """),
                            @ExampleObject(name = "VP as JWT", value = """
                                     {
                                       "vp": "eyJraWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidHlwIjoiSldUIiwiYWxnIjoiRWREU0EifQ.eyJzdWIiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwiYXVkIjoic21hcnQiLCJpc3MiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidnAiOnsiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIzM4ZTU2ZTg1LTNkODQtNGEyNS1iZjg1LWFiMjRlYzY4MmMwOSIsInR5cGUiOlsiVmVyaWZpYWJsZVByZXNlbnRhdGlvbiJdLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vY2F0ZW5heC1uZy5naXRodWIuaW8vcHJvZHVjdC1jb3JlLXNjaGVtYXMvYnVzaW5lc3NQYXJ0bmVyRGF0YS5qc29uIiwiaHR0cHM6Ly93M2lkLm9yZy9zZWN1cml0eS9zdWl0ZXMvandzLTIwMjAvdjEiXSwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIkJwbkNyZWRlbnRpYWwiXSwiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwI2Y3M2UzNjMxLWJhODctNGEwMy1iZWEzLWIyODcwMDA1Njg3OSIsImlzc3VlciI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJpc3N1YW5jZURhdGUiOiIyMDIzLTA3LTE5VDA5OjExOjM0WiIsImV4cGlyYXRpb25EYXRlIjoiMjAyNC0xMi0zMVQxODozMDowMFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJicG4iOiJCUE5MMDAwMDAwMDAwMDAwIiwidHlwZSI6IkJwbkNyZWRlbnRpYWwifSwicHJvb2YiOnsicHJvb2ZQdXJwb3NlIjoicHJvb2ZQdXJwb3NlIiwidHlwZSI6Ikpzb25XZWJTaWduYXR1cmUyMDIwIiwidmVyaWZpY2F0aW9uTWV0aG9kIjoiZGlkOndlYjpsb2NhbGhvc3Q6QlBOTDAwMDAwMDAwMDAwMCMiLCJjcmVhdGVkIjoiMjAyMy0wNy0xOVQwOToxMTozOVoiLCJqd3MiOiJleUpoYkdjaU9pSkZaRVJUUVNKOS4uZmRuMnFVODVhdU9sdGRIRExkSEk3c0pWVjFaUGRmdHBpWGRfbmRYTjBkRmdTRFdpSXJTY2REMDN3dHZLTHFfSC1zaFFXZmgyUlllTW1ybEV6QWhmRHcifX19LCJleHAiOjE2ODk4MzQ4MDUsImp0aSI6ImIwODYzOWZiLWQ5MWEtNGUwZS1iNmY4LTYzYjdhMzQ1ZTRhZiJ9.80x0AB-OauefdeZfx1cwhitdVKRvCRFeFzYwU73DL7y4w34vu6BdfHWLBGjkwELxkQEoFfiTPOqtuyqhtsyDBg"
                                     }
                                    """)
                    })
            })
    })
    public @interface GetVerifiablePresentationIATPApiDocs {
    }

}
