package io.reitmaier.transcripttool.register

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reitmaier.transcripttool.core.data.TranscriptRepo
import io.reitmaier.transcripttool.core.data.domain.*
import io.reitmaier.transcripttool.core.data.parcelizers.*
import io.reitmaier.transcripttool.core.data.util.IntentDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import java.util.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

internal sealed class ViewIntent {
  data class UpdateName(val name: Name) : ViewIntent()
  data class UpdateMobile(val mobile: MobileNumber) : ViewIntent()
  data class UpdatePin(val pin: String) : ViewIntent()
  data class UpdateNetwork(val network: MobileNetwork) : ViewIntent()
  object Register : ViewIntent()
}

sealed class RegisterSideEffect {
  data class RegistrationFailed(val reason: ApiError) : RegisterSideEffect()
  object RegistrationSucceeded : RegisterSideEffect()
}
enum class ValidationError(val message: String) {
  NAME_MISSING("Your name is required to register"),
  PHONE_NUMBER_INVALID("A valid phone number is required"),
  PIN_INCORRECT("Please enter the PIN that was given to you."),
  NO_NETWORK_SELECTED("We need to know the mobile network of the phone number"),
}

enum class MobileNetwork(val networkName: String) {
  EMPTY(""),
  MTN("MTN"),
  CELL_C("CELL C"),
  VODACOM("Vodacom"),
  TELKOM("TELKOM"),
}
const val PIN: String = "LANGA2022"
sealed class RegisterTaskState : Parcelable {

//  @Parcelize
//  object Initial: RegisterTaskState(), Parcelable


  @Parcelize
  data class UserInputting(
    val name: @WriteWith<NameParceler> Name = Name(""),
    val mobile: @WriteWith<MobileNumberParceler> MobileNumber = MobileNumber(""),
    val pin: String = "",
    val network: MobileNetwork = MobileNetwork.EMPTY,
    val nameFirstChange: Boolean = false,
    val phoneNumberFirstChange: Boolean = false,
    val validationErrors: Set<ValidationError> = emptySet(),
    val isRegistering: Boolean = false
  ): RegisterTaskState(), Parcelable {
    val canRegister: Boolean
      get() = isMobileNumberValid(mobile) && isNetworkValid(network) && isNameValid(name) && isPinValid(pin)
  }

//  @Parcelize
//  data class Registering(val name: String, val phoneNumber: String, val network: MobileNetwork): RegisterTaskState(), Parcelable

//  @Parcelize
//  data class Error(val error: DomainMessage): RegisterTaskState(), Parcelable
}

private fun isMobileNumberValid(mobile: MobileNumber): Boolean = mobile.value.length == 10
private fun isNetworkValid(network: MobileNetwork): Boolean = network != MobileNetwork.EMPTY
private fun isNameValid(name: Name): Boolean = name.value.length >= 2
private fun isPinValid(pin: String): Boolean = pin.uppercase(Locale.getDefault()) == PIN

@ExperimentalTime
@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
class RegisterViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val repo: TranscriptRepo,
  ) : ViewModel(), ContainerHost<RegisterTaskState, RegisterSideEffect> {
  private val _intentFlow = MutableSharedFlow<ViewIntent>(extraBufferCapacity = 64)
  internal val processIntent: IntentDispatcher<ViewIntent> = { _intentFlow.tryEmit(it) }

  override val container : Container<RegisterTaskState, RegisterSideEffect> =
    container(
      RegisterTaskState.UserInputting(
//      name = Name("TESTING"),
        name = Name(""),
//      mobile = MobileNumber("000000001"),
        network = MobileNetwork.EMPTY,
      ), savedStateHandle) {}



  init {
    // Handle ViewIntents
    _intentFlow
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
      // ignore Updates that don't change anything
//      .distinctUntilChanged { old, new ->
//        old is ViewIntent.Update &&
//          new is ViewIntent.Update &&
//          old.transcript == new.transcript
//      }
      // Update inputtedTranscript straightaway
      .onEach { viewIntent ->
        processViewIntent(viewIntent)
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0L
      )

    // Also debounce and persist transcript updates to repo
    _intentFlow
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
      .filter {
        when(it) {
          ViewIntent.Register -> false
          is ViewIntent.UpdateName -> true
          is ViewIntent.UpdateNetwork -> true
          is ViewIntent.UpdateMobile -> true
          is ViewIntent.UpdatePin -> true
        }
      }
      .distinctUntilChanged()
      .debounce(500.milliseconds)
      .onEach {
        when(it) {
          ViewIntent.Register -> Unit
          is ViewIntent.UpdateName -> updateNameError(it)
          is ViewIntent.UpdateNetwork -> updateNetworkError(it)
          is ViewIntent.UpdateMobile -> updatePhoneError(it)
          is ViewIntent.UpdatePin -> updatePinError(it)
        }
      }
//      .onEach {
//        persistTranscript(it.transcript)
//      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0L
      )

  }

  private fun processViewIntent(viewIntent: ViewIntent) =
    intent {
      state.let { s ->
        when(s) {
          is RegisterTaskState.UserInputting -> when(viewIntent) {
            ViewIntent.Register -> {
              if(!s.canRegister) {
                reduce { s.copy(validationErrors = validateAllInputs(s.name,s.mobile,s.network,s.pin)) }
                return@intent
              }
              reduce { s.copy(isRegistering = true) }
              repo.registerUser(s.name,s.mobile,MobileOperator(s.network.networkName))
                .fold(
                  success = {
                    postSideEffect(RegisterSideEffect.RegistrationSucceeded)
                  },
                  failure = {
                    postSideEffect(RegisterSideEffect.RegistrationFailed(it))
                  }
                )
              reduce { s.copy(isRegistering = false) }
            }
            is ViewIntent.UpdateName -> reduce { s.copy(name = viewIntent.name) }
            is ViewIntent.UpdateNetwork -> reduce { s.copy(network = viewIntent.network) }
            is ViewIntent.UpdateMobile -> reduce { s.copy(mobile = viewIntent.mobile) }
            is ViewIntent.UpdatePin -> reduce { s.copy(pin = viewIntent.pin) }
          }
        }
      }
    }

  private fun validateAllInputs(name: Name, mobile: MobileNumber, mobileNetwork: MobileNetwork, pin: String) : Set<ValidationError> {
    val errors = mutableSetOf<ValidationError>()
    if(!isMobileNumberValid(mobile)) errors.add(ValidationError.PHONE_NUMBER_INVALID)
    if(!isNameValid(name)) errors.add(ValidationError.NAME_MISSING)
    if(!isNetworkValid(mobileNetwork)) errors.add(ValidationError.NO_NETWORK_SELECTED)
    if(!isPinValid(pin)) errors.add(ValidationError.PIN_INCORRECT)
    return errors
  }

  private fun updatePinError(updatePin: ViewIntent.UpdatePin) =
    intent {
      state.let { s ->
        when(s) {
          is RegisterTaskState.UserInputting -> {
            reduce {
              val errors = if(isPinValid(s.pin)) {
                s.validationErrors - ValidationError.PIN_INCORRECT
              } else {
                s.validationErrors + ValidationError.PIN_INCORRECT
              }
              s.copy(validationErrors = errors)
            }
          }
        }
      }
    }
  private fun updatePhoneError(updateMobile: ViewIntent.UpdateMobile) =
    intent {
      state.let { s ->
        when(s) {
          is RegisterTaskState.UserInputting -> {
            reduce {
              val errors = if(isMobileNumberValid(s.mobile)) {
                s.validationErrors - ValidationError.PHONE_NUMBER_INVALID
              } else {
                s.validationErrors + ValidationError.PHONE_NUMBER_INVALID
              }
              s.copy(validationErrors = errors)
            }
          }
        }
      }
    }


  private fun updateNetworkError(updateNetwork: ViewIntent.UpdateNetwork) =
    intent {
      state.let { s ->
          when(s) {
            is RegisterTaskState.UserInputting -> {
              reduce {
                val errors = if(isNetworkValid(s.network)) {
                  s.validationErrors - ValidationError.NO_NETWORK_SELECTED
                } else {
                  s.validationErrors + ValidationError.NO_NETWORK_SELECTED
                }
                s.copy(validationErrors = errors)
              }
            }
          }
        }
    }

  private fun updateNameError(updateName: ViewIntent.UpdateName) =
    intent {
      state.let { s ->
        when(s) {
          is RegisterTaskState.UserInputting -> {
            reduce {
              val errors = if(isNameValid(s.name)) {
                s.validationErrors - ValidationError.NAME_MISSING
              } else {
                s.validationErrors + ValidationError.NAME_MISSING
              }
              s.copy(validationErrors = errors)
            }
          }
        }
      }
    }
}
