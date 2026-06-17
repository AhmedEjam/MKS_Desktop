package com.ahmedyejam.mks.ui.flashcard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.ui.components.LoadingErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedFlashcardStudyScreen(
    viewModel: FlashcardDeckViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.deck?.title ?: "Flashcards") },
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
            
            LoadingErrorState(state.isLoading, state.error, { state.deck?.id?.let { viewModel.loadDeck(it) } })

            if (state.deck != null) {
                FlashcardContent(state, viewModel)
            }
        }
    }
}

@Composable
private fun FlashcardContent(
    state: FlashcardDeckUiState,
    viewModel: FlashcardDeckViewModel
) {
    val card = state.currentCard

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Card ${state.currentIndex + 1} of ${state.cards.size}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        if (card != null) {
            Card(
                modifier = Modifier.fillMaxWidth().height(300.dp).clickable { viewModel.flipCard() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isFlipped) card.backText else card.frontText,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("Tap to flip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))

            if (state.isFlipped) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.rateCurrentCard(FLASHCARD_RATING_AGAIN) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Again") }
                    Button(
                        onClick = { viewModel.rateCurrentCard(FLASHCARD_RATING_GOOD) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Good") }
                    Button(
                        onClick = { viewModel.rateCurrentCard(FLASHCARD_RATING_EASY) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) { Text("Easy") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { viewModel.previousCard() }, enabled = state.currentIndex > 0) {
                        Text("Previous")
                    }
                    Button(onClick = { viewModel.nextCard() }, enabled = state.currentIndex < state.cards.size - 1) {
                        Text("Next")
                    }
                }
            }
        } else {
            Text("No cards in this deck.")
        }
    }
}
