package com.ahmedyejam.mks.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahmedyejam.mks.data.local.MksDatabase
import com.ahmedyejam.mks.data.local.entity.AssetReferenceEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetReferenceDaoTest {
    private lateinit var db: MksDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MksDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun countReferencesTracksSharedAssetOwners() =
        runBlocking {
            db.assetReferenceDao().insertReferences(
                listOf(
                    AssetReferenceEntity(path = "/data/user/0/app/files/images/shared.webp", ownerType = "question", ownerId = 1),
                    AssetReferenceEntity(path = "/data/user/0/app/files/images/shared.webp", ownerType = "flashcard", ownerId = 2),
                ),
            )

            assertEquals(2, db.assetReferenceDao().countReferencesForPath("/data/user/0/app/files/images/shared.webp"))

            db.assetReferenceDao().deleteReferencesForOwner("flashcard", 2)

            assertEquals(1, db.assetReferenceDao().countReferencesForPath("/data/user/0/app/files/images/shared.webp"))
        }
}
