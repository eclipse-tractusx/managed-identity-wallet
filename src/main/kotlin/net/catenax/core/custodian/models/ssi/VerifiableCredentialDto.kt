@file:UseSerializers(AnySerializer::class)

package net.catenax.core.custodian.models.ssi

import com.apicatalog.jsonld.loader.DocumentLoaderOptions
import com.danubetech.verifiablecredentials.CredentialSubject
import com.danubetech.verifiablecredentials.VerifiableCredential
import com.danubetech.verifiablecredentials.validation.Validation
import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDUtils
import info.weboftrust.ldsignatures.LdProof
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.*
import net.catenax.core.custodian.plugins.AnySerializer

import java.net.URI

@Serializable
class VerifiableCredentialDto(
    val id: String,
    @JsonNames("@context") val context: List<String>,
    val type: List<String>,
    val issuer: String,
    var issuanceDate: String? = null, // In Rfc3339
    val expirationDate: String, // In Rfc3339
    val credentialSubject: Map<String, Any>,
    val proof: LdProofDto? = null
) {

    constructor(
        id: String,
        context: List<String>,
        type: List<String>,
        issuer: String,
        issuanceDate: String,
        expirationDate: String,
        credentialSubject: Map<String, Any>
    ) : this(id, context, type, issuer, issuanceDate, expirationDate, credentialSubject, null) {}

    init {
        // impl. related
        val credentialSubject = CredentialSubject.builder()
            .id(URI.create(this.id))
            .claims(this.credentialSubject)
            .build()
        val verifiableCredential = VerifiableCredential.builder();
        this.context.forEach { verifiableCredential.context(URI.create(it)) }
        this.type.forEach { verifiableCredential.type(it) }
        verifiableCredential.id(URI.create(this.id))
        verifiableCredential.issuer(URI.create(this.issuer))
        verifiableCredential.issuanceDate(JsonLDUtils.stringToDate(this.issuanceDate))
        verifiableCredential.expirationDate(JsonLDUtils.stringToDate(this.expirationDate))
        verifiableCredential.credentialSubject(credentialSubject)
        if (this.proof != null) {
            val ldProof = LdProof.builder()
                .type(this.proof.type)
                .proofPurpose(this.proof.proofPurpose)
                .created(JsonLDUtils.stringToDate(this.proof.created))
                .verificationMethod(URI.create(this.proof.verificationMethod))
                .jws(this.proof.jws).build()
            verifiableCredential.ldProof(ldProof)
        }
        var vc = verifiableCredential.build()
        var documentLoader = ConfigurableDocumentLoader()
        documentLoader.httpLoader = ConfigurableDocumentLoader.getDefaultHttpLoader()
        documentLoader.isEnableHttps = true
        documentLoader.isEnableLocalCache = true
        vc.contexts.forEach {
            documentLoader.loadDocument(it, DocumentLoaderOptions())
        }
        vc.documentLoader = documentLoader;
        Validation.validate(vc)
    }
}
