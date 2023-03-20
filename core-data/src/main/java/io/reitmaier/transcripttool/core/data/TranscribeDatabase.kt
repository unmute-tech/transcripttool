package io.reitmaier.transcripttool.core.data

import com.github.michaelbull.result.Result
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import io.reitmaier.transcripttool.core.data.domain.DatabaseError
import io.reitmaier.transcripttool.core.data.domain.DbResult
import io.reitmaier.transcripttool.core.data.domain.Difficulty
import io.reitmaier.transcripttool.core.data.domain.FileId
import io.reitmaier.transcripttool.core.data.domain.LocalId
import io.reitmaier.transcripttool.core.data.domain.PartialTranscriptId
import io.reitmaier.transcripttool.core.data.domain.RegionId
import io.reitmaier.transcripttool.core.data.domain.RejectReason
import io.reitmaier.transcripttool.core.data.domain.RemoteId
import io.reitmaier.transcripttool.core.data.domain.SavedTranscript
import io.reitmaier.transcripttool.core.data.domain.SubmittedTranscript
import io.reitmaier.transcripttool.core.data.domain.TaskId
import io.reitmaier.transcripttool.data.File_Info
import io.reitmaier.transcripttool.data.Local_Entity
import io.reitmaier.transcripttool.data.Partial_Transcript_Entity
import io.reitmaier.transcripttool.data.Region_Entity
import io.reitmaier.transcripttool.data.Task_Entity
import io.reitmaier.transcripttool.data.TranscriptToolDb
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.io.File

interface TranscribeDatabase {
  companion object Factory {
    fun build(sqlDriver: SqlDriver): TranscribeDatabase {
      return TranscribeDatabaseImpl(
        TranscriptToolDb(
          driver = sqlDriver,
          Region_EntityAdapter = Region_Entity.Adapter(
            idAdapter = regionIdAdapter,
            task_idAdapter = taskIdAdapter,
          ),
          Task_EntityAdapter = Task_Entity.Adapter(
            completed_atAdapter = instantAdapter,
            created_atAdapter = instantAdapter,
            submitted_atAdapter = instantAdapter,
            updated_atAdapter = instantAdapter,
            idAdapter = taskIdAdapter,
            remote_idAdapter = remoteIdAdapter,
            latest_transcriptAdapter = savedTranscriptAdapter,
            submitted_transcriptAdapter = submittedTranscriptAdapter,
            local_entityAdapter = localIdAdapter,
            provenanceAdapter = EnumColumnAdapter(),
            reject_reasonAdapter = EnumColumnAdapter(),
            difficultyAdapter = EnumColumnAdapter(),
            confidenceAdapter = EnumColumnAdapter(),
          ),
          Partial_Transcript_EntityAdapter = Partial_Transcript_Entity.Adapter(
            idAdapter = partialTranscriptionIdAdapter,
            task_idAdapter = taskIdAdapter,
            region_idAdapter = regionIdAdapter,
            updated_atAdapter = instantAdapter,
            contentAdapter = savedTranscriptAdapter,
          ),
          Local_EntityAdapter = Local_Entity.Adapter(
            idAdapter = localIdAdapter,
            file_idAdapter = fileIdAdapter,
            created_atAdapter = instantAdapter,
            updated_atAdapter = instantAdapter,
          ),
          File_InfoAdapter = File_Info.Adapter(

            idAdapter = fileIdAdapter,
            created_atAdapter = instantAdapter,
            updated_atAdapter = instantAdapter,
          ),
        ),
      )
    }

    val schema: SqlDriver.Schema = TranscriptToolDb.Schema
    const val dbName: String = "transcribe.db"

    private val regionIdAdapter = object : ColumnAdapter<RegionId, Long> {
      override fun decode(databaseValue: Long): RegionId = RegionId(databaseValue.toInt())
      override fun encode(value: RegionId): Long = value.value.toLong()
    }

    private val partialTranscriptionIdAdapter = object : ColumnAdapter<PartialTranscriptId, Long> {
      override fun decode(databaseValue: Long): PartialTranscriptId = PartialTranscriptId(databaseValue.toInt())
      override fun encode(value: PartialTranscriptId): Long = value.value.toLong()
    }

    private val localIdAdapter = object : ColumnAdapter<LocalId, Long> {
      override fun decode(databaseValue: Long): LocalId = LocalId(databaseValue.toInt())
      override fun encode(value: LocalId): Long = value.value.toLong()
    }

    private val fileIdAdapter = object : ColumnAdapter<FileId, Long> {
      override fun decode(databaseValue: Long): FileId = FileId(databaseValue.toInt())
      override fun encode(value: FileId): Long = value.value.toLong()
    }

    private val taskIdAdapter = object : ColumnAdapter<TaskId, Long> {
      override fun decode(databaseValue: Long): TaskId = TaskId(databaseValue.toInt())
      override fun encode(value: TaskId): Long = value.value.toLong()
    }

    private val remoteIdAdapter = object : ColumnAdapter<RemoteId, Long> {
      override fun decode(databaseValue: Long): RemoteId = RemoteId(databaseValue.toInt())
      override fun encode(value: RemoteId): Long = value.value.toLong()
    }

    private val submittedTranscriptAdapter = object : ColumnAdapter<SubmittedTranscript, String> {
      override fun decode(databaseValue: String): SubmittedTranscript = SubmittedTranscript(databaseValue)
      override fun encode(value: SubmittedTranscript): String = value.value
    }

    private val savedTranscriptAdapter = object : ColumnAdapter<SavedTranscript, String> {
      override fun decode(databaseValue: String): SavedTranscript = SavedTranscript(databaseValue)
      override fun encode(value: SavedTranscript): String = value.value
    }

//    private val instantAdapter = object : ColumnAdapter<Instant, Long> {
//      override fun decode(databaseValue: Long) = Instant.fromEpochSeconds(databaseValue)
//      override fun encode(value: Instant) = value.epochSeconds
//    }

    private val instantAdapter = object : ColumnAdapter<Instant, Long> {
      override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)
      override fun encode(value: Instant) = value.toEpochMilliseconds()
    }

    private val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
      override fun decode(databaseValue: Long) = databaseValue == 0L
      override fun encode(value: Boolean) = if (value) 1L else 0L
    }
  }

  fun allTasks(): Flow<List<Task_Entity>>
  fun getTask(id: TaskId): DbResult<Task_Entity>

  //  fun getTaskAsFlow(id: TaskId) : Flow<Task_Entity>
  fun deleteTask(id: TaskId)
  fun insertLocalTask(
    remoteId: RemoteId,
    remoteUrl: String,
    fileId: FileId,
    path: String,
    displayName: String,
    durationMs: Long,
  ): DbResult<Task_Entity>
  fun markTaskUploaded(id: TaskId): DbResult<Unit>
  fun rejectTask(taskId: TaskId, rejectReason: RejectReason): DbResult<Unit>

  fun getAllRegions(id: TaskId): List<Region_Entity>
  fun getActiveRegions(id: TaskId): List<Region_Entity>
  fun markRegionsInActive(id: TaskId)
  fun getRegion(id: RegionId): Region_Entity?
  fun deleteRegion(id: RegionId)
  fun insertRegion(taskId: TaskId, start: Int, end: Int): Region_Entity

  fun getPartialTranscripts(id: TaskId): List<Partial_Transcript_Entity>
  fun getPartialTranscript(id: PartialTranscriptId): Partial_Transcript_Entity?
  fun getLatestPartialTranscript(id: TaskId): Partial_Transcript_Entity?
  fun insertPartialTranscript(
    region: Region_Entity,
    transcript: String,
  ): Partial_Transcript_Entity
  fun updatePartialTranscript(id: PartialTranscriptId, transcript: String): SavedTranscript

  fun insertFileInfo(
    extension: String,
    origUri: String,
    origDisplayName: String?,
  ): File_Info

  fun getFileName(id: FileId): String
  fun deleteFileInfo(id: FileId)

  fun addLocalAudioFile(taskId: TaskId, file: File, fileId: FileId): DbResult<Task_Entity>
  fun getLocalEntity(id: LocalId): Local_Entity
  fun getTaskByRemoteId(remoteId: RemoteId): DbResult<Task_Entity>
  fun insertRemoteTask(
    remoteId: RemoteId,
    remoteUrl: String,
    displayName: String,
    durationMs: Long,
  ): DbResult<Task_Entity>

  fun nextTask(id: TaskId): DbResult<Task_Entity>
  fun setSubmittedTranscript(id: TaskId, transcript: SubmittedTranscript): DbResult<Unit>
  fun completeTask(
    taskId: TaskId,
    difficulty: Difficulty,
  ): Result<Unit, DatabaseError>
}
