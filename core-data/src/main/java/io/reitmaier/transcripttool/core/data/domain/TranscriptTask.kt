package io.reitmaier.transcripttool.core.data.domain

import android.os.Parcelable
import io.reitmaier.transcripttool.core.data.parcelizers.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import kotlinx.serialization.Serializable

enum class TaskState(val order: Int) {
  NEW(0),
  IN_PROGRESS(1),
  COMPLETED(2),
  REJECTED(3),
}

@Parcelize
data class TranscriptTask(
  val id: @WriteWith<TaskIdParceler> TaskId,
  val remoteUrl: String,
  val remoteId: @WriteWith<RemoteIdParceler> RemoteId,
  val length: Int,
  val regionLength: Int,
  val taskProvenance: TaskProvenance,
  val displayName: String,
  val createdAt: @WriteWith<InstantParceler> Instant,
  val updatedAt: @WriteWith<InstantParceler> Instant,
  val completedAt: @WriteWith<NullableInstantParceler> Instant?,
  val submittedAt: @WriteWith<NullableInstantParceler> Instant?,
  val inputtedTranscript: @WriteWith<InputtedTranscriptParceler> InputtedTranscript,
  val savedTranscript: @WriteWith<SavedTranscriptParceler> SavedTranscript,
  val submittedTranscript: @WriteWith<NullableSubmittedTranscriptParceler> SubmittedTranscript?,
  val rejectReason: RejectReason?,
  val difficulty: Difficulty?,
//  val activeRegion: @WriteWith<NullableRegionIdParceler> RegionId?,
) : Parcelable {
  val state : TaskState
    get() {
      return if(rejectReason != null) {
        TaskState.REJECTED
      } else if (completedAt != null) {
        TaskState.COMPLETED
      } else if(updatedAt > createdAt) {
        TaskState.IN_PROGRESS
      } else TaskState.NEW
    }
  val isSynced : Boolean
    get() = (submittedAt != null && submittedAt >= updatedAt)

  companion object {
    val Preview = TranscriptTask(
      id = TaskId(1),
      remoteUrl = "https://url",
      remoteId = RemoteId.Preview,
      length = 5000,
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now(),
      completedAt = Clock.System.now(),
      submittedAt = Clock.System.now(),
      inputtedTranscript  = InputtedTranscript.Preview,
      savedTranscript = SavedTranscript.Preview,
      submittedTranscript = SubmittedTranscript.Preview,
      rejectReason = null,
//      activeRegion = RegionId(1),
      regionLength = REGION_LENGTH.toInt(),
      taskProvenance = TaskProvenance.LOCAL,
      displayName = "PTT-20210804-WA0002.opus",
      difficulty = Difficulty.HARD,
      )

    val Preview2 = Preview.copy(id = TaskId(2), displayName = "PTT-20210804-WA0003.opus")
    val Preview3 = Preview.copy(id = TaskId(3), displayName = "PTT-20210804-WA0005.opus")
  }
}

enum class TaskProvenance(val provenance: String) {
  LOCAL("LOCAL"),
  REMOTE("REMOTE")
}

@Serializable
enum class RejectReason(val value: String) {
  BLANK("BLANK"),
  INAPPROPRIATE("INAPPROPRIATE"),
  UNDERAGE("UNDERAGE"),
}

