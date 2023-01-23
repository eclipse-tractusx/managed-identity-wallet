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

package org.eclipse.tractusx.managedidentitywallets.persistence.entities

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object SchedulerTasks : Table("scheduled_tasks") {
    val taskName = text("task_name")
    val taskInstance = text("task_instance")
    val taskData = binary("task_data").nullable()
    // It should be timestampWithTimeZone (timestampez), but jetbrains exposed does not support it yet
    val executionTime = timestamp("execution_time")
    val picked = bool("picked")
    val pickedBy = text("picked_by").nullable()
    val lastSuccess = timestamp("last_success").nullable()
    val lastFailure = timestamp("last_failure").nullable()
    val consecutiveFailures = integer("consecutive_failures").nullable()
    val lastHeartbeat = timestamp("last_heartbeat").nullable()
    val version = long("version")
    override val primaryKey = PrimaryKey(taskName, taskInstance, name = "PK_Scheduler_Tasks")
}
