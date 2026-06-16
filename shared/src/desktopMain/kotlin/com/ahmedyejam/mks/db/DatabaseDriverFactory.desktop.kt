package com.ahmedyejam.mks.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dataDir = File(System.getProperty("user.home"), ".local/share/mks")
        dataDir.mkdirs()
        val dbFile = File(dataDir, "mks_database.db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        MksDatabase.Schema.create(driver)
        return driver
    }
}
