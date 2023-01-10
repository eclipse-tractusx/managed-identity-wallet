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

package org.eclipse.tractusx.managedidentitywallets.persistence.repositories

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.ssi.IssuedVerifiableCredentialRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class CredentialRepository {

    fun deleteCredentialsOfWallet(walletId: Int) {
        VerifiableCredentials.deleteWhere { VerifiableCredentials.walletId eq walletId }
    }

    fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?
    ): List<VerifiableCredentialDto> = transaction {
        val query = VerifiableCredentials.selectAll()
        issuerIdentifier?.let {
            query.andWhere { VerifiableCredentials.issuerDid eq it }
        }
        holderIdentifier?.let {
            query.andWhere { VerifiableCredentials.holderDid eq it }
        }
        type?.let {
            query.andWhere { VerifiableCredentials.type eq it }
        }
        credentialId?.let {
            query.andWhere { VerifiableCredentials.credentialId eq it }
        }
        query.toList().map {
            Json.decodeFromString(it[VerifiableCredentials.content])
        }
    }

    fun storeCredential(
        issuedCredential: IssuedVerifiableCredentialRequestDto,
        credentialAsJson: String,
        typesAsString: String,
        holderWallet: Wallet
    ): VerifiableCredential {
        return VerifiableCredential.new {
            credentialId = issuedCredential.id
            content = credentialAsJson
            issuerDid = issuedCredential.issuer
            holderDid = issuedCredential.credentialSubject["id"] as String
            type = typesAsString
            wallet = holderWallet
        }
    }

    fun storeCredential(
        issuedCredentialId: String,
        issuerOfCredential: String,
        holderOfCredential: String,
        credentialAsJson: String,
        typesAsString: String,
        holderWallet: Wallet
    ): VerifiableCredential {
        return VerifiableCredential.new {
            credentialId = issuedCredentialId
            issuerDid = issuerOfCredential
            holderDid = holderOfCredential
            content = credentialAsJson
            type = typesAsString
            wallet = holderWallet
        }
    }

    fun deleteCredentialByCredentialId(credentialId: String): Boolean {
        VerifiableCredentials.deleteWhere { VerifiableCredentials.credentialId eq credentialId }
        return true
    }

}
