/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the CatenaX (ng) GitHub Organisation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.managedidentitywallets.routes

import io.ktor.application.*
import kotlinx.serialization.Serializable
import org.eclipse.tractusx.managedidentitywallets.Services
import org.eclipse.tractusx.managedidentitywallets.plugins.AuthConstants
import org.slf4j.LoggerFactory

@Serializable
data class AuthorizationResponse(
    val valid: Boolean,
    val errorMsg: String? = null,
)

object AuthorizationHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun hasRightsToViewOwnWallet(call: ApplicationCall, identifier: String): AuthorizationResponse {
        return hasRightsForViewOrUpdateOwnWallet(call, identifier, AuthConstants.ROLE_VIEW_WALLET)
    }

    fun hasRightsToViewOwnCredentials(call: ApplicationCall, holderIdentifier: String?): AuthorizationResponse {
        return hasRightsForViewOrUpdateOwnWallet(call, holderIdentifier, AuthConstants.ROLE_VIEW_WALLET)
    }

    fun hasRightsToStoreCredential(call: ApplicationCall, holderIdentifier: String): AuthorizationResponse {
        return hasRightsForViewOrUpdateOwnWallet(call, holderIdentifier, AuthConstants.ROLE_UPDATE_WALLET)
    }

    fun hasRightToIssueCredential(call: ApplicationCall, issuerIdentifier: String): AuthorizationResponse {
        return hasRightsForViewOrUpdateOwnWallet(call, issuerIdentifier, AuthConstants.ROLE_UPDATE_WALLET)
    }

    fun hasRightToIssuePresentation(call: ApplicationCall, holderIdentifier: String): AuthorizationResponse {
        return hasRightsForViewOrUpdateOwnWallet(call, holderIdentifier, AuthConstants.ROLE_UPDATE_WALLET)
    }

    fun hasRightToTriggerUpdateOwnBPD(call: ApplicationCall, identifier: String?): AuthorizationResponse {
        return hasRightsForViewOrUpdateOwnWallet(call, identifier, AuthConstants.ROLE_UPDATE_WALLET)
    }

    private fun hasRightsForViewOrUpdateOwnWallet(
        call: ApplicationCall,
        identifier: String?,
        role: String
    ): AuthorizationResponse {
        val principal = AuthConstants.getPrincipal(call.attributes)
        if (principal == null || principal.role.isBlank()) {
            return AuthorizationResponse(false, "Authorization failed: Principal is null " +
                    "or it has an empty role")
        }
        if (principal.role == role) {
            if (identifier.isNullOrBlank()) {
                val errorMsg = "Authorization failed: The Identifier is mandatory " +
                        "for $role role"
                log.error(errorMsg)
                return AuthorizationResponse(false, errorMsg)
            }
            val bpnOfHolder = Services.walletService.getBpnFromIdentifier(identifier)
            return checkIfBpnMatchesToPrincipalBpn(bpnOfHolder, principal.bpn)
        }
        // reaching this line means that the entity has the Role of viewing or updating all wallets, and it is checked
        // by notarizedAuthenticate
        return AuthorizationResponse(true)
    }

    private fun checkIfBpnMatchesToPrincipalBpn(
        givenBpn: String,
        principalBpn: String?
    ) : AuthorizationResponse {
        return if (!principalBpn.isNullOrEmpty() && givenBpn == principalBpn) {
            log.debug("Authorization successful: Wallet BPN $givenBpn does match requestors BPN $principalBpn")
            AuthorizationResponse(true)
        } else {
            val errorMsg = "Authorization failed: Wallet BPN $givenBpn does not match requestors BPN $principalBpn"
            log.error(errorMsg)
            AuthorizationResponse(false, errorMsg)
        }
    }

}
