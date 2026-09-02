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
package com.pico.spatial.sample.spatialmesh.manager

import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.PhysicsVelocityComponent
import com.pico.spatial.core.ecs.RigidBodyComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.PolygonFillMode
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.simulation.CollisionFilter
import com.pico.spatial.core.ecs.simulation.CollisionGroup
import com.pico.spatial.core.ecs.simulation.CollisionResponseMode
import com.pico.spatial.core.math.Bool3
import com.pico.spatial.sample.spatialmesh.data.SCENE_AMMO_MODEL
import com.pico.spatial.sample.spatialmesh.data.assetBundle
import com.pico.spatial.sample.spatialmesh.ecs.components.AmmoComponent
import com.pico.spatial.sample.spatialmesh.ecs.components.SpatialMeshComponent

/**
 * AmmoManager implements an Entity Pool pattern for projectiles (ammo).
 *
 * Design Philosophy:
 * 1. Performance: Projectiles are high-frequency entities. Creating/destroying them at runtime
 *    causes GC pressure and performance spikes. We pre-allocate a fixed pool at startup.
 * 2. State Reset: When an ammo is 'freed', we must meticulously reset its state (physics,
 *    transform, visual components) to ensure it behaves like a fresh entity when reused.
 * 3. Physics Lifecycle: We lock physics translation when ammo entities are in the pool to prevent
 *    unintended simulation of inactive objects.
 */
object AmmoManager {
    private const val MAX = 32
    private lateinit var ammoArray: Array<Entity>
    private val freeIndices = IntArray(MAX) { it }
    private val isUsed = BooleanArray(MAX) { false }
    private var freeCount = MAX
    private lateinit var ammoModel: Entity

    // Ammo properties
    private var colliderRadius: Float = 0.05f

    private var isInitialized = false

    suspend fun initialize() {
        if (isInitialized) return
        ammoModel = assetBundle.await().loadModel(SCENE_AMMO_MODEL)
        ammoArray = Array(MAX) { makeAmmo() }
        isInitialized = true
    }

    /**
     * Allocate an inactive ammo slot. Returns the index of the allocated ammo entity, or -1 if the
     * pool is full.
     */
    fun allocate(): Int {
        if (freeCount == 0) return -1 // Pool full
        val index = freeIndices[--freeCount]
        isUsed[index] = true
        ammoArray[index].apply {
            setName("Ammo_$index")
            enabled = true

            // Initial component reset for allocation
            components[AmmoComponent::class.java]?.apply {
                ammoId = index
                hitMesh = null
                isDespawning = false
                despawnTimer = 0f
            }
        }
        return index
    }

    /** Return ammo to the pool and reset its state. */
    fun free(index: Int) {
        if (index < 0 || index >= MAX || !isUsed[index]) return
        isUsed[index] = false
        ammoArray[index].apply {
            components[AmmoComponent::class.java]?.apply {
                // If this ammo was stuck to a mesh, reset that mesh's visual state
                val mesh = hitMesh
                if (mesh != null) {
                    mesh.components[SpatialMeshComponent::class.java]?.isHit = false
                    (mesh.components[ModelComponent::class.java]?.materials?.get(0)
                            as? PhysicallyBasedMaterial)
                        ?.setPolygonFillMode(PolygonFillMode.LINE)
                }
                hitMesh = null
                isDespawning = false
                despawnTimer = 0f
                ammoId = -1 // Mark as inactive
            }

            // Detach from current parent (e.g., Ground or Shooter)
            getParent()?.removeChild(this)

            // Disable to stop processing/rendering
            enabled = false

            // Reset transform and physics state
            components.set(TransformComponent())
            components.set(PhysicsVelocityComponent()) // Stop all movement

            components[CollisionComponent::class.java]?.apply {
                // Restore to full collision mode if it was changed to TRIGGER_LITE during hit
                collisionResponseMode = CollisionResponseMode.COLLIDER_FULL
            }
            components[RigidBodyComponent::class.java]?.apply {
                // Lock movement while in pool
                isTranslationLocked = Bool3(true)
            }
        }
        freeIndices[freeCount++] = index
    }

    /** Get the entity instance for an allocated ammo. */
    fun get(index: Int): Entity = ammoArray[index]

    /** Clear all ammo entities. */
    fun clear() {
        for (i in 0 until MAX) {
            if (isUsed[i]) {
                free(i)
            }
        }
        // Ensure pool state is fully clean in case of any inconsistency
        freeCount = MAX
        for (i in 0 until MAX) {
            freeIndices[i] = i
            isUsed[i] = false
        }
    }

    private fun makeAmmo(): Entity {
        val ammo =
            ammoModel.clone(
                Entity.CloneOptions(recursive = true, shouldShareMaterialInstance = true)
            ) ?: Entity()
        ammo.components.apply {
            set(AmmoComponent())
            set(RigidBodyComponent().apply { isTranslationLocked = Bool3(true) })
            set(
                CollisionComponent(
                    collisionShape = listOf(ShapeResource.createSphere(colliderRadius)),
                    physicsMaterial = PhysicsMaterialResource(),
                    collisionFilter =
                        CollisionFilter(
                            group = CollisionGroup(GROUP_AMMO),
                            mask = CollisionGroup(GROUP_SPATIAL_MESH)
                        )
                )
            )
        }
        return ammo
    }
}

const val GROUP_INTERACTION = 1u
const val GROUP_AMMO = 2u
const val GROUP_SPATIAL_MESH = 4u
