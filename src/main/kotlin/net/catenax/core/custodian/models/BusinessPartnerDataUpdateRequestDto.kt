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
    @Field(description = "short name", name = "shortName")
    val shortName: String,
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
data class EmptyDto(
    @Field(description = "Optional field", name = "field")
    val field: String? = null
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
    val addresses: List<EmptyDto>,
    @Field(description = "Profile classifications", name = "profileClassifications")
    val profileClassifications: List<EmptyDto>,
    @Field(description = "Types", name = "types")
    val types: List<ExtendedNameDto>,
    @Field(description = "Bank accounts", name = "bankAccounts")
    val bankAccounts: List<EmptyDto>,
    @Field(description = "Roles", name = "roles")
    val roles: List<EmptyDto>,
    @Field(description = "Relations", name = "relations")
    val relations: List<EmptyDto>
)