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

@file:UseSerializers(AnySerializer::class)

package org.eclipse.tractusx.managedidentitywallets.models.ssi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.eclipse.tractusx.managedidentitywallets.plugins.AnySerializer

@Serializable
data class PublicKeyJwkDto(
    val kty: String,
    val use: String? = null,
    @SerialName("key_ops")
    val keyOps: List<String>? = null,
    val alg: String? = null,
    val kid: String? = null,
    val crv: String? = null,
    val x: String? = null,
    val y: String? = null,
    val additionalAttributes: Map<String, Any>? = null
)
