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

import org.eclipse.tractusx.managedidentitywallets.models.ConnectionDto
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Connection
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Connections
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ConnectionRepository {

    fun getAll(): List<Connection> = transaction { Connection.all().toList() }

    fun get(
        connectionId: String,
    ): Connection = Connection.find { Connections.connectionId eq connectionId }
        .firstOrNull()
        ?: throw NotFoundException("Connection with id $connectionId not found")

    fun getOrNull(
        connectionId: String,
    ): Connection? = Connection.find { Connections.connectionId eq connectionId }
        .firstOrNull()

    fun add(
        idOfConnection: String,
        connectionTargetDid: String,
        connectionOwnerDid: String,
        rfc23State: String
    ): Connection {
        return Connection.new {
            connectionId = idOfConnection
            theirDid = connectionTargetDid
            myDid =  connectionOwnerDid
            state = rfc23State
        }
    }

    fun updateConnectionState(connectionId: String, rfc23State: String) {
        get(connectionId).apply {
            state = rfc23State
        }
    }

    fun deleteConnections(did: String): Boolean {
        Connections.deleteWhere {
            (Connections.myDid eq did) or (Connections.theirDid eq did)
        }
        return true
    }

    fun deleteConnection(connectionId: String): Boolean {
        get(connectionId).delete()
        return true
    }

    fun getConnections(
        myDid: String?,
        theirDid: String?
    ): List<ConnectionDto> = transaction {
        val query = Connections.selectAll()
        myDid?.let {
            query.andWhere { Connections.myDid eq it }
        }
        theirDid?.let {
            query.andWhere { Connections.theirDid eq it }
        }
        query.toList().map {
            ConnectionDto(
                theirDid = it[Connections.theirDid],
                myDid = it[Connections.myDid],
                connectionId = it[Connections.connectionId],
                state = it[Connections.state]
            )
        }
    }

    fun toObject(entity: Connection): ConnectionDto = entity.run {
        ConnectionDto(
            connectionId,
            theirDid,
            myDid,
            state
        )
    }
}
