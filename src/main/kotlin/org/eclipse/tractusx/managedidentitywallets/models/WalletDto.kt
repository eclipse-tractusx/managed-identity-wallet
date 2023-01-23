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

package org.eclipse.tractusx.managedidentitywallets.models

import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.Serializable
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.plugins.LocalDateTimeAsStringSerializer
import java.time.LocalDateTime

@Serializable
data class WalletDto(
    val name: String,
    val bpn: String,
    val did: String,
    val verKey: String? = null,
    @Serializable(with = LocalDateTimeAsStringSerializer::class) val createdAt: LocalDateTime,
    val vcs: List<VerifiableCredentialDto>,
    val revocationListName: String? = null,
    val pendingMembershipIssuance: Boolean,
    val isSelfManaged: Boolean = false
) {

    init {
        require(did.isNotBlank()) { "Field 'did' is required not to be blank, but it was blank" }
        require(bpn.isNotBlank()) { "Field 'bpn' is required not to be blank, but it was blank" }
        require(name.isNotBlank()) { "Field 'name' is required not to be blank, but it was blank" }
    }
}

// for documentation
@Serializable
data class StoreVerifiableCredentialParameter(
    @Param(type = ParamType.PATH)
    @Field(
        description = "The DID or BPN of the credential holder. The DID must match to the id of the credential subject if present.",
        name = "identifier"
    )
    val identifier: String
)

@Serializable
data class WalletDtoParameter(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of the Wallet", name = "identifier")
    val identifier: String,
    @Param(type = ParamType.QUERY)
    @Field(
        description = "Flag whether all stored credentials of the wallet should be included in the response",
        name = "withCredentials"
    )
    val withCredentials: Boolean
)

@Serializable
data class SelfManagedWalletResultDto(
    val name: String,
    val bpn: String,
    val did: String,
    @Serializable(with = LocalDateTimeAsStringSerializer::class) val createdAt: LocalDateTime,
)

@Serializable
data class ConnectionDto(
    val connectionId: String,
    val theirDid: String,
    val myDid: String,
    val state: String
)
