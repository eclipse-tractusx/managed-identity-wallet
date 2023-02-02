/********************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import io.bkbn.kompendium.auth.configuration.JwtAuthConfiguration
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.util.*
import org.eclipse.tractusx.managedidentitywallets.Services
import org.eclipse.tractusx.managedidentitywallets.models.AuthorizationException
import org.eclipse.tractusx.managedidentitywallets.models.ForbiddenException
import org.eclipse.tractusx.managedidentitywallets.plugins.MIWPrincipal

typealias Role = String

object AuthorizationHandler {

    const val RESOURCE_ACCESS = "resource_access"
    const val ROLES = "roles"
    const val ROLE_CREATE_WALLETS = "create_wallets"
    const val ROLE_UPDATE_WALLETS = "update_wallets"
    const val ROLE_VIEW_WALLETS = "view_wallets"
    const val ROLE_DELETE_WALLETS = "delete_wallets"
    const val ROLE_UPDATE_WALLET = "update_wallet"
    const val ROLE_VIEW_WALLET = "view_wallet"

    const val CONFIG_TOKEN = "auth-token"

    private val view_roles = setOf(ROLE_VIEW_WALLET, ROLE_VIEW_WALLETS)
    private val update_roles = setOf(ROLE_UPDATE_WALLET, ROLE_UPDATE_WALLETS)
    private val create_role = setOf(ROLE_CREATE_WALLETS)
    private val delete_role = setOf(ROLE_DELETE_WALLETS)

    private val rolePermissionMap = mutableMapOf(
        ROLE_CREATE_WALLETS to ROLE_CREATE_WALLETS,
        ROLE_UPDATE_WALLETS to ROLE_UPDATE_WALLETS,
        ROLE_VIEW_WALLETS to ROLE_VIEW_WALLETS,
        ROLE_DELETE_WALLETS to ROLE_DELETE_WALLETS,
        ROLE_UPDATE_WALLET to ROLE_UPDATE_WALLET,
        ROLE_VIEW_WALLET to ROLE_VIEW_WALLET
    )

    val JWT_AUTH_TOKEN = object : JwtAuthConfiguration {
        override val name: String = CONFIG_TOKEN
    }

    fun checkHasRightToCreateWallets(call: ApplicationCall) = checkHasAnyRolesOf(call, create_role)

    fun checkHasRightToDeleteWallets(call: ApplicationCall) = checkHasAnyRolesOf(call, delete_role)

    fun checkHasAnyViewRoles(call: ApplicationCall) = checkHasAnyRolesOf(call, view_roles)

    fun checkHasRightsToViewWallet(
        call: ApplicationCall,
        identifier: String? = null
    ) {
        val principal = checkHasAnyRolesOf(call, view_roles)
        if (!principal.roles.contains(rolePermissionMap[ROLE_VIEW_WALLETS])
             && principal.roles.contains(rolePermissionMap[ROLE_VIEW_WALLET])) {
            return checkIfBpnMatchesToPrincipalBpn(identifier, principal, ROLE_VIEW_WALLET)
        }
    }

    fun checkHasRightsToUpdateWallet(
        call: ApplicationCall,
        identifier: String? = null
    ) {
        val principal = checkHasAnyRolesOf(call, update_roles)
        if (!principal.roles.contains(rolePermissionMap[ROLE_UPDATE_WALLETS])
            && principal.roles.contains(rolePermissionMap[ROLE_UPDATE_WALLET])) {
            return checkIfBpnMatchesToPrincipalBpn(identifier, principal, ROLE_UPDATE_WALLET)
        }
    }

    fun setRolePermissionMapping(mapping: Map<String, String>) {
        rolePermissionMap += mapping
    }

    fun getPermissionOfRole(role: Role): String {
       return rolePermissionMap[role].toString()
    }

    private fun checkIfBpnMatchesToPrincipalBpn(
        identifier: String?,
        principal: MIWPrincipal,
        role: Role
    ) {
        if (identifier.isNullOrBlank()) {
            throw AuthorizationException("Authorization failed: The Identifier is mandatory for $role role")
        }
        val bpnOfHolder = Services.walletService.getBpnFromIdentifier(identifier)
        if (principal.bpn.isNullOrEmpty() || bpnOfHolder != principal.bpn) {
            throw ForbiddenException("Wallet BPN $bpnOfHolder does not match requestors BPN ${principal.bpn}")
        }
    }

    private fun checkHasAnyRolesOf(call: ApplicationCall, requiredRoles: Set<Role>): MIWPrincipal {
        val principal = getPrincipalFromPayload(call)
        if (principal == null || principal.roles.isEmpty()) {
            throw AuthorizationException( "Authorization failed: Principal is null or it has an empty role")
        }
        val roles: Set<Role> = principal.roles
        if (requiredRoles.none { rolePermissionMap[it] in roles }) {
            throw ForbiddenException(
                "It has none of the sufficient role(s) ${ requiredRoles.joinToString( " or " ) }"
            )
        }
        return principal
    }

    private fun getPrincipalFromPayload(call: ApplicationCall): MIWPrincipal? {
        val attributes = call.attributes
        val authContextKey = attributes.allKeys.firstOrNull { it.name == "AuthContext" } ?: return null
        val authContext = attributes[authContextKey as AttributeKey<AuthenticationContext>]
        return authContext.principal as MIWPrincipal
    }

}
