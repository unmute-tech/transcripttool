package io.reitmaier.transcripttool.core.data.domain

import android.os.Parcelable
import io.reitmaier.transcripttool.core.data.parcelizers.FileIdParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ProvisionalTask(
  val fileId: @WriteWith<FileIdParceler> FileId,
  @SerialName("audio_path")
  val audioPath: String,
  val displayName: String,
) : Parcelable {
  companion object {
    val Preview = ProvisionalTask(FileId.Preview, "Preview Audio Path", "PTT-20220126-TR-1.oga")
  }
}
