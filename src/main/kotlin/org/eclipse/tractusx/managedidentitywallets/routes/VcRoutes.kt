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
import io.bkbn.kompendium.core.Notarized.notarizedGet
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.ParameterExample
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.GetInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.eclipse.tractusx.managedidentitywallets.Services
import org.eclipse.tractusx.managedidentitywallets.models.forbiddenException

import org.eclipse.tractusx.managedidentitywallets.models.semanticallyInvalidInputException
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdContexts
import org.eclipse.tractusx.managedidentitywallets.models.syntacticallyInvalidInputException
import org.eclipse.tractusx.managedidentitywallets.models.unauthorizedException
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService

fun Route.vcRoutes(walletService: IWalletService) {

    route("/credentials") {

        notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
            notarizedGet(
                GetInfo<VerifiableCredentialParameters, List<VerifiableCredentialDto>>(
                    summary = "Query Verifiable Credentials",
                    description = "Permission: " +
                            "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_VIEW_WALLETS)}** OR " +
                            "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_VIEW_WALLET)}** " +
                            "(The BPN of holderIdentifier must equal BPN of caller)\n" +
                            "\nSearch verifiable credentials with filter criteria",
                    parameterExamples = setOf(
                        ParameterExample("id", "id", "http://example.edu/credentials/3732"),
                        ParameterExample("type", "type", "['University-Degree-Credential']"),
                        ParameterExample("issuerIdentifier", "issuerIdentifierDid", "did:example:0123"),
                        ParameterExample("holderIdentifier", "holderIdentifierDid", "did:example:4567"),
                        ParameterExample("issuerIdentifier", "issuerIdentifierBPN", "BPN0123"),
                        ParameterExample("holderIdentifier", "holderIdentifierBPN", "BPN4567")
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The list of verifiable credentials matching the query, empty if no match found"
                    ),
                    tags = setOf("VerifiableCredentials"),
                    canThrow = setOf(forbiddenException, unauthorizedException)
                )
            ) {
                val id = call.request.queryParameters["id"]
                val type = call.request.queryParameters["type"]
                val issuerIdentifier = call.request.queryParameters["issuerIdentifier"]
                val holderIdentifier = call.request.queryParameters["holderIdentifier"]

                AuthorizationHandler.checkHasRightsToViewWallet(call, holderIdentifier)

                call.respond(HttpStatusCode.OK,
                    walletService.getCredentials(issuerIdentifier, holderIdentifier, type, id)
                )
            }
        }

        notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
            notarizedPost(
                PostInfo<Unit, VerifiableCredentialRequestDto, VerifiableCredentialDto>(
                    summary = "Issue Verifiable Credential",
                    description = "Permission: " +
                        "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}** OR " +
                        "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLET)}** " +
                            "(The BPN of the issuer of the Verifiable Credential must equal BPN of caller)\n" +
                        "\nIssue a verifiable credential with a given issuer DID",
                    requestInfo = RequestInfo(
                        description = "The verifiable credential input data",
                        examples = verifiableCredentialRequestDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "The created Verifiable Credential",
                        examples = signedVerifiableCredentialDtoExample
                    ),
                    canThrow = setOf(semanticallyInvalidInputException, syntacticallyInvalidInputException,
                        forbiddenException, unauthorizedException),
                    tags = setOf("VerifiableCredentials")
                )
            ) {
                val verifiableCredentialDto = call.receive<VerifiableCredentialRequestDto>()

                AuthorizationHandler.checkHasRightsToUpdateWallet(call, verifiableCredentialDto.issuerIdentifier)

                call.respond(HttpStatusCode.Created, walletService.issueCredential(verifiableCredentialDto))
            }
        }

        route("/issuer") {
            notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                notarizedPost(
                    PostInfo<Unit, VerifiableCredentialRequestWithoutIssuerDto, VerifiableCredentialDto>(
                        summary = "Issue a Verifiable Credential with Catena-X platform issuer",
                        description = "Permission: " +
                            "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}** OR " +
                            "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLET)}** " +
                                "(The BPN of Catena-X wallet must equal BPN of caller)\n" +
                            "\nIssue a verifiable credential by Catena-X wallet",
                        requestInfo = RequestInfo(
                            description = "The verifiable credential input",
                            examples = verifiableCredentialRequestWithoutIssuerDtoExample
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.Created,
                            description = "The created Verifiable Credential",
                            examples = signedVerifiableCredentialDtoExample
                        ),
                        canThrow = setOf(semanticallyInvalidInputException, syntacticallyInvalidInputException,
                            forbiddenException, unauthorizedException),
                        tags = setOf("VerifiableCredentials")
                    )
                ) {
                    val verifiableCredentialRequestDto = call.receive<VerifiableCredentialRequestWithoutIssuerDto>()

                    AuthorizationHandler.checkHasRightsToUpdateWallet(call, Services.walletService.getCatenaXBpn())

                    val verifiableCredentialDto = walletService.issueCatenaXCredential(verifiableCredentialRequestDto)
                    call.respond(HttpStatusCode.Created, verifiableCredentialDto)
                }
            }
        }
    }
}

val verifiableCredentialRequestDtoExample = mapOf(
    "demo" to VerifiableCredentialRequestDto(
        context = listOf(
            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
        ),
        id = "http://example.edu/credentials/3732",
        type = listOf("University-Degree-Credential, VerifiableCredential"),
        issuerIdentifier = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        issuanceDate = "2019-06-16T18:56:59Z",
        expirationDate = "2019-06-17T18:56:59Z",
        credentialSubject = mapOf("college" to "Test-University"),
        holderIdentifier = "did:example:492edf208"
    )
)

val verifiableCredentialRequestWithoutIssuerDtoExample = mapOf(
    "demo" to VerifiableCredentialRequestWithoutIssuerDto(
        context = listOf(
            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
        ),
        id = "http://example.edu/credentials/3732",
        type = listOf("University-Degree-Credential, VerifiableCredential"),
        issuanceDate = "2019-06-16T18:56:59Z",
        expirationDate = "2019-06-17T18:56:59Z",
        credentialSubject = mapOf("college" to "Test-University"),
        holderIdentifier = "did:example:492edf208"
    )
)

val signedVerifiableCredentialDtoExample =  mapOf(
    "demo" to VerifiableCredentialDto(
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
