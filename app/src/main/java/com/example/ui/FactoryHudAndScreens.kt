package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameCatalog
import com.example.model.*
import com.example.viewmodel.ActiveModal
import com.example.viewmodel.AppScreen
import com.example.viewmodel.FactoryUiState
import com.example.viewmodel.FactoryViewModel

@Composable
fun FactoryAppRoot(viewModel: FactoryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.currentScreen == AppScreen.GAMEPLAY || uiState.activeModal != ActiveModal.NONE) {
        when {
            uiState.activeModal != ActiveModal.NONE -> viewModel.closeModal()
            uiState.buildTool != null || uiState.isDemolishMode || uiState.selectedTile != null -> {
                viewModel.cancelBuildOrSelection()
            }
            uiState.currentScreen == AppScreen.GAMEPLAY -> viewModel.returnToMainMenu()
        }
    }

    if (uiState.currentScreen == AppScreen.MAIN_MENU) {
        MainMenuScreen(
            uiState = uiState,
            onContinueGame = { viewModel.continueSavedGame() },
            onStartNewGameRequest = {
                if (uiState.hasSavedGame) {
                    viewModel.openModal(ActiveModal.CONFIRM_NEW_GAME)
                } else {
                    viewModel.startNewGameConfirmed()
                }
            },
            onOpenGuide = { viewModel.openModal(ActiveModal.CATALOG_HELP) },
            onOpenSettings = { viewModel.openModal(ActiveModal.SETTINGS) }
        )
    } else {
        GameplayScreen(
            uiState = uiState,
            viewModel = viewModel
        )
    }

    // Active Modals
    when (uiState.activeModal) {
        ActiveModal.CONFIRM_NEW_GAME -> {
            ConfirmNewGameDialog(
                onConfirm = { viewModel.startNewGameConfirmed() },
                onDismiss = { viewModel.closeModal() }
            )
        }
        ActiveModal.RESEARCH -> {
            ResearchModal(
                uiState = uiState,
                onStartResearch = { viewModel.startResearch(it) },
                onClose = { viewModel.closeModal() }
            )
        }
        ActiveModal.OBJECTIVES -> {
            ObjectivesAndEconomyModal(
                uiState = uiState,
                onClose = { viewModel.closeModal() }
            )
        }
        ActiveModal.CATALOG_HELP, ActiveModal.BIOME_MAP_INFO -> {
            CatalogAndBiomeHelpModal(
                uiState = uiState,
                onJumpToBiome = { biome ->
                    val target = when (biome) {
                        BiomeType.TEMPERATE_VALLEY -> 7 to 6
                        BiomeType.SCORCHED_DUNES -> 23 to 5
                        BiomeType.FROZEN_TUNDRA -> 7 to 17
                        BiomeType.VOLCANIC_CALDERA -> 23 to 17
                    }
                    viewModel.focusCameraOn(target.first, target.second)
                },
                onRestartTutorial = { viewModel.restartTutorial() },
                onClose = { viewModel.closeModal() }
            )
        }
        ActiveModal.POWER_SYSTEM -> {
            PowerSystemModal(
                uiState = uiState,
                onToggleOverlay = { viewModel.togglePowerOverlay() },
                onFocusMachine = { x, y ->
                    viewModel.closeModal()
                    viewModel.focusCameraOn(x, y)
                    viewModel.onTileTapped(x, y)
                },
                onClose = { viewModel.closeModal() }
            )
        }
        ActiveModal.SETTINGS -> {
            SettingsModal(
                uiState = uiState,
                onUpdateAudio = { sfx, music, muted ->
                    viewModel.updateAudioSettings(sfx, music, muted)
                },
                onUpdateAccessibility = { reduced, scale ->
                    viewModel.updateAccessibilitySettings(reduced, scale)
                },
                onManualSave = { viewModel.saveGameManual() },
                onReturnToMainMenu = { viewModel.returnToMainMenu() },
                onClose = { viewModel.closeModal() }
            )
        }
        ActiveModal.NONE -> {}
    }
}

@Composable
private fun GameplayScreen(
    uiState: FactoryUiState,
    viewModel: FactoryViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1017))
            .testTag("gameplay_screen")
    ) {
        // 1. 2.5D Interactive World Map Canvas
        FactoryCanvas25D(
            uiState = uiState,
            onTileTapped = { x, y -> viewModel.onTileTapped(x, y) },
            onCameraMoved = { viewModel.onCameraMovedByPlayer() },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Industrial HUD Bar
        TopIndustrialHud(
            uiState = uiState,
            onTogglePause = { viewModel.togglePauseSimulation() },
            onCycleSpeed = { viewModel.cycleSimSpeed() },
            onOpenPowerModal = { viewModel.openModal(ActiveModal.POWER_SYSTEM) },
            onTogglePowerOverlay = { viewModel.togglePowerOverlay() },
            onOpenResearch = { viewModel.openModal(ActiveModal.RESEARCH) },
            onOpenObjectives = { viewModel.openModal(ActiveModal.OBJECTIVES) },
            onOpenGuide = { viewModel.openModal(ActiveModal.CATALOG_HELP) },
            onManualSave = { viewModel.saveGameManual() },
            onOpenSettings = { viewModel.openModal(ActiveModal.SETTINGS) },
            onJumpBiome = { x, y -> viewModel.focusCameraOn(x, y) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        // 2b. Floating Power Grid Visual Overlay Legend (Bottom-Start above Construction Bar)
        if (uiState.showPowerGridOverlay) {
            PowerGridOverlayLegendCard(
                uiState = uiState,
                onOpenPowerDetails = { viewModel.openModal(ActiveModal.POWER_SYSTEM) },
                onFocusFirstOffline = {
                    val offlineNode = uiState.consumerNodes.firstOrNull { !it.isPowered }
                    if (offlineNode != null) {
                        viewModel.focusCameraOn(offlineNode.x, offlineNode.y)
                        viewModel.onTileTapped(offlineNode.x, offlineNode.y)
                    }
                },
                onHideOverlay = { viewModel.togglePowerOverlay() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 12.dp, bottom = 92.dp)
            )
        }

        // 3. Status Notification Banner
        AnimatedVisibility(
            visible = uiState.statusBannerMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .clickable { viewModel.dismissBanner() }
                    .testTag("status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.statusBannerMessage ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. Skippable Guided Tutorial Panel (Top-Left floating card)
        if (!uiState.tutorialCompletedOrSkipped) {
            TutorialOverlayCard(
                step = uiState.tutorialStep,
                onSkip = { viewModel.skipTutorial() },
                onActionFocus = { x, y -> viewModel.focusCameraOn(x, y) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 60.dp, start = 12.dp)
            )
        }

        // 5. Selected Machine / Tile Inspector Panel (Right floating card)
        val selPos = uiState.selectedTile
        if (selPos != null) {
            val selBuilding = uiState.buildings[selPos]
            val selMapTile = uiState.mapTiles.find { it.x == selPos.first && it.y == selPos.second }
            SelectionInspectorPanel(
                tile = selMapTile,
                building = selBuilding,
                unlockedTechIds = uiState.unlockedTechIds,
                onSelectRecipe = { recipeId -> viewModel.setBuildingRecipe(selPos.first, selPos.second, recipeId) },
                onSelectFilterItem = { item -> viewModel.setFilterItem(selPos.first, selPos.second, item) },
                onTogglePause = { viewModel.toggleBuildingPause(selPos.first, selPos.second) },
                onRotateBuilding = { viewModel.rotateBuildDirection() },
                onSellStored = { viewModel.sellStoredItemsAt(selPos.first, selPos.second) },
                onDemolish = { viewModel.demolishAt(selPos.first, selPos.second) },
                onClose = { viewModel.cancelBuildOrSelection() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(end = 12.dp, top = 52.dp, bottom = 84.dp)
            )
        }

        // 6. Bottom Construction & Tool Bar
        ConstructionBottomBar(
            uiState = uiState,
            onSelectCategory = { viewModel.selectCategory(it) },
            onSelectTool = { viewModel.selectBuildTool(it) },
            onRotate = { viewModel.rotateBuildDirection() },
            onConfirmPreview = { viewModel.confirmPreviewPlacement() },
            onUndo = { viewModel.undoLastAction() },
            onToggleDemolish = { viewModel.toggleDemolishMode() },
            onCancel = { viewModel.cancelBuildOrSelection() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
        )
    }
}

@Composable
private fun TopIndustrialHud(
    uiState: FactoryUiState,
    onTogglePause: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenPowerModal: () -> Unit,
    onTogglePowerOverlay: () -> Unit,
    onOpenResearch: () -> Unit,
    onOpenObjectives: () -> Unit,
    onOpenGuide: () -> Unit,
    onManualSave: () -> Unit,
    onOpenSettings: () -> Unit,
    onJumpBiome: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.94f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Money & Income Rate
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFF064E3B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("hud_money_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$${uiState.money}",
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (uiState.recentIncomePerMin > 0) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "+$${uiState.recentIncomePerMin}/m",
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Power Grid Status Pill
                val powerOk = uiState.totalPowerGeneratedKw >= uiState.totalPowerDemandKw && uiState.offlineMachineCount == 0
                val powerColor = when {
                    uiState.offlineMachineCount > 0 -> Color(0xFFEF4444)
                    uiState.totalPowerDemandKw == 0 && uiState.totalPowerGeneratedKw == 0 -> Color(0xFF94A3B8)
                    powerOk -> Color(0xFF38BDF8)
                    else -> Color(0xFFF59E0B)
                }
                Surface(
                    color = powerColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, powerColor.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { onOpenPowerModal() }
                        .testTag("hud_power_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = "Rede Elétrica",
                            tint = powerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${uiState.totalPowerDemandKw}/${uiState.totalPowerGeneratedKw} kW",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (uiState.offlineMachineCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "• ${uiState.offlineMachineCount} OFFLINE",
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.testTag("offline_machine_badge")
                            )
                        }
                    }
                }

                // Direct 1-tap Power Grid Visual Overlay Toggle Button
                val overlayActive = uiState.showPowerGridOverlay
                Surface(
                    color = if (overlayActive) Color(0xFF0369A1) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (overlayActive) Color(0xFF38BDF8) else Color(0xFF475569)
                    ),
                    modifier = Modifier
                        .clickable { onTogglePowerOverlay() }
                        .testTag("hud_power_overlay_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "Alternar Malha Elétrica",
                            tint = if (overlayActive) Color(0xFF7DD3FC) else Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (overlayActive) "Malha: ON" else "Malha: OFF",
                            color = if (overlayActive) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Quick Biome Jump Pills
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BiomeMiniButton("Vale", Color(0xFF4ADE80)) { onJumpBiome(7, 6) }
                        BiomeMiniButton("Dunas", Color(0xFFFBBF24)) { onJumpBiome(22, 5) }
                        BiomeMiniButton("Tundra", Color(0xFF38BDF8)) { onJumpBiome(7, 17) }
                        BiomeMiniButton("Vulcão", Color(0xFFF97316)) { onJumpBiome(22, 17) }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Simulation Controls & Modal Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pause / Play
                FilledTonalIconButton(
                    onClick = onTogglePause,
                    modifier = Modifier.size(36.dp).testTag("hud_pause_button")
                ) {
                    Icon(
                        if (uiState.isSimPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (uiState.isSimPaused) "Continuar" else "Pausar",
                        tint = if (uiState.isSimPaused) Color(0xFFFBBF24) else Color.White
                    )
                }

                // Speed Multiplier (1x / 2x / 3x)
                OutlinedButton(
                    onClick = onCycleSpeed,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(36.dp).testTag("hud_speed_button")
                ) {
                    Text(
                        text = "${uiState.simSpeedMultiplier}x",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }

                // Research Button
                val activeTech = GameCatalog.techTree.find { it.id == uiState.activeResearchId }
                Button(
                    onClick = onOpenResearch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp).testTag("hud_research_button")
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (activeTech != null) {
                            "Pesq: ${(uiState.activeResearchProgressSec / activeTech.researchSeconds * 100).toInt()}%"
                        } else {
                            "Pesquisa"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Objectives Button
                val doneObj = uiState.objectives.count { it.isCompleted }
                Button(
                    onClick = onOpenObjectives,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp).testTag("hud_objectives_button")
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Metas $doneObj/${uiState.objectives.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Guide Button
                IconButton(
                    onClick = onOpenGuide,
                    modifier = Modifier.size(36.dp).testTag("hud_guide_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Guia", tint = Color(0xFF38BDF8))
                }

                // Save Button
                IconButton(
                    onClick = onManualSave,
                    modifier = Modifier.size(36.dp).testTag("hud_save_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Salvar Partida", tint = Color(0xFF4ADE80))
                }

                // Settings / Menu Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp).testTag("hud_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = Color(0xFFCBD5E1))
                }
            }
        }
    }
}

@Composable
private fun BiomeMiniButton(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
private fun TutorialOverlayCard(
    step: Int,
    onSkip: () -> Unit,
    onActionFocus: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, instruction) = when (step) {
        0 -> "Passo 1/6: Câmera Livre" to "Arraste com 1 dedo para mover o mapa ou faça pinça com 2 dedos para dar zoom."
        1 -> "Passo 2/6: Extrair Minério" to "Selecione 'Extratora MK1' e toque duas vezes sobre a jazida de Ferro (Fe) próxima ao Dínamo."
        2 -> "Passo 3/6: Conectar Esteiras" to "Escolha 'Esteira MK1' na aba Logística, use 'Girar' se necessário e ligue a Extratora até o Terminal ($)."
        3 -> "Passo 4/6: Primeira Venda" to "Observe os blocos de minério percorrerem a esteira automaticamente até o Terminal de Venda ($)!"
        4 -> "Passo 5/6: Refinar Metal" to "Na aba Produção, posicione uma 'Fundição Térmica' dentro do alcance elétrico (azul) para transformar minério em Barras."
        else -> "Passo 6/6: Vender Produto Refinado" to "Conecte a saída da Fundição ao Terminal ($) para vender Barras por 3x mais valor!"
    }

    Card(
        modifier = modifier
            .widthIn(max = 310.dp)
            .testTag("tutorial_overlay_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.94f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
                TextButton(
                    onClick = onSkip,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp).testTag("skip_tutorial_button")
                ) {
                    Text("Pular Tutorial", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
            Text(
                text = instruction,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📍 Centralizar na Base",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onActionFocus(7, 6) }
                )
            }
        }
    }
}

@Composable
private fun SelectionInspectorPanel(
    tile: MapTile?,
    building: PlacedBuilding?,
    unlockedTechIds: Set<String>,
    onSelectRecipe: (String) -> Unit,
    onSelectFilterItem: (ItemType) -> Unit,
    onTogglePause: () -> Unit,
    onRotateBuilding: () -> Unit,
    onSellStored: () -> Unit,
    onDemolish: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(265.dp)
            .testTag("selection_inspector_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.96f)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = building?.type?.displayName ?: (tile?.deposit?.displayName ?: "Terreno"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                }
            }

            if (tile != null) {
                Text(
                    text = "Bioma: ${tile.biome.displayName} (${tile.x}, ${tile.y})",
                    color = Color(tile.biome.accentHex),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (building == null) {
                if (tile != null && tile.deposit != DepositType.NONE) {
                    Text(
                        text = "Jazida: ${tile.deposit.displayName}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp
                    )
                    tile.deposit.outputItem?.let { item ->
                        Text(
                            text = "Extrai: ${item.displayName} ($${item.sellValue}/un)",
                            color = Color(0xFF4ADE80),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = "Espaço livre para construção.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            } else {
                // Operational State & Power State Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color(building.operationalState.colorHex).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = building.operationalState.label,
                            color = Color(building.operationalState.colorHex),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (building.type.powerConsumptionKw > 0) {
                        Surface(
                            color = Color(building.powerState.colorHex).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⚡ ${building.type.powerConsumptionKw} kW • ${building.powerState.label}",
                                color = Color(building.powerState.colorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    } else if (building.type.powerGenerationKw > 0) {
                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Gera +${building.type.powerGenerationKw} kW",
                                color = Color(0xFF4ADE80),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (building.isOffline) {
                    Surface(
                        color = Color(0xFF7F1D1D).copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inspector_offline_banner")
                    ) {
                        Text(
                            text = if (building.powerState == PowerState.UNCONNECTED) {
                                "⚠ MÁQUINA OFFLINE: Sem conexão com gerador ou Poste Condutor."
                            } else {
                                "⚠ MÁQUINA OFFLINE: Energia insuficiente na rede (${building.type.powerConsumptionKw} kW exigidos)."
                            },
                            color = Color(0xFFFCA5A5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                // Recipe selector for processing machines
                val machineRecipes = GameCatalog.recipes.filter {
                    it.machine == building.type &&
                        (it.requiredTechId == null || it.requiredTechId in unlockedTechIds)
                }
                if (machineRecipes.isNotEmpty()) {
                    Text("Receita Ativa:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        machineRecipes.forEach { rec ->
                            val isSelected = building.selectedRecipeId == rec.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectRecipe(rec.id) },
                                label = { Text(rec.output.displayName, fontSize = 10.sp) }
                            )
                        }
                    }
                    val currentRec = GameCatalog.getRecipeById(building.selectedRecipeId) ?: machineRecipes.first()
                    val inDesc = currentRec.inputs.entries.joinToString(" + ") { "${it.value} ${it.key.shortCode}" }
                    Text(
                        text = "$inDesc → ${currentRec.outputCount} ${currentRec.output.displayName} ($${currentRec.output.sellValue})",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Filter Item Selector for FILTER building
                if (building.type == BuildingType.FILTER) {
                    Text("Item Filtrado (frente):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ItemType.entries.forEach { item ->
                            FilterChip(
                                selected = building.filterItem == item,
                                onClick = { onSelectFilterItem(item) },
                                label = { Text(item.shortCode, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Buffers summary
                if (building.inputBuffer.isNotEmpty() || building.outputBuffer.isNotEmpty()) {
                    val bufText = (building.inputBuffer.entries + building.outputBuffer.entries)
                        .joinToString(", ") { "${it.key.shortCode}: ${it.value}" }
                    Text(
                        text = "Estoque: $bufText",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )
                }

                // Quick actions row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onRotateBuilding,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("Girar (${building.direction.label})", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = onTogglePause,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (building.isPaused) "Ativar" else "Pausar", fontSize = 10.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (building.totalStoredItems() > 0) {
                        Button(
                            onClick = onSellStored,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("Vender Estoque", fontSize = 10.sp)
                        }
                    }

                    Button(
                        onClick = onDemolish,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("demolish_selected_button")
                    ) {
                        Text("Remover (+80%)", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConstructionBottomBar(
    uiState: FactoryUiState,
    onSelectCategory: (BuildingCategory) -> Unit,
    onSelectTool: (BuildingType?) -> Unit,
    onRotate: () -> Unit,
    onConfirmPreview: () -> Unit,
    onUndo: () -> Unit,
    onToggleDemolish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.96f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            // Top Row: Category Tabs + Quick Action Buttons (Rotate, Confirm, Undo, Demolish, Cancel)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BuildingCategory.entries.forEach { cat ->
                        val selected = uiState.selectedCategory == cat && !uiState.isDemolishMode
                        Surface(
                            color = if (selected) Color(0xFF2563EB) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { onSelectCategory(cat) }
                                .testTag("category_tab_${cat.name}")
                        ) {
                            Text(
                                text = cat.displayName,
                                color = if (selected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Quick Construction Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.previewTile != null && uiState.buildTool != null) {
                        Button(
                            onClick = onConfirmPreview,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).testTag("confirm_build_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Construir Aqui", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Rotate Button
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { onRotate() }
                            .testTag("rotate_tool_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RotateRight,
                                contentDescription = "Girar",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = uiState.buildDirection.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Undo Button
                    if (uiState.canUndo) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { onUndo() }
                                .testTag("undo_build_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Desfazer",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Desfazer", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // Demolish Mode Button
                    Surface(
                        color = if (uiState.isDemolishMode) Color(0xFFEF4444) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { onToggleDemolish() }
                            .testTag("demolish_mode_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Demolir",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Remover", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Cancel Build / Selection Button
                    if (uiState.buildTool != null || uiState.isDemolishMode) {
                        Surface(
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { onCancel() }
                                .testTag("cancel_build_button")
                        ) {
                            Text(
                                text = "Cancelar",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Second Row: Buildings in the Selected Category
            val categoryBuildings = BuildingType.entries.filter { it.category == uiState.selectedCategory }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryBuildings.forEach { bType ->
                    val isUnlocked = bType.requiredTechId == null || bType.requiredTechId in uiState.unlockedTechIds
                    val isSelected = uiState.buildTool == bType
                    val canAfford = uiState.money >= bType.cost

                    Surface(
                        color = when {
                            isSelected -> Color(0xFF1E3A8A)
                            !isUnlocked -> Color(0xFF0F172A)
                            else -> Color(0xFF1E293B)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> Color(0xFF38BDF8)
                                !isUnlocked -> Color(0xFF334155)
                                else -> Color(bType.roofColorHex).copy(alpha = 0.6f)
                            }
                        ),
                        modifier = Modifier
                            .clickable { onSelectTool(if (isSelected) null else bType) }
                            .testTag("build_card_${bType.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color(bType.roofColorHex), RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (!isUnlocked) "🔒" else bType.displayName.take(2).uppercase(),
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = bType.displayName,
                                    color = if (isUnlocked) Color.White else Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "$${bType.cost}",
                                        color = if (canAfford && isUnlocked) Color(0xFF4ADE80) else Color(0xFFF87171),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (bType.powerConsumptionKw > 0) {
                                        Text(
                                            text = "-${bType.powerConsumptionKw}kW",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 10.sp
                                        )
                                    } else if (bType.powerGenerationKw > 0) {
                                        Text(
                                            text = "+${bType.powerGenerationKw}kW",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerGridOverlayLegendCard(
    uiState: FactoryUiState,
    onOpenPowerDetails: () -> Unit,
    onFocusFirstOffline: () -> Unit,
    onHideOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val insufficientCount = uiState.consumerNodes.count { !it.isPowered && it.isConnectedToGrid }
    val unconnectedCount = uiState.consumerNodes.count { !it.isPowered && !it.isConnectedToGrid }

    Surface(
        modifier = modifier
            .widthIn(max = 265.dp)
            .testTag("power_grid_overlay_legend"),
        color = Color(0xFF0F172A).copy(alpha = 0.93f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenPowerDetails() }
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Overlay da Malha Elétrica",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "Ocultar",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clickable { onHideOverlay() }
                        .padding(horizontal = 4.dp)
                )
            }

            // Color legend items
            LegendDotRow(
                color = Color(0xFF22C55E),
                label = "Energizadas (${uiState.poweredMachineCount}) • Geradores (${uiState.generatorNodes.size})"
            )
            LegendDotRow(
                color = Color(0xFFF59E0B),
                label = "Falta Energia / Sobrecarga ($insufficientCount)"
            )
            LegendDotRow(
                color = Color(0xFFEF4444),
                label = "Offline / Sem Rede Elétrica ($unconnectedCount)"
            )

            if (uiState.offlineMachineCount > 0) {
                Surface(
                    color = Color(0xFF7F1D1D).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .clickable { onFocusFirstOffline() }
                        .testTag("focus_offline_machine_button")
                ) {
                    Text(
                        text = "⚡ Localizar Máquina Offline (${uiState.offlineMachineCount})",
                        color = Color(0xFFFECACA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDotRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 10.sp
        )
    }
}
