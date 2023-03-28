package io.reitmaier.transcripttool.feature.list.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.reitmaier.transcripttool.core.data.domain.TaskId
import io.reitmaier.transcripttool.core.data.domain.TaskState
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.core.data.util.toHumanReadable
import io.reitmaier.transcripttool.core.ui.pullrefresh.PullRefreshIndicator
import io.reitmaier.transcripttool.core.ui.pullrefresh.pullRefresh
import io.reitmaier.transcripttool.core.ui.pullrefresh.rememberPullRefreshState
import io.reitmaier.transcripttool.feature.list.R
import logcat.LogPriority
import logcat.logcat
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalFoundationApi
@Composable
internal fun TaskList(
  newTaskId: TaskId?,
  isRefreshing: Boolean,
  groupedTasks: Map<TaskState, TasksWithLength>,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit,
  navigateToDetailScreen: (TaskId) -> Unit,
) {
//  val lastIndex = groupedTasks.lastIndex
  val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh)
  Box(
    modifier = modifier
      .fillMaxSize()
      .pullRefresh(pullRefreshState)
  ) {
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(0.dp, 0.dp, 0.dp, 8.dp),
    ) {
      if (groupedTasks.isEmpty()) {
        item {
          Text(
            text = "New Transcript Tasks will appear here. Pull to refresh content. You can also share a voice recording with the Transcribe app.",
            modifier = Modifier
              .fillParentMaxSize()
              .wrapContentHeight(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      groupedTasks.forEach { (type, tasks) ->
        stickyHeader {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(color = MaterialTheme.colorScheme.secondary),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp, 24.dp, 12.dp),
            ) {
              val stickyHeading = when (type) {
                TaskState.NEW -> "New Tasks"
                TaskState.IN_PROGRESS -> "Ongoing Tasks"
                TaskState.COMPLETED -> "Completed Tasks"
                TaskState.REJECTED -> "Rejected Tasks"
              }

              Text(
                text = "$stickyHeading \t (${tasks.length.toHumanReadable()})",
                modifier = Modifier
                  .align(Alignment.CenterStart),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.labelLarge,
              )
              when (type) {
                TaskState.NEW -> Icon(
                  imageVector = Icons.Outlined.Star,
                  contentDescription = "New",
                  modifier = Modifier
                    .align(Alignment.CenterEnd),
                  tint = MaterialTheme.colorScheme.onPrimary,
                )
                TaskState.IN_PROGRESS -> Icon(
                  painter = painterResource(id = R.drawable.outline_unpublished_black_24),
                  contentDescription = "In Progress",
                  modifier = Modifier
                    .align(Alignment.CenterEnd),
                  tint = MaterialTheme.colorScheme.onPrimary,
                )
                TaskState.COMPLETED -> Icon(
                  painter = painterResource(id = R.drawable.outline_check_circle_black_24),
                  contentDescription = "Completed",
                  modifier = Modifier
                    .align(Alignment.CenterEnd),
                  tint = MaterialTheme.colorScheme.onPrimary,
                )
                TaskState.REJECTED -> Icon(
                  imageVector = Icons.Outlined.Block,
                  contentDescription = "Rejected",
                  modifier = Modifier
                    .align(Alignment.CenterEnd),
                  tint = MaterialTheme.colorScheme.onPrimary,
                )
              }
            }
          }
        }
        items(tasks.taskList.size) { i ->
          val item = tasks.taskList[i]
          Card(
            elevation = CardDefaults.elevatedCardElevation(),
            modifier = Modifier
              .padding(4.dp)
              .fillParentMaxWidth(),
//              contentColor = Color.Blue,
//              modifier = paddingModifier
          ) {
            TranscriptRow(
              item = item,
              newTaskId = newTaskId,
              modifier = Modifier
                .padding(4.dp)
                .fillParentMaxWidth(),
              onDelete = {
                //     processIntent(ViewIntent.RemoveTranscript(it))
                logcat("MainActivity", LogPriority.DEBUG) { "Initialising Delete Transcript" }
              },
              onClick = {
                navigateToDetailScreen(item.id)
              },
            )
          }
        }
      }
    }
    PullRefreshIndicator(
      refreshing = isRefreshing,
      state = pullRefreshState,
      modifier = Modifier.align(Alignment.TopCenter)
    )
  }
}

@ExperimentalFoundationApi
@Preview(
  widthDp = 300,
)
@Composable
fun TranscriptListPreview() {
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
    newTaskId = TranscriptTask.Preview.id,
  )
}
