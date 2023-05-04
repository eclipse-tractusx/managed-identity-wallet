package org.eclipse.tractusx.managedidentitywallets.services.agent

import io.ktor.client.*
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiablePresentationDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.*

/**
 * The IAcaPyService interface describes the functionalities that are required
 * for interacting with the AcaPy API to perform various actions.
 */
interface IAgentService {

    /**
     * Creates a new sub-wallet.
     * @param subWallet the data of the sub-wallet to create
     * @return [CreatedSubWalletResult] the created sub wallet
     */
    suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult

    fun createWallet()

    /**
     * Deletes an existing sub-wallet.
     * @param walletData the data of the wallet to delete. It requires the walletId and walletKey
     */
    suspend fun deleteSubWallet(walletData: WalletExtendedData)

    /**
     * Creates an Did-Document for an Tenant Wallet
     * @param didCreateDto the method and option to create the DID
     * @param token the token of the wallet
     * @return [DidResult] the response including the DID and its verkey
     */
    suspend fun createWebDidForWallet(didCreateDto: DidCreate, token: String): DidResult

    /**
     * Registers a DID and its verkey on ledger using base wallet.
     * @param didRegistration the registration data
     * @return [DidRegistrationResult] the response including the status of the request
     */
    suspend fun registerDidOnLedgerUsingBaseWallet(didRegistration: DidRegistration): DidRegistrationResult

    /**
     * Signs a given Json-ld document.
     * @param signRequest the Json-ld document to sign. It can be
     * of type [VerifiableCredentialDto]  or [VerifiablePresentationDto]
     * @param token the token for managed wallet, null for the base wallet
     * @return the signed Json-ld Document as String
     */
    suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String?): String

    /**
     * Verifies a given Json-ld document.
     * @param verifyRequest the Json-ld document to verify. It can be
     * of type [VerifiableCredentialDto]  or [VerifiablePresentationDto]
     * @param token the token for managed wallet, null for the base wallet
     * @return [VerifyResponse] the verify response
     */
    suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String?): VerifyResponse

    /**
     * Resolves a DID and retrieves its document.
     * @param did the DID to resolve
     * @param token the token for managed wallet, null for the base wallet or external DIDs
     * @return [ResolutionResult] the result of the resolution
     */
    suspend fun resolveDidDoc(did: String, token: String?): ResolutionResult

    /**
     * Updates the service endpoint of the base wallet.
     * @param serviceEndPoint DidEndpointWithType containing the information of the service endpoint
     */
    suspend fun updateServiceOfAuthorityWallet(serviceEndPoint: DidEndpointWithType)

    /**
     * Checks if the DID belongs to the wallet.
     * @param did the DID to check
     * @param tokenOfWallet the token of the wallet that is supposed to be the owner of the DID. null for the base wallet.
     * @return true if the DID belong to the wallet. otherwise false
     */
    suspend fun isDidOfWallet(did: String, tokenOfWallet: String?): Boolean
}