package org.eclipse.tractusx.managedidentitywallets.services.agent

import io.ktor.client.*
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiablePresentationDto
import java.util.Objects

/**
 * The IAcaPyService interface describes the functionalities that are required
 * for interacting with the AcaPy API to perform various actions.
 */
interface IAgentService {


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
    suspend fun createWebDidForWallet(didCreateDto: Objects, token: String)
/*
    /**
     * Signs a given Json-ld document.
     * @param signRequest the Json-ld document to sign. It can be
     * of type [VerifiableCredentialDto]  or [VerifiablePresentationDto]
     * @param token the token for managed wallet, null for the base wallet
     * @return the signed Json-ld Document as String
     */
    suspend fun <T> signJsonLd(signRequest: Objects<T>, token: String?): String

    /**
     * Verifies a given Json-ld document.
     * @param verifyRequest the Json-ld document to verify. It can be
     * of type [VerifiableCredentialDto]  or [VerifiablePresentationDto]
     * @param token the token for managed wallet, null for the base wallet
     * @return [VerifyResponse] the verify response
     */
    suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String?): VerifyResponse
*/

    /**
     * Resolves a DID and retrieves its document.
     * @param did the DID to resolve
     * @param token the token for managed wallet, null for the base wallet or external DIDs
     */
    suspend fun resolveDidDoc(did: String, token: String?)

    /**
     * Updates the service endpoint of the base wallet.
     */
    suspend fun updateServiceOfAuthorityWallet(serviceEndPoint: Objects)

    /**
     * Checks if the DID belongs to the wallet.
     * @param did the DID to check
     * @param tokenOfWallet the token of the wallet that is supposed to be the owner of the DID. null for the base wallet.
     * @return true if the DID belong to the wallet. otherwise false
     */
    suspend fun isDidOfWallet(did: String, bpnOfWallet: String?): Boolean
}