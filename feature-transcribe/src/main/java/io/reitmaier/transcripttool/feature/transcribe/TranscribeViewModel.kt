package io.reitmaier.transcripttool.feature.transcribe

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reitmaier.transcripttool.core.data.TranscriptRepo
import io.reitmaier.transcripttool.core.data.domain.Difficulty
import io.reitmaier.transcripttool.core.data.domain.DomainMessage
import io.reitmaier.transcripttool.core.data.domain.FullTranscriptTask
import io.reitmaier.transcripttool.core.data.domain.NAV_ARG_TASK_ID
import io.reitmaier.transcripttool.core.data.domain.PlayBackSpeed
import io.reitmaier.transcripttool.core.data.domain.PlaybackState
import io.reitmaier.transcripttool.core.data.domain.RejectReason
import io.reitmaier.transcripttool.core.data.domain.SavedTranscript
import io.reitmaier.transcripttool.core.data.domain.SubmittedTranscript
import io.reitmaier.transcripttool.core.data.domain.TaskId
import io.reitmaier.transcripttool.core.data.parcelizers.NullableSubmittedTranscriptParceler
import io.reitmaier.transcripttool.core.data.parcelizers.SavedTranscriptParceler
import io.reitmaier.transcripttool.core.data.parcelizers.TaskIdParceler
import io.reitmaier.transcripttool.core.data.util.IntentDispatcher
import io.reitmaier.transcripttool.core.data.util.next
import io.reitmaier.transcripttool.core.ui.player.ExoAudioPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import logcat.logcat
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.syntax.simple.blockingIntent
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal sealed class ViewIntent {
  object PlayPause : ViewIntent()
  object PlayNext : ViewIntent()
  object PlayPrevious : ViewIntent()

//  object Upload : ViewIntent()
  object RetryReload : ViewIntent()
  object TogglePlayBackSpeed : ViewIntent()
  object ShowConfirm : ViewIntent()
  object DismissConfirm : ViewIntent()
  data class Confirm(
    val difficulty: Difficulty,
  ) : ViewIntent()
  data class RateDifficulty(val rating: Difficulty) : ViewIntent()
  data class RejectTask(val reason: RejectReason) : ViewIntent()
  data class Update(val transcript: TextFieldValue) : ViewIntent()
}

sealed class TranscriptTaskSideEffect {
  object CompletionFailed : TranscriptTaskSideEffect()
  object CompletionSucceeded : TranscriptTaskSideEffect()
}
sealed class TranscriptTaskState : Parcelable {

  @Parcelize
  object Initial : TranscriptTaskState(), Parcelable

  @Parcelize
  data class Loading(val id: TaskId) : TranscriptTaskState(), Parcelable

  @Parcelize
  data class Error(val error: DomainMessage) : TranscriptTaskState(), Parcelable

  @Parcelize
  data class Loaded(
    val id: @WriteWith<TaskIdParceler> TaskId,
    val transcript: FullTranscriptTask,
    val isPlaying: Boolean = false,
    val isConfirming: ConfirmationInfo? = null,
    val progress: Float = 0.0f,
    var playBackSpeed: PlayBackSpeed = PlayBackSpeed.NORMAL,
    val regionIndex: Int = 0,
    val isUploading: Boolean = false,
    val rejectReason: RejectReason?,
//    val inputtedTranscript: @WriteWith<InputtedTranscriptParceler> InputtedTranscript,
    val inputtedTranscript: @WriteWith<TextFieldValueParceler> TextFieldValue,
    val savedTranscript: @WriteWith<SavedTranscriptParceler> SavedTranscript,
    val submittedTranscript: @WriteWith<NullableSubmittedTranscriptParceler> SubmittedTranscript?,
  ) : TranscriptTaskState(), Parcelable {
    val isSaving: Boolean
      get() = inputtedTranscript.text != savedTranscript.value

    val canUpload: Boolean
      get() = !isUploading && !isSaving && savedTranscript.value != (
        submittedTranscript?.value
          ?: ""
        )

    val canShare: Boolean
      get() = !isUploading && !isSaving && savedTranscript.value.isNotBlank()
  }
}

@Parcelize
data class ConfirmationInfo(
//  val confidence: Confidence? = null,
  val difficulty: Difficulty? = null,
) : Parcelable {
  val isComplete: Boolean
    get() =
      difficulty != null
  // && confidence != null
}

@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
@OptIn(OrbitExperimental::class)
class TranscribeViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val repo: TranscriptRepo,
  private val exoAudioPlayer: ExoAudioPlayer,

) : ViewModel(), ContainerHost<TranscriptTaskState, TranscriptTaskSideEffect> {

  private val _intentFlow = MutableSharedFlow<ViewIntent>(extraBufferCapacity = 64)
  internal val processIntent: IntentDispatcher<ViewIntent> = { _intentFlow.tryEmit(it) }

  // TODO avoid this replication
  private val taskId = TaskId(savedStateHandle.get<Int>(NAV_ARG_TASK_ID)!!)

  override val container =
    container<TranscriptTaskState, TranscriptTaskSideEffect>(
      initialState = TranscriptTaskState.Initial,
      savedStateHandle = savedStateHandle,
    ) { loadTranscriptTask(taskId) }

  private fun loadTranscriptTask(id: TaskId): Unit = intent {
    // Enter Loading State
    reduce { TranscriptTaskState.Loading(id = id) }

    when (val taskResult = repo.getFullTranscriptTask(id)) {
      is Ok -> {
        withContext(Dispatchers.Main) {
          exoAudioPlayer.prepare(taskResult.value.localFilePath, taskResult.value.regions, repo.getPlayBackSpeed())
          // Wait for file to be loaded
          exoAudioPlayer.playbackState.filter { it is PlaybackState.Paused }.first()
          reduce {
            TranscriptTaskState.Loaded(
              id = taskResult.value.task.id,
              transcript = taskResult.value,
              inputtedTranscript = TextFieldValue(
                text = taskResult.value.task.inputtedTranscript.value,
                // Place cursor at end
                selection = TextRange(taskResult.value.task.inputtedTranscript.value.length),
              ),
              savedTranscript = taskResult.value.task.savedTranscript,
              submittedTranscript = taskResult.value.task.submittedTranscript,
              playBackSpeed = exoAudioPlayer.speed,
              rejectReason = taskResult.value.task.rejectReason,
            )
          }
          // wait a second before playing to allow transitions to complete
          delay(1.seconds)
          playPause()
        }
      }
      is Err -> reduce { TranscriptTaskState.Error(taskResult.error) }
    }
  }

  private suspend fun togglePlayBackSpeed() {
    intent {
      state.let { s ->
        when (s) {
          is TranscriptTaskState.Error,
          is TranscriptTaskState.Initial,
          is TranscriptTaskState.Loading,
          -> Unit // Do nothing
          is TranscriptTaskState.Loaded -> {
            val nextSpeed = s.playBackSpeed.next()
            withContext(Dispatchers.Main) {
              exoAudioPlayer.speed = nextSpeed
            }
            reduce { s.copy(playBackSpeed = nextSpeed) }
            repo.setPlayBackSpeed(nextSpeed)
          }
        }
      }
    }
  }

  private fun playNextRegion() {
    exoAudioPlayer.next()
    exoAudioPlayer.resume()
  }

  private fun playPreviousRegion() {
    exoAudioPlayer.previous()
    exoAudioPlayer.resume()
  }

  private fun playPause() {
    exoAudioPlayer.playbackState.value.let {
      when (it) {
        is PlaybackState.Loading -> exoAudioPlayer.resume().also { logcat { "Playing (Loading)" } } // TODO
        is PlaybackState.Paused -> exoAudioPlayer.resume().also { logcat { "Playing" } }
        is PlaybackState.Playing -> exoAudioPlayer.pause().also { logcat { "Pausing" } }
      }
    }
  }

  private fun rejectTask(reason: RejectReason) {
    intent {
      state.let { s ->
        when (s) {
          is TranscriptTaskState.Error,
          is TranscriptTaskState.Loading,
          is TranscriptTaskState.Initial,
          -> reduce { s } // Do Nothing
          is TranscriptTaskState.Loaded -> {
            repo.rejectTask(s.id, reason)
              .fold(
                success = {
                  postSideEffect(TranscriptTaskSideEffect.CompletionSucceeded)
                  reduce { s.copy(rejectReason = it.task.rejectReason) }
                },
                failure = {
                  reduce { s.copy(isUploading = false) }
                  postSideEffect(TranscriptTaskSideEffect.CompletionFailed)
                },
              )
          }
        }
      }
    }
  }
  private fun updateTranscript(transcript: TextFieldValue) {
    blockingIntent {
      state.let {
        when (it) {
          is TranscriptTaskState.Error,
          is TranscriptTaskState.Loading,
          is TranscriptTaskState.Initial,
          -> reduce { it } // Do Nothing
          is TranscriptTaskState.Loaded -> {
            reduce {
              it.copy(
                inputtedTranscript = transcript,
              )
            }
          }
        }
      }
    }
  }

  private fun persistTranscript(transcript: TextFieldValue) {
    logcat { "Persisting transcript: ${transcript.text}" }
    intent {
      state.let {
        when (it) {
          is TranscriptTaskState.Error,
          is TranscriptTaskState.Loading,
          is TranscriptTaskState.Initial,
          -> reduce { it } // Do Nothing
          is TranscriptTaskState.Loaded -> {
            val regionId = it.transcript.regions[it.regionIndex].id
            val savedTranscript = repo.insertTranscript(transcript.text, regionId) ?: return@intent
            reduce { it.copy(savedTranscript = savedTranscript) }
          }
        }
      }
    }
  }

  private fun updatePlaybackState(
    playbackState: PlaybackState,
    loadedTranscript: TranscriptTaskState.Loaded,
  ): TranscriptTaskState {
    return when (playbackState) {
      is PlaybackState.Loading -> loadedTranscript.copy(isPlaying = false) // TODO Guard?
      is PlaybackState.Paused -> loadedTranscript.copy(isPlaying = false)
      is PlaybackState.Playing -> {
        logcat { "Playing ${loadedTranscript.regionIndex}" }
        loadedTranscript.copy(isPlaying = true)
      }
    }
  }

  private fun completeTranscript(difficulty: Difficulty) {
    hideConfirmDialog()
    intent {
      state.let { s ->
        when (s) {
          is TranscriptTaskState.Error,
          is TranscriptTaskState.Loading,
          is TranscriptTaskState.Initial,
          -> Unit // Do Nothing
          is TranscriptTaskState.Loaded -> {
            // Begin Uploading
            reduce {
              s.copy(isUploading = true)
            }
            repo.completeTask(s.id, difficulty)
              .fold(
                success = {
                  reduce { s.copy(isUploading = false, submittedTranscript = it.task.submittedTranscript) }
                  postSideEffect(TranscriptTaskSideEffect.CompletionSucceeded)
                },
                failure = {
                  reduce { s.copy(isUploading = false) }
                  postSideEffect(TranscriptTaskSideEffect.CompletionFailed)
                },
              )
          }
        }
      }
    }
  }

  fun onStop() {
    logcat { "OnStop" }
    exoAudioPlayer.pause()
  }

  init {
    // Handle ViewIntents
    _intentFlow
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
      // ignore Updates that don't change anything
//      .distinctUntilChanged { old, new ->
//        old is ViewIntent.Update &&
//          new is ViewIntent.Update &&
//          old.transcript == new.transcript
//      }
      // Update inputtedTranscript straightaway
      .onEach { viewIntent ->
        when (viewIntent) {
          ViewIntent.PlayPause -> playPause()
          is ViewIntent.Update -> updateTranscript(viewIntent.transcript)
//          is ViewIntent.Update -> Unit // Do only handle when persisting below
//          ViewIntent.Upload -> upload()
          ViewIntent.PlayNext -> playNextRegion()
          ViewIntent.PlayPrevious -> playPreviousRegion()
          ViewIntent.RetryReload -> loadTranscriptTask(taskId)
          ViewIntent.TogglePlayBackSpeed -> togglePlayBackSpeed()
          ViewIntent.DismissConfirm -> hideConfirmDialog()
          ViewIntent.ShowConfirm -> showConfirmDialog()
          is ViewIntent.Confirm -> completeTranscript(viewIntent.difficulty)
          is ViewIntent.RejectTask -> rejectTask(viewIntent.reason)
          is ViewIntent.RateDifficulty -> rateDifficulty(viewIntent.rating)
        }
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0L,
      )

    // Also debounce and persist transcript updates to repo
    _intentFlow
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
      .filterIsInstance<ViewIntent.Update>()
      .distinctUntilChanged()
      .debounce(500.milliseconds)
      .onEach {
        persistTranscript(it.transcript)
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0L,
      )

    // Collect Playback State
    exoAudioPlayer.playbackState
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
      .onEach { playbackState ->
        intent {
          state.let {
            when (it) {
              is TranscriptTaskState.Error,
              is TranscriptTaskState.Loading,
              is TranscriptTaskState.Initial,
              -> reduce { it } // Do Nothing
              is TranscriptTaskState.Loaded -> reduce { updatePlaybackState(playbackState, it) }
            }
          }
        }
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0L,
      )

    // Collect Playback Progress
    exoAudioPlayer.playbackProgress
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
      .onEach { playbackProgress ->
        intent {
          state.let {
            when (it) {
              is TranscriptTaskState.Error,
              is TranscriptTaskState.Loading,
              TranscriptTaskState.Initial,
              -> reduce { it } // Do Nothing
              is TranscriptTaskState.Loaded -> {
                // Calculate progress
//                val windowStart = progress.regionPosition*it.transcript.task.regionLength
//                val windowStart = 0
                val currentRegion = it.transcript.regions[playbackProgress.regionPosition]
                val regionLength = currentRegion.end - currentRegion.start
                val progress = (playbackProgress.current.toFloat() / regionLength).coerceIn(0.0f, 1.0f)
                // TODO Persist playcount
                reduce { it.copy(progress = progress, regionIndex = playbackProgress.regionPosition) }
              }
            }
          }
        }
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0L,
      )
  }

  private fun rateDifficulty(rating: Difficulty) {
    intent {
      state.let { s ->
        when (s) {
          is TranscriptTaskState.Error,
          is TranscriptTaskState.Initial,
          is TranscriptTaskState.Loading,
          -> Unit // Do nothing
          is TranscriptTaskState.Loaded -> reduce {
            val confirmationInfo = s.isConfirming?.copy(difficulty = rating)
            s.copy(isConfirming = confirmationInfo)
          }
        }
      }
    }
  }

  private fun showConfirmDialog() {
    intent {
      state.let { s ->
        when (s) {
          is TranscriptTaskState.Error,
          is TranscriptTaskState.Initial,
          is TranscriptTaskState.Loading,
          -> Unit // Do nothing
          is TranscriptTaskState.Loaded -> reduce { s.copy(
            isConfirming = ConfirmationInfo(s.transcript.task.difficulty),
          ) }
        }
      }
    }
  }

  private fun hideConfirmDialog() {
    intent {
      state.let { s ->
        when (s) {
          is TranscriptTaskState.Error,
          is TranscriptTaskState.Initial,
          is TranscriptTaskState.Loading,
          -> Unit // Do nothing
          is TranscriptTaskState.Loaded -> reduce { s.copy(isConfirming = null) }
        }
      }
    }
  }
}

object TextFieldValueParceler : Parceler<TextFieldValue> {
  override fun create(parcel: Parcel): TextFieldValue {
    val str = parcel.readString()
    require(str != null)
    return TextFieldValue(str)
  }
  override fun TextFieldValue.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this.text)
  }
}
