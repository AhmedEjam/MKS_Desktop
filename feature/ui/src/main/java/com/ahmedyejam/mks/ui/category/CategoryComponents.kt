package com.ahmedyejam.mks.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.model.CategoryWithMetadata

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryChip(
    category: CategoryWithMetadata,
    isSelected: Boolean = false,
    onCategorySelected: (CategoryWithMetadata) -> Unit,
    onCategoryLongClick: (CategoryWithMetadata) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()
    val hasEmoji = category.emoji != null || (category.name.trim().firstOrNull()?.let { it.code > 127 } ?: false)
    val customColor = category.color?.let { Color(it) }
    
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        category.isPinned -> MaterialTheme.colorScheme.primaryContainer
        customColor != null -> if (isDark) customColor.copy(alpha = 0.3f) else customColor.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        category.isPinned -> MaterialTheme.colorScheme.onPrimaryContainer
        customColor != null -> if (isDark) {
            customColor
        } else customColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        border = when {
            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer)
            category.isPinned -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            customColor != null -> BorderStroke(1.dp, customColor.copy(alpha = 0.5f))
            else -> null
        },
        modifier = Modifier.combinedClickable(
            onClick = { onCategorySelected(category) },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCategoryLongClick(category)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val emoji = category.emoji
            if (emoji != null) {
                Text(emoji)
            } else if (!hasEmoji) {
                Icon(
                    Icons.AutoMirrored.Rounded.FactCheck,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
            }
            
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            if (category.questionCount > 0) {
                Surface(
                    color = contentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = category.questionCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }

            if (category.isPinned) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = "Pinned",
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
fun CategoryBrowserDialog(
    categories: List<CategoryWithMetadata>,
    onDismiss: () -> Unit,
    onCategorySelected: (CategoryWithMetadata) -> Unit,
    onCategoryLongClick: (CategoryWithMetadata) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories_header)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.name }) { category ->
                    CategoryChip(
                        category = category,
                        onCategorySelected = onCategorySelected,
                        onCategoryLongClick = onCategoryLongClick
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
