package net.catenax.core.custodian.routes

import com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialContexts
import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.ParameterExample
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import net.catenax.core.custodian.models.SuccessResponse
import net.catenax.core.custodian.models.ssi.IssuedVerifiableCredentialRequestDto
import net.catenax.core.custodian.models.ssi.LdProofDto
import net.catenax.core.custodian.models.ssi.VerifiableCredentialDto

fun Route.walletRoutes() {
    route("/wallets/{identifier}/credentials") {
        notarizedPost(
            PostInfo<StoreVerifiableCredentialParameter, IssuedVerifiableCredentialRequestDto, SuccessResponse>(
                summary = "Store Verifiable credential ",
                description = "store a verifiable credential using the subject DID as identifier of wallet",
                parameterExamples = setOf(
                    ParameterExample("identifier", "did", "did:exp:123"),
                    ParameterExample("identifier", "bpn", "BPN123"),
                ),
                requestInfo = RequestInfo(
                    description = "the verifiable credential",
                    examples = issuedVerifiableCredentialRequestDtoExample
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
                tags = setOf("Wallets")
            )
        ) {
            val verifiableCredentialDto = call.receive<VerifiableCredentialDto>()
            call.respond(
                HttpStatusCode.Created,
                SuccessResponse("Credential has been successfully Stored")
            )
        }
    }
}

@Serializable
data class StoreVerifiableCredentialParameter(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of Entity", name = "identifier")
    val identifier: String
)

val issuedVerifiableCredentialRequestDtoExample =  mapOf(
    "demo" to IssuedVerifiableCredentialRequestDto(
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
