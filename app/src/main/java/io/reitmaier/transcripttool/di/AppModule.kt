package io.reitmaier.transcripttool.di

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
  @Provides
  fun @receiver:ApplicationContext Context.provideSharedPreferences(): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(this)
  }

}
