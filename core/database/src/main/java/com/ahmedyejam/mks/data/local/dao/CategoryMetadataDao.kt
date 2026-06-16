package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.CategoryMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryMetadataDao {
    @Query("SELECT * FROM category_metadata")
    fun getAllMetadata(): Flow<List<CategoryMetadataEntity>>

    @Query("SELECT * FROM category_metadata WHERE name = :name")
    suspend fun getMetadataForCategory(name: String): CategoryMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: CategoryMetadataEntity)

    @Delete
    suspend fun deleteMetadata(metadata: CategoryMetadataEntity)

    @Query("DELETE FROM category_metadata WHERE name = :name")
    suspend fun deleteMetadataByName(name: String)

    @Query("DELETE FROM category_metadata")
    suspend fun deleteAllMetadata()
}
