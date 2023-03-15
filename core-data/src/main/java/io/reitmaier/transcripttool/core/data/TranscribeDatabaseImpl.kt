package io.reitmaier.transcripttool.core.data

import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import io.reitmaier.transcripttool.core.data.domain.*
import io.reitmaier.transcripttool.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.*
import java.io.File

// TODO Dispatchers IO
class TranscribeDatabaseImpl(
  transcriptToolDb: TranscriptToolDb,
//  private val dispatcher: CoroutineScope
): TranscribeDatabase {
  private val queries = transcriptToolDb.transcriptTaskQueries
  override fun allTasks(): Flow<List<Task_Entity>> =
    queries.allTasks().asFlow().mapToList()

  override fun getTask(id: TaskId): DbResult<Task_Entity> =
  runCatching {
    queries.getTask(id).executeAsOne()
  }.mapError { DatabaseError }

  override fun nextTask(id: TaskId): DbResult<Task_Entity> =
    runCatching {
      queries.nextTask(id).executeAsOne()
    }.mapError { DatabaseError }

  override fun rejectTask(taskId: TaskId, rejectReason: RejectReason) =
    runCatching {
      queries.rejectTask(rejectReason, taskId)
    }.mapError { DatabaseError }

  override fun completeTask(taskId: TaskId, difficulty: Difficulty) =
    runCatching {
      queries.completeTask(difficulty, taskId)
    }.mapError { DatabaseError }

  override fun getTaskByRemoteId(remoteId: RemoteId): DbResult<Task_Entity> =
    queries.getTaskByRemoteId(remoteId).executeAsOneOrNull().toResultOr { DatabaseError }

//  override fun getTaskAsFlow(id: TaskId): Flow<Task_Entity> =
//    queries.getTask(id).asFlow().mapToOneNotNull()

  override fun deleteTask(id: TaskId) =
    queries.deleteTask(id)

  private fun now() : Instant =
    Clock.System.now()

  override fun insertRemoteTask(
    remoteId: RemoteId,
    remoteUrl: String,
    displayName: String,
    durationMs: Long
  ): DbResult<Task_Entity> =
    runCatching<TranscribeDatabaseImpl, Task_Entity> {
      val timestamp = now()
      queries.transactionWithResult {
        queries.insertTask(
          id = null,
          remote_id = remoteId,
          remote_url = remoteUrl,
          length = durationMs,
          region_length = REGION_LENGTH,
          provenance = TaskProvenance.REMOTE,
          display_name = displayName,

          created_at = timestamp,
          updated_at = timestamp,

          local_entity = null,
        )
        val taskId = TaskId(queries.rowid().executeAsOne().toInt())
        queries.getTask(taskId).executeAsOne()
      }
    }.mapError { DatabaseError }
  override fun insertLocalTask(
    remoteId: RemoteId,
    remoteUrl: String,
    fileId: FileId,
    path: String,
    displayName: String,
    durationMs: Long
  ): DbResult<Task_Entity> =
    runCatching<TranscribeDatabaseImpl, Task_Entity> {
      val timestamp = now()
      queries.transactionWithResult {
        queries.insertLocalEntity(
          id = null,
          file_id = fileId,
          path = path,
          created_at = timestamp,
          updated_at = timestamp,
        )
        val localId = LocalId(queries.rowid().executeAsOne().toInt())
        queries.insertTask(
          id = null,
          remote_id = remoteId,
          remote_url = remoteUrl,
          length = durationMs,
          region_length = REGION_LENGTH,
          provenance = TaskProvenance.LOCAL,
          display_name = displayName,

          created_at = timestamp,
          updated_at = timestamp,

          local_entity = localId,
        )
        val taskId = TaskId(queries.rowid().executeAsOne().toInt())
        queries.getTask(taskId).executeAsOne()
      }
    }.mapError { DatabaseError }

  override fun setSubmittedTranscript(id: TaskId, transcript: SubmittedTranscript) : DbResult<Unit> =
    runCatching {
      queries.transaction {
        queries.updateSubmittedTranscript(transcript, id)
        queries.markTaskUploaded(id)
      }
    }.mapError { DatabaseError }

  override fun markTaskUploaded(id: TaskId) : DbResult<Unit> =
    runCatching {
      queries.markTaskUploaded(id)
    }.mapError { DatabaseError }

  override fun addLocalAudioFile(taskId: TaskId, file: File, fileId: FileId): DbResult<Task_Entity> =
    runCatching {
      queries.transactionWithResult<Task_Entity> {
        val timestamp = now()
        queries.insertLocalEntity(
          id = null,
          file_id = fileId,
          path = file.path,
          created_at = timestamp,
          updated_at = timestamp,
        )
        val localId = LocalId(queries.rowid().executeAsOne().toInt())
        queries.addLocalEntityToTask(localId,taskId)
        queries.getTask(taskId).executeAsOne()
      }
    }.mapError { DatabaseError }

  override fun getActiveRegions(id: TaskId): List<Region_Entity> =
    queries.getActiveRegions(id).executeAsList()

  override fun markRegionsInActive(id: TaskId) {
    queries.markRegionsInActive(id)
  }

  override fun getAllRegions(id: TaskId): List<Region_Entity> =
    queries.getAllRegions(id).executeAsList()

  override fun getRegion(id: RegionId): Region_Entity? =
    queries.getRegion(id).executeAsOneOrNull()

  override fun deleteRegion(id: RegionId) =
    queries.deleteRegion(id)

  override fun insertRegion(taskId: TaskId, start: Int, end: Int): Region_Entity =
    queries.transactionWithResult {
      queries.insertRegion(
        id = null,
        task_id = taskId,
        start = start.toLong(),
        end = end.toLong(),
        active = true,
        play_count = 0
      )
      val id = RegionId(queries.rowid().executeAsOne().toInt())
      queries.getRegion(id).executeAsOne()
    }

  override fun getPartialTranscripts(id: TaskId): List<Partial_Transcript_Entity> =
    queries.getPartialTranscripts(id).executeAsList()

  override fun getLatestPartialTranscript(id: TaskId): Partial_Transcript_Entity? =
    queries.getLatestPartialTranscript(id).executeAsOneOrNull()

  override fun getPartialTranscript(id: PartialTranscriptId): Partial_Transcript_Entity? =
    queries.getPartialTranscript(id).executeAsOneOrNull()

  override fun insertPartialTranscript(
    region: Region_Entity,
    transcript: String
  ): Partial_Transcript_Entity =
    queries.transactionWithResult {
      queries.insertPartialTranscription(
        id = null,
        task_id = region.task_id,
        region_id = region.id,
        content = SavedTranscript(transcript),
        updated_at = Clock.System.now()
      )
      val id = PartialTranscriptId(queries.rowid().executeAsOne().toInt())
      queries.getPartialTranscript(id).executeAsOne()
    }

  override fun updatePartialTranscript(id: PartialTranscriptId, transcript: String) : SavedTranscript {
    queries.updatePartialTranscription(SavedTranscript(transcript),id)
    return queries.getPartialTranscript(id).executeAsOne().content
  }

  override fun deleteFileInfo(id: FileId) {
    queries.deleteFileInfo(id)
  }

  override fun insertFileInfo(
    extension: String,
    orig_uri: String,
    orig_display_name: String?
  ): File_Info {
    val timestamp = now()
    queries.insertFileInfo(
      id = null,
      extension = extension,
      orig_uri = orig_uri,
      created_at = timestamp,
      updated_at = timestamp,
      orig_display_name = orig_display_name,
    )

    val id = FileId(queries.rowid().executeAsOne().toInt())
    return queries.getFileInfo(id).executeAsOne()
  }

  override fun getFileName(
    id: FileId
  ) : String =
    queries.getFileName(id).executeAsOne()

  override fun getLocalEntity(
    id: LocalId
  ) : Local_Entity =
    queries.getLocalEntity(id).executeAsOne()
}
