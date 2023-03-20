package io.reitmaier.transcripttool.core.data.domain

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IncomingContent(
  val uri: Uri,
  val subject: String?,
  val mimeType: String,
) : Parcelable
