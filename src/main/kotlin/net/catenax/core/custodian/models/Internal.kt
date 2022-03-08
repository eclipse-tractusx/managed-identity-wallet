package net.catenax.core.custodian.models

import io.bkbn.kompendium.core.metadata.ExceptionInfo
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class ExceptionResponse(val message: String, val error: Boolean = true)

@Serializable
data class SuccessResponse(val message: String)

class BadRequestException(message: String) : Exception(message)

class ConflictException(message: String) : Exception(message)

class NotFoundException(message: String) : Exception(message)

val semanticallyInvalidInputException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The input can not be processed due to semantic mismatches",
    status = HttpStatusCode.UnprocessableEntity,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val syntacticallyInvalidInputException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The input does not comply to the syntax requirements",
    status = HttpStatusCode.BadRequest,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val notFoundException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The required entity does not exists",
    status = HttpStatusCode.NotFound,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val conflictException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The request could not be completed due to a conflict.",
    status = HttpStatusCode.Conflict,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)
