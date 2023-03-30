package io.reitmaier.transcripttool.feature.list.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import io.reitmaier.transcripttool.core.data.domain.TaskId
import io.reitmaier.transcripttool.core.data.domain.TaskState
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.core.data.domain.formatLocalDateTime
import io.reitmaier.transcripttool.core.data.util.toHumanReadable
import io.reitmaier.transcripttool.feature.list.R
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun TranscriptRow(
  item: TranscriptTask,
  modifier: Modifier = Modifier,
  onDelete: (TranscriptTask) -> Unit,
  onClick: () -> Unit,
  newTaskId: TaskId?,
) {
  ConstraintLayout(
    modifier = modifier
      .padding(top = 16.dp, start = 12.dp, bottom = 12.dp, end = 12.dp)
      .fillMaxWidth()
      .fillMaxHeight()
      .clickable {
        onClick()
      },
  ) {
    val (name, date, transcript, icon) = createRefs()
    Text(
      item.displayName,
      style = if (item.state != TaskState.COMPLETED) {
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
      } else {
        MaterialTheme.typography.labelLarge
      },
      color = if (item.id == newTaskId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .constrainAs(name) {
          top.linkTo(parent.top)
          start.linkTo(parent.start)
        },

    )
    Text(
      text = "Date: ${item.createdAt.formatLocalDateTime()}\nLength: ${item.length.milliseconds.toHumanReadable()}",
//        item.createdAt.formatLocalDateTime(),
      style = MaterialTheme.typography.labelMedium,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .constrainAs(date) {
          top.linkTo(name.bottom, 4.dp)
          start.linkTo(parent.start)
        },
    )
    val transcriptText = when (item.state) {
      TaskState.NEW,
      TaskState.IN_PROGRESS,
      TaskState.COMPLETED,
      -> item.inputtedTranscript.value.ifBlank { "Empty" }
      TaskState.REJECTED -> "Rejected: ${item.rejectReason?.value}"
    }

    Text(
      text = transcriptText,
      style = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 12.sp,
        fontStyle = if (item.inputtedTranscript.value.isBlank()) FontStyle.Italic else FontStyle.Normal,
        color = MaterialTheme.typography.bodyLarge.color,
      ),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .constrainAs(transcript) {
          top.linkTo(date.bottom, 4.dp)
          start.linkTo(parent.start)
        },
    )

    when (item.state) {
      TaskState.NEW -> Icon(
        imageVector = Icons.Outlined.Star,
        contentDescription = "New",
        modifier = Modifier
          .constrainAs(icon) {
            top.linkTo(name.bottom, 2.dp)
            end.linkTo(parent.end, 4.dp)
          },
        tint = MaterialTheme.colorScheme.onBackground,
      )
      TaskState.IN_PROGRESS -> Icon(
        painter = painterResource(id = R.drawable.outline_unpublished_black_24),
        contentDescription = "In Progress",
        modifier = Modifier
          .constrainAs(icon) {
            top.linkTo(name.bottom, 2.dp)
            end.linkTo(parent.end, 4.dp)
          },
        tint = MaterialTheme.colorScheme.onBackground,
      )
      TaskState.COMPLETED -> Icon(
        painter = painterResource(id = R.drawable.outline_check_circle_black_24),
        contentDescription = "Completed",
        modifier = Modifier
          .constrainAs(icon) {
            top.linkTo(name.bottom, 2.dp)
            end.linkTo(parent.end, 4.dp)
          },
        tint = MaterialTheme.colorScheme.onBackground,
      )
      TaskState.REJECTED -> Icon(
        imageVector = Icons.Filled.Block,
        contentDescription = "Rejected",
        modifier = Modifier
          .constrainAs(icon) {
            top.linkTo(name.bottom, 2.dp)
            end.linkTo(parent.end, 4.dp)
          },
        tint = MaterialTheme.colorScheme.onBackground,
      )
    }
  }
}

@Preview(
  widthDp = 300,
)
@Composable
fun TranscriptRowPreview() {
  TranscriptRow(
    item = TranscriptTask.Preview,
    onDelete = {},
    onClick = {},
    newTaskId = TaskId(1),
  )
}
