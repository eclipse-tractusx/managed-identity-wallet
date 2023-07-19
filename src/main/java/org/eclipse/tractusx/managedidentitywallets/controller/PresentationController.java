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
                                                          "vp": "eyJraWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidHlwIjoiSldUIiwiYWxnIjoiRWREU0EifQ.eyJzdWIiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwiYXVkIjoic21hcnQiLCJpc3MiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIiwidnAiOnsiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwIzZmZWRmYWRmLWYxNTItNGNjZS05MWQ1LWNjMjhhNzhlMTExMyIsInR5cGUiOlsiVmVyaWZpYWJsZVByZXNlbnRhdGlvbiJdLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vY2F0ZW5heC1uZy5naXRodWIuaW8vcHJvZHVjdC1jb3JlLXNjaGVtYXMvYnVzaW5lc3NQYXJ0bmVyRGF0YS5qc29uIiwiaHR0cHM6Ly93M2lkLm9yZy9zZWN1cml0eS9zdWl0ZXMvandzLTIwMjAvdjEiXSwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIkJwbkNyZWRlbnRpYWwiXSwiaWQiOiJkaWQ6d2ViOmxvY2FsaG9zdDpCUE5MMDAwMDAwMDAwMDAwI2Y3M2UzNjMxLWJhODctNGEwMy1iZWEzLWIyODcwMDA1Njg3OSIsImlzc3VlciI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJpc3N1YW5jZURhdGUiOiIyMDIzLTA3LTE5VDA5OjExOjM0WiIsImV4cGlyYXRpb25EYXRlIjoiMjAyNC0xMi0zMVQxODozMDowMFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6ImRpZDp3ZWI6bG9jYWxob3N0OkJQTkwwMDAwMDAwMDAwMDAiLCJicG4iOiJCUE5MMDAwMDAwMDAwMDAwIiwidHlwZSI6IkJwbkNyZWRlbnRpYWwifSwicHJvb2YiOnsicHJvb2ZQdXJwb3NlIjoicHJvb2ZQdXJwb3NlIiwidHlwZSI6Ikpzb25XZWJTaWduYXR1cmUyMDIwIiwidmVyaWZpY2F0aW9uTWV0aG9kIjoiZGlkOndlYjpsb2NhbGhvc3Q6QlBOTDAwMDAwMDAwMDAwMCMiLCJjcmVhdGVkIjoiMjAyMy0wNy0xOVQwOToxMTozOVoiLCJqd3MiOiJleUpoYkdjaU9pSkZaRVJUUVNKOS4uZmRuMnFVODVhdU9sdGRIRExkSEk3c0pWVjFaUGRmdHBpWGRfbmRYTjBkRmdTRFdpSXJTY2REMDN3dHZLTHFfSC1zaFFXZmgyUlllTW1ybEV6QWhmRHcifX19LCJleHAiOjE2ODk3NTg1NDQsImp0aSI6IjdlNWE4MzQ4LTgwZjUtNGIzMS1iMDNlLTBiOTJmNzc4ZTVjZiJ9.c7FS-CLwm3vxfO9847M5sqcVxv3QbwwSmSsFWcGif7MOesjt1pdnARlQ4pvHzgsFj1UqBEvHwZQvyYyPCQg_Cw"
                                                        }
                            """)

                    , @ExampleObject(name = "VP as json-ld", value = """
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
    public ResponseEntity<Map<String, Object>> validatePresentation(@RequestBody Map<String, Object> data,
                                                                    @Parameter(description = "Audience to validate in VP (Only supported in case of JWT formatted VP)") @RequestParam(name = "audience", required = false) String audience,
                                                                    @Parameter(description = "Pass true in case of VP is in JWT format") @RequestParam(name = "asJwt", required = false, defaultValue = "false") boolean asJwt,
                                                                    @Parameter(description = "Check expiry of VC(Only supported in case of JWT formatted VP)") @RequestParam(name = "withCredentialExpiryDate", required = false, defaultValue = "false") boolean withCredentialExpiryDate
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(presentationService.validatePresentation(data, asJwt, withCredentialExpiryDate, audience));
    }
}
