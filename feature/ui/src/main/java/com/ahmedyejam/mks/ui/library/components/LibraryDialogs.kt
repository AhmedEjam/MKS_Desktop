package com.ahmedyejam.mks.ui.library.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.importer.model.ImportPreviewDto
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.importer.model.MergeStrategy
import com.ahmedyejam.mks.data.local.entity.QuizEntity

data class QuizCreationFilters(
    val mistakesOnly: Boolean = false,
    val markedOnly: Boolean = false,
    val unansweredOnly: Boolean = false
)


@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ImportReviewDialog(
    preview: ImportPreviewDto,
    onDismiss: () -> Unit,
    onConfirm: (strategy: MergeStrategy, allowInsecureHttpImages: Boolean) -> Unit
) {
    var strategy by remember { mutableStateOf(MergeStrategy.SKIP_EXISTING) }
    var allowInsecureHttpImages by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_review_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.import_summary), style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.books_label))
                            Text("${preview.booksToCreate.size + preview.booksToUpdate.size}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.quizzes_label))
                            Text("${preview.quizzesToCreate.size + preview.quizzesToUpdate.size}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.questions_label))
                            Text("${preview.questionsToCreate.size + preview.questionsToUpdate.size}")
                        }
                    }
                }

                // Strategy selection
                Text(stringResource(R.string.merge_strategy_label), style = MaterialTheme.typography.titleSmall)
                Column {
                    MergeStrategy.entries.forEach { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { strategy = entry }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = strategy == entry, onClick = { strategy = entry })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (entry) {
                                    MergeStrategy.SKIP_EXISTING -> stringResource(R.string.strategy_skip)
                                    MergeStrategy.OVERWRITE_EXISTING -> stringResource(R.string.strategy_overwrite)
                                    MergeStrategy.DUPLICATE -> stringResource(R.string.strategy_duplicate)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Security setting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { allowInsecureHttpImages = !allowInsecureHttpImages }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = allowInsecureHttpImages, onCheckedChange = { allowInsecureHttpImages = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.allow_insecure_images), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(strategy, allowInsecureHttpImages) }) {
                Text(stringResource(R.string.import_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.import_cancel))
            }
        }
    )
}

@Composable
fun ImportWarningsDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_result_warnings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(
                        R.string.import_result_warnings_summary,
                        result.warnings.size
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (result.skippedRecordsCount > 0) {
                    Text(
                        stringResource(
                            R.string.import_result_skipped_summary,
                            result.skippedRecordsCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                result.warnings.forEach { warning ->
                    Text(
                        text = "• ${warning.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    warning.details?.takeIf { it.isNotBlank() }?.let { details ->
                        Text(
                            text = "  $details",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizDialog(
    quizzes: List<QuizEntity>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, coverImage: String?, sourceQuizIds: List<Long>, sourceCategories: List<String>, filters: QuizCreationFilters) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var coverImage by remember { mutableStateOf<String?>(null) }
    
    var isFromExisting by remember { mutableStateOf(false) }
    var selectedQuizIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    
    var filterMistakesOnly by remember { mutableStateOf(false) }
    var filterMarkedOnly by remember { mutableStateOf(false) }
    var filterUnansweredOnly by remember { mutableStateOf(false) }

    val allQuizzesSelected = quizzes.isNotEmpty() && selectedQuizIds.size == quizzes.size
    val allCategoriesSelected = categories.isNotEmpty() && selectedCategories.size == categories.size

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { coverImage = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Quiz") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imageLauncher.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (!coverImage.isNullOrBlank()) {
                        AsyncImage(
                            model = coverImage,
                            contentDescription = "Cover Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(32.dp))
                            Text(stringResource(R.string.show_cover_images), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Quiz Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Mode Selection
                Text("Quiz Content", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !isFromExisting, onClick = { isFromExisting = false })
                    Text("Empty Quiz", modifier = Modifier.clickable { isFromExisting = false })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isFromExisting, onClick = { isFromExisting = true })
                    Text("From Existing Quizzes & Categories", modifier = Modifier.clickable { isFromExisting = true })
                }

                if (isFromExisting) {
                    Text("Filters", style = MaterialTheme.typography.titleSmall)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = filterMistakesOnly, onCheckedChange = { filterMistakesOnly = it })
                            Text("Mistaken questions only", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = filterMarkedOnly, onCheckedChange = { filterMarkedOnly = it })
                            Text("Marked questions only", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = filterUnansweredOnly, onCheckedChange = { filterUnansweredOnly = it })
                            Text("Unanswered questions only", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (quizzes.isNotEmpty()) {
                        Text("Source Quizzes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedQuizIds = if (allQuizzesSelected) emptySet() else quizzes.map { it.id }.toSet()
                                    }.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = allQuizzesSelected, onCheckedChange = { 
                                        selectedQuizIds = if (it) quizzes.map { q -> q.id }.toSet() else emptySet()
                                    })
                                    Text("Select All", style = MaterialTheme.typography.bodyMedium)
                                }
                                quizzes.forEach { quiz ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedQuizIds = if (selectedQuizIds.contains(quiz.id)) selectedQuizIds - quiz.id else selectedQuizIds + quiz.id
                                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(checked = selectedQuizIds.contains(quiz.id), onCheckedChange = { checked ->
                                            selectedQuizIds = if (checked) selectedQuizIds + quiz.id else selectedQuizIds - quiz.id
                                        })
                                        Text(quiz.title, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    if (categories.isNotEmpty()) {
                        Text("Source Categories (Filters)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedCategories = if (allCategoriesSelected) emptySet() else categories.toSet()
                                    }.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = allCategoriesSelected, onCheckedChange = { 
                                        selectedCategories = if (it) categories.toSet() else emptySet()
                                    })
                                    Text("Select All", style = MaterialTheme.typography.bodyMedium)
                                }
                                categories.forEach { cat ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedCategories = if (selectedCategories.contains(cat)) selectedCategories - cat else selectedCategories + cat
                                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(checked = selectedCategories.contains(cat), onCheckedChange = { checked ->
                                            selectedCategories = if (checked) selectedCategories + cat else selectedCategories - cat
                                        })
                                        Text(cat, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(
                        titleText, 
                        descriptionText, 
                        coverImage, 
                        if (isFromExisting) selectedQuizIds.toList() else emptyList(),
                        if (isFromExisting) selectedCategories.toList() else emptyList(),
                        QuizCreationFilters(
                            mistakesOnly = filterMistakesOnly,
                            markedOnly = filterMarkedOnly,
                            unansweredOnly = filterUnansweredOnly
                        )
                    )
                },
                enabled = titleText.isNotBlank() && (!isFromExisting || (selectedQuizIds.isNotEmpty() || selectedCategories.isNotEmpty()))
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
