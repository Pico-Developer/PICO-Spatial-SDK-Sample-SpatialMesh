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
package com.pico.spatial.sample.spatialmesh.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.AnchorComponent
import com.pico.spatial.core.ecs.AnchorEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.anchor.AnchorTarget
import com.pico.spatial.core.ecs.event.CollisionEvents
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.sample.spatialmesh.R
import com.pico.spatial.sample.spatialmesh.data.AudioRepository
import com.pico.spatial.sample.spatialmesh.ecs.components.ShooterComponent
import com.pico.spatial.sample.spatialmesh.ecs.entities.InteractionReceiver
import com.pico.spatial.sample.spatialmesh.ecs.systems.AmmoSystem
import com.pico.spatial.sample.spatialmesh.ecs.systems.ShooterSystem
import com.pico.spatial.sample.spatialmesh.manager.AmmoManager
import com.pico.spatial.sample.spatialmesh.manager.GameplayManager
import com.pico.spatial.sample.spatialmesh.manager.MeshScanManager
import com.pico.spatial.tracking.hmd.HMDTrackingProvider
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.content.SpatialViewAttachments
import com.pico.spatial.ui.foundation.dsl.registerSystem
import com.pico.spatial.ui.foundation.dsl.unregisterSystem
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialTapGesture
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext

private const val TAG = "GameScene"
private const val ENTITY_NAME_CAMERA_TARGET = "CameraTarget"
private const val ENTITY_NAME_CONTROL_BUTTON = "control_button"
private val VECTOR_CAMERA_OFFSET = Vector3(0f, -0.15f, -0.6f)
private val VECTOR_BUTTON_POSITION = Vector3(0f, -0.08f, 0f)

@Composable
fun MainStage() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var restartKey by remember { mutableIntStateOf(0) }
    var hasPaused by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> hasPaused = true
                Lifecycle.Event.ON_RESUME -> {
                    if (hasPaused) {
                        hasPaused = false
                        restartKey += 1
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    key(restartKey) { MainStageContent() }
}

@Composable
private fun MainStageContent() {
    val context = LocalContext.current
    val gameViewModel: GameViewModel = viewModel()
    val score by gameViewModel.score.collectAsState()
    val showTip by gameViewModel.showTip.collectAsState()

    // Connect the UI ViewModel to ECS systems (and clean it up on dispose).
    DisposableEffect(gameViewModel) {
        GameUIBridge.viewModel = gameViewModel
        onDispose {
            if (GameUIBridge.viewModel === gameViewModel) {
                GameUIBridge.viewModel = null
            }
        }
    }

    val isSpatialEmulator = gameViewModel.isSpatialEmulator

    // Use an interaction receiver to detect spatial tap gestures on device.
    // Must be remembered to avoid creating a new Entity on every recomposition.
    val interactionReceiver =
        remember(isSpatialEmulator) { if (!isSpatialEmulator) InteractionReceiver() else null }

    val hmdTrackingProvider = remember { HMDTrackingProvider() }

    val cameraTarget =
        remember(interactionReceiver, hmdTrackingProvider) {
            AnchorEntity(AnchorTarget.createCameraTarget()).apply {
                setName(ENTITY_NAME_CAMERA_TARGET)
                components[AnchorComponent::class.java]?.apply {
                    positionOffset = VECTOR_CAMERA_OFFSET
                }
                addChild(
                    GameplayManager.shooter.apply {
                        components.set(ShooterComponent(hmdTrackingProvider))
                    }
                )
                interactionReceiver?.let { addChild(it) }
            }
        }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        try {
            // If any init step fails, throw to make the failure visible.
            AmmoManager.initialize()
            MeshScanManager.subscribeMeshAnchorUpdate()
            registerSystem<AmmoSystem>()
            hmdTrackingProvider.start()
            registerSystem<ShooterSystem>()
            AudioRepository.playBGM()

            // Keep this effect alive until the composable leaves the composition.
            // When it's cancelled, we run cleanup in the finally block.
            awaitCancellation()
        } catch (t: Throwable) {
            Log.e(TAG, "[init] failed", t)
            throw t
        } finally {
            withContext(NonCancellable) {
                // Reverse-order cleanup; don't let one failure block the rest.
                runCatching { unregisterSystem<ShooterSystem>() }
                    .onFailure { e ->
                        Log.e(TAG, "[cleanup] unregisterSystem<ShooterSystem> failed", e)
                    }
                runCatching { hmdTrackingProvider.stop() }
                    .onFailure { e -> Log.e(TAG, "[cleanup] hmdTrackingProvider.stop failed", e) }
                runCatching { unregisterSystem<AmmoSystem>() }
                    .onFailure { e ->
                        Log.e(TAG, "[cleanup] unregisterSystem<AmmoSystem> failed", e)
                    }
                runCatching { GameplayManager.clear() }
                    .onFailure { e -> Log.e(TAG, "[cleanup] GameplayManager.clear failed", e) }
                runCatching { MeshScanManager.unsubscribeMeshAnchorUpdate() }
                    .onFailure { e ->
                        Log.e(
                            TAG,
                            "[cleanup] MeshScanManager.unsubscribeMeshAnchorUpdate failed",
                            e
                        )
                    }
                runCatching { AudioRepository.stopBGM() }
                    .onFailure { e -> Log.e(TAG, "[cleanup] AudioRepository.stopBGM failed", e) }
                runCatching { gameViewModel.reset() }
                    .onFailure { e -> Log.e(TAG, "[cleanup] gameViewModel.reset failed", e) }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                AudioRepository.pauseBGM()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                AudioRepository.playBGM()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    SpatialView(
        modifier =
            Modifier.pointerInput(interactionReceiver, context) {
                interactionReceiver?.let {
                    detectSpatialTapGesture(context, TargetEntity.hit(it)) {
                        GameplayManager.markAsReadyToFire()
                        gameViewModel.hideTip()
                    }
                }
            },
        initial = { content, attachments ->
            setupSceneEntities(content, attachments, cameraTarget)

            // Collision.Enter event
            content.subscribe(eventType = CollisionEvents.Enter::class.java) { collision ->
                val entityA = collision.entityA ?: return@subscribe
                val entityB = collision.entityB ?: return@subscribe
                GameplayManager.onHit(entityA, entityB, collision.position)
            }
        },
        attachments = {
            AttachmentPanel(id = ENTITY_NAME_CONTROL_BUTTON) {
                GameUIOverlay(
                    score = score,
                    showTip = showTip,
                    isSpatialEmulator = isSpatialEmulator,
                    onEmulatorFire = {
                        GameplayManager.markAsReadyToFire()
                        gameViewModel.hideTip()
                    }
                )
            }
        }
    )
}

@Composable
private fun GameUIOverlay(
    score: Int,
    showTip: Boolean,
    isSpatialEmulator: Boolean,
    onEmulatorFire: () -> Unit,
) {
    // UI overlay for tip and score
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isSpatialEmulator) {
            Button(onClick = onEmulatorFire) {
                Text(stringResource(R.string.click_to_fire), fontSize = 16.sp)
            }
        }
        Text(
            text = stringResource(R.string.score_format, score),
            color = Color.White,
            fontSize = 16.sp,
        )
        if (!isSpatialEmulator && showTip) {
            Text(
                text = stringResource(R.string.fire_tip),
                color = Color.White,
                fontSize = 16.sp,
            )
        }
    }
}

private fun setupSceneEntities(
    content: SpatialViewContent,
    attachments: SpatialViewAttachments,
    cameraTarget: AnchorEntity
) {
    content.addEntity(AudioRepository.root)
    content.addEntity(GameplayManager.ground)
    content.addEntity(MeshScanManager.root)
    content.addEntity(cameraTarget)
    attachments.entity(ENTITY_NAME_CONTROL_BUTTON)?.apply {
        setName(ENTITY_NAME_CONTROL_BUTTON)
        components[TransformComponent::class.java]?.apply { position = VECTOR_BUTTON_POSITION }
        cameraTarget.addChild(this)
    }
}
