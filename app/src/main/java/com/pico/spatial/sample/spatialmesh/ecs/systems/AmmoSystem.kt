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
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.sample.spatialmesh.data.GlobalConfig
import com.pico.spatial.sample.spatialmesh.ecs.components.AmmoComponent
import com.pico.spatial.sample.spatialmesh.manager.AmmoManager

class AmmoSystem : System() {
    private val isAmmo = EntityQueryCondition.hasComponent(AmmoComponent::class.java)
    // Pre-allocated list to avoid object allocation in the hot update loop (reduces GC pressure)
    private val filteredEntities = mutableListOf<Entity>()

    override fun update(context: SceneUpdateContext) {
        // Design Philosophy: Dynamic List Reuse
        // Ammo entities are dynamic and frequently recycled. We re-query the scene every frame
        // to ensure we have the latest set of active projectiles.
        // We call clear() and addAll() on a pre-allocated member list to avoid creating new
        // List objects in the hot loop. This prevents GC pressure and avoids 'judder' (frame
        // drops).
        filteredEntities.clear()
        filteredEntities.addAll(context.scene.queryEntity(isAmmo))

        filteredEntities.forEach { ammo ->
            ammo.components[AmmoComponent::class.java]?.apply {
                val currentId = ammoId
                if (currentId == -1) return@forEach

                // Out of range check
                try {
                    val pos = ammo.components[TransformComponent::class.java]?.position
                    if (pos != null && pos.length() > MAX_RANGE) {
                        AmmoManager.free(currentId)
                        return@forEach
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e(
                        GlobalConfig.LOG_TAG,
                        "Failed when checking position of ammo $currentId: ${e.message}"
                    )
                    AmmoManager.free(currentId)
                    return@forEach
                }

                // Handle hit-based despawn timer
                if (isDespawning) {
                    despawnTimer -= context.deltaTime
                    if (despawnTimer <= 0) {
                        AmmoManager.free(currentId)
                    }
                }
            }
        }
    }

    companion object {
        const val MAX_RANGE = 10f
    }
}
