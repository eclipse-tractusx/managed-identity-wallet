@file:UseSerializers(AnySerializer::class)

package net.catenax.core.custodian.models

import io.bkbn.kompendium.annotations.Field
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.catenax.core.custodian.plugins.AnySerializer

// TODO need to analyze the data updates if that could be made
// in a generic way without any logic to map it to the wallet
// and verifiable credentials

@Serializable
data class ExtendedNameDto(
    @Field(description = "technical key", name = "technicalKey")
    val technicalKey: String? = null,
    @Field(description = "given name", name = "name")
    val name: String,
    @Field(description = "url - if applicable", name = "url")
    val url: String? = null
)

@Serializable
data class IdentifierDto(
    @Field(description = "unique id of the identifier", name = "uuid")
    val uuid: String,
    @Field(description = "value of the identifier", name = "value")
    val value: String,
    @Field(description = "type of the identifier", name = "type")
    val type: ExtendedNameDto,
    @Field(description = "issuing body of the identifier", name = "issuingBody")
    val issuingBody: ExtendedNameDto,
    @Field(description = "status of the identifier", name = "status")
    val status: ExtendedNameDto
)

@Serializable
data class NameDto(
    @Field(description = "unique id of the name", name = "uuid")
    val uuid: String,
    @Field(description = "value", name = "value")
    val value: String,
    @Field(description = "name", name = "name")
    val name: String? = null,
    @Field(description = "short name", name = "shortName")
    val shortName: String? = null,
    @Field(description = "fipsCode", name = "fipsCode")
    val fipsCode: String? = null,
    @Field(description = "number", name = "number")
    val number: String? = null,
    @Field(description = "direction", name = "direction")
    val direction: String? = null,
    @Field(description = "type", name = "type")
    val type: ExtendedNameDto,
    @Field(description = "language", name = "language")
    val language: ExtendedNameDto
)

@Serializable
data class LegalFormDto(
    @Field(description = "Technical key", name = "technicalKey")
    val technicalKey: String,
    @Field(description = "Given name", name = "name")
    val name: String,
    @Field(description = "Url - if applicable", name = "url")
    val url: String? = null,
    @Field(description = "Main abbreviation", name = "mainAbbreviation")
    val mainAbbreviation: String,
    @Field(description = "Language", name = "language")
    val language: ExtendedNameDto,
    @Field(description = "Categories", name = "categories")
    val categories: List<ExtendedNameDto>
)

@Serializable
data class BusinessPartnerDataUpdateRequestDto(
    @Field(description = "The BPN to which this update record applies to", name = "bpn")
    val bpn: String,
    @Field(description = "The identifiers of the record", name = "identifiers")
    val identifiers: List<IdentifierDto>? = null,
    @Field(description = "List of name", name = "names")
    val names: List<NameDto>? = null,
    @Field(description = "The legal form", name = "legalForm")
    val legalForm: LegalFormDto? = null,
    @Field(description = "Status of the entity", name = "status")
    val status: String? = null,
    @Field(description = "Addresses", name = "addresses")
    val addresses: List<AddressDto>,
    @Field(description = "Profile classifications", name = "profileClassifications")
    val profileClassifications: List<ClassificationDto>,
    @Field(description = "Types", name = "types")
    val types: List<ExtendedNameDto>,
    @Field(description = "Bank accounts", name = "bankAccounts")
    val bankAccounts: List<BankAccountDto>,
    @Field(description = "Roles", name = "roles")
    val roles: List<ExtendedNameDto>,
    @Field(description = "Relations", name = "relations")
    val relations: List<RelationDto>
)

@Serializable
data class ClassificationDto(
    @Field(description = "UUID", name = "uuid")
    val uuid: String,
    @Field(description = "Value", name = "value")
    val value: String,
    @Field(description = "Code", name = "code")
    val code: String? = null,
    @Field(description = "Type", name = "type")
    val type: ExtendedNameDto? = null
)

@Serializable
data class RelationDto(
    @Field(description = "UUID", name = "uuid")
    val uuid: String,
    @Field(description = "Relation Class", name = "relationClass")
    val relationClass: ExtendedNameDto,
    @Field(description = "Type", name = "type")
    val type: ExtendedNameDto,
    @Field(description = "Start Node", name = "startNode")
    val startNode: String,
    @Field(description = "End Node", name = "endNode")
    val endNode: String,
    @Field(description = "Started At", name = "startedAt")
    val startedAt: String? = null,
    @Field(description = "Ended At", name = "endedAt")
    val endedAt: String? = null
)

@Serializable
data class BankAccountDto(
    @Field(description = "UUID", name = "uuid")
    val uuid: String,
    @Field(description = "TrustScores", name = "trustScores")
    val trustScores: List<Float>,
    @Field(description = "Currency", name = "currency")
    val currency: ExtendedNameDto,
    @Field(description = "International Bank Account Identifier", name = "internationalBankAccountIdentifier")
    val internationalBankAccountIdentifier: String,
    @Field(description = "International Bank Identifier", name = "internationalBankIdentifier")
    val internationalBankIdentifier: String,
    @Field(description = "National Bank Account Identifier", name = "nationalBankAccountIdentifier")
    val nationalBankAccountIdentifier: String,
    @Field(description = "National Bank Identifier", name = "nationalBankIdentifier")
    val nationalBankIdentifier: String
)

@Serializable
data class AddressVersion(
    @Field(description = "Character Set", name = "characterSet")
    val characterSet: ExtendedNameDto,
    @Field(description = "Language", name = "language")
    val language: ExtendedNameDto
)
@Serializable
data class PostCode(
    @Field(description = "UUID", name = "uuid")
    val uuid: String,
    @Field(description = "Value", name = "value")
    val value: String,
    @Field(description = "Type", name = "type")
    val type: ExtendedNameDto
)

@Serializable
data class GeoCoordinateDto(
    @Field(description = "Longitude", name = "longitude")
    val longitude: Float,
    @Field(description = "Latitude", name = "latitude")
    val latitude: Float,
    @Field(description = "Altitude", name = "altitude")
    val altitude: Float? = null
)
@Serializable
data class AddressDto(
    @Field(description = "UUID", name = "uuid")
    val uuid: String,
    @Field(description = "Version", name = "version")
    val version: AddressVersion,
    @Field(description = "Care Of", name = "careOf")
    val careOf: String? = null,
    @Field(description = "Contexts", name = "contexts")
    val contexts: List<String>,
    @Field(description = "Country", name = "country")
    val country: ExtendedNameDto,
    @Field(description = "Administrative Areas", name = "administrativeAreas")
    val administrativeAreas: List<NameDto>,
    @Field(description = "PostCodes", name = "postCodes")
    val postCodes: List<PostCode>,
    @Field(description = "Localities", name = "localities")
    val localities: List<NameDto>,
    @Field(description = "Thoroughfares", name = "thoroughfares")
    val thoroughfares: List<NameDto>,
    @Field(description = "Premises", name = "premises")
    val premises: List<NameDto>,
    @Field(description = "PostalDeliveryPoints", name = "postalDeliveryPoints")
    val postalDeliveryPoints: List<NameDto>,
    @Field(description = "Geographic Coordinates", name = "geographicCoordinates")
    val geographicCoordinates: GeoCoordinateDto? = null,
    @Field(description = "Types", name = "types")
    val types: List<ExtendedNameDto>
)
