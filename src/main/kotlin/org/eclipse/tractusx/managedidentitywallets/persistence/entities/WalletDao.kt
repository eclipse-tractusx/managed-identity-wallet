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
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Wallets : IntIdTable("wallets") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime)
    val modifiedFrom = varchar("modified_from", 36)

    val name = varchar("name", 127)
    var bpn = varchar("bpn", 36).uniqueIndex("bpn")
    val did = varchar("did", 2096).uniqueIndex("did")
    val didDocument = varchar("did_document", 4096)

    val active = bool("is_active").default(true)
    val authority = bool("is_authority").default(false)

    val algorithm = varchar("algorithm", 32)
}

class Wallet(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Wallet>(Wallets)
    var createdAt by Wallets.createdAt
    var modifiedAt by Wallets.modifiedAt
    var modifiedFrom by Wallets.modifiedFrom

    var name by Wallets.name
    var bpn by Wallets.bpn
    var did by Wallets.did
    var didDocument by Wallets.didDocument

    var active by Wallets.active
    var authority by Wallets.authority

    var algorithm by Wallets.algorithm
}
