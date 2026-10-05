package com.example.data

import android.content.Context
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

data class SavedGameData(
    val money: Int,
    val totalEarned: Int,
    val buildings: List<PlacedBuilding>,
    val unlockedTechIds: Set<String>,
    val activeResearchId: String?,
    val activeResearchProgressSec: Float,
    val completedObjectiveIds: Set<String>,
    val itemProducedCounts: Map<ItemType, Int>,
    val itemSoldCounts: Map<ItemType, Int>,
    val tutorialStep: Int,
    val tutorialSkippedOrCompleted: Boolean,
    val sfxVolume: Float,
    val musicVolume: Float,
    val isMuted: Boolean,
    val reducedEffects: Boolean,
    val uiScale: Float,
    val timestamp: Long
)

class SaveManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("forjanexus_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVE_JSON = "save_state_json_v1"
        private const val KEY_BACKUP_JSON = "save_state_backup_v1"
    }

    fun hasSavedGame(): Boolean {
        val raw = prefs.getString(KEY_SAVE_JSON, null)
        return !raw.isNullOrBlank()
    }

    fun clearSave() {
        prefs.edit()
            .remove(KEY_SAVE_JSON)
            .remove(KEY_BACKUP_JSON)
            .apply()
    }

    fun saveGame(data: SavedGameData): Boolean {
        return try {
            val root = JSONObject().apply {
                put("money", data.money)
                put("totalEarned", data.totalEarned)
                put("activeResearchId", data.activeResearchId ?: "")
                put("activeResearchProgressSec", data.activeResearchProgressSec.toDouble())
                put("tutorialStep", data.tutorialStep)
                put("tutorialSkippedOrCompleted", data.tutorialSkippedOrCompleted)
                put("sfxVolume", data.sfxVolume.toDouble())
                put("musicVolume", data.musicVolume.toDouble())
                put("isMuted", data.isMuted)
                put("reducedEffects", data.reducedEffects)
                put("uiScale", data.uiScale.toDouble())
                put("timestamp", data.timestamp)

                val techArr = JSONArray()
                data.unlockedTechIds.forEach { techArr.put(it) }
                put("unlockedTechIds", techArr)

                val objArr = JSONArray()
                data.completedObjectiveIds.forEach { objArr.put(it) }
                put("completedObjectiveIds", objArr)

                val prodObj = JSONObject()
                data.itemProducedCounts.forEach { (k, v) -> prodObj.put(k.name, v) }
                put("itemProducedCounts", prodObj)

                val soldObj = JSONObject()
                data.itemSoldCounts.forEach { (k, v) -> soldObj.put(k.name, v) }
                put("itemSoldCounts", soldObj)

                val bArr = JSONArray()
                data.buildings.forEach { b ->
                    val bObj = JSONObject().apply {
                        put("x", b.x)
                        put("y", b.y)
                        put("type", b.type.name)
                        put("direction", b.direction.name)
                        put("selectedRecipeId", b.selectedRecipeId ?: "")
                        put("filterItem", b.filterItem?.name ?: "")
                        put("isPaused", b.isPaused)
                        put("beltItem", b.beltItem?.name ?: "")
                        put("beltProgress", b.beltProgress.toDouble())
                        put("craftProgress", b.craftProgress.toDouble())
                        put("splitterToggle", b.splitterToggle)

                        val inObj = JSONObject()
                        b.inputBuffer.forEach { (item, count) -> inObj.put(item.name, count) }
                        put("inputBuffer", inObj)

                        val outObj = JSONObject()
                        b.outputBuffer.forEach { (item, count) -> outObj.put(item.name, count) }
                        put("outputBuffer", outObj)
                    }
                    bArr.put(bObj)
                }
                put("buildings", bArr)
            }
            val serialized = root.toString()
            val previous = prefs.getString(KEY_SAVE_JSON, null)
            prefs.edit().apply {
                if (!previous.isNullOrBlank()) {
                    putString(KEY_BACKUP_JSON, previous)
                }
                putString(KEY_SAVE_JSON, serialized)
            }.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadGame(): Result<SavedGameData> {
        val primary = prefs.getString(KEY_SAVE_JSON, null)
        if (!primary.isNullOrBlank()) {
            parseJson(primary).onSuccess { return Result.success(it) }
        }
        val backup = prefs.getString(KEY_BACKUP_JSON, null)
        if (!backup.isNullOrBlank()) {
            parseJson(backup).onSuccess { return Result.success(it) }
        }
        return Result.failure(IllegalStateException("Nenhum salvamento válido encontrado."))
    }

    private fun parseJson(raw: String): Result<SavedGameData> {
        return try {
            val root = JSONObject(raw)
            val money = root.optInt("money", 180)
            val totalEarned = root.optInt("totalEarned", 0)
            val activeResearchId = root.optString("activeResearchId", "").takeIf { it.isNotBlank() }
            val activeResearchProgressSec = root.optDouble("activeResearchProgressSec", 0.0).toFloat()
            val tutorialStep = root.optInt("tutorialStep", 0)
            val tutorialSkippedOrCompleted = root.optBoolean("tutorialSkippedOrCompleted", false)
            val sfxVolume = root.optDouble("sfxVolume", 0.75).toFloat().coerceIn(0f, 1f)
            val musicVolume = root.optDouble("musicVolume", 0.45).toFloat().coerceIn(0f, 1f)
            val isMuted = root.optBoolean("isMuted", false)
            val reducedEffects = root.optBoolean("reducedEffects", false)
            val uiScale = root.optDouble("uiScale", 1.0).toFloat().coerceIn(0.85f, 1.2f)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            val unlockedTechIds = mutableSetOf<String>()
            root.optJSONArray("unlockedTechIds")?.let { arr ->
                for (i in 0 until arr.length()) {
                    unlockedTechIds.add(arr.optString(i))
                }
            }

            val completedObjectiveIds = mutableSetOf<String>()
            root.optJSONArray("completedObjectiveIds")?.let { arr ->
                for (i in 0 until arr.length()) {
                    completedObjectiveIds.add(arr.optString(i))
                }
            }

            val itemProducedCounts = mutableMapOf<ItemType, Int>()
            root.optJSONObject("itemProducedCounts")?.let { obj ->
                ItemType.entries.forEach { item ->
                    val count = obj.optInt(item.name, 0)
                    if (count > 0) itemProducedCounts[item] = count
                }
            }

            val itemSoldCounts = mutableMapOf<ItemType, Int>()
            root.optJSONObject("itemSoldCounts")?.let { obj ->
                ItemType.entries.forEach { item ->
                    val count = obj.optInt(item.name, 0)
                    if (count > 0) itemSoldCounts[item] = count
                }
            }

            val buildings = mutableListOf<PlacedBuilding>()
            root.optJSONArray("buildings")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val bObj = arr.optJSONObject(i) ?: continue
                    val x = bObj.optInt("x", -1)
                    val y = bObj.optInt("y", -1)
                    if (x !in 0 until GameCatalog.MAP_WIDTH || y !in 0 until GameCatalog.MAP_HEIGHT) continue
                    val type = runCatching { BuildingType.valueOf(bObj.optString("type")) }.getOrNull() ?: continue
                    val direction = runCatching { Direction.valueOf(bObj.optString("direction")) }.getOrDefault(Direction.EAST)
                    val recipeId = bObj.optString("selectedRecipeId", "").takeIf { it.isNotBlank() }
                    val filterItem = bObj.optString("filterItem", "").takeIf { it.isNotBlank() }
                        ?.let { runCatching { ItemType.valueOf(it) }.getOrNull() } ?: ItemType.IRON_ORE
                    val isPaused = bObj.optBoolean("isPaused", false)
                    val beltItem = bObj.optString("beltItem", "").takeIf { it.isNotBlank() }
                        ?.let { runCatching { ItemType.valueOf(it) }.getOrNull() }
                    val beltProgress = bObj.optDouble("beltProgress", 0.0).toFloat().coerceIn(0f, 1f)
                    val craftProgress = bObj.optDouble("craftProgress", 0.0).toFloat().coerceIn(0f, 1f)
                    val splitterToggle = bObj.optInt("splitterToggle", 0)

                    val inputBuffer = mutableMapOf<ItemType, Int>()
                    bObj.optJSONObject("inputBuffer")?.let { inObj ->
                        ItemType.entries.forEach { item ->
                            val c = inObj.optInt(item.name, 0)
                            if (c > 0) inputBuffer[item] = c
                        }
                    }

                    val outputBuffer = mutableMapOf<ItemType, Int>()
                    bObj.optJSONObject("outputBuffer")?.let { outObj ->
                        ItemType.entries.forEach { item ->
                            val c = outObj.optInt(item.name, 0)
                            if (c > 0) outputBuffer[item] = c
                        }
                    }

                    buildings.add(
                        PlacedBuilding(
                            x = x,
                            y = y,
                            type = type,
                            direction = direction,
                            selectedRecipeId = recipeId,
                            filterItem = filterItem,
                            isPaused = isPaused,
                            inputBuffer = inputBuffer,
                            outputBuffer = outputBuffer,
                            beltItem = beltItem,
                            beltProgress = beltProgress,
                            craftProgress = craftProgress,
                            splitterToggle = splitterToggle
                        )
                    )
                }
            }

            Result.success(
                SavedGameData(
                    money = money,
                    totalEarned = totalEarned,
                    buildings = buildings,
                    unlockedTechIds = unlockedTechIds,
                    activeResearchId = activeResearchId,
                    activeResearchProgressSec = activeResearchProgressSec,
                    completedObjectiveIds = completedObjectiveIds,
                    itemProducedCounts = itemProducedCounts,
                    itemSoldCounts = itemSoldCounts,
                    tutorialStep = tutorialStep,
                    tutorialSkippedOrCompleted = tutorialSkippedOrCompleted,
                    sfxVolume = sfxVolume,
                    musicVolume = musicVolume,
                    isMuted = isMuted,
                    reducedEffects = reducedEffects,
                    uiScale = uiScale,
                    timestamp = timestamp
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
