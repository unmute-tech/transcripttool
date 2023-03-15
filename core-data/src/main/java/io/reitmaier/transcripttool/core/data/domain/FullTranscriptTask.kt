package io.reitmaier.transcripttool.core.data.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FullTranscriptTask(
  val task: TranscriptTask,
  val regions: List<Region>,
  val partialTranscripts: List<PartialTranscript>,
  val localFilePath: String,
//  val inputtedTranscript: @WriteWith<InputtedTranscriptParceler> InputtedTranscript,
//  val savedTranscript: @WriteWith<SavedTranscriptParceler> SavedTranscript,
//  val submittedTranscript: @WriteWith<NullableSubmittedTranscriptParceler> SubmittedTranscript?,
) : Parcelable {
  fun toNewTranscriptsRequest() : List<NewTranscript> {
    return partialTranscripts.sortedByDescending { it.updatedAt }.map { partialTranscript ->
      val region = regions.first { region -> region.id == partialTranscript.regionId}
      NewTranscript(
        regionStart = region.start,
        regionEnd = region.end,
        updatedAt = partialTranscript.updatedAt,
        transcript = partialTranscript.content.value,
      )
    }
  }
}


