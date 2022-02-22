package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable


@Serializable
class LdProofDto(
    val creator: String?,
    val created: String,
    val domain: String? = null,
    val challenge: String? = null,
    val nonce: String? = null,
    val type: String,
    val proofPurpose: String,
    val verificationMethod: String,
    val proofValue: String? = null,
    val jws: String
) {

    constructor(created: String, type: String, proofPurpose: String, verificationMethod: String, jws: String)
            : this(
        creator = null,
        created,
        domain = null,
        challenge = null,
        nonce = null,
        type,
        proofPurpose,
        verificationMethod,
        proofValue = null,
        jws
    )
}
