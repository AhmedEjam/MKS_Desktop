package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "question_categories",
    primaryKeys = ["questionId", "category"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("questionId"),
        Index("category"),
        Index("deletedAt")
    ]
)
data class QuestionCategoryEntity(
    val questionId: Long,
    val category: String,
    val deletedAt: Long? = null
)
