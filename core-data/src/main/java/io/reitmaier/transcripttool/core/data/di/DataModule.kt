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

package io.reitmaier.transcripttool.core.data.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reitmaier.transcripttool.core.data.DefaultTranscriptRepository
import io.reitmaier.transcripttool.core.data.TranscribeDatabase
import io.reitmaier.transcripttool.core.data.TranscribeService
import io.reitmaier.transcripttool.core.data.TranscriptRepo
import io.reitmaier.transcripttool.core.data.TranscriptRepository
import io.reitmaier.transcripttool.core.data.dispatchers.CoroutineDispatchers
import io.reitmaier.transcripttool.core.data.dispatchers.DefaultCoroutineDispatchers
import io.reitmaier.transcripttool.core.data.migrateIfNeeded
import io.reitmaier.transcripttool.core.database.TranscriptDao
import io.reitmaier.transcripttool.data.TranscriptToolDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

  @Provides
  @Singleton
  @Named("transcribeAndroidSqlDriver") // in case you had another db
  fun provideAndroidDriver(app: Application): SqlDriver {
    val driver = AndroidSqliteDriver(
      schema = TranscriptToolDb.Schema,
      context = app,
      name = "transcripttool.db",
    )
    migrateIfNeeded(driver)
    return driver
  }

  @Provides
  @Singleton
  fun provideTranscribeDatabase(
    @Named("transcribeAndroidSqlDriver") sqlDriver: SqlDriver,
  ): TranscribeDatabase {
    return TranscribeDatabase.build(sqlDriver)
  }

  @Provides
  @Singleton
  fun provideService(
    prefs: SharedPreferences,
    @ApplicationContext context: Context,
    dispatchers: CoroutineDispatchers,
  ): TranscribeService {
    return TranscribeService(prefs, context, dispatchers)
  }

  @Provides
  @Singleton
  fun provideRepo(
    database: TranscribeDatabase,
    service: TranscribeService,
    @ApplicationContext context: Context,
    prefs: SharedPreferences,
    dispatchers: CoroutineDispatchers,
  ): TranscriptRepo {
    return TranscriptRepo(
      database = database,
      service = service,
      context = context,
      prefs = prefs,
      dispatchers = dispatchers,
    )
  }

  @Provides
  @Singleton
  fun coroutineDispatchers(impl: DefaultCoroutineDispatchers): CoroutineDispatchers {
    return impl
  }

  @Singleton
  @Provides
  fun providesTranscriptRepository(
    transcriptDao: TranscriptDao,
  ): TranscriptRepository = DefaultTranscriptRepository(transcriptDao)
}

class FakeTranscriptRepository @Inject constructor() : TranscriptRepository {
  override val transcripts: Flow<List<String>> = flowOf(fakeTranscripts)

  override suspend fun add(name: String) {
    throw NotImplementedError()
  }
}

val fakeTranscripts = listOf("One", "Two", "Three")
