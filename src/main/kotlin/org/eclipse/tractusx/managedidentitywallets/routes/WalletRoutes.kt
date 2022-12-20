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
import io.bkbn.kompendium.core.Notarized.notarizedDelete
import io.bkbn.kompendium.core.Notarized.notarizedGet
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.ParameterExample
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.DeleteInfo
import io.bkbn.kompendium.core.metadata.method.GetInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdContexts
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService

import org.jetbrains.exposed.exceptions.ExposedSQLException

import java.time.LocalDateTime

fun Route.walletRoutes(walletService: IWalletService) {

    route("/wallets") {

        notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
            notarizedGet(
                GetInfo<Unit, List<WalletDto>>(
                    summary = "List of wallets",
                    description = "Permission: " +
                        "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_VIEW_WALLETS)}**\n" +
                        "\nRetrieve list of registered wallets",
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "List of wallets",
                    ),
                    canThrow = setOf(forbiddenException, unauthorizedException),
                    tags = setOf("Wallets")
                )
            ) {
                AuthorizationHandler.checkHasRightsToViewWallet(call)
                call.respond(walletService.getAll())
            }
        }

        notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
            notarizedPost(
                PostInfo<Unit, WalletCreateDto, WalletDto>(
                    summary = "Create wallet",
                    description = "Permission: " +
                        "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_CREATE_WALLETS)}**\n" +
                        "\nCreate a wallet and store it ",
                    requestInfo = RequestInfo(
                        description = "wallet to create",
                        examples = walletCreateDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "Wallet was successfully created",
                        examples = walletDtoWithVerKeyExample
                    ),
                    canThrow = setOf(syntacticallyInvalidInputException, conflictException,
                        forbiddenException, unauthorizedException),
                    tags = setOf("Wallets")
                )
            ) {
                AuthorizationHandler.checkHasRightToCreateWallets(call)
                try {
                    val walletToCreate = call.receive<WalletCreateDto>()
                    val createdWallet = walletService.createWallet(walletToCreate)
                    call.respond(HttpStatusCode.Created, createdWallet)
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException(e.message)
                } catch (e: ExposedSQLException) {
                    val isUniqueConstraintError = e.sqlState == "23505"
                    if (isUniqueConstraintError) {
                        throw ConflictException("Wallet with given BPN already exists!")
                    } else {
                        throw UnprocessableEntityException(e.message)
                    }
                }
            }
        }

        route("/self-managed-wallets") {
            notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                notarizedPost(
                    PostInfo<Unit, SelfManagedWalletCreateDto, SelfManagedWalletResultDto>(
                        summary = "Register and Establish Initial Connection with Partners",
                        description = "Permission: " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}**\n" +
                                "\n Register self managed wallet and establish the initial connection with CatenaX. " +
                                "Also issue their membership and BPN credentials",
                        requestInfo = RequestInfo(
                            description = "Register self managed wallet, establish a connection and issue membership and BPN credentials",
                            examples = selfManagedWalletCreateDtoExample
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.Created,
                            description = "The request was able send a connection request to the DID",
                        ),
                        canThrow = setOf(notFoundException, syntacticallyInvalidInputException),
                    )
                ) {
                    val selfManagedWalletCreateDto = call.receive<SelfManagedWalletCreateDto>()
                    AuthorizationHandler.checkHasRightsToUpdateWallet(call, selfManagedWalletCreateDto.bpn)
                    return@notarizedPost call.respond(
                        HttpStatusCode.Created,
                        walletService.registerSelfManagedWalletAndBuildConnection(selfManagedWalletCreateDto)
                    )
                }
            }
        }

        route("/{identifier}") {

            notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                notarizedGet(
                    GetInfo<WalletDtoParameter, WalletDto>(
                        summary = "Retrieve wallet by identifier",
                        description = "Permission: " +
                            "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_VIEW_WALLETS)}** OR " +
                            "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_VIEW_WALLET)}** " +
                                "(The BPN of Wallet to retrieve must equal the BPN of caller)\n" +
                            "\nRetrieve single wallet by identifier, with or without its credentials",
                        parameterExamples = setOf(
                            ParameterExample("identifier", "did", "did:example:0123"),
                            ParameterExample("identifier", "bpn", "bpn123"),
                            ParameterExample("withCredentials", "withCredentials", "false")
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.OK,
                            description = "The wallet",
                            examples = walletDtoExample
                        ),
                        canThrow = setOf(syntacticallyInvalidInputException, notFoundException,
                            forbiddenException, unauthorizedException),
                        tags = setOf("Wallets")
                    )
                ) {
                    val identifier =
                        call.parameters["identifier"] ?: throw BadRequestException("Missing or malformed identifier")
                    var withCredentials = false
                    if (call.request.queryParameters["withCredentials"] != null) {
                        withCredentials = call.request.queryParameters["withCredentials"].toBoolean()
                    }
                    AuthorizationHandler.checkHasRightsToViewWallet(call, identifier)
                    val walletDto: WalletDto = walletService.getWallet(identifier, withCredentials)
                    return@notarizedGet call.respond(HttpStatusCode.OK, walletDto)
                }
            }

            notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                notarizedDelete(
                    DeleteInfo<Unit, SuccessResponse>(
                        summary = "Remove wallet",
                        description = "Permission: " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_DELETE_WALLETS)}**\n" +
                                "\nRemove hosted wallet",
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.OK,
                            description = "Wallet successfully removed!",
                            examples = mapOf("demo" to SuccessResponse("Wallet successfully removed!"))
                        ),
                        canThrow = setOf(notFoundException, syntacticallyInvalidInputException,
                            forbiddenException, unauthorizedException),
                        tags = setOf("Wallets")
                    )
                ) {
                    val identifier =
                        call.parameters["identifier"] ?: return@notarizedDelete call.respond(HttpStatusCode.BadRequest)
                    AuthorizationHandler.checkHasRightToDeleteWallets(call)
                    if (walletService.deleteWallet(identifier)) {
                        return@notarizedDelete call.respond(
                            HttpStatusCode.OK,
                            SuccessResponse("Wallet successfully removed!")
                        )
                    }
                    call.respond(HttpStatusCode.BadRequest, ExceptionResponse("Delete wallet $identifier has failed!"))
                }
            }

            route("/credentials") {
                notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                    notarizedPost(
                        PostInfo<StoreVerifiableCredentialParameter, IssuedVerifiableCredentialRequestDto, SuccessResponse>(
                            summary = "Store Verifiable Credential",
                            description = "Permission: " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}** OR " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLET)}** " +
                                    "(The BPN of wallet to extract credentials from must equal BPN of caller)\n" +
                                "\nStore a verifiable credential in the wallet of the given identifier",
                            parameterExamples = setOf(
                                ParameterExample("identifier", "did", "did:exp:123"),
                                ParameterExample("identifier", "bpn", "BPN123"),
                            ),
                            requestInfo = RequestInfo(
                                description = "The verifiable credential to be stored",
                                examples = issuedVerifiableCredentialRequestDtoExample
                            ),
                            responseInfo = ResponseInfo(
                                status = HttpStatusCode.Created,
                                description = "Success message",
                                examples = mapOf(
                                    "demo" to SuccessResponse(
                                        "Credential with id http://example.edu/credentials/3732" +
                                                "has been successfully stored"
                                    )
                                )
                            ),
                            canThrow = setOf(semanticallyInvalidInputException, notFoundException,
                                forbiddenException, unauthorizedException),
                            tags = setOf("Wallets")
                        )
                    ) {
                        val identifier = call.parameters["identifier"]
                            ?: throw BadRequestException("Missing or malformed identifier")
                        AuthorizationHandler.checkHasRightsToUpdateWallet(call, identifier)

                        try {
                            val verifiableCredential = call.receive<IssuedVerifiableCredentialRequestDto>()
                            walletService.storeCredential(identifier, verifiableCredential)
                            call.respond(
                                HttpStatusCode.Created,
                                SuccessResponse("Credential has been successfully stored")
                            )
                        } catch (e: ExposedSQLException) {
                            val isUniqueConstraintError = e.sqlState == "23505"
                            if (isUniqueConstraintError) {
                                throw ConflictException("Credential already exists!")
                            } else {
                                throw UnprocessableEntityException(e.message)
                            }
                        } catch (e: Exception) {
                            throw e
                        }
                    }
                }
            }

            route("/send-invitation") {
                notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                    notarizedPost(
                        PostInfo<Unit, InvitationRequestDto, Unit>(
                            summary = "Send Connection Request",
                            description = "Permission: " +
                                    "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}**\n" +
                                    "\n Send connection request to internal or external wallets.",
                            requestInfo = RequestInfo(
                                description = "The invitation request",
                                examples = exampleInvitation
                            ),
                            responseInfo = ResponseInfo(
                                status = HttpStatusCode.Accepted,
                                description = "The connection request has been sent to the given DID",
                            ),
                            canThrow = setOf(notFoundException, syntacticallyInvalidInputException),
                        )
                    ) {
                        val identifier = call.parameters["identifier"]
                            ?: throw BadRequestException("Missing or malformed identifier")
                        AuthorizationHandler.checkHasRightsToUpdateWallet(call, identifier)
                        val invitationRequestDto = call.receive<InvitationRequestDto>()
                        walletService.sendInvitation(identifier, invitationRequestDto)
                        return@notarizedPost call.respond(HttpStatusCode.Accepted)
                    }
                }
            }
        }
    }
}


val exampleInvitation = mapOf(
    "demo" to InvitationRequestDto(
        theirPublicDid = "did:sov:example",
        alias = "alias",
        myLabel = "myLabel"
    )
)

val issuedVerifiableCredentialRequestDtoExample = mapOf(
    "demo" to IssuedVerifiableCredentialRequestDto(
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
        credentialStatus = CredentialStatus(
            statusId = "http://example.edu/api/credentials/status/test#3",
            credentialType = "StatusList2021Entry",
            statusPurpose = "revocation",
            index= "3",
            listUrl= "http://example.edu/api/credentials/status/test"
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

val walletDtoWithVerKeyExample = mapOf(
    "demo" to WalletDto(
        "name",
        "bpn",
        "did",
        "verkey",
        LocalDateTime.now(),
        vcs = emptyList<VerifiableCredentialDto>().toMutableList(),
        pendingMembershipIssuance = false
    )
)

val walletDtoExample = mapOf(
    "demo" to WalletDto(
        "name",
        "bpn",
        "did",
        null,
        LocalDateTime.now(),
        vcs = emptyList<VerifiableCredentialDto>().toMutableList(),
        pendingMembershipIssuance = false
    )
)

val walletCreateDtoExample = mapOf(
    "demo" to WalletCreateDto(
        "name",
        "bpn"
    )
)

val selfManagedWalletCreateDtoExample = mapOf(
    "demo" to SelfManagedWalletCreateDto(
        name ="name",
        bpn = "bpn",
        did = "did",
    )
)
