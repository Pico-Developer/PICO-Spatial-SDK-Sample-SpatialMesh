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

import com.pico.spatial.core.ecs.resource.AssetBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * Asset bundle configuration and loading.
 *
 * This file defines the asset bundle containing all the 3D models and resources used by the game.
 * The bundle is loaded lazily when first accessed.
 */

// URI of the asset bundle containing game resources (models, textures, etc.)
private const val BUNDLE_URI = "asset://editor-asset.bundle"

// Name for loading the ammo model from the asset bundle
const val SCENE_AMMO_MODEL = "Sphere"

/**
 * Lazy-loaded asset bundle.
 *
 * The asset bundle is loaded asynchronously on the IO dispatcher when first accessed, ensuring that
 * the main thread isn't blocked during initialization.
 */
val assetBundle =
    CoroutineScope(Dispatchers.IO).async(start = CoroutineStart.LAZY) {
        AssetBundle.load(BUNDLE_URI)
    }
