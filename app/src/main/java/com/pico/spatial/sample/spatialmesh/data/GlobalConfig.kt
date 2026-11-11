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
package com.pico.spatial.sample.spatialmesh.data

import android.os.Build
import com.pico.spatial.core.platform.SpatialBuild

/** Global configuration settings for the application. */
object GlobalConfig {
    /** Global tag for Android logcat. Keep stable for filtering. */
    const val LOG_TAG: String = "SpatialMeshSample"

    /**
     * Flag to indicate if the application is running on an PICO emulator for PICO OS 6. This can be
     * used to switch interaction methods or show/hide specific UI elements.
     *
     * WARNING: This flag is provided for convenience and debugging purposes only. We do not
     * guarantee future maintenance or accuracy of this check. Developers should use this with
     * caution and at their own responsibility.
     *
     * Usage examples:
     * - Logic/Systems: if (GlobalConfig.isSpatialEmulator) { /* emulator specific logic */ }
     * - UI/ViewModels: val isEmulator = GlobalConfig.isSpatialEmulator
     */
    val isSpatialEmulator: Boolean by lazy {
        if (!SpatialBuild.isSpatialPlatform()) {
            return@lazy false
        }
        Build.FINGERPRINT.contains("emulator") || Build.PRODUCT.contains("emulator")
    }
}
