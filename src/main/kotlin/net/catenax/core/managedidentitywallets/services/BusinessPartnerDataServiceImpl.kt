package net.catenax.core.managedidentitywallets.services

import foundation.identity.jsonld.JsonLDUtils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.catenax.core.managedidentitywallets.models.*
import net.catenax.core.managedidentitywallets.models.ssi.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class BusinessPartnerDataServiceImpl(private val walletService: WalletService) : BusinessPartnerDataService {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    // TODO currently we check only if the uuid is exits and if not a new credential is issued.
    //  However, it should be checked if the issued credentials need to be updated, in this case
    //  the old credentials should be revoked and deleted from database and new should be issued
    // TODO: notify if issue credentials failed
    override suspend fun issueAndUpdateCatenaXCredentials(businessPartnerData: BusinessPartnerDataUpdateRequestDto) {
        val bpn = businessPartnerData.bpn
        if (businessPartnerData.names.isNotEmpty()) {
            businessPartnerData.names.forEach {
                if (isNewCredential(bpn, it.uuid, JsonLdTypes.NAME_TYPE)) {
                    issueAndStoreCatenaXCredentialsAsync(
                        bpn = bpn,
                        type = JsonLdTypes.NAME_TYPE,
                        data = it
                    )
                }
            }
        }
        if (businessPartnerData.bankAccounts.isNotEmpty()) {
            businessPartnerData.bankAccounts.forEach {
                if (isNewCredential(bpn, it.uuid, JsonLdTypes.BANK_ACCOUNT_TYPE)) {
                    issueAndStoreCatenaXCredentialsAsync(
                        bpn = bpn,
                        type = JsonLdTypes.BANK_ACCOUNT_TYPE,
                        data = it
                    )
                }
            }
        }
        if (businessPartnerData.addresses.isNotEmpty()) {
            businessPartnerData.addresses.forEach {
                if (isNewCredential(bpn, it.uuid, JsonLdTypes.ADDRESS_TYPE)) {
                    issueAndStoreCatenaXCredentialsAsync(
                        bpn = bpn,
                        type = JsonLdTypes.ADDRESS_TYPE,
                        data = it
                    )
                }
            }
        }
        if (businessPartnerData.legalForm != null) {
            if (isNewLegalForm(bpn, businessPartnerData.legalForm)) {
                issueAndStoreCatenaXCredentialsAsync(
                    bpn = bpn,
                    type = JsonLdTypes.LEGAL_FORM_TYPE,
                    data = businessPartnerData.legalForm
                )
            }
        }
    }

    private fun isNewCredential(bpn: String, uuid: String, type: String): Boolean {
        return walletService.getCredentials(null, bpn, type, uuid).isEmpty()
    }

    private fun isNewLegalForm(bpn: String, legalForm: LegalFormDto): Boolean {
        val credentials = walletService.getCredentials(
            null,
            bpn,
            JsonLdTypes.LEGAL_FORM_TYPE,
            null)
        if (credentials.isEmpty()) {
            return true
        } else {
            credentials.forEach {
                // TODO check if language and categories are equal
                if (
                    legalForm.technicalKey == it.credentialSubject["technicalKey"] as String &&
                    legalForm.name == it.credentialSubject["name"] as String &&
                    legalForm.url == it.credentialSubject["url"] as String? &&
                    legalForm.mainAbbreviation == it.credentialSubject["mainAbbreviation"] as String &&
                    legalForm.technicalKey == it.credentialSubject["technicalKey"] as String
                ) {
                    return false
                }
            }
            return true
        }
    }

    override suspend fun <T> issueAndStoreCatenaXCredentialsAsync(
        bpn: String,
        type: String,
        data: T?
    ): Deferred<Boolean> = GlobalScope.async {
        try {
            val vcToIssue = when (type) {
                // TODO: how to handle null values in some properties
                //  of NameDto, BankAccountDto, AddressDto and LegalFormDto
                JsonLdTypes.MEMBERSHIP_TYPE -> prepareMembershipCredential(bpn)
                JsonLdTypes.BPN_TYPE -> prepareBpnCredentials(bpn)
                JsonLdTypes.NAME_TYPE -> prepareNamesCredential(bpn, data as ExtendedMultiPurposeDto)
                JsonLdTypes.BANK_ACCOUNT_TYPE -> prepareBankAccountCredential(bpn, data as BankAccountDto)
                JsonLdTypes.ADDRESS_TYPE -> prepareAddressCredential(bpn, data as AddressDto)
                JsonLdTypes.LEGAL_FORM_TYPE -> prepareLegalFormCredential(bpn, data as LegalFormDto)
                else -> throw NotImplementedException("Credential of type $type is not implemented!")
            }
            val verifiableCredential: VerifiableCredentialDto = walletService.issueCatenaXCredential(vcToIssue)
            val issuedVC = toIssuedVerifiableCredentialRequestDto(verifiableCredential)
            if (issuedVC != null) {
                walletService.storeCredential(bpn, issuedVC)
                return@async true
            }
            log.error("Error: Proof of Credential of type $type is empty")
            false
        } catch (e: Exception) {
            log.error("Error: Issue Catena-X Credentials of type $type failed with message ${e.message}")
            false
        }
    }

    private fun prepareNamesCredential(bpn: String, name: ExtendedMultiPurposeDto): VerifiableCredentialRequestWithoutIssuerDto {
        val currentDateAsString = JsonLDUtils.dateToString(Date.from(Instant.now()))
        return VerifiableCredentialRequestWithoutIssuerDto(
            id = name.uuid,
            context = listOf(
                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS
            ),
            type = listOf(JsonLdTypes.NAME_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
            issuanceDate = currentDateAsString,
            credentialSubject = mapOf(
                "type" to listOf(JsonLdTypes.NAME_TYPE),
                "uuid" to name.uuid,
                "value" to name.value,
                "shortName" to (name.shortName ?: ""),
                "nameType" to TypeKeyNameUrlDto(
                    technicalKey = (name.type.technicalKey),
                    name = name.type.name,
                    url = name.type.url
                ),
                "language" to TypeKeyNameDto(
                    technicalKey = name.language.technicalKey,
                    name = name.language.name
                )
            ),
            holderIdentifier = bpn
        )
    }

    private fun prepareLegalFormCredential(
        bpn: String,
        legalForm: LegalFormDto
    ): VerifiableCredentialRequestWithoutIssuerDto {
        val currentDateAsString = JsonLDUtils.dateToString(Date.from(Instant.now()))
        return VerifiableCredentialRequestWithoutIssuerDto(
            id = UUID.randomUUID().toString(),
            context = listOf(
                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS
            ),
            type = listOf(JsonLdTypes.LEGAL_FORM_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
            issuanceDate = currentDateAsString,
            credentialSubject = mapOf(
                "type" to listOf(JsonLdTypes.LEGAL_FORM_TYPE),
                "technicalKey" to legalForm.technicalKey,
                "name" to legalForm.name,
                "url" to (legalForm.url ?: ""),
                "mainAbbreviation" to (legalForm.mainAbbreviation ?: ""),
                "language" to legalForm.language,
                "categories" to legalForm.categories
            ),
            holderIdentifier = bpn
        )
    }

    private fun prepareBankAccountCredential(
        bpn: String,
        bankAccount: BankAccountDto
    ): VerifiableCredentialRequestWithoutIssuerDto {
        val currentDateAsString = JsonLDUtils.dateToString(Date.from(Instant.now()))
        return VerifiableCredentialRequestWithoutIssuerDto(
            id = bankAccount.uuid,
            context = listOf(
                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS
            ),
            type = listOf(JsonLdTypes.BANK_ACCOUNT_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
            issuanceDate = currentDateAsString,
            credentialSubject = mapOf(
                "type" to listOf(JsonLdTypes.BANK_ACCOUNT_TYPE),
                "uuid" to bankAccount.uuid,
                "trustScores" to bankAccount.trustScores,
                "currency" to TypeKeyNameDto(
                    technicalKey = bankAccount.currency.technicalKey,
                    name = bankAccount.currency.name,
                ),
                "internationalBankAccountIdentifier" to bankAccount.internationalBankAccountIdentifier,
                "internationalBankIdentifier" to bankAccount.internationalBankIdentifier,
                "nationalBankAccountIdentifier" to bankAccount.nationalBankAccountIdentifier,
                "nationalBankIdentifier" to bankAccount.nationalBankIdentifier
            ),
            holderIdentifier = bpn
        )
    }

    private fun prepareAddressCredential(
        bpn: String,
        address: AddressDto
    ): VerifiableCredentialRequestWithoutIssuerDto {
        val currentDateAsString = JsonLDUtils.dateToString(Date.from(Instant.now()))

        val credSubject = mutableMapOf<String, Any>()
        credSubject["type"] = listOf(JsonLdTypes.ADDRESS_TYPE, JsonLdTypes.CREDENTIAL_TYPE)
        credSubject["uuid"] = address.uuid
        credSubject["version"] = address.version
        if (address.careOf != null) {
            credSubject["careOf"] = address.careOf
        }
        if (address.contexts.isNotEmpty()) {
            credSubject["contexts"] = address.contexts
        }
        credSubject["country"] = address.country
        val administrativeAreas = mutableListOf<Any>()
        address.administrativeAreas.forEach {
            val administrativeArea = mapOf(
                "uuid" to it.uuid,
                "value" to it.value,
                "shortName" to (it.shortName ?: ""),
                "fipsCode" to (it.fipsCode ?: ""),
                "administrativeAreasType" to mapOf(
                    "technicalKey" to it.type.technicalKey,
                    "name" to it.type.name,
                    "url" to (it.type.url ?: "")
                ),
                "language" to mapOf(
                    "technicalKey" to it.language.technicalKey,
                    "name" to it.name
                )
            )
            administrativeAreas.add(administrativeArea)
        }
        credSubject["administrativeAreas"] = administrativeAreas
        val postCodes = mutableListOf<Any>()
        address.postCodes.forEach {
            val postCode = mapOf(
                "uuid" to it.uuid,
                "value" to it.value,
                "postCodesType" to mapOf(
                    "technicalKey" to it.type.technicalKey,
                    "name" to it.type.name,
                    "url" to (it.type.url ?: "")
                )
            )
            postCodes.add(postCode)
        }
        credSubject["postCodes"] = postCodes
        val localities = mutableListOf<Any>()
        address.localities.forEach {
            val locality = mapOf(
                "uuid" to it.uuid,
                "value" to it.value,
                "shortName" to (it.shortName ?: ""),
                "localitiesType" to mapOf(
                    "technicalKey" to it.type.technicalKey,
                    "name" to it.type.name,
                    "url" to (it.type.url ?: "")
                ),
                "language" to mapOf(
                    "technicalKey" to it.language.technicalKey,
                    "name" to (it.language.name)
                )
            )
            localities.add(locality)
        }
        credSubject["localities"] = localities

        val thoroughfares = mutableListOf<Any>()
        address.thoroughfares.forEach {
            val thoroughfare = mapOf(
                "uuid" to it.uuid,
                "value" to it.value,
                "name" to (it.name ?: ""),
                "shortName" to (it.shortName ?: ""),
                "number" to (it.number ?: ""),
                "direction" to (it.direction ?: ""),
                "thoroughfaresType" to mapOf(
                    "technicalKey" to it.type.technicalKey,
                    "name" to it.type.name,
                    "url" to (it.type.url ?: "")
                ),
                "language" to mapOf(
                    "technicalKey" to it.language.technicalKey,
                    "name" to it.language.name
                )
            )
            thoroughfares.add(thoroughfare)
        }
        credSubject["thoroughfares"] = thoroughfares
        if (address.premises.isNotEmpty()) {
            credSubject["premises"] = address.premises
        }
        if (address.postalDeliveryPoints.isNotEmpty()) {
            credSubject["postalDeliveryPoints"] = address.postalDeliveryPoints
        }
        if (address.geographicCoordinates != null) {
            credSubject["geographicCoordinates"] = address.geographicCoordinates
        }
        if (address.types.isNotEmpty()) {
            credSubject["types"] = address.types
        }
        return VerifiableCredentialRequestWithoutIssuerDto(
            id = address.uuid,
            context = listOf(
                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS
            ),
            type = listOf(JsonLdTypes.ADDRESS_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
            issuanceDate = currentDateAsString,
            credentialSubject = credSubject,
            holderIdentifier = bpn
        )
    }

    private fun prepareMembershipCredential(bpn: String): VerifiableCredentialRequestWithoutIssuerDto {
        val currentDateAsString = JsonLDUtils.dateToString(Date.from(Instant.now()))
        return VerifiableCredentialRequestWithoutIssuerDto(
            id = UUID.randomUUID().toString(),
            context = listOf(
                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS
            ),
            type = listOf(JsonLdTypes.MEMBERSHIP_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
            issuanceDate = currentDateAsString,
            credentialSubject = mapOf(
                "type" to listOf(JsonLdTypes.MEMBERSHIP_TYPE),
                "memberOf" to "Catena-X",
                "status" to "Active",
                "startTime" to currentDateAsString
            ),
            holderIdentifier = bpn
        )
    }

    private fun prepareBpnCredentials(bpn: String): VerifiableCredentialRequestWithoutIssuerDto {
        return VerifiableCredentialRequestWithoutIssuerDto(
            id = UUID.randomUUID().toString(),
            context = listOf(
                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS
            ),
            type = listOf(JsonLdTypes.BPN_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
            issuanceDate = JsonLDUtils.dateToString(Date.from(Instant.now())),
            credentialSubject = mapOf(
                "type" to listOf(JsonLdTypes.BPN_TYPE),
                "bpn" to bpn
            ),
            holderIdentifier = bpn
        )
    }

    private fun toIssuedVerifiableCredentialRequestDto(
        vcDto: VerifiableCredentialDto
    ): IssuedVerifiableCredentialRequestDto? {
        if (vcDto.proof != null) {
            return IssuedVerifiableCredentialRequestDto(
                id = vcDto.id,
                type = vcDto.type,
                context = vcDto.context,
                issuer = vcDto.issuer,
                issuanceDate = vcDto.issuanceDate,
                expirationDate = vcDto.expirationDate,
                credentialSubject = vcDto.credentialSubject,
                proof = vcDto.proof
            )
        }
        return null
    }

}
