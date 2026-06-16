package com.ahmedyejam.mks.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.local.entity.AnnotationColorLabel
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity
import com.ahmedyejam.mks.data.local.entity.QuestionAssetEntity
import com.ahmedyejam.mks.data.local.entity.QuestionAssetType
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.SourceDocumentEntity
import com.ahmedyejam.mks.data.local.entity.SourceDocumentTypes
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun QuestionAssetsDialog(
    question: QuestionEntity,
    bookId: Long,
    assets: List<QuestionAssetEntity>,
    annotations: List<AnnotationEntity> = emptyList(),
    sourceDocuments: List<SourceDocumentEntity> = emptyList(),
    linkedBlueprints: List<NoteBlueprintEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAddAsset: (QuestionAssetEntity) -> Unit,
    onUpdateAsset: (QuestionAssetEntity) -> Unit,
    onDeleteAsset: (QuestionAssetEntity) -> Unit,
    onAddAnnotation: (selectedText: String?, noteBody: String, colorLabel: String) -> Unit = { _, _, _ -> },
    onUpdateAnnotation: (AnnotationEntity) -> Unit = {},
    onDeleteAnnotation: (AnnotationEntity) -> Unit = {},
    onCreateSourceAndAddAsset: (QuestionAssetEntity, SourceDocumentEntity) -> Unit = { asset, _ -> onAddAsset(asset) },
    onCreateBlueprintFromQuestion: () -> Unit = {}
) {
    var editingAsset by remember { mutableStateOf<QuestionAssetEntity?>(null) }
    var editingAnnotation by remember { mutableStateOf<AnnotationEntity?>(null) }
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    var showAnnotationForm by rememberSaveable { mutableStateOf(false) }

    if (showAnnotationForm || editingAnnotation != null) {
        AnnotationFormDialog(
            existing = editingAnnotation,
            onDismiss = {
                editingAnnotation = null
                showAnnotationForm = false
            },
            onSave = { selectedText, noteBody, colorLabel ->
                val existing = editingAnnotation
                if (existing == null) {
                    onAddAnnotation(selectedText, noteBody, colorLabel)
                } else {
                    onUpdateAnnotation(
                        existing.copy(
                            selectedText = selectedText?.takeIf { it.isNotBlank() },
                            noteBody = noteBody.takeIf { it.isNotBlank() },
                            colorLabel = colorLabel,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                editingAnnotation = null
                showAnnotationForm = false
            }
        )
    }

    if (showAddForm || editingAsset != null) {
        QuestionAssetFormDialog(
            question = question,
            bookId = bookId,
            existing = editingAsset,
            sourceDocuments = sourceDocuments,
            onDismiss = {
                editingAsset = null
                showAddForm = false
            },
            onSave = { asset ->
                if (asset.id == 0L) onAddAsset(asset) else onUpdateAsset(asset)
                editingAsset = null
                showAddForm = false
            },
            onCreateSourceAndSave = { asset, source ->
                onCreateSourceAndAddAsset(asset, source)
                editingAsset = null
                showAddForm = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Question attachments & sources") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = question.text,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (assets.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("No attachments yet", fontWeight = FontWeight.SemiBold)
                            Text("Add an image, PDF, text note, web link, or source citation to make this question a richer study workspace.")
                        }
                    }
                } else {
                    assets.forEach { asset ->
                        QuestionAssetCard(
                            asset = asset,
                            sourceDocuments = sourceDocuments,
                            onEdit = { editingAsset = asset },
                            onDelete = { onDeleteAsset(asset) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Annotations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAnnotationForm = true }) { Text("Add note") }
                }
                if (annotations.isEmpty()) {
                    Text("No annotations yet. Add a highlight or margin note for this question.")
                } else {
                    annotations.forEach { annotation ->
                        AnnotationCard(
                            annotation = annotation,
                            onEdit = { editingAnnotation = annotation },
                            onDelete = { onDeleteAnnotation(annotation) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Linked blueprints", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (linkedBlueprints.isEmpty()) {
                    Text("No linked blueprint yet. Create one from this question when you want a structured reusable note.")
                } else {
                    linkedBlueprints.forEach { blueprint ->
                        val tokens = LocalMksDesignTokens.current
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(tokens.cardRadius),
                            elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(blueprint.title, fontWeight = FontWeight.SemiBold)
                                }
                                Text(blueprint.blueprintMode, style = MaterialTheme.typography.labelSmall)
                                blueprint.summary?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 3) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCreateBlueprintFromQuestion) { Text("Make blueprint") }
                Button(onClick = { showAddForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun AnnotationCard(
    annotation: AnnotationEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showActions: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(annotation.colorLabel.lowercase().replaceFirstChar { it.titlecase() }, fontWeight = FontWeight.SemiBold)
                    Text(annotation.ownerType.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.labelSmall)
                }
                if (showActions) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit annotation") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete annotation") }
                }
            }
            annotation.selectedText?.takeIf { it.isNotBlank() }?.let { Text("Highlight: $it", maxLines = 3) }
            annotation.noteBody?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 5) }
        }
    }
}

@Composable
private fun AnnotationFormDialog(
    existing: AnnotationEntity?,
    onDismiss: () -> Unit,
    onSave: (selectedText: String?, noteBody: String, colorLabel: String) -> Unit
) {
    var selectedText by rememberSaveable(existing?.id) { mutableStateOf(existing?.selectedText.orEmpty()) }
    var noteBody by rememberSaveable(existing?.id) { mutableStateOf(existing?.noteBody.orEmpty()) }
    var colorLabel by rememberSaveable(existing?.id) { mutableStateOf(existing?.colorLabel ?: AnnotationColorLabel.YELLOW) }
    val colorOptions = listOf(
        AnnotationColorLabel.YELLOW,
        AnnotationColorLabel.GREEN,
        AnnotationColorLabel.BLUE,
        AnnotationColorLabel.PINK,
        AnnotationColorLabel.ORANGE,
        AnnotationColorLabel.RED
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add annotation" else "Edit annotation") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = selectedText,
                    onValueChange = { selectedText = it },
                    label = { Text("Highlighted text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = noteBody,
                    onValueChange = { noteBody = it },
                    label = { Text("Margin note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    colorOptions.forEach { option ->
                        FilterChip(
                            selected = colorLabel == option,
                            onClick = { colorLabel = option },
                            label = { Text(option.lowercase().replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedText.isNotBlank() || noteBody.isNotBlank(),
                onClick = { onSave(selectedText.trim().takeIf { it.isNotBlank() }, noteBody.trim(), colorLabel) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun QuestionAssetCard(
    asset: QuestionAssetEntity,
    sourceDocuments: List<SourceDocumentEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showActions: Boolean = true
) {
    val tokens = LocalMksDesignTokens.current
    val source = asset.sourceDocumentId?.let { id -> sourceDocuments.firstOrNull { it.id == id } }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (asset.assetType == QuestionAssetType.SOURCE_REFERENCE) Icons.Default.Book else Icons.Default.AttachFile,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(asset.title, fontWeight = FontWeight.SemiBold)
                    Text(asset.assetType.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
                }
                if (asset.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary)
                }
                if (showActions) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit asset")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete asset")
                    }
                }
            }
            source?.let {
                AssistChip(onClick = {}, label = { Text(it.title) }, leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) })
            }
            asset.sourcePage?.takeIf { it.isNotBlank() }?.let { Text("Page/location: $it") }
            asset.sourceQuote?.takeIf { it.isNotBlank() }?.let { Text("Quote: $it", maxLines = 4) }
            asset.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
            asset.externalUrl?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            asset.localPath?.takeIf { it.isNotBlank() }?.let { Text("Local: $it", style = MaterialTheme.typography.labelSmall) }
            asset.textContent?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 4) }
        }
    }
}

@Composable
private fun QuestionAssetFormDialog(
    question: QuestionEntity,
    bookId: Long,
    existing: QuestionAssetEntity?,
    sourceDocuments: List<SourceDocumentEntity>,
    onDismiss: () -> Unit,
    onSave: (QuestionAssetEntity) -> Unit,
    onCreateSourceAndSave: (QuestionAssetEntity, SourceDocumentEntity) -> Unit
) {
    val availableTypes = remember { QuestionAssetType.firstPassTypes + QuestionAssetType.SOURCE_REFERENCE }
    var assetType by rememberSaveable(existing?.id) { mutableStateOf(existing?.assetType ?: QuestionAssetType.IMAGE) }
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var localPath by rememberSaveable(existing?.id) { mutableStateOf(existing?.localPath ?: "") }
    var externalUrl by rememberSaveable(existing?.id) { mutableStateOf(existing?.externalUrl ?: "") }
    var textContent by rememberSaveable(existing?.id) { mutableStateOf(existing?.textContent ?: "") }
    var selectedSourceIdText by rememberSaveable(existing?.id) { mutableStateOf(existing?.sourceDocumentId?.toString().orEmpty()) }
    var sourcePage by rememberSaveable(existing?.id) { mutableStateOf(existing?.sourcePage ?: "") }
    var sourceQuote by rememberSaveable(existing?.id) { mutableStateOf(existing?.sourceQuote ?: "") }
    var newSourceTitle by rememberSaveable(existing?.id) { mutableStateOf("") }
    var newSourceType by rememberSaveable(existing?.id) { mutableStateOf(SourceDocumentTypes.BOOK) }
    var newSourceDetails by rememberSaveable(existing?.id) { mutableStateOf("") }
    var isPinned by rememberSaveable(existing?.id) { mutableStateOf(existing?.isPinned ?: false) }
    var isPrimary by rememberSaveable(existing?.id) { mutableStateOf(existing?.isPrimary ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add attachment/source" else "Edit attachment/source") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableTypes.forEach { type ->
                        FilterChip(
                            selected = assetType == type,
                            onClick = { assetType = type },
                            label = { Text(type.replace('_', ' ')) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                when (assetType) {
                    QuestionAssetType.WEB_LINK -> {
                        OutlinedTextField(
                            value = externalUrl,
                            onValueChange = { externalUrl = it },
                            label = { Text("Web link URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    QuestionAssetType.TEXT_NOTE -> {
                        OutlinedTextField(
                            value = textContent,
                            onValueChange = { textContent = it },
                            label = { Text("Text note") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )
                    }
                    QuestionAssetType.SOURCE_REFERENCE -> {
                        Text("Pick existing source", style = MaterialTheme.typography.labelMedium)
                        if (sourceDocuments.isEmpty()) {
                            Text("No book sources yet. Create a simple source below.")
                        } else {
                            sourceDocuments.forEach { source ->
                                FilterChip(
                                    selected = selectedSourceIdText == source.id.toString(),
                                    onClick = { selectedSourceIdText = source.id.toString() },
                                    label = { Text(source.title) },
                                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) }
                                )
                            }
                            TextButton(onClick = { selectedSourceIdText = "" }) { Text("Create new source instead") }
                        }
                        OutlinedTextField(
                            value = sourcePage,
                            onValueChange = { sourcePage = it },
                            label = { Text("Page / section / timestamp") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sourceQuote,
                            onValueChange = { sourceQuote = it },
                            label = { Text("Quote / citation note") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        if (selectedSourceIdText.isBlank() && existing == null) {
                            Text("New source", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newSourceTitle,
                                onValueChange = { newSourceTitle = it },
                                label = { Text("Source title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SourceDocumentTypes.all.forEach { type ->
                                    FilterChip(
                                        selected = newSourceType == type,
                                        onClick = { newSourceType = type },
                                        label = { Text(type.lowercase().replaceFirstChar { it.titlecase() }) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = newSourceDetails,
                                onValueChange = { newSourceDetails = it },
                                label = { Text("Source details") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = localPath,
                            onValueChange = { localPath = it },
                            label = { Text("Local path / content URI") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = isPinned, onClick = { isPinned = !isPinned }, label = { Text("Pinned") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = isPrimary, onClick = { isPrimary = !isPrimary }, label = { Text("Primary") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val effectiveTitle = title.ifBlank {
                        when (assetType) {
                            QuestionAssetType.SOURCE_REFERENCE -> newSourceTitle.ifBlank { "Source reference" }
                            else -> assetType.replace('_', ' ')
                        }
                    }
                    val baseAsset = QuestionAssetEntity(
                        id = existing?.id ?: 0L,
                        bookId = bookId,
                        quizId = question.quizId,
                        questionId = question.id,
                        assetType = assetType,
                        title = effectiveTitle,
                        description = description.takeIf { it.isNotBlank() },
                        localPath = localPath.takeIf { it.isNotBlank() },
                        externalUrl = externalUrl.takeIf { it.isNotBlank() },
                        textContent = textContent.takeIf { it.isNotBlank() },
                        sourceDocumentId = selectedSourceIdText.toLongOrNull(),
                        sourcePage = sourcePage.takeIf { it.isNotBlank() },
                        sourceQuote = sourceQuote.takeIf { it.isNotBlank() },
                        sortOrder = existing?.sortOrder ?: 0,
                        isPinned = isPinned,
                        isPrimary = isPrimary,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    if (assetType == QuestionAssetType.SOURCE_REFERENCE && selectedSourceIdText.isBlank() && newSourceTitle.isNotBlank() && existing == null) {
                        onCreateSourceAndSave(
                            baseAsset,
                            SourceDocumentEntity(
                                bookId = bookId,
                                title = newSourceTitle.trim(),
                                sourceType = newSourceType,
                                description = newSourceDetails.takeIf { it.isNotBlank() }
                            )
                        )
                    } else {
                        onSave(baseAsset)
                    }
                },
                enabled = title.isNotBlank() || assetType == QuestionAssetType.SOURCE_REFERENCE || assetType == QuestionAssetType.TEXT_NOTE
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun QuestionAssetsReadOnlyDialog(
    question: QuestionEntity,
    assets: List<QuestionAssetEntity>,
    sourceDocuments: List<SourceDocumentEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assets & sources (${assets.size})") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(question.text, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (assets.isEmpty()) {
                    Text("No attachments or source citations for this question yet.")
                } else {
                    assets.forEach { asset ->
                        QuestionAssetCard(
                            asset = asset,
                            sourceDocuments = sourceDocuments,
                            onEdit = {},
                            onDelete = {},
                            showActions = false
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
