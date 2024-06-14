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


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RevocationAPIDoc {

    @Parameter(description = "Specifies whether the VC (Verifiable Credential) should revocable. The default value will be true")
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Revocable {

    }

    @Tag(name = "Verifiable Credential - Revoke")
    @ApiResponse(responseCode = "201", description = "Issuer credential", content = {
            @Content(examples = {
                    @ExampleObject(name = "Success response", value = """
                            {
                                "message": "Credential has been revoked"
                            }
                            """)
            })
    })
    @ApiResponse(responseCode = "404", description = "Wallet not found with credential issuer", content = { @Content(examples = {
            @ExampleObject(name = "Wallet not found with credential issuer", value = """
                    {
                      "type": "about:blank",
                      "title": "Wallet not found for identifier web:did:localhost:BPN",
                      "status": 404,
                      "detail": "Error Details",
                      "instance": "API endpoint",
                      "properties": {
                        "timestamp": 1689762476720
                      }
                    }
                    """)
    }) })
    @ApiResponse(responseCode = "500", description = "Any other internal server error", content = { @Content(examples = {
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
    }) })
    @ApiResponse(responseCode = "409", description = "Credential is already revoked", content = { @Content(examples = {
            @ExampleObject(name = "Credential is already revoked", value = """
                    {
                       "type": "about:blank",
                       "title": "Revocation service error",
                       "status": 409,
                       "detail": "RevocationProblem: Credential already revoked",
                       "instance": "/api/credentials/revoke",
                       "properties": {
                         "timestamp": "1704438069232",
                         "type": "about:blank",
                         "title": "Revocation service error",
                         "status": "409",
                         "detail": "Credential already revoked",
                         "instance": "/api/v1/revocations/revoke"
                       }
                     }
                    """)
    }) })
    @ApiResponse(responseCode = "403", description = "The request could not be completed due to a forbidden access", content = { @Content(examples = {}) })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                    "issuanceDate": "2023-12-26T10:58:02Z",
                                    "credentialSubject":
                                    [
                                        {
                                            "holderIdentifier": "BPNL000000000002",
                                            "id": "did:web:localhost:BPNL000000000002",
                                            "type": "SummaryCredential",
                                            "items":
                                            [
                                                "BpnCredential"
                                            ],
                                            "contractTemplate": "https://public.catena-x.org/contracts/"
                                        }
                                    ],
                                    "id": "did:web:localhost:BPNL000000000000#6b680abe-8869-435f-9d83-ad5ac336b8da",
                                    "proof":
                                    {
                                        "proofPurpose": "assertionMethod",
                                        "type": "JsonWebSignature2020",
                                        "verificationMethod": "did:web:localhost:BPNL000000000000#a8233b68-f41e-4f14-8dff-fe16a63e0b19",
                                        "created": "2023-12-26T10:58:02Z",
                                        "jws": "eyJhbGciOiJFZERTQSJ9..uFqnCMbcOJneZDl7mCg8PeUjhWdUN53C8dB1E3EoWx7_hVgxsU8L7WkRYxvxIEa_DddViOoKs8E95ymYK081Aw"
                                    },
                                    "type":
                                    [
                                        "VerifiableCredential",
                                        "SummaryCredential"
                                    ],
                                    "@context":
                                    [
                                        "https://www.w3.org/2018/credentials/v1",
                                        "https://cofinity-x.github.io/schema-registry/v1.1/SummaryVC.json",
                                        "https://w3id.org/security/suites/jws-2020/v1",
                                        "https://w3id.org/vc/status-list/2021/v1"
                                    ],
                                    "issuer": "did:web:localhost:BPNL000000000000",
                                    "expirationDate": "2024-12-31T18:30:00Z",
                                    "credentialStatus":
                                    {
                                        "id": "did:web:localhost:BPNL000000000000#1",
                                        "statusPurpose": "revocation",
                                        "statusListIndex": "1",
                                        "statusListCredential": "https://7337-203-129-213-107.ngrok-free.app/api/v1/revocations/credentials?issuerId=did:web:localhost:BPNL000000000000",
                                        "type": "BitstringStatusListEntry"
                                    }
                                }
                    """))
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RevokeCredentialDoc {

    }
}
