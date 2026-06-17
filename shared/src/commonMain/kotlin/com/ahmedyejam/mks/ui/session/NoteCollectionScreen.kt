package com.ahmedyejam.mks.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedNoteCollectionScreen(
    viewModel: NoteCollectionViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.collection?.title ?: "Notebook") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.blueprints, key = { it.id }) { blueprint ->
                    NoteBlueprintCard(blueprint)
                }
            }
        }
    }
}

@Composable
fun NoteBlueprintCard(blueprint: NoteBlueprintEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(blueprint.title, style = MaterialTheme.typography.titleMedium)
            if (!blueprint.summary.isNullOrBlank()) {
                Text(blueprint.summary!!, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(blueprint.body, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
    }
}
