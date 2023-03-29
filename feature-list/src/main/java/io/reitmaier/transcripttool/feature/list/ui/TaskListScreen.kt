package io.reitmaier.transcripttool.feature.list.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.reitmaier.transcripttool.core.data.domain.TaskId
import io.reitmaier.transcripttool.core.data.domain.TaskState
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.core.data.util.IntentDispatcher
import io.reitmaier.transcripttool.core.ui.components.IconButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalLayoutApi
@OptIn(
  ExperimentalFoundationApi::class,
  ExperimentalCoroutinesApi::class,
  ExperimentalMaterial3Api::class,
)
@FlowPreview
@Composable
fun TaskListScreen(
  newTaskId: TaskId?,
  modifier: Modifier = Modifier,
  navigateToDetailScreen: (TaskId) -> Unit,
) {
  val viewModel = hiltViewModel<TaskListViewModel>()
  val state = viewModel.container.stateFlow.collectAsState().value
  val processIntent = viewModel.processIntent
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(viewModel, snackbarHostState) {
    launch {
      viewModel.processIntent(ViewIntent.UploadChanges)
      viewModel.container.sideEffectFlow.collect { sideEffect ->
        when (sideEffect) {
          is TaskListSideEffect.RefreshFailed -> {
            snackbarHostState.showSnackbar(
              "Failed to refresh list from server. Please try again and check that you have internet access.",
            )
          }
          TaskListSideEffect.RefreshSucceeded ->
            snackbarHostState.showSnackbar("Refresh Completed.")
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
    topBar = {
      TaskListAppBar(
        processIntent = processIntent,
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
      // Screen content
      when (state) {
        is TaskListState.Error -> {
          Column(
            modifier = modifier
              .fillMaxSize()
              .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "An error occurred", // TODO be more descriptive on error type
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = {
                // TODO
              },
              contentPadding = PaddingValues(
                vertical = 12.dp,
                horizontal = 24.dp,
              ),
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(text = "RETRY")
            }
          }
        }
        is TaskListState.Loaded -> {
          TaskList(
            newTaskId = newTaskId,
            isRefreshing = state.isRefreshing,
            groupedTasks = state.groupedTasks,
            navigateToDetailScreen = navigateToDetailScreen,
            onRefresh = { processIntent(ViewIntent.Refresh) },
//            modifier = Modifier.padding(4.dp)
          )
        }
      }
    }
  }
}

@ExperimentalFoundationApi
@Preview(
  widthDp = 300,
)
@Composable
fun TranscriptScreenPreview() {
  val items = mapOf(
    Pair(
      TaskState.NEW,
      TasksWithLength(
        listOf(
          TranscriptTask.Preview,
          TranscriptTask.Preview2,
          TranscriptTask.Preview3,
        ),
        10.milliseconds,
      ),
    ),
  )
  TaskList(
    isRefreshing = false,
    groupedTasks = items,
    onRefresh = {},
    navigateToDetailScreen = { },
    newTaskId = TaskId(1),
  )
}

@ExperimentalMaterial3Api
@Composable
private fun TaskListAppBar(
  processIntent: IntentDispatcher<ViewIntent>,
) {
  val title = "Transcription Tasks"
  CenterAlignedTopAppBar(
    title = { Text(title) },
    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    actions = {
      IconButton(
        imageVector = Icons.Outlined.Refresh,
        contentDescription = "Refresh",
        onClick = {
          processIntent(ViewIntent.Refresh)
        },
      )
    },
  )
}
