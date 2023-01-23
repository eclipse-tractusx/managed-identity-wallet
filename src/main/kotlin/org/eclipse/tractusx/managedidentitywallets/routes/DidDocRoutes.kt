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
import io.bkbn.kompendium.core.Notarized.notarizedPut
import io.bkbn.kompendium.core.metadata.ParameterExample
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.DeleteInfo
import io.bkbn.kompendium.core.metadata.method.GetInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo
import io.bkbn.kompendium.core.metadata.method.PutInfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException
import org.eclipse.tractusx.managedidentitywallets.models.forbiddenException
import org.eclipse.tractusx.managedidentitywallets.models.notFoundException
import org.eclipse.tractusx.managedidentitywallets.models.semanticallyInvalidInputException
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidDocumentDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidDocumentParameters
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidDocumentServiceParameters
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidServiceDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidServiceUpdateRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidVerificationMethodDto
import org.eclipse.tractusx.managedidentitywallets.models.syntacticallyInvalidInputException
import org.eclipse.tractusx.managedidentitywallets.models.unauthorizedException
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService

fun Route.didDocRoutes(walletService: IWalletService) {

    route("/didDocuments") {

        route("/{identifier}") {

            // unprotected, as it is public information
            notarizedGet(
                GetInfo<DidDocumentParameters, DidDocumentDto>(
                    summary = "Resolve DID Document",
                    description = "Resolve the DID document for a given DID or BPN",
                    parameterExamples = setOf(
                        ParameterExample("identifier", "did", "did:exp:123"),
                        ParameterExample("identifier", "bpn", "BPN123"),
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The resolved DID Document",
                        examples = didDocumentDtoExample
                    ),
                    canThrow = setOf(notFoundException, syntacticallyInvalidInputException),
                    tags = setOf("DIDDocument"),
                )
            ) {
                val identifier =
                    call.parameters["identifier"] ?: throw BadRequestException("Missing or malformed identifier")
                call.respond(HttpStatusCode.OK, walletService.resolveDocument(identifier))
            }

            route("/services") {
                notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                    notarizedPost(
                        PostInfo<DidDocumentParameters, DidServiceDto, Unit>(
                            summary = "Add New Service Endpoint",
                            description = "Permission: " +
                                "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}**\n" +
                                "\nAdd a new service endpoint to the DID Document",
                            parameterExamples = setOf(
                                ParameterExample("identifier", "did", "did:exp:123"),
                                ParameterExample("identifier", "bpn", "BPN123")
                            ),
                            requestInfo = RequestInfo(
                                description = "The Service endpoint",
                                examples = didServiceDtoExample
                            ),
                            responseInfo = ResponseInfo(
                                status = HttpStatusCode.Accepted,
                                description = "Adding the Service is accepted and processing"
                            ),
                            canThrow = setOf(notFoundException, syntacticallyInvalidInputException),
                            tags = setOf("DIDDocument")
                        )
                    ) {
                        val serviceDto = call.receive<DidServiceDto>()
                        val identifier =
                            call.parameters["identifier"] ?: throw BadRequestException("Missing or malformed identifier")
                        AuthorizationHandler.checkHasRightsToUpdateWallet(call)
                        return@notarizedPost call.respond(
                            HttpStatusCode.Accepted,
                            walletService.addService(identifier, serviceDto)
                        )
                    }
                }

                route("/{id}") {
                    notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
                        notarizedPut(
                            PutInfo<DidDocumentServiceParameters, DidServiceUpdateRequestDto, Unit>(
                                summary = "Update an existing Service Endpoint",
                                description = "Permission: " +
                                    "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}**\n" +
                                    "\nUpdate the service endpoint in the DID Document based on its id",
                                parameterExamples = setOf(
                                    ParameterExample("identifier", "did", "did:exp:123"),
                                    ParameterExample("identifier", "bpn", "BPN123"),
                                    ParameterExample("id", "id", "did:example:123#edv")
                                ),
                                requestInfo = RequestInfo(
                                    description = "The updated service endpoint data",
                                    examples = didServiceUpdateRequestDtoExample
                                ),
                                responseInfo = ResponseInfo(
                                    status = HttpStatusCode.Accepted,
                                    description = "Updating the Service is accepted and processing",
                                ),
                                canThrow = setOf(notFoundException, semanticallyInvalidInputException,
                                    syntacticallyInvalidInputException, forbiddenException, unauthorizedException),
                                tags = setOf("DIDDocument")
                            )
                        ) {
                            val identifier = call.parameters["identifier"]
                                ?: throw BadRequestException("Missing or malformed identifier")

                            AuthorizationHandler.checkHasRightsToUpdateWallet(call)

                            val serviceId =
                                call.parameters["id"] ?: throw BadRequestException("Missing or malformed service id")
                            val serviceDto = call.receive<DidServiceUpdateRequestDto>()
                            return@notarizedPut call.respond(
                                HttpStatusCode.Accepted,
                                walletService.updateService(
                                    identifier = identifier,
                                    id = serviceId,
                                    serviceUpdateRequestDto = serviceDto
                                )
                            )
                        }

                        notarizedDelete(
                            DeleteInfo<DidDocumentServiceParameters, Unit>(
                                summary = "Remove Service Endpoint",
                                description = "Permission: " +
                                    "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}**\n" +
                                    "\nRemove service endpoint in DID Document based on its id",
                                parameterExamples = setOf(
                                    ParameterExample("identifier", "did", "did:exp:123"),
                                    ParameterExample("identifier", "bpn", "BPN123"),
                                    ParameterExample("id", "id", "did:example:123#edv")
                                ),
                                responseInfo = ResponseInfo(
                                    status = HttpStatusCode.Accepted,
                                    description = "Deleting the Service is accepted and processing",
                                ),
                                canThrow = setOf(notFoundException,semanticallyInvalidInputException,
                                    syntacticallyInvalidInputException, forbiddenException, unauthorizedException),
                                tags = setOf("DIDDocument")
                            )
                        ) {
                            val identifier = call.parameters["identifier"]
                                ?: throw BadRequestException("Missing or malformed identifier")
                            AuthorizationHandler.checkHasRightsToUpdateWallet(call)
                            val serviceId =
                                call.parameters["id"] ?: throw BadRequestException("Missing or malformed service id")
                            call.respond(HttpStatusCode.Accepted, walletService.deleteService(identifier, serviceId))
                        }
                    }
                }
            }
        }
    }
}

val didDocumentDtoExample: Map<String, DidDocumentDto> = mapOf(
    "demo" to DidDocumentDto(
        id = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        context = listOf("https://www.w3.org/ns/did/v1"),
        controller = listOf("123", "1231"),
        verificationMethods = listOf(
            DidVerificationMethodDto(
                id = "did:example:76e12ec712ebc6f1c221ebfeb1f#key-1",
                type = "Ed25519VerificationKey2018",
                controller = "did:example:76e12ec712ebc6f1c221ebfeb1f",
                publicKeyBase58 = "FyfKP2HvTKqDZQzvyL38yXH7bExmwofxHf2NR5BrcGf1"
            )
        ),
        services = listOf(
            DidServiceDto(
                id = "did:example:123#edv",
                type = "ServiceEndpointProxyService",
                serviceEndpoint = "https://myservice.com/myendpoint"
            )
        )
    )
)

val didServiceDtoExample = mapOf(
    "demo" to DidServiceDto(
        id = "did:example:123#edv",
        type = "ServiceEndpointProxyService",
        serviceEndpoint = "https://myservice.com/myendpoint"
    )
)

val didServiceUpdateRequestDtoExample = mapOf(
    "demo" to DidServiceUpdateRequestDto(
        type = "ServiceEndpointProxyService",
        serviceEndpoint = "https://myservice.com/myendpoint"
    )
)
