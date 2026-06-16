package com.ahmedyejam.mks.data.exchange.v7

import kotlinx.serialization.Serializable

object MksExchangeV7Paths {
    const val FORMAT = "mks.exchange"
    const val SCHEMA_VERSION = 7
    const val ANDROID_ROOM_SCHEMA = 26
    const val MANIFEST = "manifest.json"
    const val WORKSPACE = "workspace.json"
    const val BOOKS = "data/books.json"
    const val QUIZZES = "data/quizzes.json"
    const val QUESTIONS = "data/questions.json"
    const val QUESTION_CATEGORIES = "data/question_categories.json"
    const val ASSET_REFERENCES = "data/asset_references.json"
    const val QUESTION_ASSETS = "data/question_assets.json"
    const val SOURCE_DOCUMENTS = "data/source_documents.json"
    const val ANNOTATIONS = "data/annotations.json"
    const val MEDIA_MANIFEST = "data/media_manifest.json"
    const val SOFT_DELETES = "data/soft_deletes.json"
    const val MEDIA_DIRECTORY = "media"
}

@Serializable
data class MksExchangeV7Manifest(
    val format: String = MksExchangeV7Paths.FORMAT,
    val schemaVersion: Int = MksExchangeV7Paths.SCHEMA_VERSION,
    val androidRoomSchema: Int = MksExchangeV7Paths.ANDROID_ROOM_SCHEMA,
    val iosBundleVersion: String = "unknown",
    val exportedAt: Long = System.currentTimeMillis(),
    val app: String = "Android MKS",
    val archiveKind: String = "stage4c-android-schema7-parity",
    val includesMedia: Boolean = false,
    val stableIdPolicy: String = "externalId required for workspace/book/quiz/question; numeric ids are local.",
    val softDeletePolicy: String = "deletedAt rows are preserved through soft_deletes.json when present.",
    val entries: List<String> = emptyList(),
    val counts: MksExchangeV7Counts = MksExchangeV7Counts(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class MksExchangeV7Counts(
    val workspaces: Int = 0,
    val workspaceSettings: Int = 0,
    val books: Int = 0,
    val quizzes: Int = 0,
    val questions: Int = 0,
    val questionCategories: Int = 0,
    val assetReferences: Int = 0,
    val questionAssets: Int = 0,
    val sourceDocuments: Int = 0,
    val annotations: Int = 0,
    val mediaFiles: Int = 0,
    val softDeletedRecords: Int = 0,
)

@Serializable
data class MksExchangeV7WorkspaceEnvelope(
    val workspaces: List<MksExchangeV7Workspace> = emptyList(),
    val workspaceSettings: List<MksExchangeV7WorkspaceSettings> = emptyList(),
)

@Serializable
data class MksExchangeV7Workspace(
    val id: Long = 0,
    val externalId: String = "default-workspace",
    val name: String = "Default Workspace",
    val description: String? = null,
    val isDefault: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7WorkspaceSettings(
    val id: Long = 0,
    val workspaceId: Long = 0,
    val language: String? = null,
    val theme: String? = null,
    val defaultSort: String? = null,
    val quizDefaultsJson: String? = null,
    val importDefaultsJson: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7Book(
    val id: Long = 0,
    val workspaceId: Long = 0,
    val externalId: String,
    val title: String,
    val description: String = "",
    val iconName: String? = null,
    val coverImage: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val contentUpdatedAt: Long = 0,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0,
    val isPinned: Boolean = false,
    val isSystem: Boolean = false,
    val fields: List<String> = emptyList(),
    val questionCount: Int = 0,
    val answeredCount: Int = 0,
    val totalAttempts: Int = 0,
    val completionPercentage: Float = 0f,
    val accuracyPercentage: Float = 0f,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7Quiz(
    val id: Long = 0,
    val externalId: String,
    val bookId: Long = 0,
    val title: String,
    val description: String = "",
    val category: String? = null,
    val iconName: String? = null,
    val coverImage: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val contentUpdatedAt: Long = 0,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0,
    val isPinned: Boolean = false,
    val isSystem: Boolean = false,
    val questionCount: Int = 0,
    val answeredCount: Int = 0,
    val totalAttempts: Int = 0,
    val completionPercentage: Float = 0f,
    val accuracyPercentage: Float = 0f,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7Question(
    val id: Long = 0,
    val externalId: String,
    val quizId: Long = 0,
    val text: String,
    val type: String = "SINGLE_CHOICE",
    val options: List<String> = emptyList(),
    val correctAnswers: List<Int> = emptyList(),
    val explanation: String? = null,
    val hint: String? = null,
    val reference: String? = null,
    val weight: Int = 1,
    val imagePath: String? = null,
    val imageName: String? = null,
    val imageSource: String? = null,
    val attempts: Int = 0,
    val correctCount: Int = 0,
    val isDropped: Boolean = false,
    val droppedAt: Long? = null,
    val droppedReason: String? = null,
    val isMarked: Boolean = false,
    val markedAt: Long? = null,
    val markReason: String? = null,
    val markReviewAt: Long? = null,
    val notes: String? = null,
    val categories: List<String> = emptyList(),
    val additionalInfo: String? = null,
    val sourceBookId: String? = null,
    val sourceQuizId: String? = null,
    val sourceQuestionId: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0,
    val timeSpentMs: Long = 0,
    val lastAttemptResult: Boolean? = null,
    val consecutiveCorrect: Int = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7QuestionCategory(
    val questionId: Long = 0,
    val category: String = "",
)

@Serializable
data class MksExchangeV7AssetReference(
    val id: Long = 0,
    val path: String,
    val ownerType: String,
    val ownerId: Long,
    val createdAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7QuestionAsset(
    val id: Long = 0,
    val bookId: Long = 0,
    val quizId: Long = 0,
    val questionId: Long = 0,
    val assetType: String,
    val title: String,
    val description: String? = null,
    val localPath: String? = null,
    val externalUrl: String? = null,
    val mimeType: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long? = null,
    val textContent: String? = null,
    val sourceDocumentId: Long? = null,
    val sourcePage: String? = null,
    val sourceQuote: String? = null,
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    val isPrimary: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7SourceDocument(
    val id: Long = 0,
    val bookId: Long? = null,
    val title: String,
    val sourceType: String = "OTHER",
    val author: String? = null,
    val edition: String? = null,
    val year: String? = null,
    val publisher: String? = null,
    val localPath: String? = null,
    val externalUrl: String? = null,
    val description: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7Annotation(
    val id: Long = 0,
    val workspaceId: Long = 0,
    val bookId: Long = 0,
    val ownerType: String,
    val ownerId: Long,
    val selectedText: String? = null,
    val noteBody: String? = null,
    val colorLabel: String = "YELLOW",
    val positionDataJson: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class MksExchangeV7MediaManifest(
    val files: List<MksExchangeV7MediaFile> = emptyList(),
    val missingFiles: List<String> = emptyList(),
)

@Serializable
data class MksExchangeV7MediaFile(
    val archivePath: String,
    val originalPath: String? = null,
    val fileName: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val ownerKind: String,
    val ownerExternalId: String? = null,
    val ownerId: Long = 0,
    val sha256: String? = null,
)

@Serializable
data class MksExchangeV7SoftDeleteRecord(
    val kind: String,
    val recordId: Long,
    val externalId: String? = null,
    val deletedAt: Long,
)

/**
 * R2 supplemental DAO-native payload for schema-7 export.
 * These structures are not written directly; they carry Room rows into the
 * split schema-7 writer so Android export can populate domains that are not
 * represented by the legacy LibraryBundleDto bridge.
 */
data class MksExchangeV7SupplementalData(
    val assetReferences: List<MksExchangeV7SupplementalAssetReference> = emptyList(),
    val questionAssets: List<MksExchangeV7SupplementalQuestionAsset> = emptyList(),
    val sourceDocuments: List<MksExchangeV7SupplementalSourceDocument> = emptyList(),
    val annotations: List<MksExchangeV7SupplementalAnnotation> = emptyList(),
)

data class MksExchangeV7SupplementalAssetReference(
    val id: Long = 0,
    val path: String,
    val ownerType: String,
    val ownerId: Long,
    val ownerExternalId: String? = null,
    val createdAt: Long = 0,
    val deletedAt: Long? = null,
)

data class MksExchangeV7SupplementalQuestionAsset(
    val id: Long = 0,
    val bookId: Long = 0,
    val quizId: Long = 0,
    val questionId: Long = 0,
    val bookExternalId: String? = null,
    val quizExternalId: String? = null,
    val questionExternalId: String? = null,
    val assetType: String,
    val title: String,
    val description: String? = null,
    val localPath: String? = null,
    val externalUrl: String? = null,
    val mimeType: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long? = null,
    val textContent: String? = null,
    val sourceDocumentId: Long? = null,
    val sourcePage: String? = null,
    val sourceQuote: String? = null,
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    val isPrimary: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

data class MksExchangeV7SupplementalSourceDocument(
    val id: Long = 0,
    val bookId: Long? = null,
    val bookExternalId: String? = null,
    val title: String,
    val sourceType: String = "OTHER",
    val author: String? = null,
    val edition: String? = null,
    val year: String? = null,
    val publisher: String? = null,
    val localPath: String? = null,
    val externalUrl: String? = null,
    val description: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

data class MksExchangeV7SupplementalAnnotation(
    val id: Long = 0,
    val workspaceId: Long = 0,
    val bookId: Long = 0,
    val bookExternalId: String? = null,
    val ownerType: String,
    val ownerId: Long,
    val ownerExternalId: String? = null,
    val selectedText: String? = null,
    val noteBody: String? = null,
    val colorLabel: String = "YELLOW",
    val positionDataJson: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)
