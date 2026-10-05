package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.GameCatalog
import com.example.model.*
import com.example.viewmodel.ActiveModal
import com.example.viewmodel.FactoryUiState

@Composable
fun MainMenuScreen(
    uiState: FactoryUiState,
    onContinueGame: () -> Unit,
    onStartNewGameRequest: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B111E), Color(0xFF152238), Color(0xFF0E1726))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("main_menu_screen")
    ) {
        // Decorative background banner
        Image(
            painter = painterResource(id = R.drawable.img_menu_banner_1791204642034),
            contentDescription = "Banner da fábrica 2.5D em múltiplos biomas",
            contentScale = ContentScale.Crop,
            alpha = 0.32f,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Hero Column
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .padding(end = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "PIXEL ART 2.5D • AUTOMAÇÃO INDUSTRIAL",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "ForjaNexus",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Construa extratoras, esteiras inteligentes, usinas elétricas e linhas de montagem através de 4 biomas dinâmicos.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )

                // Feature Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureBadge("4 Biomas Únicos", Color(0xFF4ADE80))
                    FeatureBadge("Rede Elétrica kW", Color(0xFF38BDF8))
                    FeatureBadge("22 Receitas", Color(0xFFFBBF24))
                }
            }

            // Right Action Buttons Card
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .widthIn(max = 380.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162032).copy(alpha = 0.94f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.hasSavedGame) {
                        Button(
                            onClick = onContinueGame,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("continue_game_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Continuar Fábrica",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Button(
                        onClick = onStartNewGameRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.hasSavedGame) Color(0xFF2563EB) else Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("new_game_button")
                    ) {
                        Icon(Icons.Default.PrecisionManufacturing, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Nova Partida",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (uiState.hasSavedGame) Color.White else Color(0xFF0F172A)
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenGuide,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_guide_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(Modifier.width(8.dp))
                        Text("Cadeias, Biomas & Guia", color = Color(0xFFE2E8F0))
                    }

                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF94A3B8))
                        Spacer(Modifier.width(8.dp))
                        Text("Configurações & Áudio", color = Color(0xFFE2E8F0))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ConfirmNewGameDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCBD5E1),
        title = { Text("Iniciar Nova Partida?", fontWeight = FontWeight.Bold) },
        text = {
            Text("Você já possui uma fábrica salva. Iniciar uma nova partida substituirá o progresso atual. Deseja confirmar?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.testTag("confirm_new_game_button")
            ) {
                Text("Sim, Criar Nova Fábrica", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun ResearchModal(
    uiState: FactoryUiState,
    onStartResearch: (String) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("research_modal"),
            color = Color(0xFF111827),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Centro de Pesquisa & Tecnologias",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Saldo Disponível: $${uiState.money} • Desbloqueadas: ${uiState.unlockedTechIds.size}/${GameCatalog.techTree.size}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_modal_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(GameCatalog.techTree, key = { it.id }) { tech ->
                        val isUnlocked = tech.id in uiState.unlockedTechIds
                        val isActive = uiState.activeResearchId == tech.id
                        val prereqMet = tech.prerequisiteId == null || tech.prerequisiteId in uiState.unlockedTechIds
                        val prereqTitle = GameCatalog.techTree.find { it.id == tech.prerequisiteId }?.title

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isUnlocked -> Color(0xFF064E3B).copy(alpha = 0.45f)
                                    isActive -> Color(0xFF1E3A8A).copy(alpha = 0.65f)
                                    prereqMet -> Color(0xFF1E293B)
                                    else -> Color(0xFF0F172A)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            tech.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            tech.subtitle,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        tech.description,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    tech.unlocksDescription.forEach { u ->
                                        Text(
                                            "• $u",
                                            color = Color(0xFF4ADE80),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    if (!prereqMet && prereqTitle != null) {
                                        Text(
                                            "Requer pré-requisito: $prereqTitle",
                                            color = Color(0xFFF87171),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    if (isActive) {
                                        val progress = (uiState.activeResearchProgressSec / tech.researchSeconds).coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            color = Color(0xFF38BDF8),
                                            trackColor = Color(0xFF0F172A),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .height(6.dp)
                                        )
                                    }
                                }

                                when {
                                    isUnlocked -> {
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "CONCLUÍDA",
                                                color = Color(0xFF34D399),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                    isActive -> {
                                        Text(
                                            "Em andamento\n(${tech.researchSeconds}s)",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    else -> {
                                        Button(
                                            onClick = { onStartResearch(tech.id) },
                                            enabled = prereqMet && uiState.money >= tech.costMoney && uiState.activeResearchId == null,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            modifier = Modifier.testTag("research_btn_${tech.id}")
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Pesquisar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("$${tech.costMoney} • ${tech.researchSeconds}s", fontSize = 11.sp)
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
    }
}

@Composable
fun ObjectivesAndEconomyModal(
    uiState: FactoryUiState,
    onClose: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("objectives_modal"),
            color = Color(0xFF111827),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Objetivos & Relatório Econômico",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Caixa: $${uiState.money} • Ganho Total: $${uiState.totalEarned} • Ritmo: +$${uiState.recentIncomePerMin}/min",
                                color = Color(0xFF4ADE80),
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        label = { Text("Marcos de Progressão (${uiState.objectives.count { it.isCompleted }}/${uiState.objectives.size})") }
                    )
                    FilterChip(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        label = { Text("Tabela de Produtos, Produção & Vendas") }
                    )
                }

                if (tabIndex == 0) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.objectives, key = { it.id }) { obj ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (obj.isCompleted) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            obj.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            obj.description,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Surface(
                                        color = if (obj.isCompleted) Color(0xFF10B981) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (obj.isCompleted) "✓ Concluído (+$${obj.rewardMoney})" else "Recompensa: +$${obj.rewardMoney}",
                                            color = if (obj.isCompleted) Color(0xFF052E16) else Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(ItemType.entries) { item ->
                            val prod = uiState.itemProducedCounts[item] ?: 0
                            val sold = uiState.itemSoldCounts[item] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(item.composeColor, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item.shortCode, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(item.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(item.category.label, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Valor: $${item.sellValue}", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Prod: $prod", color = Color(0xFF38BDF8), fontSize = 12.sp)
                                    Text("Vendidos: $sold", color = Color(0xFF4ADE80), fontSize = 12.sp)
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
fun CatalogAndBiomeHelpModal(
    uiState: FactoryUiState,
    onJumpToBiome: (BiomeType) -> Unit,
    onRestartTutorial: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("guide_modal"),
            color = Color(0xFF111827),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Enciclopédia da Fábrica: Receitas, Biomas & Energia",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        TextButton(onClick = onRestartTutorial) {
                            Text("Rever Tutorial", color = Color(0xFF38BDF8))
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Receitas & Cadeias (${GameCatalog.recipes.size})") }
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Biomas & Clima (4)") }
                    )
                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("Como Funciona a Rede Elétrica") }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(GameCatalog.recipes, key = { it.id }) { recipe ->
                                val inputsText = recipe.inputs.entries.joinToString(" + ") { "${it.value}x ${it.key.displayName}" }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "${recipe.name} → ${recipe.outputCount}x ${recipe.output.displayName}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                "Insumos: $inputsText",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                "Máquina: ${recipe.machine.displayName} (${recipe.machine.powerConsumptionKw} kW) • Tempo: ${recipe.craftSeconds}s",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "Venda: $${recipe.output.sellValue}/un",
                                                color = Color(0xFFFBBF24),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(BiomeType.entries) { biome ->
                                val unlocked = biome.requiredTechId == null || biome.requiredTechId in uiState.unlockedTechIds
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .background(Color(biome.accentHex), CircleShape)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    biome.displayName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    if (unlocked) "✓ Desbloqueado" else "🔒 Requer Pesquisa",
                                                    color = if (unlocked) Color(0xFF4ADE80) else Color(0xFFF87171),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                biome.description,
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                            Text(
                                                "Clima: ${biome.weatherName} • Custo de Obra: ${(biome.buildCostMultiplier * 100).toInt()}% • Eficiência Solar: ${(biome.solarEfficiency * 100).toInt()}%",
                                                color = Color(biome.accentHex),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                onJumpToBiome(biome)
                                                onClose()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                        ) {
                                            Text("Ver no Mapa", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Guia de Energia & Automação",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "1. GERADORES: Dínamos a Biomassa (50-75 kW), Painéis Solares (40-60 kW) e Usinas Geotérmicas (160-220 kW) fornecem energia para sua rede.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                "2. POSTES CONDUTORES: Retransmitem a energia elétrica por 6 blocos de raio. Construa linhas de Postes para levar eletricidade até outros biomas e jazidas distantes.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                "3. ESTADOS VISUAIS DE ENERGIA: Luz Verde indica máquina 100% energizada. Luz Âmbar indica Sobrecarga (demanda maior que a geração total: máquinas operam em ritmo reduzido). Luz Vermelha indica máquina fora do alcance de qualquer gerador ou poste.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                "4. LOGÍSTICA INTELIGENTE: Use Divisores para repartir uma esteira em até 3 rotas, Junções para unir esteiras e Filtros Seletores para separar itens específicos.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsModal(
    uiState: FactoryUiState,
    onUpdateAudio: (Float?, Float?, Boolean?) -> Unit,
    onUpdateAccessibility: (Boolean?, Float?) -> Unit,
    onManualSave: () -> Unit,
    onReturnToMainMenu: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("settings_modal"),
            color = Color(0xFF111827),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Configurações, Áudio & Desempenho",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }

                // Mute switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Silenciar Todo o Áudio", color = Color(0xFFE2E8F0), fontSize = 14.sp)
                    Switch(
                        checked = uiState.isMuted,
                        onCheckedChange = { onUpdateAudio(null, null, it) },
                        modifier = Modifier.testTag("mute_audio_switch")
                    )
                }

                // SFX Volume
                Text(
                    "Volume dos Efeitos Industriais: ${(uiState.sfxVolume * 100).toInt()}%",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
                Slider(
                    value = uiState.sfxVolume,
                    onValueChange = { onUpdateAudio(it, null, null) },
                    enabled = !uiState.isMuted
                )

                // Music Volume
                Text(
                    "Volume da Música Ambiente: ${(uiState.musicVolume * 100).toInt()}%",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
                Slider(
                    value = uiState.musicVolume,
                    onValueChange = { onUpdateAudio(null, it, null) },
                    enabled = !uiState.isMuted
                )

                // Accessibility & Performance: Reduced effects
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Econômico (Reduzir Efeitos e Clima)", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                        Text("Ideal para celulares modestos e economia de bateria", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = uiState.reducedEffects,
                        onCheckedChange = { onUpdateAccessibility(it, null) },
                        modifier = Modifier.testTag("reduced_effects_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onManualSave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f).testTag("manual_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Salvar Agora")
                    }

                    OutlinedButton(
                        onClick = onReturnToMainMenu,
                        modifier = Modifier.weight(1f).testTag("return_menu_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Menu Principal")
                    }
                }
            }
        }
    }
}

@Composable
fun PowerSystemModal(
    uiState: FactoryUiState,
    onToggleOverlay: () -> Unit,
    onFocusMachine: (Int, Int) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("power_system_modal"),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Central da Rede Elétrica (Power System)",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Monitoramento em tempo real de geração, consumo e máquinas offline",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleOverlay,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("toggle_power_overlay_button")
                        ) {
                            Text(
                                if (uiState.showPowerGridOverlay) "Ocultar Malha no Mapa" else "Mostrar Malha no Mapa",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        IconButton(onClick = onClose, modifier = Modifier.testTag("close_power_modal_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Summary KPI Row
                val netKw = uiState.totalPowerGeneratedKw - uiState.totalPowerDemandKw
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PowerKpiCard(
                        title = "Geração Total",
                        value = "${uiState.totalPowerGeneratedKw} kW",
                        accent = Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f)
                    )
                    PowerKpiCard(
                        title = "Consumo Exigido",
                        value = "${uiState.totalPowerDemandKw} kW",
                        accent = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    PowerKpiCard(
                        title = "Saldo da Rede",
                        value = if (netKw >= 0) "+$netKw kW" else "$netKw kW",
                        accent = if (netKw >= 0) Color(0xFF4ADE80) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    PowerKpiCard(
                        title = "Máquinas Offline",
                        value = "${uiState.offlineMachineCount}",
                        accent = if (uiState.offlineMachineCount == 0) Color(0xFF94A3B8) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f).testTag("offline_machines_count_card")
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column: Active Generators
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            "Geradores Ativos (${uiState.generatorNodes.size})",
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        if (uiState.generatorNodes.isEmpty()) {
                            Text(
                                "Nenhum gerador ativo. Construa um Dínamo a Biomassa, Painel Solar ou Usina Geotérmica.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(uiState.generatorNodes, key = { "${it.x}_${it.y}" }) { gen ->
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onFocusMachine(gen.x, gen.y) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "${gen.type.displayName} (${gen.x}, ${gen.y})",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = if (gen.isBoosted) {
                                                        "${gen.biome.displayName} • Bônus de Eficiência Ativo!"
                                                    } else {
                                                        gen.biome.displayName
                                                    },
                                                    color = if (gen.isBoosted) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Text(
                                                "+${gen.productionKw} kW",
                                                color = Color(0xFF4ADE80),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Consumer Machines & Offline Status
                    Column(modifier = Modifier.weight(1.15f).fillMaxHeight()) {
                        Text(
                            "Consumidores & Estado Elétrico (${uiState.consumerNodes.size})",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        if (uiState.consumerNodes.isEmpty()) {
                            Text(
                                "Nenhuma máquina consumidora posicionada.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(uiState.consumerNodes, key = { "${it.x}_${it.y}" }) { node ->
                                    val isOffline = !node.isPowered
                                    Surface(
                                        color = if (isOffline) Color(0xFF450A0A) else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isOffline) Color(0xFFEF4444) else Color(0xFF334155)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onFocusMachine(node.x, node.y) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "${node.type.displayName} (${node.x}, ${node.y})",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = node.powerState.label,
                                                    color = if (isOffline) Color(0xFFFCA5A5) else Color(0xFF4ADE80),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = if (isOffline) "OFFLINE (-${node.requiredKw} kW)" else "-${node.requiredKw} kW",
                                                color = if (isOffline) Color(0xFFF87171) else Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
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
}

@Composable
private fun PowerKpiCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(
                value,
                color = accent,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
