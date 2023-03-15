package io.reitmaier.transcripttool.core.data.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PlaybackState: Parcelable {

  @Parcelize
    object Paused : PlaybackState()

  @Parcelize
  object Playing : PlaybackState()

  @Parcelize
    object Loading : PlaybackState()

}