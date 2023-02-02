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

package org.eclipse.tractusx.managedidentitywallets.persistence.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object VerifiableCredentials : IntIdTable("verifiable_credentials") {
    // uniqueIndex("content") not working in h2 database
    val content = text("content").uniqueIndex("content")
    val credentialId = varchar("credential_id", 4096).uniqueIndex("credentialId").nullable()
    val issuerDid = varchar("issuer_did", 4096)
    val holderDid = varchar("holder_did", 4096)
    val type = varchar("type", 4096)
    val walletId = reference("wallet_id", Wallets)
}

class VerifiableCredential(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, VerifiableCredential>(VerifiableCredentials)
    var content by VerifiableCredentials.content
    var credentialId by VerifiableCredentials.credentialId
    var issuerDid by VerifiableCredentials.issuerDid
    var holderDid by VerifiableCredentials.holderDid
    var type by VerifiableCredentials.type
    var wallet by Wallet referencedOn VerifiableCredentials.walletId
}
