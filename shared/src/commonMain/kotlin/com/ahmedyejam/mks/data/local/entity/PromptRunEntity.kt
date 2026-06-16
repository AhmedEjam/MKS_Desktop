package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class PromptRunEntity(
    val id: Long = 0,
    val promptCardId: Long,
    val inputValuesJson: String,
    val renderedPrompt: String,
    val outputText: String? = null,
    val linkedAssetType: String? = null,
    val linkedAssetId: Long? = null,
    val createdAt: Long = 0L,
    val deletedAt: Long? = null
)
