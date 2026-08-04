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
package com.pico.spatial.sample.spatialmesh.ecs.entities

import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.simulation.CollisionFilter
import com.pico.spatial.core.ecs.simulation.CollisionGroup
import com.pico.spatial.core.ecs.simulation.CollisionResponseMode
import com.pico.spatial.sample.spatialmesh.manager.GROUP_INTERACTION

/**
 * Entity that acts as a receiver for spatial tap gestures.
 *
 * This entity is used to detect when the user performs a spatial tap gesture in the world. It has a
 * large spherical collision shape to ensure it covers a wide area for interaction.
 *
 * The interaction receiver is only used on physical devices (not emulators) where spatial gesture
 * detection is supported.
 */
class InteractionReceiver : Entity() {
    init {
        setName("InteractionReceiver")

        // Mark this entity as interactable
        components.set(InteractableComponent())

        // Create a large spherical trigger for detecting spatial taps
        components.set(
            CollisionComponent(
                collisionShape = listOf(ShapeResource.createSphere(INTERACTION_RADIUS)),
                physicsMaterial = PhysicsMaterialResource(),
                collisionResponseMode = CollisionResponseMode.TRIGGER_LITE,
                // Exclude interaction receiver from ammo and mesh collision
                collisionFilter =
                    CollisionFilter(
                        group = CollisionGroup(GROUP_INTERACTION),
                        mask = CollisionGroup(GROUP_INTERACTION)
                    )
            )
        )
    }

    companion object {
        // Radius of the interaction receiver sphere (in meters)
        private const val INTERACTION_RADIUS = 3f
    }
}
