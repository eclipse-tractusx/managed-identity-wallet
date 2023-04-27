package org.eclipse.tractusx.managedidentitywallets.services

import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CredentialOfferResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord

class AgentWalletServiceImpl:IWalletService {
    override fun getWallet(identifier: String, withCredentials: Boolean): WalletDto {
        TODO("Not yet implemented")
    }

    override fun getBaseWallet(): WalletExtendedData {
        TODO("Not yet implemented")
    }

    override fun getDidFromBpn(bpn: String): String {
        TODO("Not yet implemented")
    }

    override fun getBpnFromDid(did: String): String {
        TODO("Not yet implemented")
    }

    override fun getBpnFromIdentifier(identifier: String): String {
        TODO("Not yet implemented")
    }

    override fun getAll(): List<WalletDto> {
        TODO("Not yet implemented")
    }

    override fun getAllBpns(): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun createWallet(walletCreateDto: WalletCreateDto): WalletDto {
        TODO("Not yet implemented")
    }

    override suspend fun registerSelfManagedWalletAndBuildConnection(selfManagedWalletCreateDto: SelfManagedWalletCreateDto): SelfManagedWalletResultDto {
        TODO("Not yet implemented")
    }

    override suspend fun deleteWallet(identifier: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun storeCredential(identifier: String, issuedCredential: IssuedVerifiableCredentialRequestDto): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto {
        TODO("Not yet implemented")
    }

    override suspend fun issueBaseWalletCredential(vcBaseWalletRequest: VerifiableCredentialRequestWithoutIssuerDto): VerifiableCredentialDto {
        TODO("Not yet implemented")
    }

    override suspend fun triggerCredentialIssuanceFlow(vc: VerifiableCredentialIssuanceFlowRequest): CredentialOfferResponse {
        TODO("Not yet implemented")
    }

    override suspend fun resolveDocument(identifier: String): DidDocumentDto {
        TODO("Not yet implemented")
    }

    override suspend fun issuePresentation(
        vpRequest: VerifiablePresentationRequestDto,
        withCredentialsValidation: Boolean,
        withCredentialsDateValidation: Boolean,
        withRevocationValidation: Boolean
    ): VerifiablePresentationDto {
        TODO("Not yet implemented")
    }

    override fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?
    ): List<VerifiableCredentialDto> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCredential(credentialId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun addService(identifier: String, serviceDto: DidServiceDto) {
        TODO("Not yet implemented")
    }

    override suspend fun updateService(
        identifier: String,
        id: String,
        serviceUpdateRequestDto: DidServiceUpdateRequestDto
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteService(identifier: String, id: String): DidDocumentDto {
        TODO("Not yet implemented")
    }

    override suspend fun verifyVerifiablePresentation(
        vpDto: VerifiablePresentationDto,
        withDateValidation: Boolean,
        withRevocationValidation: Boolean
    ): VerifyResponse {
        TODO("Not yet implemented")
    }

    override suspend fun issueStatusListCredential(
        profileName: String,
        listCredentialRequestData: ListCredentialRequestData
    ): VerifiableCredentialDto {
        TODO("Not yet implemented")
    }

    override suspend fun revokeVerifiableCredential(vc: VerifiableCredentialDto) {
        TODO("Not yet implemented")
    }

    override fun setPartnerMembershipIssued(walletDto: WalletDto) {
        TODO("Not yet implemented")
    }

    override fun updateConnectionState(connectionId: String, rfc23State: String) {
        TODO("Not yet implemented")
    }

    override fun getConnection(connectionId: String): ConnectionDto? {
        TODO("Not yet implemented")
    }

    override fun getConnectionWithBaseWallet(theirDid: String): ConnectionDto? {
        TODO("Not yet implemented")
    }

    override fun addConnection(
        connectionId: String,
        connectionTargetDid: String,
        connectionOwnerDid: String,
        connectionState: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun initBaseWalletWithListeners(bpn: String, did: String, verkey: String, name: String) {

        return
        TODO("Not yet implemented")
    }

    override suspend fun acceptConnectionRequest(identifier: String, connectionRecord: ConnectionRecord) {
        TODO("Not yet implemented")
    }

    override suspend fun acceptReceivedOfferVc(identifier: String, credExRecord: V20CredExRecord) {
        TODO("Not yet implemented")
    }

    override suspend fun acceptAndStoreReceivedIssuedVc(identifier: String, credExRecord: V20CredExRecord) {
        TODO("Not yet implemented")
    }

    override suspend fun setEndorserMetaDataForConnection(connectionId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun setAuthorMetaData(walletId: String, connectionId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun sendInvitation(identifier: String, invitationRequestDto: InvitationRequestDto) {
        TODO("Not yet implemented")
    }

    override suspend fun setCommunicationEndpointUsingEndorsement(walletId: String) {
        TODO("Not yet implemented")
    }

    override fun validateConnectionRequestForManagedWallets(connection: ConnectionRecord): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun validateConnectionRequestForBaseWallet(connection: ConnectionRecord, bpn: String): WalletDto? {
        TODO("Not yet implemented")
    }
}