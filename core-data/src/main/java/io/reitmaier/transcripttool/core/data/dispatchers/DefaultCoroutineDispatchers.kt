package io.reitmaier.transcripttool.core.data.dispatchers

import io.reitmaier.transcripttool.core.data.dispatchers.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class DefaultCoroutineDispatchers @Inject constructor() : CoroutineDispatchers {
  override val main: CoroutineDispatcher = Dispatchers.Main
  override val io: CoroutineDispatcher = Dispatchers.IO
}