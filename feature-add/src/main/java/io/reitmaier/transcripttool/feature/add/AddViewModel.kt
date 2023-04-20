package io.reitmaier.transcripttool.feature.add

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reitmaier.transcripttool.core.data.TranscriptRepo
import io.reitmaier.transcripttool.core.data.dispatchers.CoroutineDispatchers
import io.reitmaier.transcripttool.core.data.domain.DomainMessage
import io.reitmaier.transcripttool.core.data.domain.IOError
import io.reitmaier.transcripttool.core.data.domain.LoadingError
import io.reitmaier.transcripttool.core.data.domain.NetworkError
import io.reitmaier.transcripttool.core.data.domain.PlaybackState
import io.reitmaier.transcripttool.core.data.domain.ProvisionalTask
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.core.data.util.IntentDispatcher
import io.reitmaier.transcripttool.core.ui.player.ExoAudioPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize
import logcat.logcat
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

internal sealed class ViewIntent {
  data class ProcessProvisionalTask(val incoming: ProvisionalTask?) : ViewIntent()
  data class UploadTask(val loadedState: AddTaskState.Loaded) : ViewIntent()
  data class PlayPause(val loadedState: AddTaskState.Loaded) : ViewIntent()
  data class SeekForward(val loadedState: AddTaskState.Loaded) : ViewIntent()
  data class SeekBack(val loadedState: AddTaskState.Loaded) : ViewIntent()
}

sealed class AddTaskSideEffect {
  object LoadingFailed : AddTaskSideEffect()

  //  object LoadingSucceeded : AddTaskSideEffect()
  data class UploadSucceeded(val transcriptTask: TranscriptTask) : AddTaskSideEffect()
  object UploadFailed : AddTaskSideEffect()
}
sealed class AddTaskState : Parcelable {

  @Parcelize
  object Initial : AddTaskState(), Parcelable

  @Parcelize
  data class Processing(val provisionalTask: ProvisionalTask) : AddTaskState(), Parcelable {
    companion object {
      val Preview = Processing(ProvisionalTask.Preview)
    }
  }

  @Parcelize
  data class Loaded(
    val provisionalTask: ProvisionalTask,
    val durationMs: Long,
    val playbackState: PlaybackState,
    val progress: Float = 0.0f,
  ) : AddTaskState(), Parcelable {
    companion object {
      val Preview = Loaded(ProvisionalTask.Preview, 60_000L, PlaybackState.Playing, 0.5f)
    }
  }

  @Parcelize
  data class Uploading(
    val provisionalTask: ProvisionalTask,
    val durationMs: Long,
    val progress: Float,
  ) : AddTaskState(), Parcelable {
    companion object {
      val Preview = Uploading(ProvisionalTask.Preview, 60_000L, 0.66f)
    }
  }

  @Parcelize
  data class UploadingError(
    val provisionalTask: ProvisionalTask,
    val durationMs: Long,
    val error: DomainMessage,
  ) : AddTaskState(), Parcelable {
    companion object {
      val Preview = UploadingError(ProvisionalTask.Preview, 60_000L, NetworkError(Throwable("Demo")))
    }
  }

  @Parcelize
  data class ProcessingError(val error: DomainMessage) : AddTaskState(), Parcelable {
    companion object {
      val Preview = ProcessingError(IOError)
    }
  }
}

@ExperimentalTime
@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
class AddViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val repo: TranscriptRepo,
  private val exoAudioPlayer: ExoAudioPlayer,
  private val dispatchers: CoroutineDispatchers,
) : ViewModel(), ContainerHost<AddTaskState, AddTaskSideEffect> {
  private val _intentFlow = MutableSharedFlow<ViewIntent>(extraBufferCapacity = 64)
  internal val processIntent: IntentDispatcher<ViewIntent> = { _intentFlow.tryEmit(it) }

  override val container: Container<AddTaskState, AddTaskSideEffect> =
    container(AddTaskState.Initial, savedStateHandle) { }

  private fun processProvisionalTask(task: ProvisionalTask?): Unit = intent {
    // Do nothing if we're not in the initial state
    if (state !is AddTaskState.Initial) {
      logcat { "Already processing task with state: $state" }
      return@intent
    }

    logcat { "Processing $task" }
    if (task != null) {
      reduce { AddTaskState.Processing(task) }
      // Access exoplayer on main thread
      withContext(dispatchers.main) {
        withTimeoutOrNull(2000.milliseconds) {
          exoAudioPlayer.prepare(task.audioPath)
          logcat { "Computing Duration" }
          // Wait for file to be loaded
          exoAudioPlayer.playbackState.filter { it is PlaybackState.Paused }.first()
          val duration = exoAudioPlayer.duration()
          logcat { "Computed Duration: $duration" }
//          exoAudioPlayer.resume()
          Ok(duration)
        } ?: Err(IOError)
      }
        .fold(
          success = { duration ->
//          postSideEffect(AddTaskSideEffect.LoadingSucceeded)
            viewModelScope.launch {
              delay(1.seconds)
              exoAudioPlayer.resume()
            }
            reduce {
              AddTaskState.Loaded(task, duration, exoAudioPlayer.playbackState.value)
            }
          },
          failure = {
            postSideEffect(AddTaskSideEffect.LoadingFailed)
            reduce { AddTaskState.ProcessingError(it) }
          },
        )
    } else {
      postSideEffect(AddTaskSideEffect.LoadingFailed)
      reduce { AddTaskState.ProcessingError(LoadingError) }
    }
  }

  private fun addTask(loadedState: AddTaskState.Loaded): Unit = intent {
    reduce {
      AddTaskState.Uploading(
        loadedState.provisionalTask,
        loadedState.durationMs,
        progress = 0f,
      )
    }
    // TODO Track upload progress
    repo.createTranscriptTask(loadedState.provisionalTask, loadedState.durationMs).fold(
      success = { task -> postSideEffect(AddTaskSideEffect.UploadSucceeded(task)) },
      failure = {
        postSideEffect(AddTaskSideEffect.UploadFailed)
        reduce {
          AddTaskState.UploadingError(
            loadedState.provisionalTask,
            loadedState.durationMs,
            it,
          )
        }
      },
    )
  }

  fun onStop() {
    logcat { "OnStop" }
    exoAudioPlayer.pause()
    exoAudioPlayer.clear()
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
          is ViewIntent.UploadTask -> {
            exoAudioPlayer.pause()
            addTask(viewIntent.loadedState)
          }
          is ViewIntent.ProcessProvisionalTask -> processProvisionalTask(viewIntent.incoming)
          is ViewIntent.PlayPause -> playPause(viewIntent.loadedState)
          is ViewIntent.SeekBack -> exoAudioPlayer.seekBack()
          is ViewIntent.SeekForward -> exoAudioPlayer.seekForward()
        }
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
              is AddTaskState.Initial -> Unit // Do Nothing
              is AddTaskState.Loaded -> reduce { it.copy(playbackState = playbackState) }
              is AddTaskState.Processing -> Unit
              is AddTaskState.ProcessingError -> Unit
              is AddTaskState.Uploading -> Unit
              is AddTaskState.UploadingError -> Unit
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
              is AddTaskState.Initial,
              is AddTaskState.Processing,
              is AddTaskState.ProcessingError,
              is AddTaskState.Uploading,
              is AddTaskState.UploadingError,
              -> Unit
              is AddTaskState.Loaded -> {
                val progress = (playbackProgress.current.toFloat() / playbackProgress.duration).coerceIn(0.0f, 1.0f)
                reduce { it.copy(progress = progress) }
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

  private fun playPause(loadedState: AddTaskState.Loaded) {
    exoAudioPlayer.playPause()
  }
}
