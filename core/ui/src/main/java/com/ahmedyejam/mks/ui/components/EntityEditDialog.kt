package com.ahmedyejam.mks.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityEditDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialImage: String = "",
    showDescription: Boolean = true,
    showImage: Boolean = false,
    titleLabel: String = "Title",
    descriptionLabel: String = "Description (Optional)",
    allowBlankName: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, image: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var image by remember { mutableStateOf(initialImage) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { image = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imageLauncher.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (image.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = image,
                                contentDescription = "Cover Image",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(androidx.compose.material.icons.Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(32.dp))
                                Text("Add Cover Image", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = image,
                        onValueChange = { image = it },
                        label = { Text("Image URL/Path (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(titleLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (showDescription) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(descriptionLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), description.trim(), image.trim()) },
                enabled = allowBlankName || name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
