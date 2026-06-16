package com.ahmedyejam.mks.ui.navigation

import androidx.navigation.NavBackStackEntry

/**
 * Validates navigation arguments to prevent loading with fallback IDs like 0L.
 */
object MksRouteValidator {
    fun requirePositiveLongArg(backStackEntry: NavBackStackEntry, key: String): Long? {
        val value = backStackEntry.arguments?.getLong(key) ?: 0L
        return if (value > 0) value else null
    }

    fun requireNonBlankStringArg(backStackEntry: NavBackStackEntry, key: String): String? {
        val value = backStackEntry.arguments?.getString(key)
        return if (!value.isNullOrBlank()) value else null
    }
}

fun NavBackStackEntry.requirePositiveLongArg(key: String): Long? = 
    MksRouteValidator.requirePositiveLongArg(this, key)

fun NavBackStackEntry.requireNonBlankStringArg(key: String): String? = 
    MksRouteValidator.requireNonBlankStringArg(this, key)
