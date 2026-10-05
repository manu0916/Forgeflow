package com.example.power

import com.example.model.*
import com.example.viewmodel.PowerLink
import kotlin.math.hypot

data class GeneratorNodeInfo(
    val x: Int,
    val y: Int,
    val type: BuildingType,
    val biome: BiomeType,
    val productionKw: Int,
    val isBoosted: Boolean
)

data class ConsumerNodeInfo(
    val x: Int,
    val y: Int,
    val type: BuildingType,
    val requiredKw: Int,
    val isConnectedToGrid: Boolean,
    val isPowered: Boolean,
    val powerState: PowerState
)

data class PowerGridEvaluation(
    val updatedBuildings: Map<Pair<Int, Int>, PlacedBuilding>,
    val powerLinks: List<PowerLink>,
    val totalProductionKw: Int,
    val totalConsumptionKw: Int,
    val allocatedPowerKw: Int,
    val poweredMachineCount: Int,
    val offlineMachineCount: Int,
    val generators: List<GeneratorNodeInfo>,
    val consumers: List<ConsumerNodeInfo>
) {
    val netBalanceKw: Int get() = totalProductionKw - totalConsumptionKw
    val hasDeficit: Boolean get() = totalConsumptionKw > totalProductionKw || offlineMachineCount > 0
}

/**
 * Core Power System responsible for tracking energy production from generators,
 * power transmission across conduits, power consumption from industrial machines,
 * and transitioning machines without sufficient energy into an explicit OFFLINE state.
 */
class PowerSystem {

    fun calculateGeneratorProductionKw(
        building: PlacedBuilding,
        tile: MapTile?
    ): Pair<Int, Boolean> {
        if (building.isPaused || building.type.powerGenerationKw <= 0) return 0 to false
        val biome = tile?.biome ?: BiomeType.TEMPERATE_VALLEY
        return when (building.type) {
            BuildingType.BIOMASS_GEN -> {
                val hasFuelBonus = (building.inputBuffer[ItemType.WOOD_LOG] ?: 0) > 0 ||
                    (building.inputBuffer[ItemType.WOOD_PLANK] ?: 0) > 0
                if (hasFuelBonus) 75 to true else 50 to false
            }
            BuildingType.SOLAR_PANEL -> {
                val prod = (building.type.powerGenerationKw * biome.solarEfficiency).toInt()
                prod to (biome == BiomeType.SCORCHED_DUNES)
            }
            BuildingType.GEOTHERMAL_PLANT -> {
                val onMagma = tile?.deposit == DepositType.MAGMA_VENT
                if (onMagma) 220 to true else 160 to false
            }
            else -> building.type.powerGenerationKw to false
        }
    }

    fun calculateMachineConsumptionKw(building: PlacedBuilding): Int {
        if (building.isPaused || building.type.powerConsumptionKw <= 0) return 0
        return building.type.powerConsumptionKw
    }

    fun evaluate(
        buildingsInput: Map<Pair<Int, Int>, PlacedBuilding>,
        tileLookup: Map<Pair<Int, Int>, MapTile>,
        unlockedTechIds: Set<String>
    ): PowerGridEvaluation {
        val buildings = buildingsInput.toMutableMap()
        val conduitBonus = if ("TECH_SOLAR_GRID" in unlockedTechIds) 2 else 0

        val activeGenerators = buildings.values
            .filter { !it.isPaused && it.type.powerGenerationKw > 0 }
            .sortedWith(compareBy({ it.y }, { it.x }))

        val activeConduits = buildings.values
            .filter { !it.isPaused && it.type == BuildingType.POWER_CONDUIT }
            .sortedWith(compareBy({ it.y }, { it.x }))

        var totalProductionKw = 0
        val generatorInfos = mutableListOf<GeneratorNodeInfo>()

        data class GridRelayNode(val x: Int, val y: Int, val radius: Int)
        val energizedNodes = mutableListOf<GridRelayNode>()
        val visitedConduits = mutableSetOf<Pair<Int, Int>>()
        val links = mutableListOf<PowerLink>()
        val queue = ArrayDeque<GridRelayNode>()

        // 1. Sum energy production from all active generators
        for (gen in activeGenerators) {
            val tile = tileLookup[gen.x to gen.y]
            val (prodKw, boosted) = calculateGeneratorProductionKw(gen, tile)
            totalProductionKw += prodKw
            generatorInfos.add(
                GeneratorNodeInfo(
                    x = gen.x,
                    y = gen.y,
                    type = gen.type,
                    biome = tile?.biome ?: BiomeType.TEMPERATE_VALLEY,
                    productionKw = prodKw,
                    isBoosted = boosted
                )
            )
            val node = GridRelayNode(gen.x, gen.y, gen.type.powerRadius)
            energizedNodes.add(node)
            queue.addLast(node)
            buildings[gen.x to gen.y] = gen.copy(
                powerState = PowerState.POWERED,
                operationalState = OperationalState.ACTIVE
            )
        }

        // 2. Propagate power through connected Power Conduits via BFS
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (conduit in activeConduits) {
                val pos = conduit.x to conduit.y
                if (pos in visitedConduits) continue
                val dist = hypot((current.x - conduit.x).toDouble(), (current.y - conduit.y).toDouble())
                val maxReach = maxOf(current.radius, conduit.type.powerRadius + conduitBonus)
                if (dist <= maxReach) {
                    visitedConduits.add(pos)
                    val cNode = GridRelayNode(conduit.x, conduit.y, conduit.type.powerRadius + conduitBonus)
                    energizedNodes.add(cNode)
                    queue.addLast(cNode)
                    links.add(
                        PowerLink(
                            fromX = current.x,
                            fromY = current.y,
                            toX = conduit.x,
                            toY = conduit.y,
                            isEnergized = totalProductionKw > 0
                        )
                    )
                    buildings[pos] = conduit.copy(
                        powerState = if (totalProductionKw > 0) PowerState.POWERED else PowerState.OFFLINE,
                        operationalState = if (totalProductionKw > 0) OperationalState.ACTIVE else OperationalState.OFFLINE,
                        connectedPowerSourceX = current.x,
                        connectedPowerSourceY = current.y
                    )
                }
            }
        }

        // Mark isolated conduits as offline/unconnected
        for (conduit in activeConduits) {
            val pos = conduit.x to conduit.y
            if (pos !in visitedConduits) {
                buildings[pos] = conduit.copy(
                    powerState = PowerState.UNCONNECTED,
                    operationalState = OperationalState.NO_POWER,
                    connectedPowerSourceX = null,
                    connectedPowerSourceY = null
                )
            }
        }

        // 3. Evaluate all power-consuming machines & allocate available energy
        val consumingMachines = buildings.values
            .filter { it.type.powerConsumptionKw > 0 }
            .sortedWith(
                compareBy<PlacedBuilding>(
                    // Priority order: Extractors first, then Smelters/Sawmills, then advanced machines
                    {
                        when (it.type.category) {
                            BuildingCategory.EXTRACTION -> 0
                            BuildingCategory.PROCESSING -> 1
                            BuildingCategory.BELTS_LOGISTICS -> 2
                            else -> 3
                        }
                    },
                    { it.type.powerConsumptionKw },
                    { it.y },
                    { it.x }
                )
            )

        var totalConsumptionKw = 0
        var allocatedPowerKw = 0
        var remainingPowerKw = totalProductionKw
        var poweredCount = 0
        var offlineCount = 0
        val consumerInfos = mutableListOf<ConsumerNodeInfo>()

        for (machine in consumingMachines) {
            val pos = machine.x to machine.y
            if (machine.isPaused) {
                buildings[pos] = machine.copy(
                    powerState = PowerState.NOT_REQUIRED,
                    operationalState = OperationalState.PAUSED,
                    connectedPowerSourceX = null,
                    connectedPowerSourceY = null
                )
                continue
            }

            val reqKw = calculateMachineConsumptionKw(machine)
            totalConsumptionKw += reqKw

            // Find closest energized node in range
            var bestSource: GridRelayNode? = null
            var bestDist = Double.MAX_VALUE
            for (node in energizedNodes) {
                val d = hypot((node.x - machine.x).toDouble(), (node.y - machine.y).toDouble())
                if (d <= node.radius && d < bestDist) {
                    bestDist = d
                    bestSource = node
                }
            }

            if (bestSource == null) {
                // Machine is disconnected from the power grid -> OFFLINE
                offlineCount++
                val newState = PowerState.UNCONNECTED
                buildings[pos] = machine.copy(
                    powerState = newState,
                    operationalState = OperationalState.OFFLINE,
                    connectedPowerSourceX = null,
                    connectedPowerSourceY = null
                )
                consumerInfos.add(
                    ConsumerNodeInfo(
                        x = machine.x,
                        y = machine.y,
                        type = machine.type,
                        requiredKw = reqKw,
                        isConnectedToGrid = false,
                        isPowered = false,
                        powerState = newState
                    )
                )
            } else {
                // Machine is connected to the grid; check if sufficient energy remains
                if (remainingPowerKw >= reqKw) {
                    remainingPowerKw -= reqKw
                    allocatedPowerKw += reqKw
                    poweredCount++
                    val newState = PowerState.POWERED
                    val opState = if (machine.operationalState.isOffline) {
                        OperationalState.WAITING_INPUT
                    } else {
                        machine.operationalState
                    }
                    buildings[pos] = machine.copy(
                        powerState = newState,
                        operationalState = opState,
                        connectedPowerSourceX = bestSource.x,
                        connectedPowerSourceY = bestSource.y
                    )
                    links.add(
                        PowerLink(
                            fromX = bestSource.x,
                            fromY = bestSource.y,
                            toX = machine.x,
                            toY = machine.y,
                            isEnergized = true
                        )
                    )
                    consumerInfos.add(
                        ConsumerNodeInfo(
                            x = machine.x,
                            y = machine.y,
                            type = machine.type,
                            requiredKw = reqKw,
                            isConnectedToGrid = true,
                            isPowered = true,
                            powerState = newState
                        )
                    )
                } else {
                    // Insufficient energy -> Machine enters explicit OFFLINE state!
                    offlineCount++
                    val newState = PowerState.OFFLINE
                    buildings[pos] = machine.copy(
                        powerState = newState,
                        operationalState = OperationalState.OFFLINE,
                        connectedPowerSourceX = bestSource.x,
                        connectedPowerSourceY = bestSource.y
                    )
                    links.add(
                        PowerLink(
                            fromX = bestSource.x,
                            fromY = bestSource.y,
                            toX = machine.x,
                            toY = machine.y,
                            isEnergized = false
                        )
                    )
                    consumerInfos.add(
                        ConsumerNodeInfo(
                            x = machine.x,
                            y = machine.y,
                            type = machine.type,
                            requiredKw = reqKw,
                            isConnectedToGrid = true,
                            isPowered = false,
                            powerState = newState
                        )
                    )
                }
            }
        }

        return PowerGridEvaluation(
            updatedBuildings = buildings,
            powerLinks = links,
            totalProductionKw = totalProductionKw,
            totalConsumptionKw = totalConsumptionKw,
            allocatedPowerKw = allocatedPowerKw,
            poweredMachineCount = poweredCount,
            offlineMachineCount = offlineCount,
            generators = generatorInfos,
            consumers = consumerInfos
        )
    }
}
