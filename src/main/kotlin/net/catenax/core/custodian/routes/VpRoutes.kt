package net.catenax.core.custodian.routes

import com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialContexts
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.catenax.core.custodian.models.semanticallyInvalidInputException
import net.catenax.core.custodian.models.ssi.LdProofDto
import net.catenax.core.custodian.models.ssi.VerifiableCredentialDto
import net.catenax.core.custodian.models.ssi.VerifiablePresentationDto
import net.catenax.core.custodian.models.ssi.VerifiablePresentationRequestDto
import net.catenax.core.custodian.services.WalletService

fun Route.vpRoutes(walletService: WalletService) {

    route("/presentations") {
        notarizedPost(
            PostInfo<Unit, VerifiablePresentationRequestDto, VerifiablePresentationDto>(
                summary = "Create Verifiable Presentation ",
                description = "Create a verifiable presentation from a list of verifiable credentials, signed by the holder",
                requestInfo = RequestInfo(
                    description = "The verifiable presentation input data",
                    examples = verifiablePresentationRequestDtoExample
                ),
                responseInfo = ResponseInfo(
                    status = HttpStatusCode.Created,
                    description = "The created verifiable presentation",
                    examples = verifiablePresentationResponseDtoExample
                ),
                canThrow = setOf(semanticallyInvalidInputException),
                tags = setOf("VerifiablePresentations")
            )
        ) {
            val verifiableCredentialDto = call.receive<VerifiablePresentationRequestDto>()
            call.respond(HttpStatusCode.Created, walletService.issuePresentation(verifiableCredentialDto))
        }
    }
}

val verifiablePresentationRequestDtoExample = mapOf(
    "demo" to VerifiablePresentationRequestDto(
        holderIdentifier = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        verifiableCredentials = listOf(
            VerifiableCredentialDto(
                context = listOf(
                    VerifiableCredentialContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1.toString(),
                    VerifiableCredentialContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1.toString()
                ),
                id = "http://example.edu/credentials/333",
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
    )
)

val verifiablePresentationResponseDtoExample = mapOf(
    "demo" to VerifiablePresentationDto(
        context = listOf("https://www.w3.org/2018/credentials/v1"),
        type =  listOf("VerifiablePresentation"),
        holder = "did:example:76e12ec712ebc6f1c221ebfeb1f",
        verifiableCredential = listOf(
            VerifiableCredentialDto(
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
        ),
        proof = LdProofDto(
            type = "Ed25519Signature2018",
            created = "2021-11-17T22:20:27Z",
            proofPurpose = "assertionMethod",
            verificationMethod = "did:example:76e12ec712ebc6f1c221ebfeb1f#keys-1",
            jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
        )
    )
)
