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
import net.catenax.core.custodian.models.ssi.*

fun Route.walletRoutes() {
    route("/wallets/{identifier}") {

        route("/credentials") {
            notarizedPost(
                PostInfo<StoreVerifiableCredentialParameter, IssuedVerifiableCredentialRequestDto, SuccessResponse>(
                    summary = "Store Verifiable Credential ",
                    description = "Store a verifiable credential in the wallet of the given identifier",
                    parameterExamples = setOf(
                        ParameterExample("identifier", "did", "did:exp:123"),
                        ParameterExample("identifier", "bpn", "BPN123"),
                    ),
                    requestInfo = RequestInfo(
                        description = "The verifiable credential to be stored",
                        examples = issuedVerifiableCredentialRequestDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "Success message",
                        examples = mapOf(
                            "demo" to SuccessResponse(
                                "Credential with id http://example.edu/credentials/3732" +
                                        "has been successfully stored"
                            )
                        )
                    ),
                    canThrow = setOf(semanticallyInvalidInputException, notFoundException),
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

        route("/signatures") {
            notarizedPost(
                PostInfo<SignMessageParameter, SignMessageDto, SignMessageResponseDto>(
                    summary = "Sign Message",
                    description = "Sign a message using the wallet of the given identifier",
                    requestInfo = RequestInfo(
                        description = "the message to sign and the wallet did",
                        examples = mapOf("demo" to SignMessageDto(
                            message = "message_string")
                        )
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "The signed message response",
                        examples = mapOf(
                            "demo" to SignMessageResponseDto(
                                "did:example", "message_string",
                                "signed_message_hex", "public_key_base58"
                            )
                        )
                    ),
                    canThrow = setOf(notFoundException),
                    tags = setOf("Wallets")
                )
            ) {
                val signMessageParameter = call.receive<SignMessageParameter>()
                val signMessageDto = call.receive<SignMessageDto>()
                val response = SignMessageResponseDto(
                    identifier = signMessageParameter.identifier,
                    message = signMessageDto.message,
                    signedMessageInHex = "0x123....",
                    publicKeyBase58 = "FyfKP2HvTKqDZQzvyL38yXH7bExmwofxHf2NR5BrcGf1"
                )
                call.respond(HttpStatusCode.Created, response)
            }
        }
    }
}

@Serializable
data class StoreVerifiableCredentialParameter(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of the credential holder. The DID must match to the id of the credential subject if present.",
        name = "identifier")
    val identifier: String
)

@Serializable
data class SignMessageParameter(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of the signer.", name = "identifier")
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
