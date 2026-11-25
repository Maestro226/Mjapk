package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CampaignStatus
import com.example.data.local.MontanteCampaignEntity
import com.example.data.local.MontanteStepEntity
import com.example.data.remote.SportCategory
import com.example.data.repository.MontanteRepository
import com.example.domain.AnalyzedMatch
import com.example.domain.BettingOption
import com.example.domain.CustomAnalysisResult
import com.example.domain.CustomMatchInput
import com.example.domain.LadderSimulationStep
import com.example.domain.RiskTier
import com.example.domain.StatisticalEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PicksTierFilter(val label: String) {
    SAFE_MONTANTE("Montante Sûre"),
    BALANCED("Palier Modéré"),
    ALL("Toutes Cotes")
}

enum class SportFilter(val label: String) {
    ALL("Tous Sports"),
    FOOTBALL("Football"),
    BASKETBALL("NBA"),
    HOCKEY("NHL")
}

sealed interface DailyPicksUiState {
    data object Loading : DailyPicksUiState
    data class Success(
        val allMatches: List<AnalyzedMatch>,
        val filteredMatches: List<AnalyzedMatch>,
        val lastUpdatedMillis: Long
    ) : DailyPicksUiState
    data class Error(val message: String) : DailyPicksUiState
}

data class LadderSimulatorParams(
    val initialStake: Double = 20.0,
    val averageOdds: Double = 1.32,
    val steps: Int = 8,
    val winProbability: Double = 0.78,
    val reinvestRatio: Double = 1.0
)

class MontanteViewModel(
    private val repository: MontanteRepository
) : ViewModel() {

    private val _rawMatches = MutableStateFlow<List<AnalyzedMatch>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _lastUpdated = MutableStateFlow(0L)

    private val _tierFilter = MutableStateFlow(PicksTierFilter.SAFE_MONTANTE)
    val tierFilter: StateFlow<PicksTierFilter> = _tierFilter.asStateFlow()

    private val _sportFilter = MutableStateFlow(SportFilter.ALL)
    val sportFilter: StateFlow<SportFilter> = _sportFilter.asStateFlow()

    val dailyPicksState: StateFlow<DailyPicksUiState> = combine(
        _rawMatches,
        _isLoading,
        _errorMessage,
        _tierFilter,
        _sportFilter
    ) { matches, loading, error, tier, sport ->
        when {
            loading && matches.isEmpty() -> DailyPicksUiState.Loading
            error != null && matches.isEmpty() -> DailyPicksUiState.Error(error)
            else -> {
                val filtered = matches.filter { match ->
                    val sportMatches = when (sport) {
                        SportFilter.ALL -> true
                        SportFilter.FOOTBALL -> match.sportCategory == SportCategory.FOOTBALL
                        SportFilter.BASKETBALL -> match.sportCategory == SportCategory.BASKETBALL
                        SportFilter.HOCKEY -> match.sportCategory == SportCategory.HOCKEY
                    }
                    val tierMatches = when (tier) {
                        PicksTierFilter.SAFE_MONTANTE -> match.recommendedOption.riskTier == RiskTier.SAFE_MONTANTE
                        PicksTierFilter.BALANCED -> match.recommendedOption.riskTier == RiskTier.SAFE_MONTANTE ||
                            match.recommendedOption.riskTier == RiskTier.BALANCED
                        PicksTierFilter.ALL -> true
                    }
                    sportMatches && tierMatches
                }
                DailyPicksUiState.Success(
                    allMatches = matches,
                    filteredMatches = filtered,
                    lastUpdatedMillis = _lastUpdated.value
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DailyPicksUiState.Loading
    )

    val campaigns: StateFlow<List<MontanteCampaignEntity>> = repository.allCampaigns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val stepsByCampaign: StateFlow<Map<Long, List<MontanteStepEntity>>> = repository.allSteps
        .map { steps -> steps.groupBy { it.campaignId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    private val _selectedCampaignId = MutableStateFlow<Long?>(null)
    val activeCampaign: StateFlow<MontanteCampaignEntity?> = combine(
        campaigns,
        _selectedCampaignId
    ) { list, selectedId ->
        list.firstOrNull { it.id == selectedId }
            ?: list.firstOrNull { it.status == CampaignStatus.ACTIVE }
            ?: list.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    private val _customInput = MutableStateFlow(
        CustomMatchInput(
            homeName = "Équipe Domicile",
            awayName = "Équipe Extérieur",
            homePlayed = 12,
            homeWins = 8,
            homeDraws = 3,
            homeGoalsFor = 24,
            homeGoalsAgainst = 9,
            homeForm = "WWDWW",
            awayPlayed = 12,
            awayWins = 4,
            awayDraws = 3,
            awayGoalsFor = 14,
            awayGoalsAgainst = 18,
            awayForm = "LDLWD",
            bookmakerOdds = 1.30,
            selectedMarketKey = "1X"
        )
    )
    val customInput: StateFlow<CustomMatchInput> = _customInput.asStateFlow()

    val customResult: StateFlow<CustomAnalysisResult> = _customInput
        .map { StatisticalEngine.analyzeCustomMatch(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StatisticalEngine.analyzeCustomMatch(_customInput.value)
        )

    private val _simulatorParams = MutableStateFlow(LadderSimulatorParams())
    val simulatorParams: StateFlow<LadderSimulatorParams> = _simulatorParams.asStateFlow()

    val simulatedSteps: StateFlow<List<LadderSimulationStep>> = _simulatorParams
        .map { p ->
            StatisticalEngine.simulateLadder(
                initialStake = p.initialStake,
                averageOdds = p.averageOdds,
                steps = p.steps,
                winProbabilityPerStep = p.winProbability,
                reinvestRatio = p.reinvestRatio
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        refreshDailyMatches()
    }

    fun refreshDailyMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.fetchDailyAnalyzedMatches(includeTomorrow = true)
                .onSuccess { result ->
                    _rawMatches.value = result.matches
                    _lastUpdated.value = result.fetchedAtMillis
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Erreur réseau lors du chargement des matchs."
                    _isLoading.value = false
                }
        }
    }

    fun setTierFilter(filter: PicksTierFilter) {
        _tierFilter.value = filter
    }

    fun setSportFilter(filter: SportFilter) {
        _sportFilter.value = filter
    }

    fun selectCampaign(campaignId: Long) {
        _selectedCampaignId.value = campaignId
    }

    fun createCampaign(
        name: String,
        initialStake: Double,
        targetBankroll: Double,
        reinvestRatio: Double,
        targetOddsMin: Double,
        targetOddsMax: Double
    ) {
        viewModelScope.launch {
            val id = repository.createCampaign(
                name = name,
                initialStake = initialStake,
                targetBankroll = targetBankroll,
                reinvestRatio = reinvestRatio,
                targetOddsMin = targetOddsMin,
                targetOddsMax = targetOddsMax
            )
            _selectedCampaignId.value = id
            _events.tryEmit("Montante créée avec succès")
        }
    }

    fun deleteCampaign(campaignId: Long) {
        viewModelScope.launch {
            repository.deleteCampaign(campaignId)
            _events.tryEmit("Montante supprimée")
        }
    }

    fun engageOptionInMontante(
        match: AnalyzedMatch,
        option: BettingOption,
        customStake: Double? = null,
        customOdds: Double? = null
    ) {
        viewModelScope.launch {
            var targetCampaign = activeCampaign.value
            if (targetCampaign == null || targetCampaign.status != CampaignStatus.ACTIVE) {
                val createdId = repository.createCampaign(
                    name = "Montante Sûre 20€ → 120€",
                    initialStake = 20.0,
                    targetBankroll = 120.0,
                    reinvestRatio = 0.90,
                    targetOddsMin = 1.20,
                    targetOddsMax = 1.50
                )
                _selectedCampaignId.value = createdId
                targetCampaign = MontanteCampaignEntity(
                    id = createdId,
                    name = "Montante Sûre 20€ → 120€",
                    initialStake = 20.0,
                    targetBankroll = 120.0,
                    currentBankroll = 20.0
                )
            }
            val stake = customStake ?: targetCampaign.currentBankroll
            val odds = customOdds ?: option.referenceOdds
            repository.addStepToCampaign(
                campaignId = targetCampaign.id,
                matchTitle = "${match.homeTeam.shortName} vs ${match.awayTeam.shortName}",
                leagueName = match.leagueName,
                marketLabel = option.selectionLabel,
                odds = odds,
                statProbability = option.statisticalProbability,
                stake = stake
            ).onSuccess {
                _events.tryEmit("Palier #${targetCampaign.currentStepNumber} engagé (${"%.2f".format(stake)} €)")
            }.onFailure { err ->
                _events.tryEmit(err.message ?: "Impossible d'ajouter le palier")
            }
        }
    }

    fun engageCustomPickInMontante(
        matchTitle: String,
        marketLabel: String,
        odds: Double,
        probability: Double,
        stake: Double? = null
    ) {
        viewModelScope.launch {
            var targetCampaign = activeCampaign.value
            if (targetCampaign == null || targetCampaign.status != CampaignStatus.ACTIVE) {
                val createdId = repository.createCampaign(
                    name = "Montante Analyse 20€ → 120€",
                    initialStake = 20.0,
                    targetBankroll = 120.0,
                    reinvestRatio = 0.90,
                    targetOddsMin = 1.20,
                    targetOddsMax = 1.50
                )
                _selectedCampaignId.value = createdId
                targetCampaign = MontanteCampaignEntity(
                    id = createdId,
                    name = "Montante Analyse 20€ → 120€",
                    initialStake = 20.0,
                    targetBankroll = 120.0,
                    currentBankroll = 20.0
                )
            }
            val usedStake = stake ?: targetCampaign.currentBankroll
            repository.addStepToCampaign(
                campaignId = targetCampaign.id,
                matchTitle = matchTitle,
                leagueName = "Analyse Statistique",
                marketLabel = marketLabel,
                odds = odds,
                statProbability = probability,
                stake = usedStake
            ).onSuccess {
                _events.tryEmit("Palier ajouté à ${targetCampaign.name}")
            }.onFailure { err ->
                _events.tryEmit(err.message ?: "Impossible d'ajouter le palier")
            }
        }
    }

    fun addManualStepToCampaign(
        campaignId: Long,
        matchTitle: String,
        marketLabel: String,
        odds: Double,
        probability: Double,
        stake: Double
    ) {
        viewModelScope.launch {
            repository.addStepToCampaign(
                campaignId = campaignId,
                matchTitle = matchTitle,
                leagueName = "Saisie manuelle",
                marketLabel = marketLabel,
                odds = odds,
                statProbability = probability,
                stake = stake
            ).onSuccess {
                _events.tryEmit("Palier enregistré (${"%.2f".format(stake)} €)")
            }.onFailure { err ->
                _events.tryEmit(err.message ?: "Impossible d'enregistrer le palier")
            }
        }
    }

    fun resolveStep(stepId: Long, outcome: String) {
        viewModelScope.launch {
            repository.resolveStep(stepId, outcome)
        }
    }

    fun updateCustomInput(transform: (CustomMatchInput) -> CustomMatchInput) {
        _customInput.update(transform)
    }

    fun updateSimulatorParams(transform: (LadderSimulatorParams) -> LadderSimulatorParams) {
        _simulatorParams.update(transform)
    }

    class Factory(private val repository: MontanteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MontanteViewModel(repository) as T
        }
    }
}
