package net.catenax.core.custodian.models

import kotlinx.serialization.Serializable

@Serializable
data class CompanyDto(val bpn: String, val name: String, val wallet: WalletDto = WalletDto.INVALID) {
    init {
        require(bpn.isNotBlank()) { "Field 'bpn' is required not to be blank, but it was blank" }
        require(name.isNotBlank()) { "Field 'name' is required not to be blank, but it was blank" }
    }
}