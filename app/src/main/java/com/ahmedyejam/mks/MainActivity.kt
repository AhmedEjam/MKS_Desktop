package com.ahmedyejam.mks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.rememberNavController
import com.ahmedyejam.mks.data.error.GlobalErrorHandler
import com.ahmedyejam.mks.data.focus.FocusManager
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.ui.MksNavHost
import com.ahmedyejam.mks.ui.theme.MKSTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val dataStoreManager: DataStoreManager by inject()
    private val focusManager: FocusManager by inject()
    private val globalErrorHandler: GlobalErrorHandler by inject()

    private val _sharedUris = MutableStateFlow<List<Uri>?>(null)
    val sharedUris = _sharedUris.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        enableEdgeToEdge()

        setContent {
            val themeMode by dataStoreManager.themeMode.collectAsState(initial = "DAWN")
            val fontScale by dataStoreManager.fontScale.collectAsState(initial = 1.0f)
            val uiDensity by dataStoreManager.uiDensity.collectAsState(initial = 1.0f)
            val language by dataStoreManager.language.collectAsState(initial = "en")

            LaunchedEffect(language) {
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != language) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
                }
            }

            MKSTheme(
                themeMode = themeMode,
                fontScale = fontScale,
                uiDensity = uiDensity,
            ) {
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    globalErrorHandler.errorFlow.collect { error ->
                        snackbarHostState.showSnackbar(error.message)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    val showWelcomeOnStartup by dataStoreManager.showWelcomeOnStartup
                        .collectAsState(initial = true)
                    val sharedUrisByState by sharedUris.collectAsState()

                    MksNavHost(
                        navController = navController,
                        dataStoreManager = dataStoreManager,
                        showWelcomeOnStartup = showWelcomeOnStartup,
                        sharedUris = sharedUrisByState,
                    ) { _sharedUris.value = null }

                    SnackbarHost(hostState = snackbarHostState)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { uri ->
                    _sharedUris.value = listOf(uri)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { uris ->
                    _sharedUris.value = uris
                }
            }
        }
    }
}
