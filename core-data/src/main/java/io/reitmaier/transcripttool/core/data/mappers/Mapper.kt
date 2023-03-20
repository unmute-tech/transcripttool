@file:Suppress("FunctionName")

package io.reitmaier.transcripttool.core.data.mappers

import io.reitmaier.transcripttool.core.data.domain.InputtedTranscript
import io.reitmaier.transcripttool.core.data.domain.PartialTranscript
import io.reitmaier.transcripttool.core.data.domain.Region
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.data.Partial_Transcript_Entity
import io.reitmaier.transcripttool.data.Region_Entity
import io.reitmaier.transcripttool.data.Task_Entity

fun Task_Entity.toTranscriptTask(): TranscriptTask {
  return ToTranscriptTask.fromTask_Entity(this)
}

internal object ToTranscriptTask {
  fun fromTask_Entity(taskEntity: Task_Entity): TranscriptTask =
    TranscriptTask(
      id = taskEntity.id,
      remoteUrl = taskEntity.remote_url,
      remoteId = taskEntity.remote_id,
      updatedAt = taskEntity.updated_at,
      createdAt = taskEntity.created_at,
      submittedTranscript = taskEntity.submitted_transcript,
      savedTranscript = taskEntity.latest_transcript,
      inputtedTranscript = InputtedTranscript(taskEntity.latest_transcript.value),
      submittedAt = taskEntity.submitted_at,
      completedAt = taskEntity.completed_at,
      length = taskEntity.length.toInt(),
      regionLength = taskEntity.region_length.toInt(),
      taskProvenance = taskEntity.provenance,
      displayName = taskEntity.display_name,
      rejectReason = taskEntity.reject_reason,
      difficulty = taskEntity.difficulty,
    )
}

internal object ToRegion {
  fun fromRegion_Entity(regionEntity: Region_Entity): Region =
    Region(
      id = regionEntity.id,
      taskId = regionEntity.task_id,
      start = regionEntity.start.toInt(),
      end = regionEntity.end.toInt(),
      playCount = regionEntity.play_count.toInt(),
      active = regionEntity.active,
    )
}

internal object ToPartialTranscript {
  fun fromPartial_Transcript_Entity(partialTranscriptEntity: Partial_Transcript_Entity): PartialTranscript =
    PartialTranscript(
      id = partialTranscriptEntity.id,
      taskId = partialTranscriptEntity.task_id,
      content = partialTranscriptEntity.content,
      regionId = partialTranscriptEntity.region_id,
      updatedAt = partialTranscriptEntity.updated_at,
    )
}
