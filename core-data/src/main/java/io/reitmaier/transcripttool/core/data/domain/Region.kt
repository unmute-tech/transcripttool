package io.reitmaier.transcripttool.core.data.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import io.reitmaier.transcripttool.core.data.parcelizers.RegionIdParceler
import io.reitmaier.transcripttool.core.data.parcelizers.TaskIdParceler

@Parcelize
data class Region(
  val id: @WriteWith<RegionIdParceler> RegionId,
  val taskId: @WriteWith<TaskIdParceler> TaskId,
  val start: Int,
  val end: Int,
  val active: Boolean,
  val playCount: Int,
) : Parcelable
