package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable


@Serializable
data class LdProofDto(
    val type: String,
    val created: String,
    val proofPurpose: String,
    val verificationMethod: String,
    val jws: String? = null,
    val proofValue: String? = null,
    val creator: String? = null,
    val domain: String? = null,
    val challenge: String? = null,
    val nonce: String? = null
) {

    constructor(created: String, type: String, proofPurpose: String, verificationMethod: String, jws: String)
            : this(
        type,
        created,
        proofPurpose,
        verificationMethod,
        jws,
        proofValue = null,
        creator = null,
        domain = null,
        challenge = null,
        nonce = null
    )
}
