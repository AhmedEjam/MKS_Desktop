package com.ahmedyejam.mks.ui.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.core.ui.R

@Composable
fun LibraryFabMenu(
    fabExpanded: Boolean,
    onFabExpandedChange: (Boolean) -> Unit,
    selectedCategory: String?,
    onAdaptiveSelected: (String, String) -> Unit,
    onNewBookClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = fabExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = {
                        onFabExpandedChange(false)
                        val type = if (selectedCategory != null) "CATEGORY" else "ALL"
                        val id = selectedCategory ?: "0"
                        onAdaptiveSelected(type, id)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    FabActionRow(Icons.Rounded.Psychology, stringResource(R.string.adaptive_training))
                }

                if (selectedCategory == null) {
                    SmallFloatingActionButton(
                        onClick = {
                            onFabExpandedChange(false)
                            onNewBookClick()
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        FabActionRow(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.new_book))
                    }
                }

                SmallFloatingActionButton(
                    onClick = {
                        onFabExpandedChange(false)
                        onImportClick()
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    FabActionRow(Icons.Rounded.FileDownload, stringResource(R.string.import_label))
                }

                SmallFloatingActionButton(
                    onClick = {
                        onFabExpandedChange(false)
                        onExportClick()
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    FabActionRow(Icons.Rounded.FileUpload, stringResource(R.string.export_label))
                }
            }
        }

        FloatingActionButton(
            onClick = { onFabExpandedChange(!fabExpanded) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                if (fabExpanded) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = stringResource(R.string.app_name)
            )
        }
    }
}

@Composable
private fun FabActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
