package io.reitmaier.transcripttool.feature.transcribe

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import io.reitmaier.transcripttool.core.data.domain.Difficulty
import io.reitmaier.transcripttool.core.data.domain.FullTranscriptTask
import io.reitmaier.transcripttool.core.data.domain.InputtedTranscript
import io.reitmaier.transcripttool.core.data.domain.SavedTranscript
import io.reitmaier.transcripttool.core.data.domain.SubmittedTranscript
import io.reitmaier.transcripttool.core.data.domain.TaskId
import io.reitmaier.transcripttool.core.data.domain.TranscriptTask
import io.reitmaier.transcripttool.core.data.util.IntentDispatcher
import io.reitmaier.transcripttool.core.ui.components.IconButton
import io.reitmaier.transcripttool.core.ui.components.IconTextButton
import io.reitmaier.transcripttool.core.ui.components.PlayPauseButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalComposeUiApi
@ExperimentalTime
@FlowPreview
@Composable
fun TranscribeScreen(
  modifier: Modifier = Modifier,
  taskId: TaskId,
  shareTranscript: (SavedTranscript) -> Unit,
  navigateBackToList: () -> Unit,
  navigateToNextOrBack: () -> Unit,
) {
  val viewModel = hiltViewModel<TranscribeViewModel>()
  val state = viewModel.container.stateFlow.collectAsState().value
  val processIntent = viewModel.processIntent

  val snackbarHostState = remember { SnackbarHostState() }

  var openBottomSheet by rememberSaveable { mutableStateOf(false) }
  val skipPartiallyExpanded by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val bottomSheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = skipPartiallyExpanded,
  )

  DisposableEffect(viewModel) {
    onDispose { viewModel.onStop() }
  }
  BackHandler(enabled = true) {
    bottomSheetState.currentValue.let { sheetStateValue ->
      when (sheetStateValue) {
        SheetValue.Hidden -> navigateBackToList()
        SheetValue.Expanded,
        SheetValue.PartiallyExpanded,
        -> scope.launch { bottomSheetState.hide() }
      }
    }
  }
  val context = LocalContext.current
  LaunchedEffect(viewModel, snackbarHostState) {
    launch {
      viewModel.container.sideEffectFlow.collect {
        when (it) {
          is TranscriptTaskSideEffect.CompletionFailed -> {
            snackbarHostState.showSnackbar("Something went wrong. Please try again.")
          }
          TranscriptTaskSideEffect.CompletionSucceeded -> {
            Toast.makeText(context, "Task Completed", Toast.LENGTH_SHORT).show()
            navigateToNextOrBack()
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
      TranscribeAppBar(
        state = state,
        processIntent = processIntent,
        navigateBack = navigateBackToList,
        shareTranscript = shareTranscript,
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
      when (state) {
        is TranscriptTaskState.Error -> {
          Column(
            modifier = modifier
              .fillMaxSize()
              .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              // TODO Map domain message
              text = "An expected error occurred",
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = { viewModel.processIntent(ViewIntent.RetryReload) },
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
        is TranscriptTaskState.Initial,
        is TranscriptTaskState.Loading,
        -> {
          Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            CircularProgressIndicator()
          }
        }
        is TranscriptTaskState.Loaded -> {
          if (state.isConfirming != null) {
            CompleteDialog(
              confirmationInfo = state.isConfirming,
              onConfirm = { difficulty -> processIntent(ViewIntent.Confirm(difficulty)) },
              onDismiss = { processIntent(ViewIntent.DismissConfirm) },
              onDifficultySelected = { processIntent(ViewIntent.RateDifficulty(it)) },
            )
          }
          TranscribeContents(
            data = state,
            regionProgress = state.progress,
            isUploading = state.isUploading,
            regionIndex = state.regionIndex,
            isPlaying = state.isPlaying,
            isSaving = state.isSaving,
            showSheet = { openBottomSheet = true },
            processIntent = processIntent,
          )
        }
      }
    }
    if (openBottomSheet) {
      ModalBottomSheet(
        onDismissRequest = { openBottomSheet = false },
        sheetState = bottomSheetState,
      ) {
        Box(
          modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .height(200.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        ) {
          Column(
            modifier = modifier
              .fillMaxSize()
              .padding(8.dp),
          ) {
            Text("Mark content as:", style = MaterialTheme.typography.headlineSmall)
            ListItem(
              headlineContent = { Text("Inappropriate") },
              leadingContent = {
                Icon(
                  Icons.Filled.Block,
                  contentDescription = "Inappropriate Content",
                )
              },
            )
            ListItem(
              headlineContent = { Text("Underage Content") },
              leadingContent = {
                Icon(
                  Icons.Filled.Block,
                  contentDescription = "Underage Content",
                )
              },
            )
            ListItem(
              headlineContent = { Text("Blank / Background Noise") },
              leadingContent = {
                Icon(
                  Icons.Filled.Block,
                  contentDescription = "Blank / Background Noise",
                )
              },
            )
          }
        }
      }
    }
  }
}

@ExperimentalMaterial3Api
@Composable
private fun TranscribeAppBar(
  state: TranscriptTaskState,
  shareTranscript: (SavedTranscript) -> Unit,
  navigateBack: () -> Unit,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  val title = when (state) {
    is TranscriptTaskState.Initial,
    is TranscriptTaskState.Loading,
    -> "Loading Transcription ..."
    is TranscriptTaskState.Loaded -> "Transcribe Audio"
    is TranscriptTaskState.Error -> "Error"
  }
  CenterAlignedTopAppBar(
    title = { Text(title) },
    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    actions = {
      state.let {
        if (it is TranscriptTaskState.Loaded) {
          IconButton(
            imageVector = Icons.Outlined.Done,
            // TODO enable based on submission
            enabled = it.canUpload,
            contentDescription = "Complete Transcript",
          ) {
            // Upload Action
            processIntent(ViewIntent.ShowConfirm)
          }
          IconButton(
            imageVector = Icons.Outlined.Share,
            enabled = it.canShare,
            contentDescription = "Share Transcript",
          ) {
            shareTranscript(it.savedTranscript)
          }
        }
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

@ExperimentalComposeUiApi
@Composable
private fun TranscribeContents(
  data: TranscriptTaskState.Loaded,
  regionProgress: Float,
  isUploading: Boolean,
  isSaving: Boolean,
  regionIndex: Int,
  isPlaying: Boolean,
//  modifier: Modifier = Modifier,
  showSheet: () -> Unit,
  processIntent: IntentDispatcher<ViewIntent>,
) {
  Column(
    verticalArrangement = Arrangement.Top,
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .imePadding()
      .background(MaterialTheme.colorScheme.background),
  ) {
    ConstraintLayout(
      modifier = Modifier
//      .fillMaxWidth()
        .fillMaxHeight(),
    ) {
      val (previous, playPause, next, input, more, speed, uploadProgress, progressIndicator) = createRefs()
      createHorizontalChain(speed, previous, playPause, next, more, chainStyle = ChainStyle.SpreadInside)

      if (isUploading) {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp)
            .constrainAs(uploadProgress) {
//                centerHorizontallyTo(parent)
              start.linkTo(parent.start)
              end.linkTo(parent.end)
              top.linkTo(parent.top)
            },
        )
      }

//      val transcriptText = remember { mutableStateOf(item.task.inputtedTranscript.value)  }

//      val cursorState = remember {
//        mutableStateOf(TextRange(data.inputtedTranscript.value.length))
//      }
      // initialize focus reference to be able to request focus programmatically
      val focusRequester = FocusRequester()
      DisposableEffect(Unit) {
        focusRequester.requestFocus()
        onDispose { }
      }
      TextField(
        label = { Text("Transcript") },
        value = data.inputtedTranscript,
        onValueChange = {
          processIntent(ViewIntent.Update(it))
        },
        singleLine = false,
//        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        modifier = Modifier
//          .navigationBarsWithImePadding()
          .focusRequester(focusRequester)
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 6.dp)
          .onKeyEvent {
            false
//          if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER){
// //            focusRequester.requestFocus()
//            true
//          }
//          false
          }
          .constrainAs(input) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(parent.top)
            bottom.linkTo(progressIndicator.top)
            height = Dimension.fillToConstraints
//            top.linkTo(parent.bottom, 8.dp)
          },
      )

      Row(
        Modifier
          .fillMaxWidth()
          .padding(8.dp)
          .constrainAs(progressIndicator) {
            centerHorizontallyTo(parent)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(playPause.top, 8.dp)
//            top.linkTo(parent.bottom, 8.dp)
          },
      ) {
        val color = MaterialTheme.colorScheme.primary
        val backgroundColor = color.copy(alpha = 0.25f)
        val stroke: Float
        with(LocalDensity.current) {
          stroke = 4.dp.toPx()
        }
        val modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 1.dp)
          .height(4.dp)
          .focusable()
          .weight(1f)
        for (r in data.transcript.regions.indices) {
          val progress =
            when {
              r < regionIndex -> {
                1f
              }
              r == regionIndex -> {
                regionProgress
              }
              else -> {
                0f
              }
            }
          LinearProgressIndicator(modifier = modifier.progressSemantics(progress), progress = progress)

//          Canvas(
//            modifier
//              .progressSemantics(progress)
// //            .fillMaxWidth()
//          ) {
//            // Draw Background
//            drawSegments(1f, backgroundColor, stroke)
//            // Draw Foreground
//            drawSegments(progress, color, stroke)
//          }
        }
      }
//      SegmentedProgressIndicator(
//        progress = regionProgress,
//        numberOfSegments = item.regions.size,
//        modifier = Modifier
//          .padding(top = 4.dp, start = 12.dp, end = 12.dp)
//          .fillMaxWidth()
//          .constrainAs(progressIndicator) {
//            centerHorizontallyTo(parent)
//            start.linkTo(parent.start)
//            end.linkTo(parent.end)
//            bottom.linkTo(playPause.top, 8.dp)
// //            top.linkTo(parent.bottom, 8.dp)
//          }
//      )
      IconTextButton(
        imageVector = Icons.Outlined.Speed,
        text = "${data.playBackSpeed.value}x",
        enabled = true,
        contentDescription = "Playback Speed",
        buttonColors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground),
        modifier = Modifier
          .constrainAs(speed) {
//            centerHorizontallyTo(parent)
            centerVerticallyTo(playPause)
//            top.linkTo(playPause.top, margin = 0.dp)
          },
      ) {
        processIntent(ViewIntent.TogglePlayBackSpeed)
      }
      IconButton(
        imageVector = Icons.Filled.SkipPrevious,
        enabled = regionIndex > 0,
        contentDescription = "Previous Segment",
        modifier = Modifier
          .size(50.dp)
          .constrainAs(previous) {
//            centerHorizontallyTo(parent)
            bottom.linkTo(parent.bottom, margin = 4.dp)
          },
      ) {
        processIntent(ViewIntent.PlayPrevious)
      }
      PlayPauseButton(
        isPlaying = isPlaying,
        modifier = Modifier
          .size(50.dp)
          .constrainAs(playPause) {
//            centerHorizontallyTo(parent)
            bottom.linkTo(parent.bottom, margin = 4.dp)
          },
      ) {
        processIntent(ViewIntent.PlayPause)
      }

      IconButton(
        imageVector = Icons.Filled.SkipNext,
        enabled = regionIndex < data.transcript.regions.size - 1,
        contentDescription = "Next Segment",
        modifier = Modifier
          .size(50.dp)
          .constrainAs(next) {
//            centerHorizontallyTo(parent)
            bottom.linkTo(parent.bottom, margin = 4.dp)
          },
      ) { processIntent(ViewIntent.PlayNext) }

      IconButton(
        imageVector = Icons.Outlined.Block,
        enabled = true,
        contentDescription = "Block Content",
        modifier = Modifier
          .size(50.dp)
          .constrainAs(more) {
//            centerHorizontallyTo(parent)
            bottom.linkTo(parent.bottom, margin = 4.dp)
          },
      ) {
        showSheet()
      }
    }
    // TODO select length of pause
    // TODO select segment length
    // TODO playback speed

    // TODO headphone shortcuts (long)press on action button
    // Listening privately

    // Pro shortcuts
    // TODO enter to play next segment
    // TODO shift enter repeat segment
//    if(item.task.audio_file.isNotBlank()) {
//      VideoPlayer(item = item.task.audio_file,
//        onUpdateProgress = {progress = it / 26894f })
//    } else {
//      Text(
//        "No Audio File",
//        style = MaterialTheme.typography.body2.copy(
//          fontSize = 11.sp,
//          color = MaterialTheme.typography.caption.color,
//        ),
//        maxLines = 1,
//      )
//    }
  }
}

@ExperimentalComposeUiApi
@Preview(
  widthDp = 300,
)
@Composable
fun TranscribeScreenPreview() {
  TranscribeContents(
    data = TranscriptTaskState.Loaded(
      id = TaskId(1),
      inputtedTranscript = TextFieldValue(InputtedTranscript.Preview.value),
      savedTranscript = SavedTranscript.Preview,
      submittedTranscript = SubmittedTranscript.Preview,
      transcript = FullTranscriptTask(
        task = TranscriptTask.Preview,
        regions = listOf(),
        partialTranscripts = listOf(),
        localFilePath = "",
      ),
      rejectReason = null,
      isConfirming = null,
    ),
    regionProgress = 0.75f,
    isUploading = false,
    isSaving = true,
    regionIndex = 0,
    isPlaying = true,
    showSheet = {},
    {
      Unit
    },
  )
//  CompleteAlertDialog(onConfirm = { /*TODO*/ }) {}
//  CompleteDialog(confirmationInfo = ConfirmationInfo(), onConfirm = { }, onDifficultySelected = {}) {}
}

@Composable
private fun CompleteDialog(
  confirmationInfo: ConfirmationInfo,
  difficultyOptions: Array<Difficulty> = Difficulty.values(),
  onDifficultySelected: (Difficulty) -> Unit,
  onConfirm: (Difficulty) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(4.dp),
    ) {
      Column(modifier = Modifier.padding(8.dp)) {
        Text(
          text = "Complete Transcript?",
          style = MaterialTheme.typography.labelLarge
            .copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Color Selection

        Text(
          text = "Rate task difficulty:",
          modifier = Modifier.padding(16.dp, 0.dp),
          style = MaterialTheme.typography.labelMedium,
        )
        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        ) {
          difficultyOptions.forEachIndexed { index, item ->
            OutlinedButton(
              onClick = { onDifficultySelected(item) },
              modifier = when (index) {
                0 ->
                  Modifier
                    .offset(0.dp, 0.dp)
                    .zIndex(if (confirmationInfo.difficulty == item) 1f else 0f)
                else ->
                  Modifier
                    .offset((-1 * index).dp, 0.dp)
                    .zIndex(if (confirmationInfo.difficulty == item) 1f else 0f)
              },
              shape = when (index) {
                // left outer button
                0 -> RoundedCornerShape(topStart = 4.dp, topEnd = 0.dp, bottomStart = 4.dp, bottomEnd = 0.dp)
                // right outer button
                difficultyOptions.size - 1 -> RoundedCornerShape(
                  topStart = 0.dp,
                  topEnd = 4.dp,
                  bottomStart = 0.dp,
                  bottomEnd = 4.dp,
                )
                // middle buttons
                else -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
              },
              border = BorderStroke(
                1.dp,
                if (confirmationInfo.difficulty == item) { MaterialTheme.colorScheme.primary } else {
                  MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.65f,
                  )
                },
              ),
              colors = if (confirmationInfo.difficulty == item) {
                // selected colors
                ButtonDefaults.outlinedButtonColors(
                  containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                  contentColor = MaterialTheme.colorScheme.primary,
                )
              } else {
                // not selected colors
                ButtonDefaults.outlinedButtonColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  contentColor = MaterialTheme.colorScheme.primary,
                )
              },
            ) {
              Text(
                text = item.name.toLowerCase(Locale.current),
                color = if (confirmationInfo.difficulty == item) { MaterialTheme.colorScheme.primary } else {
                  MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.9f,
                  )
                },
                modifier = Modifier.padding(horizontal = 4.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        Row(
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth(),
        ) {
          TextButton(onClick = onDismiss) {
            Text(text = "CANCEL")
          }
          Spacer(modifier = Modifier.width(4.dp))
//          val confirmEnabled = false
          TextButton(
            enabled = confirmationInfo.isComplete,
            // isComplete check guards here
            onClick = { onConfirm(confirmationInfo.difficulty!!) },
          ) {
            Text(text = "COMPLETE")
          }
        }
      }
    }
  }
}

@Composable
private fun CompleteAlertDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(text = "OK") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(text = "Cancel") }
    },
    title = { Text(text = "Please confirm") },
    text = { Text(text = "Is the transcript complete and ready to be submitted?") },
  )
}

@Composable
fun BottomSheetListItem(icon: ImageVector, title: String, contentDescription: String, onItemClick: (String) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = { onItemClick(title) })
      .height(55.dp)
      .background(color = MaterialTheme.colorScheme.background)
      .padding(start = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onBackground)
    Spacer(modifier = Modifier.width(20.dp))
    Text(text = title, color = MaterialTheme.colorScheme.onBackground)
  }
}
