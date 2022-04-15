package net.catenax.core.custodian

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.services.BusinessPartnerDataService

class BusinessPartnerDataMockedService: BusinessPartnerDataService {

    override suspend fun issueAndUpdateCatenaXCredentials(businessPartnerData: BusinessPartnerDataUpdateRequestDto) {

    }

    override suspend fun <T> issueAndStoreCatenaXCredentialsAsync(
        bpn: String,
        type: String,
        data: T?
    ): Deferred<Boolean> {
        return CompletableDeferred(true)
    }

}
