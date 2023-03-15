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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.reitmaier.transcripttool.core.data.domain.*
import io.reitmaier.transcripttool.feature.transcript.ui.TranscriptScreen
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import logcat.logcat

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") { TranscriptScreen(modifier = Modifier.padding(16.dp)) }
        // TODO: Add more destinations
    }
}

private const val listRoute = "taskList"
private const val registerRoute = "register"
private const val transcribeRoute = "transcribe"
private const val addRoute = "add"

sealed class Screen(val route: String, val arguments: List<NamedNavArgument>) {
  object ListScreen : Screen(
    route = listRoute,
    arguments = listOf(navArgument(navNewTaskId) {
      type = NavType.IntType
      defaultValue = -1
    })
  )

  object RegisterScreen : Screen(
    route = registerRoute,
    arguments = emptyList()
  )

  object TranscribeScreen : Screen(
    route = transcribeRoute,
    arguments = listOf(navArgument(navArgTaskId) {
      type = NavType.IntType
    })
  )

  object AddScreen : Screen(
    route = addRoute,
    arguments = listOf(
      navArgument(navArgNewAudio) {
        type = ProvisionalTaskParamType()
      },
    )
  )

}

class TaskIdParamType : NavType<TaskId>(isNullableAllowed = true) {
  override fun get(bundle: Bundle, key: String): TaskId? {
    return bundle.getParcelable(key)
  }

  override fun parseValue(value: String): TaskId {
    logcat { value }
    return Json.decodeFromString<TaskId>(value)
  }

  override fun put(bundle: Bundle, key: String, value: TaskId) {
    bundle.putParcelable(key, value)
  }
}
class ProvisionalTaskParamType : NavType<ProvisionalTask>(isNullableAllowed = false) {
  override fun get(bundle: Bundle, key: String): ProvisionalTask? {
    return bundle.getParcelable(key)
  }

  override fun parseValue(value: String): ProvisionalTask {
    logcat { value }
    return Json.decodeFromString<ProvisionalTask>(value)
  }

  override fun put(bundle: Bundle, key: String, value: ProvisionalTask) {
    bundle.putParcelable(key, value)
  }
}
