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

package org.eclipse.tractusx.managedidentitywallets.models

import io.bkbn.kompendium.annotations.Field
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionDto(
    @Field(description = "The aries connection Id", name = "connectionId")
    val connectionId: String,
    @Field(description = "The DID of the partner", name = "did")
    val theirDid: String,
    @Field(description = "The internal wallet did", name = "myDid")
    val myDid: String,
    @Field(description = "The State at which the connection is at - request|accepted", name = "state")
    val state: String
)
