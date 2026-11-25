package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CampaignStatus
import com.example.data.local.MontanteCampaignEntity
import com.example.data.local.MontanteStepEntity
import com.example.data.local.StepOutcome
import com.example.domain.LadderSimulationStep
import com.example.ui.components.BankrollCurveChart
import com.example.ui.theme.LocalMontanteColors
import com.example.ui.viewmodel.LadderSimulatorParams
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MontanteTrackerScreen(
    campaigns: List<MontanteCampaignEntity>,
    activeCampaign: MontanteCampaignEntity?,
    stepsByCampaign: Map<Long, List<MontanteStepEntity>>,
    simulatorParams: LadderSimulatorParams,
    simulatedSteps: List<LadderSimulationStep>,
    onSelectCampaign: (Long) -> Unit,
    onCreateCampaign: (String, Double, Double, Double, Double, Double) -> Unit,
    onDeleteCampaign: (Long) -> Unit,
    onAddManualStep: (Long, String, String, Double, Double, Double) -> Unit,
    onResolveStep: (Long, String) -> Unit,
    onUpdateSimulator: ((LadderSimulatorParams) -> LadderSimulatorParams) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddStepDialog by remember { mutableStateOf(false) }
    var simulatorExpanded by remember { mutableStateOf(false) }

    val semantic = LocalMontanteColors.current
    val currentSteps = activeCampaign?.let { stepsByCampaign[it.id].orEmpty() } ?: emptyList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mes Montantes",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Suivi de paliers & capitalisation sécurisée",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("create_campaign_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nouvelle")
                }
            }
        }

        if (campaigns.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    campaigns.forEach { campaign ->
                        FilterChip(
                            selected = campaign.id == activeCampaign?.id,
                            onClick = { onSelectCampaign(campaign.id) },
                            label = {
                                Text(
                                    text = "${campaign.name} (${"%.0f".format(campaign.currentBankroll)}€)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }

        if (activeCampaign == null) {
            item {
                EmptyCampaignStarterCard(
                    onPresetSelect = { name, init, target, ratio ->
                        onCreateCampaign(name, init, target, ratio, 1.20, 1.48)
                    },
                    onCustomClick = { showCreateDialog = true }
                )
            }
        } else {
            item {
                ActiveCampaignDashboardCard(
                    campaign = activeCampaign,
                    steps = currentSteps,
                    onAddManualStep = { showAddStepDialog = true },
                    onDeleteCampaign = { onDeleteCampaign(activeCampaign.id) }
                )
            }

            if (currentSteps.isNotEmpty()) {
                item {
                    Text(
                        text = "Paliers de la campagne (${currentSteps.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(
                    items = currentSteps.reversed(),
                    key = { it.id }
                ) { step ->
                    MontanteStepCard(
                        step = step,
                        onResolve = { outcome -> onResolveStep(step.id, outcome) }
                    )
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.medium)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = semantic.safeEmerald,
                                modifier = Modifier.size(36.dp)
                            )
                            Text("Prêt pour le Palier #${activeCampaign.currentStepNumber}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Sélectionnez une option dans « Options du Jour » ou saisissez un pari manuellement.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { showAddStepDialog = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Ajouter un pari manuel")
                            }
                        }
                    }
                }
            }
        }

        item {
            LadderSimulatorCard(
                expanded = simulatorExpanded,
                onToggle = { simulatorExpanded = !simulatorExpanded },
                params = simulatorParams,
                steps = simulatedSteps,
                onUpdate = onUpdateSimulator
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showCreateDialog) {
        CreateCampaignDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, init, target, ratio, minOdds, maxOdds ->
                onCreateCampaign(name, init, target, ratio, minOdds, maxOdds)
                showCreateDialog = false
            }
        )
    }

    if (showAddStepDialog && activeCampaign != null) {
        AddManualStepDialog(
            campaign = activeCampaign,
            onDismiss = { showAddStepDialog = false },
            onConfirm = { matchTitle, marketLabel, odds, prob, stake ->
                onAddManualStep(activeCampaign.id, matchTitle, marketLabel, odds, prob, stake)
                showAddStepDialog = false
            }
        )
    }
}

@Composable
private fun EmptyCampaignStarterCard(
    onPresetSelect: (String, Double, Double, Double) -> Unit,
    onCustomClick: () -> Unit
) {
    val semantic = LocalMontanteColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.no_active_campaign),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Démarrez une montante structurée pour suivre vos paliers et sécuriser vos gains étape par étape :",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PresetStarterButton(
                title = "Montante Prudente • 20 € → 100 €",
                subtitle = "90% réinvesti / 10% sécurisé • Cotes 1.20–1.40",
                onClick = { onPresetSelect("Montante Prudente 20→100€", 20.0, 100.0, 0.90) }
            )
            PresetStarterButton(
                title = "Montante Classique • 50 € → 250 €",
                subtitle = "100% réinvesti (All-In palier) • Cotes 1.25–1.45",
                onClick = { onPresetSelect("Montante Classique 50→250€", 50.0, 250.0, 1.0) }
            )
            OutlinedButton(
                onClick = onCustomClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
            ) {
                Text("Configurer une montante personnalisée")
            }
        }
    }
}

@Composable
private fun PresetStarterButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val semantic = LocalMontanteColors.current
    Surface(
        color = semantic.cardElevated,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, semantic.safeEmerald.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = semantic.safeEmerald)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActiveCampaignDashboardCard(
    campaign: MontanteCampaignEntity,
    steps: List<MontanteStepEntity>,
    onAddManualStep: () -> Unit,
    onDeleteCampaign: () -> Unit
) {
    val semantic = LocalMontanteColors.current
    val totalWealth = campaign.currentBankroll + campaign.securedProfit
    val progress = ((totalWealth - campaign.initialStake) /
        (campaign.targetBankroll - campaign.initialStake).coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

    // Reconstruct bankroll curve points from initialStake + resolved WON/LOST steps
    val curvePoints = remember(campaign, steps) {
        val pts = mutableListOf(campaign.initialStake)
        var running = campaign.initialStake
        var locked = 0.0
        steps.forEach { st ->
            when (st.outcome) {
                StepOutcome.WON -> {
                    val net = (st.stake * st.odds - st.stake).coerceAtLeast(0.0)
                    locked += net * (1.0 - campaign.reinvestRatio)
                    running += net * campaign.reinvestRatio
                    pts += (running + locked)
                }
                StepOutcome.LOST -> {
                    running = (running - st.stake).coerceAtLeast(0.0)
                    pts += (running + locked)
                }
            }
        }
        pts
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.large)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = campaign.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatusBadge(status = campaign.status)
                    }
                    Text(
                        text = "Réinvestissement ${(campaign.reinvestRatio * 100).roundToInt()}% • Cotes ${"%.2f".format(campaign.targetOddsMin)}–${"%.2f".format(campaign.targetOddsMax)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDeleteCampaign,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Supprimer la montante",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BankrollCurveChart(
                points = curvePoints,
                targetAmount = campaign.targetBankroll
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiTile(
                    label = "Bankroll Active",
                    value = "${"%.2f".format(campaign.currentBankroll)} €",
                    accent = semantic.safeEmerald,
                    modifier = Modifier.weight(1f)
                )
                KpiTile(
                    label = "Gains Sécurisés",
                    value = "${"%.2f".format(campaign.securedProfit)} €",
                    accent = semantic.telemetryCyan,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiTile(
                    label = "Palier Actuel",
                    value = "#${campaign.currentStepNumber}",
                    accent = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                KpiTile(
                    label = "Objectif Final",
                    value = "${"%.0f".format(campaign.targetBankroll)} €",
                    accent = semantic.boostAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progression vers l'objectif", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = semantic.safeEmerald)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = semantic.safeEmerald,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }

            if (campaign.status == CampaignStatus.ACTIVE) {
                Button(
                    onClick = onAddManualStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("add_manual_step_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enregistrer Palier #${campaign.currentStepNumber}")
                }
            }
        }
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val semantic = LocalMontanteColors.current
    val (label, bg, fg) = when (status) {
        CampaignStatus.COMPLETED -> Triple("OBJECTIF ATTEINT", semantic.safeContainer, semantic.safeEmerald)
        CampaignStatus.FAILED -> Triple("STOPPÉE", semantic.riskContainer, semantic.riskCrimson)
        else -> Triple("ACTIVE", semantic.telemetryContainer, semantic.telemetryCyan)
    }
    Surface(color = bg, shape = RoundedCornerShape(99.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MontanteStepCard(
    step: MontanteStepEntity,
    onResolve: (String) -> Unit
) {
    val semantic = LocalMontanteColors.current
    val borderColor = when (step.outcome) {
        StepOutcome.WON -> semantic.safeEmerald.copy(alpha = 0.5f)
        StepOutcome.LOST -> semantic.riskCrimson.copy(alpha = 0.5f)
        StepOutcome.REFUNDED -> semantic.telemetryCyan.copy(alpha = 0.5f)
        else -> semantic.boostAmber.copy(alpha = 0.65f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, MaterialTheme.shapes.medium)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PALIER #${step.stepNumber} • ${step.leagueName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val (badgeLabel, badgeColor) = when (step.outcome) {
                    StepOutcome.WON -> "GAGNÉ" to semantic.safeEmerald
                    StepOutcome.LOST -> "PERDU" to semantic.riskCrimson
                    StepOutcome.REFUNDED -> "REMBOURSÉ" to semantic.telemetryCyan
                    else -> "EN COURS" to semantic.boostAmber
                }
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            Text(step.matchTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${step.marketLabel} • Cote ${"%.2f".format(step.odds)} (Prob. ${"%.0f".format(step.statProbability * 100)}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Mise : ${"%.2f".format(step.stake)} € → Gain brut potentiel : ${"%.2f".format(step.potentialReturn)} €",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (step.outcome == StepOutcome.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onResolve(StepOutcome.WON) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = semantic.safeEmerald,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("step_won_button_${step.id}")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gagné")
                    }
                    OutlinedButton(
                        onClick = { onResolve(StepOutcome.REFUNDED) },
                        modifier = Modifier
                            .weight(0.9f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nul/Remb.")
                    }
                    OutlinedButton(
                        onClick = { onResolve(StepOutcome.LOST) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.riskCrimson),
                        modifier = Modifier
                            .weight(0.85f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Perdu")
                    }
                }
            }
        }
    }
}

@Composable
private fun LadderSimulatorCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    params: LadderSimulatorParams,
    steps: List<LadderSimulationStep>,
    onUpdate: ((LadderSimulatorParams) -> LadderSimulatorParams) -> Unit
) {
    val semantic = LocalMontanteColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.large)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .minimumInteractiveComponentSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Calculate, contentDescription = null, tint = semantic.boostAmber)
                    Column {
                        Text(stringResource(R.string.simulator_title), style = MaterialTheme.typography.titleLarge)
                        val last = steps.lastOrNull()
                        if (last != null) {
                            Text(
                                text = "${params.steps} paliers à ${"%.2f".format(params.averageOdds)} → ${"%.2f".format(last.cumulativeBankroll)} € (Survie ${"%.1f".format(last.survivalProbability * 100)}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.safeEmerald
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Déplier"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Mise initiale : ${"%.0f".format(params.initialStake)} €", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = params.initialStake.toFloat(),
                        onValueChange = { v -> onUpdate { it.copy(initialStake = v.roundToInt().toDouble()) } },
                        valueRange = 5f..200f
                    )

                    Text("Cote moyenne par palier : ${"%.2f".format(params.averageOdds)}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = params.averageOdds.toFloat(),
                        onValueChange = { v -> onUpdate { it.copy(averageOdds = (v * 100).roundToInt() / 100.0) } },
                        valueRange = 1.15f..1.85f
                    )

                    Text("Nombre de paliers : ${params.steps}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = params.steps.toFloat(),
                        onValueChange = { v -> onUpdate { it.copy(steps = v.roundToInt()) } },
                        valueRange = 3f..15f
                    )

                    Text("Probabilité moyenne par pari : ${(params.winProbability * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = params.winProbability.toFloat(),
                        onValueChange = { v -> onUpdate { it.copy(winProbability = (v * 100).roundToInt() / 100.0) } },
                        valueRange = 0.55f..0.90f
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        steps.forEach { st ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Palier ${st.stepNumber}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "Mise ${"%.1f".format(st.stake)}€ → ${"%.1f".format(st.cumulativeBankroll)}€",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = semantic.safeEmerald
                                )
                                Text(
                                    text = "${"%.0f".format(st.survivalProbability * 100)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCampaignDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Double, Double, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("Montante Sûre") }
    var initial by remember { mutableStateOf("20") }
    var target by remember { mutableStateOf("120") }
    var reinvestRatio by remember { mutableStateOf(0.90) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_montante)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la campagne") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initial,
                        onValueChange = { initial = it },
                        label = { Text("Capital (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Objectif (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Stratégie de réinvestissement des gains :", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1.0 to "100%", 0.90 to "90%", 0.80 to "80%").forEach { (ratio, label) ->
                        FilterChip(
                            selected = reinvestRatio == ratio,
                            onClick = { reinvestRatio = ratio },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initVal = initial.replace(',', '.').toDoubleOrNull() ?: 20.0
                    val targetVal = target.replace(',', '.').toDoubleOrNull() ?: (initVal * 5.0)
                    onCreate(name, initVal, targetVal, reinvestRatio, 1.20, 1.50)
                }
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun AddManualStepDialog(
    campaign: MontanteCampaignEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Double) -> Unit
) {
    var matchTitle by remember { mutableStateOf("") }
    var marketLabel by remember { mutableStateOf("Double Chance 1X") }
    var odds by remember { mutableStateOf("1.30") }
    var stake by remember { mutableStateOf("%.2f".format(Locale.US, campaign.currentBankroll)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter Palier #${campaign.currentStepNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = matchTitle,
                    onValueChange = { matchTitle = it },
                    label = { Text("Match (ex: PSG vs Lens)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = marketLabel,
                    onValueChange = { marketLabel = it },
                    label = { Text("Option / Marché") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = odds,
                        onValueChange = { odds = it },
                        label = { Text("Cote") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stake,
                        onValueChange = { stake = it },
                        label = { Text("Mise (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanMatch = matchTitle.trim().ifEmpty { "Pari Palier #${campaign.currentStepNumber}" }
                    val cleanMarket = marketLabel.trim().ifEmpty { "Sélection Sûre" }
                    val o = odds.replace(',', '.').toDoubleOrNull() ?: 1.30
                    val s = stake.replace(',', '.').toDoubleOrNull() ?: campaign.currentBankroll
                    val estProb = (1.0 / (o * 1.05)).coerceIn(0.30, 0.92)
                    onConfirm(cleanMatch, cleanMarket, o, estProb, s)
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
