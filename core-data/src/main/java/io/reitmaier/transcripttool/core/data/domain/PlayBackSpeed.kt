package io.reitmaier.transcripttool.core.data.domain

enum class PlayBackSpeed(val value: Float) {
  VERY_SLOW(0.5f),
  SLOW(0.75f),
  NORMAL(1.0f),
  FAST(1.5f),
  VERY_FAST(2.0f),
}
