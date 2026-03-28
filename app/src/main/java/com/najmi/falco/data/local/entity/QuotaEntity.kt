package com.najmi.falco.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_quota")
data class QuotaEntity(
    @PrimaryKey val provider: String,
    val tokensUsedToday: Int,
    val requestsUsedToday: Int,
    val lastResetDate: String
)
