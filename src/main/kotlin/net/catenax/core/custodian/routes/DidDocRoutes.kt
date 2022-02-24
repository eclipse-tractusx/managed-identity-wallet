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
import net.catenax.core.custodian.models.ssi.*

fun Route.didDocRoutes() {

    route("/didDocuments") {

        route("/{identifier}") {
            notarizedGet(
                GetInfo<DidDocumentParameters, DidDocumentDto>(
                    summary = "Resolve DIDDocument",
                    description = "resolve the DIDDocument for given DID or BPN",
                    parameterExamples = setOf(
                        ParameterExample("identifier", "did", "did:exp:123"),
                        ParameterExample("identifier", "bpn", "BPN123"),
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The resolved DIDDocument",
                        examples = didDocumentDtoExample
                    ),
                    canThrow = setOf(invalidInputException),
                    tags = setOf("DIDDocument")
                )
            ) {
                call.respond(
                    HttpStatusCode.Created,
                    didDocumentDtoExample["demo"] as DidDocumentDto
                )
            }
        }

        route("/{identifier}/services") {
            notarizedPost(
                PostInfo<DidDocumentServiceParameters, DidServiceDto, DidDocumentDto>(
                    summary = "Add New Service Endpoint",
                    description = "add a new service endpoint to the DIDDocument",
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
                        description = "The resolved DIDDocument after the adding the new Service",
                        examples = didDocumentDtoExample
                    ),
                    canThrow = setOf(notFoundException, invalidInputException),
                    tags = setOf("DIDDocument")
                )
            ) {
                val serviceDto = call.receive<DidServiceDto>()
                call.respond(
                    HttpStatusCode.Created,
                    didDocumentDtoExample["demo"] as DidDocumentDto
                )
            }

            route("/{id}") {
                notarizedPut(
                    PutInfo<DidDocumentParameters, DidServiceUpdateRequestDto, DidDocumentDto>(
                        summary = "Update an existing Service Endpoint",
                        description = "update the service endpoint in the DIDDocument based on its id",
                        parameterExamples = setOf(
                            ParameterExample("identifier", "did", "did:exp:123"),
                            ParameterExample("identifier", "bpn", "BPN123"),
                            ParameterExample("id", "id", "did:example:123#edv")
                        ),
                        requestInfo = RequestInfo(
                            description = "The Service endpoint",
                            examples = didServiceUpdateRequestDtoExample
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.OK,
                            description = "The resolved DIDDocument after the updating the Service",
                            examples = didDocumentDtoExample
                        ),
                        canThrow = setOf(notFoundException, invalidInputException, invalidInputSyntaxException),
                        tags = setOf("DIDDocument")
                    )
                ) {
                    val serviceDto = call.receive<DidServiceDto>()
                    call.respond(
                        HttpStatusCode.Created,
                        didDocumentDtoExample["demo"] as DidDocumentDto
                    )
                }

                notarizedDelete(
                    DeleteInfo<DidDocumentServiceParameters, DidDocumentDto>(
                        summary = "Remove the Service endpoint",
                        description = "remove service endpoint in DIDDocument based on its id",
                        parameterExamples = setOf(
                            ParameterExample("identifier", "did", "did:exp:123"),
                            ParameterExample("identifier", "bpn", "BPN123"),
                            ParameterExample("id", "id", "did:example:123#edv")
                        ),
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.OK,
                            description = "The resolved DIDDocument after removing the service",
                            examples = didDocumentDtoWithoutServiceExample
                        ),
                        canThrow = setOf(notFoundException, invalidInputException, invalidInputSyntaxException),
                        tags = setOf("DIDDocument")
                    )
                ) {
                    call.respond(
                        HttpStatusCode.OK,
                        didDocumentDtoWithoutServiceExample["demo"] as DidDocumentDto
                    )
                }
            }
        }
    }
}

val didDocumentDtoExample = mapOf(
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

