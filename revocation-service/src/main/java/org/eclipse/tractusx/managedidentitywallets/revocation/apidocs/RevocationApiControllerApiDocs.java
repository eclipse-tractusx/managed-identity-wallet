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

package org.eclipse.tractusx.managedidentitywallets.revocation.apidocs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RevocationApiControllerApiDocs {


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "if credential is revoked",
                            content = @Content(
                                    examples = {
                                            @ExampleObject(description = "if credential is revoked", value = """
                                                    {
                                                        "status":"revoked"
                                                    }
                                                    """),
                                            @ExampleObject(description = "if credential is is active", value = """
                                                    {
                                                        "status":"active"
                                                    }
                                                    """) })),
                    @ApiResponse(
                            responseCode = "401",
                            description = "UnauthorizedException: invalid token",
                            content = @Content()),
                    @ApiResponse(
                            responseCode = "403",
                            description = "ForbiddenException: invalid caller",
                            content = @Content()),
                    @ApiResponse(
                            responseCode = "409",
                            description = "ConflictException: Revocation service error",
                            content =
                            @Content(
                                    examples =
                                    @ExampleObject(),
                                    mediaType = "application/json")),
                    @ApiResponse(
                            responseCode = "500",
                            description = "RevocationServiceException: Internal Server Error",
                            content = @Content())
            })
    @RequestBody(
            content = {
                    @Content(
                            examples =
                            @ExampleObject(
                                    value = """
                                            {
                                                "id": "https://977d-203-129-213-107.ngrok-free.app/api/v1/revocations/credentials/BPNL000000000000/revocation/1#12",
                                                "statusPurpose": "revocation",
                                                "statusListIndex": "12",
                                                "statusListCredential": "https://977d-203-129-213-107.ngrok-free.app/api/v1/revocations/credentials/BPNL000000000000/revocation/1",
                                                "type": "BitstringStatusListEntry"
                                            }
                                            """),
                            mediaType = "application/json")
            })
    @Operation(
            summary = "Verify Revocation status",
            description = "Verify revocation status of Credential")
    public @interface verifyCredentialDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Verifiable credential revoked successfully.",
                            content = @Content()),
                    @ApiResponse(
                            responseCode = "401",
                            description = "UnauthorizedException: invalid token",
                            content = @Content()),
                    @ApiResponse(
                            responseCode = "403",
                            description = "ForbiddenException: invalid caller",
                            content = @Content()),
                    @ApiResponse(
                            responseCode = "409",
                            description = "ConflictException: Revocation service error",
                            content =
                            @Content(
                                    examples =
                                    @ExampleObject(
                                            value = """
                                                    {
                                                        "type": "BitstringStatusListEntry",
                                                        "title": "Revocation service error",
                                                        "status": "409",
                                                        "detail": "Credential already revoked",
                                                        "instance": "/api/v1/revocations/revoke",
                                                        "timestamp": 1707133388128
                                                    }
                                                    """),
                                    mediaType = "application/json")),
                    @ApiResponse(
                            responseCode = "500",
                            description = "RevocationServiceException: Internal Server Error",
                            content = @Content())
            })
    @RequestBody(
            content = {
                    @Content(
                            examples =
                            @ExampleObject(
                                    value = """
                                            {
                                                "id": "https://977d-203-129-213-107.ngrok-free.app/api/v1/revocations/credentials/BPNL000000000000/revocation/1#12",
                                                "statusPurpose": "revocation",
                                                "statusListIndex": "12",
                                                "statusListCredential": "https://977d-203-129-213-107.ngrok-free.app/api/v1/revocations/credentials/BPNL000000000000/revocation/1",
                                                "type": "BitstringStatusListEntry"
                                            }
                                            """),
                            mediaType = "application/json")
            })
    @Operation(
            summary = "Revoke a VerifiableCredential",
            description = "Revoke a VerifiableCredential using the provided Credential Status")
    public @interface revokeCredentialDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Status list credential created/updated successfully.",
                            content = {
                                    @Content(
                                            examples =
                                            @ExampleObject(
                                                    value = """
                                                            {
                                                                "id": "https://977d-203-129-213-107.ngrok-free.app/api/v1/revocations/credentials/BPNL000000000000/revocation/1#17",
                                                                "statusPurpose": "revocation",
                                                                "statusListIndex": "17",
                                                                "statusListCredential": "https://977d-203-129-213-107.ngrok-free.app/api/v1/revocations/credentials/BPNL000000000000/revocation/1",
                                                                "type": "BitstringStatusListEntry"
                                                            }
                                                            """),
                                            mediaType = "application/json")
                            }),
                    @ApiResponse(
                            responseCode = "401",
                            description = "UnauthorizedException: invalid token",
                            content = @Content()),
                    @ApiResponse(
                            responseCode = "403",
                            description = "ForbiddenException: invalid caller",
                            content = @Content()),
                    @ApiResponse(
                            responseCode = "500",
                            description = "RevocationServiceException: Internal Server Error",
                            content = @Content())
            })
    @RequestBody(
            content = {
                    @Content(
                            examples =
                            @ExampleObject(
                                    value = """
                                            {
                                              "purpose": "revocation",
                                              "issuerId": "did:web:localhost:BPNL000000000000"
                                            }
                                            """),
                            mediaType = "application/json")
            })
    @Operation(
            summary = "Create or Update a Status List Credential",
            description = "Create the status list credential if it does not exist, else update it.")
    public @interface StatusEntryApiDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get Status list credential ",
                            content = {
                                    @Content(
                                            examples =
                                            @ExampleObject(
                                                    value = """
                                                            {
                                                                "@context":
                                                                [
                                                                    "https://www.w3.org/2018/credentials/v1",
                                                                    "https://eclipse-tractusx.github.io/schema-registry/w3c/v1.0/BitstringStatusList.json",
                                                                    "https://w3id.org/security/suites/jws-2020/v1"
                                                                ],
                                                                "id": "http://localhost/api/v1/revocations/credentials/BPNL000000000000/revocation/1",
                                                                "type":
                                                                [
                                                                    "VerifiableCredential",
                                                                    "BitstringStatusListCredential"
                                                                ],
                                                                "issuer": "did:web:localhost:BPNL000000000000",
                                                                "issuanceDate": "2024-02-05T09:39:58Z",
                                                                "credentialSubject":
                                                                [
                                                                    {
                                                                        "statusPurpose": "revocation",
                                                                        "id": "http://localhost/api/v1/revocations/credentials/BPNL000000000000/revocation/1",
                                                                        "type": "BitstringStatusList",
                                                                        "encodedList": "H4sIAAAAAAAA/wMAAAAAAAAAAAA="
                                                                    }
                                                                ],
                                                                "proof":
                                                                {
                                                                    "proofPurpose": "assertionMethod",
                                                                    "type": "JsonWebSignature2020",
                                                                    "verificationMethod": "did:web:localhost:BPNL000000000000#ed463e4c-b900-481a-b5d0-9ae439c434ae",
                                                                    "created": "2024-02-05T09:39:58Z",
                                                                    "jws": "eyJhbGciOiJFZERTQSJ9..swX1PLJkSlxB6JMmY4a2uUzR-uszlyLrVdNppoYSx4PTV1LzQrDb0afzp_dvTNUWEYDI57a8iPh78BDjqMjSDQ"
                                                                }
                                                            }
                                                            """),
                                            mediaType = "application/json")
                            }),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Status list credential not found",
                            content = @Content()),
                    @ApiResponse(
                            responseCode = "500",
                            description = "RevocationServiceException: Internal Server Error",
                            content = @Content())
            })
    @Operation(
            summary = "Get status list credential",
            description =
                    "Get status list credential using the provided issuer BPN and status purpose and status list index")
    public @interface GetStatusListCredentialDocs {
    }

    @Parameter(description = "Issuer BPN", example = "BPNL000000000000")
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IssuerBPNPathParamDoc {
    }

    @Parameter(description = "Status Purpose ( Revocation or Suspension)", example = "revocation")
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface StatusPathParamDoc {
    }

    @Parameter(description = "status list index", example = "1")
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IndexPathParamDoc {
    }
}
