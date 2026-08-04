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
package com.pico.spatial.sample.spatialmesh.data

import com.pico.spatial.core.ecs.AmbientAudioComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.audio.AudioPlayerController
import com.pico.spatial.core.ecs.resource.AudioResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * AudioRepository manages the loading, preparation, and playback of audio resources.
 *
 * Design Philosophy:
 * 1. Emitter Pooling: Instead of attaching audio components/controllers to every projectile (which
 *    is resource-intensive and can lead to voice cutting if entities are recycled), we use a fixed
 *    pool of dedicated 'HitEmitter' entities.
 * 2. Spatialization via Displacement: For one-shot spatial sounds (like impacts), we move a pooled
 *    emitter to the target world position and play the sound. This ensures consistent
 *    spatialization without entity overhead.
 * 3. Pre-warming: Controllers are prepared (pre-warmed) during startup to avoid runtime allocation
 *    stutter when sounds are first played.
 */
object AudioRepository {
    private const val MAX_HIT_EMITTERS = 10
    private val resources = mutableMapOf<String, AudioResource>()
    private val bgmControllerMap = mutableMapOf<String, AudioPlayerController>()

    // Pool for hit sounds (spatialized at hit location)
    // We use a pool to avoid the overhead of adding AudioComponents to hundreds of entities.
    private val hitEmitterPool =
        List(MAX_HIT_EMITTERS) {
            Entity().apply {
                setName("HitEmitter_$it")
                components.set(AmbientAudioComponent())
                components.set(TransformComponent())
            }
        }
    private var nextEmitterIndex = 0
    private val emitterControllerMap = mutableMapOf<Pair<Int, String>, AudioPlayerController>()

    // Controller for fire sounds (attached to shooter)
    // Fire sounds are attached to the shooter entity so they follow the player.
    private var shooterEntity: Entity? = null
    private val shooterControllerMap = mutableMapOf<String, AudioPlayerController>()

    val root =
        Entity().apply {
            setName("AudioRoot")
            components.set(AmbientAudioComponent())
            hitEmitterPool.forEach { addChild(it) }
        }

    private val audioNamePathList =
        listOf(
            "bgm" to "asset://audio/bgm.ogg",
            "shoot_01" to "asset://audio/shoot_01.wav",
            "shoot_02" to "asset://audio/shoot_02.wav",
            "shoot_03" to "asset://audio/shoot_03.wav",
            "hit_01" to "asset://audio/hit_01.wav",
            "hit_02" to "asset://audio/hit_02.wav",
            "hit_03" to "asset://audio/hit_03.wav",
        )

    suspend fun loadAudio() =
        withContext(Dispatchers.IO) {
            audioNamePathList
                .map { (name, path) ->
                    name to async { AudioResource.load(name = name, path = path) }
                }
                .forEach { (name, deferred) -> resources[name] = deferred.await() }
        }

    suspend fun prepareAudio() {
        // Prepare BGM
        resources["bgm"]?.let { bgmControllerMap["bgm"] = root.prepareAudio(it) }

        // Pre-prepare hit controllers for each emitter in the pool
        val hitSounds = listOf("hit_01", "hit_02", "hit_03")
        hitEmitterPool.forEachIndexed { emitterIdx, entity ->
            hitSounds.forEach { soundName ->
                resources[soundName]?.let { res ->
                    emitterControllerMap[emitterIdx to soundName] = entity.prepareAudio(res)
                }
            }
        }
    }

    fun playBGM() {
        bgmControllerMap["bgm"]?.apply {
            setLoop(true)
            setVolume(0.2f)
            play()
        }
    }

    fun pauseBGM() {
        bgmControllerMap["bgm"]?.pause()
    }

    fun stopBGM() {
        bgmControllerMap["bgm"]?.apply {
            stop()
            try {
                seekTo(0L)
            } catch (e: Exception) {
                com.pico.spatial.core.internal.Logger.w(
                    "AudioRepository",
                    "seekTo failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Best practice for high-frequency sounds at world locations: Use a pooled emitter, move it to
     * the location, and play.
     */
    fun playHitSFX(position: com.pico.spatial.core.math.Vector3) {
        val soundName = listOf("hit_01", "hit_02", "hit_03").random()
        val emitterIdx = nextEmitterIndex
        val emitter = hitEmitterPool[emitterIdx]

        // Move emitter to hit location
        emitter.components[TransformComponent::class.java]?.position = position

        // Play sound
        emitterControllerMap[emitterIdx to soundName]?.apply {
            setVolume(1.0f)
            stop()
            play()
        }

        nextEmitterIndex = (nextEmitterIndex + 1) % MAX_HIT_EMITTERS
    }

    /** For sounds attached to a specific entity (like the shooter). */
    fun playFireSFX(entity: Entity) {
        val soundName = listOf("shoot_01", "shoot_02", "shoot_03").random()

        // Initialize shooter controllers if this is a new entity
        if (shooterEntity != entity) {
            shooterEntity = entity
            shooterControllerMap.clear()
            if (!entity.components.has(AmbientAudioComponent::class.java)) {
                entity.components.set(AmbientAudioComponent())
            }
        }

        val controller =
            shooterControllerMap.getOrPut(soundName) {
                val res = resources[soundName]!!
                entity.prepareAudio(res)
            }

        controller.apply {
            setVolume(1.0f)
            stop()
            play()
        }
    }
}
