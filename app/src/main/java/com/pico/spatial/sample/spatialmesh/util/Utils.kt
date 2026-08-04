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
package com.pico.spatial.sample.spatialmesh.util

import com.pico.spatial.core.math.Color4
import kotlin.random.Random

/**
 * Simple timer utility for managing time-based events.
 *
 * This class is used to track elapsed time and trigger events at specific intervals. It's commonly
 * used for cooldowns, spawn timers, and despawning logic.
 *
 * @param interval The time interval (in seconds) at which to trigger events
 */
class Timer(private val interval: Float) {
    private var elapsed = 0f

    /**
     * Updates the timer with the elapsed time since the last frame.
     *
     * @param deltaTime Time elapsed since last frame (in seconds)
     * @return True if the interval has been reached and the timer is reset
     */
    fun tryTrigger(deltaTime: Float): Boolean {
        elapsed += deltaTime
        return if (elapsed >= interval) {
            elapsed = 0f
            true
        } else {
            false
        }
    }

    /** Resets the timer to zero */
    fun reset() {
        elapsed = 0f
    }
}

/**
 * Generates a random RGBA color.
 *
 * This function is used to give each spatial mesh a unique color when it's first scanned. The alpha
 * value is fixed at 1.0 for full opacity.
 *
 * @return Random color with full opacity
 */
fun randomColor4(): Color4 {
    val maxAlpha = 1f
    val minAlpha = 1f
    return Color4(
        Random.nextFloat(), // Red component (0-1)
        Random.nextFloat(), // Green component (0-1)
        Random.nextFloat(), // Blue component (0-1)
        Random.nextFloat() * (maxAlpha - minAlpha) + minAlpha // Alpha (fixed at 1.0)
    )
}
