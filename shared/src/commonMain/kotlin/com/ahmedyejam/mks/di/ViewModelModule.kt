package com.ahmedyejam.mks.di

import com.ahmedyejam.mks.ui.importer.CompilerViewModel
import com.ahmedyejam.mks.ui.importer.ImportViewModel
import com.ahmedyejam.mks.ui.quiz.QuizViewModel
import com.ahmedyejam.mks.ui.review.ReviewDashboardViewModel
import com.ahmedyejam.mks.ui.search.GlobalSearchViewModel
import com.ahmedyejam.mks.ui.slideshow.SlideshowCourseViewModel
import com.ahmedyejam.mks.ui.flashcard.FlashcardDeckViewModel
import com.ahmedyejam.mks.ui.session.NoteCollectionViewModel
import com.ahmedyejam.mks.ui.session.PromptViewModel
import com.ahmedyejam.mks.ui.scanner.ScannerViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory { ImportViewModel(get(), get()) }
    factory { CompilerViewModel(get(), get(), get()) }
    factory { QuizViewModel(get(), get(), get(), get(), get(), get()) }
    factory { ReviewDashboardViewModel(get(), get()) }
    factory { GlobalSearchViewModel(get()) }
    factory { SlideshowCourseViewModel(get(), get(), get(), get(), get()) }
    factory { FlashcardDeckViewModel(get(), get(), get(), get(), get()) }
    factory { NoteCollectionViewModel(get()) }
    factory { PromptViewModel(get(), get(), get()) }
    factory { ScannerViewModel(get(), get()) }
}
