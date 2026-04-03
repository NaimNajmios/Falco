package com.najmi.falco.data.local.dao

import androidx.room.*
import com.najmi.falco.data.local.entity.ClaimEntity
import kotlinx.coroutines.flow.Flow

data class ClaimWithVerdict(
    val id: String,
    val text: String,
    val type: String,
    val submittedAt: Long,
    val lean: String?,
    val confidence: Float?,
    val supportingCount: Int = 0,
    val opposingCount: Int = 0,
    val neutralCount: Int = 0,
    val isFavorite: Boolean = false
)

@Dao
interface ClaimDao {
    @Query("""
        SELECT c.id, c.text, c.type, c.submittedAt, v.lean, v.confidence,
               COALESCE(v.supportingCount, 0) as supportingCount,
               COALESCE(v.opposingCount, 0) as opposingCount,
               COALESCE(v.neutralCount, 0) as neutralCount,
               COALESCE(c.isFavorite, 0) as isFavorite
        FROM claims c
        LEFT JOIN verdicts v ON c.id = v.claimId
        ORDER BY c.submittedAt DESC
        LIMIT 10
    """)
    fun getRecentClaimsWithVerdicts(): Flow<List<ClaimWithVerdict>>

    @Query("SELECT * FROM claims ORDER BY submittedAt DESC")
    fun getAllClaims(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE id = :id")
    suspend fun getClaimById(id: String): ClaimEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(claim: ClaimEntity)

    @Delete
    suspend fun delete(claim: ClaimEntity)

    @Query("DELETE FROM claims WHERE submittedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM claims WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE claims SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)
}
