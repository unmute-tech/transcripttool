package io.reitmaier.transcripttool.core.data.util

import kotlin.time.Duration

fun Duration.toHumanReadable() : String =
  this.toComponents { hours, minutes, seconds, _ ->
    val humanReadableString =  "" +
      when(hours) {
        0L -> ""
        else -> "${hours.toString().padStart(2,'0')}H"
      } +
      " ${minutes.toString().padStart(2,'0')}M" +
      " ${seconds.toString().padStart(2,'0')}S "
    return@toComponents humanReadableString.trim()
  }
