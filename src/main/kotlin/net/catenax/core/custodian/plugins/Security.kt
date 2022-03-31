package net.catenax.core.custodian.plugins

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.client.*
import io.ktor.response.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.sessions.*

import com.auth0.jwt.*
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwk.*
import com.auth0.jwt.algorithms.*
import java.security.interfaces.*

import java.net.URL

data class UserSession(val token: String) : Principal

fun Application.configureSecurity() {

    val jwksUrl = environment.config.property("auth.jwksUrl").getString()
    val issuerUrl = environment.config.property("auth.issuerUrl").getString()
    val jwkRealm = environment.config.property("auth.realm").getString()
    val role = environment.config.property("auth.role").getString()
    val clientId = environment.config.property("auth.clientId").getString()
    val clientSecret = environment.config.property("auth.clientSecret").getString()
    val redirectUrl = environment.config.property("auth.redirectUrl").getString()
    val jwkProvider = UrlJwkProvider(URL(jwksUrl))

    val RESOURCE_ACCESS = "resource_access"
    val ROLES = "roles"

    val oauthProvider = OAuthServerSettings.OAuth2ServerSettings(
        name = "keycloak",
        authorizeUrl = "$issuerUrl/protocol/openid-connect/auth",
        accessTokenUrl = "$issuerUrl/protocol/openid-connect/token",
        clientId = clientId,
        clientSecret = clientSecret,
        accessTokenRequiresBasicAuth = false,
        requestMethod = HttpMethod.Post, // must POST to token endpoint
        defaultScopes = listOf(ROLES)
    )

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
                    val algorithm = Algorithm.RSA256(jwk.getPublicKey() as RSAPublicKey, null)
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

        // for the API use JWT validation 
        jwt("auth-jwt") {
            verifier(jwkProvider, issuerUrl)
            realm = jwkRealm
            validate {
                credentials -> 
                    if (credentials.payload.claims != null && credentials.payload.claims.contains(RESOURCE_ACCESS)) {
                        val clientResources = credentials.payload.claims.get(RESOURCE_ACCESS)!!.asMap().get(clientId)
                        if (clientResources != null && clientResources is Map<*, *> && clientResources.contains(ROLES)) {
                            val roles = clientResources.get(ROLES)
                            if (roles != null && roles is List<*> && roles.contains(role))
                                JWTPrincipal(credentials.payload)
                            else null
                        } else null
                    } else null
            }
        }
    }
}