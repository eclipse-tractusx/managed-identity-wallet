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

package org.eclipse.tractusx.managedidentitywallets

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import org.eclipse.tractusx.managedidentitywallets.routes.AuthorizationHandler
import org.eclipse.tractusx.managedidentitywallets.routes.Role
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

object JwtConfigTest {

    const val issuerUrl = "http://localhost:8081/auth/realms/localkeycloak"
    const val resourceId = "ManagedIdentityWallets"
    private const val validityInMs = 36_000_00 * 10 // 10 hours
    val kp: KeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()!!
    private val provider = object : RSAKeyProvider {
        override fun getPublicKeyById(kid: String): RSAPublicKey {
            return kp.public as RSAPublicKey
        }

        override fun getPrivateKey(): RSAPrivateKey {
            return kp.private as RSAPrivateKey
        }

        override fun getPrivateKeyId(): String {
            return "jEpf8fJRWA9Tc7muBqbCGgcqhhzFWIyDeL9GZAv8-zY"
        }

    }

    private val algorithm = Algorithm.RSA256(provider)

    private fun makeBaseToken(roles: List<Role>): JWTCreator.Builder = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuerUrl)
        .withAudience(resourceId)
        .withClaim("typ", "Bearer")
        .withClaim("resource_access", mapOf(resourceId to mapOf(AuthorizationHandler.ROLES to roles)))
        .withExpiresAt(getExpiration())

    fun makeToken(roles: List<Role>, bpn: String? = null): String {
        return if (!bpn.isNullOrEmpty()) {
            makeBaseToken(roles).withClaim("BPN", bpn).sign(algorithm)
        } else {
            makeBaseToken(roles).sign(algorithm)
        }
    }

    fun jwks(): String {
        val pubKey = kp.public as RSAPublicKey
        val modulus = Base64.getUrlEncoder().encodeToString(pubKey.modulus.toByteArray())
        val exponent = Base64.getUrlEncoder().encodeToString(pubKey.publicExponent.toByteArray())
        return """
{
  "keys": [
    {
      "kid": "jEpf8fJRWA9Tc7muBqbCGgcqhhzFWIyDeL9GZAv8-zY",
      "kty": "RSA",
      "alg": "RS256",
      "use": "sig",
      "n": "$modulus",
      "e": "$exponent",
      "x5c": [
        "MIICozCCAYsCBgGBI/qjTDANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApDWC1DZW50cmFsMB4XDTIyMDYwMjEwMzIxN1oXDTMyMDYwMjEwMzM1N1owFTETMBEGA1UEAwwKQ1gtQ2VudHJhbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJW41obiIA9Qn5mil8LtlUSGaWJ+qs6S368pQhYDvtdyftS8QJCvdFZb3h2HDqJl92L4JJ33rysY/wlMxSH0MMHYZZORMryDKnjGzL5V7/5a4BRmm691zr0Nizx7gRE9A4c2PL7MTFeTn7z8qxR0FpPs2jTaWhZMYFMZurrlfie1WAPttg1Fohs3ao8/T6LMdQAnIj0ahwj+E5MpCYx++4brMzvfzOmF4fmPPDKf0MXMVdjGCu1jVND1SygwJhn6qe+OaObT1KPFwzW0DguijYlgzFvKcL8eJ4U/pC929uUMaSiEyr0Qlrof+85MP9Fgtmj4qGMUiongWn66x6O6CMUCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEARcnTn/oSSp+V0si1bcjz/JLX5ctSbJMuP3HVnMZMuQ+LBzITZYjVpmlz3/ZckEcbv2hzEFOpuI7+/JSvidWi3+xwuUpukaQqEDmIP+KkH5bFlGbEYYJGgLtYmdHVOez5cO9GOC1eoXeatD30N4arRTOqSo9d79OVbZk3fEG4FJ+74LT1x80yUbI3pbKnfUIDlTtm5GZq2WN8axN82v5dnI6jVzkMGyj9f2DQUher2+eytsr0kmkU7xepsPj+LlzUFJMyF5CDBRy+jy/51ph4RdrRvkGtcXRlJYqvclc316x9B66wcZZJYR5n4iR5Yf3cZZZUWQo4QpDLPu055RE56g=="
      ],
      "x5t": "MpbgCqkBr47cjCY6d7zZbWw7qew",
      "x5t#S256": "eaXcPQWMazr102rZ5DfzxzRlDcppaYOfwnEptVaZDCs"
    }
  ]
}
"""
    }

    /**
     * Calculate the expiration Date based on current time + the given validity
     */
    private fun getExpiration() = Date(System.currentTimeMillis() + validityInMs)

}