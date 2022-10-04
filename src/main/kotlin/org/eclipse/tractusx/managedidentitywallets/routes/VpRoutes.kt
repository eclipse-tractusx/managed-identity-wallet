/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.managedidentitywallets.routes

import io.bkbn.kompendium.auth.Notarized.notarizedAuthenticate
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.ParameterExample
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.eclipse.tractusx.managedidentitywallets.models.*

import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService

fun Route.vpRoutes(walletService: IWalletService) {

    route("/presentations") {
        notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
            notarizedPost(
                PostInfo<VerifiablePresentationIssuanceParameter, VerifiablePresentationRequestDto, VerifiablePresentationDto>(
                    summary = "Create Verifiable Presentation",
                    description = "Permission: " +
                        "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}** OR " +
                        "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLET)}** " +
                            "(The BPN of the issuer of the Verifiable Presentation must equal to BPN of caller)\n" +
                        "\nCreate a verifiable presentation from a list of verifiable credentials, signed by the holder",
                    parameterExamples = setOf(
                        ParameterExample(
                            "withCredentialsValidation",
                            "withCredentialsValidation",
                            "false"
                        ),
                        ParameterExample(
                            "withCredentialsDateValidation",
                            "withCredentialsDateValidation",
                            "false"
                        ),
                        ParameterExample(
                            "withRevocationValidation",
                            "withRevocationValidation",
                            "false"
                        )
                    ),
                    requestInfo = RequestInfo(
                        description = "The verifiable presentation input data",
                        examples = verifiablePresentationRequestDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "The created verifiable presentation",
                        examples = verifiablePresentationResponseDtoExample
                    ),
                    canThrow = setOf(semanticallyInvalidInputException, forbiddenException, unauthorizedException),
                    tags = setOf("VerifiablePresentations")
                )
            ) {
                val verifiableCredentialDto = call.receive<VerifiablePresentationRequestDto>()

                AuthorizationHandler.checkHasRightsToUpdateWallet(call, verifiableCredentialDto.holderIdentifier)

                val withCredentialsValidation = if (call.request.queryParameters["withCredentialsValidation"] != null) {
                    call.request.queryParameters["withCredentialsValidation"].toBoolean()
                } else { true }

                val withCredentialsDateValidation = if (call.request.queryParameters["withCredentialsDateValidation"] != null) {
                    call.request.queryParameters["withCredentialsDateValidation"].toBoolean()
                } else { true }

                val withRevocationValidation = if (call.request.queryParameters["withRevocationValidation"] != null) {
                    call.request.queryParameters["withRevocationValidation"].toBoolean()
                } else { true }

                call.respond(
                    HttpStatusCode.Created,
                    walletService.issuePresentation(
                        verifiableCredentialDto,
                        withCredentialsValidation,
                        withCredentialsDateValidation,
                        withRevocationValidation
                    )
                )
            }
        }

        route("/validation") {
            notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                notarizedPost(
                    PostInfo<WithDateValidation, VerifiablePresentationDto, VerifyResponse>(
                        summary = "Validate Verifiable Presentation",
                        description = "Permission: " +
                            "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_VIEW_WALLETS)}** OR " +
                            "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_VIEW_WALLET)}**\n" +
                            "\nValidate Verifiable Presentation with all included credentials",
                        parameterExamples = setOf(
                            ParameterExample("withDateValidation", "withDateValidation", "false"),
                            ParameterExample("withRevocationValidation", "withRevocationValidation", "false")
                        ),
                        requestInfo = RequestInfo(
                            description = "The verifiable presentation to validate",
                            examples = verifiablePresentationResponseDtoExample
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.OK,
                            description = "The verification value",
                            examples = mapOf("demo" to VerifyResponse(
                                error = null,
                                valid = true,
                                vp = verifiablePresentationResponseDtoExample["demo"]!!
                            ))
                        ),
                        canThrow = setOf(semanticallyInvalidInputException, syntacticallyInvalidInputException,
                            forbiddenException, unauthorizedException),
                        tags = setOf("VerifiablePresentations")
                    )
                ) {
                    AuthorizationHandler.checkHasAnyViewRoles(call)
                    val verifiablePresentation = call.receive<VerifiablePresentationDto>()
                    val withDateValidation = if (call.request.queryParameters["withDateValidation"] != null) {
                        call.request.queryParameters["withDateValidation"].toBoolean()
                    } else { false }
                    val withRevocationValidation = if (call.request.queryParameters["withRevocationValidation"] != null) {
                        call.request.queryParameters["withRevocationValidation"].toBoolean()
                    } else { true }
                    val verifyResponse =  walletService.verifyVerifiablePresentation(
                        vpDto = verifiablePresentation,
                        withDateValidation = withDateValidation,
                        withRevocationValidation = withRevocationValidation
                    )
                    call.respond(HttpStatusCode.OK, verifyResponse)
                }
            }
        }
    }
}

val verifiablePresentationRequestDtoExample = mapOf(
    "demo" to VerifiablePresentationRequestDto(
        holderIdentifier = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        verifiableCredentials = listOf(
            VerifiableCredentialDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/333",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuer = "did:example:76e12ec712ebc6f1c221ebfeb1f",
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
                proof = LdProofDto(
                    type = "Ed25519Signature2018",
                    created = "2021-11-17T22:20:27Z",
                    proofPurpose = "assertionMethod",
                    verificationMethod = "did:example:76e12ec712ebc6f1c221ebfeb1f#keys-1",
                    jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                )
            )
        )
    )
)

val verifiablePresentationResponseDtoExample = mapOf(
    "demo" to VerifiablePresentationDto(
        context = listOf(JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1),
        type =  listOf("VerifiablePresentation"),
        holder = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        verifiableCredential = listOf(
            VerifiableCredentialDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuer = "did:example:76e12ec712ebc6f1c221ebfeb1f",
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
                proof = LdProofDto(
                    type = "Ed25519Signature2018",
                    created = "2021-11-17T22:20:27Z",
                    proofPurpose = "assertionMethod",
                    verificationMethod = "did:example:76e12ec712ebc6f1c221ebfeb1f#key-1",
                    jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                )
            )
        ),
        proof = LdProofDto(
            type = "Ed25519Signature2018",
            created = "2021-11-17T22:20:27Z",
            proofPurpose = "assertionMethod",
            verificationMethod = "did:example:76e12ec712ebc6f1c221ebfeb1f#key-1",
            jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
        )
    )
)
