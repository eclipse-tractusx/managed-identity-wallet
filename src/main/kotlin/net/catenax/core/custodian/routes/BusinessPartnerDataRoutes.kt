package net.catenax.core.custodian.routes

import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.services.BusinessPartnerDataService

fun Route.businessPartnerDataRoutes(businessPartnerDataService: BusinessPartnerDataService) {

    route("/businessPartnerData") {
        notarizedPost(
            PostInfo<Unit, BusinessPartnerDataUpdateRequestDto, String>(
                summary = "Update business partner data in the corresponding wallet",
                description = "Create or update data associated with a given bpn in the corresponding wallet by creating or updating verifiable credentials",
                requestInfo = RequestInfo(
                    description = "The input data to use for the update",
                    examples = dataUpdateRequestDtoExample
                ),
                responseInfo = ResponseInfo(
                    status = HttpStatusCode.Accepted,
                    description = "Empty response body"
                ),
                canThrow = setOf(semanticallyInvalidInputException),
                tags = setOf("BusinessPartnerData")
            )
        ) {
            val dataUpdateRequestDto = call.receive<BusinessPartnerDataUpdateRequestDto>()
            call.respond(
                HttpStatusCode.Accepted
            )
        }
    }
}

val dataUpdateRequestDtoExample = mapOf(
    "demo" to BusinessPartnerDataUpdateRequestDto(
        bpn = "BPNL000000000001",
        identifiers = listOf(
          IdentifierDto(
            uuid = "089e828d-01ed-4d3e-ab1e-cccca26814b3",
            value = "BPNL000000000001",
            type = ExtendedNameDto(
              technicalKey = "BPN",
              name = "Business Partner Number",
              url = ""
            ),
            issuingBody = ExtendedNameDto(
              technicalKey = "CATENAX",
              name = "Catena-X",
              url = ""
            ),
            status = ExtendedNameDto(
              technicalKey = "UNKNOWN",
              name = "Unknown"
            )
          )
        ),
        names = listOf(
          NameDto(
            uuid = "de3f3db6-e337-436b-a4e0-fc7d17e8af89",
            value = "German Car Company",
            shortName = "GCC",
            type = ExtendedNameDto(
              technicalKey = "REGISTERED",
              name = "The main name under which a business is officially registered in a country's business register.",
              url = ""
            ),
            language = ExtendedNameDto(
              technicalKey = "undefined",
              name = "Undefined"
            )
          )
        ),
        legalForm = LegalFormDto(
          technicalKey = "DE_AG",
          name = "Aktiengesellschaft",
          url = "",
          mainAbbreviation = "AG",
          language = ExtendedNameDto(
            technicalKey = "de",
            name = "German"
          ),
          categories = listOf(
            ExtendedNameDto(
              name = "AG",
              url = ""
            )
          )
        ),
        status = null,
        addresses = listOf(EmptyDto()),
        profileClassifications = listOf(EmptyDto()),
        types = listOf(
          ExtendedNameDto(
            technicalKey = "LEGAL_ENTITY",
            name = "Legal Entity",
            url = ""
          )
        ),
        bankAccounts = listOf(EmptyDto()),
        roles = listOf(EmptyDto()),
        relations = listOf(EmptyDto())
    )
)