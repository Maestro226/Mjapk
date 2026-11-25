package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MontanteCampaignEntity
import com.example.domain.AnalyzedMatch
import com.example.domain.BettingOption
import com.example.ui.components.FormStreakRow
import com.example.ui.components.MontanteScoreBadge
import com.example.ui.components.ProbabilityMeterBar
import com.example.ui.components.RiskTierPill
import com.example.ui.components.TeamCrest
import com.example.ui.theme.LocalMontanteColors
import com.example.ui.viewmodel.DailyPicksUiState
import com.example.ui.viewmodel.PicksTierFilter
import com.example.ui.viewmodel.SportFilter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPicksScreen(
    uiState: DailyPicksUiState,
    tierFilter: PicksTierFilter,
    sportFilter: SportFilter,
    activeCampaign: MontanteCampaignEntity?,
    onTierFilterSelected: (PicksTierFilter) -> Unit,
    onSportFilterSelected: (SportFilter) -> Unit,
    onRefresh: () -> Unit,
    onEngageOption: (AnalyzedMatch, BettingOption, Double?, Double?) -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectedMatch by remember { mutableStateOf<AnalyzedMatch?>(null) }
    var engageTarget by remember { mutableStateOf<Pair<AnalyzedMatch, BettingOption>?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroAnalyticsBanner(
                activeCampaign = activeCampaign,
                onRefresh = onRefresh
            )
        }

        item {
            FilterBarSection(
                tierFilter = tierFilter,
                sportFilter = sportFilter,
                onTierFilterSelected = onTierFilterSelected,
                onSportFilterSelected = onSportFilterSelected
            )
        }

        when (uiState) {
            is DailyPicksUiState.Loading -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Calcul des probabilités Poisson & analyse des formes en cours...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is DailyPicksUiState.Error -> {
                item {
                    ErrorOrEmptyCard(
                        title = "Flux sportifs indisponibles",
                        message = uiState.message,
                        primaryLabel = "Réessayer",
                        onPrimaryClick = onRefresh,
                        secondaryLabel = "Ouvrir l'Analyseur",
                        onSecondaryClick = onNavigateToAnalyzer
                    )
                }
            }

            is DailyPicksUiState.Success -> {
                if (uiState.filteredMatches.isEmpty()) {
                    item {
                        val fallbackMsg = if (uiState.allMatches.isEmpty()) {
                            "Aucun match à venir trouvé sur les championnats suivis aujourd'hui. Utilisez l'Analyseur Poisson pour évaluer n'importe quelle rencontre."
                        } else {
                            "Aucun match ne correspond au filtre « ${tierFilter.label} ». Essayez « Palier Modéré » ou « Toutes Cotes »."
                        }
                        ErrorOrEmptyCard(
                            title = "Aucune option dans ce filtre",
                            message = fallbackMsg,
                            primaryLabel = if (uiState.allMatches.isNotEmpty()) "Voir toutes les cotes" else "Actualiser",
                            onPrimaryClick = {
                                if (uiState.allMatches.isNotEmpty()) onTierFilterSelected(PicksTierFilter.ALL)
                                else onRefresh()
                            },
                            secondaryLabel = "Analyseur manuel",
                            onSecondaryClick = onNavigateToAnalyzer
                        )
                    }
                } else {
                    val spotlight = uiState.filteredMatches.first()
                    item {
                        SpotlightPickCard(
                            match = spotlight,
                            onInspect = { inspectedMatch = spotlight },
                            onEngage = { engageTarget = spotlight to spotlight.recommendedOption }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Options du Jour (${uiState.filteredMatches.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Tri : Indice Montante",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(
                        items = uiState.filteredMatches.drop(1),
                        key = { it.eventId }
                    ) { match ->
                        DailyMatchCard(
                            match = match,
                            onInspect = { inspectedMatch = match },
                            onEngage = { engageTarget = match to match.recommendedOption }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    inspectedMatch?.let { match ->
        ModalBottomSheet(
            onDismissRequest = { inspectedMatch = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MatchStatisticalSheetContent(
                match = match,
                onSelectOption = { option ->
                    inspectedMatch = null
                    engageTarget = match to option
                }
            )
        }
    }

    engageTarget?.let { (match, option) ->
        EngageStepDialog(
            match = match,
            option = option,
            activeCampaign = activeCampaign,
            onDismiss = { engageTarget = null },
            onConfirm = { odds, stake ->
                onEngageOption(match, option, stake, odds)
                engageTarget = null
            }
        )
    }
}

@Composable
private fun HeroAnalyticsBanner(
    activeCampaign: MontanteCampaignEntity?,
    onRefresh: () -> Unit
) {
    val semantic = LocalMontanteColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.large)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_analytics_hero),
            contentDescription = "Bannière analytique Montante Stat",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xEE0A1016),
                            Color(0xCC0A1016),
                            Color(0x660A1016)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = semantic.safeContainer,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = semantic.safeEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "STRATÉGIE MONTANTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.safeEmerald
                        )
                    }
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("refresh_picks_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.refresh_picks),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Text(
                text = "Options Statistiques du Jour",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (activeCampaign != null) {
                    "${activeCampaign.name} • Bankroll ${"%.2f".format(activeCampaign.currentBankroll)} € (Palier #${activeCampaign.currentStepNumber})"
                } else {
                    stringResource(R.string.hero_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterBarSection(
    tierFilter: PicksTierFilter,
    sportFilter: SportFilter,
    onTierFilterSelected: (PicksTierFilter) -> Unit,
    onSportFilterSelected: (SportFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PicksTierFilter.entries.forEach { tier ->
                val selected = tier == tierFilter
                FilterChip(
                    selected = selected,
                    onClick = { onTierFilterSelected(tier) },
                    label = { Text(tier.label, style = MaterialTheme.typography.titleSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("tier_filter_${tier.name.lowercase()}")
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SportFilter.entries.forEach { sport ->
                FilterChip(
                    selected = sport == sportFilter,
                    onClick = { onSportFilterSelected(sport) },
                    label = { Text(sport.label, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.testTag("sport_filter_${sport.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun SpotlightPickCard(
    match: AnalyzedMatch,
    onInspect: () -> Unit,
    onEngage: () -> Unit
) {
    val semantic = LocalMontanteColors.current
    val opt = match.recommendedOption
    Card(
        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, semantic.safeEmerald.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .clickable(onClick = onInspect)
            .testTag("spotlight_pick_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = semantic.boostAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "MEILLEURE OPTION MONTANTE",
                        style = MaterialTheme.typography.labelMedium,
                        color = semantic.boostAmber
                    )
                }
                MontanteScoreBadge(score = opt.montanteScore)
            }

            Text(
                text = "${match.leagueName} • ${formatKickoff(match.kickoffIso, match.statusDetail)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    TeamCrest(match.homeTeam.shortName, match.homeTeam.logoUrl, size = 40.dp)
                    Column {
                        Text(
                            text = match.homeTeam.shortName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FormStreakRow(form = match.homeTeam.recentForm)
                    }
                }
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text(
                            text = match.awayTeam.shortName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FormStreakRow(form = match.awayTeam.recentForm)
                    }
                    TeamCrest(match.awayTeam.shortName, match.awayTeam.logoUrl, size = 40.dp)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opt.marketLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = opt.selectionLabel,
                                style = MaterialTheme.typography.titleLarge,
                                color = semantic.safeEmerald
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Cote réf. ${"%.2f".format(opt.referenceOdds)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = semantic.boostAmber
                            )
                            Text(
                                text = "Prob. ${"%.1f".format(opt.statisticalProbability * 100)}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    ProbabilityMeterBar(
                        probability = opt.statisticalProbability,
                        riskTier = opt.riskTier
                    )

                    Text(
                        text = opt.rationale,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Détails Stats")
                }
                Button(
                    onClick = onEngage,
                    modifier = Modifier
                        .weight(1.2f)
                        .minimumInteractiveComponentSize()
                        .testTag("engage_spotlight_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Engager")
                }
            }
        }
    }
}

@Composable
private fun DailyMatchCard(
    match: AnalyzedMatch,
    onInspect: () -> Unit,
    onEngage: () -> Unit
) {
    val semantic = LocalMontanteColors.current
    val opt = match.recommendedOption
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.medium)
            .clickable(onClick = onInspect)
            .testTag("match_card_${match.eventId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.leagueName} • ${formatKickoff(match.kickoffIso, match.statusDetail)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                RiskTierPill(tier = opt.riskTier)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    TeamCrest(match.homeTeam.shortName, match.homeTeam.logoUrl, size = 34.dp)
                    Column {
                        Text(
                            text = "${match.homeTeam.shortName} vs ${match.awayTeam.shortName}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Bilans : ${match.homeTeam.recordSummary} | ${match.awayTeam.recordSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                MontanteScoreBadge(score = opt.montanteScore)
            }

            ProbabilityMeterBar(
                probability = opt.statisticalProbability,
                riskTier = opt.riskTier
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = opt.selectionLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = semantic.safeEmerald
                    )
                    Text(
                        text = "Prob. ${"%.1f".format(opt.statisticalProbability * 100)}% • Cote ~${"%.2f".format(opt.referenceOdds)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onEngage,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Engager")
                }
            }
        }
    }
}

@Composable
private fun MatchStatisticalSheetContent(
    match: AnalyzedMatch,
    onSelectOption: (BettingOption) -> Unit
) {
    val semantic = LocalMontanteColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "${match.homeTeam.name} vs ${match.awayTeam.name}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "${match.leagueName} • Modèle Statistique ${if (match.over15Prob != null) "Poisson" else "Logistique"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChipBox(
                label = match.homeTeam.shortName,
                value = "${"%.1f".format(match.homeWinProb * 100)}%",
                sub = "Moy. ${"%.2f".format(match.expectedHomeScore)} buts",
                modifier = Modifier.weight(1f)
            )
            if (match.drawProb > 0.01) {
                StatChipBox(
                    label = "Nul (X)",
                    value = "${"%.1f".format(match.drawProb * 100)}%",
                    sub = "Équilibre",
                    modifier = Modifier.weight(1f)
                )
            }
            StatChipBox(
                label = match.awayTeam.shortName,
                value = "${"%.1f".format(match.awayWinProb * 100)}%",
                sub = "Moy. ${"%.2f".format(match.expectedAwayScore)} buts",
                modifier = Modifier.weight(1f)
            )
        }

        if (match.topExactScores.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scores probables :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                match.topExactScores.forEach { sc ->
                    Surface(
                        color = semantic.cardElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${sc.homeGoals}-${sc.awayGoals} (${"%.0f".format(sc.probability * 100)}%)",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = "Marchés classés par Indice Montante",
            style = MaterialTheme.typography.titleMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            match.allOptions.take(5).forEach { option ->
                Surface(
                    color = semantic.cardElevated,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, semantic.borderSubtle, MaterialTheme.shapes.medium)
                        .clickable { onSelectOption(option) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.selectionLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${option.marketLabel} • Prob. ${"%.1f".format(option.statisticalProbability * 100)}% • Cote ~${"%.2f".format(option.referenceOdds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        MontanteScoreBadge(score = option.montanteScore)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChipBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    val semantic = LocalMontanteColors.current
    Surface(
        color = semantic.cardElevated,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleLarge, color = semantic.safeEmerald)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EngageStepDialog(
    match: AnalyzedMatch,
    option: BettingOption,
    activeCampaign: MontanteCampaignEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var oddsText by remember { mutableStateOf("%.2f".format(Locale.US, option.referenceOdds)) }
    val defaultStake = activeCampaign?.currentBankroll ?: 20.0
    var stakeText by remember { mutableStateOf("%.2f".format(Locale.US, defaultStake)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Engager en Montante") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${match.homeTeam.shortName} vs ${match.awayTeam.shortName} — ${option.selectionLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Probabilité statistique estimée : ${"%.1f".format(option.statisticalProbability * 100)}%",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = oddsText,
                    onValueChange = { oddsText = it },
                    label = { Text("Cote proposée") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stakeText,
                    onValueChange = { stakeText = it },
                    label = { Text("Mise (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odds = oddsText.replace(',', '.').toDoubleOrNull() ?: option.referenceOdds
                    val stake = stakeText.replace(',', '.').toDoubleOrNull() ?: defaultStake
                    onConfirm(odds.coerceAtLeast(1.01), stake.coerceAtLeast(0.5))
                }
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Valider le palier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun ErrorOrEmptyCard(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String,
    onSecondaryClick: () -> Unit
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
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSecondaryClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(secondaryLabel)
                }
                Button(
                    onClick = onPrimaryClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(primaryLabel)
                }
            }
        }
    }
}

private fun formatKickoff(iso: String, fallback: String): String {
    val localizedFallback = when {
        fallback.contains("Halftime", ignoreCase = true) -> "Mi-temps"
        fallback.contains("Final", ignoreCase = true) -> "Terminé"
        fallback.contains("Postponed", ignoreCase = true) -> "Reporté"
        fallback.contains("Delayed", ignoreCase = true) -> "Retardé"
        fallback.contains("Scheduled", ignoreCase = true) -> "À venir"
        else -> fallback.ifEmpty { "À venir" }
    }
    val formats = listOf("yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
    for (pattern in formats) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(iso)
            if (date != null) {
                val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).apply {
                    timeZone = TimeZone.getDefault()
                }
                return formatter.format(date)
            }
        } catch (_: Exception) {
            // Try next format
        }
    }
    return localizedFallback
}
