package net.catenax.core.custodian.models

import kotlinx.serialization.Serializable

@Serializable
data class ExceptionResponse(val message: String, val error: Boolean = true)

@Serializable
data class SuccessResponse(val message: String)

class BadRequestException(message: String) : Exception(message)

class NotFoundException(message: String) : Exception(message)
