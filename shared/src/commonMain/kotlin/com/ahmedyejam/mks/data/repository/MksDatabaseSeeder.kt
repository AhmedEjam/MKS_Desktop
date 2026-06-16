package com.ahmedyejam.mks.data.repository

import com.ahmedyejam.mks.data.local.WorkspaceDefaults
import com.ahmedyejam.mks.db.MksDatabase
import com.ahmedyejam.mks.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MksDatabaseSeeder(private val db: MksDatabase) {
    suspend fun seedIfNeeded() {
        withContext(Dispatchers.IO) {
            val existing = db.workspaceQueriesQueries.ws_selectDefault().executeAsOneOrNull()
            if (existing != null) return@withContext

            val now = currentTimeMillis()
            val extId = WorkspaceDefaults.DEFAULT_EXTERNAL_ID
            db.workspaceQueriesQueries.ws_insert(
                externalId = extId,
                name = WorkspaceDefaults.DEFAULT_NAME,
                description = WorkspaceDefaults.DEFAULT_DESCRIPTION,
                isDefault = 1L,
                createdAt = now,
                updatedAt = now
            )
            val ws = db.workspaceQueriesQueries.ws_selectByExternalId(extId)
                .executeAsOneOrNull()
            if (ws != null) {
                db.workspaceQueriesQueries.ws_insertSettings(
                    workspaceId = ws.id,
                    language = null, theme = null, defaultSort = null,
                    quizDefaultsJson = null, importDefaultsJson = null,
                    createdAt = now, updatedAt = now
                )
            }
        }
    }
}
