package io.reitmaier.transcripttool.core.data.domain

/**
 *  @param current  Current playback position in millisecond.
 *  @param duration Current track duration in millisecond.
 */
data class PlaybackProgress(
    val current: Long,
    val regionPosition: Int,
    val duration: Long
)
