package com.ahmedyejam.mks.ui.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    isSearching: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedBookId: Long,
    selectedCategory: String?,
    currentBookTitle: String?,
    onNavigationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSortClick: () -> Unit,
    currentViewMode: String,
    onToggleViewMode: () -> Unit,
    showOverflowMenu: Boolean,
    onOverflowMenuToggle: (Boolean) -> Unit,
    onContactClick: () -> Unit,
    onWorkspaceManagerClick: () -> Unit,
    onTrashBinClick: () -> Unit
) {
    val tokens = LocalMksDesignTokens.current
    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            if (isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(tokens.chipRadius),
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, null)
                            }
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            } else {
                Text(
                    text = when {
                        selectedBookId != -1L -> currentBookTitle ?: stringResource(R.string.quizzes_title)
                        selectedCategory != null -> stringResource(R.string.category_title_prefix, selectedCategory)
                        else -> stringResource(R.string.mks_library_title)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (selectedBookId != -1L || selectedCategory != null || isSearching) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            } else {
                Box(Modifier.size(48.dp))
            }
        },
        actions = {
            if (!isSearching) {
                IconButton(onClick = onContactClick) {
                    Icon(Icons.Rounded.SupportAgent, contentDescription = "Contact")
                }
                Box {
                    IconButton(onClick = { onOverflowMenuToggle(true) }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.settings_title))
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { onOverflowMenuToggle(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, null) },
                            onClick = {
                                onOverflowMenuToggle(false)
                                onSortClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (currentViewMode == "GRID") stringResource(R.string.list_view_label) else stringResource(R.string.grid_view_label)) },
                            leadingIcon = {
                                Icon(
                                    if (currentViewMode == "GRID") Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                    null
                                )
                            },
                            onClick = {
                                onOverflowMenuToggle(false)
                                onToggleViewMode()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.search)) },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            onClick = {
                                onOverflowMenuToggle(false)
                                onSearchClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_title)) },
                            leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                            onClick = {
                                onOverflowMenuToggle(false)
                                onSettingsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Workspaces") },
                            leadingIcon = { Icon(Icons.Rounded.Workspaces, null) },
                            onClick = {
                                onOverflowMenuToggle(false)
                                onWorkspaceManagerClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Trash Bin") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = {
                                onOverflowMenuToggle(false)
                                onTrashBinClick()
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    )
}
