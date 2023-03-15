package io.reitmaier.transcripttool.core.data.domain
import io.reitmaier.transcripttool.core.data.parcelizers.InstantEpochSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
  val accessToken: String,
  val refreshToken: String,
  @Serializable(with = InstantEpochSerializer::class)
  val expiresAt: Instant,
)
