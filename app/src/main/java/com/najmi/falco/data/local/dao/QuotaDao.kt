package com.najmi.falco.data.local.dao

import androidx.room.*
import com.najmi.falco.data.local.entity.QuotaEntity

@Dao
interface QuotaDao {
    @Query("SELECT * FROM provider_quota WHERE provider = :provider AND lastResetDate = :today")
    suspend fun getQuota(provider: String, today: String): QuotaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuota(quota: QuotaEntity)

    @Query("UPDATE provider_quota SET tokensUsedToday = tokensUsedToday + :tokens WHERE provider = :provider")
    suspend fun incrementTokens(provider: String, tokens: Int)

    @Query("UPDATE provider_quota SET requestsUsedToday = requestsUsedToday + 1 WHERE provider = :provider")
    suspend fun incrementRequests(provider: String)

    @Query("SELECT * FROM provider_quota")
    suspend fun getAllQuotas(): List<QuotaEntity>
}
