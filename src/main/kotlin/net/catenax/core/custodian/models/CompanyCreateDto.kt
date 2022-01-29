package net.catenax.core.custodian.models

import kotlinx.serialization.Serializable

@Serializable
data class CompanyCreateDto(val bpn: String, val name: String, val wallet: WalletCreateDto = WalletCreateDto.INVALID) {
    init {
        require(bpn.isNotBlank()) { "Field 'bpn' is required not to be blank, but it was blank" }
        require(name.isNotBlank()) { "Field 'name' is required not to be blank, but it was blank" }
    }
}