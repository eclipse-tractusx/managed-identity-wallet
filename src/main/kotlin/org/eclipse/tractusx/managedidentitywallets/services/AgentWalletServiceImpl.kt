package org.eclipse.tractusx.managedidentitywallets.services

import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord

class AgentWalletServiceImpl:IWalletService {
    override fun getWallet(identifier: String, withCredentials: Boolean): WalletDto {
        TODO("Not yet implemented")
    }

    override fun getAuthorityWallet(): WalletExtendedData {
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

    override fun createWallet(walletCreateDto: WalletCreateDto): WalletDto {
        TODO("Not yet implemented")
    }

    override fun deleteWallet(identifier: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun storeCredential(identifier: String, issuedCredential: IssuedVerifiableCredentialRequestDto): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto {
        TODO("Not yet implemented")
    }

    override suspend fun issueAuthorityWalletCredential(vcBaseWalletRequest: VerifiableCredentialRequestWithoutIssuerDto): VerifiableCredentialDto {
        TODO("Not yet implemented")
    }

    override suspend fun resolveDocument(identifier: String): DidDocumentDto {
        TODO("Not yet implemented")
    }

    override suspend fun issuePresentation(
        vpRequest: VerifiablePresentationRequestDto,
        withCredentialsValidation: Boolean,
        withCredentialsDateValidation: Boolean,
        withRevocationValidation: Boolean,
        asJwt: Boolean
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
    ) {
        TODO("Not yet implemented")
    }

}