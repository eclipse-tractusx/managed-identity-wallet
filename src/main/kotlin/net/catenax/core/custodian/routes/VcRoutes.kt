package net.catenax.core.custodian.routes

import com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialContexts
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
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.LdProofDto
import net.catenax.core.custodian.models.ssi.VerifiableCredentialDto
import net.catenax.core.custodian.models.ssi.VerifiableCredentialParameters

fun Route.vcRoutes() {

    route("/credentials") {

        route("") {
            notarizedGet(
                GetInfo<VerifiableCredentialParameters, List<VerifiableCredentialDto>>(
                    summary = "Get Verifiable credentials",
                    description = "get verifiable credentials",
                    parameterExamples = setOf(
                        ParameterExample("id", "did", "http://example.edu/credentials/3732"),
                        ParameterExample("type", "type", "['University-Degree-Credential']"),
                        ParameterExample("issuer", "issuer", "did:example:0123"),
                        ParameterExample("holder", "holder", "did:example:4567")
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The created Verifiable Credential",
                    ),
                    tags = setOf("VerifiableCredentials")
                )
            ) {
                val id = call.request.queryParameters["id"]
                call.respond(
                    HttpStatusCode.OK,
                    listOf( signedVerifiableCredentialDtoExample["demo"] as VerifiableCredentialDto )
                )
            }
        }

        route("/issue") {
            notarizedPost(
                PostInfo<Unit, VerifiableCredentialDto, VerifiableCredentialDto>(
                    summary = "Issue Verifiable credential ",
                    description = "issue a verifiable credential",
                    requestInfo = RequestInfo(
                        description = "the verifiable credential",
                        examples = unsignedVerifiableCredentialDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "The created Verifiable Credential",
                        examples = signedVerifiableCredentialDtoExample
                    ),
                    canThrow = setOf(invalidInputException, notFoundException),
                    tags = setOf("VerifiableCredentials")
                )
            ) {
                val verifiableCredentialDto = call.receive<VerifiableCredentialDto>()
                call.respond(
                    HttpStatusCode.Created,
                    signedVerifiableCredentialDtoExample["demo"] as VerifiableCredentialDto
                )
            }
        }

        route("/store") {
            notarizedPost(
                PostInfo<Unit, VerifiableCredentialDto, SuccessResponse>(
                    summary = "Store Verifiable credential ",
                    description = "store a verifiable credential using the subject DID as identifier of wallet",
                    requestInfo = RequestInfo(
                        description = "the verifiable credential",
                        examples = signedVerifiableCredentialDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "Success message",
                        examples = mapOf(
                            "demo" to SuccessResponse(
                                "Credential with id http://example.edu/credentials/3732" +
                                        "has been successfully Stored"
                            )
                        )
                    ),
                    canThrow = setOf(invalidInputException, notFoundException),
                    tags = setOf("VerifiableCredentials")
                )
            ) {
                val verifiableCredentialDto = call.receive<VerifiableCredentialDto>()
                call.respond(
                    HttpStatusCode.Created,
                    SuccessResponse("Credential has been successfully Stored")
                )
            }
        }

        route("/cx-issue") {
            notarizedPost(
                PostInfo<Unit, VerifiableCredentialDto, VerifiableCredentialDto>(
                    summary = "Issue catena-x related credentials",
                    description = "issue a verifiable credential by catena-x wallets",
                    requestInfo = RequestInfo(
                        description = "the verifiable credential",
                        examples = unsignedVerifiableCredentialDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "The created Verifiable Credential",
                        examples = signedVerifiableCredentialDtoExample
                    ),
                    canThrow = setOf(invalidInputException, notFoundException),
                    tags = setOf("VerifiableCredentials")
                )
            ) {
                val verifiableCredentialDto = call.receive<VerifiableCredentialDto>()
                call.respond(
                    HttpStatusCode.Created,
                    signedVerifiableCredentialDtoExample["demo"] as VerifiableCredentialDto
                )
            }
        }
    }
}

val unsignedVerifiableCredentialDtoExample = mapOf(
    "demo" to VerifiableCredentialDto(
        context = listOf(
            VerifiableCredentialContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1.toString(),
            VerifiableCredentialContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1.toString()
        ),
        id = "http://example.edu/credentials/3732",
        type = listOf("University-Degree-Credential, VerifiableCredential"),
        issuer = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        issuanceDate = "2019-06-16T18:56:59Z",
        expirationDate = "2019-06-17T18:56:59Z",
        credentialSubject = mapOf("college" to "Test-University")
    )
)

val signedVerifiableCredentialDtoExample =  mapOf(
    "demo" to VerifiableCredentialDto(
        context = listOf(
            VerifiableCredentialContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1.toString(),
            VerifiableCredentialContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1.toString()
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
