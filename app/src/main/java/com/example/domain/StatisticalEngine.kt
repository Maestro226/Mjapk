package com.example.domain

import com.example.data.remote.EspnCompetitor
import com.example.data.remote.EspnEvent
import com.example.data.remote.EspnOdds
import com.example.data.remote.LeagueFeedConfig
import com.example.data.remote.SportCategory
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class RiskTier(val label: String) {
    SAFE_MONTANTE("Montante Sûre"),
    BALANCED("Palier Modéré"),
    VALUE_HIGH("Risque Élevé")
}

data class TeamStatsSummary(
    val name: String,
    val shortName: String,
    val logoUrl: String?,
    val recordSummary: String,
    val homeOrAwayRecord: String,
    val recentForm: String,
    val winRate: Double,
    val formScore: Double
)

data class BettingOption(
    val id: String,
    val marketLabel: String,
    val selectionLabel: String,
    val statisticalProbability: Double, // 0.0 .. 1.0
    val fairOdds: Double,
    val referenceOdds: Double,
    val expectedValuePercent: Double,
    val montanteScore: Int, // 0 .. 100
    val riskTier: RiskTier,
    val rationale: String
)

data class ExactScoreProbability(
    val homeGoals: Int,
    val awayGoals: Int,
    val probability: Double
)

data class AnalyzedMatch(
    val eventId: String,
    val leagueName: String,
    val sportCategory: SportCategory,
    val kickoffIso: String,
    val statusState: String,
    val statusDetail: String,
    val homeTeam: TeamStatsSummary,
    val awayTeam: TeamStatsSummary,
    val expectedHomeScore: Double,
    val expectedAwayScore: Double,
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val over15Prob: Double?,
    val under35Prob: Double?,
    val bttsProb: Double?,
    val topExactScores: List<ExactScoreProbability>,
    val recommendedOption: BettingOption,
    val allOptions: List<BettingOption>
)

data class CustomMatchInput(
    val homeName: String,
    val awayName: String,
    val homePlayed: Int,
    val homeWins: Int,
    val homeDraws: Int,
    val homeGoalsFor: Int,
    val homeGoalsAgainst: Int,
    val homeForm: String,
    val awayPlayed: Int,
    val awayWins: Int,
    val awayDraws: Int,
    val awayGoalsFor: Int,
    val awayGoalsAgainst: Int,
    val awayForm: String,
    val bookmakerOdds: Double,
    val selectedMarketKey: String
)

data class CustomAnalysisResult(
    val lambdaHome: Double,
    val lambdaAway: Double,
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val doubleChance1XProb: Double,
    val doubleChanceX2Prob: Double,
    val dnbHomeProb: Double,
    val dnbAwayProb: Double,
    val over15Prob: Double,
    val under35Prob: Double,
    val bttsProb: Double,
    val selectedMarketProb: Double,
    val fairOdds: Double,
    val expectedValuePercent: Double,
    val quarterKellyPercent: Double,
    val montanteScore: Int,
    val riskTier: RiskTier,
    val topScores: List<ExactScoreProbability>
)

data class LadderSimulationStep(
    val stepNumber: Int,
    val stake: Double,
    val grossReturn: Double,
    val lockedProfit: Double,
    val cumulativeBankroll: Double,
    val survivalProbability: Double
)

object StatisticalEngine {

    private data class ParsedRecord(val wins: Int, val draws: Int, val losses: Int) {
        val total: Int get() = wins + draws + losses
        val winRate: Double get() = if (total > 0) wins.toDouble() / total else 0.40
        val drawRate: Double get() = if (total > 0) draws.toDouble() / total else 0.25
        val pointsPerGame: Double get() = if (total > 0) (wins * 3.0 + draws) / total else 1.35
    }

    fun analyzeEvent(event: EspnEvent, league: LeagueFeedConfig): AnalyzedMatch? {
        val competition = event.competitions?.firstOrNull() ?: return null
        val competitors = competition.competitors ?: return null
        val home = competitors.firstOrNull { it.homeAway.equals("home", ignoreCase = true) } ?: competitors.getOrNull(0) ?: return null
        val away = competitors.firstOrNull { it.homeAway.equals("away", ignoreCase = true) } ?: competitors.getOrNull(1) ?: return null

        val homeName = home.team?.displayName ?: home.team?.shortDisplayName ?: "Domicile"
        val awayName = away.team?.displayName ?: away.team?.shortDisplayName ?: "Extérieur"
        val homeShort = home.team?.shortDisplayName ?: home.team?.abbreviation ?: homeName.take(3).uppercase()
        val awayShort = away.team?.shortDisplayName ?: away.team?.abbreviation ?: awayName.take(3).uppercase()

        val homeTotalRec = parseRecord(findRecordSummary(home, "total"))
        val awayTotalRec = parseRecord(findRecordSummary(away, "total"))
        val homeSplitRec = parseRecord(findRecordSummary(home, "home"))
        val awaySplitRec = parseRecord(findRecordSummary(away, "away"))

        val homeFormStr = sanitizeForm(home.form)
        val awayFormStr = sanitizeForm(away.form)
        val homeFormScore = computeFormMomentum(homeFormStr, homeTotalRec.winRate)
        val awayFormScore = computeFormMomentum(awayFormStr, awayTotalRec.winRate)

        val homeSummary = TeamStatsSummary(
            name = homeName,
            shortName = homeShort,
            logoUrl = home.team?.logo,
            recordSummary = findRecordSummary(home, "total") ?: "N/D",
            homeOrAwayRecord = findRecordSummary(home, "home") ?: "N/D",
            recentForm = homeFormStr,
            winRate = homeTotalRec.winRate,
            formScore = homeFormScore
        )
        val awaySummary = TeamStatsSummary(
            name = awayName,
            shortName = awayShort,
            logoUrl = away.team?.logo,
            recordSummary = findRecordSummary(away, "total") ?: "N/D",
            homeOrAwayRecord = findRecordSummary(away, "away") ?: "N/D",
            recentForm = awayFormStr,
            winRate = awayTotalRec.winRate,
            formScore = awayFormScore
        )

        val statusState = event.status?.type?.state ?: competition.status?.type?.state ?: "pre"
        val statusDetail = event.status?.type?.shortDetail ?: competition.status?.type?.shortDetail ?: ""
        val oddsInfo = competition.odds?.firstOrNull()

        return when (league.sportCategory) {
            SportCategory.FOOTBALL -> analyzeFootballMatch(
                event = event,
                league = league,
                homeSummary = homeSummary,
                awaySummary = awaySummary,
                homeTotal = homeTotalRec,
                awayTotal = awayTotalRec,
                homeSplit = homeSplitRec,
                awaySplit = awaySplitRec,
                oddsInfo = oddsInfo,
                statusState = statusState,
                statusDetail = statusDetail
            )
            SportCategory.BASKETBALL, SportCategory.HOCKEY -> analyzeTwoWaySportMatch(
                event = event,
                league = league,
                homeSummary = homeSummary,
                awaySummary = awaySummary,
                homeTotal = homeTotalRec,
                awayTotal = awayTotalRec,
                homeSplit = homeSplitRec,
                awaySplit = awaySplitRec,
                oddsInfo = oddsInfo,
                statusState = statusState,
                statusDetail = statusDetail
            )
        }
    }

    private fun analyzeFootballMatch(
        event: EspnEvent,
        league: LeagueFeedConfig,
        homeSummary: TeamStatsSummary,
        awaySummary: TeamStatsSummary,
        homeTotal: ParsedRecord,
        awayTotal: ParsedRecord,
        homeSplit: ParsedRecord,
        awaySplit: ParsedRecord,
        oddsInfo: EspnOdds?,
        statusState: String,
        statusDetail: String
    ): AnalyzedMatch {
        // Blend season points-per-game, home/away split, and 5-match momentum
        val homeBaseStrength = (homeTotal.pointsPerGame / 3.0) * 0.50 +
            (if (homeSplit.total >= 3) (homeSplit.pointsPerGame / 3.0) * 0.25 else (homeTotal.pointsPerGame / 3.0) * 0.25) +
            homeSummary.formScore * 0.25

        val awayBaseStrength = (awayTotal.pointsPerGame / 3.0) * 0.50 +
            (if (awaySplit.total >= 3) (awaySplit.pointsPerGame / 3.0) * 0.25 else (awayTotal.pointsPerGame / 3.0) * 0.25) +
            awaySummary.formScore * 0.25

        // Incorporate market implied probabilities if ESPN odds are available
        val marketProbs = extractMarketImpliedSoccerProbs(oddsInfo)

        val strengthDiff = (homeBaseStrength - awayBaseStrength).coerceIn(-0.65, 0.65)
        var lambdaHome = (1.42 + strengthDiff * 0.95 + (homeBaseStrength - 0.48) * 0.45).coerceIn(0.65, 2.85)
        var lambdaAway = (1.10 - strengthDiff * 0.85 + (awayBaseStrength - 0.45) * 0.40).coerceIn(0.45, 2.50)

        if (marketProbs != null) {
            val marketTotalBias = (marketProbs.first - marketProbs.third) * 0.65
            lambdaHome = (lambdaHome * 0.72 + (1.38 + marketTotalBias) * 0.28).coerceIn(0.60, 2.95)
            lambdaAway = (lambdaAway * 0.72 + (1.12 - marketTotalBias) * 0.28).coerceIn(0.45, 2.60)
        }

        val matrix = computePoissonMatrix(lambdaHome, lambdaAway)
        val p1 = matrix.homeWin
        val px = matrix.draw
        val p2 = matrix.awayWin
        val p1x = (p1 + px).coerceIn(0.05, 0.96)
        val px2 = (p2 + px).coerceIn(0.05, 0.96)
        val dnb1 = (p1 / max(0.01, p1 + p2)).coerceIn(0.05, 0.95)
        val dnb2 = (p2 / max(0.01, p1 + p2)).coerceIn(0.05, 0.95)
        val over15 = matrix.over15
        val under35 = matrix.under35
        val btts = matrix.btts

        val candidates = mutableListOf<BettingOption>()

        candidates += buildOption(
            id = "${event.id}_1x",
            marketLabel = "Double Chance",
            selectionLabel = "${homeSummary.shortName} ou Nul (1X)",
            prob = p1x,
            bookOdds = oddsInfo?.let { estimateDerivedOdds(p1x) },
            isLowVarianceMarket = true,
            formAlignment = homeSummary.formScore,
            rationale = "Couvre victoire domicile (${pct(p1)}) et nul (${pct(px)}) — idéal pour sécuriser un palier."
        )
        candidates += buildOption(
            id = "${event.id}_x2",
            marketLabel = "Double Chance",
            selectionLabel = "Nul ou ${awaySummary.shortName} (X2)",
            prob = px2,
            bookOdds = oddsInfo?.let { estimateDerivedOdds(px2) },
            isLowVarianceMarket = true,
            formAlignment = awaySummary.formScore,
            rationale = "Protège contre le match nul (${pct(px)}) avec une solidité extérieure de ${pct(awaySummary.formScore)}."
        )
        candidates += buildOption(
            id = "${event.id}_o15",
            marketLabel = "Total Buts",
            selectionLabel = "Plus de 1,5 buts",
            prob = over15,
            bookOdds = oddsInfo?.let { estimateDerivedOdds(over15) },
            isLowVarianceMarket = true,
            formAlignment = (homeSummary.formScore + awaySummary.formScore) / 2.0,
            rationale = "Total combiné estimé à ${format2(lambdaHome + lambdaAway)} buts selon le modèle de Poisson."
        )
        candidates += buildOption(
            id = "${event.id}_u35",
            marketLabel = "Total Buts",
            selectionLabel = "Moins de 3,5 buts",
            prob = under35,
            bookOdds = oddsInfo?.let { estimateDerivedOdds(under35) },
            isLowVarianceMarket = true,
            formAlignment = 0.60,
            rationale = "Probabilité Poisson de ${pct(under35)} que le match reste sous 4 buts."
        )
        if (dnb1 >= dnb2) {
            candidates += buildOption(
                id = "${event.id}_dnb1",
                marketLabel = "Remboursé si Nul",
                selectionLabel = "${homeSummary.shortName} (Remboursé si Nul)",
                prob = dnb1,
                bookOdds = oddsInfo?.let { estimateDerivedOdds(dnb1) },
                isLowVarianceMarket = true,
                formAlignment = homeSummary.formScore,
                rationale = "En cas de match nul (${pct(px)}), votre mise montante est intégralement remboursée."
            )
        } else {
            candidates += buildOption(
                id = "${event.id}_dnb2",
                marketLabel = "Remboursé si Nul",
                selectionLabel = "${awaySummary.shortName} (Remboursé si Nul)",
                prob = dnb2,
                bookOdds = oddsInfo?.let { estimateDerivedOdds(dnb2) },
                isLowVarianceMarket = true,
                formAlignment = awaySummary.formScore,
                rationale = "En cas de match nul (${pct(px)}), votre mise montante est intégralement remboursée."
            )
        }
        candidates += buildOption(
            id = "${event.id}_1",
            marketLabel = "Résultat 1X2",
            selectionLabel = "Victoire ${homeSummary.shortName}",
            prob = p1,
            bookOdds = oddsInfo?.homeTeamOdds?.moneyLine?.let { americanToDecimal(it) },
            isLowVarianceMarket = false,
            formAlignment = homeSummary.formScore,
            rationale = "Victoire sèche à domicile avec avantage terrain et indice de forme ${pct(homeSummary.formScore)}."
        )
        candidates += buildOption(
            id = "${event.id}_2",
            marketLabel = "Résultat 1X2",
            selectionLabel = "Victoire ${awaySummary.shortName}",
            prob = p2,
            bookOdds = oddsInfo?.awayTeamOdds?.moneyLine?.let { americanToDecimal(it) },
            isLowVarianceMarket = false,
            formAlignment = awaySummary.formScore,
            rationale = "Victoire sèche à l'extérieur selon l'écart de rendement (${homeTotal.pointsPerGame.let { format2(it) }} vs ${awayTotal.pointsPerGame.let { format2(it) }} pts/m)."
        )

        val sorted = candidates.sortedByDescending { it.montanteScore }
        return AnalyzedMatch(
            eventId = event.id,
            leagueName = league.displayName,
            sportCategory = league.sportCategory,
            kickoffIso = event.date,
            statusState = statusState,
            statusDetail = statusDetail,
            homeTeam = homeSummary,
            awayTeam = awaySummary,
            expectedHomeScore = lambdaHome,
            expectedAwayScore = lambdaAway,
            homeWinProb = p1,
            drawProb = px,
            awayWinProb = p2,
            over15Prob = over15,
            under35Prob = under35,
            bttsProb = btts,
            topExactScores = matrix.topScores,
            recommendedOption = sorted.first(),
            allOptions = sorted
        )
    }

    private fun analyzeTwoWaySportMatch(
        event: EspnEvent,
        league: LeagueFeedConfig,
        homeSummary: TeamStatsSummary,
        awaySummary: TeamStatsSummary,
        homeTotal: ParsedRecord,
        awayTotal: ParsedRecord,
        homeSplit: ParsedRecord,
        awaySplit: ParsedRecord,
        oddsInfo: EspnOdds?,
        statusState: String,
        statusDetail: String
    ): AnalyzedMatch {
        val homePct = (homeTotal.winRate * 0.55 +
            (if (homeSplit.total >= 3) homeSplit.winRate else homeTotal.winRate) * 0.25 +
            homeSummary.formScore * 0.20)
        val awayPct = (awayTotal.winRate * 0.55 +
            (if (awaySplit.total >= 3) awaySplit.winRate else awayTotal.winRate) * 0.25 +
            awaySummary.formScore * 0.20)

        val homeAdvantage = if (league.sportCategory == SportCategory.BASKETBALL) 0.16 else 0.12
        val logit = (homePct - awayPct) * 3.4 + homeAdvantage
        var pHome = 1.0 / (1.0 + exp(-logit))

        val marketHome = oddsInfo?.homeTeamOdds?.moneyLine?.let { americanToDecimal(it) }
        val marketAway = oddsInfo?.awayTeamOdds?.moneyLine?.let { americanToDecimal(it) }
        if (marketHome != null && marketAway != null) {
            val rawH = 1.0 / marketHome
            val rawA = 1.0 / marketAway
            val impliedHome = rawH / (rawH + rawA)
            pHome = (pHome * 0.65 + impliedHome * 0.35).coerceIn(0.12, 0.92)
        }
        val pAway = (1.0 - pHome).coerceIn(0.08, 0.88)

        val options = mutableListOf<BettingOption>()
        options += buildOption(
            id = "${event.id}_ml_home",
            marketLabel = "Vainqueur (Prol. incl.)",
            selectionLabel = "Victoire ${homeSummary.shortName}",
            prob = pHome,
            bookOdds = marketHome,
            isLowVarianceMarket = pHome >= 0.68,
            formAlignment = homeSummary.formScore,
            rationale = "Bilan ${homeSummary.recordSummary} à domicile (${homeSummary.homeOrAwayRecord}) face à ${awaySummary.recordSummary}."
        )
        options += buildOption(
            id = "${event.id}_ml_away",
            marketLabel = "Vainqueur (Prol. incl.)",
            selectionLabel = "Victoire ${awaySummary.shortName}",
            prob = pAway,
            bookOdds = marketAway,
            isLowVarianceMarket = pAway >= 0.68,
            formAlignment = awaySummary.formScore,
            rationale = "Performance extérieure ${awaySummary.homeOrAwayRecord} avec dynamique ${pct(awaySummary.formScore)}."
        )
        // Safe alternative: Handicap +4.5 / +1.5 for Basketball/Hockey
        val handicapLabel = if (league.sportCategory == SportCategory.BASKETBALL) "+5,5 pts" else "+1,5 buts"
        val pSafeHome = (pHome + (1.0 - pHome) * 0.38).coerceIn(0.55, 0.91)
        val pSafeAway = (pAway + (1.0 - pAway) * 0.38).coerceIn(0.55, 0.91)
        if (pSafeHome >= pSafeAway) {
            options += buildOption(
                id = "${event.id}_hcap_home",
                marketLabel = "Handicap Sécurisé",
                selectionLabel = "${homeSummary.shortName} $handicapLabel",
                prob = pSafeHome,
                bookOdds = null,
                isLowVarianceMarket = true,
                formAlignment = homeSummary.formScore,
                rationale = "Marge de sécurité $handicapLabel augmentant la probabilité de survie en montante."
            )
        } else {
            options += buildOption(
                id = "${event.id}_hcap_away",
                marketLabel = "Handicap Sécurisé",
                selectionLabel = "${awaySummary.shortName} $handicapLabel",
                prob = pSafeAway,
                bookOdds = null,
                isLowVarianceMarket = true,
                formAlignment = awaySummary.formScore,
                rationale = "Marge de sécurité $handicapLabel augmentant la probabilité de survie en montante."
            )
        }

        val sorted = options.sortedByDescending { it.montanteScore }
        return AnalyzedMatch(
            eventId = event.id,
            leagueName = league.displayName,
            sportCategory = league.sportCategory,
            kickoffIso = event.date,
            statusState = statusState,
            statusDetail = statusDetail,
            homeTeam = homeSummary,
            awayTeam = awaySummary,
            expectedHomeScore = if (league.sportCategory == SportCategory.BASKETBALL) 114.0 + (pHome - 0.5) * 14 else 3.2 + (pHome - 0.5) * 1.4,
            expectedAwayScore = if (league.sportCategory == SportCategory.BASKETBALL) 110.0 + (pAway - 0.5) * 14 else 2.8 + (pAway - 0.5) * 1.4,
            homeWinProb = pHome,
            drawProb = 0.0,
            awayWinProb = pAway,
            over15Prob = null,
            under35Prob = null,
            bttsProb = null,
            topExactScores = emptyList(),
            recommendedOption = sorted.first(),
            allOptions = sorted
        )
    }

    fun analyzeCustomMatch(input: CustomMatchInput): CustomAnalysisResult {
        val hPlayed = max(1, input.homePlayed)
        val aPlayed = max(1, input.awayPlayed)

        val hAttack = input.homeGoalsFor.toDouble() / hPlayed
        val hDefense = input.homeGoalsAgainst.toDouble() / hPlayed
        val aAttack = input.awayGoalsFor.toDouble() / aPlayed
        val aDefense = input.awayGoalsAgainst.toDouble() / aPlayed

        val hPointsRatio = ((input.homeWins * 3.0 + input.homeDraws) / (hPlayed * 3.0)).coerceIn(0.15, 0.95)
        val aPointsRatio = ((input.awayWins * 3.0 + input.awayDraws) / (aPlayed * 3.0)).coerceIn(0.15, 0.95)

        val hForm = computeFormMomentum(sanitizeForm(input.homeForm), hPointsRatio)
        val aForm = computeFormMomentum(sanitizeForm(input.awayForm), aPointsRatio)

        // Poisson expected goals blending goals attack/defense + home advantage + recent form
        val leagueAvg = 1.32
        val rawLambdaHome = ((hAttack / leagueAvg) * (aDefense / leagueAvg) * leagueAvg * 1.12) * (0.85 + hForm * 0.30)
        val rawLambdaAway = ((aAttack / leagueAvg) * (hDefense / leagueAvg) * leagueAvg * 0.92) * (0.85 + aForm * 0.30)

        val lambdaHome = rawLambdaHome.coerceIn(0.45, 3.30)
        val lambdaAway = rawLambdaAway.coerceIn(0.35, 3.00)

        val matrix = computePoissonMatrix(lambdaHome, lambdaAway)
        val p1 = matrix.homeWin
        val px = matrix.draw
        val p2 = matrix.awayWin
        val p1x = (p1 + px).coerceIn(0.05, 0.97)
        val px2 = (p2 + px).coerceIn(0.05, 0.97)
        val dnb1 = (p1 / max(0.01, p1 + p2)).coerceIn(0.05, 0.96)
        val dnb2 = (p2 / max(0.01, p1 + p2)).coerceIn(0.05, 0.96)

        val selectedProb = when (input.selectedMarketKey) {
            "1X" -> p1x
            "X2" -> px2
            "O15" -> matrix.over15
            "U35" -> matrix.under35
            "DNB1" -> dnb1
            "DNB2" -> dnb2
            "1" -> p1
            "2" -> p2
            "BTTS" -> matrix.btts
            else -> p1x
        }

        val fairOdds = 1.0 / max(0.02, selectedProb)
        val safeBookOdds = max(1.01, input.bookmakerOdds)
        val evPercent = ((selectedProb * safeBookOdds) - 1.0) * 100.0
        val fullKelly = (((safeBookOdds - 1.0) * selectedProb - (1.0 - selectedProb)) / (safeBookOdds - 1.0)).coerceAtLeast(0.0)
        val quarterKellyPercent = (fullKelly * 0.25 * 100.0).coerceIn(0.0, 25.0)

        val option = buildOption(
            id = "custom",
            marketLabel = input.selectedMarketKey,
            selectionLabel = input.selectedMarketKey,
            prob = selectedProb,
            bookOdds = safeBookOdds,
            isLowVarianceMarket = input.selectedMarketKey in setOf("1X", "X2", "O15", "U35", "DNB1", "DNB2"),
            formAlignment = if (input.selectedMarketKey in setOf("X2", "DNB2", "2")) aForm else hForm,
            rationale = ""
        )

        return CustomAnalysisResult(
            lambdaHome = lambdaHome,
            lambdaAway = lambdaAway,
            homeWinProb = p1,
            drawProb = px,
            awayWinProb = p2,
            doubleChance1XProb = p1x,
            doubleChanceX2Prob = px2,
            dnbHomeProb = dnb1,
            dnbAwayProb = dnb2,
            over15Prob = matrix.over15,
            under35Prob = matrix.under35,
            bttsProb = matrix.btts,
            selectedMarketProb = selectedProb,
            fairOdds = fairOdds,
            expectedValuePercent = evPercent,
            quarterKellyPercent = quarterKellyPercent,
            montanteScore = option.montanteScore,
            riskTier = option.riskTier,
            topScores = matrix.topScores
        )
    }

    fun simulateLadder(
        initialStake: Double,
        averageOdds: Double,
        steps: Int,
        winProbabilityPerStep: Double,
        reinvestRatio: Double
    ): List<LadderSimulationStep> {
        val result = mutableListOf<LadderSimulationStep>()
        var currentStake = initialStake.coerceAtLeast(1.0)
        var lockedTotal = 0.0
        var survival = 1.0

        for (step in 1..steps.coerceIn(1, 20)) {
            val gross = currentStake * averageOdds
            val netProfit = (gross - currentStake).coerceAtLeast(0.0)
            val lockedThisStep = netProfit * (1.0 - reinvestRatio)
            lockedTotal += lockedThisStep
            val nextStake = currentStake + netProfit * reinvestRatio
            survival *= winProbabilityPerStep.coerceIn(0.10, 0.99)

            result += LadderSimulationStep(
                stepNumber = step,
                stake = currentStake,
                grossReturn = gross,
                lockedProfit = lockedTotal,
                cumulativeBankroll = nextStake + lockedTotal,
                survivalProbability = survival
            )
            currentStake = nextStake
        }
        return result
    }

    private data class PoissonMatrixResult(
        val homeWin: Double,
        val draw: Double,
        val awayWin: Double,
        val over15: Double,
        val under35: Double,
        val btts: Double,
        val topScores: List<ExactScoreProbability>
    )

    private fun computePoissonMatrix(lambdaHome: Double, lambdaAway: Double): PoissonMatrixResult {
        var homeWin = 0.0
        var draw = 0.0
        var awayWin = 0.0
        var underOrEqual1 = 0.0
        var underOrEqual3 = 0.0
        var zeroHome = 0.0
        var zeroAway = 0.0
        val scores = mutableListOf<ExactScoreProbability>()

        for (h in 0..6) {
            val ph = poissonPmf(lambdaHome, h)
            for (a in 0..6) {
                val pa = poissonPmf(lambdaAway, a)
                val p = ph * pa
                when {
                    h > a -> homeWin += p
                    h == a -> draw += p
                    else -> awayWin += p
                }
                if (h + a <= 1) underOrEqual1 += p
                if (h + a <= 3) underOrEqual3 += p
                if (h == 0) zeroHome += p
                if (a == 0) zeroAway += p
                scores += ExactScoreProbability(h, a, p)
            }
        }

        val totalSum = max(0.001, homeWin + draw + awayWin)
        val p1 = homeWin / totalSum
        val px = draw / totalSum
        val p2 = awayWin / totalSum
        val over15 = (1.0 - underOrEqual1 / totalSum).coerceIn(0.05, 0.97)
        val under35 = (underOrEqual3 / totalSum).coerceIn(0.05, 0.97)
        val p00 = (poissonPmf(lambdaHome, 0) * poissonPmf(lambdaAway, 0)) / totalSum
        val btts = (1.0 - zeroHome / totalSum - zeroAway / totalSum + p00).coerceIn(0.05, 0.95)

        return PoissonMatrixResult(
            homeWin = p1,
            draw = px,
            awayWin = p2,
            over15 = over15,
            under35 = under35,
            btts = btts,
            topScores = scores.sortedByDescending { it.probability }.take(3)
        )
    }

    private fun poissonPmf(lambda: Double, k: Int): Double {
        var factorial = 1.0
        for (i in 2..k) factorial *= i.toDouble()
        return exp(-lambda) * lambda.pow(k.toDouble()) / factorial
    }

    private fun buildOption(
        id: String,
        marketLabel: String,
        selectionLabel: String,
        prob: Double,
        bookOdds: Double?,
        isLowVarianceMarket: Boolean,
        formAlignment: Double,
        rationale: String
    ): BettingOption {
        val fairOdds = (1.0 / max(0.02, prob))
        val refOdds = (bookOdds ?: estimateDerivedOdds(prob)).coerceAtLeast(1.05)
        val ev = ((prob * refOdds) - 1.0) * 100.0

        // Montante Score prioritizes survival probability (>72%) + low variance + ideal odds band (1.18..1.52)
        val probPoints = (prob * 75.0).coerceIn(0.0, 75.0)
        val varianceBonus = if (isLowVarianceMarket) 12.0 else 3.0
        val oddsSweetSpotBonus = when {
            refOdds in 1.18..1.52 -> 8.0
            refOdds in 1.52..1.80 -> 4.0
            refOdds < 1.18 -> 3.0
            else -> -4.0
        }
        val formBonus = (formAlignment * 6.0).coerceIn(0.0, 6.0)
        val score = (probPoints + varianceBonus + oddsSweetSpotBonus + formBonus).toInt().coerceIn(15, 98)

        val tier = when {
            prob >= 0.72 && refOdds <= 1.58 -> RiskTier.SAFE_MONTANTE
            prob >= 0.60 && refOdds <= 1.95 -> RiskTier.BALANCED
            else -> RiskTier.VALUE_HIGH
        }

        return BettingOption(
            id = id,
            marketLabel = marketLabel,
            selectionLabel = selectionLabel,
            statisticalProbability = prob,
            fairOdds = fairOdds,
            referenceOdds = refOdds,
            expectedValuePercent = ev,
            montanteScore = score,
            riskTier = tier,
            rationale = rationale
        )
    }

    private fun estimateDerivedOdds(prob: Double): Double {
        // Typical 5.5% bookmaker margin applied to statistical fair odds
        return (1.0 / (max(0.03, prob) * 1.055)).coerceIn(1.06, 15.0)
    }

    private fun extractMarketImpliedSoccerProbs(odds: EspnOdds?): Triple<Double, Double, Double>? {
        val hMl = odds?.homeTeamOdds?.moneyLine ?: return null
        val aMl = odds.awayTeamOdds?.moneyLine ?: return null
        val dMl = odds.drawOdds?.moneyLine ?: return null
        val rh = 1.0 / americanToDecimal(hMl)
        val rd = 1.0 / americanToDecimal(dMl)
        val ra = 1.0 / americanToDecimal(aMl)
        val sum = rh + rd + ra
        if (sum <= 0.0) return null
        return Triple(rh / sum, rd / sum, ra / sum)
    }

    private fun americanToDecimal(moneyLine: Double): Double {
        return if (moneyLine > 0) {
            1.0 + (moneyLine / 100.0)
        } else {
            1.0 + (100.0 / kotlin.math.abs(moneyLine))
        }.coerceIn(1.02, 50.0)
    }

    private fun findRecordSummary(competitor: EspnCompetitor, type: String): String? {
        return competitor.records
            ?.firstOrNull { it.type.equals(type, ignoreCase = true) || it.name.equals(type, ignoreCase = true) }
            ?.summary
    }

    private fun parseRecord(summary: String?): ParsedRecord {
        if (summary.isNullOrBlank()) return ParsedRecord(0, 0, 0)
        val parts = summary.split("-").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            3 -> ParsedRecord(parts[0], parts[1], parts[2])
            2 -> ParsedRecord(parts[0], 0, parts[1])
            else -> ParsedRecord(0, 0, 0)
        }
    }

    private fun sanitizeForm(raw: String?): String {
        if (raw.isNullOrBlank()) return "-----"
        return raw.uppercase().filter { it in charArrayOf('W', 'D', 'L') }.takeLast(5).ifEmpty { "-----" }
    }

    private fun computeFormMomentum(form: String, fallbackWinRate: Double): Double {
        val valid = form.filter { it in charArrayOf('W', 'D', 'L') }
        if (valid.isEmpty()) return fallbackWinRate.coerceIn(0.25, 0.85)
        var weightedSum = 0.0
        var totalWeight = 0.0
        valid.forEachIndexed { idx, c ->
            val weight = (idx + 1).toDouble() // More recent matches carry higher weight
            val pts = when (c) {
                'W' -> 1.0
                'D' -> 0.42
                else -> 0.0
            }
            weightedSum += pts * weight
            totalWeight += weight
        }
        return (weightedSum / max(1.0, totalWeight)).coerceIn(0.10, 0.95)
    }

    private fun pct(value: Double): String = "${"%.1f".format(value * 100.0)}%"
    private fun format2(value: Double): String = "%.2f".format(value)
}
