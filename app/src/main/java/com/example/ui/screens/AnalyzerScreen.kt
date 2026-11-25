package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CustomAnalysisResult
import com.example.domain.CustomMatchInput
import com.example.ui.components.MontanteScoreBadge
import com.example.ui.components.ProbabilityMeterBar
import com.example.ui.components.RiskTierPill
import com.example.ui.theme.LocalMontanteColors
import java.util.Locale

private val marketOptions = listOf(
    "1X" to "Double Chance 1X",
    "X2" to "Double Chance X2",
    "O15" to "+1,5 Buts",
    "U35" to "-3,5 Buts",
    "DNB1" to "Remboursé si Nul (Dom.)",
    "DNB2" to "Remboursé si Nul (Ext.)",
    "BTTS" to "2 Équipes Marquent",
    "1" to "Victoire Domicile",
    "2" to "Victoire Extérieur"
)

@Composable
fun AnalyzerScreen(
    input: CustomMatchInput,
    result: CustomAnalysisResult,
    onUpdateInput: ((CustomMatchInput) -> CustomMatchInput) -> Unit,
    onEngageCustom: (String, String, Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalMontanteColors.current
    var oddsText by remember(input.bookmakerOdds) {
        mutableStateOf("%.2f".format(Locale.US, input.bookmakerOdds))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Functions,
                    contentDescription = null,
                    tint = semantic.safeEmerald,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.analyzer_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Calculez vos probabilités exactes avant d'engager un palier",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Result Verdict Card at top so users see instant feedback as they tweak inputs
        item {
            val selectedLabel = marketOptions.firstOrNull { it.first == input.selectedMarketKey }?.second ?: input.selectedMarketKey
            Card(
                colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, semantic.safeEmerald.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            RiskTierPill(tier = result.riskTier)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = selectedLabel,
                                style = MaterialTheme.typography.titleLarge,
                                color = semantic.safeEmerald
                            )
                        }
                        MontanteScoreBadge(score = result.montanteScore)
                    }

                    ProbabilityMeterBar(
                        probability = result.selectedMarketProb,
                        riskTier = result.riskTier
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyzerMetricBox(
                            title = "Probabilité",
                            value = "${"%.1f".format(result.selectedMarketProb * 100)}%",
                            color = semantic.safeEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyzerMetricBox(
                            title = "Cote Juste",
                            value = "%.2f".format(result.fairOdds),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyzerMetricBox(
                            title = "Espérance",
                            value = "${if (result.expectedValuePercent >= 0) "+" else ""}${"%.1f".format(result.expectedValuePercent)}%",
                            color = if (result.expectedValuePercent >= 0) semantic.safeEmerald else semantic.riskCrimson,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Buts attendus : ${"%.2f".format(result.lambdaHome)} – ${"%.2f".format(result.lambdaAway)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.telemetryCyan
                        )
                        Text(
                            text = "Mise Kelly 1/4 : ${"%.1f".format(result.quarterKellyPercent)}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.boostAmber
                        )
                    }

                    if (result.topScores.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scores probables :", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            result.topScores.forEach { sc ->
                                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "${sc.homeGoals}-${sc.awayGoals} (${"%.0f".format(sc.probability * 100)}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onEngageCustom(
                                "${input.homeName.ifBlank { "Domicile" }} vs ${input.awayName.ifBlank { "Extérieur" }}",
                                selectedLabel,
                                input.bookmakerOdds,
                                result.selectedMarketProb
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("engage_custom_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_to_montante))
                    }
                }
            }
        }

        // Market & Bookmaker Odds selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.medium)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Marché ciblé & Cote proposée", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        marketOptions.forEach { (key, label) ->
                            FilterChip(
                                selected = input.selectedMarketKey == key,
                                onClick = { onUpdateInput { it.copy(selectedMarketKey = key) } },
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = oddsText,
                        onValueChange = { raw ->
                            oddsText = raw
                            val parsed = raw.replace(',', '.').toDoubleOrNull()
                            if (parsed != null && parsed >= 1.01) {
                                onUpdateInput { it.copy(bookmakerOdds = parsed) }
                            }
                        },
                        label = { Text("Cote proposée par votre site de paris") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Home Team Inputs
        item {
            TeamStatsEditorCard(
                title = "Équipe Domicile",
                name = input.homeName,
                played = input.homePlayed,
                wins = input.homeWins,
                draws = input.homeDraws,
                goalsFor = input.homeGoalsFor,
                goalsAgainst = input.homeGoalsAgainst,
                form = input.homeForm,
                onNameChange = { v -> onUpdateInput { it.copy(homeName = v) } },
                onPlayedChange = { v -> onUpdateInput { it.copy(homePlayed = v) } },
                onWinsChange = { v -> onUpdateInput { it.copy(homeWins = v) } },
                onDrawsChange = { v -> onUpdateInput { it.copy(homeDraws = v) } },
                onGoalsForChange = { v -> onUpdateInput { it.copy(homeGoalsFor = v) } },
                onGoalsAgainstChange = { v -> onUpdateInput { it.copy(homeGoalsAgainst = v) } },
                onFormToggle = { idx ->
                    val chars = input.homeForm.padEnd(5, 'W').take(5).toCharArray()
                    chars[idx] = nextFormChar(chars[idx])
                    onUpdateInput { it.copy(homeForm = String(chars)) }
                }
            )
        }

        // Away Team Inputs
        item {
            TeamStatsEditorCard(
                title = "Équipe Extérieur",
                name = input.awayName,
                played = input.awayPlayed,
                wins = input.awayWins,
                draws = input.awayDraws,
                goalsFor = input.awayGoalsFor,
                goalsAgainst = input.awayGoalsAgainst,
                form = input.awayForm,
                onNameChange = { v -> onUpdateInput { it.copy(awayName = v) } },
                onPlayedChange = { v -> onUpdateInput { it.copy(awayPlayed = v) } },
                onWinsChange = { v -> onUpdateInput { it.copy(awayWins = v) } },
                onDrawsChange = { v -> onUpdateInput { it.copy(awayDraws = v) } },
                onGoalsForChange = { v -> onUpdateInput { it.copy(awayGoalsFor = v) } },
                onGoalsAgainstChange = { v -> onUpdateInput { it.copy(awayGoalsAgainst = v) } },
                onFormToggle = { idx ->
                    val chars = input.awayForm.padEnd(5, 'D').take(5).toCharArray()
                    chars[idx] = nextFormChar(chars[idx])
                    onUpdateInput { it.copy(awayForm = String(chars)) }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AnalyzerMetricBox(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

@Composable
private fun TeamStatsEditorCard(
    title: String,
    name: String,
    played: Int,
    wins: Int,
    draws: Int,
    goalsFor: Int,
    goalsAgainst: Int,
    form: String,
    onNameChange: (String) -> Unit,
    onPlayedChange: (Int) -> Unit,
    onWinsChange: (Int) -> Unit,
    onDrawsChange: (Int) -> Unit,
    onGoalsForChange: (Int) -> Unit,
    onGoalsAgainstChange: (Int) -> Unit,
    onFormToggle: (Int) -> Unit
) {
    val semantic = LocalMontanteColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.medium)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nom de l'équipe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntStatField("Matchs", played, onPlayedChange, Modifier.weight(1f))
                IntStatField("Victoires", wins, onWinsChange, Modifier.weight(1f))
                IntStatField("Nuls", draws, onDrawsChange, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntStatField("Buts pour", goalsFor, onGoalsForChange, Modifier.weight(1f))
                IntStatField("Buts contre", goalsAgainst, onGoalsAgainstChange, Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Forme (5 derniers, toucher pour changer) :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chars = form.padEnd(5, 'W').take(5)
                    chars.forEachIndexed { index, ch ->
                        val (label, bg, fg) = when (ch.uppercaseChar()) {
                            'W' -> Triple("V", semantic.safeContainer, semantic.safeEmerald)
                            'D' -> Triple("N", semantic.boostContainer, semantic.boostAmber)
                            else -> Triple("D", semantic.riskContainer, semantic.riskCrimson)
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { onFormToggle(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = fg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntStatField(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw.filter { it.isDigit() }
            text.toIntOrNull()?.let { onChange(it.coerceIn(0, 200)) }
        },
        label = { Text(label, maxLines = 1) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

private fun nextFormChar(current: Char): Char {
    return when (current.uppercaseChar()) {
        'W' -> 'D'
        'D' -> 'L'
        else -> 'W'
    }
}
