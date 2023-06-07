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
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.service.PresentationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * The type Presentation controller.
 */
@RestController
@RequiredArgsConstructor
public class PresentationController extends BaseController {

    public static final String API_TAG_VERIFIABLE_PRESENTATIONS_GENERATION = "Verifiable Presentations - Generation";
    public static final String API_TAG_VERIFIABLE_PRESENTATIONS_VALIDATION = "Verifiable Presentations - Validation";
    private final PresentationService presentationService;

    /**
     * Create presentation response entity.
     *
     * @param data      the data
     * @param audience  the audience
     * @param asJwt     the as jwt
     * @param principal the principal
     * @return the response entity
     */
    @Tag(name = API_TAG_VERIFIABLE_PRESENTATIONS_GENERATION)
    @Operation(summary = "Create Verifiable Presentation", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of the issuer of the Verifiable Presentation must equal to BPN of caller) \n\n Create a verifiable presentation from a list of verifiable credentials, signed by the holder")
    @PostMapping(path = RestURI.API_PRESENTATIONS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                  "holderIdentifier": "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                  "verifiableCredentials": [
                                    {
                                      "id": "http://example.edu/credentials/333",
                                      "@context": [
                                        "https://www.w3.org/2018/credentials/v1",
                                        "https://www.w3.org/2018/credentials/examples/v1"
                                      ],
                                      "type": [
                                        "University-Degree-Credential", "VerifiableCredential"
                                      ],
                                      "issuer": "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                      "issuanceDate": "2019-06-16T18:56:59Z",
                                      "expirationDate": "2019-06-17T18:56:59Z",
                                      "credentialSubject": [{
                                        "college": "Test-University"
                                      }],
                                      "proof": {
                                        "type": "Ed25519Signature2018",
                                        "created": "2021-11-17T22:20:27Z",
                                        "proofPurpose": "assertionMethod",
                                        "verificationMethod": "did:example:76e12ec712ebc6f1c221ebfeb1f#keys-1",
                                        "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                                      }
                                    }
                                  ]
                                }
                    """))
    })
    public ResponseEntity<Map<String, Object>> createPresentation(@RequestBody Map<String, Object> data,
                                                                  @RequestParam(name = "audience", required = false) String audience,
                                                                  @RequestParam(name = "asJwt", required = false, defaultValue = "false") boolean asJwt, Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presentationService.createPresentation(data, asJwt, audience, getBPNFromToken(principal)));
    }

    /**
     * Validate presentation response entity.
     *
     * @param data                     the data
     * @param audience                 the audience
     * @param asJwt                    the as jwt
     * @param withCredentialExpiryDate the with credential expiry date
     * @return the response entity
     */
    @Tag(name = API_TAG_VERIFIABLE_PRESENTATIONS_VALIDATION)
    @Operation(summary = "Validate Verifiable Presentation", description = "Permission: **view_wallets** OR **view_wallet**  \n\n Validate Verifiable Presentation with all included credentials")
    @PostMapping(path = RestURI.API_PRESENTATIONS_VALIDATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = {

                    @ExampleObject(name = "VP as JWT", value = """
                                                        {
                                                          "vp": "eyJhbGciOiJFZERTQSJ9.eyJzdWIiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwiYXVkIjoic21hcnRTZW5zZSIsImlzcyI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJ2cCI6eyJpZCI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAjMWQ2ODg4N2EtMzY4NC00ZDU0LWFkYjAtMmM4MWJiNjc4NTJiIiwidHlwZSI6WyJWZXJpZmlhYmxlUHJlc2VudGF0aW9uIl0sIkBjb250ZXh0IjpbImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL3YxIl0sInZlcmlmaWFibGVDcmVkZW50aWFsIjp7IkBjb250ZXh0IjpbImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL3YxIl0sInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJCcG5DcmVkZW50aWFsQ1giXSwiaWQiOiJhY2I5NTIyZi1kYjIyLTRmOTAtOTQ3NS1jM2YzNTExZjljZGUiLCJpc3N1ZXIiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwiaXNzdWFuY2VEYXRlIjoiMjAyMy0wNi0wMVQwODo1Nzo1MFoiLCJleHBpcmF0aW9uRGF0ZSI6IjIwMjQtMTItMzFUMTg6MzA6MDBaIiwiY3JlZGVudGlhbFN1YmplY3QiOnsiYnBuIjoiQlBOTDAwMDAwMDAwMDAwMCIsImlkIjoiZGlkOndlYjpsb2NhbGhvc3Q6QlBOTDAwMDAwMDAwMDAwMCIsInR5cGUiOiJCcG5DcmVkZW50aWFsIn0sInByb29mIjp7InByb29mUHVycG9zZSI6InByb29mUHVycG9zZSIsInZlcmlmaWNhdGlvbk1ldGhvZCI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJ0eXBlIjoiRWQyNTUxOVNpZ25hdHVyZTIwMjAiLCJwcm9vZlZhbHVlIjoiejRkdUJmY0NzYVN6aU5lVXc4WUJ5eUZkdlpYVzhlQUs5MjhkeDNQeExqV0N2S3p0Slo5bWh4aEh3ZTVCdVRRUW5KRmtvb01nUUdLREU0OGNpTHJHaHBzUEEiLCJjcmVhdGVkIjoiMjAyMy0wNi0wMVQwODo1Nzo1MFoifX19LCJleHAiOjE2ODU2ODEwNTIsImp0aSI6IjFhYmQxYjAxLTBkZTUtNGY1Ny04ZjBlLWRmNzBhNzNkMjE2NyJ9.Hfm-ANjoeZ8fO-32LPOsQ3-xXSclPUd28p9hvlWyVVB0Mz7n0k-KAHra5kpT0oGrGtdhC1lZ0AitdB_td6VrAQ"
                                                        }
                            """)

                    , @ExampleObject(name = "VP as json-ld", value = """
                                        {
                                          "vp": {
                                            "id": "b9d97cef-758d-4a7c-843d-86f17632b08a",
                                            "type": [
                                              "VerifiablePresentation"
                                            ],
                                            "@context": [
                                              "https://www.w3.org/2018/credentials/v1"
                                            ],
                                            "verifiableCredential": [
                                              {
                                                "issuanceDate": "2023-06-01T08:57:50Z",
                                                "credentialSubject": [
                                                  {
                                                    "bpn": "BPNL000000000000",
                                                    "id": "did:web:localhost:BPNL000000000000",
                                                    "type": "BpnCredential"
                                                  }
                                                ],
                                                "id": "acb9522f-db22-4f90-9475-c3f3511f9cde",
                                                "proof": {
                                                  "proofPurpose": "proofPurpose",
                                                  "verificationMethod": "did:web:localhost:BPNL000000000000",
                                                  "type": "Ed25519Signature2020",
                                                  "proofValue": "z4duBfcCsaSziNeUw8YByyFdvZXW8eAK928dx3PxLjWCvKztJZ9mhxhHwe5BuTQQnJFkooMgQGKDE48ciLrGhpsPA",
                                                  "created": "2023-06-01T08:57:50Z"
                                                },
                                                "type": [
                                                  "VerifiableCredential",
                                                  "BpnCredentialCX"
                                                ],
                                                "@context": [
                                                  "https://www.w3.org/2018/credentials/v1",
                                                  "https://raw.githubusercontent.com/catenax-ng/product-core-schemas/main/businessPartnerData"
                                                ],
                                                "issuer": "did:web:localhost:BPNL000000000000",
                                                "expirationDate": "2024-12-31T18:30:00Z"
                                              }
                                            ]
                                          }
                                        }
                    """)

            })
    })
    public ResponseEntity<Map<String, Object>> validatePresentation(@RequestBody Map<String, Object> data,
                                                                    @Parameter(description = "Audience to validate in VP (Only supported in case of JWT formatted VP)") @RequestParam(name = "audience", required = false) String audience,
                                                                    @Parameter(description = "Pass true in case of VP is in JWT format") @RequestParam(name = "asJwt", required = false, defaultValue = "false") boolean asJwt,
                                                                    @Parameter(description = "Check expiry of VC(Only supported in case of JWT formatted VP)") @RequestParam(name = "withCredentialExpiryDate", required = false, defaultValue = "false") boolean withCredentialExpiryDate
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(presentationService.validatePresentation(data, asJwt, withCredentialExpiryDate, audience));
    }
}
