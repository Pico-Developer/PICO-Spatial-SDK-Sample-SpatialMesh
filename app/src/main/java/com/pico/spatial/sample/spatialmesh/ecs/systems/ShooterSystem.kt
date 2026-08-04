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
package com.pico.spatial.sample.spatialmesh.ecs.systems

import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.EntityQueryCondition
import com.pico.spatial.core.ecs.PhysicsVelocityComponent
import com.pico.spatial.core.ecs.RigidBodyComponent
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Bool3
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.sample.spatialmesh.data.GlobalConfig
import com.pico.spatial.sample.spatialmesh.ecs.components.ShooterComponent
import com.pico.spatial.sample.spatialmesh.manager.AmmoManager
import com.pico.spatial.sample.spatialmesh.manager.GameplayManager
import com.pico.spatial.sample.spatialmesh.util.Timer
import com.pico.spatial.tracking.hmd.HMDPose

class ShooterSystem : System() {
    private val spawnTimer = Timer(SPAWN_GAP)
    private var isTicking = false
    private val isShooter = EntityQueryCondition.hasComponent(ShooterComponent::class.java)
    // Pre-allocated list to avoid object allocation in the hot update loop (reduces GC pressure)
    private val filteredEntities = mutableListOf<Entity>()

    override fun update(context: SceneUpdateContext) {
        // Design Philosophy: Singleton Caching
        // Since the shooter is a static singleton created once at startup, we only query it once.
        // This avoids the O(N) cost of scene-wide queries in every subsequent frame.
        if (filteredEntities.isEmpty()) {
            filteredEntities.addAll(context.scene.queryEntity(isShooter))
        }

        filteredEntities.forEach { shooter ->
            val component = shooter.components[ShooterComponent::class.java] ?: return@forEach

            if (component.currentAmmoId == -1) {
                spawnAmmo(shooter, component)
            }

            if (component.canFire && component.currentAmmoId != -1) {
                val newData = component.hmdTrackingProvider?.latestData ?: return@forEach
                fireAmmo(shooter, component, newData.hmdPose)
                component.canFire = false
                isTicking = true
            }

            if (isTicking) {
                if (spawnTimer.tryTrigger(context.deltaTime)) {
                    spawnAmmo(shooter, component)
                    isTicking = false
                    spawnTimer.reset()
                }
            }
        }
    }

    private fun spawnAmmo(shooter: Entity, component: ShooterComponent) {
        component.currentAmmoId = AmmoManager.allocate()
        if (component.currentAmmoId != -1) {
            AmmoManager.get(component.currentAmmoId).apply {
                this.components[TransformComponent::class.java]?.position = Vector3.ZERO
                shooter.addChild(this)
            }
        }
    }

    private fun fireAmmo(
        shooter: Entity,
        component: ShooterComponent,
        pose: HMDPose,
        speed: Float = 10f
    ) {
        val ammoId = component.currentAmmoId
        if (ammoId == -1) return

        try {
            val forward = pose.rotation.rotateVector(Vector3(0f, 0f, -1f))
            val firePosition = pose.position + forward * 1f
            val convertedPosition = GameplayManager.ground.convertPositionFrom(firePosition, null)

            AmmoManager.get(ammoId).apply {
                shooter.removeChild(this)
                GameplayManager.ground.addChild(this)
                components[TransformComponent::class.java]?.apply { position = convertedPosition }
                components[RigidBodyComponent::class.java]?.apply {
                    isTranslationLocked = Bool3(false)
                }
                components.set(
                    PhysicsVelocityComponent().apply { linearVelocity = forward * speed }
                )

                // Direct call to manager for on-shoot effects
                GameplayManager.onShoot()
            }
        } catch (e: IllegalArgumentException) {
            Log.e(GlobalConfig.LOG_TAG, "Failed to fire ammo: ${e.message}")
        }
    }

    companion object {
        // The time gap between fire and spawn next ammo
        private const val SPAWN_GAP = 0.06f
    }
}
