package com.ahmedyejam.mks.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.search.GlobalSearchResult
import com.ahmedyejam.mks.ui.components.EmptyStateCard
import com.ahmedyejam.mks.ui.components.LoadingErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    viewModel: GlobalSearchViewModel,
    onBack: () -> Unit,
    onOpenRoute: (Any) -> Unit
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Search") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search books, questions, notes, assets...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            LoadingErrorState(state.isLoading, state.error) { viewModel.search() }
            if (state.query.trim().length < 2) {
                EmptyStateCard("Search MKS", "Type at least two characters to search across books, quizzes, questions, assets, sources, flashcards, blueprints, prompts, and mistakes.")
            } else if (!state.isLoading && state.results.isEmpty() && state.error == null) {
                EmptyStateCard("No results", "No matching learning material was found.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.results, key = { it.type.name + it.id }) { result ->
                        SearchResultCard(result, onOpenRoute)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: GlobalSearchResult, onOpenRoute: (Any) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = result.route != null) { result.route?.let(onOpenRoute) }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(result.type.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(result.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            if (!result.subtitle!!.isNullOrBlank()) Text(result.subtitle!!, style = MaterialTheme.typography.bodySmall)
            if (!result.snippet!!.isNullOrBlank()) Text(result.snippet!!.take(180), style = MaterialTheme.typography.bodySmall)
        }
    }
}
