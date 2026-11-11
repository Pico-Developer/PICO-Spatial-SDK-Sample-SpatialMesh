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

import android.util.Log
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.PhysicsVelocityComponent
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PolygonFillMode
import com.pico.spatial.core.ecs.simulation.CollisionResponseMode
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.sample.spatialmesh.data.AudioRepository
import com.pico.spatial.sample.spatialmesh.data.GlobalConfig
import com.pico.spatial.sample.spatialmesh.ecs.components.AmmoComponent
import com.pico.spatial.sample.spatialmesh.ecs.components.ShooterComponent
import com.pico.spatial.sample.spatialmesh.ecs.components.SpatialMeshComponent
import com.pico.spatial.sample.spatialmesh.ui.GameUIBridge

object GameplayManager {
    val shooter = Entity().apply { setName("Shooter") }
    val ground = Entity().apply { setName("Ground") }

    fun markAsReadyToFire() {
        shooter.components[ShooterComponent::class.java]?.canFire = true
    }

    /**
     * Called when ammo is successfully fired. Use this to trigger sound effects or other "on-fire"
     * logic.
     */
    fun onShoot() {
        AudioRepository.playFireSFX(shooter)
    }

    fun onHit(entityA: Entity, entityB: Entity, position: Vector3) {
        val spatialMeshEntity =
            when {
                entityA.components.has(SpatialMeshComponent::class.java) -> entityA
                entityB.components.has(SpatialMeshComponent::class.java) -> entityB
                else -> null
            }

        val ammoEntity =
            when {
                entityA.components.has(AmmoComponent::class.java) -> entityA
                entityB.components.has(AmmoComponent::class.java) -> entityB
                else -> null
            }

        if (spatialMeshEntity != null && ammoEntity != null) {
            val spatialMesh = spatialMeshEntity.components[SpatialMeshComponent::class.java]
            val ammo = ammoEntity.components[AmmoComponent::class.java]
            if (spatialMesh?.isHit == true || ammo?.isDespawning == true) {
                if (spatialMesh?.isHit == true) {
                    Log.i(
                        GlobalConfig.LOG_TAG,
                        "Ignoring hit on mesh ${spatialMeshEntity.getName()}: already hit"
                    )
                }
                if (ammo?.isDespawning == true) {
                    Log.i(
                        GlobalConfig.LOG_TAG,
                        "Ignoring hit from ammo ${ammoEntity.getName()}: already despawning"
                    )
                }
                return
            }
            handleAmmoHit(ammoEntity, spatialMeshEntity, position)
        }
    }

    private fun handleAmmoHit(ammo: Entity, mesh: Entity, position: Vector3) {
        // Mark mesh as hit
        mesh.components[SpatialMeshComponent::class.java]?.isHit = true

        // Update UI State via Bridge
        GameUIBridge.viewModel?.incrementScore()

        // Visual feedback
        val material =
            mesh.components[ModelComponent::class.java]?.materials?.get(0)
                as PhysicallyBasedMaterial
        material.setPolygonFillMode(PolygonFillMode.FILL)

        // Trigger hit sound at the exact collision position.
        // We pass the position to AudioRepository which uses an emitter pool for efficient spatial
        // audio.
        AudioRepository.playHitSFX(position)

        // Make ammo a trigger
        ammo.components[CollisionComponent::class.java]?.apply {
            collisionResponseMode = CollisionResponseMode.TRIGGER_LITE
        }

        // Stop ammo and mark for despawn
        ammo.apply {
            components[PhysicsVelocityComponent::class.java]?.linearVelocity = Vector3.ZERO
            components[AmmoComponent::class.java]?.apply {
                hitMesh = mesh
                isDespawning = true
                despawnTimer = 2.0f
            }
        }
    }

    fun clear() {
        shooter.removeAllChildren()
        ground.removeAllChildren()
        AmmoManager.clear()
        shooter.components[ShooterComponent::class.java]?.apply {
            canFire = false
            currentAmmoId = -1
        }
    }
}
