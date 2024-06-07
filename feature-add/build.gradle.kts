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

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.compose.compiler)

}

val javaVersion: JavaVersion by rootProject.extra

android {
  namespace = "io.reitmaier.transcripttool.feature.add"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()

    testInstrumentationRunner = "io.reitmaier.transcripttool.core.testing.HiltTestRunner"
  }

  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    renderScript = false
    shaders = false
  }

  compileOptions {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  kotlinOptions {
    jvmTarget = javaVersion.majorVersion
  }
}

dependencies {
  implementation(projects.coreData)
  implementation(projects.coreUi)
  androidTestImplementation(project(":core-testing"))

  // Core Android dependencies
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.constraintlayout.compose)

  // logcat
  implementation(libs.logcat)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.hilt.navigation.compose)

  // Compose
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)

  // monads
  implementation(libs.kotlin.result)
  implementation(libs.kotlin.result.coroutines)
  implementation(libs.kotlin.retry)

  // MVI
  implementation(libs.orbitmvi.core)
  implementation(libs.orbitmvi.viewmodel)

  // Media3

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)
  // Hilt and instrumented tests.
  androidTestImplementation(libs.hilt.android.testing)
  kaptAndroidTest(libs.hilt.android.compiler)
  // Hilt and Robolectric tests.
  testImplementation(libs.hilt.android.testing)
  kaptTest(libs.hilt.android.compiler)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
