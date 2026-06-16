package com.ahmedyejam.mks.di

import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineScope

/** Qualifier for the application-wide [CoroutineScope] that outlives ViewModels. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
