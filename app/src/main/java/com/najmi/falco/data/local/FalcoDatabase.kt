package com.najmi.falco.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.najmi.falco.data.local.dao.ClaimDao
import com.najmi.falco.data.local.dao.PaperStanceDao
import com.najmi.falco.data.local.dao.QuotaDao
import com.najmi.falco.data.local.dao.VerdictDao
import com.najmi.falco.data.local.entity.ClaimEntity
import com.najmi.falco.data.local.entity.PaperStanceEntity
import com.najmi.falco.data.local.entity.QuotaEntity
import com.najmi.falco.data.local.entity.VerdictEntity

@Database(
    entities = [
        ClaimEntity::class,
        VerdictEntity::class,
        PaperStanceEntity::class,
        QuotaEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(FalcoTypeConverters::class)
abstract class FalcoDatabase : RoomDatabase() {
    abstract fun claimDao(): ClaimDao
    abstract fun verdictDao(): VerdictDao
    abstract fun paperStanceDao(): PaperStanceDao
    abstract fun quotaDao(): QuotaDao

    companion object {
        const val DATABASE_NAME = "falco.db"
    }
}
