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
package com.pico.spatial.sample.spatialmesh

import com.pico.spatial.sample.spatialmesh.data.AudioRepository
import com.pico.spatial.sample.spatialmesh.data.assetBundle
import com.pico.spatial.sample.spatialmesh.ui.MainStage
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Entry point of the Spatial Mesh Shooting Game sample.
 *
 * This function initializes the application by:
 * 1. Setting up the main UI stage
 * 2. Loading and preparing audio resources in a coroutine
 * 3. Loading the asset bundle containing 3D models and other resources
 *
 * The sample demonstrates how to use PICO Spatial SDK features including:
 * - Environment mesh scanning and visualization
 * - Physics simulation with collision detection
 * - Spatial audio with emitter pooling
 * - ECS (Entity Component System) architecture
 * - HMD tracking for aiming and shooting
 *
 * @param scope The SpatialAppScope provided by the PICO Spatial UI framework
 */
fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        // Create the main UI stage
        DefaultStage { MainStage() }

        // Initialize resources asynchronously to avoid blocking the main thread
        MainScope().launch {
            assetBundle.await() // Wait for asset bundle to load
            AudioRepository.loadAudio() // Load all audio resources
            AudioRepository.prepareAudio() // Pre-warm audio controllers for immediate playback
        }
    }
