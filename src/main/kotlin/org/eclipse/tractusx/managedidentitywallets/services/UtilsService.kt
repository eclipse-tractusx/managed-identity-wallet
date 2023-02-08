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

package org.eclipse.tractusx.managedidentitywallets.services

import org.eclipse.tractusx.managedidentitywallets.models.NotImplementedException
import org.eclipse.tractusx.managedidentitywallets.models.UnprocessableEntityException
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.EndPointType
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.util.*
import java.util.zip.GZIPInputStream

/**
 * The UtilsService provides utility functionalities that can be used by all services.
 */
class UtilsService(private val networkIdentifier: String) {

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val arrayOfSupportedIds = listOf("did-communication", "linked_domains", "profile")

    /**
     * Checks if the given identifier starts with a "did:" indicating it is a DID.
     * @param identifier the string to be checked
     * @return true if the given string starts with "did:", false otherwise
     */
    fun isDID(identifier: String) : Boolean = identifier.startsWith("did:")

    /**
     * Generates a random string of length 25 characters.
     * @return the generated random string
     */
    fun createRandomString(): String {
        return (1..25)
            .map { SecureRandom().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    /**
     * Extracts the identifier part of the given DID.
     * @param did the DID as string
     * @return the identifier part of the given DID
     */
    fun getIdentifierOfDid(did: String): String {
        val elementsOfDid: List<String> = did.split(":")
        return elementsOfDid[elementsOfDid.size - 1]
    }

    /**
     * Maps the given service type to its corresponding enum [EndPointType] name.
     * @param type the service type as string
     * @return the enum name of the given service type
     * @throws NotImplementedException if the given service type is not supported
     */
    fun mapServiceTypeToEnum(type: String): String = when (type) {
        "did-communication" -> EndPointType.Endpoint.name
        "linked_domains" -> EndPointType.LinkedDomains.name
        "profile" -> EndPointType.Profile.name
        else -> throw NotImplementedException("Service type $type is not supported")
    }

    /**
     * Checks if the given Id of services in DID document is supported.
     * @param id the Id of the service to be checked
     * @throws NotImplementedException if the given id is not supported
     */
    fun checkSupportedServiceId(id: String) {
        if (!arrayOfSupportedIds.contains(id)) {
            throw NotImplementedException("The Id $id of the service is not supported")
        }
    }

    /**
     * Gets the prefix of DID `did:sov`.
     * @return The prefix of DID `did:sov`
     */
    fun getDidMethodPrefixWithNetworkIdentifier(): String {
        //TODO replace implementation when indy is supported by AcaPy
        //return "did:indy:$networkIdentifier:"
        return "did:sov:"
    }

    /**
     * Gets the prefix of DID `did:indy` with the configured network identifier.
     * @return The prefix of DID `did:indy` with the configured network identifier
     */
    fun getOldDidMethodPrefixWithNetworkIdentifier(): String {
        //TODO replace implementation when indy is supported by AcaPy
        return "did:indy:$networkIdentifier:"
    }

    /**
     * Replaces the `sov` in input with the `indy` and configured network identifier.
     * @param input The input string, expected to be a SOV method
     * @return The input string where `sov` replaced by the configured the network identifier
     */
    fun replaceSovWithNetworkIdentifier(input: String): String {
        //TODO check if this method is needed when indy is supported by AcaPy
        //input.replace(":sov:", ":indy:$networkIdentifier:")
        return input
    }

    /**
     * Replaces `indy` and the configured network identifier in the input with `sov`.
     * @param input The input string that contains the network identifier to be replaced
     * @return The input string with  `indy` and the network identifier replaced by `sov`
     */
    fun replaceNetworkIdentifierWithSov(input: String): String {
        //TODO check if this method is needed when indy is supported by AcaPy
        // replacing always because of AcaPys limitations
        return input.replace(":indy:$networkIdentifier:", ":sov:")
    }

    /**
     * Checks if the DID is a valid and supported Indy DID.
     * @param did The DID as string
     * @throws UnprocessableEntityException If the DID is not a valid and supported Indy DID
     */
    fun checkIndyDid(did: String) {
        // allow old and new DID methods to accomodate migrated scenarios
        val regex = """(${getDidMethodPrefixWithNetworkIdentifier()}|${getOldDidMethodPrefixWithNetworkIdentifier()})[^-\s]{16,}[^-\s]*${'$'}""".toRegex()
        if (!regex.matches(did)) {
            throw UnprocessableEntityException(
                "The DID must be a valid and supported DID: ${getDidMethodPrefixWithNetworkIdentifier()} " +
                        "or ${getOldDidMethodPrefixWithNetworkIdentifier()}")
        }
    }

    /**
     * Decodes the encoded bitset string.
     * @param encoded The encoded bitset string
     * @return The decoded bitset as a BitSet object
     */
    fun decodeBitset(encoded: String): BitSet {
        val unzipped = decodeBytes(encoded)
        return BitSet.valueOf(unzipped)
    }

    /**
     * Decodes the encoded byte array.
     * @param encoded The encoded byte array string
     * @return The decoded byte array
     */
    private fun decodeBytes(encoded: String): ByteArray {
        val rawBytes = Base64.getDecoder().decode(encoded)
        return ByteArrayInputStream(rawBytes).run {
            GZIPInputStream(this).use {
                it.readBytes()
            }
        }
    }

    /**
     * Converts a short DID to a full DID if necessary.
     * @param did the DID as string
     * @return a full DID if the input is a short DID, the input otherwise
     */
    fun convertToFullDidIfShort(did: String): String {
        return if (!did.startsWith(getOldDidMethodPrefixWithNetworkIdentifier())
            && !did.startsWith(getDidMethodPrefixWithNetworkIdentifier())
        ) {
            getDidMethodPrefixWithNetworkIdentifier() + did
        } else {
            did
        }
    }

}
