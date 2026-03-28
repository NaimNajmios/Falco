package com.najmi.falco.data.local.dao

import androidx.room.*
import com.najmi.falco.data.local.entity.ClaimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaimDao {
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
}
