package com.example.data

import com.example.model.*

object GameCatalog {

    const val MAP_WIDTH = 34
    const val MAP_HEIGHT = 24

    val recipes: List<Recipe> = listOf(
        // SMELTER RECIPES
        Recipe(
            id = "REC_IRON_INGOT",
            name = "Fundir Barra de Ferro",
            machine = BuildingType.SMELTER,
            inputs = mapOf(ItemType.IRON_ORE to 1),
            output = ItemType.IRON_INGOT,
            outputCount = 1,
            craftSeconds = 1.5f
        ),
        Recipe(
            id = "REC_COPPER_INGOT",
            name = "Fundir Barra de Cobre",
            machine = BuildingType.SMELTER,
            inputs = mapOf(ItemType.COPPER_ORE to 1),
            output = ItemType.COPPER_INGOT,
            outputCount = 1,
            craftSeconds = 1.5f
        ),
        Recipe(
            id = "REC_STONE_BLOCK",
            name = "Sinterizar Bloco de Pedra",
            machine = BuildingType.SMELTER,
            inputs = mapOf(ItemType.STONE_RAW to 1),
            output = ItemType.STONE_BLOCK,
            outputCount = 1,
            craftSeconds = 1.3f
        ),
        Recipe(
            id = "REC_GOLD_INGOT",
            name = "Refinar Lingote de Ouro",
            machine = BuildingType.SMELTER,
            inputs = mapOf(ItemType.GOLD_ORE to 1),
            output = ItemType.GOLD_INGOT,
            outputCount = 1,
            craftSeconds = 2.0f
        ),
        Recipe(
            id = "REC_SILICON_WAFER",
            name = "Purificar Pastilha de Silício",
            machine = BuildingType.SMELTER,
            inputs = mapOf(ItemType.QUARTZ_ORE to 1),
            output = ItemType.SILICON_WAFER,
            outputCount = 1,
            craftSeconds = 1.8f,
            requiredTechId = "TECH_DESERT_EXPANSION"
        ),
        Recipe(
            id = "REC_COBALT_ALLOY",
            name = "Fundir Liga Criogênica",
            machine = BuildingType.SMELTER,
            inputs = mapOf(ItemType.COBALT_ORE to 1),
            output = ItemType.COBALT_ALLOY,
            outputCount = 1,
            craftSeconds = 2.2f,
            requiredTechId = "TECH_CRYO_EXPANSION"
        ),

        // SAWMILL RECIPES
        Recipe(
            id = "REC_WOOD_PLANK",
            name = "Serrar Tábuas Tratadas",
            machine = BuildingType.SAWMILL,
            inputs = mapOf(ItemType.WOOD_LOG to 1),
            output = ItemType.WOOD_PLANK,
            outputCount = 1,
            craftSeconds = 1.2f
        ),
        Recipe(
            id = "REC_WOOD_CRATE",
            name = "Montar Caixa de Transporte",
            machine = BuildingType.SAWMILL,
            inputs = mapOf(ItemType.WOOD_PLANK to 2),
            output = ItemType.WOOD_CRATE,
            outputCount = 1,
            craftSeconds = 1.6f
        ),

        // CUTTER RECIPES
        Recipe(
            id = "REC_IRON_PLATE",
            name = "Laminar Chapa de Ferro",
            machine = BuildingType.CUTTER,
            inputs = mapOf(ItemType.IRON_INGOT to 1),
            output = ItemType.IRON_PLATE,
            outputCount = 1,
            craftSeconds = 1.4f,
            requiredTechId = "TECH_METALWORKING"
        ),
        Recipe(
            id = "REC_IRON_GEAR",
            name = "Usinar Engrenagem",
            machine = BuildingType.CUTTER,
            inputs = mapOf(ItemType.IRON_INGOT to 1),
            output = ItemType.IRON_GEAR,
            outputCount = 1,
            craftSeconds = 1.6f,
            requiredTechId = "TECH_METALWORKING"
        ),
        Recipe(
            id = "REC_COPPER_WIRE",
            name = "Extrusar Fio de Cobre",
            machine = BuildingType.CUTTER,
            inputs = mapOf(ItemType.COPPER_INGOT to 1),
            output = ItemType.COPPER_WIRE,
            outputCount = 1,
            craftSeconds = 1.3f,
            requiredTechId = "TECH_METALWORKING"
        ),
        Recipe(
            id = "REC_STONE_BRICK",
            name = "Lapidar Tijolo Refratário",
            machine = BuildingType.CUTTER,
            inputs = mapOf(ItemType.STONE_BLOCK to 1),
            output = ItemType.STONE_BRICK,
            outputCount = 1,
            craftSeconds = 1.4f,
            requiredTechId = "TECH_METALWORKING"
        ),
        Recipe(
            id = "REC_GOLD_CONTACT",
            name = "Estampar Contato de Ouro",
            machine = BuildingType.CUTTER,
            inputs = mapOf(ItemType.GOLD_INGOT to 1),
            output = ItemType.GOLD_CONTACT,
            outputCount = 1,
            craftSeconds = 1.8f,
            requiredTechId = "TECH_METALWORKING"
        ),

        // ASSEMBLER RECIPES (2 distinct inputs)
        Recipe(
            id = "REC_INSULATED_CABLE",
            name = "Encapar Cabo Isolado",
            machine = BuildingType.ASSEMBLER,
            inputs = mapOf(ItemType.COPPER_WIRE to 1, ItemType.WOOD_PLANK to 1),
            output = ItemType.INSULATED_CABLE,
            outputCount = 1,
            craftSeconds = 1.8f,
            requiredTechId = "TECH_ASSEMBLY"
        ),
        Recipe(
            id = "REC_MECH_COMPONENT",
            name = "Montar Componente Mecânico",
            machine = BuildingType.ASSEMBLER,
            inputs = mapOf(ItemType.IRON_PLATE to 1, ItemType.IRON_GEAR to 1),
            output = ItemType.MECH_COMPONENT,
            outputCount = 1,
            craftSeconds = 2.0f,
            requiredTechId = "TECH_ASSEMBLY"
        ),
        Recipe(
            id = "REC_ELEC_CIRCUIT",
            name = "Soldar Circuito Elétrico",
            machine = BuildingType.ASSEMBLER,
            inputs = mapOf(ItemType.COPPER_WIRE to 2, ItemType.STONE_BRICK to 1),
            output = ItemType.ELEC_CIRCUIT,
            outputCount = 1,
            craftSeconds = 2.1f,
            requiredTechId = "TECH_ASSEMBLY"
        ),
        Recipe(
            id = "REC_ELEC_CIRCUIT_SILICON",
            name = "Circuito de Silício (Rápido)",
            machine = BuildingType.ASSEMBLER,
            inputs = mapOf(ItemType.COPPER_WIRE to 1, ItemType.SILICON_WAFER to 1),
            output = ItemType.ELEC_CIRCUIT,
            outputCount = 2,
            craftSeconds = 1.7f,
            requiredTechId = "TECH_DESERT_EXPANSION"
        ),
        Recipe(
            id = "REC_CRYO_PROCESSOR",
            name = "Integrar Processador Crio",
            machine = BuildingType.ASSEMBLER,
            inputs = mapOf(ItemType.ADV_CIRCUIT to 1, ItemType.COBALT_ALLOY to 1),
            output = ItemType.CRYO_PROCESSOR,
            outputCount = 1,
            craftSeconds = 2.5f,
            requiredTechId = "TECH_CRYO_EXPANSION"
        ),

        // MANUFACTURER RECIPES (2-3 inputs, high value)
        Recipe(
            id = "REC_REINFORCED_FRAME",
            name = "Forjar Estrutura Pesada",
            machine = BuildingType.MANUFACTURER,
            inputs = mapOf(
                ItemType.IRON_PLATE to 1,
                ItemType.STONE_BRICK to 1,
                ItemType.WOOD_PLANK to 1
            ),
            output = ItemType.REINFORCED_FRAME,
            outputCount = 1,
            craftSeconds = 2.2f,
            requiredTechId = "TECH_HIGH_TECH_MFG"
        ),
        Recipe(
            id = "REC_ADV_CIRCUIT",
            name = "Fabricar Circuito Avançado",
            machine = BuildingType.MANUFACTURER,
            inputs = mapOf(
                ItemType.ELEC_CIRCUIT to 1,
                ItemType.GOLD_CONTACT to 1,
                ItemType.INSULATED_CABLE to 1
            ),
            output = ItemType.ADV_CIRCUIT,
            outputCount = 1,
            craftSeconds = 2.6f,
            requiredTechId = "TECH_HIGH_TECH_MFG"
        ),
        Recipe(
            id = "REC_INDUSTRIAL_MODULE",
            name = "Montar Módulo Industrial",
            machine = BuildingType.MANUFACTURER,
            inputs = mapOf(
                ItemType.MECH_COMPONENT to 1,
                ItemType.ELEC_CIRCUIT to 1,
                ItemType.WOOD_CRATE to 1
            ),
            output = ItemType.INDUSTRIAL_MODULE,
            outputCount = 1,
            craftSeconds = 2.8f,
            requiredTechId = "TECH_HIGH_TECH_MFG"
        ),
        Recipe(
            id = "REC_QUANTUM_CORE",
            name = "Sintetizar Núcleo Nexus",
            machine = BuildingType.MANUFACTURER,
            inputs = mapOf(
                ItemType.CRYO_PROCESSOR to 1,
                ItemType.INDUSTRIAL_MODULE to 1,
                ItemType.GOLD_INGOT to 1
            ),
            output = ItemType.QUANTUM_CORE,
            outputCount = 1,
            craftSeconds = 3.5f,
            requiredTechId = "TECH_VOLCANIC_EXPANSION"
        )
    )

    fun getRecipeById(id: String?): Recipe? = recipes.find { it.id == id }

    fun getDefaultRecipeForMachine(type: BuildingType): Recipe? =
        recipes.firstOrNull { it.machine == type && it.requiredTechId == null }
            ?: recipes.firstOrNull { it.machine == type }

    val techTree: List<TechNode> = listOf(
        TechNode(
            id = "TECH_METALWORKING",
            title = "Metalurgia & Corte",
            subtitle = "Etapa 1 • Processamento Mecânico",
            description = "Desbloqueia a Prensa & Cortadora para produzir Chapas de Ferro, Engrenagens, Fios de Cobre, Tijolos e Contatos de Ouro.",
            costMoney = 90,
            researchSeconds = 5,
            prerequisiteId = null,
            tier = 1,
            unlocksDescription = listOf(
                "Máquina: Prensa & Cortadora",
                "Receitas: Chapa, Engrenagem, Fio, Tijolo e Contato de Ouro"
            )
        ),
        TechNode(
            id = "TECH_FAST_LOGISTICS",
            title = "Logística Expressa MK2",
            subtitle = "Etapa 1 • Velocidade & Extração",
            description = "Dobra a velocidade das esteiras com Esteiras Expressas MK2 e libera a Extratora Laser MK2.",
            costMoney = 160,
            researchSeconds = 6,
            prerequisiteId = "TECH_METALWORKING",
            tier = 1,
            unlocksDescription = listOf(
                "Esteira Expressa MK2 (2x velocidade, imune ao gelo)",
                "Extratora Laser MK2 (2x taxa de extração)"
            )
        ),
        TechNode(
            id = "TECH_SMART_ROUTING",
            title = "Roteamento Inteligente",
            subtitle = "Etapa 2 • Automação de Fluxo",
            description = "Libera o Filtro Seletor para separar linhas mistas e amplia a capacidade dos Silos de Carga para 250 itens.",
            costMoney = 200,
            researchSeconds = 6,
            prerequisiteId = "TECH_METALWORKING",
            tier = 2,
            unlocksDescription = listOf(
                "Logística: Filtro Seletor Configurável",
                "Silo de Carga: Capacidade ampliada (120 -> 250 itens)"
            )
        ),
        TechNode(
            id = "TECH_ASSEMBLY",
            title = "Automação de Montagem",
            subtitle = "Etapa 2 • Produtos Combinados",
            description = "Desbloqueia a Montadora Dupla para unir 2 recursos diferentes em Cabos Isolados, Componentes Mecânicos e Circuitos Elétricos.",
            costMoney = 280,
            researchSeconds = 8,
            prerequisiteId = "TECH_METALWORKING",
            tier = 2,
            unlocksDescription = listOf(
                "Máquina: Montadora Dupla",
                "Receitas: Cabo Isolado, Comp. Mecânico, Circuito Elétrico"
            )
        ),
        TechNode(
            id = "TECH_SOLAR_GRID",
            title = "Rede Solar & Alta Tensão",
            subtitle = "Etapa 2 • Energia Limpa",
            description = "Libera Painéis Solares Industriais e aumenta o alcance de todos os Postes Condutores em +2 blocos.",
            costMoney = 240,
            researchSeconds = 7,
            prerequisiteId = "TECH_FAST_LOGISTICS",
            tier = 2,
            unlocksDescription = listOf(
                "Energia: Painel Solar Industrial (40 kW / 60 kW no Deserto)",
                "Bônus: +2 blocos de raio nos Postes Condutores"
            )
        ),
        TechNode(
            id = "TECH_DESERT_EXPANSION",
            title = "Expedição: Dunas de Silício",
            subtitle = "Etapa 3 • Desbloqueio de Bioma Árido",
            description = "Permite construir nas Dunas de Silício, neutraliza a penalidade de Tempestade de Areia e libera o refino de Quartzo em Pastilhas de Silício.",
            costMoney = 380,
            researchSeconds = 9,
            prerequisiteId = "TECH_SOLAR_GRID",
            tier = 3,
            unlocksDescription = listOf(
                "Bioma Desbloqueado: Dunas de Silício (Ouro & Quartzo)",
                "Proteção contra Tempestade de Areia para máquinas",
                "Receita: Pastilha de Silício e Circuito de Silício Duplo"
            )
        ),
        TechNode(
            id = "TECH_HIGH_TECH_MFG",
            title = "Manufatura Pesada Nexus",
            subtitle = "Etapa 3 • Indústria de 3 Insumos",
            description = "Libera a Fábrica Nexus capaz de combinar 3 linhas em Estruturas Pesadas, Circuitos Avançados e Módulos Industriais.",
            costMoney = 520,
            researchSeconds = 10,
            prerequisiteId = "TECH_ASSEMBLY",
            tier = 3,
            unlocksDescription = listOf(
                "Máquina: Fábrica Nexus (3 entradas)",
                "Receitas: Estrutura Pesada, Circuito Avançado, Módulo Industrial"
            )
        ),
        TechNode(
            id = "TECH_CRYO_EXPANSION",
            title = "Perfuração Boreal Criogênica",
            subtitle = "Etapa 4 • Desbloqueio da Tundra",
            description = "Desbloqueia a Tundra Boreal, aquece esteiras contra nevascas e permite refinar Cristal de Cobalto em Ligas e Processadores Criogênicos.",
            costMoney = 750,
            researchSeconds = 11,
            prerequisiteId = "TECH_HIGH_TECH_MFG",
            tier = 4,
            unlocksDescription = listOf(
                "Bioma Desbloqueado: Tundra Boreal (Cobalto & Ouro)",
                "Aquecimento de esteiras na Tundra (+20% velocidade)",
                "Receitas: Liga Criogênica & Processador Crio ($580)"
            )
        ),
        TechNode(
            id = "TECH_VOLCANIC_EXPANSION",
            title = "Domínio Geotérmico & Núcleo Nexus",
            subtitle = "Etapa 4 • Caldeira Magmática & Fim de Jogo",
            description = "Desbloqueia a Caldeira Magmática, Usinas Geotérmicas de 160-220 kW e a síntese do lendário Núcleo Nexus ($1.400).",
            costMoney = 1200,
            researchSeconds = 12,
            prerequisiteId = "TECH_CRYO_EXPANSION",
            tier = 4,
            unlocksDescription = listOf(
                "Bioma Desbloqueado: Caldeira Magmática",
                "Energia: Usina Geotérmica (até 220 kW)",
                "Receita Suprema: Núcleo Nexus ($1.400 cada)"
            )
        )
    )

    val initialObjectives: List<GameObjective> = listOf(
        GameObjective(
            id = "OBJ_FIRST_EXTRACTOR",
            title = "Primeira Extração",
            description = "Posicione uma Extratora MK1 sobre uma jazida de Ferro, Cobre, Madeira ou Pedra.",
            rewardMoney = 40
        ),
        GameObjective(
            id = "OBJ_FIRST_SALE",
            title = "Fluxo de Caixa",
            description = "Conecte esteiras até o Terminal de Venda e venda 10 recursos de qualquer tipo.",
            rewardMoney = 60
        ),
        GameObjective(
            id = "OBJ_SMELT_METAL",
            title = "Era da Fundição",
            description = "Construa uma Fundição Térmica energizada e produza 5 Barras (Ferro, Cobre ou Ouro) ou Tábuas.",
            rewardMoney = 90
        ),
        GameObjective(
            id = "OBJ_POWER_EXPANSION",
            title = "Engenheiro Eletricista",
            description = "Amplie sua Rede Elétrica para pelo menos 100 kW de capacidade total usando Dínamos ou Painéis Solares.",
            rewardMoney = 120
        ),
        GameObjective(
            id = "OBJ_FIRST_TECH",
            title = "Inovação Industrial",
            description = "Conclua sua primeira Pesquisa no laboratório tecnológico.",
            rewardMoney = 110
        ),
        GameObjective(
            id = "OBJ_MULTI_RESOURCE",
            title = "Linha de Montagem",
            description = "Use uma Montadora Dupla para fabricar e vender Componentes Mecânicos, Cabos ou Circuitos.",
            rewardMoney = 220
        ),
        GameObjective(
            id = "OBJ_BIOME_PIONEER",
            title = "Pioneiro Multi-Biomas",
            description = "Desbloqueie e construa uma estrutura nas Dunas de Silício, Tundra Boreal ou Caldeira Magmática.",
            rewardMoney = 350
        ),
        GameObjective(
            id = "OBJ_TYCOON_5000",
            title = "Magnata de ForjaNexus",
            description = "Acumule $2.500 em caixa ou produza um Módulo Industrial / Processador Crio.",
            rewardMoney = 600
        )
    )

    fun generateWorldMap(): List<MapTile> {
        val tiles = mutableListOf<MapTile>()
        for (y in 0 until MAP_HEIGHT) {
            for (x in 0 until MAP_WIDTH) {
                val biome = when {
                    x < 17 && y < 13 -> BiomeType.TEMPERATE_VALLEY
                    x >= 17 && y < 12 -> BiomeType.SCORCHED_DUNES
                    x < 17 && y >= 13 -> BiomeType.FROZEN_TUNDRA
                    else -> BiomeType.VOLCANIC_CALDERA
                }
                val deposit = determineDeposit(x, y, biome)
                tiles.add(MapTile(x = x, y = y, biome = biome, deposit = deposit))
            }
        }
        return tiles
    }

    private fun determineDeposit(x: Int, y: Int, biome: BiomeType): DepositType {
        // Starter Temperate Valley (x: 0..16, y: 0..12)
        // Starter Hub is at (8, 6) and Starter Biomass Gen at (8, 4)
        if (biome == BiomeType.TEMPERATE_VALLEY) {
            return when {
                // Close Iron cluster to the West of Starter Hub
                (x in 4..5 && y in 5..7) -> DepositType.IRON_VEIN
                // Close Copper cluster to the South-West
                (x in 5..6 && y in 9..10) -> DepositType.COPPER_VEIN
                // Wood grove to the North-West
                (x in 3..5 && y in 2..3) -> DepositType.TIMBER_GROVE
                // Stone quarry to the East of Starter Hub
                (x in 11..12 && y in 4..5) -> DepositType.STONE_QUARRY
                // Secondary Iron & Wood in Valley
                (x in 13..14 && y in 8..9) -> DepositType.IRON_VEIN
                (x in 10..11 && y in 10..11) -> DepositType.TIMBER_GROVE
                // Border Gold outpost inside Valley near Desert border
                (x == 15 && y in 5..6) -> DepositType.GOLD_VEIN
                else -> DepositType.NONE
            }
        }

        // Scorched Dunes (x: 17..33, y: 0..11)
        if (biome == BiomeType.SCORCHED_DUNES) {
            return when {
                (x in 19..21 && y in 3..4) -> DepositType.GOLD_VEIN
                (x in 23..25 && y in 6..8) -> DepositType.QUARTZ_CRYSTAL
                (x in 28..30 && y in 2..4) -> DepositType.GOLD_VEIN
                (x in 20..21 && y in 9..10) -> DepositType.COPPER_VEIN
                (x in 29..31 && y in 8..9) -> DepositType.QUARTZ_CRYSTAL
                (x in 25..26 && y in 2..3) -> DepositType.STONE_QUARRY
                else -> DepositType.NONE
            }
        }

        // Frozen Tundra (x: 0..16, y: 13..23)
        if (biome == BiomeType.FROZEN_TUNDRA) {
            return when {
                (x in 4..6 && y in 15..16) -> DepositType.COBALT_GLACIER
                (x in 9..11 && y in 18..19) -> DepositType.IRON_VEIN
                (x in 3..5 && y in 20..21) -> DepositType.GOLD_VEIN
                (x in 13..14 && y in 15..16) -> DepositType.TIMBER_GROVE
                (x in 12..14 && y in 20..21) -> DepositType.COBALT_GLACIER
                else -> DepositType.NONE
            }
        }

        // Volcanic Caldera (x: 17..33, y: 12..23)
        return when {
            (x in 20..21 && y in 14..15) -> DepositType.MAGMA_VENT
            (x in 26..27 && y in 17..18) -> DepositType.MAGMA_VENT
            (x in 22..24 && y in 19..20) -> DepositType.GOLD_VEIN
            (x in 29..31 && y in 14..16) -> DepositType.COBALT_GLACIER
            (x in 18..19 && y in 20..21) -> DepositType.STONE_QUARRY
            (x in 28..30 && y in 20..21) -> DepositType.IRON_VEIN
            else -> DepositType.NONE
        }
    }
}
