package com.ahmedyejam.mks.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.local.entity.WorkspaceEntity
import com.ahmedyejam.mks.ui.library.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceManagerDialog(
    viewModel: LibraryViewModel,
    activeWorkspaceId: Long,
    onDismiss: () -> Unit
) {
    val activeWorkspaces by viewModel.workspaces.collectAsState(initial = emptyList<WorkspaceEntity>())
    val deletedWorkspaces by viewModel.deletedWorkspaces.collectAsState(initial = emptyList<WorkspaceEntity>())

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingWorkspace by remember { mutableStateOf<WorkspaceEntity?>(null) }
    var deletingWorkspace by remember { mutableStateOf<WorkspaceEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Workspaces") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active workspaces
                Text("Active Workspaces", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeWorkspaces, key = { it.id }) { workspace ->
                        val isActive = workspace.id == activeWorkspaceId
                        val containerColor = if (isActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(containerColor)
                                .clickable {
                                    if (!isActive) {
                                        viewModel.selectWorkspace(workspace.id)
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = workspace.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (!workspace.description.isNullOrBlank()) {
                                    Text(
                                        text = workspace.description!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { editingWorkspace = workspace }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit workspace",
                                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!workspace.isDefault) {
                                    IconButton(onClick = { deletingWorkspace = workspace }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete workspace",
                                            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Deleted Workspaces section
                if (deletedWorkspaces.isNotEmpty()) {
                    Text("Deleted Workspaces (Trash)", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedWorkspaces, key = { it.id }) { workspace ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = workspace.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { viewModel.restoreWorkspace(workspace.id) }) {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = "Restore workspace",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { viewModel.permanentlyDeleteWorkspace(workspace) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Permanently delete workspace",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Workspace")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Nested Workspace Creation Dialog
    if (showCreateDialog) {
        var newName by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Workspace Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.insertWorkspace(newName, newDesc.takeIf { it.isNotBlank() })
                            showCreateDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Nested Workspace Edit Dialog
    editingWorkspace?.let { workspace ->
        var editName by remember { mutableStateOf(workspace.name) }
        var editDesc by remember { mutableStateOf(workspace.description ?: "") }

        AlertDialog(
            onDismissRequest = { editingWorkspace = null },
            title = { Text("Edit Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Workspace Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateWorkspace(
                                workspace.copy(
                                    name = editName.trim(),
                                    description = editDesc.trim().takeIf { it.isNotBlank() }
                                )
                            )
                            editingWorkspace = null
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingWorkspace = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Nested Workspace Delete Confirmation Dialog
    deletingWorkspace?.let { workspace ->
        AlertDialog(
            onDismissRequest = { deletingWorkspace = null },
            title = { Text("Delete Workspace?") },
            text = {
                Text("Are you sure you want to delete workspace \"${workspace.name}\"? All books and content inside this workspace will be soft-deleted and moved to the Trash Bin.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkspace(workspace)
                        deletingWorkspace = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingWorkspace = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
