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
package com.pico.spatial.sample.spatialmesh.ecs.components

import com.pico.spatial.core.ecs.Component
import com.pico.spatial.tracking.hmd.HMDTrackingProvider

/**
 * Component representing the shooter entity in the game.
 *
 * This component manages the state of the shooting mechanism, including whether the shooter is
 * ready to fire and tracking the current ammo.
 *
 * @param hmdTrackingProvider Optional HMD tracking provider for aiming
 */
class ShooterComponent(var hmdTrackingProvider: HMDTrackingProvider? = null) : Component() {
    /** Flag indicating if the shooter is ready to fire */
    var canFire: Boolean = false

    /** ID of the currently loaded ammo */
    var currentAmmoId: Int = -1

    // You need to override the clone method to make the custom component clonable
    override fun clone(): ShooterComponent {
        val clone = ShooterComponent(hmdTrackingProvider)
        clone.canFire = canFire
        clone.currentAmmoId = currentAmmoId
        return clone
    }
}
