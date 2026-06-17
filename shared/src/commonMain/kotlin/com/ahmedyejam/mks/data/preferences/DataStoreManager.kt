package com.ahmedyejam.mks.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreManager(private val dataStore: DataStore<Preferences>) {
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
        private val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
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

    val themeMode: Flow<String> = dataStore.data.map { SettingsSanitizer.theme(it[THEME_MODE]) }.distinctUntilChanged()
    val autoAdvanceDelay: Flow<Int> = dataStore.data.map { SettingsSanitizer.autoAdvanceDelay(it[AUTO_ADVANCE_DELAY]) }
    val lastSession: Flow<Pair<Long, Int>?> = dataStore.data.map { 
        val qid = it[LAST_QUIZ_ID]; val idx = it[LAST_QUESTION_INDEX]
        if (qid != null && qid > 0 && idx != null && idx >= 0) qid to idx else null
    }
    val unansweredSkipEnabled: Flow<Boolean> = dataStore.data.map { it[UNANSWERED_SKIP_ENABLED] ?: true }
    val defIncludeFilters: Flow<Set<String>> = dataStore.data.map { SettingsSanitizer.includeFilters(it[DEF_INCLUDE_FILTERS]) }
    val defShuffleQuestions: Flow<Boolean> = dataStore.data.map { it[DEF_SHUFFLE_QUESTIONS] ?: false }
    val defShuffleOptions: Flow<Boolean> = dataStore.data.map { it[DEF_SHUFFLE_OPTIONS] ?: false }
    val defRapidMode: Flow<Boolean> = dataStore.data.map { it[DEF_RAPID_MODE] ?: false }
    val defRepeatWrong: Flow<Boolean> = dataStore.data.map { it[DEF_REPEAT_WRONG] ?: false }
    val defQuizTimer: Flow<Int> = dataStore.data.map { SettingsSanitizer.quizTimerSeconds(it[DEF_QUIZ_TIMER]) }
    val defQuestionTimer: Flow<Int> = dataStore.data.map { SettingsSanitizer.questionTimerSeconds(it[DEF_QUESTION_TIMER]) }

    val showCategorization: Flow<Boolean> = dataStore.data.map { it[SHOW_CATEGORIZATION] ?: true }
    val oneByOneMode: Flow<Boolean> = dataStore.data.map { it[ONE_BY_ONE_MODE] ?: false }
    val eliminationModeEnabled: Flow<Boolean> = dataStore.data.map { it[ELIMINATION_MODE_ENABLED] ?: false }
    val doubleTapToSubmit: Flow<Boolean> = dataStore.data.map { it[DOUBLE_TAP_TO_SUBMIT] ?: true }
    val focusModeEnabled: Flow<Boolean> = dataStore.data.map { it[FOCUS_MODE_ENABLED] ?: false }

    val ollamaBaseUrl: Flow<String> = dataStore.data.map { it[OLLAMA_BASE_URL] ?: "http://localhost:11434" }
    val ollamaModelName: Flow<String> = dataStore.data.map { it[OLLAMA_MODEL_NAME] ?: "llama3" }
    val ollamaApiKey: Flow<String> = dataStore.data.map { it[OLLAMA_API_KEY] ?: "" }

    suspend fun setAutoAdvanceDelay(delayMs: Int) { dataStore.edit { it[AUTO_ADVANCE_DELAY] = SettingsSanitizer.autoAdvanceDelay(delayMs) } }
    suspend fun setShowCategorization(enabled: Boolean) { dataStore.edit { it[SHOW_CATEGORIZATION] = enabled } }
    suspend fun setOneByOneMode(enabled: Boolean) { dataStore.edit { it[ONE_BY_ONE_MODE] = enabled } }
    suspend fun setEliminationModeEnabled(enabled: Boolean) { dataStore.edit { it[ELIMINATION_MODE_ENABLED] = enabled } }
    suspend fun setDoubleTapToSubmit(enabled: Boolean) { dataStore.edit { it[DOUBLE_TAP_TO_SUBMIT] = enabled } }
    suspend fun setFocusModeEnabled(enabled: Boolean) { dataStore.edit { it[FOCUS_MODE_ENABLED] = enabled } }

    suspend fun saveSession(quizId: Long, questionIndex: Int) {
        dataStore.edit { it[LAST_QUIZ_ID] = quizId; it[LAST_QUESTION_INDEX] = questionIndex }
    }
    suspend fun clearSession() {
        dataStore.edit { it.remove(LAST_QUIZ_ID); it.remove(LAST_QUESTION_INDEX) }
    }
}
