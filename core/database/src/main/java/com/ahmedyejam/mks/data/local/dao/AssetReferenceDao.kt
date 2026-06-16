package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ahmedyejam.mks.data.local.entity.AssetReferenceEntity

@Dao
interface AssetReferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReference(reference: AssetReferenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferences(references: List<AssetReferenceEntity>)

    @Query("UPDATE asset_references SET deletedAt = :deletedAt WHERE ownerType = :ownerType AND ownerId = :ownerId AND path = :path AND deletedAt IS NULL")
    suspend fun deleteReference(ownerType: String, ownerId: Long, path: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE asset_references SET deletedAt = :deletedAt WHERE ownerType = :ownerType AND ownerId = :ownerId AND deletedAt IS NULL")
    suspend fun deleteReferencesForOwner(ownerType: String, ownerId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM asset_references WHERE path = :path AND deletedAt IS NULL")
    suspend fun countReferencesForPath(path: String): Int

    @Query("SELECT * FROM asset_references WHERE ownerType = :ownerType AND ownerId = :ownerId AND deletedAt IS NULL")
    suspend fun getReferencesForOwner(ownerType: String, ownerId: Long): List<AssetReferenceEntity>

    @Query("SELECT * FROM asset_references WHERE deletedAt IS NULL")
    suspend fun getAllReferences(): List<AssetReferenceEntity>

    @Query("SELECT * FROM asset_references ORDER BY createdAt DESC, id ASC")
    suspend fun getAllReferencesIncludingDeleted(): List<AssetReferenceEntity>

    @Query("UPDATE asset_references SET deletedAt = :deletedAt WHERE deletedAt IS NULL")
    suspend fun clearAllReferences(deletedAt: Long = System.currentTimeMillis())

    @Transaction
    suspend fun replaceOwnerReferences(ownerType: String, ownerId: Long, paths: List<String?>) {
        deleteReferencesForOwner(ownerType, ownerId)
        val references = paths
            .mapNotNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }
            .distinct()
            .map { path -> AssetReferenceEntity(path = path, ownerType = ownerType, ownerId = ownerId) }
        if (references.isNotEmpty()) {
            insertReferences(references)
        }
    }
}
