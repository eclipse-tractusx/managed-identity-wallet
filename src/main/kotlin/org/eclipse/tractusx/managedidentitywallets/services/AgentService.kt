package org.eclipse.tractusx.managedidentitywallets.services

import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.services.agent.IAgentService
import java.util.*

class AgentService: IAgentService {
    override fun createWallet() {
        TODO("Not yet implemented")
    }

    override suspend fun deleteSubWallet(walletData: WalletExtendedData) {
        TODO("Not yet implemented")
    }

    override suspend fun createWebDidForWallet(didCreateDto: Objects, token: String) {
        TODO("Not yet implemented")
    }

    override suspend fun resolveDidDoc(did: String, token: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun updateServiceOfAuthorityWallet(serviceEndPoint: Objects) {
        TODO("Not yet implemented")
    }

    override suspend fun isDidOfWallet(did: String, bpnOfWallet: String?): Boolean {
        TODO("Not yet implemented")
    }

}