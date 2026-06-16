package com.ahmedyejam.mks.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File

fun createDesktopDataStore(): DataStore<Preferences> {
    val dataRoot = File(System.getProperty("user.home"), ".local/share/mks").also { it.mkdirs() }
    return PreferenceDataStoreFactory.create(
        produceFile = { File(dataRoot, "mks_settings.preferences_pb") }
    )
}
