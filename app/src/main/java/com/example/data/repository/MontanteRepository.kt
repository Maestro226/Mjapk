package com.example.data.repository

import com.example.data.local.CampaignStatus
import com.example.data.local.MontanteCampaignEntity
import com.example.data.local.MontanteDao
import com.example.data.local.MontanteStepEntity
import com.example.data.local.StepOutcome
import com.example.data.remote.EspnSportsApi
import com.example.data.remote.SportsApiClient
import com.example.domain.AnalyzedMatch
import com.example.domain.StatisticalEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class DailyFixturesResult(
    val matches: List<AnalyzedMatch>,
    val successfulFeeds: Int,
    val totalFeeds: Int,
    val fetchedAtMillis: Long
)

class MontanteRepository(
    private val dao: MontanteDao,
    private val api: EspnSportsApi = SportsApiClient.api
) {
    val allCampaigns: Flow<List<MontanteCampaignEntity>> = dao.observeAllCampaigns()
    val allSteps: Flow<List<MontanteStepEntity>> = dao.observeAllSteps()

    suspend fun fetchDailyAnalyzedMatches(includeTomorrow: Boolean = true): Result<DailyFixturesResult> {
        return withContext(Dispatchers.IO) {
            try {
                val dateFmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val todayStr = dateFmt.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowStr = dateFmt.format(cal.time)

                val dateStrings = if (includeTomorrow) listOf(todayStr, tomorrowStr) else listOf(todayStr)

                val analyzed = mutableListOf<AnalyzedMatch>()
                var successCount = 0
                val totalCalls = SportsApiClient.supportedLeagues.size * dateStrings.size

                coroutineScope {
                    val jobs = SportsApiClient.supportedLeagues.flatMap { league ->
                        dateStrings.map { dateQuery ->
                            async<Pair<Boolean, List<AnalyzedMatch>>> {
                                try {
                                    val resp = api.getScoreboard(
                                        sport = league.sport,
                                        league = league.leagueSlug,
                                        dates = dateQuery
                                    )
                                    val events = resp.events.orEmpty()
                                    val mapped = events.mapNotNull { event ->
                                        StatisticalEngine.analyzeEvent(event, league)
                                    }
                                    true to mapped
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    false to emptyList()
                                }
                            }
                        }
                    }

                    val results = jobs.awaitAll()
                    results.forEach { (ok, list) ->
                        if (ok) successCount++
                        analyzed += list
                    }
                }

                if (successCount == 0) {
                    return@withContext Result.failure(
                        IllegalStateException("Impossible de joindre les flux sportifs. Vérifiez votre connexion Internet.")
                    )
                }

                // Deduplicate by eventId and prioritize upcoming/live matches, sorted by Montante Score
                val uniqueMatches = analyzed
                    .distinctBy { it.eventId }
                    .filter { it.statusState != "post" }
                    .sortedWith(
                        compareByDescending<AnalyzedMatch> { it.recommendedOption.montanteScore }
                            .thenBy { it.kickoffIso }
                    )

                Result.success(
                    DailyFixturesResult(
                        matches = uniqueMatches,
                        successfulFeeds = successCount,
                        totalFeeds = totalCalls,
                        fetchedAtMillis = System.currentTimeMillis()
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createCampaign(
        name: String,
        initialStake: Double,
        targetBankroll: Double,
        reinvestRatio: Double,
        targetOddsMin: Double,
        targetOddsMax: Double
    ): Long {
        val campaign = MontanteCampaignEntity(
            name = name.trim().ifEmpty { "Montante ${"%.0f".format(initialStake)}€" },
            initialStake = initialStake.coerceAtLeast(1.0),
            targetBankroll = targetBankroll.coerceAtLeast(initialStake * 1.5),
            currentBankroll = initialStake.coerceAtLeast(1.0),
            securedProfit = 0.0,
            reinvestRatio = reinvestRatio.coerceIn(0.5, 1.0),
            targetOddsMin = targetOddsMin.coerceIn(1.05, 5.0),
            targetOddsMax = targetOddsMax.coerceIn(targetOddsMin, 10.0),
            currentStepNumber = 1,
            status = CampaignStatus.ACTIVE
        )
        return dao.insertCampaign(campaign)
    }

    suspend fun deleteCampaign(campaignId: Long) {
        dao.deleteCampaign(campaignId)
    }

    suspend fun addStepToCampaign(
        campaignId: Long,
        matchTitle: String,
        leagueName: String,
        marketLabel: String,
        odds: Double,
        statProbability: Double,
        stake: Double
    ): Result<Long> {
        val campaign = dao.getCampaignById(campaignId)
            ?: return Result.failure(IllegalStateException("Campagne introuvable"))
        if (campaign.status != CampaignStatus.ACTIVE) {
            return Result.failure(IllegalStateException("Cette montante n'est plus active"))
        }
        val cleanOdds = odds.coerceAtLeast(1.01)
        val cleanStake = stake.coerceIn(0.5, campaign.currentBankroll.coerceAtLeast(0.5))
        val step = MontanteStepEntity(
            campaignId = campaignId,
            stepNumber = campaign.currentStepNumber,
            matchTitle = matchTitle.trim(),
            leagueName = leagueName.trim(),
            marketLabel = marketLabel.trim(),
            odds = cleanOdds,
            statProbability = statProbability.coerceIn(0.01, 0.99),
            stake = cleanStake,
            potentialReturn = cleanStake * cleanOdds,
            outcome = StepOutcome.PENDING
        )
        val id = dao.insertStep(step)
        return Result.success(id)
    }

    suspend fun resolveStep(stepId: Long, newOutcome: String) {
        val step = dao.getStepById(stepId) ?: return
        if (step.outcome != StepOutcome.PENDING) return
        val campaign = dao.getCampaignById(step.campaignId) ?: return

        dao.updateStep(step.copy(outcome = newOutcome))

        when (newOutcome) {
            StepOutcome.WON -> {
                val grossReturn = step.stake * step.odds
                val netProfit = (grossReturn - step.stake).coerceAtLeast(0.0)
                val lockedProfit = netProfit * (1.0 - campaign.reinvestRatio)
                val reinvestedProfit = netProfit * campaign.reinvestRatio
                val newBankroll = (campaign.currentBankroll + reinvestedProfit)
                val newSecured = campaign.securedProfit + lockedProfit
                val totalReached = newBankroll + newSecured >= campaign.targetBankroll

                dao.updateCampaign(
                    campaign.copy(
                        currentBankroll = newBankroll,
                        securedProfit = newSecured,
                        currentStepNumber = campaign.currentStepNumber + 1,
                        status = if (totalReached) CampaignStatus.COMPLETED else CampaignStatus.ACTIVE
                    )
                )
            }
            StepOutcome.LOST -> {
                val remaining = (campaign.currentBankroll - step.stake).coerceAtLeast(0.0)
                val newStatus = if (remaining < 1.0) CampaignStatus.FAILED else CampaignStatus.ACTIVE
                dao.updateCampaign(
                    campaign.copy(
                        currentBankroll = remaining,
                        status = newStatus
                    )
                )
            }
            StepOutcome.REFUNDED -> {
                // Bankroll and step number remain untouched on a void/DNB refund
            }
        }
    }
}
