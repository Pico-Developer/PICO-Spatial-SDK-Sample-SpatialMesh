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
package com.pico.spatial.sample.spatialmesh.platform

import android.app.Application
import com.pico.spatial.sample.spatialmesh.mainApp
import com.pico.spatial.ui.foundation.dsl.launch

/**
 * Application class for the Spatial Mesh Shooting Game.
 *
 * This class is the entry point of the Android application. It initializes the PICO Spatial UI
 * framework and launches the main application logic.
 */
class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Launch the main application using the PICO Spatial UI framework
        launch(::mainApp)
    }
}
