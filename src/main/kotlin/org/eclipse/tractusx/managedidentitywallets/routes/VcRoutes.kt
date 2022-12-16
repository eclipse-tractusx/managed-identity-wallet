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
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException

import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdContexts
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CredentialOfferResponse
import org.eclipse.tractusx.managedidentitywallets.services.IRevocationService
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService
import org.eclipse.tractusx.managedidentitywallets.services.UtilsService

fun Route.vcRoutes(
    walletService: IWalletService,
    revocationService: IRevocationService,
    utilsService: UtilsService
) {

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
                    AuthorizationHandler.checkHasRightsToUpdateWallet(
                        call,
                        walletService.getCatenaXWallet().bpn
                    )

                    val verifiableCredentialDto = walletService.issueCatenaXCredential(verifiableCredentialRequestDto)
                    call.respond(HttpStatusCode.Created, verifiableCredentialDto)
                }
            }
        }

        route("/issuance-flow") {
            notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                notarizedPost(
                    PostInfo<Unit, VerifiableCredentialIssuanceFlowRequestDto, CredentialOfferResponse>(
                        summary = "Issue credential flow according to Aries RFC 0453",
                        description = "Permission: " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}** OR " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLET)}** " +
                                "(The BPN of Catena-X wallet must equal BPN of caller)\n" +
                                "\nTrigger an issue credential flow according to Aries RFC 0453 from the issuer to the holder. " + 
                                "Issuer must be a DID managed by the MIW",
                        requestInfo = RequestInfo(
                            description = "The verifiable credential input",
                            examples = verifiableCredentialIssuanceFlowRequestDtoExample
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.Created,
                            description = "The credential Offer as String",
                            examples = credentialOfferResponseExample
                        ),
                        canThrow = setOf(semanticallyInvalidInputException, syntacticallyInvalidInputException,
                            forbiddenException, unauthorizedException),
                        tags = setOf("VerifiableCredentials")
                    )
                ) {
                    val verifiableCredentialRequestDto = call.receive<VerifiableCredentialIssuanceFlowRequestDto>()
                    AuthorizationHandler.checkHasRightsToUpdateWallet(
                        call,
                        walletService.getCatenaXWallet().bpn
                    )
                    val vc = verifiableCredentialRequestDto.toInternalVerifiableCredentialIssuanceFlowRequest()
                    call.respond(
                        HttpStatusCode.Created,
                        walletService.triggerCredentialIssuanceFlow(vc)
                    )
                }
            }
        }

        route("/revocations") {
            notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                notarizedPost(
                    PostInfo<Unit, VerifiableCredentialDto, String>(
                        summary = "Revoke issued Verifiable Credential",
                        description = "Permission: " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}** OR " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLET)}** " +
                                "(The BPN of the issuer of the Verifiable Credential must equal BPN of caller)\n" +
                                "\nRevoke issued Verifiable Credential by issuer",
                        requestInfo = RequestInfo(
                            description = "The signed verifiable credential",
                            examples = signedVerifiableCredentialDtoExample
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.Accepted,
                            description = "Empty response body"
                        ),
                        canThrow = setOf(semanticallyInvalidInputException, syntacticallyInvalidInputException,
                            forbiddenException, unauthorizedException),
                        tags = setOf("VerifiableCredentials")
                    )
                ) {
                    val verifiableCredentialDto = call.receive<VerifiableCredentialDto>()
                    AuthorizationHandler.checkHasRightsToUpdateWallet(call, verifiableCredentialDto.issuer)
                    walletService.revokeVerifiableCredential(verifiableCredentialDto)
                    call.respond(HttpStatusCode.Accepted)
                }
            }

            route("/statusListCredentialRefresh") {
                notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                    notarizedPost(
                        PostInfo<StatusListRefreshParameters, Unit, String>(
                            summary = "Re-issue the Status-List Credential for all or given wallet",
                            description = "Permission: " +
                                    "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}** OR" +
                                    "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLET)}** " +
                                    "(The BPN of wallet to update must equal BPN of caller) \n" +
                                    "\nRe-issue the Status-List Credential for all registered wallet",
                            parameterExamples = setOf(
                                ParameterExample("identifier", "identifier", "BPN0001")
                            ),
                            requestInfo = null,
                            responseInfo = ResponseInfo(
                                status = HttpStatusCode.Accepted,
                                description = "Empty response body",
                            ),
                            canThrow = setOf(semanticallyInvalidInputException, syntacticallyInvalidInputException,
                                forbiddenException, unauthorizedException),
                            tags = setOf("VerifiableCredentials")
                        )
                    ) {
                        val identifier = call.request.queryParameters["identifier"]
                        AuthorizationHandler.checkHasRightsToUpdateWallet(call, identifier)

                        val force: Boolean = if (!call.request.queryParameters["force"].isNullOrBlank()) {
                            call.request.queryParameters["force"].toBoolean()
                        } else { false }

                        if (identifier.isNullOrBlank()) {
                            revocationService.issueStatusListCredentials()
                        } else {
                            val wallet = walletService.getWallet(identifier, false)
                            revocationService.issueStatusListCredentials(
                                profileName = utilsService.getIdentifierOfDid(did = wallet.did),
                                force = force
                            )
                        }
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
            }
        }

        // Public endpoint to get Status-List Credential
        route("/status/{listName}") {
            notarizedGet(
                GetInfo<ListNameParameter, VerifiableCredentialDto>(
                    summary = "Query Status-List Credentials",
                    description =  "Get the Status-List Credential for a given listName",
                    parameterExamples = setOf(
                        ParameterExample("listName", "listName", "5cb9ce19-9a10-48fe-bfa6-384632b89dc3"),
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The Verifiable Credential",
                        examples = statusListCredentialExample
                    ),
                    tags = setOf("VerifiableCredentials"),
                )
            ) {
                val listName = call.parameters["listName"] ?: throw BadRequestException("Missing or malformed listName")
                call.respond(HttpStatusCode.OK, revocationService.getStatusListCredentialOfManagedWallet(listName))
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
        credentialStatus = CredentialStatus(
            statusId = "https://example.com/credentials/status/3#94567",
            credentialType = "StatusList2021Entry",
            statusPurpose = "revocation",
            index = "94567",
            listUrl = "https://example.com/credentials/status/3"
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

val listCredentialRequestData = mapOf(
    "demo" to ListCredentialRequestData(
        listId = "urn:uuid:93731387-dec1-4bf6-8087-d5210f661422",
        subject = ListCredentialSubject (
            credentialId = "https://example.com/status/3#list",
            credentialType = "StatusList2021",
            statusPurpose = "revocation",
            encodedList = "H4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"
        )
    )
)

val statusListCredentialExample = mapOf(
    "demo" to VerifiableCredentialDto(
        id = "https://example.com/api/credentials/status/5c145c85-8fcb-42d4-893c-d19a55581e00",
        context = listOf("https://www.w3.org/2018/credentials/v1", "https://w3id.org/vc/status-list/2021/v1"),
        type = listOf( "VerifiableCredential", "StatusList2021Credential"),
        issuer =  "did:indy:local:test:Ae49DuXZy2PLBjSL9W2V2i",
        issuanceDate = "2022-08-31T07:19:36Z",
        credentialSubject = mapOf(
            "id" to "https://example.com/api/credentials/status/5c145c85-8fcb-42d4-893c-d19a55581e00#list",
            "type" to "StatusList2021",
            "statusPurpose" to "revocation",
            "encodedList" to "H4sIAAAAAAAAAO3BIQEAAAACIAf4f68zLEADAAAAAAAAAAAAAAAAAAAAvA3HJiyHAEAAAA=="
        ),
        proof = LdProofDto(
            type =  "Ed25519Signature2018",
            created = "2022-08-31T07:19:42Z",
            proofPurpose = "assertionMethod",
            verificationMethod = "did:indy:local:test:Ae49DuXZy2PLBjSL9W2V2i#key-1",
            jws = "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0FB66o-WAn8W4qnNK0NsHBFMJj_ZM42ADdbwYO-P8oGywaYWeBPZylgD35AV2-CR0b5Hs8uDq0EIn8iHycjmBQ"
        )
    )
)

val verifiableCredentialIssuanceFlowRequestDtoExample = mapOf(
    "demo" to VerifiableCredentialIssuanceFlowRequestDto(
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
        holderIdentifier = "did:example:492edf208",
        webhookUrl = "http://example.com/webhooks"
    )
)

val credentialOfferAsString = """
{"credential": {"@context": ["https://www.w3.org/2018/credentials/v1", "https://raw.githubusercontent.com/catenax-ng/product-core-schemas/main/businessPartnerData"], "type": ["BpnCredential", "VerifiableCredential"], "issuer": "did:sov:HsfwvUFcZkAcxDa2kASMr7", "issuanceDate": "2021-06-16T18:56:59Z", "credentialSubject": {"type": ["BpnCredential"], "bpn": "NEWNEWTestTest", "id": "did:sov:7rB93fLvW5kgujZ4E57ZxL"}}, "options": {"proofType": "Ed25519Signature2018"}}
""".trimIndent()

val credentialOfferResponseExample = mapOf(
    "demo" to CredentialOfferResponse(
        credentialOffer = credentialOfferAsString,
        threadId = "2ewqe-qwe24-eqweqwrqwr-rwqrqwr"
    )
)
