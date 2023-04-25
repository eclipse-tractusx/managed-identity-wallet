package org.eclipse.tractusx.managedidentitywallets.services

import org.eclipse.tractusx.managedidentitywallets.models.SelfManagedWalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialIssuanceFlowRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.*
import org.hyperledger.acy_py.generated.model.TransactionJobs
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord

class AgentService: IAgentService {
    override fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig {
        TODO("Not yet implemented")
    }

    override suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult {
        TODO("Not yet implemented")
    }

    override suspend fun deleteSubWallet(walletData: WalletExtendedData) {
        TODO("Not yet implemented")
    }

    override suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse {
        TODO("Not yet implemented")
    }

    override suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult {
        TODO("Not yet implemented")
    }

    override suspend fun registerDidOnLedgerUsingBaseWallet(didRegistration: DidRegistration): DidRegistrationResult {
        TODO("Not yet implemented")
    }

    override suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String?): String {
        TODO("Not yet implemented")
    }

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String?): VerifyResponse {
        TODO("Not yet implemented")
    }

    override suspend fun resolveDidDoc(did: String, token: String?): ResolutionResult {
        TODO("Not yet implemented")
    }

    override suspend fun updateServiceOfBaseWallet(serviceEndPoint: DidEndpointWithType) {
        TODO("Not yet implemented")
    }

    override suspend fun updateServiceUsingEndorsement(serviceEndPoint: DidEndpointWithType, token: String) {
        TODO("Not yet implemented")
    }

    override fun subscribeBaseWalletForWebSocket() {
        TODO("Not yet implemented")
    }

    override suspend fun getAcapyClient(walletToken: String?): AriesClient {
        TODO("Not yet implemented")
    }

    override suspend fun sendConnectionRequest(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto,
        token: String?
    ): ConnectionRecord {
        TODO("Not yet implemented")
    }

    override suspend fun sendConnectionRequest(
        didOfTheirWallet: String,
        usePublicDid: Boolean,
        alias: String?,
        token: String?,
        label: String?
    ): ConnectionRecord {
        TODO("Not yet implemented")
    }

    override suspend fun issuanceFlowCredentialSend(
        token: String?,
        vc: VerifiableCredentialIssuanceFlowRequest
    ): V20CredExRecord {
        TODO("Not yet implemented")
    }

    override suspend fun deleteConnection(connectionId: String, token: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun acceptConnectionRequest(connectionId: String, token: String?): ConnectionRecord {
        TODO("Not yet implemented")
    }

    override suspend fun acceptCredentialOfferBySendingRequest(
        holderDid: String,
        credentialExchangeId: String,
        token: String?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun acceptCredentialReceivedByStoringIssuedCredential(
        credentialId: String,
        credentialExchangeId: String,
        token: String?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getRequestedConnectionsToBaseWallet(): List<ConnectionRecord> {
        TODO("Not yet implemented")
    }

    override suspend fun setEndorserMetaData(connectionId: String): TransactionJobs? {
        TODO("Not yet implemented")
    }

    override suspend fun setAuthorRoleAndInfoMetaData(connectionId: String, endorserDID: String, token: String) {
        TODO("Not yet implemented")
    }

    override suspend fun setDidAsPublicUsingEndorser(did: String, token: String) {
        TODO("Not yet implemented")
    }

    override suspend fun isDidOfWallet(did: String, tokenOfWallet: String?): Boolean {
        TODO("Not yet implemented")
    }
}