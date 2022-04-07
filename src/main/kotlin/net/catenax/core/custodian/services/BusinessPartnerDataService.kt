package net.catenax.core.custodian.services

import kotlinx.coroutines.Deferred

interface BusinessPartnerDataService {

    suspend fun issueAndStoreCatenaXCredentialsAsync(bpn: String, type: String): Deferred<Boolean>

    companion object {
        fun createBusinessPartnerDataService(walletService: WalletService): BusinessPartnerDataService {
            return BusinessPartnerDataServiceImpl(walletService)
        }
    }
}
