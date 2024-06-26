import java.util.*

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
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.compose.compiler)
}

val javaVersion: JavaVersion by rootProject.extra

android {
  namespace = "io.reitmaier.transcripttool"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "io.reitmaier.transcripttool"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "io.reitmaier.transcripttool.core.testing.HiltTestRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  kotlinOptions {
    jvmTarget = javaVersion.majorVersion
  }

  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    renderScript = false
    shaders = false
  }

  packagingOptions {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation(projects.coreData)
  implementation(projects.coreUi)
  implementation(projects.featureAdd)
  implementation(projects.featureList)
  implementation(projects.featureRegister)
  implementation(projects.featureTranscribe)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // logcat
  implementation(libs.logcat)

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)
  // Hilt and instrumented tests.
  androidTestImplementation(libs.hilt.android.testing)
  kaptAndroidTest(libs.hilt.android.compiler)
  // Hilt and Robolectric tests.
  testImplementation(libs.hilt.android.testing)
  kaptTest(libs.hilt.android.compiler)

  // Arch Components
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.hilt.navigation.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  // json
  implementation(libs.kotlinx.serialization.json)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)

  // monads
  implementation(libs.kotlin.result)
  implementation(libs.kotlin.result.coroutines)
  implementation(libs.kotlin.retry)

  // navigation
  implementation(libs.androidx.navigation.compose)

  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
