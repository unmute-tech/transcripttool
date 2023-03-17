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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.michaelbull.result.fold
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint
import io.reitmaier.transcripttool.core.data.TranscriptRepo
import io.reitmaier.transcripttool.core.ui.TranscriptToolTheme
import io.reitmaier.transcripttool.register.RegisterScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Enable drawing under the status bar
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // start at register screen if no saved user info found
    val startDestination = repo.getUserInfo().fold(
      success = {
//        Screen.ListScreen.route + "?$navNewTaskId={$navNewTaskId}"
        // TODO Enable List Screen
        Screen.RegisterScreen.route
      },
      failure = {
        Screen.RegisterScreen.route
      }
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
//            addNewTranscript(
//              navController = navController,
//            )
//            addTranscriptList(
//              navController = navController,
//            )
//            addTranscriptDetail(
//              navController = navController,
//              nextTranscript = repo::nextTask,
//              shareTranscript = ::shareTranscript
//            )
          }
        )
      }
    }
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
        AnimatedContentScope.SlideDirection.Start, animationSpec = tween(
          ANIMATION_SPEED
        )
      )
    },
    exitTransition = {
      slideOutOfContainer(
        AnimatedContentScope.SlideDirection.Start, animationSpec = tween(
          ANIMATION_SPEED
        )
      )
    },
    popEnterTransition = {
      slideIntoContainer(
        AnimatedContentScope.SlideDirection.End, animationSpec = tween(ANIMATION_SPEED)
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
