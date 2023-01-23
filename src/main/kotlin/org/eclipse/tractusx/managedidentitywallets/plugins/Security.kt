/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.managedidentitywallets.plugins


import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.sessions.*
import org.eclipse.tractusx.managedidentitywallets.routes.AuthorizationHandler
import java.net.URL
import java.security.interfaces.RSAPublicKey

data class UserSession(val token: String) : Principal

typealias Role = String

class MIWPrincipal(val roles: Set<Role>, val bpn: String?) : Principal

fun Application.configureSecurity() {

    val jwksUrl = environment.config.property("auth.jwksUrl").getString()
    val issuerUrl = environment.config.property("auth.issuerUrl").getString()
    val jwkRealm = environment.config.property("auth.realm").getString()
    val roleMappings = environment.config.property("auth.roleMappings").getString()
    val resourceId = environment.config.property("auth.resourceId").getString()
    val clientId = environment.config.property("auth.clientId").getString()
    val clientSecret = environment.config.property("auth.clientSecret").getString()
    val redirectUrl = environment.config.property("auth.redirectUrl").getString()
    val jwkProvider = UrlJwkProvider(URL(jwksUrl))

    val rolePermissionMap = roleMappings.split(",")
        .associate { it.split(":")[0] to it.split(":")[1] }

    val oauthProvider = OAuthServerSettings.OAuth2ServerSettings(
        name = "keycloak",
        authorizeUrl = "$issuerUrl/protocol/openid-connect/auth",
        accessTokenUrl = "$issuerUrl/protocol/openid-connect/token",
        clientId = clientId,
        clientSecret = clientSecret,
        accessTokenRequiresBasicAuth = false,
        requestMethod = HttpMethod.Post, // must POST to token endpoint
        defaultScopes = listOf(AuthorizationHandler.ROLES)
    )

    fun getPrincipalFromPayload(credentials: JWTCredential): MIWPrincipal? {
        if (credentials.payload.claims != null && credentials.payload.claims.contains(AuthorizationHandler.RESOURCE_ACCESS)) {
            val clientResources = credentials.payload.claims[AuthorizationHandler.RESOURCE_ACCESS]!!.asMap()[resourceId]
            return if (clientResources != null && clientResources is Map<*, *> && clientResources.contains(AuthorizationHandler.ROLES)) {
                val roles = (clientResources[AuthorizationHandler.ROLES] as ArrayList<Role>).toSet()
                if (roles.isNotEmpty())
                    MIWPrincipal(roles = roles, bpn = credentials.payload.claims["BPN"]?.asString())
                else {
                    log.warn("Authentication information incomplete: missing roles")
                    null
                }
            } else {
                log.warn("Authentication information incomplete: missing ${AuthorizationHandler.ROLES} for $resourceId")
                null
            }
        } else {
            log.warn("Authentication information incomplete: missing ${AuthorizationHandler.RESOURCE_ACCESS}")
            return null
        }
    }

    install(Sessions) {
        cookie<UserSession>("user_session")
        // potentially header<UserSession>("user_session")
    }

    install(Authentication) {

        // for the ui use oauth
        oauth("auth-ui") {
            client = HttpClient(Apache)
            providerLookup = { oauthProvider }
            urlProvider = {
                redirectUrl
            }
        }

        session<UserSession>("auth-ui-session") {
            validate {
                try {
                    val decoded = JWT.decode(it.token)
                    val kid = decoded.keyId
                    val jwk = jwkProvider.get(kid)
                    val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)
                    val verifier = JWT.require(algorithm).withIssuer(issuerUrl).build()
                    verifier.verify(it.token)
                    it
                } catch (ex:Exception) {
                    log.warn("Authentication information validation error: " + ex.message)
                    null
                }
            }
            challenge {
                call.sessions.clear<UserSession>()
                call.respondRedirect("/login")
            }
        }

        // verify that all mappings are there
        if (rolePermissionMap[AuthorizationHandler.ROLE_VIEW_WALLETS] == null) {
            log.error("Configuration error, ${AuthorizationHandler.ROLE_VIEW_WALLETS} role mapping not defined, system will not behave correctly!")
            throw Exception("Configuration error, ${AuthorizationHandler.ROLE_VIEW_WALLETS} role mapping not defined, system will not behave correctly!")
        }
        if (rolePermissionMap[AuthorizationHandler.ROLE_CREATE_WALLETS] == null) {
            log.error("Configuration error, ${AuthorizationHandler.ROLE_CREATE_WALLETS} role mapping not defined, system will not behave correctly!")
            throw Exception("Configuration error, ${AuthorizationHandler.ROLE_CREATE_WALLETS} role mapping not defined, system will not behave correctly!")
        }
        if (rolePermissionMap[AuthorizationHandler.ROLE_UPDATE_WALLETS] == null) {
            log.error("Configuration error, ${AuthorizationHandler.ROLE_UPDATE_WALLETS} role mapping not defined, system will not behave correctly!")
            throw Exception("Configuration error, ${AuthorizationHandler.ROLE_UPDATE_WALLETS} role mapping not defined, system will not behave correctly!")
        }
        if (rolePermissionMap[AuthorizationHandler.ROLE_DELETE_WALLETS] == null) {
            log.error("Configuration error, ${AuthorizationHandler.ROLE_DELETE_WALLETS} role mapping not defined, system will not behave correctly!")
            throw Exception("Configuration error, ${AuthorizationHandler.ROLE_DELETE_WALLETS} role mapping not defined, system will not behave correctly!")
        }
        if (rolePermissionMap[AuthorizationHandler.ROLE_UPDATE_WALLET] == null) {
            log.error("Configuration error, ${AuthorizationHandler.ROLE_UPDATE_WALLET} role mapping not defined, system will not behave correctly!")
            throw Exception("Configuration error, ${AuthorizationHandler.ROLE_UPDATE_WALLET} role mapping not defined, system will not behave correctly!")
        }
        if (rolePermissionMap[AuthorizationHandler.ROLE_VIEW_WALLET] == null) {
            log.error("Configuration error, ${AuthorizationHandler.ROLE_VIEW_WALLET} role mapping not defined, system will not behave correctly!")
            throw Exception("Configuration error, ${AuthorizationHandler.ROLE_VIEW_WALLET} role mapping not defined, system will not behave correctly!")
        }

        AuthorizationHandler.setRolePermissionMapping(rolePermissionMap)

        // verify that all mappings are there
        jwt(AuthorizationHandler.CONFIG_TOKEN) {
            verifier(jwkProvider, issuerUrl)
            realm = jwkRealm
            validate {
                    credentials -> getPrincipalFromPayload(credentials)
            }
        }

    }
}