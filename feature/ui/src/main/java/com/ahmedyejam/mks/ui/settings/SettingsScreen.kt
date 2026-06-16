package com.ahmedyejam.mks.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onBack: () -> Unit,
    onGlobalSearch: () -> Unit = {},
    onReviewDashboard: () -> Unit = {},
    onDataTools: () -> Unit = {}
) {
    val dataStoreManager = viewModel.dataStoreManager
    val focusManager = viewModel.focusManager
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tokens = LocalMksDesignTokens.current
    val snackbarHostState = remember { SnackbarHostState() }

    val skipUnanswered by dataStoreManager.unansweredSkipEnabled.collectAsState(initial = true)
    val doubleTapToSubmit by dataStoreManager.doubleTapToSubmit.collectAsState(initial = false)
    val showCovers by dataStoreManager.showCovers.collectAsState(initial = true)
    val autoAdvanceDelay by dataStoreManager.autoAdvanceDelay.collectAsState(initial = 2000)
    val currentLanguage by dataStoreManager.language.collectAsState(initial = "en")
    val showWelcomeOnStartup by dataStoreManager.showWelcomeOnStartup.collectAsState(initial = true)

    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showClearCategoriesDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { exportUri ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(exportUri)?.use { stream ->
                        viewModel.exportAllData(stream)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsGroup(title = stringResource(R.string.library_backup_group)) {
                Button(
                    onClick = {
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        exportLauncher.launch("mks_full_library_$timestamp.zip")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(tokens.chipRadius)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.export_full_library))
                }
                OutlinedButton(
                    onClick = onDataTools,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(tokens.chipRadius)
                ) {
                    Text("Advanced import/export preview")
                }
                OutlinedButton(
                    onClick = onGlobalSearch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(tokens.chipRadius)
                ) {
                    Text("Global search")
                }
                OutlinedButton(
                    onClick = onReviewDashboard,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(tokens.chipRadius)
                ) {
                    Text("Review dashboard")
                }
            }

            val themeOptions = listOf(
                ThemeOption("DAWN", R.string.theme_dawn, R.string.theme_dawn_desc),
                ThemeOption("FOREST", R.string.theme_forest, R.string.theme_forest_desc),
                ThemeOption("MIDNIGHT", R.string.theme_midnight, R.string.theme_midnight_desc),
                ThemeOption("LAVENDER", R.string.theme_lavender, R.string.theme_lavender_desc),
                ThemeOption("PLAIN_LIGHT", R.string.theme_plain_light, R.string.theme_plain_light_desc),
                ThemeOption("PLAIN_DARK", R.string.theme_plain_dark, R.string.theme_plain_dark_desc),
                ThemeOption("SYSTEM", R.string.theme_plain_system, R.string.theme_plain_system_desc)
            )
            val currentThemeMode by dataStoreManager.themeMode.collectAsState(initial = "DAWN")
            val savedFontScale by dataStoreManager.fontScale.collectAsState(initial = 1.0f)
            val savedUiDensity by dataStoreManager.uiDensity.collectAsState(initial = 1.0f)

            var fontScale by remember(savedFontScale) { mutableStateOf(savedFontScale) }
            var uiDensity by remember(savedUiDensity) { mutableStateOf(savedUiDensity) }

            SettingsGroup(
                title = stringResource(R.string.appearance_group),
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            dataStoreManager.setThemeMode("DAWN")
                            dataStoreManager.setFontScale(1.0f)
                            dataStoreManager.setUiDensity(1.0f)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.reset))
                    }
                }
            ) {
                Text(stringResource(R.string.theme_label), style = MaterialTheme.typography.labelLarge)
                
                var themeExpanded by remember { mutableStateOf(false) }
                val currentThemeOption = themeOptions.find { it.key == currentThemeMode } ?: themeOptions.last()

                ExposedDropdownMenuBox(
                    expanded = themeExpanded,
                    onExpandedChange = { themeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = stringResource(currentThemeOption.labelRes),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.theme_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        themeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(stringResource(option.labelRes))
                                        Text(
                                            text = stringResource(option.descriptionRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    scope.launch { dataStoreManager.setThemeMode(option.key) }
                                    themeExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.font_scale_format, fontScale), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = fontScale,
                        onValueChange = { fontScale = it },
                        onValueChangeFinished = { scope.launch { dataStoreManager.setFontScale(fontScale) } },
                        valueRange = 0.5f..2.0f,
                        steps = 14
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ui_density_format, uiDensity), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = uiDensity,
                        onValueChange = { uiDensity = it },
                        onValueChangeFinished = { scope.launch { dataStoreManager.setUiDensity(uiDensity) } },
                        valueRange = 0.5f..1.5f,
                        steps = 10
                    )
                }

                SettingsToggle(
                    title = stringResource(R.string.show_cover_images),
                    description = stringResource(R.string.show_cover_images_desc),
                    checked = showCovers,
                    onCheckedChange = { scope.launch { dataStoreManager.setShowCovers(it) } }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(stringResource(R.string.language_label), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentLanguage == "en",
                        onClick = { scope.launch { dataStoreManager.setLanguage("en") } },
                        label = { Text(stringResource(R.string.language_en)) }
                    )
                    FilterChip(
                        selected = currentLanguage == "ar",
                        onClick = { scope.launch { dataStoreManager.setLanguage("ar") } },
                        label = { Text(stringResource(R.string.language_ar)) }
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.global_config_group)) {
                Text(
                    stringResource(R.string.global_config_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SettingsToggle(
                    title = stringResource(R.string.show_welcome_screen),
                    description = stringResource(R.string.show_welcome_screen_desc),
                    checked = showWelcomeOnStartup,
                    onCheckedChange = { scope.launch { dataStoreManager.setShowWelcomeOnStartup(it) } }
                )

                val focusModeEnabled by dataStoreManager.focusModeEnabled.collectAsState(initial = false)
                var hasFocusPermission by remember { mutableStateOf(focusManager.hasNotificationPolicyAccess()) }

                SettingsToggle(
                    title = stringResource(R.string.focus_mode_label),
                    description = stringResource(R.string.focus_mode_desc),
                    checked = focusModeEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !focusManager.hasNotificationPolicyAccess()) {
                            focusManager.requestNotificationPolicyAccess()
                            hasFocusPermission = focusManager.hasNotificationPolicyAccess()
                        } else {
                            scope.launch { dataStoreManager.setFocusModeEnabled(enabled) }
                        }
                    }
                )

                if (focusModeEnabled && !hasFocusPermission) {
                    TextButton(
                        onClick = {
                            focusManager.requestNotificationPolicyAccess()
                            hasFocusPermission = focusManager.hasNotificationPolicyAccess()
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.grant_permission), style = MaterialTheme.typography.labelMedium)
                    }
                }

                SettingsToggle(
                    title = stringResource(R.string.skip_answered_label),
                    description = stringResource(R.string.skip_answered_desc),
                    checked = skipUnanswered,
                    onCheckedChange = { scope.launch { dataStoreManager.setUnansweredSkipEnabled(it) } }
                )

                SettingsToggle(
                    title = stringResource(R.string.double_tap_submit_label),
                    description = stringResource(R.string.double_tap_submit_desc),
                    checked = doubleTapToSubmit,
                    onCheckedChange = { scope.launch { dataStoreManager.setDoubleTapToSubmit(it) } }
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.rapid_advance_delay_format, autoAdvanceDelay), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = autoAdvanceDelay.toFloat(),
                        onValueChange = { scope.launch { dataStoreManager.setAutoAdvanceDelay(it.toInt()) } },
                        valueRange = 500f..5000f,
                        steps = 8
                    )
                }
            }

            val ollamaBaseUrl by dataStoreManager.ollamaBaseUrl.collectAsState(initial = "http://10.0.2.2:11434")
            val ollamaModelName by dataStoreManager.ollamaModelName.collectAsState(initial = "llama3")
            val ollamaApiKey by dataStoreManager.ollamaApiKey.collectAsState(initial = "")
            var editOllamaBaseUrl by remember(ollamaBaseUrl) { mutableStateOf(ollamaBaseUrl) }
            var editOllamaModelName by remember(ollamaModelName) { mutableStateOf(ollamaModelName) }
            var editOllamaApiKey by remember(ollamaApiKey) { mutableStateOf(ollamaApiKey) }
            var isTestingOllama by remember { mutableStateOf(false) }

            SettingsGroup(title = "AI Integrations") {
                Text(
                    "Configure local LLM settings for the AI Prompt Deck. Ollama is recommended. For Android Emulators, use http://10.0.2.2:11434. A remote server can be reached via its http(s) URL; add an API key if it is protected by a reverse proxy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = editOllamaBaseUrl,
                    onValueChange = { editOllamaBaseUrl = it },
                    label = { Text("Ollama Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editOllamaModelName,
                    onValueChange = { editOllamaModelName = it },
                    label = { Text("Ollama Model Name (e.g. llama3)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editOllamaApiKey,
                    onValueChange = { editOllamaApiKey = it },
                    label = { Text("API Key (optional, for secured servers)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        enabled = !isTestingOllama,
                        onClick = {
                            scope.launch {
                                isTestingOllama = true
                                val result = viewModel.ollamaRepository.testConnection(
                                    baseUrl = editOllamaBaseUrl,
                                    modelName = editOllamaModelName,
                                    apiKey = editOllamaApiKey.takeIf { it.isNotBlank() }
                                )
                                isTestingOllama = false
                                snackbarHostState.showSnackbar(result.message)
                            }
                        }
                    ) {
                        Text(if (isTestingOllama) "Testing..." else "Test Connection")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                dataStoreManager.setOllamaBaseUrl(editOllamaBaseUrl)
                                dataStoreManager.setOllamaModelName(editOllamaModelName)
                                dataStoreManager.setOllamaApiKey(editOllamaApiKey)
                                snackbarHostState.showSnackbar("AI settings saved")
                            }
                        }
                    ) {
                        Text("Save Settings")
                    }
                }
            }

            SettingsGroup(title = stringResource(R.string.danger_zone_group), titleColor = MaterialTheme.colorScheme.error) {
                OutlinedButton(
                    onClick = { showClearCategoriesDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(stringResource(R.string.clear_categories))
                    }
                }

                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reset_database))
                }
            }
        }
    }

    if (showClearCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showClearCategoriesDialog = false },
            title = { Text(stringResource(R.string.clear_categories_title)) },
            text = { Text(stringResource(R.string.clear_categories_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCategoriesDialog = false
                        viewModel.clearCategories()
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.clear_categories_success))
                        }
                    }
                ) {
                    Text(stringResource(R.string.clear_categories), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCategoriesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_database_title)) },
            text = { Text(stringResource(R.string.reset_database_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetDatabase()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private data class ThemeOption(
    val key: String,
    val labelRes: Int,
    val descriptionRes: Int
)

@Composable
fun SettingsGroup(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    actions: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalMksDesignTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
            actions?.invoke(this)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(tokens.cardRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
