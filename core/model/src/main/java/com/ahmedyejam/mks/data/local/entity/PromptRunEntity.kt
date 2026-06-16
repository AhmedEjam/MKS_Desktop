package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prompt_runs",
    foreignKeys = [
        ForeignKey(
            entity = PromptCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["promptCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("promptCardId"),
        Index("createdAt"),
        Index("deletedAt")
    ]
)
data class PromptRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val promptCardId: Long,
    val inputValuesJson: String,
    val renderedPrompt: String,
    val outputText: String? = null,
    val linkedAssetType: String? = null,
    val linkedAssetId: Long? = null,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
