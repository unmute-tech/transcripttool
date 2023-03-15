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
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.parcelize)
}

val javaVersion: JavaVersion by rootProject.extra

android {
  namespace = "io.reitmaier.transcripttool.core.data"
  compileSdk = 33

  defaultConfig {
    minSdk = 21

    testInstrumentationRunner = "io.reitmaier.transcripttool.core.testing.HiltTestRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
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

sqldelight {
  database("TranscriptToolDb") {
    packageName = "io.reitmaier.transcripttool.data"
    sourceFolders = listOf("sqldelight")
    deriveSchemaFromMigrations = true
//    schemaOutputDirectory = file("src/main/sqldelight/databases")
//    verifyMigrations = true
  }
}

dependencies {
  implementation(project(":core-database"))

  // Arch Components
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)

  // coroutines
  implementation(libs.kotlinx.coroutines.android)

  // datetime
  implementation(libs.kotlinx.datetime)

  // json
  implementation(libs.kotlinx.serialization.json)

  // ktor
  implementation(libs.ktor.client.auth)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.serialization.kotlinx.json)

  // conscript + desugaring
  implementation(libs.conscript)
  coreLibraryDesugaring(libs.android.tools.desugar.jdk.libs)

  // monads
  implementation(libs.kotlin.result)
  implementation(libs.kotlin.result.coroutines)
  implementation(libs.kotlin.retry)

  // SQLDelight
  implementation(libs.squareup.sqldelight.android.driver)
  implementation(libs.squareup.sqldelight.coroutine.extensions)
  implementation(libs.squareup.sqldelight.runtime)

  // logging
  implementation(libs.logcat)

  // util
  implementation(libs.androidx.documentfile)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
