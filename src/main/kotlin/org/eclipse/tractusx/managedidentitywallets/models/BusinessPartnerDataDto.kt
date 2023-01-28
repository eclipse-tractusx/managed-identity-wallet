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

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class AccessToken(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("refresh_expires_in")
    val refreshExpiresIn: Int,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("id_token")
    val idToken: String,
    @SerialName("not-before-policy")
    val notBeforePolicy: Int,
    val scope: String
)

@Serializable
data class BPDMConfig(
    val url: String,
    val tokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val grantType: String,
    val scope: String,
    val memberOfPlatform: String
)

@Serializable
data class BusinessPartnerDataRefreshParameters(
    @Param(type = ParamType.QUERY)
    @Field(description = "The DID or BPN of the business partner whose data should be refreshed", name = "identifier")
    val identifier: String? = null
)

@Serializable
data class TypeKeyNameDto<T> (
    @Field(description = "Unique key of this type for reference")
    val technicalKey: T,
    @Field(description = "Name or denotation of this type")
    val name: String,
)

@Serializable
data class TypeNameUrlDto (
    @Field(description = "Name of the type")
    val name: String,
    @Field(description = "URL link leading to page with further information on the type")
    val url: String?
)

@Serializable
data class TypeKeyNameUrlDto<T> (
    @Field(description = "Unique key of this type for reference")
    val technicalKey: T,
    @Field(description = "Name or denotation of this type")
    val name: String,
    @Field(description = "URL link leading to page with further information on the type")
    val url: String?
)

@Serializable
data class IdentifierDto(
    @Field(description = "Value of the identifier")
    val value: String,
    @Field(description = "Type of the identifier")
    val type: TypeKeyNameUrlDto<String>,
    @Field(description = "Body which issued the identifier")
    val issuingBody:  TypeKeyNameUrlDto<String>?,
    @Field(description = "Status of the identifier")
    val status: TypeKeyNameDto<String>?
)

@ExperimentalSerializationApi
@Serializable
data class NameResponse (
    @Field(description = "Full name")
    val value: String,
    @Field(description = "Abbreviated name or shorthand")
    val shortName: String? = null,
    @Field(description = "Type of name")
    @JsonProperty("nameType") @JsonNames("type", "nameType") @SerializedName("nameType")
    val type: TypeKeyNameUrlDto<String>,
    @Field(description = "Language in which the name is specified")
    val language: TypeKeyNameDto<String>
)

@Serializable
data class LegalFormDto(
    @Field(description = "Unique key to be used for reference")
    val technicalKey: String,
    @Field(description = "Full name of the legal form")
    val name: String,
    @Field(description = "Link for further information on the legal form")
    val url: String? = null,
    @Field(description = "Abbreviation of the legal form name")
    val mainAbbreviation: String? = null,
    @Field(description = "Language in which the legal form is specified")
    val language: TypeKeyNameDto<String>,
    @Field(description = "Categories in which this legal form falls under")
    val categories: List<TypeNameUrlDto>
)

@ExperimentalSerializationApi
@Serializable
data class LegalAddressDto(
    @Field(description = "Business Partner Number", name = "legalEntity")
    val legalEntity: String,
    @Field(description = "The Legal Address of Legal Entity", name = "legalAddress")
    val legalAddress: AddressDto,
)

@ExperimentalSerializationApi
@Serializable
data class BusinessPartnerDataDto(
    @Field(description = "Business Partner Number of this legal entity", name = "bpn")
    val bpn: String,
    @Field(description = "The identifiers of the record", name = "identifiers")
    val identifiers: Collection<IdentifierDto>,
    @Field(description = "List of name", name = "names")
    val names: Collection<NameResponse>,
    @Field(description = "The legal form", name = "legalForm")
    val legalForm: LegalFormDto? = null,
    @Field(description = "Status of the entity", name = "status")
    val status: BusinessStatusResponse? = null,
    @Field(description = "Profile classifications", name = "profileClassifications")
    val profileClassifications: List<ClassificationDto>,
    @Field(description = "sites", name = "sites")
    val sites: List<SiteDto>? = null,
    @Field(description = "Types", name = "types")
    val types: Collection<TypeKeyNameUrlDto<String>>,
    @Field(description = "Bank accounts", name = "bankAccounts")
    val bankAccounts: List<BankAccountDto>,
    @Field(description = "Roles", name = "roles")
    val roles: Collection<TypeKeyNameDto<String>>,
    @Field(description = "Relations", name = "relations")
    val relations: Collection<RelationDto>,
    @Field(description = "Currentness", name = "currentness")
    val currentness: String?
)

@Serializable
data class ClassificationDto(
    @Field(description = "Value", name = "value")
    val value: String?,
    @Field(description = "Code", name = "code")
    val code: String?,
    @Field(description = "Type", name = "type")
    val type: TypeNameUrlDto?
)

@Serializable
data class RelationDto(
    @Field(description = "Class of relation like Catena, LEI or DNB relation", name = "relationClass")
    val relationClass: TypeKeyNameDto<String>,
    @Field(description = "Type of relation like predecessor or ownership relation", name = "type")
    val type: TypeKeyNameDto<String>,
    @Field(description = "BPN of partner which is the source of the relation", name = "startNode")
    val startNode: String,
    @Field(description = "BPN of partner which is the target of the relation", name = "endNode")
    val endNode: String,
    @Field(description = "Time when the relation started", name = "startedAt")
    val startedAt: String?,
    @Field(description = "Time when the relation ended", name = "endedAt")
    val endedAt: String?
)

@Serializable
data class BankAccountDto(
    @Field(description = "Trust scores for the account" )
    val trustScores: List<Float>,
    @Field(description = "Used currency in the account")
    val currency: TypeKeyNameDto<String>,
    @Field(description = "ID used to identify this account internationally")
    val internationalBankAccountIdentifier: String,
    @Field(description = "ID used to identify the account's bank internationally")
    val internationalBankIdentifier: String,
    @Field(description = "ID used to identify the account domestically")
    val nationalBankAccountIdentifier: String,
    @Field(description = "ID used to identify the account's bank domestically")
    val nationalBankIdentifier: String
)

@Serializable
data class BusinessStatusResponse(
    @Field(description = "Exact, official denotation of the status")
    val officialDenotation: String?,
    @Field(description = "Since when the status is/was valid")
    val validFrom: String?, // LocalDateTime
    @Field(description = "Until the status was valid, if applicable")
    val validUntil: String? = null, // LocalDateTime
    @Field(description = "The type of this status")
    val type: TypeKeyNameUrlDto<String>
)

@Serializable
data class AddressVersion(
    @Field(description = "Character set in which the address is written")
    val characterSet: TypeKeyNameDto<String>,
    @Field(description = "Language in which the address is written")
    val language: TypeKeyNameDto<String>
)

@ExperimentalSerializationApi
@Serializable
data class PostCode(
    @Field(description = "Full postcode denotation", name = "value")
    val value: String,
    @Field(description = "Type of specified postcode", name = "type")
    @JsonProperty("postCodeType") @JsonNames("type", "postCodeType") @SerializedName("postCodeType")
    val type: TypeKeyNameUrlDto<String>
)

@Serializable
data class GeoCoordinateDto(
    @Field(description = "Longitude coordinate", name = "longitude")
    val longitude: Float,
    @Field(description = "Latitude coordinate", name = "latitude")
    val latitude: Float,
    @Field(description = "Altitude, if applicable", name = "altitude")
    val altitude: Float? = null
)

@ExperimentalSerializationApi
@Serializable
data class PostalDeliveryPointResponse(
    @Field(description = "Full denotation of the delivery point")
    val value: String,
    @Field(description = "Abbreviation or shorthand of the locality's name")
    val shortName: String? = null,
    @Field(description = "Number/code of the delivery point")
    val number: String? = null,
    @Field(description = "Type of the specified delivery point")
    @JsonProperty("postalDeliveryPointType")
    @JsonNames("type", "postalDeliveryPointType")
    @SerializedName("postalDeliveryPointType")
    val type: TypeKeyNameUrlDto<String>,
    @Field(description = "Language the delivery point is specified in")
    val language: TypeKeyNameDto<String>
)

@ExperimentalSerializationApi
@Serializable
data class AddressDto(
    @Field(description = "Version", name = "version")
    val version: AddressVersion,
    @Field(description = "Entity which is in care of this address", name = "careOf")
    val careOf: String? = null,
    @Field(description = "Contexts of this address", name = "contexts")
    val contexts: List<String>,
    @Field(description = "Address country", name = "country")
    val country: TypeKeyNameDto<String>,
    @Field(description = "Areas such as country region and county", name = "administrativeAreas")
    val administrativeAreas: List<AdministrativeAreaResponse>,
    @Field(description = "Postcodes assigned to this address", name = "postCodes")
    val postCodes: List<PostCode>,
    @Field(description = "Localities such as city, block and quarter", name = "localities")
    val localities: List<LocalityResponse>,
    @Field(description = "Thoroughfares such as street, zone and square", name = "thoroughfares")
    val thoroughfares: List<ThoroughfareResponse>,
    @Field(description = "Premises such as building, level and room", name = "premises")
    val premises: List<PremiseResponse>,
    @Field(description = "Delivery points for post", name = "postalDeliveryPoints")
    val postalDeliveryPoints: List<PostalDeliveryPointResponse>,
    @Field(description = "Geographic Coordinates", name = "geographicCoordinates")
    val geographicCoordinates: GeoCoordinateDto? = null,
    @Field(description = "Types of this address", name = "types")
    val types: List<TypeKeyNameUrlDto<String>>
)

@ExperimentalSerializationApi
@Serializable
data class AdministrativeAreaResponse (
    @Field(description = "Full name of the area")
    val value: String,
    @Field(description = "Abbreviation or shorthand of the area")
    val shortName: String? = null,
    @Field(description = "FIPS code if applicable")
    val fipsCode: String? = null,
    @Field(description = "Type of specified area")
    @JsonProperty("administrativeAreaType")
    @JsonNames("type", "administrativeAreaType")
    @SerializedName("administrativeAreaType")
    val type: TypeKeyNameUrlDto<String>,
    @Field(description = "Language the area is specified in")
    val language: TypeKeyNameDto<String>
)

@ExperimentalSerializationApi
@Serializable
data class LocalityResponse (
    @Field(description = "Full name of the locality")
    val value: String,
    @Field(description = "Abbreviation or shorthand of the locality's name")
    val shortName: String? = null,
    @Field(description = "Type of locality")
    @JsonProperty("localityType") @JsonNames("type", "localityType") @SerializedName("localityType")
    val type: TypeKeyNameUrlDto<String>,
    @Field(description = "Language the locality is specified in")
    val language: TypeKeyNameDto<String>
)

@ExperimentalSerializationApi
@Serializable
data class ThoroughfareResponse(
    @Field(description = "Full denotation of the thoroughfare")
    val value: String,
    @Field(description = "Full name of the thoroughfare")
    val name: String? = null,
    @Field(description = "Abbreviation or shorthand")
    val shortName: String? = null,
    @Field(description = "Thoroughfare number")
    val number: String? = null,
    @Field(description = "Direction information on the thoroughfare")
    val direction: String? = null,
    @Field(description = "Type of thoroughfare")
    @JsonProperty("thoroughfareType")
    @JsonNames("type", "thoroughfareType")
    @SerializedName("thoroughfareType")
    var type: TypeKeyNameUrlDto<String>,
    @Field(description = "Language the thoroughfare is specified in")
    var language: TypeKeyNameDto<String>
)

@ExperimentalSerializationApi
@Serializable
data class PremiseResponse (
    @Field(description = "Full denotation of the premise")
    val value: String,
    @Field(description = "Abbreviation or shorthand")
    val shortName: String? = null,
    @Field(description = "Premise number")
    val number: String? = null,
    @Field(description = "Type of premise")
    @JsonProperty("premiseType") @JsonNames("type", "premiseType") @SerializedName("premiseType")
    val type: TypeKeyNameUrlDto<String>,
    @Field(description = "Language the premise is specified in")
    val language: TypeKeyNameDto<String>
)

@Serializable
data class SiteDto (
    @Field(description = "Business Partner Number, main identifier value for sites", name = "bpn")
    val bpn: String,
    @Field(description = "Site name", name = "name")
    val name: String,
)
