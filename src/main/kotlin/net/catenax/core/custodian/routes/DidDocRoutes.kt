package net.catenax.core.custodian.routes

import io.bkbn.kompendium.core.Notarized.notarizedDelete
import io.bkbn.kompendium.core.Notarized.notarizedGet
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.Notarized.notarizedPut
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
import net.catenax.core.custodian.models.ssi.DidDocumentDto
import net.catenax.core.custodian.models.ssi.DidServiceDto
import net.catenax.core.custodian.models.ssi.DidVerificationMethodDto
import net.catenax.core.custodian.models.ssi.VerifiableCredentialDto

fun Route.didDocRoutes() {

    route("/didDocuments") {

        route("/{did}") {
            notarizedGet(
                GetInfo<Unit, DidDocumentDto>(
                    summary = "Resolve DID Document",
                    description = "resolve the did document for given did",
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The resolved Did Document",
                        examples = didDocumentDtoExample
                    ),
                    canThrow = setOf(invalidInputException),
                    tags = setOf("DidDocuments")
                )
            ) {
                val verifiableCredentialDto = call.receive<VerifiableCredentialDto>()
                call.respond(
                    HttpStatusCode.Created,
                    didDocumentDtoExample["demo"] as DidDocumentDto
                )
            }
        }

        route("/{did}/services") {
            notarizedPost(
                PostInfo<Unit, DidServiceDto, DidDocumentDto>(
                    summary = "Add New Service Endpoint",
                    description = "add a new service endpoint to the DID Document",
                    requestInfo = RequestInfo(
                        description = "The Service endpoint",
                        examples = didServiceDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The resolved Did Document after the adding the new Service",
                        examples = didDocumentDtoExample
                    ),
                    canThrow = setOf(notFoundException, invalidInputException),
                    tags = setOf("DidDocuments")
                )
            ) {
                val serviceDto = call.receive<DidServiceDto>()
                call.respond(
                    HttpStatusCode.Created,
                    didDocumentDtoExample["demo"] as DidDocumentDto
                )
            }
        }

        route("/{did}/services") {
            notarizedPut(
                PutInfo<Unit, DidServiceDto, DidDocumentDto>(
                    summary = "Update an existing Service Endpoint",
                    description = "update the service endpoint in the DID Document based on its id",
                    requestInfo = RequestInfo(
                        description = "The Service endpoint",
                        examples = didServiceDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The resolved Did Document after the updating the Service",
                        examples = didDocumentDtoExample
                    ),
                    canThrow = setOf(notFoundException, invalidInputException),
                    tags = setOf("DidDocuments")
                )
            ) {
                val serviceDto = call.receive<DidServiceDto>()
                call.respond(
                    HttpStatusCode.Created,
                    didDocumentDtoExample["demo"] as DidDocumentDto
                )
            }
        }

        route("/{did}/services/{id}") {
            notarizedDelete(
                DeleteInfo<Unit, DidDocumentDto>(
                    summary = "Remove the Service endpoint",
                    description = "remove service endpoint in DID Document based on its id",
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The resolved Did Document after removing the service",
                        examples = mapOf(
                            "demo" to DidDocumentDto(
                                id = "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                type = listOf("University-Degree-Credential, VerifiableCredential"),
                                context = listOf("https://www.w3.org/ns/did/v1"),
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
                    ),
                    canThrow = setOf(notFoundException, invalidInputException),
                    tags = setOf("DidDocuments")
                )
            ) {
                call.respond(
                    HttpStatusCode.OK,
                    DidDocumentDto(
                        id = "did:example:76e12ec712ebc6f1c221ebfeb1f",
                        type = listOf("University-Degree-Credential, VerifiableCredential"),
                        context = listOf("https://www.w3.org/ns/did/v1"),
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
            }
        }
    }
}

val didDocumentDtoExample = mapOf(
    "demo" to DidDocumentDto(
        id = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        type = listOf("University-Degree-Credential, VerifiableCredential"),
        context = listOf("https://www.w3.org/ns/did/v1"),
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
