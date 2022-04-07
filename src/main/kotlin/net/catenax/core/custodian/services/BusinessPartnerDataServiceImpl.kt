package net.catenax.core.custodian.services

import foundation.identity.jsonld.JsonLDUtils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class BusinessPartnerDataServiceImpl(private val walletService: WalletService) : BusinessPartnerDataService {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    override suspend fun issueAndStoreCatenaXCredentialsAsync(
        bpn: String,
        type: String
    ): Deferred<Boolean> = GlobalScope.async {
        try {
            val vcToIssue = when (type) {
                JsonLdTypes.MEMBERSHIP_TYPE -> prepareMembershipCredential(bpn)
                JsonLdTypes.BPN_TYPE -> prepareBpnCredentials(bpn)
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
