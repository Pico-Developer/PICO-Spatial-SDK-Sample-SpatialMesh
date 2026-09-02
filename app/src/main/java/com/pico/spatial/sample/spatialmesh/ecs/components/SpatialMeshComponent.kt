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

/**
 * Component representing a spatial mesh entity in the game.
 *
 * This component tracks whether a spatial mesh has been hit by a projectile, which is used for
 * score calculation and visual feedback.
 */
class SpatialMeshComponent : Component() {
    /** Flag indicating if this spatial mesh has been hit by a projectile */
    var isHit: Boolean = false

    // You need to override the clone method to make the custom component clonable
    override fun clone(): SpatialMeshComponent {
        val clone = SpatialMeshComponent()
        clone.isHit = isHit
        return clone
    }
}
