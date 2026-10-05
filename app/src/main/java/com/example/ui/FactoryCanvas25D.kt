package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.model.*
import com.example.viewmodel.FactoryUiState
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun FactoryCanvas25D(
    uiState: FactoryUiState,
    onTileTapped: (Int, Int) -> Unit,
    onCameraMoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    var zoom by remember { mutableFloatStateOf(1.05f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val baseTilePx = 64f
    val tilePx = baseTilePx * zoom

    val currentZoom by rememberUpdatedState(zoom)
    val currentPanOffset by rememberUpdatedState(panOffset)
    val currentOnTileTapped by rememberUpdatedState(onTileTapped)
    val currentOnCameraMoved by rememberUpdatedState(onCameraMoved)

    // Center camera when requested via cameraFocusNonce or when canvas is first sized
    LaunchedEffect(uiState.cameraFocusNonce, canvasSize) {
        val target = uiState.cameraFocusTile ?: return@LaunchedEffect
        if (canvasSize.width > 0f && canvasSize.height > 0f) {
            val currentTilePx = baseTilePx * currentZoom
            panOffset = Offset(
                x = canvasSize.width / 2f - (target.first + 0.5f) * currentTilePx,
                y = canvasSize.height / 2f - (target.second + 0.5f) * currentTilePx
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "factory_anim")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "belt_and_machine_phase"
    )

    val tileMap = remember(uiState.mapTiles) {
        uiState.mapTiles.associateBy { it.x to it.y }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B1017))
            .onSizeChanged { intSize ->
                val newSize = Size(intSize.width.toFloat(), intSize.height.toFloat())
                if (newSize != canvasSize) {
                    canvasSize = newSize
                }
            }
            .testTag("factory_map_canvas")
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    val oldZoom = currentZoom
                    val newZoom = (oldZoom * gestureZoom).coerceIn(0.55f, 2.25f)
                    val zoomFactor = newZoom / oldZoom
                    val oldPan = currentPanOffset
                    panOffset = Offset(
                        x = centroid.x - (centroid.x - oldPan.x) * zoomFactor + pan.x,
                        y = centroid.y - (centroid.y - oldPan.y) * zoomFactor + pan.y
                    )
                    zoom = newZoom
                    if (pan.getDistanceSquared() > 12f || kotlin.math.abs(gestureZoom - 1f) > 0.02f) {
                        currentOnCameraMoved()
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapPos ->
                    val activeTilePx = baseTilePx * currentZoom
                    val activePan = currentPanOffset
                    val gx = floor((tapPos.x - activePan.x) / activeTilePx).toInt()
                    val gy = floor((tapPos.y - activePan.y) / activeTilePx).toInt()
                    if (gx in 0 until GameCatalog.MAP_WIDTH && gy in 0 until GameCatalog.MAP_HEIGHT) {
                        currentOnTileTapped(gx, gy)
                    }
                }
            }
    ) {
        if (size.width <= 4f || size.height <= 4f) return@Canvas

        // Calculate visible tile bounds for high mobile performance
        val minX = max(0, floor((-panOffset.x) / tilePx).toInt())
        val maxX = min(GameCatalog.MAP_WIDTH - 1, floor((size.width - panOffset.x) / tilePx).toInt())
        val minY = max(0, floor((-panOffset.y) / tilePx).toInt())
        val maxY = min(GameCatalog.MAP_HEIGHT - 1, floor((size.height - panOffset.y) / tilePx).toInt())

        // 1. Draw Ground Tiles, Biome Textures, and Resource Deposits
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val tile = tileMap[x to y] ?: continue
                val left = panOffset.x + x * tilePx
                val top = panOffset.y + y * tilePx
                val isChecker = (x + y) % 2 == 0
                val baseColor = Color(if (isChecker) tile.biome.groundPrimaryHex else tile.biome.groundSecondaryHex)
                val isBiomeLocked = tile.biome.requiredTechId != null &&
                    tile.biome.requiredTechId !in uiState.unlockedTechIds

                drawRect(
                    color = if (isBiomeLocked) baseColor.copy(alpha = 0.55f) else baseColor,
                    topLeft = Offset(left, top),
                    size = Size(tilePx, tilePx)
                )

                // Crisp pixel grid bevel
                drawRect(
                    color = Color.Black.copy(alpha = 0.22f),
                    topLeft = Offset(left, top),
                    size = Size(tilePx, tilePx),
                    style = Stroke(width = max(1f, 1.2f * zoom))
                )

                // Subtle 2.5D pixel terrain detail
                if (!uiState.reducedEffects && zoom >= 0.7f) {
                    val detailColor = Color(tile.biome.accentHex).copy(alpha = 0.14f)
                    val seed = ((x * 37 + y * 17) and 7)
                    if (seed < 3) {
                        drawRect(
                            color = detailColor,
                            topLeft = Offset(left + tilePx * 0.2f, top + tilePx * 0.25f),
                            size = Size(tilePx * 0.12f, tilePx * 0.12f)
                        )
                    } else if (seed == 4) {
                        drawRect(
                            color = detailColor,
                            topLeft = Offset(left + tilePx * 0.65f, top + tilePx * 0.6f),
                            size = Size(tilePx * 0.14f, tilePx * 0.08f)
                        )
                    }
                }

                // Draw Resource Deposit in 2.5D pixel style
                if (tile.deposit != DepositType.NONE) {
                    drawDeposit25D(
                        deposit = tile.deposit,
                        left = left,
                        top = top,
                        tilePx = tilePx,
                        zoom = zoom,
                        animPhase = animPhase,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }

        // 2. Draw Biome Boundary Lines & Labels
        drawBiomeBorders(
            panOffset = panOffset,
            tilePx = tilePx,
            zoom = zoom,
            unlockedTechs = uiState.unlockedTechIds,
            textMeasurer = textMeasurer
        )

        // 3. Draw Power Grid Radius Overlay when enabled or placing power/machine buildings
        val shouldShowPower = uiState.showPowerGridOverlay ||
            uiState.buildTool?.category == BuildingCategory.POWER_GRID ||
            (uiState.buildTool?.powerConsumptionKw ?: 0) > 0

        if (shouldShowPower) {
            val conduitBonus = if ("TECH_SOLAR_GRID" in uiState.unlockedTechIds) 2 else 0
            for (b in uiState.buildings.values) {
                if (b.type.powerRadius > 0 && b.powerState == PowerState.POWERED) {
                    val effRadius = if (b.type == BuildingType.POWER_CONDUIT) {
                        b.type.powerRadius + conduitBonus
                    } else {
                        b.type.powerRadius
                    }
                    val cx = panOffset.x + (b.x + 0.5f) * tilePx
                    val cy = panOffset.y + (b.y + 0.5f) * tilePx
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.07f),
                        radius = effRadius * tilePx,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                        radius = effRadius * tilePx,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 1.5f * zoom,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    )
                }
            }
        }

        // 4. Draw Buildings & Animated Belt Items in 2.5D top-to-bottom order
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val b = uiState.buildings[x to y] ?: continue
                val left = panOffset.x + x * tilePx
                val top = panOffset.y + y * tilePx
                val isSelected = uiState.selectedTile == (x to y)

                drawBuilding25D(
                    building = b,
                    left = left,
                    top = top,
                    tilePx = tilePx,
                    zoom = zoom,
                    animPhase = if (uiState.isSimPaused) 0f else animPhase,
                    reducedEffects = uiState.reducedEffects,
                    isSelected = isSelected,
                    storageCap = if ("TECH_SMART_ROUTING" in uiState.unlockedTechIds) 250 else 120,
                    textMeasurer = textMeasurer
                )
            }
        }

        // 5. Draw Power Grid Visual Overlay (Conduit Links + Per-Machine Powered vs Offline Highlights)
        if (shouldShowPower) {
            drawPowerGridVisualOverlay(
                uiState = uiState,
                panOffset = panOffset,
                tilePx = tilePx,
                zoom = zoom,
                animPhase = if (uiState.isSimPaused) 0f else animPhase,
                minX = minX,
                maxX = maxX,
                minY = minY,
                maxY = maxY,
                textMeasurer = textMeasurer
            )
        }

        // 6. Draw Ghost Preview before confirming placement
        val previewPos = uiState.previewTile
        val activeTool = uiState.buildTool
        if (previewPos != null && activeTool != null) {
            val px = previewPos.first
            val py = previewPos.second
            val left = panOffset.x + px * tilePx
            val top = panOffset.y + py * tilePx
            val tile = tileMap[px to py]
            val biomeUnlocked = tile?.biome?.requiredTechId == null ||
                tile.biome.requiredTechId in uiState.unlockedTechIds
            val depositOk = if (activeTool == BuildingType.EXTRACTOR_MK1 || activeTool == BuildingType.EXTRACTOR_MK2) {
                tile?.deposit?.outputItem != null
            } else true
            val occupied = uiState.buildings.containsKey(px to py) &&
                !(activeTool == BuildingType.BELT_MK1 || activeTool == BuildingType.BELT_MK2)
            val isValid = biomeUnlocked && depositOk && !occupied

            val previewColor = if (isValid) Color(0xFF22C55E) else Color(0xFFEF4444)

            if (activeTool.powerRadius > 0) {
                val conduitBonus = if ("TECH_SOLAR_GRID" in uiState.unlockedTechIds) 2 else 0
                val rad = if (activeTool == BuildingType.POWER_CONDUIT) {
                    activeTool.powerRadius + conduitBonus
                } else activeTool.powerRadius
                drawCircle(
                    color = previewColor.copy(alpha = 0.14f),
                    radius = rad * tilePx,
                    center = Offset(left + tilePx * 0.5f, top + tilePx * 0.5f)
                )
            }

            val boxSide = max(2f, tilePx - 4f)
            drawRoundRect(
                color = previewColor.copy(alpha = 0.32f),
                topLeft = Offset(left + 2f, top + 2f),
                size = Size(boxSide, boxSide),
                cornerRadius = CornerRadius(6f * zoom, 6f * zoom)
            )
            drawRoundRect(
                color = previewColor,
                topLeft = Offset(left + 2f, top + 2f),
                size = Size(boxSide, boxSide),
                cornerRadius = CornerRadius(6f * zoom, 6f * zoom),
                style = Stroke(width = 3f * zoom)
            )

            drawDirectionArrow(
                cx = left + tilePx * 0.5f,
                cy = top + tilePx * 0.5f,
                tilePx = tilePx,
                direction = uiState.buildDirection,
                color = Color.White
            )
        }

        // 7. Draw Weather Effects in Visible Biomes
        if (!uiState.reducedEffects) {
            drawBiomeWeatherParticles(
                panOffset = panOffset,
                tilePx = tilePx,
                animPhase = animPhase,
                weatherPhase = uiState.weatherPhase
            )
        }

        // 8. Draw Floating Sale Popups (+$XX)
        for (popup in uiState.floatingPopups) {
            val px = panOffset.x + (popup.tileX + 0.2f) * tilePx
            val py = panOffset.y + (popup.tileY - popup.ageProgress * 0.7f) * tilePx
            safeDrawText(
                textMeasurer = textMeasurer,
                text = popup.text,
                topLeft = Offset(px, py),
                style = TextStyle(
                    color = Color(popup.colorHex).copy(alpha = (1f - popup.ageProgress).coerceIn(0f, 1f)),
                    fontSize = (13f * zoom.coerceIn(0.8f, 1.4f)).sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

/**
 * Safely measures and draws text on a [DrawScope] without ever passing negative maxWidth/maxHeight
 * constraints when [topLeft] is near or outside the canvas edges.
 */
private fun DrawScope.safeDrawText(
    textMeasurer: TextMeasurer,
    text: String,
    topLeft: Offset,
    style: TextStyle
) {
    if (text.isEmpty()) return
    if (topLeft.x < 0f || topLeft.y < 0f) return
    if (topLeft.x >= size.width - 12f || topLeft.y >= size.height - 12f) return
    runCatching {
        val layoutResult = textMeasurer.measure(
            text = AnnotatedString(text),
            style = style
        )
        if (topLeft.x + layoutResult.size.width <= size.width &&
            topLeft.y + layoutResult.size.height <= size.height
        ) {
            drawText(
                textLayoutResult = layoutResult,
                topLeft = topLeft
            )
        }
    }
}

private fun DrawScope.drawDeposit25D(
    deposit: DepositType,
    left: Float,
    top: Float,
    tilePx: Float,
    zoom: Float,
    animPhase: Float,
    textMeasurer: TextMeasurer
) {
    val primary = Color(deposit.colorHex)
    val accent = Color(deposit.accentHex)

    // 2.5D Shadow base
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.38f),
        topLeft = Offset(left + tilePx * 0.14f, top + tilePx * 0.22f),
        size = Size(tilePx * 0.74f, tilePx * 0.68f),
        cornerRadius = CornerRadius(4f * zoom, 4f * zoom)
    )

    // Extruded pixel clusters
    drawRoundRect(
        color = primary,
        topLeft = Offset(left + tilePx * 0.14f, top + tilePx * 0.14f),
        size = Size(tilePx * 0.72f, tilePx * 0.66f),
        cornerRadius = CornerRadius(5f * zoom, 5f * zoom)
    )

    // Top crystal/ore facets
    drawRect(
        color = accent,
        topLeft = Offset(left + tilePx * 0.24f, top + tilePx * 0.22f),
        size = Size(tilePx * 0.22f, tilePx * 0.22f)
    )
    drawRect(
        color = accent.copy(alpha = 0.85f),
        topLeft = Offset(left + tilePx * 0.52f, top + tilePx * 0.42f),
        size = Size(tilePx * 0.20f, tilePx * 0.20f)
    )

    if (deposit == DepositType.MAGMA_VENT) {
        val pulse = 0.5f + 0.5f * sin(animPhase * 6.28f)
        drawCircle(
            color = Color(0xFFFDE047).copy(alpha = 0.45f + 0.4f * pulse),
            radius = tilePx * 0.22f,
            center = Offset(left + tilePx * 0.5f, top + tilePx * 0.46f)
        )
    }

    if (zoom >= 0.65f) {
        val label = deposit.outputItem?.shortCode ?: "GEO"
        safeDrawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(left + tilePx * 0.22f, top + tilePx * 0.52f),
            style = TextStyle(
                color = Color.White,
                fontSize = (10f * zoom.coerceIn(0.75f, 1.3f)).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

private fun DrawScope.drawBuilding25D(
    building: PlacedBuilding,
    left: Float,
    top: Float,
    tilePx: Float,
    zoom: Float,
    animPhase: Float,
    reducedEffects: Boolean,
    isSelected: Boolean,
    storageCap: Int,
    textMeasurer: TextMeasurer
) {
    val type = building.type
    val isBeltLike = type == BuildingType.BELT_MK1 || type == BuildingType.BELT_MK2

    if (isBeltLike) {
        // 2.5D Conveyor Track
        val trackColor = Color(type.primaryColorHex)
        val railColor = Color(type.roofColorHex)
        val pad = tilePx * 0.14f
        val beltSide = max(2f, tilePx - pad * 2)

        // Base shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(left + pad, top + pad + 3f * zoom),
            size = Size(beltSide, beltSide),
            cornerRadius = CornerRadius(4f * zoom, 4f * zoom)
        )

        // Track body
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(left + pad, top + pad),
            size = Size(beltSide, beltSide),
            cornerRadius = CornerRadius(4f * zoom, 4f * zoom)
        )

        // Side rails
        drawRoundRect(
            color = railColor,
            topLeft = Offset(left + pad, top + pad),
            size = Size(beltSide, beltSide),
            cornerRadius = CornerRadius(4f * zoom, 4f * zoom),
            style = Stroke(width = max(2f, 3f * zoom))
        )

        // Animated directional chevron arrow
        val shift = (animPhase - 0.5f) * tilePx * 0.24f
        drawDirectionArrow(
            cx = left + tilePx * 0.5f + building.direction.dx * shift,
            cy = top + tilePx * 0.5f + building.direction.dy * shift,
            tilePx = tilePx * 0.72f,
            direction = building.direction,
            color = Color(0xFFE2E8F0).copy(alpha = 0.85f)
        )

        // Draw Item moving smoothly along the belt!
        val item = building.beltItem
        if (item != null) {
            val prog = building.beltProgress.coerceIn(0f, 1f)
            val startX = left + tilePx * 0.5f - building.direction.dx * (tilePx * 0.35f)
            val startY = top + tilePx * 0.5f - building.direction.dy * (tilePx * 0.35f)
            val itemX = startX + building.direction.dx * (tilePx * 0.70f * prog)
            val itemY = startY + building.direction.dy * (tilePx * 0.70f * prog)
            val itemSize = max(2f, tilePx * 0.38f)

            // Item 2.5D shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(itemX - itemSize / 2f, itemY - itemSize / 2f + 3f * zoom),
                size = Size(itemSize, itemSize),
                cornerRadius = CornerRadius(3f * zoom, 3f * zoom)
            )
            // Item 2.5D Pixel Crate/Ingot
            drawRoundRect(
                color = item.composeColor,
                topLeft = Offset(itemX - itemSize / 2f, itemY - itemSize / 2f),
                size = Size(itemSize, itemSize),
                cornerRadius = CornerRadius(3f * zoom, 3f * zoom)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(itemX - itemSize / 2f, itemY - itemSize / 2f),
                size = Size(itemSize, itemSize),
                cornerRadius = CornerRadius(3f * zoom, 3f * zoom),
                style = Stroke(width = max(1f, 1.3f * zoom))
            )
        }
    } else {
        // 2.5D Extruded Industrial Structure
        val wallColor = Color(type.primaryColorHex)
        val roofColor = Color(type.roofColorHex)
        val pad = tilePx * 0.07f
        val extrude = tilePx * 0.14f
        val structWidth = max(2f, tilePx - pad * 2)
        val roofHeight = max(2f, tilePx - pad * 2 - extrude * 0.55f)

        // Drop shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(left + pad + 2f * zoom, top + pad + 4f * zoom),
            size = Size(structWidth, structWidth),
            cornerRadius = CornerRadius(6f * zoom, 6f * zoom)
        )

        // Front 2.5D extruded wall face (lower block)
        drawRoundRect(
            color = wallColor,
            topLeft = Offset(left + pad, top + pad),
            size = Size(structWidth, structWidth),
            cornerRadius = CornerRadius(6f * zoom, 6f * zoom)
        )

        // Elevated 2.5D roof face (shifted slightly up to expose front wall height)
        drawRoundRect(
            color = roofColor,
            topLeft = Offset(left + pad, top + pad - extrude * 0.45f),
            size = Size(structWidth, roofHeight),
            cornerRadius = CornerRadius(6f * zoom, 6f * zoom)
        )

        // Pixel-art roof border highlight
        drawRoundRect(
            color = Color.White.copy(alpha = 0.28f),
            topLeft = Offset(left + pad, top + pad - extrude * 0.45f),
            size = Size(structWidth, roofHeight),
            cornerRadius = CornerRadius(6f * zoom, 6f * zoom),
            style = Stroke(width = max(1.2f, 1.8f * zoom))
        )

        val centerRoofX = left + tilePx * 0.5f
        val centerRoofY = top + tilePx * 0.42f
        val isWorking = building.operationalState == OperationalState.ACTIVE ||
            building.operationalState == OperationalState.LOW_POWER

        // Machine-specific 2.5D Pixel Art Details & Animations
        when (type) {
            BuildingType.EXTRACTOR_MK1, BuildingType.EXTRACTOR_MK2 -> {
                val angle = if (isWorking) animPhase * 360f else 0f
                val r = tilePx * 0.18f
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = r,
                    center = Offset(centerRoofX, centerRoofY)
                )
                val rad = Math.toRadians(angle.toDouble())
                val dx = (cos(rad) * r * 0.8f).toFloat()
                val dy = (sin(rad) * r * 0.8f).toFloat()
                drawLine(
                    color = Color(0xFFFDE047),
                    start = Offset(centerRoofX - dx, centerRoofY - dy),
                    end = Offset(centerRoofX + dx, centerRoofY + dy),
                    strokeWidth = 3f * zoom
                )
            }

            BuildingType.SMELTER -> {
                // Glowing furnace chamber
                val flicker = if (isWorking) 0.65f + 0.35f * sin(animPhase * 6.28f) else 0.2f
                drawRoundRect(
                    color = Color(0xFF1E1B18),
                    topLeft = Offset(centerRoofX - tilePx * 0.20f, centerRoofY - tilePx * 0.16f),
                    size = Size(tilePx * 0.40f, tilePx * 0.30f),
                    cornerRadius = CornerRadius(3f * zoom, 3f * zoom)
                )
                drawCircle(
                    color = Color(0xFFF97316).copy(alpha = flicker),
                    radius = tilePx * 0.13f,
                    center = Offset(centerRoofX, centerRoofY)
                )
                if (isWorking && !reducedEffects) {
                    // Rising smoke pixel puff
                    val smokeY = centerRoofY - tilePx * (0.18f + animPhase * 0.25f)
                    drawCircle(
                        color = Color(0xFFCBD5E1).copy(alpha = (1f - animPhase) * 0.6f),
                        radius = tilePx * (0.06f + animPhase * 0.06f),
                        center = Offset(centerRoofX + tilePx * 0.1f, smokeY)
                    )
                }
            }

            BuildingType.SAWMILL, BuildingType.CUTTER -> {
                val slide = if (isWorking) sin(animPhase * 6.28f) * tilePx * 0.1f else 0f
                drawRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset(centerRoofX - tilePx * 0.18f + slide, centerRoofY - tilePx * 0.06f),
                    size = Size(tilePx * 0.36f, tilePx * 0.12f)
                )
            }

            BuildingType.ASSEMBLER, BuildingType.MANUFACTURER -> {
                // Robotic cross-arms
                val armShift = if (isWorking) sin(animPhase * 6.28f) * tilePx * 0.08f else 0f
                drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(centerRoofX - tilePx * 0.22f, centerRoofY - tilePx * 0.18f),
                    size = Size(tilePx * 0.44f, tilePx * 0.34f)
                )
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = tilePx * 0.09f,
                    center = Offset(centerRoofX + armShift, centerRoofY)
                )
            }

            BuildingType.BIOMASS_GEN, BuildingType.SOLAR_PANEL,
            BuildingType.GEOTHERMAL_PLANT, BuildingType.POWER_CONDUIT -> {
                val pulse = 0.55f + 0.45f * sin(animPhase * 6.28f)
                drawCircle(
                    color = Color(0xFFFEF08A).copy(alpha = pulse),
                    radius = tilePx * 0.15f,
                    center = Offset(centerRoofX, centerRoofY)
                )
            }

            BuildingType.SELL_HUB -> {
                if (zoom >= 0.6f) {
                    safeDrawText(
                        textMeasurer = textMeasurer,
                        text = "$",
                        topLeft = Offset(centerRoofX - tilePx * 0.12f, centerRoofY - tilePx * 0.20f),
                        style = TextStyle(
                            color = Color(0xFFFEF08A),
                            fontSize = (15f * zoom.coerceIn(0.75f, 1.4f)).sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            BuildingType.STORAGE -> {
                val fillRatio = (building.totalStoredItems().toFloat() / storageCap.toFloat()).coerceIn(0f, 1f)
                val barW = tilePx * 0.56f
                val barH = tilePx * 0.12f
                drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(centerRoofX - barW / 2f, centerRoofY - barH / 2f),
                    size = Size(barW, barH)
                )
                drawRect(
                    color = if (fillRatio > 0.9f) Color(0xFFF97316) else Color(0xFF38BDF8),
                    topLeft = Offset(centerRoofX - barW / 2f, centerRoofY - barH / 2f),
                    size = Size(barW * fillRatio, barH)
                )
            }

            else -> {}
        }

        // Output direction arrow for directional machines
        if (type != BuildingType.SELL_HUB &&
            type != BuildingType.BIOMASS_GEN &&
            type != BuildingType.SOLAR_PANEL &&
            type != BuildingType.GEOTHERMAL_PLANT &&
            type != BuildingType.POWER_CONDUIT
        ) {
            drawDirectionArrow(
                cx = left + tilePx * 0.5f + building.direction.dx * tilePx * 0.26f,
                cy = top + tilePx * 0.44f + building.direction.dy * tilePx * 0.26f,
                tilePx = tilePx * 0.45f,
                direction = building.direction,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        // Progress bar at bottom of active processing/extraction machines
        if (building.craftProgress > 0.02f) {
            val pBarW = tilePx * 0.68f
            val pBarH = max(3f, 4.5f * zoom)
            val barLeft = left + (tilePx - pBarW) / 2f
            val barTop = top + tilePx * 0.78f
            drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                topLeft = Offset(barLeft, barTop),
                size = Size(pBarW, pBarH)
            )
            drawRect(
                color = Color(0xFF4ADE80),
                topLeft = Offset(barLeft, barTop),
                size = Size(pBarW * building.craftProgress.coerceIn(0f, 1f), pBarH)
            )
        }

        // Operational / Power Status LED Indicator (top-right corner of structure)
        val statusColor = Color(building.operationalState.colorHex)
        drawCircle(
            color = Color.Black.copy(alpha = 0.75f),
            radius = max(4f, 6.5f * zoom),
            center = Offset(left + tilePx * 0.80f, top + tilePx * 0.20f)
        )
        drawCircle(
            color = statusColor,
            radius = max(3f, 4.8f * zoom),
            center = Offset(left + tilePx * 0.80f, top + tilePx * 0.20f)
        )

        if (building.isOffline && zoom >= 0.65f) {
            val badgeW = tilePx * 0.68f
            val badgeH = tilePx * 0.24f
            val bx = left + (tilePx - badgeW) / 2f
            val by = top + tilePx * 0.68f
            drawRoundRect(
                color = Color(0xFF991B1B).copy(alpha = 0.92f),
                topLeft = Offset(bx, by),
                size = Size(badgeW, badgeH),
                cornerRadius = CornerRadius(3f * zoom, 3f * zoom)
            )
            safeDrawText(
                textMeasurer = textMeasurer,
                text = "OFF",
                topLeft = Offset(bx + tilePx * 0.12f, by + tilePx * 0.01f),
                style = TextStyle(
                    color = Color.White,
                    fontSize = (8.5f * zoom.coerceIn(0.8f, 1.25f)).sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }

    // Selection Highlight
    if (isSelected) {
        val selSide = max(2f, tilePx - 2f)
        drawRoundRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(left + 1f, top + 1f),
            size = Size(selSide, selSide),
            cornerRadius = CornerRadius(6f * zoom, 6f * zoom),
            style = Stroke(width = 3f * zoom)
        )
    }
}

private fun DrawScope.drawDirectionArrow(
    cx: Float,
    cy: Float,
    tilePx: Float,
    direction: Direction,
    color: Color
) {
    val s = tilePx * 0.22f
    val path = Path().apply {
        when (direction) {
            Direction.EAST -> {
                moveTo(cx + s, cy)
                lineTo(cx - s * 0.7f, cy - s * 0.75f)
                lineTo(cx - s * 0.7f, cy + s * 0.75f)
                close()
            }
            Direction.WEST -> {
                moveTo(cx - s, cy)
                lineTo(cx + s * 0.7f, cy - s * 0.75f)
                lineTo(cx + s * 0.7f, cy + s * 0.75f)
                close()
            }
            Direction.SOUTH -> {
                moveTo(cx, cy + s)
                lineTo(cx - s * 0.75f, cy - s * 0.7f)
                lineTo(cx + s * 0.75f, cy - s * 0.7f)
                close()
            }
            Direction.NORTH -> {
                moveTo(cx, cy - s)
                lineTo(cx - s * 0.75f, cy + s * 0.7f)
                lineTo(cx + s * 0.75f, cy + s * 0.7f)
                close()
            }
        }
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawBiomeBorders(
    panOffset: Offset,
    tilePx: Float,
    zoom: Float,
    unlockedTechs: Set<String>,
    textMeasurer: TextMeasurer
) {
    // Vertical border at x = 17
    val vx = panOffset.x + 17 * tilePx
    drawLine(
        color = Color.White.copy(alpha = 0.28f),
        start = Offset(vx, panOffset.y),
        end = Offset(vx, panOffset.y + GameCatalog.MAP_HEIGHT * tilePx),
        strokeWidth = 2.5f * zoom,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
    )

    // Horizontal borders
    val hyLeft = panOffset.y + 13 * tilePx
    drawLine(
        color = Color.White.copy(alpha = 0.28f),
        start = Offset(panOffset.x, hyLeft),
        end = Offset(vx, hyLeft),
        strokeWidth = 2.5f * zoom,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
    )
    val hyRight = panOffset.y + 12 * tilePx
    drawLine(
        color = Color.White.copy(alpha = 0.28f),
        start = Offset(vx, hyRight),
        end = Offset(panOffset.x + GameCatalog.MAP_WIDTH * tilePx, hyRight),
        strokeWidth = 2.5f * zoom,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
    )

    if (zoom >= 0.6f) {
        val biomeHeaders = listOf(
            Triple(BiomeType.TEMPERATE_VALLEY, 1, 1),
            Triple(BiomeType.SCORCHED_DUNES, 18, 1),
            Triple(BiomeType.FROZEN_TUNDRA, 1, 14),
            Triple(BiomeType.VOLCANIC_CALDERA, 18, 13)
        )
        for ((biome, tx, ty) in biomeHeaders) {
            val bx = panOffset.x + tx * tilePx
            val by = panOffset.y + ty * tilePx
            val locked = biome.requiredTechId != null && biome.requiredTechId !in unlockedTechs
            val tag = if (locked) "🔒 ${biome.displayName}" else "✓ ${biome.displayName}"
            safeDrawText(
                textMeasurer = textMeasurer,
                text = tag,
                topLeft = Offset(bx, by),
                style = TextStyle(
                    color = Color(biome.accentHex),
                    fontSize = (11f * zoom.coerceIn(0.8f, 1.25f)).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}

private fun DrawScope.drawBiomeWeatherParticles(
    panOffset: Offset,
    tilePx: Float,
    animPhase: Float,
    weatherPhase: Float
) {
    // Lightweight pixel particles for Desert Sandstorm, Tundra Snow, and Volcanic Embers
    for (i in 0 until 12) {
        val drift = (animPhase + i * 0.13f + weatherPhase * 0.1f) % 1f
        // Desert sand particle
        val dx = panOffset.x + (18f + (i * 1.3f + drift * 6f) % 15f) * tilePx
        val dy = panOffset.y + (1f + (i * 0.9f + drift * 2f) % 10f) * tilePx
        if (dx in 0f..(size.width - 6f) && dy in 0f..(size.height - 6f)) {
            drawRect(
                color = Color(0xFFFDE047).copy(alpha = 0.28f),
                topLeft = Offset(dx, dy),
                size = Size(5f, 3f)
            )
        }

        // Tundra snow particle
        val sx = panOffset.x + (1f + (i * 1.4f + drift * 2f) % 15f) * tilePx
        val sy = panOffset.y + (13.5f + (i * 0.8f + drift * 4f) % 9.5f) * tilePx
        if (sx in 0f..(size.width - 6f) && sy in 0f..(size.height - 6f)) {
            drawRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(sx, sy),
                size = Size(4f, 4f)
            )
        }

        // Volcanic ember particle
        val vx = panOffset.x + (18f + (i * 1.2f + drift * 2f) % 15f) * tilePx
        val vy = panOffset.y + (22f - (i * 0.9f + drift * 5f) % 9.5f) * tilePx
        if (vx in 0f..(size.width - 6f) && vy in 0f..(size.height - 6f)) {
            drawRect(
                color = Color(0xFFF97316).copy(alpha = 0.40f),
                topLeft = Offset(vx, vy),
                size = Size(4f, 4f)
            )
        }
    }
}

/**
 * Draws the visual Power Grid Overlay highlighting:
 * - Energized vs unpowered power transmission lines (`PowerLink`)
 * - Color-coded highlights for every generator, conduit, and power-consuming machine:
 *   - Emerald Green (`#22C55E`): Connected & Powered machine / active generator
 *   - Amber Orange (`#F59E0B`): Connected to grid, but offline due to insufficient power
 *   - Crimson Red (`#EF4444`): Unconnected / isolated from the power grid (offline)
 *   - Sky Cyan (`#38BDF8`): Active Power Conduit relay
 */
private fun DrawScope.drawPowerGridVisualOverlay(
    uiState: FactoryUiState,
    panOffset: Offset,
    tilePx: Float,
    zoom: Float,
    animPhase: Float,
    minX: Int,
    maxX: Int,
    minY: Int,
    maxY: Int,
    textMeasurer: TextMeasurer
) {
    val pulseAlpha = 0.55f + 0.35f * sin(animPhase * 6.28318f)

    // 1. Draw Power Transmission Links between Generators, Conduits, and Machines
    for (link in uiState.powerLinks) {
        val start = Offset(
            x = panOffset.x + (link.fromX + 0.5f) * tilePx,
            y = panOffset.y + (link.fromY + 0.35f) * tilePx
        )
        val end = Offset(
            x = panOffset.x + (link.toX + 0.5f) * tilePx,
            y = panOffset.y + (link.toY + 0.35f) * tilePx
        )
        if (link.isEnergized) {
            // Glowing outer beam for energized connection
            drawLine(
                color = Color(0xFF22C55E).copy(alpha = 0.28f),
                start = start,
                end = end,
                strokeWidth = max(4.5f, 6.0f * zoom)
            )
            // Crisp inner core line
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.88f),
                start = start,
                end = end,
                strokeWidth = max(2f, 2.6f * zoom)
            )
            if (!uiState.reducedEffects) {
                val pulsePos = Offset(
                    x = start.x + (end.x - start.x) * animPhase,
                    y = start.y + (end.y - start.y) * animPhase
                )
                drawCircle(
                    color = Color(0xFF86EFAC),
                    radius = max(2.8f, 4.2f * zoom),
                    center = pulsePos
                )
            }
        } else {
            // Unpowered / overloaded link (dashed amber-red line)
            drawLine(
                color = Color(0xFFEF4444).copy(alpha = 0.85f),
                start = start,
                end = end,
                strokeWidth = max(2.2f, 2.8f * zoom),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
            )
        }
    }

    // 2. Draw Per-Machine & Per-Node Power Status Highlight Frames & Badges
    for (y in minY..maxY) {
        for (x in minX..maxX) {
            val b = uiState.buildings[x to y] ?: continue
            val isConsumer = b.type.powerConsumptionKw > 0
            val isGenerator = b.type.powerGenerationKw > 0
            val isConduit = b.type == BuildingType.POWER_CONDUIT
            if (!isConsumer && !isGenerator && !isConduit) continue

            val left = panOffset.x + x * tilePx
            val top = panOffset.y + y * tilePx
            val pad = max(2f, 3f * zoom)
            val boxSide = max(4f, tilePx - pad * 2f)

            val (frameColor, badgeText, isDashed) = when {
                b.isPaused -> Triple(Color(0xFF94A3B8), "PAUSA", true)
                isGenerator -> Triple(Color(0xFF4ADE80), "+${b.type.powerGenerationKw}kW", false)
                isConduit -> {
                    if (b.powerState == PowerState.POWERED) {
                        Triple(Color(0xFF38BDF8), "REDE OK", false)
                    } else {
                        Triple(Color(0xFFEF4444), "SEM REDE", true)
                    }
                }
                b.powerState == PowerState.POWERED -> {
                    // Powered consumer machine -> vibrant emerald green
                    Triple(Color(0xFF22C55E), "⚡ -${b.type.powerConsumptionKw}kW", false)
                }
                b.powerState == PowerState.OFFLINE || b.powerState == PowerState.BROWN_OUT -> {
                    // Connected to grid but insufficient power -> amber-orange warning
                    Triple(Color(0xFFF59E0B), "FALTA kW", true)
                }
                else -> {
                    // Disconnected from grid (PowerState.UNCONNECTED) -> crimson red offline
                    Triple(Color(0xFFEF4444), "SEM REDE", true)
                }
            }

            val fillAlpha = if (b.isOffline) 0.22f * pulseAlpha else 0.14f
            val strokeAlpha = if (b.isOffline) pulseAlpha.coerceIn(0.65f, 1f) else 0.92f

            // Translucent color-coded tint over the machine footprint
            drawRoundRect(
                color = frameColor.copy(alpha = fillAlpha),
                topLeft = Offset(left + pad, top + pad),
                size = Size(boxSide, boxSide),
                cornerRadius = CornerRadius(6f * zoom, 6f * zoom)
            )

            // Distinct color-coded border (solid for powered, dashed for unpowered/offline)
            drawRoundRect(
                color = frameColor.copy(alpha = strokeAlpha),
                topLeft = Offset(left + pad, top + pad),
                size = Size(boxSide, boxSide),
                cornerRadius = CornerRadius(6f * zoom, 6f * zoom),
                style = Stroke(
                    width = max(2f, 2.8f * zoom),
                    pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) else null
                )
            )

            // Connection node indicator at top-center of the machine
            val nodeCenter = Offset(left + tilePx * 0.5f, top + tilePx * 0.35f)
            drawCircle(
                color = Color(0xFF0F172A),
                radius = max(3.5f, 5f * zoom),
                center = nodeCenter
            )
            drawCircle(
                color = frameColor,
                radius = max(2.2f, 3.4f * zoom),
                center = nodeCenter
            )

            // Compact overlay label pill at top of tile when zoom is readable
            if (zoom >= 0.65f) {
                val pillW = tilePx * 0.86f
                val pillH = tilePx * 0.22f
                val pillX = left + (tilePx - pillW) / 2f
                val pillY = top + tilePx * 0.03f
                drawRoundRect(
                    color = Color(0xFF090D16).copy(alpha = 0.88f),
                    topLeft = Offset(pillX, pillY),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(3f * zoom, 3f * zoom)
                )
                drawRoundRect(
                    color = frameColor.copy(alpha = 0.85f),
                    topLeft = Offset(pillX, pillY),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(3f * zoom, 3f * zoom),
                    style = Stroke(width = max(1f, 1.2f * zoom))
                )
                safeDrawText(
                    textMeasurer = textMeasurer,
                    text = badgeText,
                    topLeft = Offset(pillX + tilePx * 0.06f, pillY + tilePx * 0.01f),
                    style = TextStyle(
                        color = frameColor,
                        fontSize = (7.5f * zoom.coerceIn(0.8f, 1.25f)).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}
