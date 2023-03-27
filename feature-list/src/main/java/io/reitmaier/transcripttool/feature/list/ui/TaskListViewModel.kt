package io.reitmaier.transcripttool.feature.list.ui

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reitmaier.transcripttool.core.data.TranscriptRepo
import io.reitmaier.transcripttool.core.data.domain.DomainMessage
import io.reitmaier.transcripttool.core.data.domain.TaskState
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.core.data.parcelizers.DurationParceler
import io.reitmaier.transcripttool.core.data.util.IntentDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

sealed class TaskListSideEffect {
//  data class NavigateToTranscriptTask(val id: TaskId) : TaskListSideEffect()
  data class RefreshFailed(val reason: DomainMessage) : TaskListSideEffect()
  object RefreshSucceeded : TaskListSideEffect()
}

internal sealed class ViewIntent {
  object Refresh : ViewIntent()
  object UploadChanges : ViewIntent()
}

@Parcelize
data class TasksWithLength(
  val taskList: List<TranscriptTask>,
  val length: @WriteWith<DurationParceler> Duration,
) : Parcelable

sealed class TaskListState(open val isRefreshing: Boolean) : Parcelable {
  // Empty State?

  @Parcelize
  data class Loaded(
    // TODO group by status
    val groupedTasks: Map<TaskState, TasksWithLength> = emptyMap(),
    override val isRefreshing: Boolean = false,
  ) : TaskListState(isRefreshing), Parcelable

  @Parcelize
  data class Error(
    val error: DomainMessage,
    override val isRefreshing: Boolean = false,
  ) : TaskListState(isRefreshing), Parcelable
}

@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
class TaskListViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val repo: TranscriptRepo,
) : ViewModel(), ContainerHost<TaskListState, TaskListSideEffect> {
  private val _intentFlow = MutableSharedFlow<ViewIntent>(extraBufferCapacity = 64)
  internal val processIntent: IntentDispatcher<ViewIntent> = { _intentFlow.tryEmit(it) }
  override val container: Container<TaskListState, TaskListSideEffect> =
    container(TaskListState.Loaded(), savedStateHandle) {
      refreshTasks()
    }

  init {
    // Handle ViewIntents
    _intentFlow
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
      .onEach { viewIntent ->
        when (viewIntent) {
          ViewIntent.Refresh -> refreshTasks()
          ViewIntent.UploadChanges -> uploadChanges()
        }
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0L,
      )

    // Collect Task List
    repo.flowAllTasks()
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
      .map { updatedList ->
        updatedList.groupBy {
          it.state
        }.toSortedMap { p0, p1 -> p0.order.compareTo(p1.order) }
          .mapValues {
            TasksWithLength(it.value, it.value.sumOf { task -> task.length }.milliseconds)
          }
      }
      .onEach { groupedList ->
        intent {
          state.let { currentState ->
            when (currentState) {
              is TaskListState.Error -> reduce { TaskListState.Loaded(groupedList) }
              is TaskListState.Loaded -> reduce { currentState.copy(groupedTasks = groupedList) }
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

//  private fun navigateToTask(id: TaskId) = intent {
//    postSideEffect(TaskListSideEffect.NavigateToTranscriptTask(id))
//  }

  private fun uploadChanges() = intent {
    state.let { currentState ->
      when (currentState) {
        is TaskListState.Error -> {
          if (currentState.isRefreshing) return@intent
        }
        is TaskListState.Loaded -> {
          if (currentState.isRefreshing) return@intent
        }
      }
    }
    repo.uploadChanges()
  }
  private fun refreshTasks() = intent {
    state.let { currentState ->
      when (currentState) {
        is TaskListState.Error -> {
          if (currentState.isRefreshing) return@intent
          reduce { currentState.copy(isRefreshing = true) }
        }
        is TaskListState.Loaded -> {
          if (currentState.isRefreshing) return@intent
          reduce { currentState.copy(isRefreshing = true) }
        }
      }
    }
    repo.refreshTasks().fold(
      success = {
        postSideEffect(TaskListSideEffect.RefreshSucceeded)
      },
      failure = {
        postSideEffect(TaskListSideEffect.RefreshFailed(it))
      },
    )
    state.let { currentState ->
      when (currentState) {
        is TaskListState.Error -> reduce { currentState.copy(isRefreshing = false) }
        is TaskListState.Loaded -> reduce { currentState.copy(isRefreshing = false) }
      }
    }
  }
}
