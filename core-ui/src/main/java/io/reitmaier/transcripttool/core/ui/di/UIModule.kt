package io.reitmaier.transcripttool.core.ui.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reitmaier.transcripttool.core.ui.player.ExoAudioPlayer
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

@Module
@ExperimentalTime
@UnstableApi
@InstallIn(SingletonComponent::class)
object UIModule {
  @Provides
  @Singleton
  fun provideExoAudioPlayer(
    @ApplicationContext context: Context
  ): ExoAudioPlayer {
    return ExoAudioPlayer(context)
  }
}
