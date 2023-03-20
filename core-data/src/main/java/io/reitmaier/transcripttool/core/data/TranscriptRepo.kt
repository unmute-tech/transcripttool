package io.reitmaier.transcripttool.core.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.get
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import io.reitmaier.transcripttool.core.data.dispatchers.CoroutineDispatchers
import io.reitmaier.transcripttool.core.data.domain.ApiResult
import io.reitmaier.transcripttool.core.data.domain.CompleteTaskRequest
import io.reitmaier.transcripttool.core.data.domain.Difficulty
import io.reitmaier.transcripttool.core.data.domain.DomainMessage
import io.reitmaier.transcripttool.core.data.domain.DomainResult
import io.reitmaier.transcripttool.core.data.domain.FullTranscriptTask
import io.reitmaier.transcripttool.core.data.domain.IOError
import io.reitmaier.transcripttool.core.data.domain.IncomingContent
import io.reitmaier.transcripttool.core.data.domain.LoadingError
import io.reitmaier.transcripttool.core.data.domain.MobileNumber
import io.reitmaier.transcripttool.core.data.domain.MobileOperator
import io.reitmaier.transcripttool.core.data.domain.Name
import io.reitmaier.transcripttool.core.data.domain.PREFS_PLAYBACK_SPEED
import io.reitmaier.transcripttool.core.data.domain.Password
import io.reitmaier.transcripttool.core.data.domain.PlayBackSpeed
import io.reitmaier.transcripttool.core.data.domain.ProvisionalTask
import io.reitmaier.transcripttool.core.data.domain.RegionId
import io.reitmaier.transcripttool.core.data.domain.RegistrationRequest
import io.reitmaier.transcripttool.core.data.domain.RejectReason
import io.reitmaier.transcripttool.core.data.domain.SavedTranscript
import io.reitmaier.transcripttool.core.data.domain.TaskId
import io.reitmaier.transcripttool.core.data.domain.TaskProvenance
import io.reitmaier.transcripttool.core.data.domain.TaskState
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.core.data.domain.UserInfo
import io.reitmaier.transcripttool.core.data.mappers.ToPartialTranscript
import io.reitmaier.transcripttool.core.data.mappers.ToRegion
import io.reitmaier.transcripttool.core.data.mappers.ToTranscriptTask
import io.reitmaier.transcripttool.core.data.mappers.toTranscriptTask
import io.reitmaier.transcripttool.core.data.util.getEnum
import io.reitmaier.transcripttool.core.data.util.putEnum
import io.reitmaier.transcripttool.data.File_Info
import io.reitmaier.transcripttool.data.Region_Entity
import io.reitmaier.transcripttool.data.Task_Entity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import logcat.logcat
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.round

// TODO make into interface
class TranscriptRepo @Inject constructor(
  private val database: TranscribeDatabase,
  private val service: TranscribeService,
  private val context: Context,
  private val prefs: SharedPreferences,
  private val dispatchers: CoroutineDispatchers,
) {
  private val mimeTypeMap = MimeTypeMap.getSingleton()
  fun flowAllTasks(): Flow<List<TranscriptTask>> =
    database.allTasks().map { list ->
      list.map { it.toTranscriptTask() }
    }

  fun nextTask(id: TaskId) = database.nextTask(id).map { it.id }.get()

  fun getPlayBackSpeed(): PlayBackSpeed =
    prefs.getEnum(PREFS_PLAYBACK_SPEED, PlayBackSpeed.NORMAL)

  fun setPlayBackSpeed(playBackSpeed: PlayBackSpeed) {
    prefs.edit()
      .putEnum(PREFS_PLAYBACK_SPEED, playBackSpeed)
      .apply()
  }

  fun getUserInfo(): DomainResult<UserInfo> = service.getUserInfo()

  suspend fun registerUser(
    name: Name,
    mobile: MobileNumber,
    operator: MobileOperator,
    password: Password = Password(UUID.randomUUID().toString()),
  ): ApiResult<UserInfo> =
    service.registerAccount(
      RegistrationRequest(mobile, operator, name, password),
    ).map {
      service.saveUserInfo(it.mobile, it.password)
      it
    }

  suspend fun logErrorS(message: String) {
    service.logError(message).fold(
      success = {
        logcat { "Logged error: $message" }
      },
      failure = {
        logcat { " Failed to log error $it" }
      },
    )
  }

  private fun getDisplayName(uri: Uri): DomainResult<String> {
    return runCatching {
      val documentFile = DocumentFile.fromSingleUri(context, uri)
      documentFile!!.name!!.substringBeforeLast(".") // guarded by runCatching
    }.mapError { IOError }
  }

  suspend fun createProvisionalTask(incomingContent: IncomingContent): DomainResult<ProvisionalTask> {
    val mimetypeExt = mimeTypeMap.getExtensionFromMimeType(incomingContent.mimeType)
      ?: "audio" // Default to generic audio extension
    val displayName = getDisplayName(incomingContent.uri).get()
    val fileInfo: File_Info = database.insertFileInfo(
      extension = mimetypeExt,
      orig_uri = incomingContent.uri.toString(),
      orig_display_name = displayName,
    )

    return createFileFromInfo(fileInfo)
      .andThen { file -> saveUriToFile(incomingContent.uri, file) }
      .map {
        ProvisionalTask(
          fileId = fileInfo.id,
          audio_path = it.path,
          displayName = it.name,
        )
      }
  }

  suspend fun createTranscriptTask(provisionalTask: ProvisionalTask, durationMs: Long): DomainResult<TranscriptTask> =
    service.submitTask(provisionalTask, durationMs)
      .andThen {
        database.insertLocalTask(
          it.remoteId,
          it.url,
          provisionalTask.fileId,
          provisionalTask.audio_path,
          provisionalTask.displayName,
          durationMs,
        )
      }
      .map { ToTranscriptTask.fromTask_Entity(it) }

  @Suppress("BlockingMethodInNonBlockingContext")
  private suspend fun saveUriToFile(uri: Uri, file: File): DomainResult<File> {
    return withContext(dispatchers.io) {
      runCatching {
        context.contentResolver.openInputStream(uri).use { input ->
          file.outputStream().use { output ->
            input!!.copyTo(output) // runCatching is guarding
          }
        }
        file
      }.mapError {
//        logError("Could not save $uri to ${file.path}")
        IOError
      }
    }
  }

  private fun createFileFromInfo(fileInfo: File_Info): DomainResult<File> {
    val filename = database.getFileName(fileInfo.id)
    val file = context.filesDir.resolve(filename)
    return if (!file.createNewFile()) {
      Err(IOError)
    } else {
      Ok(file)
    }
  }

  private suspend fun hydrateTask(taskEntity: Task_Entity): DomainResult<Task_Entity> =
    withContext(dispatchers.io) {
      // check if file already exists
      if (taskEntity.local_entity != null) {
        return@withContext Ok(taskEntity)
      }

      // Otherwise download file and update DB
      val extension = taskEntity.display_name.substringAfterLast(".")

      val fileInfo = database.insertFileInfo(
        extension,
        taskEntity.remote_url,
        taskEntity.display_name.substringBeforeLast("."),
      )
      val filename = database.getFileName(fileInfo.id)
      val file = context.filesDir.resolve(filename)
      if (!file.createNewFile()) {
        return@withContext Err(IOError).also { database.deleteFileInfo(fileInfo.id) }
      }
      return@withContext service.downloadToFile(taskEntity.remote_url, file)
        .andThen {
          database.addLocalAudioFile(taskEntity.id, file, fileInfo.id)
        }
        .onFailure { database.deleteFileInfo(id = fileInfo.id) }
    }

  suspend fun getFullTranscriptTask(id: TaskId): DomainResult<FullTranscriptTask> =
    withContext(dispatchers.io) {
      val task = database.getTask(id)
        .andThen { hydrateTask(it) }
        .get() ?: return@withContext Err(LoadingError)

      val regions = database.getActiveRegions(id)
      val partialTranscripts = database.getPartialTranscripts(id)
      if (task.local_entity == null) {
        return@withContext Err(LoadingError)
      }

      val localEntity = database.getLocalEntity(task.local_entity)
//      val localFilePath = localEntity.path

      val regionNumTarget = ceil(task.length.toDouble() / task.region_length).toInt()
      val regionLengthTarget = round(task.length.toDouble() / regionNumTarget.toDouble()).toInt()

      // Regenerate Regions
      return@withContext if (regions.size != regionNumTarget) {
        database.markRegionsInActive(id)
        val regionList = mutableListOf<Region_Entity>()
        for (n in 0 until regionNumTarget) {
          val region =
            database.insertRegion(
              id,
              ((n * regionLengthTarget) - 750).coerceAtLeast(0),
              (n + 1) * regionLengthTarget - 1,
            )
          regionList.add(region)
        }
        val transcriptTask = ToTranscriptTask.fromTask_Entity(task)
        Ok(
          FullTranscriptTask(
            task = transcriptTask,
            regions = regionList.map { ToRegion.fromRegion_Entity(it) },
            partialTranscripts = partialTranscripts.map {
              ToPartialTranscript.fromPartial_Transcript_Entity(
                it,
              )
            },
            localFilePath = localEntity.path,
//            submittedTranscript = task.submitted_transcript,
//            savedTranscript = task.latest_transcript,
//            inputtedTranscript = InputtedTranscript(task.latest_transcript.value),
          ),
        )
      }

      // Use regions from DB
      else {
        Ok(
          FullTranscriptTask(
            task = ToTranscriptTask.fromTask_Entity(task),
            regions = regions.map { ToRegion.fromRegion_Entity(it) },
            partialTranscripts = partialTranscripts.map {
              ToPartialTranscript.fromPartial_Transcript_Entity(
                it,
              )
            },
            localFilePath = localEntity.path,
//            submittedTranscript = task.submitted_transcript,
//            savedTranscript = task.latest_transcript,
//            inputtedTranscript = InputtedTranscript(task.latest_transcript.value),
          ),
        )
      }
    }

  suspend fun completeTask(taskId: TaskId, difficulty: Difficulty): DomainResult<FullTranscriptTask> =
    database.completeTask(taskId, difficulty)
      .andThen { getFullTranscriptTask(taskId) }

  suspend fun rejectTask(taskId: TaskId, rejectReason: RejectReason): DomainResult<FullTranscriptTask> =
    database.rejectTask(taskId, rejectReason)
      .andThen { getFullTranscriptTask(taskId) }

  private suspend fun submitTranscripts(taskId: TaskId): DomainResult<Unit> =
    getFullTranscriptTask(taskId)
      .andThen {
        service.uploadTranscripts(it.task.remoteId, it.toNewTranscriptsRequest())
      }
      .andThen {
        database.setSubmittedTranscript(taskId, it)
      }

  fun insertTranscript(transcript: String, regionId: RegionId): SavedTranscript? {
    // TODO refactor with result monad
    val regionEntity = database.getRegion(regionId)
    if (regionEntity != null) {
      val latest = database.getLatestPartialTranscript(regionEntity.task_id)
      return if (latest != null && latest.region_id == regionEntity.id) {
        // Update Transcript
        database.updatePartialTranscript(latest.id, transcript)
      } else {
        database.insertPartialTranscript(regionEntity, transcript).content
      }
    }
    return null
  }

  public suspend fun uploadChanges() {
    val allTasks = database.allTasks().first().map {
      it.toTranscriptTask()
    }
    val changedTasks = allTasks.filter { !it.isSynced }
    val (rejected, transcribed) = changedTasks.partition { it.state == TaskState.REJECTED }

    for (r in rejected) {
      service.rejectTask(r.remoteId, r.rejectReason)
        .andThen { database.markTaskUploaded(r.id) }
    }
    for (t in transcribed) {
      if (t.savedTranscript.value.isNotEmpty()) {
        submitTranscripts(t.id)
          .onFailure { logcat { "Error submitting transcript $it" } }
      }
    }
    transcribed.filter { it.completedAt != null }
      .onEach {
        service.completeTask(it.remoteId, CompleteTaskRequest(it.difficulty, it.completedAt!!))
      }
  }

  suspend fun refreshTasks(): DomainResult<String> {
    return binding<String, DomainMessage> {
      val tasks = service.refreshTasks().bind()
      val remoteTasks = tasks.filter { it.provenance == TaskProvenance.REMOTE }
      // TODO Check that everything goes right here
      remoteTasks.map { remoteTask ->
        val localTask = database.getTaskByRemoteId(remoteTask.id).get()
        if (localTask == null) {
          database.insertRemoteTask(
            remoteTask.id,
            "${TranscribeService.BASE_URL}/tasks/${remoteTask.id.value}/file",
            remoteTask.displayName,
            remoteTask.lengthMs,
          ).bind()
          1
        } else {
          1
        }
      }
      uploadChanges()
      // TODO Propagate error messages
      "OK"
    }
  }
}
