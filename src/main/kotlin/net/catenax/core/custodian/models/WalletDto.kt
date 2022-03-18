package net.catenax.core.custodian.models

import java.time.LocalDateTime
import net.catenax.core.custodian.plugins.*

import kotlinx.serialization.Serializable
import net.catenax.core.custodian.models.ssi.VerifiableCredentialDto

@Serializable
data class WalletDto(
    val name: String,
    val bpn: String,
    val did: String,
    @Serializable(with = LocalDateTimeAsStringSerializer::class) val createdAt: LocalDateTime,
    @Serializable(with = StringListSerializer::class) val vcs: List<VerifiableCredentialDto>
) {

    init {
        require(did.isNotBlank()) { "Field 'did' is required not to be blank, but it was blank" }
        require(bpn.isNotBlank()) { "Field 'bpn' is required not to be blank, but it was blank" }
        require(name.isNotBlank()) { "Field 'name' is required not to be blank, but it was blank" }
    }
}
