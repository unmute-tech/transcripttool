package io.reitmaier.transcripttool.core.data.domain

import io.reitmaier.transcripttool.core.data.parcelizers.InstantEpochSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
internal data class TaskDto(
  val id: RemoteId,
  val displayName: String,
  val lengthMs: Long,
  val provenance: TaskProvenance,
  val transcript: String,
  val rejectReason: RejectReason?,

  @Serializable(with = InstantEpochSerializer::class)
  val created_at: Instant,
  @Serializable(with = InstantEpochSerializer::class)
  val updated_at: Instant,

  @Serializable(with = InstantEpochSerializer::class)
  val completed_at: Instant?,
)
