package io.reitmaier.transcripttool.register

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.reitmaier.transcripttool.core.data.domain.*
import io.reitmaier.transcripttool.core.data.util.IntentDispatcher
import io.reitmaier.transcripttool.core.ui.components.TranscriptToolTopAppBar
import io.reitmaier.transcripttool.feature.register.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalLayoutApi
@ExperimentalComposeUiApi
@ExperimentalTime
@FlowPreview
@Composable
fun RegisterScreen(modifier: Modifier = Modifier,
                   onRegistration: () -> Unit) {
  val viewModel = hiltViewModel<RegisterViewModel>()
  val state = viewModel.container.stateFlow.collectAsState().value
  val processIntent = viewModel.processIntent
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  LaunchedEffect(viewModel, snackbarHostState) {
    launch {
      viewModel.container.sideEffectFlow.collect {
        when (it) {
          is RegisterSideEffect.RegistrationFailed ->
            when(it.reason) {
              is DuplicateUser ->
                snackbarHostState.showSnackbar(
                  message = "A user has already registered with this mobile number.\n\nPlease try a different one or get in touch with the research team.",
                  duration = SnackbarDuration.Indefinite,
                  withDismissAction = true,
                )
              is NetworkError,
              ParsingError,
              ServerError,
              Unauthorized ->
                snackbarHostState.showSnackbar(
                  message = "Registration failed due to a network error. Please try again later: ${it}.",
                  duration = SnackbarDuration.Long,
                  withDismissAction = true,
                )
            }
          RegisterSideEffect.RegistrationSucceeded -> {
            Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
            onRegistration()
          }
        }

      }
    }
  }

  Scaffold(
    snackbarHost = {
      SnackbarHost(
        modifier = Modifier
          .navigationBarsPadding()
          .imePadding(),
        hostState = snackbarHostState,
      )
    },
    modifier = Modifier.imePadding(),
    topBar = {
      RegisterAppBar(
        state = state,
        processIntent = processIntent,
      )
    },
    bottomBar = {},
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { scaffoldPadding ->
    Box(
      Modifier
        .fillMaxSize()
        .padding(scaffoldPadding)
        .consumedWindowInsets(scaffoldPadding)
        .systemBarsPadding()
    ) {

      when(state) {
        is RegisterTaskState.UserInputting -> {
          RegisterContents(modifier, state, processIntent)
        }
      }

    }
  }
}

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
private fun RegisterContents(
  modifier: Modifier = Modifier,
  state: RegisterTaskState.UserInputting,
  processIntent: IntentDispatcher<ViewIntent>
) {

  val scrollState = rememberScrollState()
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(8.dp),
//    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // initialize focus reference to be able to request focus programmatically
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFocusRequester = FocusRequester()
    val mobileFocusRequester = FocusRequester()
    val pinFocusRequester = FocusRequester()
    val networkFocusRequester = FocusRequester()
    DisposableEffect(Unit) {
      nameFocusRequester.requestFocus()
      onDispose { }
    }
    val nameError = state.validationErrors.contains(ValidationError.NAME_MISSING)
    OutlinedTextField(
      label = {Text("Name")},
      value = state.name.value,
      onValueChange = { processIntent(ViewIntent.UpdateName(Name(it))) },
      isError = nameError,
      trailingIcon = {
        if (nameError)
          Icon(Icons.Filled.Warning,"error", tint = MaterialTheme.colorScheme.error)
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Text,
      ),
      keyboardActions = KeyboardActions(
        onNext = { mobileFocusRequester.requestFocus() }
      ),
      modifier = Modifier
        .focusRequester(nameFocusRequester)
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 6.dp)
    )
    Text(
      text = if(nameError) ValidationError.NAME_MISSING.message else "",
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(start = 16.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    val phoneError = state.validationErrors.contains(ValidationError.PHONE_NUMBER_INVALID)
    OutlinedTextField(
      label = {Text("Mobile Number")},
      value = state.mobile.value,
      onValueChange = { processIntent(ViewIntent.UpdateMobile(MobileNumber(it))) },
      isError = phoneError,
      trailingIcon = {
        if (phoneError)
          Icon(Icons.Filled.Warning,"error", tint = MaterialTheme.colorScheme.error)
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Number,
      ),
      keyboardActions = KeyboardActions(
        onNext = { pinFocusRequester.requestFocus() }
      ),
      modifier = Modifier
        .focusRequester(mobileFocusRequester)
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 6.dp)
    )
    Text(
      text = if(phoneError) ValidationError.PHONE_NUMBER_INVALID.message else "",
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(start = 16.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))

    val pinError = state.validationErrors.contains(ValidationError.PIN_INCORRECT)
    OutlinedTextField(
      label = {Text("PIN")},
      value = state.pin,
      onValueChange = { processIntent(ViewIntent.UpdatePin(it)) },
      isError = pinError,
      trailingIcon = {
        if (pinError)
          Icon(Icons.Filled.Warning,"error", tint = MaterialTheme.colorScheme.error)
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Text,
      ),
      keyboardActions = KeyboardActions( onNext = {
        pinFocusRequester.requestFocus()
        keyboardController?.hide()}
      ),
      modifier = Modifier
        .focusRequester(pinFocusRequester)
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 6.dp)
    )
    Text(
      text = if(pinError) ValidationError.PIN_INCORRECT.message else "",
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(start = 16.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Select Mobile Network:",
      color = MaterialTheme.colorScheme.onBackground,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(start = 8.dp)
    )
    var expanded by remember{ mutableStateOf(false)}
    Box(modifier = Modifier.fillMaxWidth()) {
      Button(
        modifier = Modifier
          .focusRequester(networkFocusRequester)
          .fillMaxWidth(),
        onClick = { expanded = true }) {
        Row(modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          if(state.network == MobileNetwork.EMPTY) {
            Text("Mobile Network")
          } else {
            Text(state.network.networkName)
          }
          Icon(Icons.Default.ArrowDropDown, "")

        }
      }
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth()
      ) {
        MobileNetwork.values().filter { it != MobileNetwork.EMPTY }.forEach { item ->
          DropdownMenuItem(
            onClick = {
              processIntent(ViewIntent.UpdateNetwork(item))
              expanded = false
            },
            text = {
              Text(text = item.networkName)
            }
          )
        }
      }
    }

    val networkSelectionError = state.validationErrors.contains(ValidationError.NO_NETWORK_SELECTED)
    Text(
      text = if(networkSelectionError) ValidationError.NO_NETWORK_SELECTED.message else "",
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(start = 16.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))
    if(state.isRegistering) {
      CircularProgressIndicator(
        Modifier.align(alignment = Alignment.CenterHorizontally)
//        horiz
      )
    }
  }
}

@ExperimentalMaterial3Api
@Composable
private fun RegisterAppBar(
  state: RegisterTaskState,
  processIntent: IntentDispatcher<ViewIntent>
){
  val title = when(state) {
    is RegisterTaskState.UserInputting -> "Register"
  }
  CenterAlignedTopAppBar(
    title = { Text("Register") },
    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    actions = {
      when(state) {
        is RegisterTaskState.UserInputting -> {
          IconButton(
            icon = Icons.Outlined.Check,
            contentDescription = "Register",
            color = if(isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
          ) {
            // Upload Action
            processIntent(ViewIntent.Register)
          }
        }
      }
    },
//    navigationIcon = {},
  )
}
@Composable
private fun IconButton(
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.onBackground,
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  IconButton(
    enabled = enabled,
    modifier = modifier.then(Modifier.size(50.dp)),
    onClick = { onClick() }) {
    Icon(
      icon,
      contentDescription,
      tint = if(enabled) color else LightGray,
    )
  }
}

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Preview(
  widthDp = 300,
)
@Composable
fun RegisterScreenPreview() {
  RegisterContents(
    processIntent = {},
    state = RegisterTaskState.UserInputting(
      name = Name("Thomas"),
      mobile = MobileNumber("07492172131"),
      pin = "Langa2021",
//      network = MobileNetwork.MTN,
      network = MobileNetwork.EMPTY,
      isRegistering = true,
      validationErrors = setOf(
//        ValidationError.NAME_MISSING,
        ValidationError.PHONE_NUMBER_INVALID,
        ValidationError.PIN_INCORRECT,
        ValidationError.NO_NETWORK_SELECTED
      )
    ),
  )
}
