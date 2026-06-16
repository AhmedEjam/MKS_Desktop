package com.ahmedyejam.mks.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mks_settings")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val AUTO_ADVANCE_DELAY = intPreferencesKey("auto_advance_delay")
        private val LAST_QUIZ_ID = longPreferencesKey("last_quiz_id")
        private val LAST_QUESTION_INDEX = intPreferencesKey("last_question_index")
        private val UNANSWERED_SKIP_ENABLED = booleanPreferencesKey("unanswered_skip_enabled")

        private val DEF_INCLUDE_FILTERS = stringSetPreferencesKey("def_include_filters")
        private val DEF_SHUFFLE_QUESTIONS = booleanPreferencesKey("def_shuffle_questions")
        private val DEF_SHUFFLE_OPTIONS = booleanPreferencesKey("def_shuffle_options")
        private val DEF_RAPID_MODE = booleanPreferencesKey("def_rapid_mode")
        private val DEF_REPEAT_WRONG = booleanPreferencesKey("def_repeat_wrong")
        private val DEF_QUIZ_TIMER = intPreferencesKey("def_quiz_timer")
        private val DEF_QUESTION_TIMER = intPreferencesKey("def_question_timer")

        private val LIBRARY_SORT_OPTION = stringPreferencesKey("library_sort_option")
        private val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode") // "GRID" or "LIST"
        private val BOOK_SORT_OPTION = stringPreferencesKey("book_sort_option")
        private val BOOK_VIEW_MODE = stringPreferencesKey("book_view_mode")
        private val LAST_EXCEL_MAPPING = stringPreferencesKey("last_excel_mapping")
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        private val UI_DENSITY = floatPreferencesKey("ui_density")

        private val SHOW_CATEGORIZATION = booleanPreferencesKey("show_categorization")
        private val ONE_BY_ONE_MODE = booleanPreferencesKey("one_by_one_mode")
        private val ELIMINATION_MODE_ENABLED = booleanPreferencesKey("elimination_mode_enabled")
        private val SHOW_COVERS = booleanPreferencesKey("show_covers")
        private val DOUBLE_TAP_TO_SUBMIT = booleanPreferencesKey("double_tap_to_submit")
        private val FOCUS_MODE_ENABLED = booleanPreferencesKey("focus_mode_enabled")

        private val LANGUAGE = stringPreferencesKey("language")
        private val SHOW_WELCOME_ON_STARTUP = booleanPreferencesKey("show_welcome_on_startup")
        private val CURRENT_WORKSPACE_ID = longPreferencesKey("current_workspace_id")

        private val OLLAMA_BASE_URL = stringPreferencesKey("ollama_base_url")
        private val OLLAMA_MODEL_NAME = stringPreferencesKey("ollama_model_name")
        private val OLLAMA_API_KEY = stringPreferencesKey("ollama_api_key")
    }

    val themeMode: Flow<String> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.theme(preferences[THEME_MODE])
        }.distinctUntilChanged()

    val fontScale: Flow<Float> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.fontScale(preferences[FONT_SCALE])
        }.distinctUntilChanged()

    val uiDensity: Flow<Float> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.uiDensity(preferences[UI_DENSITY])
        }.distinctUntilChanged()

    val autoAdvanceDelay: Flow<Int> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.autoAdvanceDelay(preferences[AUTO_ADVANCE_DELAY])
        }

    val lastSession: Flow<Pair<Long, Int>?> =
        context.dataStore.data.map { preferences ->
            val quizId = preferences[LAST_QUIZ_ID]
            val questionIndex = preferences[LAST_QUESTION_INDEX]
            if (quizId != null && quizId > 0 && questionIndex != null && questionIndex >= 0) {
                quizId to questionIndex
            } else {
                null
            }
        }

    val unansweredSkipEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[UNANSWERED_SKIP_ENABLED] ?: true
        }

    val defIncludeFilters: Flow<Set<String>> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.includeFilters(preferences[DEF_INCLUDE_FILTERS])
        }

    val defShuffleQuestions: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[DEF_SHUFFLE_QUESTIONS] ?: false
        }

    val defShuffleOptions: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[DEF_SHUFFLE_OPTIONS] ?: false
        }

    val defRapidMode: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[DEF_RAPID_MODE] ?: false
        }

    val defRepeatWrong: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[DEF_REPEAT_WRONG] ?: false
        }

    val defQuizTimer: Flow<Int> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.quizTimerSeconds(preferences[DEF_QUIZ_TIMER])
        }

    val defQuestionTimer: Flow<Int> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.questionTimerSeconds(preferences[DEF_QUESTION_TIMER])
        }

    val librarySortOption: Flow<String> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.sortOption(preferences[LIBRARY_SORT_OPTION])
        }

    val libraryViewMode: Flow<String> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.viewMode(preferences[LIBRARY_VIEW_MODE])
        }

    val bookSortOption: Flow<String> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.sortOption(preferences[BOOK_SORT_OPTION])
        }

    val bookViewMode: Flow<String> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.viewMode(preferences[BOOK_VIEW_MODE])
        }

    val lastExcelMapping: Flow<String?> =
        context.dataStore.data.map { preferences ->
            preferences[LAST_EXCEL_MAPPING]
        }

    val showCategorization: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SHOW_CATEGORIZATION] ?: true
        }

    val oneByOneMode: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ONE_BY_ONE_MODE] ?: false
        }

    val eliminationModeEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ELIMINATION_MODE_ENABLED] ?: false
        }

    val showCovers: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SHOW_COVERS] ?: true
        }

    val doubleTapToSubmit: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[DOUBLE_TAP_TO_SUBMIT] ?: true
        }

    val focusModeEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[FOCUS_MODE_ENABLED] ?: false
        }

    val language: Flow<String> =
        context.dataStore.data.map { preferences ->
            SettingsSanitizer.language(preferences[LANGUAGE])
        }.distinctUntilChanged()

    val showWelcomeOnStartup: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SHOW_WELCOME_ON_STARTUP] ?: true
        }.distinctUntilChanged()

    val currentWorkspaceId: Flow<Long?> =
        context.dataStore.data.map { preferences ->
            preferences[CURRENT_WORKSPACE_ID]?.takeIf { it > 0L }
        }.distinctUntilChanged()

    val ollamaBaseUrl: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[OLLAMA_BASE_URL] ?: "http://10.0.2.2:11434"
        }.distinctUntilChanged()

    val ollamaModelName: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[OLLAMA_MODEL_NAME] ?: "llama3"
        }.distinctUntilChanged()

    val ollamaApiKey: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[OLLAMA_API_KEY] ?: ""
        }.distinctUntilChanged()

    suspend fun setLibrarySortOption(option: String) {
        context.dataStore.edit { preferences -> preferences[LIBRARY_SORT_OPTION] = SettingsSanitizer.sortOption(option) }
    }

    suspend fun setLibraryViewMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[LIBRARY_VIEW_MODE] = SettingsSanitizer.viewMode(mode) }
    }

    suspend fun setBookSortOption(option: String) {
        context.dataStore.edit { preferences -> preferences[BOOK_SORT_OPTION] = SettingsSanitizer.sortOption(option) }
    }

    suspend fun setBookViewMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[BOOK_VIEW_MODE] = SettingsSanitizer.viewMode(mode) }
    }

    suspend fun setLastExcelMapping(mappingJson: String) {
        context.dataStore.edit { preferences -> preferences[LAST_EXCEL_MAPPING] = mappingJson }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = SettingsSanitizer.theme(mode)
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SCALE] = SettingsSanitizer.fontScale(scale)
        }
    }

    suspend fun setUiDensity(density: Float) {
        context.dataStore.edit { preferences ->
            preferences[UI_DENSITY] = SettingsSanitizer.uiDensity(density)
        }
    }

    suspend fun setUnansweredSkipEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[UNANSWERED_SKIP_ENABLED] = enabled
        }
    }

    suspend fun setAutoAdvanceDelay(delayMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_ADVANCE_DELAY] = SettingsSanitizer.autoAdvanceDelay(delayMs)
        }
    }

    suspend fun setShowCategorization(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_CATEGORIZATION] = enabled
        }
    }

    suspend fun setOneByOneMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONE_BY_ONE_MODE] = enabled
        }
    }

    suspend fun setEliminationModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ELIMINATION_MODE_ENABLED] = enabled
        }
    }

    suspend fun setShowCovers(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_COVERS] = enabled
        }
    }

    suspend fun setDoubleTapToSubmit(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_TAP_TO_SUBMIT] = enabled
        }
    }

    suspend fun setFocusModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_MODE_ENABLED] = enabled
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = SettingsSanitizer.language(lang)
        }
    }

    suspend fun setShowWelcomeOnStartup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_WELCOME_ON_STARTUP] = enabled
        }
    }

    suspend fun setCurrentWorkspaceId(workspaceId: Long) {
        context.dataStore.edit { preferences ->
            if (workspaceId > 0L) {
                preferences[CURRENT_WORKSPACE_ID] = workspaceId
            } else {
                preferences.remove(CURRENT_WORKSPACE_ID)
            }
        }
    }

    suspend fun setOllamaBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[OLLAMA_BASE_URL] = url
        }
    }

    suspend fun setOllamaModelName(model: String) {
        context.dataStore.edit { preferences ->
            preferences[OLLAMA_MODEL_NAME] = model
        }
    }

    suspend fun setOllamaApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[OLLAMA_API_KEY] = apiKey
        }
    }

    suspend fun saveDefaultSessionSettings(
        filters: Set<String>,
        shuffleQ: Boolean,
        shuffleO: Boolean,
        rapid: Boolean,
        repeatWrong: Boolean,
        quizTimer: Int,
        qTimer: Int,
    ) {
        context.dataStore.edit { preferences ->
            preferences[DEF_INCLUDE_FILTERS] = filters
            preferences[DEF_SHUFFLE_QUESTIONS] = shuffleQ
            preferences[DEF_SHUFFLE_OPTIONS] = shuffleO
            preferences[DEF_RAPID_MODE] = rapid
            preferences[DEF_REPEAT_WRONG] = repeatWrong
            preferences[DEF_QUIZ_TIMER] = quizTimer
            preferences[DEF_QUESTION_TIMER] = qTimer
        }
    }

    suspend fun saveSession(
        quizId: Long,
        questionIndex: Int,
    ) {
        context.dataStore.edit { preferences ->
            preferences[LAST_QUIZ_ID] = quizId
            preferences[LAST_QUESTION_INDEX] = questionIndex
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(LAST_QUIZ_ID)
            preferences.remove(LAST_QUESTION_INDEX)
        }
    }
}
