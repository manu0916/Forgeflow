package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.GameCatalog
import com.example.data.SaveManager
import com.example.data.SavedGameData
import com.example.model.*
import com.example.power.ConsumerNodeInfo
import com.example.power.GeneratorNodeInfo
import com.example.power.PowerSystem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    GAMEPLAY
}

enum class ActiveModal {
    NONE,
    RESEARCH,
    OBJECTIVES,
    CATALOG_HELP,
    SETTINGS,
    BIOME_MAP_INFO,
    POWER_SYSTEM,
    CONFIRM_NEW_GAME
}

data class PowerLink(
    val fromX: Int,
    val fromY: Int,
    val toX: Int,
    val toY: Int,
    val isEnergized: Boolean
)

data class UndoEntry(
    val x: Int,
    val y: Int,
    val previousBuilding: PlacedBuilding?,
    val costDelta: Int
)

data class FactoryUiState(
    val currentScreen: AppScreen = AppScreen.MAIN_MENU,
    val activeModal: ActiveModal = ActiveModal.NONE,
    val mapTiles: List<MapTile> = GameCatalog.generateWorldMap(),
    val buildings: Map<Pair<Int, Int>, PlacedBuilding> = emptyMap(),
    val powerLinks: List<PowerLink> = emptyList(),
    val money: Int = 220,
    val totalEarned: Int = 0,
    val recentIncomePerMin: Int = 0,
    val lastEarnedAmount: Int = 0,
    val totalPowerGeneratedKw: Int = 0,
    val totalPowerDemandKw: Int = 0,
    val allocatedPowerKw: Int = 0,
    val poweredMachineCount: Int = 0,
    val offlineMachineCount: Int = 0,
    val generatorNodes: List<GeneratorNodeInfo> = emptyList(),
    val consumerNodes: List<ConsumerNodeInfo> = emptyList(),
    // Build & Selection Controls
    val selectedCategory: BuildingCategory = BuildingCategory.EXTRACTION,
    val buildTool: BuildingType? = null,
    val buildDirection: Direction = Direction.EAST,
    val isDemolishMode: Boolean = false,
    val previewTile: Pair<Int, Int>? = null,
    val selectedTile: Pair<Int, Int>? = null,
    // Simulation Speed & Pause
    val isSimPaused: Boolean = false,
    val simSpeedMultiplier: Int = 1, // 1x, 2x, 3x
    // Research & Objectives
    val unlockedTechIds: Set<String> = emptySet(),
    val activeResearchId: String? = null,
    val activeResearchProgressSec: Float = 0f,
    val objectives: List<GameObjective> = GameCatalog.initialObjectives,
    val itemProducedCounts: Map<ItemType, Int> = emptyMap(),
    val itemSoldCounts: Map<ItemType, Int> = emptyMap(),
    // Tutorial
    val tutorialStep: Int = 0,
    val tutorialCompletedOrSkipped: Boolean = false,
    // Visual & Audio Settings
    val sfxVolume: Float = 0.75f,
    val musicVolume: Float = 0.45f,
    val isMuted: Boolean = false,
    val reducedEffects: Boolean = false,
    val uiScale: Float = 1.0f,
    val showPowerGridOverlay: Boolean = true,
    // Camera target request (for centering on starter base or biomes)
    val cameraFocusTile: Pair<Int, Int>? = Pair(8, 6),
    val cameraFocusNonce: Long = 1L,
    // Notifications & Floating Popups
    val statusBannerMessage: String? = null,
    val floatingPopups: List<FloatingPopup> = emptyList(),
    val hasSavedGame: Boolean = false,
    val lastSavedTimeText: String = "Não salvo",
    val canUndo: Boolean = false,
    val weatherPhase: Float = 0f
)

class FactoryViewModel(application: Application) : AndroidViewModel(application) {

    private val saveManager = SaveManager(application)
    val soundEngine = SoundEngine()
    val powerSystem = PowerSystem()

    private val _uiState = MutableStateFlow(
        FactoryUiState(hasSavedGame = saveManager.hasSavedGame())
    )
    val uiState: StateFlow<FactoryUiState> = _uiState.asStateFlow()

    private val tileLookup: Map<Pair<Int, Int>, MapTile> =
        _uiState.value.mapTiles.associateBy { it.x to it.y }

    private val undoStack = ArrayDeque<UndoEntry>()
    private val recentSalesWindow = ArrayDeque<Pair<Long, Int>>() // timestampMs to amount
    private var popupIdCounter = 1L
    private var autoSaveCounterTicks = 0
    private var simJob: Job? = null

    init {
        setupStarterFactory()
        startSimulationLoop()
    }

    fun startNewGameConfirmed() {
        saveManager.clearSave()
        undoStack.clear()
        recentSalesWindow.clear()
        val starterBuildings = createStarterBuildings()
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.GAMEPLAY,
                activeModal = ActiveModal.NONE,
                buildings = starterBuildings,
                money = 240,
                totalEarned = 0,
                recentIncomePerMin = 0,
                lastEarnedAmount = 0,
                unlockedTechIds = emptySet(),
                activeResearchId = null,
                activeResearchProgressSec = 0f,
                objectives = GameCatalog.initialObjectives,
                itemProducedCounts = emptyMap(),
                itemSoldCounts = emptyMap(),
                tutorialStep = 0,
                tutorialCompletedOrSkipped = false,
                buildTool = BuildingType.EXTRACTOR_MK1,
                buildDirection = Direction.EAST,
                isDemolishMode = false,
                previewTile = Pair(5, 6),
                selectedTile = null,
                cameraFocusTile = Pair(7, 6),
                cameraFocusNonce = System.currentTimeMillis(),
                hasSavedGame = false,
                canUndo = false,
                statusBannerMessage = "Nova fábrica iniciada no Vale Esmeralda!"
            )
        }
        recalculatePowerGrid()
        soundEngine.startAmbientMusic()
    }

    fun continueSavedGame() {
        val loaded = saveManager.loadGame()
        loaded.onSuccess { data ->
            val map = data.buildings.associateBy { it.x to it.y }
            val updatedObjectives = GameCatalog.initialObjectives.map { obj ->
                obj.copy(isCompleted = obj.id in data.completedObjectiveIds)
            }
            soundEngine.sfxVolume = data.sfxVolume
            soundEngine.musicVolume = data.musicVolume
            soundEngine.isMuted = data.isMuted
            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.GAMEPLAY,
                    activeModal = ActiveModal.NONE,
                    money = data.money,
                    totalEarned = data.totalEarned,
                    buildings = map,
                    unlockedTechIds = data.unlockedTechIds,
                    activeResearchId = data.activeResearchId,
                    activeResearchProgressSec = data.activeResearchProgressSec,
                    objectives = updatedObjectives,
                    itemProducedCounts = data.itemProducedCounts,
                    itemSoldCounts = data.itemSoldCounts,
                    tutorialStep = data.tutorialStep,
                    tutorialCompletedOrSkipped = data.tutorialSkippedOrCompleted,
                    sfxVolume = data.sfxVolume,
                    musicVolume = data.musicVolume,
                    isMuted = data.isMuted,
                    reducedEffects = data.reducedEffects,
                    uiScale = data.uiScale,
                    hasSavedGame = true,
                    lastSavedTimeText = "Carregado com sucesso",
                    statusBannerMessage = "Fábrica carregada com sucesso!"
                )
            }
            recalculatePowerGrid()
            soundEngine.startAmbientMusic()
        }.onFailure {
            startNewGameConfirmed()
            _uiState.update {
                it.copy(statusBannerMessage = "Salvamento anterior inválido; nova fábrica iniciada.")
            }
        }
    }

    fun saveGameManual() {
        val s = _uiState.value
        val ok = saveManager.saveGame(
            SavedGameData(
                money = s.money,
                totalEarned = s.totalEarned,
                buildings = s.buildings.values.toList(),
                unlockedTechIds = s.unlockedTechIds,
                activeResearchId = s.activeResearchId,
                activeResearchProgressSec = s.activeResearchProgressSec,
                completedObjectiveIds = s.objectives.filter { it.isCompleted }.map { it.id }.toSet(),
                itemProducedCounts = s.itemProducedCounts,
                itemSoldCounts = s.itemSoldCounts,
                tutorialStep = s.tutorialStep,
                tutorialSkippedOrCompleted = s.tutorialCompletedOrSkipped,
                sfxVolume = s.sfxVolume,
                musicVolume = s.musicVolume,
                isMuted = s.isMuted,
                reducedEffects = s.reducedEffects,
                uiScale = s.uiScale,
                timestamp = System.currentTimeMillis()
            )
        )
        if (ok) {
            _uiState.update {
                it.copy(
                    hasSavedGame = true,
                    lastSavedTimeText = "Salvo agora",
                    statusBannerMessage = "Progresso da fábrica salvo!"
                )
            }
            soundEngine.playBuildSound()
        }
    }

    fun returnToMainMenu() {
        saveGameManual()
        soundEngine.stopAmbientMusic()
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.MAIN_MENU,
                activeModal = ActiveModal.NONE,
                hasSavedGame = saveManager.hasSavedGame()
            )
        }
    }

    private fun setupStarterFactory() {
        val starter = createStarterBuildings()
        _uiState.update {
            it.copy(
                buildings = starter,
                hasSavedGame = saveManager.hasSavedGame()
            )
        }
        recalculatePowerGrid()
    }

    private fun createStarterBuildings(): Map<Pair<Int, Int>, PlacedBuilding> {
        val map = mutableMapOf<Pair<Int, Int>, PlacedBuilding>()
        // Central Sell Hub at (8, 6)
        map[8 to 6] = PlacedBuilding(
            x = 8,
            y = 6,
            type = BuildingType.SELL_HUB,
            direction = Direction.EAST
        )
        // Starter Biomass Generator at (7, 4) covering the Iron Vein at (5,6) and area around Hub
        map[7 to 4] = PlacedBuilding(
            x = 7,
            y = 4,
            type = BuildingType.BIOMASS_GEN,
            direction = Direction.SOUTH
        )
        // Starter Power Conduit at (6, 6) so (4..5, 5..7) Iron Vein is well inside the power grid
        map[6 to 5] = PlacedBuilding(
            x = 6,
            y = 5,
            type = BuildingType.POWER_CONDUIT,
            direction = Direction.SOUTH
        )
        return map
    }

    fun openModal(modal: ActiveModal) {
        _uiState.update { it.copy(activeModal = modal) }
    }

    fun closeModal() {
        _uiState.update { it.copy(activeModal = ActiveModal.NONE) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun selectCategory(category: BuildingCategory) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                isDemolishMode = false
            )
        }
    }

    fun selectBuildTool(type: BuildingType?) {
        val s = _uiState.value
        if (type != null && type.requiredTechId != null && type.requiredTechId !in s.unlockedTechIds) {
            val tech = GameCatalog.techTree.find { it.id == type.requiredTechId }
            _uiState.update {
                it.copy(statusBannerMessage = "Requer pesquisa: ${tech?.title ?: type.requiredTechId}")
            }
            soundEngine.playAlertSound()
            return
        }
        _uiState.update {
            it.copy(
                buildTool = type,
                selectedCategory = type?.category ?: it.selectedCategory,
                isDemolishMode = false,
                selectedTile = if (type != null) null else it.selectedTile
            )
        }
    }

    fun rotateBuildDirection() {
        _uiState.update { state ->
            val nextDir = state.buildDirection.rotateClockwise()
            // Also rotate selected building if no buildTool is active
            val selPos = state.selectedTile
            val updatedBuildings = if (state.buildTool == null && selPos != null && state.buildings.containsKey(selPos)) {
                val m = state.buildings.toMutableMap()
                val existing = m[selPos]!!
                m[selPos] = existing.copy(direction = existing.direction.rotateClockwise())
                m
            } else {
                state.buildings
            }
            state.copy(
                buildDirection = nextDir,
                buildings = updatedBuildings
            )
        }
        soundEngine.playBuildSound()
    }

    fun toggleDemolishMode() {
        _uiState.update {
            it.copy(
                isDemolishMode = !it.isDemolishMode,
                buildTool = null,
                previewTile = null
            )
        }
    }

    fun cancelBuildOrSelection() {
        _uiState.update {
            it.copy(
                buildTool = null,
                isDemolishMode = false,
                previewTile = null,
                selectedTile = null
            )
        }
    }

    fun togglePauseSimulation() {
        _uiState.update { it.copy(isSimPaused = !it.isSimPaused) }
    }

    fun cycleSimSpeed() {
        _uiState.update {
            val next = when (it.simSpeedMultiplier) {
                1 -> 2
                2 -> 3
                else -> 1
            }
            it.copy(simSpeedMultiplier = next, isSimPaused = false)
        }
    }

    fun togglePowerOverlay() {
        _uiState.update { it.copy(showPowerGridOverlay = !it.showPowerGridOverlay) }
    }

    fun focusCameraOn(x: Int, y: Int) {
        _uiState.update {
            it.copy(
                cameraFocusTile = Pair(x, y),
                cameraFocusNonce = System.currentTimeMillis()
            )
        }
    }

    fun onCameraMovedByPlayer() {
        val s = _uiState.value
        if (!s.tutorialCompletedOrSkipped && s.tutorialStep == 0) {
            _uiState.update { it.copy(tutorialStep = 1) }
        }
    }

    fun skipTutorial() {
        _uiState.update {
            it.copy(
                tutorialCompletedOrSkipped = true,
                statusBannerMessage = "Tutorial concluído! Use o botão Guia se precisar de ajuda."
            )
        }
    }

    fun restartTutorial() {
        _uiState.update {
            it.copy(
                tutorialStep = 0,
                tutorialCompletedOrSkipped = false,
                activeModal = ActiveModal.NONE
            )
        }
    }

    fun updateAudioSettings(sfx: Float? = null, music: Float? = null, muted: Boolean? = null) {
        _uiState.update { state ->
            val newSfx = sfx ?: state.sfxVolume
            val newMusic = music ?: state.musicVolume
            val newMuted = muted ?: state.isMuted
            soundEngine.sfxVolume = newSfx
            soundEngine.musicVolume = newMusic
            soundEngine.isMuted = newMuted
            state.copy(
                sfxVolume = newSfx,
                musicVolume = newMusic,
                isMuted = newMuted
            )
        }
    }

    fun updateAccessibilitySettings(reducedEffects: Boolean? = null, uiScale: Float? = null) {
        _uiState.update { state ->
            state.copy(
                reducedEffects = reducedEffects ?: state.reducedEffects,
                uiScale = (uiScale ?: state.uiScale).coerceIn(0.85f, 1.20f)
            )
        }
    }

    fun getEffectiveBuildCost(type: BuildingType, x: Int, y: Int): Int {
        val s = _uiState.value
        val tile = tileLookup[x to y] ?: return type.cost
        val biome = tile.biome
        val multiplier = when (biome) {
            BiomeType.SCORCHED_DUNES -> if ("TECH_DESERT_EXPANSION" in s.unlockedTechIds) 1.0f else biome.buildCostMultiplier
            BiomeType.FROZEN_TUNDRA -> biome.buildCostMultiplier
            BiomeType.VOLCANIC_CALDERA -> biome.buildCostMultiplier
            BiomeType.TEMPERATE_VALLEY -> 1.0f
        }
        return (type.cost * multiplier).toInt().coerceAtLeast(1)
    }

    fun canPlaceBuildingAt(type: BuildingType, x: Int, y: Int): Pair<Boolean, String?> {
        if (x !in 0 until GameCatalog.MAP_WIDTH || y !in 0 until GameCatalog.MAP_HEIGHT) {
            return false to "Fora dos limites do mapa."
        }
        val s = _uiState.value
        val tile = tileLookup[x to y] ?: return false to "Bloco inválido."

        // Check biome tech lock
        val reqBiomeTech = tile.biome.requiredTechId
        if (reqBiomeTech != null && reqBiomeTech !in s.unlockedTechIds) {
            val techName = GameCatalog.techTree.find { it.id == reqBiomeTech }?.title ?: reqBiomeTech
            return false to "Bioma ${tile.biome.displayName} bloqueado! Pesquise: $techName"
        }

        // Check building tech lock
        val reqBuildingTech = type.requiredTechId
        if (reqBuildingTech != null && reqBuildingTech !in s.unlockedTechIds) {
            val techName = GameCatalog.techTree.find { it.id == reqBuildingTech }?.title ?: reqBuildingTech
            return false to "Requer pesquisa: $techName"
        }

        // Check existing building (allow replacing belts with belts of different direction/tier)
        val existing = s.buildings[x to y]
        if (existing != null) {
            val canUpgradeBelt = (existing.type == BuildingType.BELT_MK1 || existing.type == BuildingType.BELT_MK2) &&
                (type == BuildingType.BELT_MK1 || type == BuildingType.BELT_MK2) &&
                (existing.type != type || existing.direction != s.buildDirection)
            if (!canUpgradeBelt) {
                return false to "Local já ocupado por ${existing.type.displayName}."
            }
        }

        // Check Extractor deposit requirement
        if (type == BuildingType.EXTRACTOR_MK1 || type == BuildingType.EXTRACTOR_MK2) {
            if (tile.deposit.outputItem == null) {
                return false to "Extratoras precisam ser posicionadas sobre uma jazida de recurso!"
            }
        }

        // Check Magma Vent restriction (only Geothermal Plant or belts/conduits can sit on Magma Vent)
        if (tile.deposit == DepositType.MAGMA_VENT &&
            type != BuildingType.GEOTHERMAL_PLANT &&
            type != BuildingType.POWER_CONDUIT
        ) {
            return false to "Fenda Magmática só comporta Usinas Geotérmicas ou Postes Condutores!"
        }

        val cost = getEffectiveBuildCost(type, x, y)
        if (s.money < cost) {
            return false to "Saldo insuficiente ($$cost necessários)."
        }

        return true to null
    }

    fun onTileTapped(x: Int, y: Int) {
        if (x !in 0 until GameCatalog.MAP_WIDTH || y !in 0 until GameCatalog.MAP_HEIGHT) return
        val s = _uiState.value

        // 1. Demolish Mode
        if (s.isDemolishMode) {
            demolishAt(x, y)
            return
        }

        // 2. Build Mode
        val tool = s.buildTool
        if (tool != null) {
            // For belts & conduits, single tap places immediately for snappy mobile UX;
            // for larger machines, first tap sets ghost preview, second tap on same tile confirms!
            val isInstantPlace = tool == BuildingType.BELT_MK1 ||
                tool == BuildingType.BELT_MK2 ||
                tool == BuildingType.POWER_CONDUIT
            if (isInstantPlace || s.previewTile == (x to y)) {
                placeBuildingAt(tool, x, y, s.buildDirection)
            } else {
                _uiState.update { it.copy(previewTile = x to y, selectedTile = null) }
            }
            return
        }

        // 3. Inspection / Selection Mode
        val existing = s.buildings[x to y]
        if (existing != null) {
            _uiState.update { it.copy(selectedTile = x to y, previewTile = null) }
        } else {
            // Tapping an empty tile when no tool is active selects the tile info or auto-suggests Extractor if it's a deposit
            val tile = tileLookup[x to y]
            if (tile != null && tile.deposit.outputItem != null) {
                _uiState.update {
                    it.copy(
                        selectedCategory = BuildingCategory.EXTRACTION,
                        buildTool = BuildingType.EXTRACTOR_MK1,
                        previewTile = x to y,
                        selectedTile = null
                    )
                }
            } else {
                _uiState.update { it.copy(selectedTile = x to y, previewTile = null) }
            }
        }
    }

    fun confirmPreviewPlacement() {
        val s = _uiState.value
        val tool = s.buildTool ?: return
        val preview = s.previewTile ?: return
        placeBuildingAt(tool, preview.first, preview.second, s.buildDirection)
    }

    fun placeBuildingAt(type: BuildingType, x: Int, y: Int, direction: Direction = _uiState.value.buildDirection): Boolean {
        val (valid, reason) = canPlaceBuildingAt(type, x, y)
        if (!valid) {
            _uiState.update { it.copy(statusBannerMessage = reason) }
            soundEngine.playAlertSound()
            return false
        }

        val cost = getEffectiveBuildCost(type, x, y)
        val defaultRecipe = GameCatalog.getDefaultRecipeForMachine(type)?.id
        val newBuilding = PlacedBuilding(
            x = x,
            y = y,
            type = type,
            direction = direction,
            selectedRecipeId = defaultRecipe,
            filterItem = ItemType.IRON_ORE
        )

        val prevBuilding = _uiState.value.buildings[x to y]
        undoStack.addLast(UndoEntry(x, y, prevBuilding, cost))
        if (undoStack.size > 20) undoStack.removeFirst()

        _uiState.update { state ->
            val updated = state.buildings.toMutableMap()
            updated[x to y] = newBuilding

            // Advance tutorial if applicable
            var nextTut = state.tutorialStep
            var tutDone = state.tutorialCompletedOrSkipped
            if (!tutDone) {
                if (nextTut <= 1 && (type == BuildingType.EXTRACTOR_MK1 || type == BuildingType.EXTRACTOR_MK2)) {
                    nextTut = 2
                } else if (nextTut == 2 && (type == BuildingType.BELT_MK1 || type == BuildingType.BELT_MK2)) {
                    nextTut = 3
                } else if (nextTut == 4 && (type == BuildingType.SMELTER || type == BuildingType.SAWMILL)) {
                    nextTut = 5
                }
            }

            // Suggest next adjacent tile for belts
            val nextPreview = if (type == BuildingType.BELT_MK1 || type == BuildingType.BELT_MK2) {
                val nx = (x + direction.dx).coerceIn(0, GameCatalog.MAP_WIDTH - 1)
                val ny = (y + direction.dy).coerceIn(0, GameCatalog.MAP_HEIGHT - 1)
                nx to ny
            } else {
                null
            }

            state.copy(
                buildings = updated,
                money = state.money - cost,
                previewTile = nextPreview,
                tutorialStep = nextTut,
                canUndo = undoStack.isNotEmpty(),
                statusBannerMessage = null
            )
        }

        recalculatePowerGrid()
        checkObjectives()
        soundEngine.playBuildSound()
        return true
    }

    fun demolishAt(x: Int, y: Int) {
        val s = _uiState.value
        val existing = s.buildings[x to y] ?: return
        // Prevent demolishing the last Sell Hub if player can't afford another
        val sellHubCount = s.buildings.values.count { it.type == BuildingType.SELL_HUB }
        if (existing.type == BuildingType.SELL_HUB && sellHubCount <= 1 && s.money < BuildingType.SELL_HUB.cost) {
            _uiState.update {
                it.copy(statusBannerMessage = "Mantenha pelo menos um Terminal de Venda ativo!")
            }
            soundEngine.playAlertSound()
            return
        }

        val refund = (existing.type.cost * 0.8f).toInt()
        val storedValue = existing.inputBuffer.entries.sumOf { it.key.sellValue * it.value } +
            existing.outputBuffer.entries.sumOf { it.key.sellValue * it.value } +
            (existing.beltItem?.sellValue ?: 0)

        undoStack.addLast(UndoEntry(x, y, existing, -(refund + storedValue)))
        if (undoStack.size > 20) undoStack.removeFirst()

        _uiState.update { state ->
            val updated = state.buildings.toMutableMap()
            updated.remove(x to y)
            state.copy(
                buildings = updated,
                money = state.money + refund + storedValue,
                selectedTile = if (state.selectedTile == (x to y)) null else state.selectedTile,
                canUndo = undoStack.isNotEmpty(),
                statusBannerMessage = "${existing.type.displayName} removido (+$${refund + storedValue})"
            )
        }
        recalculatePowerGrid()
        soundEngine.playRemoveSound()
    }

    fun undoLastAction() {
        val last = undoStack.removeLastOrNull() ?: return
        _uiState.update { state ->
            val updated = state.buildings.toMutableMap()
            if (last.previousBuilding == null) {
                updated.remove(last.x to last.y)
            } else {
                updated[last.x to last.y] = last.previousBuilding
            }
            state.copy(
                buildings = updated,
                money = (state.money + last.costDelta).coerceAtLeast(0),
                canUndo = undoStack.isNotEmpty(),
                statusBannerMessage = "Última ação desfeita."
            )
        }
        recalculatePowerGrid()
        soundEngine.playRemoveSound()
    }

    fun setBuildingRecipe(x: Int, y: Int, recipeId: String) {
        _uiState.update { state ->
            val b = state.buildings[x to y] ?: return@update state
            val updated = state.buildings.toMutableMap()
            updated[x to y] = b.copy(
                selectedRecipeId = recipeId,
                craftProgress = 0f
            )
            state.copy(buildings = updated)
        }
    }

    fun setFilterItem(x: Int, y: Int, item: ItemType) {
        _uiState.update { state ->
            val b = state.buildings[x to y] ?: return@update state
            val updated = state.buildings.toMutableMap()
            updated[x to y] = b.copy(filterItem = item)
            state.copy(buildings = updated)
        }
    }

    fun toggleBuildingPause(x: Int, y: Int) {
        _uiState.update { state ->
            val b = state.buildings[x to y] ?: return@update state
            val updated = state.buildings.toMutableMap()
            updated[x to y] = b.copy(isPaused = !b.isPaused)
            state.copy(buildings = updated)
        }
        recalculatePowerGrid()
    }

    fun sellStoredItemsAt(x: Int, y: Int) {
        _uiState.update { state ->
            val b = state.buildings[x to y] ?: return@update state
            val allItems = mutableMapOf<ItemType, Int>()
            b.inputBuffer.forEach { (k, v) -> allItems[k] = (allItems[k] ?: 0) + v }
            b.outputBuffer.forEach { (k, v) -> allItems[k] = (allItems[k] ?: 0) + v }
            val value = allItems.entries.sumOf { it.key.sellValue * it.value }
            if (value <= 0) return@update state

            val updatedBuildings = state.buildings.toMutableMap()
            updatedBuildings[x to y] = b.copy(inputBuffer = emptyMap(), outputBuffer = emptyMap())

            val updatedSold = state.itemSoldCounts.toMutableMap()
            allItems.forEach { (item, count) ->
                updatedSold[item] = (updatedSold[item] ?: 0) + count
            }

            state.copy(
                buildings = updatedBuildings,
                money = state.money + value,
                totalEarned = state.totalEarned + value,
                lastEarnedAmount = value,
                itemSoldCounts = updatedSold,
                statusBannerMessage = "Estoque vendido por +$$value!"
            )
        }
        soundEngine.playSellChime()
        checkObjectives()
    }

    fun startResearch(techId: String) {
        val s = _uiState.value
        val tech = GameCatalog.techTree.find { it.id == techId } ?: return
        if (techId in s.unlockedTechIds) return
        if (tech.prerequisiteId != null && tech.prerequisiteId !in s.unlockedTechIds) {
            _uiState.update { it.copy(statusBannerMessage = "Pesquise o pré-requisito primeiro!") }
            soundEngine.playAlertSound()
            return
        }
        if (s.money < tech.costMoney) {
            _uiState.update { it.copy(statusBannerMessage = "Dinheiro insuficiente para iniciar pesquisa ($${tech.costMoney}).") }
            soundEngine.playAlertSound()
            return
        }
        _uiState.update {
            it.copy(
                money = it.money - tech.costMoney,
                activeResearchId = techId,
                activeResearchProgressSec = 0f,
                statusBannerMessage = "Pesquisando: ${tech.title}..."
            )
        }
        soundEngine.playBuildSound()
    }

    fun recalculatePowerGrid() {
        _uiState.update { state ->
            val evaluation = powerSystem.evaluate(
                buildingsInput = state.buildings,
                tileLookup = tileLookup,
                unlockedTechIds = state.unlockedTechIds
            )
            state.copy(
                buildings = evaluation.updatedBuildings,
                powerLinks = evaluation.powerLinks,
                totalPowerGeneratedKw = evaluation.totalProductionKw,
                totalPowerDemandKw = evaluation.totalConsumptionKw,
                allocatedPowerKw = evaluation.allocatedPowerKw,
                poweredMachineCount = evaluation.poweredMachineCount,
                offlineMachineCount = evaluation.offlineMachineCount,
                generatorNodes = evaluation.generators,
                consumerNodes = evaluation.consumers
            )
        }
    }

    private fun startSimulationLoop() {
        simJob?.cancel()
        simJob = viewModelScope.launch {
            val baseTickMs = 100L
            while (isActive) {
                delay(baseTickMs)
                val s = _uiState.value
                if (s.currentScreen == AppScreen.GAMEPLAY && !s.isSimPaused) {
                    val dtSeconds = (baseTickMs / 1000f) * s.simSpeedMultiplier
                    stepSimulation(dtSeconds)
                }
            }
        }
    }

    fun stepSimulation(dtSeconds: Float) {
        recalculatePowerGrid()

        _uiState.update { state ->
            val buildings = state.buildings.toMutableMap()
            var moneyDelta = 0
            val newlyProduced = state.itemProducedCounts.toMutableMap()
            val newlySold = state.itemSoldCounts.toMutableMap()
            val popups = state.floatingPopups
                .mapNotNull { p ->
                    val nextAge = p.ageProgress + dtSeconds * 0.85f
                    if (nextAge >= 1f) null else p.copy(ageProgress = nextAge)
                }
                .toMutableList()

            val storageCapacity = if ("TECH_SMART_ROUTING" in state.unlockedTechIds) 250 else 120

            // Helper to try inserting an item into a target building at (tx, ty)
            fun tryDeliverItem(targetX: Int, targetY: Int, item: ItemType): Boolean {
                val target = buildings[targetX to targetY] ?: return false
                if (target.isPaused) return false

                return when (target.type) {
                    BuildingType.SELL_HUB -> {
                        val value = item.sellValue
                        moneyDelta += value
                        newlySold[item] = (newlySold[item] ?: 0) + 1
                        if (!state.reducedEffects && popups.size < 14) {
                            popups.add(
                                FloatingPopup(
                                    id = popupIdCounter++,
                                    tileX = targetX,
                                    tileY = targetY,
                                    text = "+$$value",
                                    colorHex = 0xFF4ADE80
                                )
                            )
                        }
                        true
                    }
                    BuildingType.BELT_MK1, BuildingType.BELT_MK2,
                    BuildingType.SPLITTER, BuildingType.MERGER, BuildingType.FILTER -> {
                        if (target.beltItem == null) {
                            buildings[targetX to targetY] = target.copy(
                                beltItem = item,
                                beltProgress = 0f,
                                operationalState = OperationalState.ACTIVE
                            )
                            true
                        } else {
                            false
                        }
                    }
                    BuildingType.STORAGE -> {
                        if (target.totalStoredItems() < storageCapacity) {
                            val outBuf = target.outputBuffer.toMutableMap()
                            outBuf[item] = (outBuf[item] ?: 0) + 1
                            buildings[targetX to targetY] = target.copy(
                                outputBuffer = outBuf,
                                operationalState = OperationalState.ACTIVE
                            )
                            true
                        } else {
                            buildings[targetX to targetY] = target.copy(
                                operationalState = OperationalState.OUTPUT_FULL
                            )
                            false
                        }
                    }
                    BuildingType.BIOMASS_GEN -> {
                        if (item == ItemType.WOOD_LOG || item == ItemType.WOOD_PLANK) {
                            val currentFuel = target.inputBuffer.values.sum()
                            if (currentFuel < 20) {
                                val inBuf = target.inputBuffer.toMutableMap()
                                inBuf[item] = (inBuf[item] ?: 0) + 1
                                buildings[targetX to targetY] = target.copy(inputBuffer = inBuf)
                                true
                            } else false
                        } else false
                    }
                    BuildingType.SMELTER, BuildingType.SAWMILL, BuildingType.CUTTER,
                    BuildingType.ASSEMBLER, BuildingType.MANUFACTURER -> {
                        // Find current recipe or compatible recipe if machine buffer is empty
                        var recipe = GameCatalog.getRecipeById(target.selectedRecipeId)
                        if (recipe == null || (item !in recipe.inputs && target.inputBuffer.isEmpty())) {
                            val compatible = GameCatalog.recipes.firstOrNull { r ->
                                r.machine == target.type &&
                                    item in r.inputs &&
                                    (r.requiredTechId == null || r.requiredTechId in state.unlockedTechIds)
                            }
                            if (compatible != null) {
                                recipe = compatible
                            }
                        }
                        if (recipe != null && item in recipe.inputs) {
                            val currentCount = target.inputBuffer[item] ?: 0
                            val maxStack = (recipe.inputs[item] ?: 1) * 8
                            if (currentCount < maxStack) {
                                val inBuf = target.inputBuffer.toMutableMap()
                                inBuf[item] = currentCount + 1
                                buildings[targetX to targetY] = target.copy(
                                    selectedRecipeId = recipe.id,
                                    inputBuffer = inBuf
                                )
                                true
                            } else false
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }

            // Pass 1: Update belts, splitters, mergers, filters, and storage outputs
            val positions = buildings.keys.toList()
            for (pos in positions) {
                val b = buildings[pos] ?: continue
                if (b.isPaused) {
                    buildings[pos] = b.copy(operationalState = OperationalState.PAUSED)
                    continue
                }

                val tileBiome = tileLookup[pos]?.biome ?: BiomeType.TEMPERATE_VALLEY

                when (b.type) {
                    BuildingType.BELT_MK1, BuildingType.BELT_MK2 -> {
                        val item = b.beltItem
                        if (item != null) {
                            val biomeBeltMod = if (b.type == BuildingType.BELT_MK1 &&
                                tileBiome == BiomeType.FROZEN_TUNDRA &&
                                "TECH_CRYO_EXPANSION" !in state.unlockedTechIds
                            ) {
                                tileBiome.beltSpeedModifier
                            } else 1.0f

                            val nextProg = b.beltProgress + (dtSeconds / b.type.baseCycleSeconds) * biomeBeltMod
                            if (nextProg >= 1f) {
                                val tx = b.x + b.direction.dx
                                val ty = b.y + b.direction.dy
                                if (tryDeliverItem(tx, ty, item)) {
                                    buildings[pos] = b.copy(
                                        beltItem = null,
                                        beltProgress = 0f,
                                        operationalState = OperationalState.ACTIVE
                                    )
                                } else {
                                    buildings[pos] = b.copy(
                                        beltProgress = 1f,
                                        operationalState = OperationalState.OUTPUT_FULL
                                    )
                                }
                            } else {
                                buildings[pos] = b.copy(
                                    beltProgress = nextProg,
                                    operationalState = OperationalState.ACTIVE
                                )
                            }
                        } else {
                            buildings[pos] = b.copy(operationalState = OperationalState.WAITING_INPUT)
                        }
                    }

                    BuildingType.SPLITTER -> {
                        val item = b.beltItem
                        if (item != null) {
                            val nextProg = b.beltProgress + (dtSeconds / b.type.baseCycleSeconds)
                            if (nextProg >= 1f) {
                                val candidateDirs = listOf(
                                    b.direction,
                                    b.direction.rotateCounterClockwise(),
                                    b.direction.rotateClockwise()
                                )
                                var delivered = false
                                var nextToggle = b.splitterToggle
                                for (offset in 0 until 3) {
                                    val idx = (b.splitterToggle + offset) % 3
                                    val dir = candidateDirs[idx]
                                    if (tryDeliverItem(b.x + dir.dx, b.y + dir.dy, item)) {
                                        delivered = true
                                        nextToggle = (idx + 1) % 3
                                        break
                                    }
                                }
                                if (delivered) {
                                    buildings[pos] = b.copy(
                                        beltItem = null,
                                        beltProgress = 0f,
                                        splitterToggle = nextToggle,
                                        operationalState = OperationalState.ACTIVE
                                    )
                                } else {
                                    buildings[pos] = b.copy(
                                        beltProgress = 1f,
                                        operationalState = OperationalState.OUTPUT_FULL
                                    )
                                }
                            } else {
                                buildings[pos] = b.copy(
                                    beltProgress = nextProg,
                                    operationalState = OperationalState.ACTIVE
                                )
                            }
                        } else {
                            buildings[pos] = b.copy(operationalState = OperationalState.WAITING_INPUT)
                        }
                    }

                    BuildingType.MERGER -> {
                        val item = b.beltItem
                        if (item != null) {
                            val nextProg = b.beltProgress + (dtSeconds / b.type.baseCycleSeconds)
                            if (nextProg >= 1f) {
                                val tx = b.x + b.direction.dx
                                val ty = b.y + b.direction.dy
                                if (tryDeliverItem(tx, ty, item)) {
                                    buildings[pos] = b.copy(
                                        beltItem = null,
                                        beltProgress = 0f,
                                        operationalState = OperationalState.ACTIVE
                                    )
                                } else {
                                    buildings[pos] = b.copy(
                                        beltProgress = 1f,
                                        operationalState = OperationalState.OUTPUT_FULL
                                    )
                                }
                            } else {
                                buildings[pos] = b.copy(
                                    beltProgress = nextProg,
                                    operationalState = OperationalState.ACTIVE
                                )
                            }
                        } else {
                            buildings[pos] = b.copy(operationalState = OperationalState.WAITING_INPUT)
                        }
                    }

                    BuildingType.FILTER -> {
                        if (b.powerState.isOffline) {
                            buildings[pos] = b.copy(operationalState = OperationalState.OFFLINE)
                        } else {
                            val item = b.beltItem
                            if (item != null) {
                                val nextProg = b.beltProgress + (dtSeconds / b.type.baseCycleSeconds)
                                if (nextProg >= 1f) {
                                    val targetDirs = if (item == b.filterItem) {
                                        listOf(b.direction)
                                    } else {
                                        listOf(
                                            b.direction.rotateClockwise(),
                                            b.direction.rotateCounterClockwise()
                                        )
                                    }
                                    var delivered = false
                                    for (d in targetDirs) {
                                        if (tryDeliverItem(b.x + d.dx, b.y + d.dy, item)) {
                                            delivered = true
                                            break
                                        }
                                    }
                                    if (delivered) {
                                        buildings[pos] = b.copy(
                                            beltItem = null,
                                            beltProgress = 0f,
                                            operationalState = OperationalState.ACTIVE
                                        )
                                    } else {
                                        buildings[pos] = b.copy(
                                            beltProgress = 1f,
                                            operationalState = OperationalState.OUTPUT_FULL
                                        )
                                    }
                                } else {
                                    buildings[pos] = b.copy(
                                        beltProgress = nextProg,
                                        operationalState = OperationalState.ACTIVE
                                    )
                                }
                            } else {
                                buildings[pos] = b.copy(operationalState = OperationalState.WAITING_INPUT)
                            }
                        }
                    }

                    BuildingType.STORAGE -> {
                        val firstEntry = b.outputBuffer.entries.firstOrNull { it.value > 0 }
                        if (firstEntry != null) {
                            val nextProg = b.craftProgress + (dtSeconds / b.type.baseCycleSeconds)
                            if (nextProg >= 1f) {
                                val tx = b.x + b.direction.dx
                                val ty = b.y + b.direction.dy
                                if (tryDeliverItem(tx, ty, firstEntry.key)) {
                                    val updatedOut = b.outputBuffer.toMutableMap()
                                    val rem = firstEntry.value - 1
                                    if (rem <= 0) updatedOut.remove(firstEntry.key) else updatedOut[firstEntry.key] = rem
                                    buildings[pos] = b.copy(
                                        outputBuffer = updatedOut,
                                        craftProgress = 0f,
                                        operationalState = OperationalState.ACTIVE
                                    )
                                } else {
                                    buildings[pos] = b.copy(
                                        craftProgress = 1f,
                                        operationalState = if (b.totalStoredItems() >= storageCapacity) {
                                            OperationalState.OUTPUT_FULL
                                        } else {
                                            OperationalState.ACTIVE
                                        }
                                    )
                                }
                            } else {
                                buildings[pos] = b.copy(craftProgress = nextProg)
                            }
                        } else {
                            buildings[pos] = b.copy(operationalState = OperationalState.WAITING_INPUT)
                        }
                    }

                    else -> {}
                }
            }

            // Pass 2: Extractors and Processing Machines
            for (pos in positions) {
                val b = buildings[pos] ?: continue
                if (b.isPaused) continue
                val tile = tileLookup[pos] ?: continue
                val biome = tile.biome

                val biomeMachineMod = when (biome) {
                    BiomeType.SCORCHED_DUNES -> {
                        if ("TECH_DESERT_EXPANSION" in state.unlockedTechIds) 1.0f else biome.thermalCoolingBonus
                    }
                    BiomeType.FROZEN_TUNDRA -> biome.thermalCoolingBonus
                    BiomeType.VOLCANIC_CALDERA -> biome.thermalCoolingBonus
                    BiomeType.TEMPERATE_VALLEY -> 1.0f
                }

                when (b.type) {
                    BuildingType.EXTRACTOR_MK1, BuildingType.EXTRACTOR_MK2 -> {
                        val resourceItem = tile.deposit.outputItem
                        if (resourceItem == null) {
                            buildings[pos] = b.copy(operationalState = OperationalState.NO_DEPOSIT)
                            continue
                        }
                        if (b.powerState.isOffline) {
                            buildings[pos] = b.copy(operationalState = OperationalState.OFFLINE)
                            continue
                        }

                        // Try pushing existing outputBuffer item first
                        var currentOut = b.outputBuffer.toMutableMap()
                        val bufferedCount = currentOut[resourceItem] ?: 0
                        if (bufferedCount > 0) {
                            val tx = b.x + b.direction.dx
                            val ty = b.y + b.direction.dy
                            if (tryDeliverItem(tx, ty, resourceItem)) {
                                if (bufferedCount - 1 <= 0) currentOut.remove(resourceItem)
                                else currentOut[resourceItem] = bufferedCount - 1
                            }
                        }

                        val newBufferedCount = currentOut[resourceItem] ?: 0
                        if (newBufferedCount >= 8) {
                            buildings[pos] = b.copy(
                                outputBuffer = currentOut,
                                operationalState = OperationalState.OUTPUT_FULL
                            )
                        } else {
                            val progressDelta = (dtSeconds / b.type.baseCycleSeconds) * biomeMachineMod
                            val nextProg = b.craftProgress + progressDelta
                            if (nextProg >= 1f) {
                                val tx = b.x + b.direction.dx
                                val ty = b.y + b.direction.dy
                                if (!tryDeliverItem(tx, ty, resourceItem)) {
                                    currentOut[resourceItem] = newBufferedCount + 1
                                }
                                newlyProduced[resourceItem] = (newlyProduced[resourceItem] ?: 0) + 1
                                buildings[pos] = b.copy(
                                    outputBuffer = currentOut,
                                    craftProgress = 0f,
                                    operationalState = OperationalState.ACTIVE
                                )
                            } else {
                                buildings[pos] = b.copy(
                                    outputBuffer = currentOut,
                                    craftProgress = nextProg,
                                    operationalState = OperationalState.ACTIVE
                                )
                            }
                        }
                    }

                    BuildingType.SMELTER, BuildingType.SAWMILL, BuildingType.CUTTER,
                    BuildingType.ASSEMBLER, BuildingType.MANUFACTURER -> {
                        if (b.powerState.isOffline) {
                            buildings[pos] = b.copy(operationalState = OperationalState.OFFLINE)
                            continue
                        }

                        val recipe = GameCatalog.getRecipeById(b.selectedRecipeId)
                            ?: GameCatalog.getDefaultRecipeForMachine(b.type)
                        if (recipe == null) {
                            buildings[pos] = b.copy(operationalState = OperationalState.WAITING_INPUT)
                            continue
                        }

                        val currentIn = b.inputBuffer.toMutableMap()
                        val currentOut = b.outputBuffer.toMutableMap()

                        // Push any ready output to the front tile
                        val outReady = currentOut[recipe.output] ?: 0
                        if (outReady > 0) {
                            val tx = b.x + b.direction.dx
                            val ty = b.y + b.direction.dy
                            if (tryDeliverItem(tx, ty, recipe.output)) {
                                if (outReady - 1 <= 0) currentOut.remove(recipe.output)
                                else currentOut[recipe.output] = outReady - 1
                            }
                        }

                        val remainingOut = currentOut[recipe.output] ?: 0
                        if (remainingOut >= 10) {
                            buildings[pos] = b.copy(
                                outputBuffer = currentOut,
                                operationalState = OperationalState.OUTPUT_FULL
                            )
                            continue
                        }

                        val hasAllInputs = recipe.inputs.all { (reqItem, reqQty) ->
                            (currentIn[reqItem] ?: 0) >= reqQty
                        }

                        if (!hasAllInputs) {
                            buildings[pos] = b.copy(
                                outputBuffer = currentOut,
                                craftProgress = 0f,
                                operationalState = OperationalState.WAITING_INPUT
                            )
                        } else {
                            val progressDelta = (dtSeconds / recipe.craftSeconds) * biomeMachineMod
                            val nextProg = b.craftProgress + progressDelta
                            if (nextProg >= 1f) {
                                // Consume inputs
                                recipe.inputs.forEach { (reqItem, reqQty) ->
                                    val rem = (currentIn[reqItem] ?: 0) - reqQty
                                    if (rem <= 0) currentIn.remove(reqItem) else currentIn[reqItem] = rem
                                }
                                // Produce outputs
                                var toBuffer = recipe.outputCount
                                val tx = b.x + b.direction.dx
                                val ty = b.y + b.direction.dy
                                if (toBuffer > 0 && tryDeliverItem(tx, ty, recipe.output)) {
                                    toBuffer -= 1
                                }
                                if (toBuffer > 0) {
                                    currentOut[recipe.output] = (currentOut[recipe.output] ?: 0) + toBuffer
                                }
                                newlyProduced[recipe.output] = (newlyProduced[recipe.output] ?: 0) + recipe.outputCount

                                buildings[pos] = b.copy(
                                    selectedRecipeId = recipe.id,
                                    inputBuffer = currentIn,
                                    outputBuffer = currentOut,
                                    craftProgress = 0f,
                                    operationalState = OperationalState.ACTIVE
                                )
                            } else {
                                buildings[pos] = b.copy(
                                    selectedRecipeId = recipe.id,
                                    outputBuffer = currentOut,
                                    craftProgress = nextProg,
                                    operationalState = OperationalState.ACTIVE
                                )
                            }
                        }
                    }

                    BuildingType.BIOMASS_GEN -> {
                        val hasWood = (b.inputBuffer[ItemType.WOOD_LOG] ?: 0) > 0 ||
                            (b.inputBuffer[ItemType.WOOD_PLANK] ?: 0) > 0
                        if (hasWood) {
                            val nextProg = b.craftProgress + dtSeconds * 0.18f
                            if (nextProg >= 1f) {
                                val inBuf = b.inputBuffer.toMutableMap()
                                val fuelKey = if ((inBuf[ItemType.WOOD_LOG] ?: 0) > 0) ItemType.WOOD_LOG else ItemType.WOOD_PLANK
                                val rem = (inBuf[fuelKey] ?: 1) - 1
                                if (rem <= 0) inBuf.remove(fuelKey) else inBuf[fuelKey] = rem
                                buildings[pos] = b.copy(inputBuffer = inBuf, craftProgress = 0f)
                            } else {
                                buildings[pos] = b.copy(craftProgress = nextProg)
                            }
                        }
                    }

                    else -> {}
                }
            }

            // Pass 3: Advance Active Research automatically
            var activeResId = state.activeResearchId
            var activeResProg = state.activeResearchProgressSec
            val unlockedTechs = state.unlockedTechIds.toMutableSet()
            var bannerMsg = state.statusBannerMessage

            if (activeResId != null) {
                val techNode = GameCatalog.techTree.find { it.id == activeResId }
                if (techNode != null) {
                    activeResProg += dtSeconds
                    if (activeResProg >= techNode.researchSeconds) {
                        unlockedTechs.add(techNode.id)
                        activeResId = null
                        activeResProg = 0f
                        bannerMsg = "Pesquisa concluída: ${techNode.title}!"
                        soundEngine.playResearchOrObjectiveComplete()
                    }
                } else {
                    activeResId = null
                    activeResProg = 0f
                }
            }

            // Track rolling income per minute
            val nowMs = System.currentTimeMillis()
            if (moneyDelta > 0) {
                recentSalesWindow.addLast(nowMs to moneyDelta)
            }
            while (recentSalesWindow.isNotEmpty() && nowMs - recentSalesWindow.first().first > 60_000L) {
                recentSalesWindow.removeFirst()
            }
            val rollingPerMin = recentSalesWindow.sumOf { it.second }

            // Advance Tutorial Steps based on live factory milestones
            var nextTutStep = state.tutorialStep
            var tutCompleted = state.tutorialCompletedOrSkipped
            if (!tutCompleted) {
                val totalSold = newlySold.values.sum()
                val refinedSold = newlySold.entries.sumOf { (item, count) ->
                    if (item.category != ItemCategory.RAW) count else 0
                }
                if (nextTutStep == 3 && totalSold >= 1) {
                    nextTutStep = 4
                } else if (nextTutStep >= 5 && refinedSold >= 1) {
                    tutCompleted = true
                    bannerMsg = "Tutorial concluído! Sua fábrica automatizada está pronta para expandir."
                    soundEngine.playResearchOrObjectiveComplete()
                }
            }

            state.copy(
                buildings = buildings,
                money = state.money + moneyDelta,
                totalEarned = state.totalEarned + moneyDelta,
                recentIncomePerMin = rollingPerMin,
                lastEarnedAmount = if (moneyDelta > 0) moneyDelta else state.lastEarnedAmount,
                unlockedTechIds = unlockedTechs,
                activeResearchId = activeResId,
                activeResearchProgressSec = activeResProg,
                itemProducedCounts = newlyProduced,
                itemSoldCounts = newlySold,
                tutorialStep = nextTutStep,
                tutorialCompletedOrSkipped = tutCompleted,
                floatingPopups = popups,
                statusBannerMessage = bannerMsg,
                weatherPhase = (state.weatherPhase + dtSeconds * 0.25f) % 100f
            )
        }

        checkObjectives()

        // Periodic non-blocking auto-save every ~15 seconds of gameplay
        autoSaveCounterTicks++
        if (autoSaveCounterTicks >= 150) {
            autoSaveCounterTicks = 0
            val s = _uiState.value
            saveManager.saveGame(
                SavedGameData(
                    money = s.money,
                    totalEarned = s.totalEarned,
                    buildings = s.buildings.values.toList(),
                    unlockedTechIds = s.unlockedTechIds,
                    activeResearchId = s.activeResearchId,
                    activeResearchProgressSec = s.activeResearchProgressSec,
                    completedObjectiveIds = s.objectives.filter { it.isCompleted }.map { it.id }.toSet(),
                    itemProducedCounts = s.itemProducedCounts,
                    itemSoldCounts = s.itemSoldCounts,
                    tutorialStep = s.tutorialStep,
                    tutorialSkippedOrCompleted = s.tutorialCompletedOrSkipped,
                    sfxVolume = s.sfxVolume,
                    musicVolume = s.musicVolume,
                    isMuted = s.isMuted,
                    reducedEffects = s.reducedEffects,
                    uiScale = s.uiScale,
                    timestamp = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(hasSavedGame = true, lastSavedTimeText = "Auto-salvo") }
        }
    }

    private fun checkObjectives() {
        _uiState.update { state ->
            var rewardBonus = 0
            var completedTitle: String? = null

            val updatedObjectives = state.objectives.map { obj ->
                if (obj.isCompleted) return@map obj

                val achieved = when (obj.id) {
                    "OBJ_FIRST_EXTRACTOR" -> state.buildings.values.any {
                        it.type == BuildingType.EXTRACTOR_MK1 || it.type == BuildingType.EXTRACTOR_MK2
                    }
                    "OBJ_FIRST_SALE" -> state.itemSoldCounts.values.sum() >= 10
                    "OBJ_SMELT_METAL" -> {
                        val smeltedCount = (state.itemProducedCounts[ItemType.IRON_INGOT] ?: 0) +
                            (state.itemProducedCounts[ItemType.COPPER_INGOT] ?: 0) +
                            (state.itemProducedCounts[ItemType.GOLD_INGOT] ?: 0) +
                            (state.itemProducedCounts[ItemType.WOOD_PLANK] ?: 0) +
                            (state.itemProducedCounts[ItemType.STONE_BLOCK] ?: 0)
                        smeltedCount >= 5
                    }
                    "OBJ_POWER_EXPANSION" -> state.totalPowerGeneratedKw >= 100
                    "OBJ_FIRST_TECH" -> state.unlockedTechIds.isNotEmpty()
                    "OBJ_MULTI_RESOURCE" -> {
                        val combined = (state.itemProducedCounts[ItemType.MECH_COMPONENT] ?: 0) +
                            (state.itemProducedCounts[ItemType.ELEC_CIRCUIT] ?: 0) +
                            (state.itemProducedCounts[ItemType.INSULATED_CABLE] ?: 0)
                        combined >= 1
                    }
                    "OBJ_BIOME_PIONEER" -> state.buildings.keys.any { pos ->
                        val b = tileLookup[pos]?.biome
                        b != null && b != BiomeType.TEMPERATE_VALLEY
                    }
                    "OBJ_TYCOON_5000" -> {
                        state.money >= 2500 ||
                            (state.itemProducedCounts[ItemType.INDUSTRIAL_MODULE] ?: 0) >= 1 ||
                            (state.itemProducedCounts[ItemType.CRYO_PROCESSOR] ?: 0) >= 1
                    }
                    else -> false
                }

                if (achieved) {
                    rewardBonus += obj.rewardMoney
                    completedTitle = obj.title
                    obj.copy(isCompleted = true)
                } else {
                    obj
                }
            }

            if (rewardBonus > 0) {
                soundEngine.playResearchOrObjectiveComplete()
                state.copy(
                    objectives = updatedObjectives,
                    money = state.money + rewardBonus,
                    totalEarned = state.totalEarned + rewardBonus,
                    statusBannerMessage = "Objetivo cumprido: $completedTitle (+$$rewardBonus)!"
                )
            } else {
                state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        simJob?.cancel()
        soundEngine.release()
    }
}
