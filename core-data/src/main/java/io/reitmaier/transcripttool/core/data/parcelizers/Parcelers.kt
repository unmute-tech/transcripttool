package io.reitmaier.transcripttool.core.data.parcelizers

import android.os.Parcel
import io.reitmaier.transcripttool.core.data.domain.*
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parceler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration

object RegionIdParceler : Parceler<RegionId> {
  override fun create(parcel: Parcel): RegionId {
    val intValue = parcel.readInt()
    return RegionId(intValue)
  }
  override fun RegionId.write(parcel: Parcel, flags: Int) {
    parcel.writeInt(this.value)
  }
}
object TaskIdParceler : Parceler<TaskId> {
  override fun create(parcel: Parcel): TaskId {
    val intValue = parcel.readInt()
    return TaskId(intValue)
  }
  override fun TaskId.write(parcel: Parcel, flags: Int) {
    parcel.writeInt(this.value)
  }
}

object RemoteIdParceler : Parceler<RemoteId> {
  override fun create(parcel: Parcel): RemoteId {
    val intValue = parcel.readInt()
    return RemoteId(intValue)
  }
  override fun RemoteId.write(parcel: Parcel, flags: Int) {
    parcel.writeInt(this.value)
  }
}

object FileIdParceler : Parceler<FileId> {
  override fun create(parcel: Parcel): FileId {
    val intValue = parcel.readInt()
    return FileId(intValue)
  }
  override fun FileId.write(parcel: Parcel, flags: Int) {
    parcel.writeInt(this.value)
  }
}
object LocalIdParceler : Parceler<LocalId> {
  override fun create(parcel: Parcel): LocalId {
    val intValue = parcel.readInt()
    return LocalId(intValue)
  }
  override fun LocalId.write(parcel: Parcel, flags: Int) {
    parcel.writeInt(this.value)
  }
}

object PartialTranscriptIdParceler : Parceler<PartialTranscriptId> {
  override fun create(parcel: Parcel): PartialTranscriptId {
    val intValue = parcel.readInt()
    return PartialTranscriptId(intValue)
  }
  override fun PartialTranscriptId.write(parcel: Parcel, flags: Int) {
    parcel.writeInt(this.value)
  }
}

object MobileNumberParceler : Parceler<MobileNumber> {
  override fun create(parcel: Parcel): MobileNumber {
    val str = parcel.readString()
    require(str != null)
    return MobileNumber(str)
  }
  override fun MobileNumber.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this.value)
  }
}
object NameParceler : Parceler<Name> {
  override fun create(parcel: Parcel): Name {
    val str = parcel.readString()
    require(str != null)
    return Name(str)
  }
  override fun Name.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this.value)
  }
}

object InputtedTranscriptParceler : Parceler<InputtedTranscript> {
  override fun create(parcel: Parcel): InputtedTranscript {
    val str = parcel.readString()
    require(str != null)
    return InputtedTranscript(str)
  }
  override fun InputtedTranscript.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this.value)
  }
}

object SavedTranscriptParceler : Parceler<SavedTranscript> {
  override fun create(parcel: Parcel): SavedTranscript {
    val str = parcel.readString()
    require(str != null)
    return SavedTranscript(str)
  }
  override fun SavedTranscript.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this.value)
  }
}
object NullableSubmittedTranscriptParceler : Parceler<SubmittedTranscript?> {
  override fun create(parcel: Parcel): SubmittedTranscript? {
    val str = parcel.readString() ?: return null
    return SubmittedTranscript(str)
  }
  override fun SubmittedTranscript?.write(parcel: Parcel, flags: Int) {
    if (this == null) {
      parcel.writeValue(null)
    } else {
      parcel.writeString(this.value)
    }
  }
}

object InstantParceler : Parceler<Instant> {
  override fun create(parcel: Parcel): Instant {
    val str = parcel.readString()
    require(str != null)
    return Instant.parse(str)
  }
  override fun Instant.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this.toString())
  }
}

object NullableInstantParceler : Parceler<Instant?> {
  override fun create(parcel: Parcel): Instant? {
    val str = parcel.readString() ?: return null
    return Instant.parse(str)
  }
  override fun Instant?.write(parcel: Parcel, flags: Int) {
    if (this == null) {
      parcel.writeValue(null)
    } else {
      parcel.writeString(this.toString())
    }
  }
}
object NullableRegionIdParceler : Parceler<RegionId?> {
  override fun create(parcel: Parcel): RegionId? {
    return runCatching {
      val id = parcel.readInt()
      return RegionId(id)
    }.getOrNull()
  }
  override fun RegionId?.write(parcel: Parcel, flags: Int) {
    if (this == null) {
      parcel.writeValue(null)
    } else {
      parcel.writeInt(this.value)
    }
  }
}
object InstantEpochSerializer : KSerializer<Instant> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

  override fun serialize(encoder: Encoder, value: Instant) =
    encoder.encodeLong(value.toEpochMilliseconds())

  override fun deserialize(decoder: Decoder): Instant =
    Instant.fromEpochMilliseconds(decoder.decodeLong())
}

object RegionIdSerializer : KSerializer<RegionId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.RegionId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: RegionId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): RegionId {
    return RegionId(decoder.decodeInt())
  }
}
object RemoteIdSerializer : KSerializer<RemoteId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.RemoteId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: RemoteId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): RemoteId {
    return RemoteId(decoder.decodeInt())
  }
}
object FileIdSerializer : KSerializer<FileId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.FileId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: FileId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): FileId {
    return FileId(decoder.decodeInt())
  }
}
object TaskIdSerializer : KSerializer<TaskId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.TaskId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: TaskId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): TaskId {
    return TaskId(decoder.decodeInt())
  }
}

object MobileNumberSerializer : KSerializer<MobileNumber> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.MobileNumber", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: MobileNumber) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): MobileNumber {
    return MobileNumber(decoder.decodeString())
  }
}

object NameSerializer : KSerializer<Name> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.Name", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Name) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): Name {
    return Name(decoder.decodeString())
  }
}
object PasswordSerializer : KSerializer<Password> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.Password", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Password) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): Password {
    return Password(decoder.decodeString())
  }
}
object MobileOperatorSerializer : KSerializer<MobileOperator> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.MobileOperator", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: MobileOperator) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): MobileOperator {
    return MobileOperator(decoder.decodeString())
  }
}

object DurationParceler : Parceler<Duration> {
  override fun create(parcel: Parcel): Duration {
    val str = parcel.readString()
    require(str != null)
    return Duration.parseIsoString(str)
  }
  override fun Duration.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this.toIsoString())
  }
}
//object NullableTaskIdParceler : Parceler<TaskId?> {
//  override fun create(parcel: Parcel): TaskId? {
//    val id = parcel.readInt() ?: return null
//    return TaskId(id)
//  }
//  override fun TaskId?.write(parcel: Parcel, flags: Int) {
//    if (this == null) {
//      parcel.writeValue(null)
//    } else {
//      parcel.writeInt(this.id)
//    }
//  }
//}
