package net.catenax.core.managedidentitywallets.routes

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

import net.catenax.core.managedidentitywallets.models.semanticallyInvalidInputException
import net.catenax.core.managedidentitywallets.models.ssi.*
import net.catenax.core.managedidentitywallets.models.ssi.JsonLdContexts
import net.catenax.core.managedidentitywallets.models.syntacticallyInvalidInputException
import net.catenax.core.managedidentitywallets.plugins.AuthConstants
import net.catenax.core.managedidentitywallets.services.WalletService

import org.slf4j.LoggerFactory

fun Route.vcRoutes(walletService: WalletService) {

    val log = LoggerFactory.getLogger(this::class.java)

    route("/credentials") {

        notarizedAuthenticate(AuthConstants.JWT_AUTH_VIEW, AuthConstants.JWT_AUTH_VIEW_SINGLE) {
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
                    tags = setOf("VerifiableCredentials"),
                    securitySchemes = setOf(AuthConstants.JWT_AUTH_VIEW.name)
                )
            ) {
                val id = call.request.queryParameters["id"]
                val type = call.request.queryParameters["type"]
                val issuerIdentifier = call.request.queryParameters["issuerIdentifier"]
                val holderIdentifier = call.request.queryParameters["holderIdentifier"]

                // verify requested holder with bpn in principal, if only ROLE_VIEW_WALLET is given
                val principal = AuthConstants.getPrincipal(call.attributes)
                if (principal?.role == AuthConstants.ROLE_VIEW_WALLET && holderIdentifier == principal?.bpn) {
                    log.debug("Authorization successful: holder identifier BPN ${holderIdentifier} does match requestors BPN ${principal.bpn}!")
                }
                if (principal?.role == AuthConstants.ROLE_VIEW_WALLET && holderIdentifier != principal?.bpn) {
                    log.error("Error: Holder identifier BPN ${holderIdentifier} does not match requestors BPN ${principal?.bpn}!")
                    return@notarizedGet call.respondText("Holder identifier BPN ${holderIdentifier} does not match requestors BPN ${principal?.bpn}!", ContentType.Text.Plain, HttpStatusCode.Unauthorized)
                }

                call.respond(
                    HttpStatusCode.OK,
                    walletService.getCredentials(issuerIdentifier, holderIdentifier, type, id)
                )
            }
        }

        notarizedAuthenticate(AuthConstants.JWT_AUTH_UPDATE, AuthConstants.JWT_AUTH_UPDATE_SINGLE) {
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
                    tags = setOf("VerifiableCredentials"),
                    securitySchemes = setOf(AuthConstants.JWT_AUTH_UPDATE.name)
                )
            ) {
                val verifiableCredentialDto = call.receive<VerifiableCredentialRequestDto>()

                // verify requested holder with bpn in principal, if only ROLE_UPDATE_WALLET is given
                val principal = AuthConstants.getPrincipal(call.attributes)
                if (principal?.role == AuthConstants.ROLE_UPDATE_WALLET && verifiableCredentialDto.holderIdentifier == principal?.bpn) {
                    log.debug("Authorization successful: holder identifier BPN ${verifiableCredentialDto.holderIdentifier} does match requestors BPN ${principal?.bpn}!")
                }
                if (principal?.role == AuthConstants.ROLE_UPDATE_WALLET && verifiableCredentialDto.holderIdentifier != principal?.bpn) {
                    log.error("Error: Holder identifier BPN ${verifiableCredentialDto.holderIdentifier} does not match requestors BPN ${principal?.bpn}!")
                    return@notarizedPost call.respondText("Holder identifier BPN ${verifiableCredentialDto.holderIdentifier} does not match requestors BPN ${principal?.bpn}!", ContentType.Text.Plain, HttpStatusCode.Unauthorized)
                }

                call.respond(HttpStatusCode.Created, walletService.issueCredential(verifiableCredentialDto))
            }
        }

        route("/issuer") {
            notarizedAuthenticate(AuthConstants.JWT_AUTH_UPDATE, AuthConstants.JWT_AUTH_UPDATE_SINGLE) {
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
                        tags = setOf("VerifiableCredentials"),
                        securitySchemes = setOf(AuthConstants.JWT_AUTH_UPDATE.name)
                    )
                ) {
                    val verifiableCredentialRequestDto = call.receive<VerifiableCredentialRequestWithoutIssuerDto>()

                    // verify requested holder with bpn in principal, if only ROLE_UPDATE_WALLET is given
                    val principal = AuthConstants.getPrincipal(call.attributes)
                    if (principal?.role == AuthConstants.ROLE_UPDATE_WALLET && verifiableCredentialRequestDto.holderIdentifier == principal?.bpn) {
                        log.debug("Authorization successful: holder identifier BPN ${verifiableCredentialRequestDto.holderIdentifier} does match requestors BPN ${principal?.bpn}!")
                    }
                    if (principal?.role == AuthConstants.ROLE_UPDATE_WALLET && verifiableCredentialRequestDto.holderIdentifier != principal?.bpn) {
                        log.error("Error: Holder identifier BPN ${verifiableCredentialRequestDto.holderIdentifier} does not match requestors BPN ${principal?.bpn}!")
                        return@notarizedPost call.respondText("Holder identifier BPN ${verifiableCredentialRequestDto.holderIdentifier} does not match requestors BPN ${principal?.bpn}!", ContentType.Text.Plain, HttpStatusCode.Unauthorized)
                    }

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
