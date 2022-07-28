/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the CatenaX (ng) GitHub Organisation
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
import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
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

import kotlinx.serialization.Serializable

import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdContexts
import org.eclipse.tractusx.managedidentitywallets.services.BusinessPartnerDataService
import org.eclipse.tractusx.managedidentitywallets.services.WalletService
import org.eclipse.tractusx.managedidentitywallets.plugins.AuthConstants

import org.jetbrains.exposed.exceptions.ExposedSQLException

import java.time.LocalDateTime

fun Route.walletRoutes(walletService: WalletService, businessPartnerDataService: BusinessPartnerDataService) {

    route("/wallets") {

        notarizedAuthenticate(AuthConstants.JWT_AUTH_VIEW) {
            notarizedGet(
                GetInfo<Unit, List<WalletDto>>(
                    summary = "List of wallets",
                    description = "Retrieve list of registered wallets",
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "List of wallets",
                    ),
                    tags = setOf("Wallets"),
                    securitySchemes = setOf(AuthConstants.JWT_AUTH_VIEW.name)
                )
            ) {
                call.respond(walletService.getAll())
            }
        }

        notarizedAuthenticate(AuthConstants.JWT_AUTH_CREATE) {
            notarizedPost(
                PostInfo<Unit, WalletCreateDto, WalletDto>(
                    summary = "Create wallet",
                    description = "Create a wallet and store it",
                    requestInfo = RequestInfo(
                        description = "wallet to create",
                        examples = walletCreateDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "Wallet was successfully created",
                        examples = walletDtoWithVerKeyExample
                    ),
                    canThrow = setOf(syntacticallyInvalidInputException, conflictException),
                    tags = setOf("Wallets"),
                    securitySchemes = setOf(AuthConstants.JWT_AUTH_CREATE.name)
                )
            ) {
                try {
                    val walletToCreate = call.receive<WalletCreateDto>()
                    val createdWallet = walletService.createWallet(walletToCreate)
                    if (!walletService.isCatenaXWallet(createdWallet.bpn)) {
                        // TODO: notify if issue credentials failed
                        // Issue and store credentials async
                        businessPartnerDataService.issueAndStoreCatenaXCredentialsAsync(
                            createdWallet.bpn,
                            JsonLdTypes.BPN_TYPE,
                            null
                        )
                        businessPartnerDataService.issueAndStoreCatenaXCredentialsAsync(
                            createdWallet.bpn,
                            JsonLdTypes.MEMBERSHIP_TYPE,
                            null
                        )
                    }
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

        route("/{identifier}") {

            notarizedAuthenticate(AuthConstants.JWT_AUTH_VIEW, AuthConstants.JWT_AUTH_VIEW_SINGLE) {
                notarizedGet(
                    GetInfo<WalletDtoParameter, WalletDto>(
                        summary = "Retrieve wallet by identifier",
                        description = "Retrieve single wallet by identifier, with or without its credentials",
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
                        canThrow = setOf(syntacticallyInvalidInputException, notFoundException),
                        tags = setOf("Wallets"),
                        securitySchemes = setOf(AuthConstants.JWT_AUTH_VIEW.name,
                            AuthConstants.JWT_AUTH_VIEW_SINGLE.name)
                    )
                ) {
                    val identifier =
                        call.parameters["identifier"] ?: throw BadRequestException("Missing or malformed identifier")
                    var withCredentials = false
                    if (call.request.queryParameters["withCredentials"] != null) {
                        withCredentials = call.request.queryParameters["withCredentials"].toBoolean()
                    }
                    val authorizationResponse = AuthorizationHandler.hasRightsToViewOwnWallet(call, identifier)
                    if (!authorizationResponse.valid) {
                        return@notarizedGet call.respondText(authorizationResponse.errorMsg!!,
                            ContentType.Text.Plain, HttpStatusCode.Unauthorized)
                    }
                    val walletDto: WalletDto = walletService.getWallet(identifier, withCredentials)
                    return@notarizedGet call.respond(HttpStatusCode.OK, walletDto)
                }
            }            

            notarizedAuthenticate(AuthConstants.JWT_AUTH_DELETE) {
                notarizedDelete(
                    DeleteInfo<Unit, SuccessResponse>(
                        summary = "Remove wallet",
                        description = "Remove hosted wallet",
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.OK,
                            description = "Wallet successfully removed!",
                            examples = mapOf("demo" to SuccessResponse("Wallet successfully removed!"))
                        ),
                        canThrow = setOf(notFoundException, syntacticallyInvalidInputException),
                        tags = setOf("Wallets"),
                        securitySchemes = setOf(AuthConstants.JWT_AUTH_DELETE.name)
                    )
                ) {
                    val identifier =
                        call.parameters["identifier"] ?: return@notarizedDelete call.respond(HttpStatusCode.BadRequest)
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

                notarizedAuthenticate(AuthConstants.JWT_AUTH_UPDATE, AuthConstants.JWT_AUTH_UPDATE_SINGLE) {
                    notarizedPost(
                        PostInfo<StoreVerifiableCredentialParameter, IssuedVerifiableCredentialRequestDto, SuccessResponse>(
                            summary = "Store Verifiable Credential",
                            description = "Store a verifiable credential in the wallet of the given identifier",
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
                            canThrow = setOf(semanticallyInvalidInputException, notFoundException),
                            tags = setOf("Wallets"),
                            securitySchemes = setOf(AuthConstants.JWT_AUTH_UPDATE.name,
                                AuthConstants.JWT_AUTH_UPDATE_SINGLE.name)
                        )
                    ) {
                        val identifier = call.parameters["identifier"]
                            ?: throw BadRequestException("Missing or malformed identifier")
                        val authorizationResponse = AuthorizationHandler.hasRightsToStoreCredential(call, identifier)
                        if (!authorizationResponse.valid) {
                            return@notarizedPost call.respondText(authorizationResponse.errorMsg!!,
                                ContentType.Text.Plain, HttpStatusCode.Unauthorized)
                        }
                        try {
                            val verifiableCredential = call.receive<IssuedVerifiableCredentialRequestDto>()
                            walletService.storeCredential(identifier, verifiableCredential)
                            call.respond(HttpStatusCode.Created, SuccessResponse("Credential has been successfully stored"))
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

            route("/public") {
                notarizedAuthenticate(AuthConstants.JWT_AUTH_UPDATE) {
                    notarizedPost(
                        PostInfo<StoreVerifiableCredentialParameter, VerKeyDto, SuccessResponse>(
                            summary = "Register on Public Chain",
                            description = "Register wallet DID on the public chain, endpoint only available for the base wallet",
                            parameterExamples = setOf(
                                ParameterExample("identifier", "did", "did:exp:123"),
                                ParameterExample("identifier", "bpn", "BPN123"),
                            ),
                            requestInfo = RequestInfo(
                                description = "VerKey",
                                examples = verKeyExample
                            ),
                            responseInfo = ResponseInfo(
                                status = HttpStatusCode.Created,
                                description = "Success message",
                                examples = mapOf(
                                    "demo" to SuccessResponse(
                                        "Wallet has been successfully registered on chain"
                                    )
                                )
                            ),
                            canThrow = setOf(semanticallyInvalidInputException, notFoundException),
                            tags = setOf("Wallets"),
                            securitySchemes = setOf(AuthConstants.JWT_AUTH_UPDATE.name)
                        )
                    ) {
                        try {
                            val identifier = call.parameters["identifier"]
                                ?: throw BadRequestException("Missing or malformed identifier")
                            val walletDto: WalletDto = walletService.getWallet(identifier)
                            if (!walletService.isCatenaXWallet(walletDto.bpn)) {
                                throw NotFoundException("Registering endpoint is not available for any other wallet but the base wallet")
                            }
                            val verKeyDto = call.receive<VerKeyDto>()
                            if (walletService.registerBaseWallet(verKeyDto.verKey)) {
                                call.respond(HttpStatusCode.Created, SuccessResponse("Wallet has been successfully registered on chain"))
                            } else {
                                throw Exception("Could not register base wallet on public chain, manual intervention needed!")
                            }
                        } catch (e: Exception) {
                            throw e
                        }
                    }
                }
            }

        }
    }
}

// for documentation
@Serializable
data class StoreVerifiableCredentialParameter(
    @Param(type = ParamType.PATH)
    @Field(
        description = "The DID or BPN of the credential holder. The DID must match to the id of the credential subject if present.",
        name = "identifier"
    )
    val identifier: String
)

@Serializable
data class WalletDtoParameter(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of the Wallet", name = "identifier")
    val identifier: String,
    @Param(type = ParamType.QUERY)
    @Field(
        description = "Flag whether all stored credentials of the wallet should be included in the response",
        name = "withCredentials"
    )
    val withCredentials: Boolean
)

@Serializable
data class VerKeyDto(
    val verKey: String
)
val verKeyExample = mapOf(
    "demo" to VerKeyDto("VERIFICATION_KEY_AFTER_CREATION")
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
        proof = LdProofDto(
            type = "Ed25519Signature2018",
            created = "2021-11-17T22:20:27Z",
            proofPurpose = "assertionMethod",
            verificationMethod = "did:example:76e12ec712ebc6f1c221ebfeb1f#keys-1",
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
        emptyList<VerifiableCredentialDto>().toMutableList()
    )
)

val walletDtoExample = mapOf(
    "demo" to WalletDto(
        "name",
        "bpn",
        "did",
        null,
        LocalDateTime.now(),
        emptyList<VerifiableCredentialDto>().toMutableList()
    )
)

val walletCreateDtoExample = mapOf(
    "demo" to WalletCreateDto(
        "name",
        "bpn"
    )
)
