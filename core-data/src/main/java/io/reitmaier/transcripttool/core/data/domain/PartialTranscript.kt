package io.reitmaier.transcripttool.core.data.domain

import android.os.Parcelable
import io.reitmaier.transcripttool.core.data.parcelizers.*
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

@Parcelize
data class PartialTranscript(
  val id: @WriteWith<PartialTranscriptIdParceler> PartialTranscriptId,
  val taskId: @WriteWith<TaskIdParceler> TaskId,
  val regionId: @WriteWith<RegionIdParceler> RegionId,
  val content: @WriteWith<SavedTranscriptParceler> SavedTranscript,
  val updatedAt: @WriteWith<InstantParceler> Instant,
) : Parcelable
