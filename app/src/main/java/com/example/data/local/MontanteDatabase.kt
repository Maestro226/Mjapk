package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MontanteCampaignEntity::class, MontanteStepEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MontanteDatabase : RoomDatabase() {
    abstract fun montanteDao(): MontanteDao

    companion object {
        @Volatile
        private var INSTANCE: MontanteDatabase? = null

        fun getInstance(context: Context): MontanteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MontanteDatabase::class.java,
                    "montante_stat_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
