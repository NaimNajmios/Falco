package com.najmi.falco.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "paper_stances",
    foreignKeys = [
        ForeignKey(
            entity = VerdictEntity::class,
            parentColumns = ["id"],
            childColumns = ["verdictId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("verdictId")]
)
data class PaperStanceEntity(
    @PrimaryKey val id: String,
    val verdictId: String,
    val paperTitle: String,
    val paperAbstract: String,
    val paperYear: Int?,
    val paperCitationCount: Int,
    val paperUrl: String?,
    val finalStance: String,
    val actorReasoning: String,
    val criticChallenge: String?,
    val groundingScore: Float?,
    val confidence: Float
)
