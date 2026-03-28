package com.najmi.falco.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "verdicts",
    foreignKeys = [
        ForeignKey(
            entity = ClaimEntity::class,
            parentColumns = ["id"],
            childColumns = ["claimId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("claimId")]
)
data class VerdictEntity(
    @PrimaryKey val id: String,
    val claimId: String,
    val lean: String,
    val confidence: Float,
    val summary: String,
    val totalPapersRetrieved: Int,
    val totalPapersPassedGate: Int,
    val temporalWarning: String?,
    val completedAt: Long
)
