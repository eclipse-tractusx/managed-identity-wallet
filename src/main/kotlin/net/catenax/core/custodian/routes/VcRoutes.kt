package net.catenax.core.custodian.routes

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
import net.catenax.core.custodian.models.semanticallyInvalidInputException
import net.catenax.core.custodian.models.ssi.*
import net.catenax.core.custodian.models.ssi.JsonLdContexts
import net.catenax.core.custodian.models.syntacticallyInvalidInputException
import net.catenax.core.custodian.services.WalletService

fun Route.vcRoutes(walletService: WalletService) {

    route("/credentials") {

            notarizedGet(
                GetInfo<VerifiableCredentialParameters, List<VerifiableCredentialDto>>(
                    summary = "Query Verifiable Credentials",
                    description = "Search verifiable credentials with filter criteria",
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
                    tags = setOf("VerifiableCredentials")
                )
            ) {
                val id = call.request.queryParameters["id"] ?: null
                val type = call.request.queryParameters["type"] ?: null
                val issuerIdentifier = call.request.queryParameters["issuerIdentifier"] ?: null
                val holderIdentifier = call.request.queryParameters["holderIdentifier"] ?: null
                call.respond(
                    HttpStatusCode.OK,
                    walletService.getCredentials(issuerIdentifier, holderIdentifier, type, id)
                )
            }

            notarizedPost(
                PostInfo<Unit, VerifiableCredentialRequestDto, VerifiableCredentialDto>(
                    summary = "Issue Verifiable Credential ",
                    description = "Issue a verifiable credential with a given issuer DID",
                    requestInfo = RequestInfo(
                        description = "The verifiable credential input data",
                        examples = verifiableCredentialRequestDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "The created Verifiable Credential",
                        examples = signedVerifiableCredentialDtoExample
                    ),
                    canThrow = setOf(semanticallyInvalidInputException, syntacticallyInvalidInputException),
                    tags = setOf("VerifiableCredentials")
                )
            ) {
                val verifiableCredentialDto = call.receive<VerifiableCredentialRequestDto>()
                call.respond(HttpStatusCode.Created, walletService.issueCredential(verifiableCredentialDto))
            }

        route("/issuer") {
            notarizedPost(
                PostInfo<Unit, VerifiableCredentialRequestWithoutIssuerDto, VerifiableCredentialDto>(
                    summary = "Issue a Verifiable Credential with Catena-X platform issuer",
                    description = "Issue a verifiable credential by Catena-X wallet",
                    requestInfo = RequestInfo(
                        description = "The verifiable credential input",
                        examples = verifiableCredentialRequestWithoutIssuerDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "The created Verifiable Credential",
                        examples = signedVerifiableCredentialDtoExample
                    ),
                    canThrow = setOf(semanticallyInvalidInputException, syntacticallyInvalidInputException),
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
