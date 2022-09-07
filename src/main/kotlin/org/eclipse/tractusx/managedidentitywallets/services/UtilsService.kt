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

package org.eclipse.tractusx.managedidentitywallets.services

import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.*
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.util.*
import java.util.zip.GZIPInputStream

class UtilsService(private val networkIdentifier: String) {

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val arrayOfSupportedIds = listOf("did-communication", "linked_domains", "profile")

    fun isDID(identifier: String) : Boolean = identifier.startsWith("did:")

    fun createRandomString(): String {
        return (1..25)
            .map { SecureRandom().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun getIdentifierOfDid(did: String): String {
        val elementsOfDid: List<String> = did.split(":")
        return elementsOfDid[elementsOfDid.size - 1]
    }

    fun mapServiceTypeToEnum(type: String): String = when (type) {
        "did-communication" -> EndPointType.Endpoint.name
        "linked_domains" -> EndPointType.LinkedDomains.name
        "profile" -> EndPointType.Profile.name
        else -> throw NotImplementedException("Service type $type is not supported")
    }

    fun checkSupportedId(id: String) {
        if (!arrayOfSupportedIds.contains(id)) {
            throw NotImplementedException("The Id $id of the service is not supported")
        }
    }

    fun replaceSovWithNetworkIdentifier(input: String): String =
        input.replace(":sov:", ":indy:$networkIdentifier:")

    fun replaceNetworkIdentifierWithSov(input: String): String =
        input.replace(":indy:$networkIdentifier:", ":sov:")

    fun checkIndyDid(did: String) {
        val regex = """did:indy:$networkIdentifier:.[^-\s]{16,}${'$'}""".toRegex()
        if (!regex.matches(did)) {
            throw UnprocessableEntityException("The DID must be a valid and supported Indy DID")
        }
    }

    fun decodeBitset(encoded: String): BitSet {
        val unzipped = decodeBytes(encoded)
        return BitSet.valueOf(unzipped)
    }

    private fun decodeBytes(encoded: String): ByteArray {
        val rawBytes = Base64.getDecoder().decode(encoded)
        return ByteArrayInputStream(rawBytes).run {
            GZIPInputStream(this).use {
                it.readBytes()
            }
        }
    }

}
