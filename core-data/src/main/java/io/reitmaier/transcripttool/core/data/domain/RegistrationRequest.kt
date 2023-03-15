package io.reitmaier.transcripttool.core.data.domain

import kotlinx.serialization.Serializable
import io.reitmaier.transcripttool.core.data.parcelizers.MobileNumberSerializer
import io.reitmaier.transcripttool.core.data.parcelizers.MobileOperatorSerializer
import io.reitmaier.transcripttool.core.data.parcelizers.NameSerializer
import io.reitmaier.transcripttool.core.data.parcelizers.PasswordSerializer

@Serializable(with = MobileNumberSerializer::class)
@JvmInline
value class MobileNumber(val value: String)

@Serializable(with = MobileOperatorSerializer::class)
@JvmInline
value class MobileOperator(val value: String)

@Serializable(with = NameSerializer::class)
@JvmInline
value class Name(val value: String)

@Serializable(with = PasswordSerializer::class)
@JvmInline
value class Password(val value: String)

@Serializable
data class RegistrationRequest(
  val mobile: MobileNumber,
  val operator: MobileOperator,
  val name: Name,
  val password: Password,
)
