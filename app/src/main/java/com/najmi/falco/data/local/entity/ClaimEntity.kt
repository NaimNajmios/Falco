package com.najmi.falco.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "claims")
data class ClaimEntity(
    @PrimaryKey val id: String,
    val text: String,
    val type: String,
    val submittedAt: Long,
    val isFavorite: Boolean = false
)
