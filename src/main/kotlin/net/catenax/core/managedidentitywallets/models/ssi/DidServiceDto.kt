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

package net.catenax.core.managedidentitywallets.models.ssi

import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.Serializable

@Serializable
data class DidServiceDto(
    @Field(description = "The ID of the Service Endpoint as String (URI compatible)", name = "id")
    val id: String,
    @Field(description = "The Type of the Service Endpoint as String", name = "type")
    val type: String,
    @Field(description = "The URL of the Service Endpoint as String (URI compatible)", name = "serviceEndpoint")
    val serviceEndpoint: String,
    val recipientKeys: List<String>? = null,
    val routingKeys: List<String>? = null,
    val priority: Int? = null
)

@Serializable
data class DidServiceUpdateRequestDto(
    @Field(description = "The Type of the Service Endpoint as String", name = "type")
    val type: String,
    @Field(description = "The URL of the Service Endpoint as String (URI compatible)", name = "serviceEndpoint")
    val serviceEndpoint: String
)

@Serializable
data class DidDocumentServiceParameters(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of Entity", name = "identifier")
    val identifier: String,
    @Param(type = ParamType.PATH)
    @Field(description = "The ID of the Service Endpoint as String (URI compatible)", name = "id")
    val id: String
)
