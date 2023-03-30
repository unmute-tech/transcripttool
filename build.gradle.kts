import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_SRC_DIR_KOTLIN
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_JAVA
import io.gitlab.arturbosch.detekt.extensions.DetektExtension.Companion.DEFAULT_TEST_SRC_DIR_KOTLIN
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

buildscript {
  dependencies {
    classpath(libs.plugins.kotlin.gradle.get().toString())
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20")
  }
}
@Suppress("DSL_SCOPE_VIOLATION") // because of https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.detekt)
  alias(libs.plugins.hilt.gradle) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.sqldelight) apply false
}
// Root build.gradle.kts
val javaVersion by extra { JavaVersion.VERSION_11 }

allprojects {
  apply {
    plugin(rootProject.libs.plugins.detekt.get().pluginId)
  }

  dependencies {
    detektPlugins(rootProject.libs.io.gitlab.arturbosch.detekt.formatting)
  }

  detekt {
    source = files(
      "src",
      DEFAULT_SRC_DIR_JAVA,
      DEFAULT_TEST_SRC_DIR_JAVA,
      DEFAULT_SRC_DIR_KOTLIN,
      DEFAULT_TEST_SRC_DIR_KOTLIN,
    )
    toolVersion = rootProject.libs.versions.detekt.get()
    config = rootProject.files("config/detekt/detekt.yml")
    parallel = true
  }
}
