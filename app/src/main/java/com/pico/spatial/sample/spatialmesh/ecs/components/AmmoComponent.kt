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
import com.pico.spatial.core.ecs.Entity

/**
 * Component representing a projectile (ammo) in the game.
 *
 * This component stores all state information related to a projectile, including its identity,
 * collision state, and despawn timer.
 */
class AmmoComponent : Component() {
    /** Unique identifier for this ammo instance */
    var ammoId: Int = -1

    /** Reference to the spatial mesh entity that this ammo hit (if any) */
    var hitMesh: Entity? = null

    /** Flag indicating if this ammo is in the process of despawning */
    var isDespawning: Boolean = false

    /** Timer tracking how long until this ammo despawns (in seconds) */
    var despawnTimer: Float = 0f

    // You need to override the clone method to make the custom component clonable
    override fun clone(): AmmoComponent {
        val clone = AmmoComponent()
        clone.ammoId = ammoId
        clone.hitMesh = hitMesh
        clone.isDespawning = isDespawning
        clone.despawnTimer = despawnTimer
        return clone
    }
}
