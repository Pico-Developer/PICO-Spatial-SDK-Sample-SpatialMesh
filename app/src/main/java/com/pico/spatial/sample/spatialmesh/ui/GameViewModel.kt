/*
 * Copyright 2025 - 2026 PICO. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pico.spatial.sample.spatialmesh.ui

import androidx.lifecycle.ViewModel
import com.pico.spatial.sample.spatialmesh.data.GlobalConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing game UI state.
 *
 * This ViewModel handles all UI-related state, including:
 * - Score tracking
 * - Tutorial tip visibility
 * - Emulator detection
 *
 * It follows the Android architecture guidelines for ViewModels, using StateFlow to expose
 * read-only state to UI components.
 */
class GameViewModel : ViewModel() {
    // Private mutable state for score (only updated internally)
    private val _score = MutableStateFlow(0)

    /** Public read-only state for current score */
    val score: StateFlow<Int> = _score.asStateFlow()

    // Private mutable state for tip visibility (only updated internally)
    private val _showTip = MutableStateFlow(true)

    /** Public read-only state indicating if the tutorial tip should be shown */
    val showTip: StateFlow<Boolean> = _showTip.asStateFlow()

    /** Flag indicating if app is running on a spatial emulator */
    val isSpatialEmulator: Boolean = GlobalConfig.isSpatialEmulator

    /** Increment the current score by 1 */
    fun incrementScore() {
        _score.value += 1
    }

    /** Hide the tutorial tip */
    fun hideTip() {
        _showTip.value = false
    }

    /** Reset the game state */
    fun reset() {
        _score.value = 0
        _showTip.value = true
    }
}
