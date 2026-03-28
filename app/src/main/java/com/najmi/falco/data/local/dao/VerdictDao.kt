package com.najmi.falco.data.local.dao

import androidx.room.*
import com.najmi.falco.data.local.entity.VerdictEntity
import com.najmi.falco.data.local.entity.PaperStanceEntity
import kotlinx.coroutines.flow.Flow

data class VerdictWithStances(
    @Embedded val verdict: VerdictEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "verdictId"
    )
    val stances: List<PaperStanceEntity>
)

@Dao
interface VerdictDao {
    @Transaction
    @Query("SELECT * FROM verdicts ORDER BY completedAt DESC")
    fun getAllVerdicts(): Flow<List<VerdictWithStances>>

    @Query("SELECT * FROM verdicts WHERE claimId = :claimId LIMIT 1")
    suspend fun getVerdictForClaim(claimId: String): VerdictEntity?

    @Transaction
    @Query("SELECT * FROM verdicts WHERE claimId = :claimId LIMIT 1")
    suspend fun getVerdictWithStancesForClaim(claimId: String): VerdictWithStances?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(verdict: VerdictEntity)

    @Query("DELETE FROM verdicts WHERE completedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
