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

@file:UseSerializers(AnySerializer::class)

package org.eclipse.tractusx.managedidentitywallets.models.ssi

import com.fasterxml.jackson.annotation.JsonProperty
import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.eclipse.tractusx.managedidentitywallets.plugins.AnySerializer

@Serializable
data class DidDocumentDto(
    val id: String,
    @SerialName("@context") @JsonProperty("@context")
    val context: List<String>,
    val alsoKnownAs: String? = null,
    @Serializable(AnySerializer::class) val controller: Any? = null,
    @SerialName("verificationMethod")
    @JsonProperty("verificationMethod")
    val verificationMethods: List<DidVerificationMethodDto>? = null,
    @SerialName("authentication")
    @JsonProperty("authentication")
    val authenticationVerificationMethods: List<Any>? = null,
    @SerialName("assertionMethod")
    @JsonProperty("assertionMethod")
    val assertionMethodVerificationMethods: List<Any>? = null,
    @SerialName("keyAgreement")
    @JsonProperty("keyAgreement")
    val keyAgreementVerificationMethods: List<Any>? = null,
    @SerialName("capabilityInvocation")
    @JsonProperty("capabilityInvocation")
    val capabilityInvocationVerificationMethods: List<Any>? = null,
    @SerialName("capabilityDelegation")
    @JsonProperty("capabilityDelegation")
    val capabilityDelegationVerificationMethods: List<Any>? = null,
    @SerialName("service")
    @JsonProperty("service")
    val services: List<DidServiceDto>? = null
)

@Serializable
data class DidDocumentParameters(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of Entity", name = "identifier")
    val identifier: String
)
