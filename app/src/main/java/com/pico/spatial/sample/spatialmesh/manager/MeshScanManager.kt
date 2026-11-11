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
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.PolygonFillMode
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.simulation.CollisionFilter
import com.pico.spatial.core.ecs.simulation.CollisionGroup
import com.pico.spatial.core.lifecycle.Cancellable
import com.pico.spatial.core.math.Transform
import com.pico.spatial.sample.spatialmesh.ecs.components.SpatialMeshComponent
import com.pico.spatial.sample.spatialmesh.util.randomColor4
import com.pico.spatial.sense.base.AnchorUpdate
import com.pico.spatial.sense.mesh.MeshTrackingManager
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object MeshScanManager {
    val root = Entity().apply { setName("SpatialMeshRoot") }
    private val spatialMeshMap = HashMap<UUID, Entity>()
    private var subscription: Cancellable? = null
    private var coroutineScope: CoroutineScope? = null
    private val meshLoadJobs = HashMap<UUID, Job>()
    private val pendingTransforms = HashMap<UUID, Transform>()

    fun subscribeMeshAnchorUpdate() {
        // Ensure handlers run on main thread and individual failures don't cancel the whole scope.
        coroutineScope?.cancel()
        coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        MeshTrackingManager.start()
        subscription =
            MeshTrackingManager.subscribeAnchorUpdate { anchorUpdate ->
                val scope = coroutineScope ?: return@subscribeAnchorUpdate
                scope.launch {
                    val anchorUUID = anchorUpdate.anchor.anchorUUID
                    when (anchorUpdate.event) {
                        AnchorUpdate.Event.ADDED ->
                            handleAdded(anchorUUID, anchorUpdate.anchor.transform)
                        AnchorUpdate.Event.UPDATED ->
                            handleUpdated(anchorUUID, anchorUpdate.anchor.transform)
                        AnchorUpdate.Event.REMOVED -> handleRemoved(anchorUUID)
                        AnchorUpdate.Event.LOADED -> {}
                        AnchorUpdate.Event.UNKNOWN -> {}
                    }
                }
            }
    }

    fun unsubscribeMeshAnchorUpdate() {
        subscription?.cancel()
        subscription = null
        coroutineScope?.cancel()
        coroutineScope = null

        meshLoadJobs.values.forEach { it.cancel() }
        meshLoadJobs.clear()
        pendingTransforms.clear()

        spatialMeshMap.values.forEach { it.destroy() }
        spatialMeshMap.clear()

        MeshTrackingManager.stop()
    }

    private fun handleAdded(anchorUUID: UUID, anchorTransform: Transform) {
        // Always keep the latest transform in case UPDATED comes in before mesh finishes loading.
        pendingTransforms[anchorUUID] = anchorTransform
        if (spatialMeshMap.containsKey(anchorUUID) || meshLoadJobs.containsKey(anchorUUID)) return

        val scope = coroutineScope ?: return
        val job =
            scope.launch {
                try {
                    val mesh =
                        withContext(Dispatchers.IO) { MeshResource.loadFromMeshAnchor(anchorUUID) }
                    val material =
                        PhysicallyBasedMaterial.create().apply {
                            setBaseColor(randomColor4())
                            setOpacity(0.7f)
                            setPolygonFillMode(PolygonFillMode.LINE)
                        }
                    val entity =
                        ModelEntity(mesh, material).apply {
                            setName("Mesh_${anchorUUID.toString().substring(0, 8)}")
                            components.set(SpatialMeshComponent())
                            components.set(
                                CollisionComponent(
                                    // Use static mesh shape instead of convex mesh shape to avoid
                                    // collision
                                    // issue
                                    collisionShape = listOf(ShapeResource.createStaticMesh(mesh)),
                                    physicsMaterial = PhysicsMaterialResource(),
                                    collisionFilter =
                                        CollisionFilter(
                                            group = CollisionGroup(GROUP_SPATIAL_MESH),
                                            mask = CollisionGroup(GROUP_AMMO)
                                        )
                                )
                            )
                        }
                    updateTransform(entity, pendingTransforms.remove(anchorUUID) ?: anchorTransform)
                    root.addChild(entity)
                    spatialMeshMap[anchorUUID] = entity
                } finally {
                    meshLoadJobs.remove(anchorUUID)
                }
            }
        meshLoadJobs[anchorUUID] = job
    }

    private fun handleUpdated(anchorUUID: UUID, anchorTransform: Transform) {
        pendingTransforms[anchorUUID] = anchorTransform

        val entity = spatialMeshMap[anchorUUID]
        if (entity == null) {
            // If the add job is already running, just keep the newest transform.
            if (!meshLoadJobs.containsKey(anchorUUID)) handleAdded(anchorUUID, anchorTransform)
            return
        }

        meshLoadJobs.remove(anchorUUID)?.cancel()
        val scope = coroutineScope ?: return
        val job =
            scope.launch {
                try {
                    val newMesh =
                        withContext(Dispatchers.IO) { MeshResource.loadFromMeshAnchor(anchorUUID) }
                    entity.components[ModelComponent::class.java]?.mesh = newMesh
                    updateTransform(entity, pendingTransforms.remove(anchorUUID) ?: anchorTransform)
                } finally {
                    meshLoadJobs.remove(anchorUUID)
                }
            }
        meshLoadJobs[anchorUUID] = job
    }

    private fun handleRemoved(anchorUUID: UUID) {
        pendingTransforms.remove(anchorUUID)
        meshLoadJobs.remove(anchorUUID)?.cancel()
        spatialMeshMap.remove(anchorUUID)?.destroy()
    }

    private fun updateTransform(entity: Entity, transform: Transform) {
        val convertedPosition = root.convertPositionFrom(transform.position, null)
        val convertedRotation = root.convertRotationFrom(transform.rotation.toQuat(), null)
        entity.components[TransformComponent::class.java]?.apply {
            position = convertedPosition
            quaternion = convertedRotation
        }
    }
}
