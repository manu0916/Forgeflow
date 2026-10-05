package com.example.model

import androidx.compose.ui.graphics.Color

enum class Direction(val dx: Int, val dy: Int, val label: String, val angle: Float) {
    NORTH(0, -1, "Norte", 0f),
    EAST(1, 0, "Leste", 90f),
    SOUTH(0, 1, "Sul", 180f),
    WEST(-1, 0, "Oeste", 270f);

    fun rotateClockwise(): Direction = when (this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }

    fun rotateCounterClockwise(): Direction = when (this) {
        NORTH -> WEST
        WEST -> SOUTH
        SOUTH -> EAST
        EAST -> NORTH
    }

    fun opposite(): Direction = when (this) {
        NORTH -> SOUTH
        SOUTH -> NORTH
        EAST -> WEST
        WEST -> EAST
    }
}

enum class BiomeType(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val description: String,
    val groundPrimaryHex: Long,
    val groundSecondaryHex: Long,
    val accentHex: Long,
    val buildCostMultiplier: Float,
    val solarEfficiency: Float,
    val thermalCoolingBonus: Float,
    val beltSpeedModifier: Float,
    val weatherName: String,
    val requiredTechId: String?
) {
    TEMPERATE_VALLEY(
        id = "TEMPERATE_VALLEY",
        displayName = "Vale Esmeralda",
        subtitle = "Zona Inicial • Clima Estável",
        description = "Terreno fértil e plano com depósitos abundantes de Ferro, Cobre, Madeira e Pedra. Sem penalidades climáticas.",
        groundPrimaryHex = 0xFF1E3528,
        groundSecondaryHex = 0xFF243F30,
        accentHex = 0xFF4ADE80,
        buildCostMultiplier = 1.0f,
        solarEfficiency = 1.0f,
        thermalCoolingBonus = 1.0f,
        beltSpeedModifier = 1.0f,
        weatherName = "Brisa Limpa",
        requiredTechId = null
    ),
    SCORCHED_DUNES(
        id = "SCORCHED_DUNES",
        displayName = "Dunas de Silício",
        subtitle = "Deserto Árido • +50% Energia Solar",
        description = "Rico em Ouro e Quartzo de Silício. Areia frouxa encarece obras em +25% e tempestades de areia reduzem máquinas em 20% até pesquisar Blindagem Térmica.",
        groundPrimaryHex = 0xFF42311B,
        groundSecondaryHex = 0xFF4F3B21,
        accentHex = 0xFFFBBF24,
        buildCostMultiplier = 1.25f,
        solarEfficiency = 1.5f,
        thermalCoolingBonus = 0.80f,
        beltSpeedModifier = 0.95f,
        weatherName = "Tempestade de Areia",
        requiredTechId = "TECH_DESERT_EXPANSION"
    ),
    FROZEN_TUNDRA(
        id = "FROZEN_TUNDRA",
        displayName = "Tundra Boreal",
        subtitle = "Permafrost • Resfriamento +20%",
        description = "Depósitos raros de Cristal de Cobalto e Ouro. O frio extremo acelera máquinas térmicas em +20%, mas congela esteiras comuns (-20% vel.) sem Aquecimento Criogênico.",
        groundPrimaryHex = 0xFF1A2E3F,
        groundSecondaryHex = 0xFF213A4E,
        accentHex = 0xFF38BDF8,
        buildCostMultiplier = 1.30f,
        solarEfficiency = 0.65f,
        thermalCoolingBonus = 1.20f,
        beltSpeedModifier = 0.80f,
        weatherName = "Nevasca Ártica",
        requiredTechId = "TECH_CRYO_EXPANSION"
    ),
    VOLCANIC_CALDERA(
        id = "VOLCANIC_CALDERA",
        displayName = "Caldeira Magmática",
        subtitle = "Zona Geotérmica • Alta Energia",
        description = "Fendas magmáticas para Usinas Geotérmicas de 160 kW e veios puros de Ouro, Cobalto e Pedra Basáltica. Exige Escudo Geotérmico.",
        groundPrimaryHex = 0xFF2D191E,
        groundSecondaryHex = 0xFF3B2027,
        accentHex = 0xFFF97316,
        buildCostMultiplier = 1.45f,
        solarEfficiency = 0.85f,
        thermalCoolingBonus = 0.90f,
        beltSpeedModifier = 1.0f,
        weatherName = "Cinzas Vulcânicas",
        requiredTechId = "TECH_VOLCANIC_EXPANSION"
    )
}

enum class DepositType(
    val displayName: String,
    val outputItem: ItemType?,
    val colorHex: Long,
    val accentHex: Long
) {
    NONE("Solo Livre", null, 0xFF000000, 0xFF000000),
    IRON_VEIN("Jazida de Ferro", ItemType.IRON_ORE, 0xFF788696, 0xFFCBD5E1),
    COPPER_VEIN("Jazida de Cobre", ItemType.COPPER_ORE, 0xFFD97736, 0xFFFDBA74),
    GOLD_VEIN("Veio de Ouro", ItemType.GOLD_ORE, 0xFFEAB308, 0xFFFEF08A),
    TIMBER_GROVE("Bosque Industrial", ItemType.WOOD_LOG, 0xFF4D7C3F, 0xFF86EFAC),
    STONE_QUARRY("Pedreira Bruta", ItemType.STONE_RAW, 0xFF64748B, 0xFF94A3B8),
    QUARTZ_CRYSTAL("Cristal de Silício", ItemType.QUARTZ_ORE, 0xFFA855F7, 0xFFE9D5FF),
    COBALT_GLACIER("Jazida de Cobalto", ItemType.COBALT_ORE, 0xFF0284C7, 0xFF7DD3FC),
    MAGMA_VENT("Fenda Magmática", null, 0xFFEA580C, 0xFFFDE047)
}

enum class ItemCategory(val label: String) {
    RAW("Matéria-Prima"),
    PROCESSED("Refinado"),
    COMPONENT("Componente"),
    HIGH_TECH("Produto Final")
}

enum class ItemType(
    val displayName: String,
    val shortCode: String,
    val sellValue: Int,
    val colorHex: Long,
    val category: ItemCategory,
    val tier: Int
) {
    // Raw Materials
    IRON_ORE("Minério de Ferro", "Fe", 4, 0xFF94A3B8, ItemCategory.RAW, 1),
    COPPER_ORE("Minério de Cobre", "Cu", 5, 0xFFF97316, ItemCategory.RAW, 1),
    GOLD_ORE("Minério de Ouro", "Au", 12, 0xFFFACC15, ItemCategory.RAW, 2),
    WOOD_LOG("Toras de Madeira", "Md", 3, 0xFF854D0E, ItemCategory.RAW, 1),
    STONE_RAW("Pedra Bruta", "Pd", 3, 0xFF64748B, ItemCategory.RAW, 1),
    QUARTZ_ORE("Quartzo de Silício", "Si", 10, 0xFFC084FC, ItemCategory.RAW, 2),
    COBALT_ORE("Cristal de Cobalto", "Co", 16, 0xFF38BDF8, ItemCategory.RAW, 3),

    // Stage 1 Refined
    IRON_INGOT("Barra de Ferro", "BFe", 12, 0xFFCBD5E1, ItemCategory.PROCESSED, 1),
    COPPER_INGOT("Barra de Cobre", "BCu", 14, 0xFFFB923C, ItemCategory.PROCESSED, 1),
    GOLD_INGOT("Lingote de Ouro", "LAu", 34, 0xFFFDE047, ItemCategory.PROCESSED, 2),
    WOOD_PLANK("Tábuas Tratadas", "Tab", 9, 0xFFA16207, ItemCategory.PROCESSED, 1),
    STONE_BLOCK("Bloco de Pedra", "Blk", 9, 0xFF94A3B8, ItemCategory.PROCESSED, 1),
    SILICON_WAFER("Pastilha de Silício", "Waf", 28, 0xFFD8B4FE, ItemCategory.PROCESSED, 2),
    COBALT_ALLOY("Liga Criogênica", "LCo", 46, 0xFF7DD3FC, ItemCategory.PROCESSED, 3),

    // Stage 2 Cut / Shaped
    IRON_PLATE("Chapa de Ferro", "Chp", 26, 0xFFE2E8F0, ItemCategory.COMPONENT, 1),
    IRON_GEAR("Engrenagem", "Eng", 30, 0xFF94A3B8, ItemCategory.COMPONENT, 1),
    COPPER_WIRE("Fio de Cobre", "Fio", 28, 0xFFFDBA74, ItemCategory.COMPONENT, 1),
    GOLD_CONTACT("Contato de Ouro", "Cnt", 74, 0xFFFEF08A, ItemCategory.COMPONENT, 2),
    WOOD_CRATE("Caixa de Transporte", "Cxa", 24, 0xFFCA8A04, ItemCategory.COMPONENT, 1),
    STONE_BRICK("Tijolo Refratário", "Tij", 24, 0xFFCBD5E1, ItemCategory.COMPONENT, 1),
    INSULATED_CABLE("Cabo Isolado", "Cab", 62, 0xFFFB7185, ItemCategory.COMPONENT, 2),

    // Stage 3 & 4 Multi-Resource Products
    MECH_COMPONENT("Comp. Mecânico", "Mec", 82, 0xFF60A5FA, ItemCategory.COMPONENT, 2),
    ELEC_CIRCUIT("Circuito Elétrico", "Cir", 95, 0xFF34D399, ItemCategory.COMPONENT, 2),
    REINFORCED_FRAME("Estrutura Pesada", "Est", 125, 0xFFA78BFA, ItemCategory.HIGH_TECH, 2),
    ADV_CIRCUIT("Circuito Avançado", "CAv", 260, 0xFFF43F5E, ItemCategory.HIGH_TECH, 3),
    INDUSTRIAL_MODULE("Módulo Industrial", "Mod", 340, 0xFF2DD4BF, ItemCategory.HIGH_TECH, 3),
    CRYO_PROCESSOR("Processador Crio", "Pro", 580, 0xFF38BDF8, ItemCategory.HIGH_TECH, 4),
    QUANTUM_CORE("Núcleo Nexus", "Nxs", 1400, 0xFFF59E0B, ItemCategory.HIGH_TECH, 4);

    val composeColor: Color get() = Color(colorHex)
}

enum class BuildingCategory(val displayName: String) {
    EXTRACTION("Extração"),
    BELTS_LOGISTICS("Logística"),
    PROCESSING("Produção"),
    POWER_GRID("Energia"),
    COMMERCE("Armazém & Venda")
}

enum class BuildingType(
    val displayName: String,
    val category: BuildingCategory,
    val cost: Int,
    val powerConsumptionKw: Int, // > 0 consumes power
    val powerGenerationKw: Int,  // > 0 generates power
    val powerRadius: Int,        // > 0 projects or relays power grid
    val baseCycleSeconds: Float,
    val description: String,
    val primaryColorHex: Long,
    val roofColorHex: Long,
    val requiredTechId: String? = null
) {
    // Extraction
    EXTRACTOR_MK1(
        displayName = "Extratora MK1",
        category = BuildingCategory.EXTRACTION,
        cost = 25,
        powerConsumptionKw = 10,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 1.4f,
        description = "Extrai minérios, madeira ou pedra de depósitos naturais. Requer 10 kW de energia.",
        primaryColorHex = 0xFFD97706,
        roofColorHex = 0xFFF59E0B
    ),
    EXTRACTOR_MK2(
        displayName = "Extratora Laser MK2",
        category = BuildingCategory.EXTRACTION,
        cost = 120,
        powerConsumptionKw = 25,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 0.7f,
        description = "Extração de alta velocidade (2x mais rápida). Ideal para depósitos raros.",
        primaryColorHex = 0xFFEA580C,
        roofColorHex = 0xFFFB923C,
        requiredTechId = "TECH_FAST_LOGISTICS"
    ),

    // Belts & Logistics
    BELT_MK1(
        displayName = "Esteira MK1",
        category = BuildingCategory.BELTS_LOGISTICS,
        cost = 4,
        powerConsumptionKw = 0,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 0.8f,
        description = "Transporta itens automaticamente entre máquinas, armazéns e pontos de venda.",
        primaryColorHex = 0xFF334155,
        roofColorHex = 0xFF475569
    ),
    BELT_MK2(
        displayName = "Esteira Expressa MK2",
        category = BuildingCategory.BELTS_LOGISTICS,
        cost = 12,
        powerConsumptionKw = 0,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 0.4f,
        description = "Esteira magnética com o dobro da velocidade e resistente ao gelo da Tundra.",
        primaryColorHex = 0xFF0369A1,
        roofColorHex = 0xFF0EA5E9,
        requiredTechId = "TECH_FAST_LOGISTICS"
    ),
    SPLITTER(
        displayName = "Divisor de Fluxo",
        category = BuildingCategory.BELTS_LOGISTICS,
        cost = 18,
        powerConsumptionKw = 0,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 0.4f,
        description = "Divide os itens recebidos alternadamente entre a saída frontal, esquerda e direita.",
        primaryColorHex = 0xFF4F46E5,
        roofColorHex = 0xFF818CF8
    ),
    MERGER(
        displayName = "Junção de Fluxo",
        category = BuildingCategory.BELTS_LOGISTICS,
        cost = 18,
        powerConsumptionKw = 0,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 0.4f,
        description = "Combina itens vindos de três lados em uma única saída frontal.",
        primaryColorHex = 0xFF0D9488,
        roofColorHex = 0xFF2DD4BF
    ),
    FILTER(
        displayName = "Filtro Seletor",
        category = BuildingCategory.BELTS_LOGISTICS,
        cost = 35,
        powerConsumptionKw = 2,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 0.35f,
        description = "Envia o item selecionado para a frente e desvia os demais itens para os lados.",
        primaryColorHex = 0xFF9333EA,
        roofColorHex = 0xFFC084FC,
        requiredTechId = "TECH_SMART_ROUTING"
    ),

    // Processing
    SMELTER(
        displayName = "Fundição Térmica",
        category = BuildingCategory.PROCESSING,
        cost = 45,
        powerConsumptionKw = 15,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 1.6f,
        description = "Funde minérios brutos em barras metálicas, blocos de pedra e pastilhas de silício.",
        primaryColorHex = 0xFFB91C1C,
        roofColorHex = 0xFFEF4444
    ),
    SAWMILL(
        displayName = "Serraria Industrial",
        category = BuildingCategory.PROCESSING,
        cost = 40,
        powerConsumptionKw = 12,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 1.4f,
        description = "Corta toras de madeira em tábuas tratadas e caixas de transporte.",
        primaryColorHex = 0xFF854D0E,
        roofColorHex = 0xFFCA8A04
    ),
    CUTTER(
        displayName = "Prensa & Cortadora",
        category = BuildingCategory.PROCESSING,
        cost = 65,
        powerConsumptionKw = 18,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 1.5f,
        description = "Transforma barras em chapas, engrenagens, fios de cobre, contatos de ouro e tijolos.",
        primaryColorHex = 0xFF1D4ED8,
        roofColorHex = 0xFF3B82F6,
        requiredTechId = "TECH_METALWORKING"
    ),
    ASSEMBLER(
        displayName = "Montadora Dupla",
        category = BuildingCategory.PROCESSING,
        cost = 110,
        powerConsumptionKw = 28,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 2.0f,
        description = "Combina 2 insumos diferentes para fabricar cabos, circuitos e componentes mecânicos.",
        primaryColorHex = 0xFF047857,
        roofColorHex = 0xFF10B981,
        requiredTechId = "TECH_ASSEMBLY"
    ),
    MANUFACTURER(
        displayName = "Fábrica Nexus",
        category = BuildingCategory.PROCESSING,
        cost = 260,
        powerConsumptionKw = 48,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 2.6f,
        description = "Unidade avançada para até 3 insumos: produz estruturas, circuitos avançados e Núcleos Nexus.",
        primaryColorHex = 0xFF6D28D9,
        roofColorHex = 0xFF8B5CF6,
        requiredTechId = "TECH_HIGH_TECH_MFG"
    ),

    // Power Grid
    BIOMASS_GEN(
        displayName = "Dínamo a Biomassa",
        category = BuildingCategory.POWER_GRID,
        cost = 40,
        powerConsumptionKw = 0,
        powerGenerationKw = 50,
        powerRadius = 5,
        baseCycleSeconds = 2.0f,
        description = "Gera 50 kW contínuos (+25 kW bônus se abastecido com Madeira/Tábuas). Alcance elétrico: 5 blocos.",
        primaryColorHex = 0xFF15803D,
        roofColorHex = 0xFF22C55E
    ),
    POWER_CONDUIT(
        displayName = "Poste Condutor",
        category = BuildingCategory.POWER_GRID,
        cost = 12,
        powerConsumptionKw = 0,
        powerGenerationKw = 0,
        powerRadius = 6,
        baseCycleSeconds = 1.0f,
        description = "Retransmite a rede elétrica por 6 blocos de raio, conectando geradores a máquinas distantes.",
        primaryColorHex = 0xFF0284C7,
        roofColorHex = 0xFF38BDF8
    ),
    SOLAR_PANEL(
        displayName = "Painel Solar Industrial",
        category = BuildingCategory.POWER_GRID,
        cost = 95,
        powerConsumptionKw = 0,
        powerGenerationKw = 40,
        powerRadius = 4,
        baseCycleSeconds = 1.0f,
        description = "Gera 40 kW limpos (60 kW nas Dunas de Silício!). Alcance elétrico: 4 blocos.",
        primaryColorHex = 0xFF1E40AF,
        roofColorHex = 0xFF60A5FA,
        requiredTechId = "TECH_SOLAR_GRID"
    ),
    GEOTHERMAL_PLANT(
        displayName = "Usina Geotérmica",
        category = BuildingCategory.POWER_GRID,
        cost = 240,
        powerConsumptionKw = 0,
        powerGenerationKw = 160,
        powerRadius = 7,
        baseCycleSeconds = 1.0f,
        description = "Gera 160 kW (+220 kW sobre Fenda Magmática!). Alcance elétrico: 7 blocos.",
        primaryColorHex = 0xFFC2410C,
        roofColorHex = 0xFFF97316,
        requiredTechId = "TECH_VOLCANIC_EXPANSION"
    ),

    // Storage & Commerce
    STORAGE(
        displayName = "Silo de Carga",
        category = BuildingCategory.COMMERCE,
        cost = 35,
        powerConsumptionKw = 0,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 0.45f,
        description = "Armazena até 120 itens e repassa automaticamente pela saída frontal quando conectado.",
        primaryColorHex = 0xFF475569,
        roofColorHex = 0xFF94A3B8
    ),
    SELL_HUB(
        displayName = "Terminal de Venda",
        category = BuildingCategory.COMMERCE,
        cost = 55,
        powerConsumptionKw = 0,
        powerGenerationKw = 0,
        powerRadius = 0,
        baseCycleSeconds = 0.25f,
        description = "Recebe recursos ou produtos por qualquer lado e converte instantaneamente em dinheiro ($).",
        primaryColorHex = 0xFFB45309,
        roofColorHex = 0xFFFBBF24
    )
}

data class Recipe(
    val id: String,
    val name: String,
    val machine: BuildingType,
    val inputs: Map<ItemType, Int>,
    val output: ItemType,
    val outputCount: Int = 1,
    val craftSeconds: Float,
    val requiredTechId: String? = null
)

enum class PowerState(val label: String, val colorHex: Long) {
    POWERED("Energizado", 0xFF22C55E),
    OFFLINE("Offline (Energia Insuficiente)", 0xFFEF4444),
    BROWN_OUT("Offline (Sobrecarga)", 0xFFF59E0B),
    UNCONNECTED("Offline (Sem Rede Elétrica)", 0xFFEF4444),
    NOT_REQUIRED("Passivo (0 kW)", 0xFF94A3B8);

    val isOffline: Boolean
        get() = this == OFFLINE || this == BROWN_OUT || this == UNCONNECTED
}

enum class OperationalState(val label: String, val colorHex: Long) {
    ACTIVE("Produzindo", 0xFF22C55E),
    WAITING_INPUT("Aguardando Insumos", 0xFFFBBF24),
    OUTPUT_FULL("Saída Bloqueada", 0xFFF97316),
    OFFLINE("Offline • Sem Energia", 0xFFEF4444),
    NO_POWER("Offline • Sem Rede", 0xFFEF4444),
    LOW_POWER("Offline • Energia Insuficiente", 0xFFF59E0B),
    NO_DEPOSIT("Sem Depósito Compatível", 0xFFEF4444),
    PAUSED("Pausada pelo Jogador", 0xFF94A3B8);

    val isOffline: Boolean
        get() = this == OFFLINE || this == NO_POWER || this == LOW_POWER
}

data class PlacedBuilding(
    val x: Int,
    val y: Int,
    val type: BuildingType,
    val direction: Direction = Direction.EAST,
    val selectedRecipeId: String? = null,
    val filterItem: ItemType? = ItemType.IRON_ORE,
    val isPaused: Boolean = false,
    // Runtime simulation state
    val inputBuffer: Map<ItemType, Int> = emptyMap(),
    val outputBuffer: Map<ItemType, Int> = emptyMap(),
    val beltItem: ItemType? = null,
    val beltProgress: Float = 0f, // 0f..1f across the tile
    val craftProgress: Float = 0f, // 0f..1f
    val splitterToggle: Int = 0,
    val powerState: PowerState = PowerState.NOT_REQUIRED,
    val operationalState: OperationalState = OperationalState.WAITING_INPUT,
    val connectedPowerSourceX: Int? = null,
    val connectedPowerSourceY: Int? = null
) {
    fun totalStoredItems(): Int = inputBuffer.values.sum() + outputBuffer.values.sum()

    val isOffline: Boolean
        get() = type.powerConsumptionKw > 0 && !isPaused && (powerState.isOffline || operationalState.isOffline)
}

data class MapTile(
    val x: Int,
    val y: Int,
    val biome: BiomeType,
    val deposit: DepositType = DepositType.NONE
)

data class TechNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val costMoney: Int,
    val requiredItems: Map<ItemType, Int> = emptyMap(), // Sold/produced milestone requirement
    val researchSeconds: Int = 8,
    val prerequisiteId: String? = null,
    val tier: Int = 1,
    val unlocksDescription: List<String>
)

data class GameObjective(
    val id: String,
    val title: String,
    val description: String,
    val rewardMoney: Int,
    val isCompleted: Boolean = false
)

data class FloatingPopup(
    val id: Long,
    val tileX: Int,
    val tileY: Int,
    val text: String,
    val colorHex: Long,
    val ageProgress: Float = 0f // 0f..1f
)
