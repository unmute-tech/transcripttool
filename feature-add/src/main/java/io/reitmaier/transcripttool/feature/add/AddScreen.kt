package io.reitmaier.transcripttool.feature.add

import android.annotation.SuppressLint
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.reitmaier.transcripttool.core.data.domain.PlaybackState
import io.reitmaier.transcripttool.core.data.domain.ProvisionalTask
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.core.data.util.IntentDispatcher
import io.reitmaier.transcripttool.core.ui.components.IconButton
import io.reitmaier.transcripttool.core.ui.components.IconTextButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@ExperimentalLayoutApi
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalTime
@FlowPreview
@Composable
fun AddScreen(
  modifier: Modifier = Modifier,
  provisionalTask: ProvisionalTask?,
  onTaskAdded: (TranscriptTask) -> Unit,
  navigateBack: () -> Unit,
) {
  val viewModel = hiltViewModel<AddViewModel>()
  val state = viewModel.container.stateFlow.collectAsState().value
  val processIntent = viewModel.processIntent
  val snackbarHostState = remember { SnackbarHostState() }
  DisposableEffect(key1 = viewModel) {
    onDispose { viewModel.onStop() }
  }
  LaunchedEffect(provisionalTask) {
    processIntent(ViewIntent.ProcessProvisionalTask(provisionalTask))
  }
  LaunchedEffect(viewModel, snackbarHostState) {
    launch {
      viewModel.container.sideEffectFlow.collect {
        when (it) {
          AddTaskSideEffect.LoadingFailed ->
            snackbarHostState.showSnackbar("Failed to load external audio content. Navigate back to try again.")
          AddTaskSideEffect.UploadFailed ->
            snackbarHostState.showSnackbar("Failed to upload content. Please try again.")
          is AddTaskSideEffect.UploadSucceeded -> {
            onTaskAdded(it.transcriptTask)
          }
        }
      }
    }
  }

  Scaffold(
    snackbarHost = {
      SnackbarHost(
        modifier = Modifier
          .navigationBarsPadding()
          .imePadding(),
        hostState = snackbarHostState,
      )
    },
    modifier = Modifier.imePadding(),
    topBar = {
      AddScreenAppBar(
        state = state,
        processIntent = processIntent,
        navigateBack = navigateBack,
      )
    },
    bottomBar = {},
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { scaffoldPadding ->
    Box(
      Modifier
        .fillMaxSize()
        .padding(scaffoldPadding)
        .consumeWindowInsets(scaffoldPadding)
        .systemBarsPadding(),
    ) {
      AddScreenContents(state = state, processIntent = processIntent)
    }
  }
}

@Composable
private fun InitialView(
  initial: AddTaskState.Initial,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  Text(
    // TODO Map domain message
    text = "Receiving Audio Content ...",
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    textAlign = TextAlign.Center,
  )
  CircularProgressIndicator()
}

@Composable
private fun LoadingView(
  loading: AddTaskState.Processing,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  Text(
    // TODO Map domain message
    text = "Processing Audio Content ... \n ${loading.provisionalTask.displayName}",
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    textAlign = TextAlign.Center,
  )
  CircularProgressIndicator()
}

@Composable
private fun LoadErrorView(
  loadError: AddTaskState.ProcessingError,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  Text(
    // TODO Map domain message
    text = "Error Loading Audio Content \n ${loadError.error}",
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    textAlign = TextAlign.Center,
  )
}

@Composable
private fun UploadingView(
  uploading: AddTaskState.Uploading,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  Text(
    text = "Uploading Audio Content: \n ${uploading.provisionalTask.displayName}",
    maxLines = 8,
    overflow = TextOverflow.Visible,
    textAlign = TextAlign.Center,
  )
  CircularProgressIndicator()
}

@Composable
private fun LoadedView(
  loaded: AddTaskState.Loaded,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  Column {
    Text(
      text = "Loaded Audio Content:",
      style = MaterialTheme.typography.headlineMedium,
      maxLines = 1,
      overflow = TextOverflow.Visible,
      textAlign = TextAlign.Start,
    )
    Text(
      text = loaded.provisionalTask.displayName,
      style = MaterialTheme.typography.headlineSmall,
      maxLines = 1,
      overflow = TextOverflow.Visible,
      textAlign = TextAlign.Start,
    )
  }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceAround,
  ) {
    IconButton(
      imageVector = Icons.Filled.FastRewind,
      contentDescription = "Replay previous 10 seconds",
      modifier = Modifier
        .size(50.dp),
    ) {
      processIntent(ViewIntent.SeekBack(loaded))
    }
    PlayPauseButton(isPlaying = loaded.playbackState == PlaybackState.Playing) {
      processIntent(ViewIntent.PlayPause(loaded))
    }
    IconButton(
      imageVector = Icons.Filled.FastForward,
      contentDescription = "Forward 10 seconds",
      modifier = Modifier
        .size(50.dp),
    ) {
      processIntent(ViewIntent.SeekForward(loaded))
    }
  }
  LinearProgressIndicator(
    modifier = Modifier.fillMaxWidth(),

    progress = loaded.progress,
  )
  Spacer(modifier = Modifier.padding(0.dp, 8.dp))
  Text(
    text = "Please check that you're your happy to share the audio content with the UnMute research team.",
    style = MaterialTheme.typography.bodyMedium,
    maxLines = 4,
    overflow = TextOverflow.Visible,
    textAlign = TextAlign.Start,
  )
  IconTextButton(
    imageVector = Icons.Outlined.Upload,
    contentDescription = "Upload",
    text = "Upload"
  ) {
    processIntent(ViewIntent.UploadTask(loaded))
  }
}

@Composable
private fun AddScreenContents(
  state: AddTaskState,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(4.dp, 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
//    verticalArrangement = Arrangement.Center,
  ) {
    when (state) {
      // Happy Path
      is AddTaskState.Initial -> InitialView(state, processIntent)
      is AddTaskState.Processing -> LoadingView(state, processIntent)
      is AddTaskState.Loaded -> LoadedView(state, processIntent)
      is AddTaskState.Uploading -> UploadingView(state, processIntent)

      // Error States
      is AddTaskState.ProcessingError -> LoadErrorView(state, processIntent)
      is AddTaskState.UploadingError ->
        Text(
          // TODO Map domain message
          text = "Error Submitting ${state.provisionalTask}. Please Retry",
          maxLines = 4,
          overflow = TextOverflow.Visible,
          textAlign = TextAlign.Center,
        )
    }
  }
}

@ExperimentalMaterial3Api
@Composable
private fun AddScreenAppBar(
  state: AddTaskState,
  navigateBack: () -> Unit,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  val title = when (state) {
    is AddTaskState.ProcessingError -> "Error Loading"
    AddTaskState.Initial -> "Receiving Audio"
    is AddTaskState.Processing -> "Processing Audio"
    is AddTaskState.Loaded -> "Upload Audio"
    is AddTaskState.UploadingError -> "Error Uploading"
    is AddTaskState.Uploading -> "Uploading Audio"
  }
  CenterAlignedTopAppBar(
    title = { Text(title) },
    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    actions = {
      if (state is AddTaskState.Loaded) {
        IconButton(
          imageVector = Icons.Outlined.Check,
          contentDescription = "Add Content",
          onClick = {
            processIntent(ViewIntent.UploadTask(state))
          },
        )
      }
    },
    navigationIcon = {
      IconButton(
        imageVector = Icons.Outlined.ArrowBack,
        contentDescription = "Navigate Back",
        onClick = {
          navigateBack()
        },
      )
    },
  )
}

@Composable
@SuppressLint("UnusedTransitionTargetStateParameter")
fun PlayPauseButton(
  isPlaying: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  IconToggleButton(
    checked = isPlaying,
    onCheckedChange = { onClick() },
    modifier = modifier,
  ) {
    val transition = updateTransition(isPlaying, label = "Play/Pause indicator")

    val tint by transition.animateColor(
      label = "Tint",
    ) { isChecked ->
      if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    }

    val size by transition.animateDp(
      transitionSpec = {
        if (false isTransitioningTo true) {
          keyframes {
            durationMillis = 250
            30.dp at 0 with LinearOutSlowInEasing // for 0-15 ms
            35.dp at 15 with FastOutLinearInEasing // for 15-75 ms
            40.dp at 75 // ms
            35.dp at 150 // ms
          }
        } else {
          spring(stiffness = Spring.StiffnessVeryLow)
        }
      },
      label = "Size",
    ) { 40.dp }

    Icon(
      imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
      contentDescription = if (isPlaying) "Pause" else "Play",
      tint = tint,
      modifier = Modifier.size(size),
    )
  }
}

@ExperimentalComposeUiApi
@Preview(
  widthDp = 300,
)
@Composable
fun AddScreenPreview() {
  AddScreenContents(
//    state = AddTaskState.Initial
//    state = AddTaskState.Loading.Preview,
    state = AddTaskState.Loaded.Preview,
//    state = AddTaskState.Uploading.Preview,
  ) {}
}
