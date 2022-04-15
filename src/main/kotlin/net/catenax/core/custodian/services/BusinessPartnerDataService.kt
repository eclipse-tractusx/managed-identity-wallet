package net.catenax.core.custodian.services

import kotlinx.coroutines.Deferred
import net.catenax.core.custodian.models.BusinessPartnerDataUpdateRequestDto

interface BusinessPartnerDataService {

    suspend fun issueAndUpdateCatenaXCredentials(businessPartnerData: BusinessPartnerDataUpdateRequestDto)

    suspend fun<T> issueAndStoreCatenaXCredentialsAsync(
        bpn: String,
        type: String,
        data: T? = null
    ): Deferred<Boolean>

    companion object {
        fun createBusinessPartnerDataService(walletService: WalletService): BusinessPartnerDataService {
            return BusinessPartnerDataServiceImpl(walletService)
        }
    }
}
