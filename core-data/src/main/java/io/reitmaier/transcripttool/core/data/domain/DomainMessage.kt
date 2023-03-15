package io.reitmaier.transcripttool.core.data.domain
import android.os.Parcelable
import com.github.michaelbull.result.*
import kotlinx.parcelize.Parcelize

typealias ApiResult<T> = Result<T, ApiError>
typealias DbResult<T> = Result<T, DbError>
typealias DomainResult<T> = Result<T, DomainMessage>

/**
 * All possible things that can happen in the use-cases
 */
sealed class DomainMessage : Parcelable

@Parcelize
object UiError : DomainMessage()

@Parcelize
object LoadingError : DomainMessage()

@Parcelize
object IOError : DomainMessage()

@Parcelize
object UserInfoNotFound : DomainMessage()

@Parcelize
object NoAccessToken : DomainMessage()

@Parcelize
object NoRefreshToken : DomainMessage()

@Parcelize
object InvalidMimeType : DomainMessage()

sealed class ApiError : DomainMessage()
sealed class DbError : DomainMessage()

/* internal errors */

@Parcelize
object DatabaseError : DbError()

@Parcelize
object ParsingError : ApiError()

@Parcelize
data class NetworkError(val throwable: Throwable) : ApiError()

@Parcelize
object Unauthorized: ApiError()

@Parcelize
object DuplicateUser: ApiError()

@Parcelize
object ServerError: ApiError()
