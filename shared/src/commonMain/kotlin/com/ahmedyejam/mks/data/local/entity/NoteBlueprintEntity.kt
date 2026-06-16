package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

enum class BlueprintMode {
    SIMPLE_NOTE, OUTLINE, CHECKLIST, ALGORITHM,
    DISEASE_TEMPLATE, DRUG_TEMPLATE, CONCEPT_TEMPLATE,
    MISTAKE_REVIEW, CUSTOM
}

enum class BlueprintReviewStatus {
    NEW, REVIEWING, REVIEWED, NEEDS_UPDATE
}

@Serializable
data class NoteBlueprintEntity(
    val id: Long = 0,
    val externalId: String,
    val collectionId: Long,
    val title: String,
    val description: String? = null,
    val summary: String? = null,
    val iconName: String? = null,
    val coverImage: String? = null,
    val body: String,
    val bulletPoints: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val blueprintMode: String = "SIMPLE_NOTE",
    val linkedQuestionsJson: String = "[]",
    val linkedAssetsJson: String = "[]",
    val reviewStatus: String = "NEW",
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val sourceQuestionId: Long? = null,
    val deletedAt: Long? = null
)
