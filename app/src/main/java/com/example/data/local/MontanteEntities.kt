package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object CampaignStatus {
    const val ACTIVE = "ACTIVE"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
}

object StepOutcome {
    const val PENDING = "PENDING"
    const val WON = "WON"
    const val LOST = "LOST"
    const val REFUNDED = "REFUNDED"
}

@Entity(tableName = "montante_campaigns")
data class MontanteCampaignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val initialStake: Double,
    val targetBankroll: Double,
    val currentBankroll: Double,
    val securedProfit: Double = 0.0,
    val reinvestRatio: Double = 1.0, // 1.0 = 100%, 0.9 = 90%, 0.8 = 80%
    val targetOddsMin: Double = 1.20,
    val targetOddsMax: Double = 1.50,
    val currentStepNumber: Int = 1,
    val status: String = CampaignStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "montante_steps",
    foreignKeys = [
        ForeignKey(
            entity = MontanteCampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["campaignId"])]
)
data class MontanteStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val campaignId: Long,
    val stepNumber: Int,
    val matchTitle: String,
    val leagueName: String,
    val marketLabel: String,
    val odds: Double,
    val statProbability: Double,
    val stake: Double,
    val potentialReturn: Double,
    val outcome: String = StepOutcome.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
