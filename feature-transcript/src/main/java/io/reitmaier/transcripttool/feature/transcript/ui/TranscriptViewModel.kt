/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reitmaier.transcripttool.feature.transcript.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.reitmaier.transcripttool.core.data.TranscriptRepository
import io.reitmaier.transcripttool.feature.transcript.ui.TranscriptUiState.Error
import io.reitmaier.transcripttool.feature.transcript.ui.TranscriptUiState.Loading
import io.reitmaier.transcripttool.feature.transcript.ui.TranscriptUiState.Success
import javax.inject.Inject

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptRepository: TranscriptRepository
) : ViewModel() {

    val uiState: StateFlow<TranscriptUiState> = transcriptRepository
        .transcripts.map { Success(data = it) }
        .catch { Error(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun addTranscript(name: String) {
        viewModelScope.launch {
            transcriptRepository.add(name)
        }
    }
}

sealed interface TranscriptUiState {
    object Loading : TranscriptUiState
    data class Error(val throwable: Throwable) : TranscriptUiState
    data class Success(val data: List<String>) : TranscriptUiState
}
