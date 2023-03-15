package io.reitmaier.transcripttool.core.data.domain

import kotlinx.datetime.*

val Instant.localDate: LocalDate
  get() = localDateTime.date

val Instant.localDateTime: LocalDateTime
  get() = toLocalDateTime(TimeZone.currentSystemDefault())

fun Instant.formatLocalDateTime(): String {
  val ldt = localDateTime
  val dateString =
    "${ldt.dayOfMonth.toString().padStart(2,'0')}/${ldt.monthNumber.toString().padStart(2,'0')}/${ldt.year}"
  return "$dateString ${ldt.toString().substringAfter("T").substringBefore(".")}"
}

