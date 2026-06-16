package com.ahmedyejam.mks.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.db.MksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AssetRepository(private val db: MksDatabase) {

    fun observeAssetsByQuestion(questionId: Long): Flow<List<QuestionAssetEntity>> =
        db.assetQueriesQueries.qas_selectByQuestion(questionId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }

    suspend fun getAssetById(id: Long): QuestionAssetEntity? = withContext(Dispatchers.IO) {
        db.assetQueriesQueries.qas_selectById(id).executeAsOneOrNull()?.toEntity()
    }

    fun observeSourceDocuments(bookId: Long): Flow<List<SourceDocumentEntity>> =
        db.assetQueriesQueries.sd_selectByBook(bookId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toEntity() } }

    suspend fun getSourceDocument(id: Long): SourceDocumentEntity? = withContext(Dispatchers.IO) {
        db.assetQueriesQueries.sd_selectById(id).executeAsOneOrNull()?.toEntity()
    }

    suspend fun getAssetReferences(ownerType: String, ownerId: Long): List<AssetReferenceEntity> =
        withContext(Dispatchers.IO) {
            db.assetQueriesQueries.as_selectRefsByOwner(ownerType, ownerId)
                .executeAsList().map { it.toEntity() }
        }

    suspend fun insertAssetReference(path: String, ownerType: String, ownerId: Long): Long =
        withContext(Dispatchers.IO) {
            db.assetQueriesQueries.as_insertRef(path, ownerType, ownerId, currentTime())
            0L
        }

    private fun currentTime(): Long = com.ahmedyejam.mks.util.currentTimeMillis()
}

// Mappers
private fun com.ahmedyejam.mks.db.Question_assets.toEntity() = QuestionAssetEntity(
    id = id, bookId = bookId, quizId = quizId, questionId = questionId,
    assetType = assetType, title = title, description = description,
    localPath = localPath, externalUrl = externalUrl, mimeType = mimeType,
    fileName = fileName, fileSizeBytes = fileSizeBytes, textContent = textContent,
    sourceDocumentId = sourceDocumentId, sourcePage = sourcePage, sourceQuote = sourceQuote,
    sortOrder = sortOrder.toInt(), isPinned = isPinned != 0L, isPrimary = isPrimary != 0L,
    createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt
)
private fun com.ahmedyejam.mks.db.Source_documents.toEntity() = SourceDocumentEntity(
    id = id, bookId = bookId, title = title, sourceType = sourceType,
    author = author, edition = edition, year = year, publisher = publisher,
    localPath = localPath, externalUrl = externalUrl, description = description,
    createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt
)
private fun com.ahmedyejam.mks.db.Asset_references.toEntity() = AssetReferenceEntity(
    id = id, path = path, ownerType = ownerType, ownerId = ownerId,
    createdAt = createdAt, deletedAt = deletedAt
)
