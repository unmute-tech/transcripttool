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

@Suppress("DSL_SCOPE_VIOLATION") // Remove when fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.android)
}

val javaVersion: JavaVersion by rootProject.extra

android {
  namespace = "io.reitmaier.transcripttool.core.ui"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    renderScript = false
    shaders = false
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
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

  // Arch Components
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)

  // media3
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)

  // logging
  implementation(libs.logcat)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
}
