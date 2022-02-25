package net.catenax.core.custodian.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequestDto(
  val access_token: String,
  val expires_in: Integer,
  val refresh_expires_in: Integer,
  val refresh_token: String,
  val id_token: String,
  val token_type: String,
  val scope: String) {
}
