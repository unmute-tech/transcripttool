package io.reitmaier.transcripttool.core.data.domain

import io.reitmaier.transcripttool.core.data.parcelizers.InstantEpochSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
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
  @SerialName("created_at")
  val createdAt: Instant,
  @Serializable(with = InstantEpochSerializer::class)
  @SerialName("updated_at")
  val updatedAt: Instant,

  @Serializable(with = InstantEpochSerializer::class)
  @SerialName("completed_at")
  val completedAt: Instant?,
)
