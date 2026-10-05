package com.example

import com.example.data.GameCatalog
import com.example.model.BiomeType
import com.example.model.ItemCategory
import com.example.model.ItemType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `verify production chains and coherent pricing`() {
        // Every recipe output must be worth more than its raw inputs combined
        for (recipe in GameCatalog.recipes) {
            val inputCost = recipe.inputs.entries.sumOf { it.key.sellValue * it.value }
            val outputValue = recipe.output.sellValue * recipe.outputCount
            assertTrue(
                "Recipe ${recipe.id} output ($outputValue) must exceed input value ($inputCost)",
                outputValue > inputCost
            )
        }
    }

    @Test
    fun `verify all four biomes and required resources exist on world map`() {
        val tiles = GameCatalog.generateWorldMap()
        assertEquals(GameCatalog.MAP_WIDTH * GameCatalog.MAP_HEIGHT, tiles.size)

        val biomesPresent = tiles.map { it.biome }.toSet()
        assertTrue(BiomeType.TEMPERATE_VALLEY in biomesPresent)
        assertTrue(BiomeType.SCORCHED_DUNES in biomesPresent)
        assertTrue(BiomeType.FROZEN_TUNDRA in biomesPresent)
        assertTrue(BiomeType.VOLCANIC_CALDERA in biomesPresent)

        val rawItemsOnMap = tiles.mapNotNull { it.deposit.outputItem }.toSet()
        assertTrue(ItemType.IRON_ORE in rawItemsOnMap)
        assertTrue(ItemType.COPPER_ORE in rawItemsOnMap)
        assertTrue(ItemType.GOLD_ORE in rawItemsOnMap)
        assertTrue(ItemType.WOOD_LOG in rawItemsOnMap)
        assertTrue(ItemType.STONE_RAW in rawItemsOnMap)
        assertTrue(ItemType.QUARTZ_ORE in rawItemsOnMap)
        assertTrue(ItemType.COBALT_ORE in rawItemsOnMap)
    }

    @Test
    fun `verify PowerSystem tracks generator production machine consumption and offline state`() {
        val powerSystem = com.example.power.PowerSystem()
        val tiles = GameCatalog.generateWorldMap().associateBy { it.x to it.y }

        // 1 Biomass Generator (50 kW) at (7, 4)
        // 4 Smelters (15 kW each = 60 kW total demand) within radius 5 of (7, 4)
        val buildings = mapOf(
            (7 to 4) to com.example.model.PlacedBuilding(x = 7, y = 4, type = com.example.model.BuildingType.BIOMASS_GEN),
            (6 to 4) to com.example.model.PlacedBuilding(x = 6, y = 4, type = com.example.model.BuildingType.SMELTER),
            (8 to 4) to com.example.model.PlacedBuilding(x = 8, y = 4, type = com.example.model.BuildingType.SMELTER),
            (7 to 5) to com.example.model.PlacedBuilding(x = 7, y = 5, type = com.example.model.BuildingType.SMELTER),
            (8 to 5) to com.example.model.PlacedBuilding(x = 8, y = 5, type = com.example.model.BuildingType.SMELTER),
            // 1 Unconnected Extractor far away at (15, 10) (10 kW demand)
            (15 to 10) to com.example.model.PlacedBuilding(x = 15, y = 10, type = com.example.model.BuildingType.EXTRACTOR_MK1)
        )

        val eval = powerSystem.evaluate(
            buildingsInput = buildings,
            tileLookup = tiles,
            unlockedTechIds = emptySet()
        )

        // Generator produces 50 kW; 4 Smelters (60 kW) + 1 Extractor (10 kW) = 70 kW total consumption
        assertEquals(50, eval.totalProductionKw)
        assertEquals(70, eval.totalConsumptionKw)
        assertEquals(45, eval.allocatedPowerKw) // 3 Smelters * 15 kW = 45 kW powered
        assertEquals(3, eval.poweredMachineCount)
        assertEquals(2, eval.offlineMachineCount) // 4th Smelter (insufficient energy) + unconnected Extractor

        // Verify the 4th smelter without sufficient energy has explicit OFFLINE state
        val fourthSmelter = eval.updatedBuildings[8 to 5]!!
        assertEquals(com.example.model.PowerState.OFFLINE, fourthSmelter.powerState)
        assertEquals(com.example.model.OperationalState.OFFLINE, fourthSmelter.operationalState)
        assertTrue("Machine with insufficient energy must be offline", fourthSmelter.isOffline)

        // Verify the unconnected extractor also has explicit OFFLINE operational state
        val farExtractor = eval.updatedBuildings[15 to 10]!!
        assertEquals(com.example.model.PowerState.UNCONNECTED, farExtractor.powerState)
        assertEquals(com.example.model.OperationalState.OFFLINE, farExtractor.operationalState)
        assertTrue("Unconnected machine must be offline", farExtractor.isOffline)
    }
}
