package com.ahmedyejam.mks.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific SqlDriver factory.
 * Android: AndroidSqliteDriver
 * Desktop: JdbcSqliteDriver
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
