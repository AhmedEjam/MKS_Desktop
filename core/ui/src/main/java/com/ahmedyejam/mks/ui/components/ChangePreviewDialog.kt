package com.ahmedyejam.mks.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.simulation.ChangeSimulationResult
import com.ahmedyejam.mks.data.simulation.SimulatedItem

@Composable
fun ChangePreviewDialog(
    result: ChangeSimulationResult,
    confirmLabel: String = "Apply",
    cancelLabel: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(result.title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(result.summary, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (result.affectedBooks > 0) AssistChip(onClick = {}, label = { Text("Books ${result.affectedBooks}") })
                    if (result.affectedQuizzes > 0) AssistChip(onClick = {}, label = { Text("Quizzes ${result.affectedQuizzes}") })
                    if (result.affectedQuestions > 0) AssistChip(onClick = {}, label = { Text("Questions ${result.affectedQuestions}") })
                }
                if (result.warnings.isNotEmpty()) {
                    PreviewSection("Warnings", result.warnings.mapIndexed { index, warning ->
                        SimulatedItem("warning-$index", "Warning", warning)
                    }, showIcon = true)
                }
                PreviewSection("Will create", result.createdItems)
                PreviewSection("Will update", result.updatedItems)
                PreviewSection("Will delete", result.deletedItems)
                PreviewSection("Will skip", result.skippedItems)
                PreviewSection("Blocked", result.blockedItems, showIcon = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !result.hasBlockingItems,
                onClick = onConfirm
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } }
    )
}

@Composable
private fun PreviewSection(
    title: String,
    items: List<SimulatedItem>,
    showIcon: Boolean = false
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Spacer(Modifier.height(2.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        items.take(8).forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (showIcon) Icon(Icons.Default.Warning, contentDescription = null)
                Column {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium)
                    val detail = listOfNotNull(item.subtitle, item.reason).joinToString(" • ")
                    if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (items.size > 8) Text("+${items.size - 8} more", style = MaterialTheme.typography.bodySmall)
    }
}
