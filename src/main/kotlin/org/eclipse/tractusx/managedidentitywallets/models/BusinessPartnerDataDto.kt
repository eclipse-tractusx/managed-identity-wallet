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
    val scope: String
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

@Serializable
data class NameResponse (
    @Field(description = "Full name")
    val value: String,
    @Field(description = "Abbreviated name or shorthand")
    val shortName: String? = null,
    @Field(description = "Type of name")
    @JsonProperty("nameType") @JsonNames("type", "nameType") @SerializedName("nameType")
    val type: TypeKeyNameUrlDto<NameType>,
    @Field(description = "Language in which the name is specified")
    val language: TypeKeyNameDto<String>
)

@Serializable
data class ExtendedMultiPurposeDto(
    @Field(description = "Unique identifier for reference purposes", name = "uuid")
    val uuid: String,
    @Field(description = "value", name = "value")
    val value: String,
    @Field(description = "name", name = "name")
    val name: String? = null,
    @Field(description = "short name", name = "shortName")
    val shortName: String? = null,
    @Field(description = "FIPS code if applicable", name = "fipsCode")
    val fipsCode: String? = null,
    @Field(description = "number", name = "number")
    val number: String? = null,
    @Field(description = "direction", name = "direction")
    val direction: String? = null,
    @Field(description = "type", name = "type")
    val type: TypeKeyNameUrlDto<BusinessStatusType>,
    @Field(description = "language", name = "language")
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


@Serializable
data class LegalAddressDto(
    @Field(description = "Business Partner Number", name = "legalEntity")
    val legalEntity: String,
    @Field(description = "The Legal Address of Legal Entity", name = "legalAddress")
    val legalAddress: AddressDto,
)

@Serializable
data class BusinessPartnerDataDto(
    @Field(description = "Business Partner Number, main identifier value for sites", name = "bpn")
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
    val types: Collection<TypeKeyNameUrlDto<BusinessPartnerType>>,
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
    val relationClass: TypeKeyNameDto<RelationClass>,
    @Field(description = "Type of relation like predecessor or ownership relation", name = "type")
    val type: TypeKeyNameDto<RelationType>,
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
    val type: TypeKeyNameUrlDto<BusinessStatusType>
)

@Serializable
data class AddressVersion(
    @Field(description = "Character set in which the address is written")
    val characterSet: TypeKeyNameDto<CharacterSet>,
    @Field(description = "Language in which the address is written")
    val language: TypeKeyNameDto<String>
)
@Serializable
data class PostCode(
    @Field(description = "Full postcode denotation", name = "value")
    val value: String,
    @Field(description = "Type of specified postcode", name = "type")
    @JsonProperty("postCodeType") @JsonNames("type", "postCodeType") @SerializedName("postCodeType")
    val type: TypeKeyNameUrlDto<PostCodeType>
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
    val type: TypeKeyNameUrlDto<PostalDeliveryPointType>,
    @Field(description = "Language the delivery point is specified in")
    val language: TypeKeyNameDto<String>
)

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
    val types: List<TypeKeyNameUrlDto<AddressType>>
)

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
    val type: TypeKeyNameUrlDto<AdministrativeAreaType>,
    @Field(description = "Language the area is specified in")
    val language: TypeKeyNameDto<String>
)

@Serializable
data class LocalityResponse (
    @Field(description = "Full name of the locality")
    val value: String,
    @Field(description = "Abbreviation or shorthand of the locality's name")
    val shortName: String? = null,
    @Field(description = "Type of locality")
    @JsonProperty("localityType") @JsonNames("type", "localityType") @SerializedName("localityType")
    val type: TypeKeyNameUrlDto<LocalityType>,
    @Field(description = "Language the locality is specified in")
    val language: TypeKeyNameDto<String>
)

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
    var type: TypeKeyNameUrlDto<ThoroughfareType>,
    @Field(description = "Language the thoroughfare is specified in")
    var language: TypeKeyNameDto<String>
)

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
    val type: TypeKeyNameUrlDto<PremiseType>,
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

interface HasDefaultValue<T> {
    fun getDefault(): T
}

interface NamedType {
    fun getTypeName(): String
}

interface NamedUrlType : NamedType {
    fun getUrl(): String
}

enum class RelationClass(private val typeName: String) : NamedType {
    CDQ_HIERARCHY("CDQ Hierarchy"),
    CDQ_TRANSITION("CDQ Transition"),
    CX_HIERARCHY("Catena-X"),
    DNB_HIERARCHY("DNB"),
    LEI_HIERARCHY("LEI");
    override fun getTypeName(): String { return typeName }
}

enum class PostCodeType(private val codeName: String, private val url: String) : NamedUrlType, HasDefaultValue<PostCodeType> {
    CEDEX("Courrier d’Entreprise à Distribution Exceptionnelle", ""),
    LARGE_MAIL_USER("Large mail user", ""),
    OTHER("Other type", ""),
    POST_BOX("Post Box", ""),
    REGULAR("Regular", "");
    override fun getTypeName(): String { return codeName }
    override fun getUrl(): String { return url }
    override fun getDefault(): PostCodeType { return OTHER }
}

enum class RelationType(private val typeName: String) : NamedType {
    CX_LEGAL_SUCCESSOR_OF("Start is legal successor of End"),
    CX_LEGAL_PREDECESSOR_OF("Start is legal predecessor of End"),
    CX_ADDRESS_OF("Start is legally registered at End"),
    CX_SITE_OF("Start operates at site of End"),
    CX_OWNED_BY("Start is legally owned by End"),
    DIRECT_LEGAL_RELATION("Start is legally owned by End"),
    COMMERCIAL_ULTIMATE("End is highest commercial organization in hierarchy of Start"),
    DOMESTIC_BRANCH_RELATION("Start is domestic branch of End"),
    INTERNATIONAL_BRANCH_RELATION("Start is international branch of End"),
    DOMESTIC_LEGAL_ULTIMATE_RELATION("End is highest domestic organization in hierarchy of Start"),
    GLOBAL_LEGAL_ULTIMATE_RELATION("End is globally highest organization in hierarchy of Start"),
    LEGAL_PREDECESSOR("Start is legal predecessor of End"),
    LEGAL_SUCCESSOR("Start is legal successor of End"),
    DNB_PARENT("Start legally owns End"),
    DNB_HEADQUARTER("Start is legal headquarter of End"),
    DNB_DOMESTIC_ULTIMATE("End is highest domestic organization in hierarchy of Start"),
    DNB_GLOBAL_ULTIMATE("End is globally highest organization in hierarchy of Start"),
    LEI_DIRECT_PARENT("Start legally owns End"),
    LEI_INTERNATIONAL_BRANCH("Start is international branch of End"),
    LEI_ULTIMATE_PARENT("End is globally highest organization in hierarchy of Start");
    override fun getTypeName(): String {return typeName }
}
enum class BusinessPartnerType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<BusinessPartnerType> {
    BRAND("Brand", ""),
    LEGAL_ENTITY("Legal Entity", ""),
    ORGANIZATIONAL_UNIT("Organizational Unit", ""),
    SITE("Site", ""),
    UNKNOWN("Unknown", "");
    override fun getTypeName(): String { return typeName }
    override fun getUrl(): String { return url }
    override fun getDefault(): BusinessPartnerType { return UNKNOWN }
}

enum class BusinessStatusType(private val statusName: String, private val url: String) : NamedUrlType, HasDefaultValue<BusinessStatusType> {
    ACTIVE("Active", ""),
    DISSOLVED("Dissolved", ""),
    IN_LIQUIDATION("In Liquidation", ""),
    INACTIVE("Inactive", ""),
    INSOLVENCY("Insolvency", ""),
    UNKNOWN("Unknown", "");
    override fun getTypeName(): String { return statusName}
    override fun getUrl(): String { return url }
    override fun getDefault(): BusinessStatusType { return UNKNOWN }
}

enum class NameType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<NameType> {
    ACRONYM("An acronym commonly used for a business partner.", ""),
    DOING_BUSINESS_AS("Alternative names a company employs for doing business", ""),
    ESTABLISHMENT("Name that is used in conjunction with the registered name to name a specific organizational unit", ""),
    INTERNATIONAL("The international version of the local name of a business partner", ""),
    LOCAL("The business partner name identifies a business partner in a given context, e.g. a country or region.", ""),
    OTHER("Any other alternative name used for a company, such as a specific language variant.", ""),
    REGISTERED("The main name under which a business is officially registered in a country's business register.", ""),
    TRANSLITERATED(
        "The transliterated name is not an officially used name, but a construct that helps to better find business partners with registered names in non-latin characters",
        ""
    ),
    VAT_REGISTERED("The name which is associated with the VAT number of a business partner, i.e. the name stored in a VAT register.", "");
    override fun getTypeName(): String { return typeName }
    override fun getUrl(): String { return url }
    override fun getDefault(): NameType { return OTHER }
}

enum class AddressType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<AddressType> {
    BRANCH_OFFICE("Branch Office", ""),
    CARE_OF("Care of (c/o) Address", ""),
    HEADQUARTER("Headquarter", ""),
    LEGAL_ALTERNATIVE("Legal Alternative", ""),
    PO_BOX("Post Office Box", ""),
    REGISTERED("Registered", ""),
    REGISTERED_AGENT_MAIL("Registered Agent Mail", ""),
    REGISTERED_AGENT_PHYSICAL("Registered Agent Physical", ""),
    VAT_REGISTERED("Vat Registered", ""),
    UNSPECIFIC("Unspecified", "");
    override fun getTypeName(): String { return typeName }
    override fun getUrl(): String { return url }
    override fun getDefault(): AddressType { return UNSPECIFIC }
}

enum class PostalDeliveryPointType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<PostalDeliveryPointType> {
    INTERURBAN_DELIVERY_POINT("Interurban Delivery Point", ""),
    MAIL_STATION("Mail Station", ""),
    MAILBOX("Mailbox", ""),
    OTHER("Other Type", ""),
    POST_OFFICE_BOX("Post Office Box", "");

    override fun getTypeName(): String { return typeName }
    override fun getUrl(): String { return url }
    override fun getDefault(): PostalDeliveryPointType { return OTHER }
}

enum class PremiseType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<PremiseType> {
    BUILDING("Building", ""),
    OTHER("Other type", ""),
    LEVEL("Level", ""),
    HARBOUR("Harbour", ""),
    ROOM("Room", ""),
    SUITE("Suite", ""),
    UNIT("Unit", ""),
    WAREHOUSE("Warehouse", "");
    override fun getTypeName(): String { return typeName }
    override fun getUrl(): String { return url }
    override fun getDefault(): PremiseType { return OTHER }
}

enum class ThoroughfareType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<ThoroughfareType> {
    INDUSTRIAL_ZONE("An industrial zone", ""),
    OTHER("Other type", ""),
    RIVER("River", ""),
    SQUARE("Square", ""),
    STREET("Street", "");
    override fun getTypeName(): String { return typeName }
    override fun getUrl(): String { return url }
    override fun getDefault(): ThoroughfareType { return OTHER }
}

enum class LocalityType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<LocalityType> {
    BLOCK("Block", ""),
    CITY("City", ""),
    DISTRICT("District", ""),
    OTHER("Other", ""),
    POST_OFFICE_CITY("Post Office City", ""),
    QUARTER("Quarter", "");
    override fun getTypeName(): String { return typeName }
    override fun getUrl(): String { return url }
    override fun getDefault(): LocalityType { return OTHER }
}

enum class AdministrativeAreaType(private val areaName: String, private val url: String) : NamedUrlType, HasDefaultValue<AdministrativeAreaType> {
    COUNTY("County", ""),
    REGION("Region", ""),
    OTHER("Other", "");
    override fun getTypeName(): String { return areaName }
    override fun getUrl(): String { return url }
    override fun getDefault(): AdministrativeAreaType { return OTHER }
}

enum class CharacterSet(private val typeName: String) : NamedType, HasDefaultValue<CharacterSet> {
    ARABIC("Arabic"),
    CHINESE("Simplified Chinese"),
    CHINESE_TRADITIONAL("Traditional Chinese"),
    CYRILLIC("Cyrillic"),
    GREEK("Greek"),
    HANGUL_KOREAN("Hangul"),
    HEBREW("Hebrew"),
    HIRAGANA("Hiragana"),
    KANJI("Kanji"),
    KATAKANA("Katakana"),
    LATIN("Latin"),
    THAI("Thai"),
    WESTERN_LATIN_STANDARD("Western Latin Standard (ISO 8859-1; Latin-1)"),
    UNDEFINED("Undefined");
    override fun getTypeName(): String { return typeName }
    override fun getDefault(): CharacterSet { return UNDEFINED }
}
