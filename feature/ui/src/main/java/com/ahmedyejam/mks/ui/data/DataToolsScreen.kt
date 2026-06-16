package com.ahmedyejam.mks.ui.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.ui.components.LoadingErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataToolsScreen(
    viewModel: DataToolsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                viewModel.exportFullLibrary(stream)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import / Export") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
            Text("Full Library Export", style = MaterialTheme.typography.titleMedium)
            Text("Creates a Schema 7 complete ZIP backup of your library, including all embedded media and metadata.")
            Button(
                onClick = { 
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    exportLauncher.launch("mks_full_library_$timestamp.zip") 
                }, 
                enabled = !state.isWorking
            ) { 
                Text("Export full library") 
            }
            
            LoadingErrorState(state.isWorking, state.error)
            state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            
            Text("Importing", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text("To import a library backup, please use the '+' menu on the main Library Screen.")
        }
    }
}
