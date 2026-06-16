package com.ahmedyejam.mks.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ahmedyejam.mks.data.local.WorkspaceDefaults
import com.ahmedyejam.mks.data.local.entity.WorkspaceEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceSettingsEntity
import com.ahmedyejam.mks.db.MksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WorkspaceRepository(
    private val db: MksDatabase,
    private val seeder: MksDatabaseSeeder
) {

    fun observeAll(): Flow<List<WorkspaceEntity>> =
        db.workspaceQueriesQueries.ws_selectAll()
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toWorkspaceEntity() } }

    suspend fun getById(id: Long): WorkspaceEntity? = withContext(Dispatchers.IO) {
        db.workspaceQueriesQueries.ws_selectById(id).executeAsOneOrNull()?.toWorkspaceEntity()
    }

    suspend fun getDefault(): WorkspaceEntity? = withContext(Dispatchers.IO) {
        db.workspaceQueriesQueries.ws_selectDefault().executeAsOneOrNull()?.toWorkspaceEntity()
    }

    suspend fun getOrCreateDefault(): WorkspaceEntity = withContext(Dispatchers.IO) {
        getDefault() ?: createDefault()
    }

    suspend fun create(name: String, description: String?, isDefault: Boolean): WorkspaceEntity =
        withContext(Dispatchers.IO) {
            val now = currentTime()
            val externalId = "ws_$now"
            db.workspaceQueriesQueries.ws_insert(
                externalId = externalId,
                name = name,
                description = description,
                isDefault = if (isDefault) 1L else 0L,
                createdAt = now,
                updatedAt = now
            )
            // JdbcSqliteDriver may not return last_insert_rowid reliably;
            // look up by externalId instead
            db.workspaceQueriesQueries.ws_selectByExternalId(externalId)
                .executeAsOneOrNull()?.toWorkspaceEntity()
                ?: error("Failed to create workspace")
        }

    fun observeSettings(workspaceId: Long): Flow<WorkspaceSettingsEntity?> =
        db.workspaceQueriesQueries.ws_settingsByWorkspaceId(workspaceId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { it.firstOrNull()?.toSettingsEntity() }

    suspend fun saveSettings(settings: WorkspaceSettingsEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            db.workspaceQueriesQueries.ws_updateSettings(
                language = settings.language,
                theme = settings.theme,
                defaultSort = settings.defaultSort,
                quizDefaultsJson = settings.quizDefaultsJson,
                importDefaultsJson = settings.importDefaultsJson,
                updatedAt = currentTime(),
                workspaceId = settings.workspaceId
            )
            true
        } catch (_: Exception) { false }
    }

    private suspend fun createDefault(): WorkspaceEntity = withContext(Dispatchers.IO) {
        val ws = create(WorkspaceDefaults.DEFAULT_NAME, WorkspaceDefaults.DEFAULT_DESCRIPTION, true)
        // Insert default settings
        val now = currentTime()
        db.workspaceQueriesQueries.ws_insertSettings(
            workspaceId = ws.id,
            language = null, theme = null, defaultSort = null,
            quizDefaultsJson = null, importDefaultsJson = null,
            createdAt = now, updatedAt = now
        )
        // Seed sample data
        seeder.seedIfNeeded()
        ws
    }

    private fun currentTime(): Long = com.ahmedyejam.mks.util.currentTimeMillis()
}

private fun com.ahmedyejam.mks.db.Workspaces.toWorkspaceEntity() = WorkspaceEntity(
    id = id, externalId = externalId, name = name, description = description,
    isDefault = isDefault != 0L, createdAt = createdAt, updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun com.ahmedyejam.mks.db.Workspace_settings.toSettingsEntity() = WorkspaceSettingsEntity(
    id = id, workspaceId = workspaceId, language = language, theme = theme,
    defaultSort = defaultSort, quizDefaultsJson = quizDefaultsJson,
    importDefaultsJson = importDefaultsJson, createdAt = createdAt, updatedAt = updatedAt,
    deletedAt = deletedAt
)
