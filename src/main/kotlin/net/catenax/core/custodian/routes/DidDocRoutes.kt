package net.catenax.core.custodian.routes

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
import net.catenax.core.custodian.models.BadRequestException
import net.catenax.core.custodian.models.notFoundException
import net.catenax.core.custodian.models.semanticallyInvalidInputException
import net.catenax.core.custodian.models.ssi.*
import net.catenax.core.custodian.models.syntacticallyInvalidInputException
import net.catenax.core.custodian.services.WalletService

fun Route.didDocRoutes(walletService: WalletService) {

    route("/didDocuments") {

        route("/{identifier}") {
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
                    tags = setOf("DIDDocument")
                )
            ) {
                val identifier =
                    call.parameters["identifier"] ?: throw BadRequestException("Missing or malformed identifier")
                call.respond(HttpStatusCode.OK, walletService.resolveDocument(identifier))
            }

            route("/services") {
                notarizedPost(
                    PostInfo<DidDocumentParameters, DidServiceDto, DidDocumentDto>(
                        summary = "Add New Service Endpoint",
                        description = "Add a new service endpoint to the DID Document",
                        parameterExamples = setOf(
                            ParameterExample("identifier", "did", "did:exp:123"),
                            ParameterExample("identifier", "bpn", "BPN123")
                        ),
                        requestInfo = RequestInfo(
                            description = "The Service endpoint",
                            examples = didServiceDtoExample
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.OK,
                            description = "The resolved DID Document after adding the new Service",
                            examples = didDocumentDtoExample
                        ),
                        canThrow = setOf(notFoundException, syntacticallyInvalidInputException),
                        tags = setOf("DIDDocument")
                    )
                ) {
                    val serviceDto = call.receive<DidServiceDto>()
                    val identifier =
                        call.parameters["identifier"] ?: throw BadRequestException("Missing or malformed identifier")
                    return@notarizedPost call.respond(
                        HttpStatusCode.Created,
                        walletService.addService(identifier, serviceDto)
                    )
                }

                route("/{id}") {
                    notarizedPut(
                        PutInfo<DidDocumentServiceParameters, DidServiceUpdateRequestDto, DidDocumentDto>(
                            summary = "Update an existing Service Endpoint",
                            description = "Update the service endpoint in the DID Document based on its id",
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
                                status = HttpStatusCode.OK,
                                description = "The resolved DID Document after the updating the Service",
                                examples = didDocumentDtoExample
                            ),
                            canThrow = setOf(
                                notFoundException,
                                semanticallyInvalidInputException,
                                syntacticallyInvalidInputException
                            ),
                            tags = setOf("DIDDocument")
                        )
                    ) {
                        val identifier = call.parameters["identifier"]
                            ?: throw BadRequestException("Missing or malformed identifier")
                        val serviceId =
                            call.parameters["id"] ?: throw BadRequestException("Missing or malformed service id")
                        val serviceDto = call.receive<DidServiceUpdateRequestDto>()
                        return@notarizedPut call.respond(
                            HttpStatusCode.OK,
                            walletService.updateService(
                                identifier = identifier,
                                id = serviceId,
                                serviceUpdateRequestDto = serviceDto
                            )
                        )
                    }

                    notarizedDelete(
                        DeleteInfo<DidDocumentServiceParameters, DidDocumentDto>(
                            summary = "Remove Service Endpoint",
                            description = "Remove service endpoint in DID Document based on its id",
                            parameterExamples = setOf(
                                ParameterExample("identifier", "did", "did:exp:123"),
                                ParameterExample("identifier", "bpn", "BPN123"),
                                ParameterExample("id", "id", "did:example:123#edv")
                            ),
                            responseInfo = ResponseInfo(
                                status = HttpStatusCode.OK,
                                description = "The resolved DID Document after removing the service",
                                examples = didDocumentDtoWithoutServiceExample
                            ),
                            canThrow = setOf(
                                notFoundException,
                                semanticallyInvalidInputException,
                                syntacticallyInvalidInputException
                            ),
                            tags = setOf("DIDDocument")
                        )
                    ) {
                        val identifier = call.parameters["identifier"]
                            ?: throw BadRequestException("Missing or malformed identifier")
                        val serviceId =
                            call.parameters["id"] ?: throw BadRequestException("Missing or malformed service id")
                        call.respond(HttpStatusCode.OK, walletService.deleteService(identifier, serviceId))
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

val didDocumentDtoWithoutServiceExample = mapOf(
    "demo" to DidDocumentDto(
        id = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        context = listOf("https://www.w3.org/ns/did/v1"),
        controller = "test",
        verificationMethods = listOf(
            DidVerificationMethodDto(
                id = "did:example:76e12ec712ebc6f1c221ebfeb1f#key-1",
                type = "Ed25519VerificationKey2018",
                controller = "did:example:76e12ec712ebc6f1c221ebfeb1f",
                publicKeyBase58 = "FyfKP2HvTKqDZQzvyL38yXH7bExmwofxHf2NR5BrcGf1"
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
