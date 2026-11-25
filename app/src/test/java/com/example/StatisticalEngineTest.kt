package com.example

import com.example.domain.CustomMatchInput
import com.example.domain.RiskTier
import com.example.domain.StatisticalEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticalEngineTest {

    @Test
    fun strongHomeFavoriteProducesSafeMontante1X() {
        val result = StatisticalEngine.analyzeCustomMatch(
            CustomMatchInput(
                homeName = "PSG",
                awayName = "Metz",
                homePlayed = 15,
                homeWins = 12,
                homeDraws = 2,
                homeGoalsFor = 36,
                homeGoalsAgainst = 10,
                homeForm = "WWWWW",
                awayPlayed = 15,
                awayWins = 3,
                awayDraws = 3,
                awayGoalsFor = 12,
                awayGoalsAgainst = 28,
                awayForm = "LLLWL",
                bookmakerOdds = 1.25,
                selectedMarketKey = "1X"
            )
        )

        assertTrue(result.doubleChance1XProb > 0.80)
        assertTrue(result.lambdaHome > result.lambdaAway)
        assertEquals(RiskTier.SAFE_MONTANTE, result.riskTier)
        assertTrue(result.montanteScore >= 75)
    }

    @Test
    fun ladderSimulationCompoundsWithLockedProfit() {
        val steps = StatisticalEngine.simulateLadder(
            initialStake = 20.0,
            averageOdds = 1.30,
            steps = 5,
            winProbabilityPerStep = 0.80,
            reinvestRatio = 0.90
        )

        assertEquals(5, steps.size)
        assertTrue(steps.last().cumulativeBankroll > 60.0)
        assertTrue(steps.last().lockedProfit > 0.0)
        assertEquals(0.80 * 0.80 * 0.80 * 0.80 * 0.80, steps.last().survivalProbability, 0.001)
    }
}
