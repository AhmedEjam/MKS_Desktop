package com.ahmedyejam.mks.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TrashUiState(
    val deletedBooks: List<BookEntity> = emptyList(),
    val deletedQuizzes: List<QuizEntity> = emptyList(),
    val deletedDecks: List<FlashcardDeckEntity> = emptyList(),
    val deletedSlideshows: List<SlideshowCourseEntity> = emptyList(),
    val deletedNotes: List<NoteBlueprintEntity> = emptyList(),
    val deletedPrompts: List<PromptDeckEntity> = emptyList(),
    val isLoading: Boolean = false
)

class TrashViewModel(
    private val bookRepository: BookRepository,
    private val quizRepository: QuizRepository,
    private val knowledgeRepository: KnowledgeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrashUiState())
    val state = _state.asStateFlow()

    fun loadTrash(workspaceId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val booksFlow = bookRepository.getDeletedBooks(workspaceId)
            val quizzesFlow = quizRepository.getDeletedQuizzes(workspaceId)
            val decksFlow = knowledgeRepository.getDeletedFlashcardDecks(workspaceId)
            val slidesFlow = knowledgeRepository.getDeletedSlideshowCourses(workspaceId)
            val notesFlow = knowledgeRepository.getDeletedNoteBlueprints(workspaceId)
            val promptsFlow = knowledgeRepository.getDeletedPromptDecks(workspaceId)

            combine(listOf(booksFlow, quizzesFlow, decksFlow, slidesFlow, notesFlow, promptsFlow)) { array ->
                @Suppress("UNCHECKED_CAST")
                TrashUiState(
                    deletedBooks = array[0] as List<BookEntity>,
                    deletedQuizzes = array[1] as List<QuizEntity>,
                    deletedDecks = array[2] as List<FlashcardDeckEntity>,
                    deletedSlideshows = array[3] as List<SlideshowCourseEntity>,
                    deletedNotes = array[4] as List<NoteBlueprintEntity>,
                    deletedPrompts = array[5] as List<PromptDeckEntity>,
                    isLoading = false
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun restoreBook(id: Long) { viewModelScope.launch { bookRepository.restoreBook(id) } }
    fun permanentlyDeleteBook(id: Long) { viewModelScope.launch { bookRepository.permanentlyDeleteBook(id) } }
    
    fun restoreQuiz(id: Long) { viewModelScope.launch { quizRepository.restoreQuiz(id) } }
    fun permanentlyDeleteQuiz(id: Long) { viewModelScope.launch { quizRepository.permanentlyDeleteQuiz(id) } }

    fun restoreDeck(id: Long) { viewModelScope.launch { knowledgeRepository.restoreFlashcardDeck(id) } }
    fun restoreSlideshow(id: Long) { viewModelScope.launch { knowledgeRepository.restoreSlideshowCourse(id) } }
    fun restoreNote(id: Long) { viewModelScope.launch { knowledgeRepository.restoreNoteBlueprint(id) } }
    fun restorePrompt(id: Long) { viewModelScope.launch { knowledgeRepository.restorePromptDeck(id) } }
}
