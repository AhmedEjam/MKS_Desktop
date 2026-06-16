package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.WorkspaceEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces WHERE deletedAt IS NULL")
    fun getAllWorkspacesFlow(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE deletedAt IS NOT NULL")
    fun getDeletedWorkspacesFlow(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE isDefault = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun getDefaultWorkspace(): WorkspaceEntity?

    @Query("SELECT * FROM workspaces WHERE id = :id AND deletedAt IS NULL")
    suspend fun getWorkspaceById(id: Long): WorkspaceEntity?

    @Query("SELECT * FROM workspaces WHERE id = :id LIMIT 1")
    suspend fun getWorkspaceByIdIncludingDeleted(id: Long): WorkspaceEntity?

    @Query("SELECT * FROM workspaces WHERE externalId = :externalId AND deletedAt IS NULL LIMIT 1")
    suspend fun getWorkspaceByExternalId(externalId: String): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity): Long

    @Update
    suspend fun updateWorkspace(workspace: WorkspaceEntity)

    @Query("UPDATE workspaces SET deletedAt = :deletedAt, updatedAt = :deletedAt, isDefault = 0 WHERE id = :workspaceId")
    suspend fun softDeleteWorkspaceById(workspaceId: Long, deletedAt: Long)

    @Query("UPDATE workspaces SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :workspaceId")
    suspend fun restoreWorkspaceById(workspaceId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeleteWorkspace(workspace: WorkspaceEntity)

    // Workspace Settings
    @Query("SELECT * FROM workspace_settings WHERE workspaceId = :workspaceId AND deletedAt IS NULL LIMIT 1")
    suspend fun getSettingsByWorkspaceId(workspaceId: Long): WorkspaceSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: WorkspaceSettingsEntity): Long

    @Update
    suspend fun updateSettings(settings: WorkspaceSettingsEntity)
}
