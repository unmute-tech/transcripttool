/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reitmaier.transcripttool

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.github.michaelbull.result.fold
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint
import io.reitmaier.transcripttool.core.data.TranscriptRepo
import io.reitmaier.transcripttool.core.data.domain.IncomingContent
import io.reitmaier.transcripttool.core.data.domain.NAV_ARG_NEW_AUDIO
import io.reitmaier.transcripttool.core.data.domain.NAV_ARG_TASK_ID
import io.reitmaier.transcripttool.core.data.domain.NAV_NEW_TASK_ID
import io.reitmaier.transcripttool.core.data.domain.ProvisionalTask
import io.reitmaier.transcripttool.core.data.domain.SavedTranscript
import io.reitmaier.transcripttool.core.data.domain.TaskId
import io.reitmaier.transcripttool.core.ui.TranscriptToolTheme
import io.reitmaier.transcripttool.feature.add.AddScreen
import io.reitmaier.transcripttool.feature.list.ui.TaskListScreen
import io.reitmaier.transcripttool.feature.transcribe.TranscribeScreen
import io.reitmaier.transcripttool.register.RegisterScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logcat.logcat
import javax.inject.Inject
import kotlin.time.ExperimentalTime

const val ANIMATION_SPEED = 300

@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalTime
@ExperimentalComposeUiApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private lateinit var navController: NavHostController

  @Inject
  lateinit var repo: TranscriptRepo

  @Suppress("DEPRECATION")
  private fun processIntent(intent: Intent): IncomingContent? {
    val incomingContent: IncomingContent? =
      if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
          val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
          val mimeType = intent.type.toString().split(" ")[0].removeSuffix(";")
          val incomingContent = IncomingContent(uri, subject, mimeType)
          incomingContent
        }.also {
          // Tidy Up
          intent.replaceExtras(Bundle())
          intent.action = ""
          intent.data = null
          intent.flags = 0
        }
      } else {
        null
      }
    return incomingContent
  }

  private fun shareTranscript(savedTranscript: SavedTranscript) {
    val shareIntent: Intent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(
        Intent.EXTRA_TEXT,
        savedTranscript.value.trim(),
      )
      type = "text/plain"
    }
    val chooser = Intent.createChooser(shareIntent, "Share Transcript ...")
    startActivity(chooser)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    logcat { "onCreate: ${intent.action}" }
    val incomingContent = processIntent(intent).also { intent = null }
    // Enable drawing under the status bar
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // start at register screen if no saved user info found
    val startDestination = repo.getUserInfo().fold(
      success = {
        Screen.ListScreen.route + "?$NAV_NEW_TASK_ID={$NAV_NEW_TASK_ID}"
      },
      failure = {
        Screen.RegisterScreen.route
      },
    )
    setContent {
      navController = rememberAnimatedNavController()
      TranscriptToolTheme {
        AnimatedNavHost(
          navController = navController,
          startDestination = startDestination,
          builder = {
            addRegister(
              navController = navController,
            )
            addNewTranscript(
              navController = navController,
            )
            addTranscriptList(
              navController = navController,
            )
            addTranscriptDetail(
              navController = navController,
              nextTranscript = repo::nextTask,
              shareTranscript = ::shareTranscript,
            )
          },
        )
      }
      LaunchedEffect(incomingContent) {
        if (incomingContent != null) {
          logcat { "Launched with Incoming Audio Content $incomingContent" }
          repo.createProvisionalTask(incomingContent).fold(
            success = {
              val encodedJson = Uri.encode(Json.encodeToString(it))
              navController.navigate("${Screen.AddScreen.route}/$encodedJson")
            },
            failure = {
              Toast.makeText(this@MainActivity, "Unable to load external content", Toast.LENGTH_SHORT).show()
            },
          )
        }
      }
    }
  }
}

@ExperimentalAnimationApi
@FlowPreview
@ExperimentalTime
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalLayoutApi
fun NavGraphBuilder.addTranscriptDetail(
  navController: NavController,
  nextTranscript: (TaskId) -> TaskId?,
  shareTranscript: (SavedTranscript) -> Unit,
) {
  composable(
    route = Screen.TranscribeScreen.route + "/{$NAV_ARG_TASK_ID}",
    arguments = Screen.TranscribeScreen.arguments,
    enterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.Start,
        animationSpec = tween(ANIMATION_SPEED),
      )
    },
    exitTransition = {
      slideOutOfContainer(
        AnimatedContentScope.SlideDirection.End,
        animationSpec = tween(ANIMATION_SPEED),
      )
    },
    popEnterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.End,
        animationSpec = tween(ANIMATION_SPEED),
      )
    },
  ) {
    val navBackToList = {
      navController.navigate(Screen.ListScreen.route + "?$NAV_NEW_TASK_ID=-1") {
        launchSingleTop = true
        it.destination.route?.let { transcribeRoute -> popUpTo(transcribeRoute) { inclusive = true } }
      }
    }
    // TODO Add guard?
    val taskId: TaskId = it.arguments?.getInt(NAV_NEW_TASK_ID)?.let { value -> TaskId(value) }!!
    TranscribeScreen(
      taskId = taskId,
      shareTranscript = shareTranscript,
      navigateBackToList = navBackToList,
      navigateToNextOrBack = {
        val nextTaskId = nextTranscript(taskId)
        if (nextTaskId != null) {
          logcat { "Navigating to $nextTaskId" }
          navController.navigate("${Screen.TranscribeScreen.route}/${nextTaskId.value}")
        } else {
          navBackToList()
        }
      },
    )
  }
}

@ExperimentalAnimationApi
@FlowPreview
@ExperimentalLayoutApi
fun NavGraphBuilder.addTranscriptList(
  navController: NavController,
) {
  composable(
    route = Screen.ListScreen.route + "?$NAV_NEW_TASK_ID={$NAV_NEW_TASK_ID}",
    arguments = Screen.ListScreen.arguments,
//    arguments =  listOf(navArgument(navNewTaskId) { defaultValue = -1 }),
    enterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.Start,
        animationSpec = tween(
          ANIMATION_SPEED,
        ),
      )
    },
    exitTransition = {
      slideOutOfContainer(
        AnimatedContentScope.SlideDirection.Start,
        animationSpec = tween(
          ANIMATION_SPEED,
        ),
      )
    },
    popEnterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.End,
        animationSpec = tween(ANIMATION_SPEED),
      )
    },
  ) {
    val id = it.arguments?.getInt(NAV_NEW_TASK_ID)?.let { newTask -> TaskId(newTask) }
    logcat { "Launched List Screen with Argument $id" }
    TaskListScreen(
      id,
      navigateToDetailScreen = { taskId ->
        navController.navigate("${Screen.TranscribeScreen.route}/${taskId.value}")
      },
    )
  }
}

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalComposeUiApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addNewTranscript(
  navController: NavController,
) {
  composable(
    route = Screen.AddScreen.route + "/{$NAV_ARG_NEW_AUDIO}",
    arguments = Screen.AddScreen.arguments,
    enterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.Start,
        animationSpec = tween(ANIMATION_SPEED),
      )
    },
    exitTransition = {
      slideOutOfContainer(
        AnimatedContentScope.SlideDirection.End,
        animationSpec = tween(ANIMATION_SPEED),
      )
    },
    popEnterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.End,
        animationSpec = tween(ANIMATION_SPEED),
      )
    },
  ) {
    it.destination.route
    val task = it.arguments?.getParcelable<ProvisionalTask>(NAV_ARG_NEW_AUDIO)
    AddScreen(
      provisionalTask = task,
      onTaskAdded = { newTask ->
        logcat { "Navigating to List Screen, highlighting: ${newTask.id}" }
        navController.navigate(
          Screen.ListScreen.route + "?$NAV_ARG_NEW_AUDIO=${newTask.id.value}",
        ) {
          launchSingleTop = true
          it.destination.route?.let { addRoute -> popUpTo(addRoute) { inclusive = true } }
        }
      },
    ) { navController.popBackStack() }
  }
}

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalComposeUiApi
@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
fun NavGraphBuilder.addRegister(
  navController: NavController,
) {
  composable(
    route = Screen.RegisterScreen.route,
    enterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.Start,
        animationSpec = tween(
          ANIMATION_SPEED,
        ),
      )
    },
    exitTransition = {
      slideOutOfContainer(
        AnimatedContentScope.SlideDirection.Start,
        animationSpec = tween(
          ANIMATION_SPEED,
        ),
      )
    },
    popEnterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.End,
        animationSpec = tween(ANIMATION_SPEED),
      )
    },
  ) {
    RegisterScreen(
      onRegistration = {
        logcat { "Navigating to List Screen" }
        navController.navigate(Screen.ListScreen.route) {
          popUpTo(Screen.RegisterScreen.route) { inclusive = true }
        }
      },
    )
  }
}
