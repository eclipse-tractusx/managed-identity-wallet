package net.catenax.core.managedidentitywallets.models

import io.bkbn.kompendium.annotations.Field
import kotlinx.serialization.Serializable

// TODO need to analyze the data updates if that could be made
// in a generic way without any logic to map it to the wallet
// and verifiable credentials
@Serializable
data class TypeKeyNameDto (
    @Field(description = "Unique key of this type for reference")
    val technicalKey: String,
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
data class TypeKeyNameUrlDto (
    @Field(description = "Unique key of this type for reference")
    val technicalKey: String,
    @Field(description = "Name or denotation of this type")
    val name: String,
    @Field(description = "URL link leading to page with further information on the type")
    val url: String?
)

@Serializable
data class IdentifierDto(
    @Field(description = "Unique identifier for reference purposes")
    val uuid: String,
    @Field(description = "Value of the identifier")
    val value: String,
    @Field(description = "Type of the identifier")
    val type: TypeKeyNameUrlDto,
    @Field(description = "Body which issued the identifier")
    val issuingBody:  TypeKeyNameUrlDto?,
    @Field(description = "Status of the identifier")
    val status: TypeKeyNameDto?
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
    val type: TypeKeyNameUrlDto,
    @Field(description = "language", name = "language")
    val language: TypeKeyNameDto
)

@Serializable
data class LegalFormDto(
    @Field(description = "Unique key to be used for reference")
    val technicalKey: String,
    @Field(description = "Full name of the legal form")
    val name: String,
    @Field(description = "Link for further information on the legal form")
    val url: String?,
    @Field(description = "Abbreviation of the legal form name")
    val mainAbbreviation: String?,
    @Field(description = "Language in which the legal form is specified")
    val language: TypeKeyNameDto,
    @Field(description = "Categories in which this legal form falls under")
    val categories: Collection<TypeNameUrlDto>
)

@Serializable
data class BusinessPartnerDataUpdateRequestDto(
    @Field(description = "The BPN to which this update record applies to", name = "bpn")
    val bpn: String,
    @Field(description = "The identifiers of the record", name = "identifiers")
    val identifiers: List<IdentifierDto>,
    @Field(description = "List of name", name = "names")
    val names: Collection<ExtendedMultiPurposeDto>,
    @Field(description = "The legal form", name = "legalForm")
    val legalForm: LegalFormDto? = null,
    @Field(description = "Status of the entity", name = "status")
    val status: String? = null,
    @Field(description = "Addresses", name = "addresses")
    val addresses: Collection<AddressDto>,
    @Field(description = "Profile classifications", name = "profileClassifications")
    val profileClassifications: List<ClassificationDto>,
    @Field(description = "Types", name = "types")
    val types: Collection<TypeKeyNameUrlDto>,
    @Field(description = "Bank accounts", name = "bankAccounts")
    val bankAccounts: List<BankAccountDto>,
    @Field(description = "Roles", name = "roles")
    val roles: Collection<TypeKeyNameDto>,
    @Field(description = "Relations", name = "relations")
    val relations: Collection<RelationDto>
)

@Serializable
data class ClassificationDto(
    @Field(description = "UUID", name = "uuid")
    val uuid: String,
    @Field(description = "Value", name = "value")
    val value: String,
    @Field(description = "Code", name = "code")
    val code: String?,
    @Field(description = "Type", name = "type")
    val type: TypeNameUrlDto?
)

@Serializable
data class RelationDto(
    @Field(description = "UUID", name = "uuid")
    val uuid: String,
    @Field(description = "Class of relation like Catena, LEI or DNB relation", name = "relationClass")
    val relationClass: TypeKeyNameDto,
    @Field(description = "Type of relation like predecessor or ownership relation", name = "type")
    val type: TypeKeyNameDto,
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
    @Field(description = "Unique identifier for reference purposes")
    val uuid: String,
    @Field(description = "Trust scores for the account" )
    val trustScores: Collection<Float>,
    @Field(description = "Used currency in the account")
    val currency: TypeKeyNameDto,
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
data class AddressVersion(
    @Field(description = "Character set in which the address is written")
    val characterSet: TypeKeyNameDto,
    @Field(description = "Language in which the address is written")
    val language: TypeKeyNameDto
)
@Serializable
data class PostCode(
    @Field(description = "Unique identifier for reference purposes", name = "uuid")
    val uuid: String,
    @Field(description = "Full postcode denotation", name = "value")
    val value: String,
    @Field(description = "Type of specified postcode", name = "type")
    val type: TypeKeyNameUrlDto
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
data class AddressDto(
    @Field(description = "UUID", name = "uuid")
    val uuid: String,
    @Field(description = "Version", name = "version")
    val version: AddressVersion,
    @Field(description = "Entity which is in care of this address", name = "careOf")
    val careOf: String?,
    @Field(description = "Contexts of this address", name = "contexts")
    val contexts: List<String>,
    @Field(description = "Address country", name = "country")
    val country: TypeKeyNameDto,
    @Field(description = "Areas such as country region and county", name = "administrativeAreas")
    val administrativeAreas: Collection<ExtendedMultiPurposeDto>,
    @Field(description = "Postcodes assigned to this address", name = "postCodes")
    val postCodes: Collection<PostCode>,
    @Field(description = "Localities such as city, block and quarter", name = "localities")
    val localities: Collection<ExtendedMultiPurposeDto>,
    @Field(description = "Thoroughfares such as street, zone and square", name = "thoroughfares")
    val thoroughfares: Collection<ExtendedMultiPurposeDto>,
    @Field(description = "Premises such as building, level and room", name = "premises")
    val premises: Collection<ExtendedMultiPurposeDto>,
    @Field(description = "Delivery points for post", name = "postalDeliveryPoints")
    val postalDeliveryPoints: Collection<ExtendedMultiPurposeDto>,
    @Field(description = "Geographic Coordinates", name = "geographicCoordinates")
    val geographicCoordinates: GeoCoordinateDto?,
    @Field(description = "Types of this address", name = "types")
    val types: Collection<TypeKeyNameUrlDto>
)
