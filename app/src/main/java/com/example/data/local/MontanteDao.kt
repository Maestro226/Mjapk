package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MontanteDao {
    @Query("SELECT * FROM montante_campaigns ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, createdAt DESC")
    fun observeAllCampaigns(): Flow<List<MontanteCampaignEntity>>

    @Query("SELECT * FROM montante_campaigns WHERE id = :campaignId")
    suspend fun getCampaignById(campaignId: Long): MontanteCampaignEntity?

    @Query("SELECT * FROM montante_steps ORDER BY createdAt ASC")
    fun observeAllSteps(): Flow<List<MontanteStepEntity>>

    @Query("SELECT * FROM montante_steps WHERE campaignId = :campaignId ORDER BY stepNumber ASC, createdAt ASC")
    fun observeStepsForCampaign(campaignId: Long): Flow<List<MontanteStepEntity>>

    @Query("SELECT * FROM montante_steps WHERE id = :stepId")
    suspend fun getStepById(stepId: Long): MontanteStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: MontanteCampaignEntity): Long

    @Update
    suspend fun updateCampaign(campaign: MontanteCampaignEntity)

    @Query("DELETE FROM montante_campaigns WHERE id = :campaignId")
    suspend fun deleteCampaign(campaignId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: MontanteStepEntity): Long

    @Update
    suspend fun updateStep(step: MontanteStepEntity)
}
