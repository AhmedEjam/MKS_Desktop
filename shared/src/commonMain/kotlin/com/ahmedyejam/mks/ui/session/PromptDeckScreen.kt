package com.ahmedyejam.mks.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.local.entity.PromptCardEntity
import com.ahmedyejam.mks.ui.components.LoadingErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPromptDeckScreen(
    viewModel: PromptViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.deck?.title ?: "AI Prompt Deck") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            // Card List
            LazyColumn(modifier = Modifier.width(300.dp).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.cards, key = { it.id }) { card ->
                    PromptCardItem(card, isSelected = state.currentCard?.id == card.id) {
                        viewModel.selectCard(card)
                    }
                }
            }

            // Runner
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                state.currentCard?.let { card ->
                    Text(card.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(card.promptText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.runPrompt() }, enabled = !state.isLoading) {
                        Icon(Icons.Default.PlayArrow, null)
                        Text("Run with AI")
                    }
                    if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Text(
                            text = state.aiResponse,
                            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Select a prompt card to run")
                }
            }
        }
    }
}

@Composable
fun PromptCardItem(card: PromptCardEntity, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(card.promptText, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}
