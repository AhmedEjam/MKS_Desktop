package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceSettingsEntity(
    val id: Long = 0,
    val workspaceId: Long,
    val language: String? = null,
    val theme: String? = null,
    val defaultSort: String? = null,
    val quizDefaultsJson: String? = null,
    val importDefaultsJson: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
