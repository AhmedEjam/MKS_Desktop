package com.ahmedyejam.mks.di

import com.ahmedyejam.mks.ui.importer.CompilerViewModel
import com.ahmedyejam.mks.ui.importer.ImportViewModel
import com.ahmedyejam.mks.ui.quiz.QuizViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory { ImportViewModel(get(), get()) }
    factory { CompilerViewModel(get(), get(), get(), get()) }
    factory { QuizViewModel(get(), get(), get(), get(), get(), get()) }
}
