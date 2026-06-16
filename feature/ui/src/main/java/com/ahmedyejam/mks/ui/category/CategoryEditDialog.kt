package com.ahmedyejam.mks.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.model.CategoryWithMetadata
import com.ahmedyejam.mks.ui.library.components.EntityInfoSection

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: CategoryWithMetadata,
    allCategories: List<CategoryWithMetadata>,
    books: List<BookEntity>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onMerge: (String) -> Unit,
    onGetMergePreview: suspend (String) -> Int,
    onCreateQuiz: (String, Long) -> Unit,
    onTogglePin: () -> Unit,
    onUpdateEmoji: (String?) -> Unit,
    onUpdateColor: (Int?) -> Unit,
    onViewQuestions: () -> Unit
) {
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showMergeDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateQuizDialog by rememberSaveable { mutableStateOf(false) }
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_msg, category.name)) },
            confirmButton = {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showRenameDialog) {
        var newName by rememberSaveable { mutableStateOf(category.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_category_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.new_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (allCategories.any { it.name == newName } && newName != category.name) {
                        showRenameDialog = false
                        showMergeDialog = true
                    } else {
                        onRename(newName)
                        showRenameDialog = false
                    }
                }) {
                    Text(stringResource(R.string.rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showMergeDialog) {
        var targetCategory by rememberSaveable(allCategories) { 
            mutableStateOf(allCategories.firstOrNull { it.name != category.name }?.name ?: "") 
        }
        var mergeCount by remember { mutableIntStateOf(0) }
        
        LaunchedEffect(targetCategory) {
            if (targetCategory.isNotBlank()) {
                mergeCount = onGetMergePreview(targetCategory)
            }
        }

        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text(stringResource(R.string.merge_category_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.merge_into_prefix, category.name))
                    Spacer(modifier = Modifier.height(8.dp))
                    allCategories.filter { it.name != category.name }.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { targetCategory = cat.name }.padding(8.dp)
                        ) {
                            RadioButton(selected = targetCategory == cat.name, onClick = { targetCategory = cat.name })
                            Text(cat.name)
                        }
                    }
                    if (targetCategory.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.merge_count_msg, mergeCount, targetCategory),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    onMerge(targetCategory)
                    showMergeDialog = false
                }, enabled = targetCategory.isNotBlank()) {
                    Text(stringResource(R.string.merge))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCreateQuizDialog) {
        val defaultTitle = stringResource(R.string.category_quiz_prefix, category.name)
        var quizTitle by rememberSaveable { mutableStateOf(defaultTitle) }
        var selectedBookId by rememberSaveable { mutableLongStateOf(books.firstOrNull()?.id ?: -1L) }
        
        AlertDialog(
            onDismissRequest = { showCreateQuizDialog = false },
            title = { Text(stringResource(R.string.create_quiz_from_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quizTitle,
                        onValueChange = { quizTitle = it },
                        label = { Text(stringResource(R.string.quiz_title_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.select_book_label), style = MaterialTheme.typography.labelMedium)
                    books.forEach { book ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedBookId = book.id }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = selectedBookId == book.id, onClick = { selectedBookId = book.id })
                            Text(book.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    onCreateQuiz(quizTitle, selectedBookId)
                    showCreateQuizDialog = false
                }, enabled = selectedBookId != -1L) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateQuizDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEmojiPicker) {
        val emojis = listOf("🎯", "📚", "💡", "🧪", "🌍", "💻", "🎨", "⚽", "🎵", "📝", "🧠", "⚖️", "⚙️", "🔋", "💊")
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false },
            title = { Text(stringResource(R.string.select_emoji_title)) },
            text = {
                Column {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emojis.forEach { emoji ->
                            TextButton(
                                onClick = {
                                    onUpdateEmoji(emoji)
                                    showEmojiPicker = false
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                        TextButton(
                            onClick = {
                                onUpdateEmoji(null)
                                showEmojiPicker = false
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clear_label))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEmojiPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showColorPicker) {
        val colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            androidx.compose.ui.graphics.Color(0xFFEF5350), // Red
            androidx.compose.ui.graphics.Color(0xFFEC407A), // Pink
            androidx.compose.ui.graphics.Color(0xFFAB47BC), // Purple
            androidx.compose.ui.graphics.Color(0xFF5C6BC0), // Indigo
            androidx.compose.ui.graphics.Color(0xFF42A5F5), // Blue
            androidx.compose.ui.graphics.Color(0xFF26A69A), // Teal
            androidx.compose.ui.graphics.Color(0xFF66BB6A), // Green
            androidx.compose.ui.graphics.Color(0xFFFFEE58), // Yellow
            androidx.compose.ui.graphics.Color(0xFFFFA726), // Orange
            androidx.compose.ui.graphics.Color(0xFF8D6E63), // Brown
            androidx.compose.ui.graphics.Color(0xFF78909C), // Blue Grey
        )
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(stringResource(R.string.select_color_title)) },
            text = {
                Column {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { color ->
                            Surface(
                                onClick = {
                                    onUpdateColor(color.toArgb())
                                    showColorPicker = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = color,
                                modifier = Modifier.size(48.dp),
                                border = if (category.color == color.toArgb()) {
                                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                                } else null
                            ) {}
                        }
                        Surface(
                            onClick = {
                                onUpdateColor(null)
                                showColorPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(48.dp),
                            border = if (category.color == null) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                            } else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clear_label))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    onClick = { showEmojiPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = category.color?.let { androidx.compose.ui.graphics.Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = category.emoji ?: "📂",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.questions_count, category.questionCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showColorPicker = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = category.color?.let { androidx.compose.ui.graphics.Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = stringResource(R.string.color_label)
                    )
                }
                IconButton(
                    onClick = onTogglePin,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (category.isPinned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (category.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = stringResource(R.string.pin_label)
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_info_stats)) },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                trailingContent = {
                    Switch(checked = showInfo, onCheckedChange = { showInfo = it })
                },
                modifier = Modifier.clickable { showInfo = !showInfo }
            )

            AnimatedVisibility(visible = showInfo) {
                EntityInfoSection(
                    title = stringResource(R.string.metadata_stats_title),
                    info = listOf(
                        stringResource(R.string.name_label) to category.name,
                        stringResource(R.string.total_questions_label) to category.questionCount.toString(),
                        stringResource(R.string.answered_label) to "${category.answeredCount} (${if (category.questionCount > 0) (category.answeredCount.toFloat() / category.questionCount * 100).toInt() else 0}%)",
                        stringResource(R.string.accuracy_label) to "${(category.accuracyPercentage * 100).toInt()}%",
                        stringResource(R.string.pinned_label) to if (category.isPinned) stringResource(R.string.yes_label) else stringResource(R.string.no_label),
                        stringResource(R.string.emoji_label) to (category.emoji ?: stringResource(R.string.default_label))
                    ),
                    modifier = Modifier.padding(8.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.browse_questions)) },
                leadingContent = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null) },
                modifier = Modifier.clickable { 
                    onViewQuestions()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(if (category.isPinned) R.string.unpin_from_top else R.string.pin_to_top)) },
                leadingContent = { 
                    Icon(
                        Icons.Rounded.PushPin, 
                        contentDescription = null,
                        tint = if (category.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                modifier = Modifier.clickable { 
                    onTogglePin()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.rename)) },
                leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                modifier = Modifier.clickable { 
                    showRenameDialog = true
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.merge_into_category)) },
                leadingContent = { Icon(Icons.Rounded.Merge, contentDescription = null) },
                modifier = Modifier.clickable { 
                    showMergeDialog = true
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.make_separate_quiz)) },
                leadingContent = { Icon(Icons.AutoMirrored.Rounded.FactCheck, contentDescription = null) },
                modifier = Modifier.clickable { 
                    showCreateQuizDialog = true
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete)) },
                leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { 
                    showDeleteConfirm = true
                }
            )
        }
    }
}
