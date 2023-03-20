package io.reitmaier.transcripttool.core.data

import android.content.Context
import android.content.SharedPreferences
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.retry.retry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.reitmaier.transcripttool.core.data.dispatchers.CoroutineDispatchers
import io.reitmaier.transcripttool.core.data.domain.ApiError
import io.reitmaier.transcripttool.core.data.domain.ApiResult
import io.reitmaier.transcripttool.core.data.domain.AuthResponse
import io.reitmaier.transcripttool.core.data.domain.CompleteTaskRequest
import io.reitmaier.transcripttool.core.data.domain.DatabaseError
import io.reitmaier.transcripttool.core.data.domain.DomainMessage
import io.reitmaier.transcripttool.core.data.domain.DomainResult
import io.reitmaier.transcripttool.core.data.domain.DuplicateUser
import io.reitmaier.transcripttool.core.data.domain.MobileNumber
import io.reitmaier.transcripttool.core.data.domain.NetworkError
import io.reitmaier.transcripttool.core.data.domain.NewTranscript
import io.reitmaier.transcripttool.core.data.domain.NoAccessToken
import io.reitmaier.transcripttool.core.data.domain.NoRefreshToken
import io.reitmaier.transcripttool.core.data.domain.PREFS_ACCESS_TOKEN
import io.reitmaier.transcripttool.core.data.domain.PREFS_MOBILE
import io.reitmaier.transcripttool.core.data.domain.PREFS_PASSWORD
import io.reitmaier.transcripttool.core.data.domain.PREFS_REFRESH_TOKEN
import io.reitmaier.transcripttool.core.data.domain.ParsingError
import io.reitmaier.transcripttool.core.data.domain.Password
import io.reitmaier.transcripttool.core.data.domain.ProvisionalTask
import io.reitmaier.transcripttool.core.data.domain.RegistrationRequest
import io.reitmaier.transcripttool.core.data.domain.RejectReason
import io.reitmaier.transcripttool.core.data.domain.RemoteId
import io.reitmaier.transcripttool.core.data.domain.ServerError
import io.reitmaier.transcripttool.core.data.domain.SubmittedTranscript
import io.reitmaier.transcripttool.core.data.domain.TaskDto
import io.reitmaier.transcripttool.core.data.domain.Unauthorized
import io.reitmaier.transcripttool.core.data.domain.UserInfo
import io.reitmaier.transcripttool.core.data.domain.UserInfoNotFound
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import logcat.logcat
import org.conscrypt.Conscrypt
import java.io.File
import java.security.Security
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscribeService @Inject constructor(
  private val prefs: SharedPreferences,
  private val context: Context,
  private val dispatcher: CoroutineDispatchers,
) {
  init {
    // Fix SSL Handshake Error in 7.0 (API 24)
    // https://stackoverflow.com/a/70862408
    Security.insertProviderAt(Conscrypt.newProvider(), 1)
  }
  private val clientNoAuth by lazy {
    HttpClient(OkHttp) {
      install(ContentNegotiation) {
        json(
          Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
          },
        )
      }
      expectSuccess = false
    }
  }
  private val client by lazy {
    HttpClient(OkHttp) {
      install(ContentNegotiation) {
        json(
          Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
          },
        )
      }
      expectSuccess = false
      install(Auth) {
        bearer {
          // Load tokens ...
          loadTokens {
            logcat { "Loading JWT Token" }
            getBearerTokens()
          }
          refreshTokens { // this: RefreshTokensParams
            this.oldTokens.let {
              if (it != null) {
                refresh(it).get()
              } else {
                null
              }
            }
          }
        }
      }
    }
  }

  data class RemoteTask(
    val remoteId: RemoteId,
    val url: String,
  )

  fun getUserInfo(): DomainResult<UserInfo> =
    binding {
      val mobile = prefs.getString(PREFS_MOBILE, null).toResultOr { UserInfoNotFound }.bind()
      val password = prefs.getString(PREFS_PASSWORD, null).toResultOr { UserInfoNotFound }.bind()
      UserInfo(MobileNumber(mobile), Password(password))
    }

  private fun saveAccessTokens(tokens: BearerTokens) {
    prefs.edit()
      .putString(PREFS_ACCESS_TOKEN, tokens.accessToken)
      .putString(PREFS_REFRESH_TOKEN, tokens.refreshToken)
      .apply()
  }
  fun saveUserInfo(mobile: MobileNumber, password: Password) {
    prefs.edit()
      .putString(PREFS_MOBILE, mobile.value)
      .putString(PREFS_PASSWORD, password.value)
      .apply()
  }
  private fun getBearerTokens(): BearerTokens? {
    val savedTokens = binding<BearerTokens, DomainMessage> {
      val accessToken = prefs.getString(PREFS_ACCESS_TOKEN, null).toResultOr { NoAccessToken }.bind()
      val refreshToken = prefs.getString(PREFS_REFRESH_TOKEN, null).toResultOr { NoRefreshToken }.bind()
      BearerTokens(accessToken, refreshToken)
    }.onFailure { logcat { "Failed to load BearerTokens, because $it" } }
      .onSuccess { logcat { "Loaded saved BearerTokens" } }

    // Return Saved tokens
    return savedTokens.get()
//      ?:
//    // Otherwise try login
//    login(userInfo)
//      .onFailure { logcat{ "Failed to load BearerToken, because $it"} }
//      .onSuccess { logcat{ "Load BearerToken through login"} }
//      .get()
  }

  suspend fun downloadToFile(url: String, file: File): Result<File, ApiError> =
    // Request File from Server
    withContext(dispatcher.io) {
      runCatching {
        val response: HttpResponse = client.get(url) {
          onDownload { bytesSentTotal, contentLength ->
            logcat { "Received $bytesSentTotal bytes from $contentLength" }
          }
        }
        if (response.status.isSuccess()) {
          // Save response to file
          val responseBody: ByteArray = response.body()
          file.writeBytes(responseBody)
          file
        } else {
          throw(Throwable("Request unsuccessful: $response"))
        }
      }.mapError { NetworkError(it) }
    }

  private suspend fun refresh(tokens: BearerTokens): Result<BearerTokens, ApiError> =
    withContext(dispatcher.io) {
      logcat { "Refreshing ${tokens.refreshToken}" }
      // Make the request
      runCatching {
        client.post("$BASE_URL/refresh") {
          contentType(ContentType.Application.Json)
          setBody(tokens.refreshToken)
        }.also { logcat { "Made refresh request: ${tokens.refreshToken}" } }
      }.mapError { NetworkError(it).also { logcat { "Network Error" } } } // Catch Network Error
        .andThen { response ->
          // Surface unauthorized request
          if (response.status == HttpStatusCode.Unauthorized) {
            return@andThen Err(Unauthorized).also { logcat { "Refresh was unauthorized (failed)" } }
          }
          // Parse response
          runCatching {
            response.body<AuthResponse>().also { logcat { "Parsing auth response" } }
          }.mapError {
            logcat { it.toString() }
            ServerError.also { logcat { "Failed to parse auth response" } }
          } // Error here is server's fault
        }
    }.map { authResponse ->
      // Save and return tokens
      val bearerTokens = BearerTokens(
        accessToken = authResponse.accessToken,
        refreshToken = authResponse.refreshToken,
      )
      saveAccessTokens(bearerTokens)
      bearerTokens
    }

  private suspend fun login(userInfo: UserInfo): Result<BearerTokens, ApiError> =
    withContext(dispatcher.io) {
      logcat { "Login $userInfo" }
      // Make the request
      runCatching {
        clientNoAuth.post("$BASE_URL/login") {
          contentType(ContentType.Application.Json)
          setBody(userInfo)
        }.also { logcat { "Made Login request: $userInfo" } }
      }.mapError { NetworkError(it).also { logcat { "Network Error" } } } // Catch Network Error
        .andThen { response ->
          logcat { response.bodyAsText() }
          // Surface unauthorized request
          if (response.status == HttpStatusCode.Unauthorized) {
            return@andThen Err(Unauthorized).also { logcat { "Login was unauthorized (failed)" } }
          }
          // Parse response
          runCatching {
            response.body<AuthResponse>().also { logcat { "Parsing login response" } }
          }.mapError { ServerError.also { logcat { "Failed to parse login response" } } } // Error here is server's fault
        }
    }.map { authResponse ->
      // Save and return tokens
      val bearerTokens = BearerTokens(
        accessToken = authResponse.accessToken,
        refreshToken = authResponse.refreshToken,
      )
      saveAccessTokens(bearerTokens)
      bearerTokens
    }
  suspend fun registerAccount(registrationRequest: RegistrationRequest): Result<UserInfo, ApiError> =
    withContext(dispatcher.io) {
      runCatching {
        clientNoAuth.post("$BASE_URL/register") {
          contentType(ContentType.Application.Json)
          setBody(registrationRequest)
        }
      }.mapError { NetworkError(it) }
        .andThen { response ->
          when (response.status) {
            HttpStatusCode.Created -> {
              val userInfo = UserInfo(registrationRequest.mobile, registrationRequest.password)
              saveUserInfo(userInfo.mobile, userInfo.password)
              Ok(userInfo)
            }
            HttpStatusCode.Conflict -> {
              Err(DuplicateUser)
            }
            else -> {
              Err(NetworkError(Throwable("Request unsuccessful: $response")))
            }
          }
        }.andThen { user ->
          login(user).map {
            user
          }
        }
    }

  suspend fun submitTask(provisionalTask: ProvisionalTask, durationMs: Long): ApiResult<RemoteTask> =
    withContext(dispatcher.io) {
      logcat { "Submitting task to API Server: $provisionalTask" }
      retry {
        runCatching {
          // Ensures refresh token is current as form submission
          // fails if refresh token is out of date
          client.post("$BASE_URL/ping")
          val audioData = context.filesDir.resolve(provisionalTask.audio_path).readBytes()
          // Submit the Task
          client.submitFormWithBinaryData(
            url = "$BASE_URL/tasks",
            formData = formData {
              append("length", durationMs.toString())
              append(
                "file",
                audioData,
                Headers.build {
                  append(HttpHeaders.ContentDisposition, "filename=${provisionalTask.displayName}")
                },
              )
            },
          ) {
            // Track progress
            onUpload { bytesSentTotal, contentLength ->
              logcat { "Sent $bytesSentTotal bytes from $contentLength" }
            }
          }
        }.mapError { NetworkError(it) } // Failure is likely due to network
      }
        .andThen { response ->
          logcat { "Upload response: $response" }
          if (response.status == HttpStatusCode.Created) {
            val id = response.body<String>().toIntOrNull() ?: return@andThen Err(ServerError)
            Ok(RemoteTask(RemoteId(id), id.toString()))
          } else {
            // TODO Figure out what went wrong (e.g. File Exists)
            Err(ServerError)
          }
        }
    }

  internal suspend fun refreshTasks(): DomainResult<List<TaskDto>> =
    withContext(dispatcher.io) {
      logcat { "Refreshing Tasks" }
      runCatching {
        client.get("$BASE_URL/tasks") {
        }
      }.mapError { NetworkError(it) }
        .andThen { response ->
          if (response.status == HttpStatusCode.OK) {
            runCatching {
              response.body<List<TaskDto>>().also { logcat { it.toString() } }
            }.mapError { error -> ParsingError.also { logcat { error.toString() } } }
          } else {
            logcat { "Refresh Failed " }
            Err(ServerError)
          }
        }
    }

  suspend fun uploadTranscripts(remoteId: RemoteId, newTranscripts: List<NewTranscript>): DomainResult<SubmittedTranscript> {
    return withContext(dispatcher.io) {
      runCatching {
        client.post("$BASE_URL/tasks/${remoteId.value}/transcripts") {
          contentType(ContentType.Application.Json)
          setBody(newTranscripts)
        }
      }.mapError { NetworkError(it) }
        .andThen { response ->
          if (!response.status.isSuccess()) {
            return@andThen Err(ServerError)
          }
          val latest = newTranscripts.maxByOrNull { it.updatedAt } ?: return@andThen Err(DatabaseError)
          Ok(SubmittedTranscript(latest.transcript))
        }
    }
  }

  suspend fun logError(message: String): Result<HttpResponse, NetworkError> {
    return runCatching {
      clientNoAuth.post("$BASE_URL/error") {
        setBody(message)
      }.also { logcat { "Submitted Log to server" } }
    }.mapError {
      logcat { "Network Error: $it" }
      NetworkError(it)
    } // Catch Network Error
  }

  internal suspend fun completeTask(remoteId: RemoteId, completeTaskRequest: CompleteTaskRequest): DomainResult<Unit> {
    return withContext(dispatcher.io) {
      runCatching {
        client.post("$BASE_URL/tasks/${remoteId.value}/complete") {
          contentType(ContentType.Application.Json)
          setBody(completeTaskRequest)
        }
      }.mapError { NetworkError(it) }
        .andThen { response ->
          runCatching {
            if (response.status == HttpStatusCode.OK) {
              return@runCatching
            } else {
              throw Throwable("Reject Request Unsuccessful: $response")
            }
          }.mapError { NetworkError(it) }
        }
    }
  }

  internal suspend fun rejectTask(remoteId: RemoteId, rejectReason: RejectReason?): DomainResult<Unit> {
    return withContext(dispatcher.io) {
      runCatching {
        client.post("$BASE_URL/tasks/${remoteId.value}/reject") {
          contentType(ContentType.Application.Json)
          setBody(rejectReason)
        }
      }.mapError { NetworkError(it) }
        .andThen { response ->
          runCatching {
            if (response.status == HttpStatusCode.OK) {
              return@runCatching
            } else {
              throw Throwable("Reject Request Unsuccessful: $response")
            }
          }.mapError { NetworkError(it) }
        }
    }
  }

  companion object {
    const val BASE_URL: String = "https://transcriptapi.reitmaier.xyz"
//    const val BASE_URL: String = "https://sidebyside.reitmaier.xyz"
//    const val BASE_URL: String = "https://transcribeapi.reitmaier.xyz"
  }
}
