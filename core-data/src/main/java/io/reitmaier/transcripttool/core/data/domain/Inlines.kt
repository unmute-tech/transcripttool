package io.reitmaier.transcripttool.core.data.domain

import android.os.Parcel
import android.os.Parcelable
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import io.reitmaier.transcripttool.core.data.parcelizers.FileIdSerializer
import io.reitmaier.transcripttool.core.data.parcelizers.RemoteIdSerializer

@Serializable
data class CompleteTaskRequest(
  val difficulty: Difficulty?,
  val completed_at: Instant
)

@Serializable
enum class Difficulty {
  EASY,
  MEDIUM,
  HARD,
}

@Serializable
enum class Confidence {
  LOW,
  MEDIUM,
  HIGH,
}

// TODO make internal
@Serializable (with = RemoteIdSerializer::class)
@JvmInline
value class RemoteId(val value: Int) {
  companion object {
    val Preview = RemoteId(1)
  }
}

// TODO can we make constructor private?

@JvmInline
//@Parcelize
value class TaskId(val value: Int) : Parcelable {
  constructor(parcel: Parcel) : this(parcel.readInt())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(value)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<TaskId> {
    override fun createFromParcel(parcel: Parcel): TaskId {
      return TaskId(parcel)
    }

    override fun newArray(size: Int): Array<TaskId?> {
      return arrayOfNulls(size)
    }
  }

}

@JvmInline
value class RegionId(val value: Int)


@JvmInline
value class LocalId(val value: Int)


@Serializable(with = FileIdSerializer::class)
@JvmInline
value class FileId(val value: Int) {
  companion object {
    val Preview = FileId(0)
  }
}

@JvmInline
value class InputtedTranscript(val value: String) {
  companion object {
    val Preview = InputtedTranscript("First Transcript (submitted and completed)")
    val PreviewNew = InputtedTranscript("First Transcript with fresh content")
  }
}

@JvmInline
value class InputtedPhoneNumber(val value: String) {
  companion object {
    val Preview = InputtedPhoneNumber("First Transcript (submitted and completed)")
    val PreviewNew = InputtedTranscript("First Transcript with fresh content")
  }
}

@JvmInline
value class SavedTranscript internal constructor(val value: String) {
  companion object {
    val Preview = SavedTranscript("First Transcript (submitted and completed)")
  }
}

@JvmInline
value class SubmittedTranscript internal constructor(val value: String) {
  companion object {
    val Preview = SubmittedTranscript("First Transcript (submitted and completed)")
    val PreviewStale = SubmittedTranscript("First Transcript")
  }
}


@JvmInline
value class PartialTranscriptId(val value: Int)

@Serializable
data class UserInfo(
  val mobile: MobileNumber,
  val password: Password,
)

