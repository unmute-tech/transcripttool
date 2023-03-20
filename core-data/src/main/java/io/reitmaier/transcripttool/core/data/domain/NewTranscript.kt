package io.reitmaier.transcripttool.core.data.domain

import io.reitmaier.transcripttool.core.data.parcelizers.InstantEpochSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class NewTranscript(
  val transcript: String,
  val regionStart: Int,
  val regionEnd: Int,
  @Serializable(with = InstantEpochSerializer::class)
  val updatedAt: Instant,
)
