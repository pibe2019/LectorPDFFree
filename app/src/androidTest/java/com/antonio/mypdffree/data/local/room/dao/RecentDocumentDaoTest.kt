package com.antonio.mypdffree.data.local.room.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.antonio.mypdffree.data.local.room.database.AppDatabase
import com.antonio.mypdffree.data.local.room.entities.RecentDocumentEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** SQL real sobre SQLite en memoria: los @Query no se pueden verificar con mocks. */
@RunWith(AndroidJUnit4::class)
class RecentDocumentDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RecentDocumentDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        dao = database.recentDocumentDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeAll_ordena_del_mas_reciente_al_mas_antiguo() = runTest {
        dao.upsert(entity("a", openedAt = 100))
        dao.upsert(entity("b", openedAt = 300))
        dao.upsert(entity("c", openedAt = 200))

        dao.observeAll().test {
            assertEquals(listOf("b", "c", "a"), awaitItem().map { it.uri })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsert_del_mismo_uri_actualiza_sin_duplicar() = runTest {
        dao.upsert(entity("a", openedAt = 100, lastPage = 0))
        dao.upsert(entity("a", openedAt = 500, lastPage = 7))

        val stored = dao.getByUri("a")
        assertEquals(7, stored?.lastPageIndex)
        assertEquals(500L, stored?.lastOpenedAt)
        assertEquals(0, dao.trimTo(keep = 1).size)
    }

    @Test
    fun trimTo_borra_y_devuelve_solo_los_mas_antiguos() = runTest {
        (1..5).forEach { dao.upsert(entity("doc$it", openedAt = it * 100L)) }

        val removed = dao.trimTo(keep = 3)

        assertEquals(listOf("doc2", "doc1"), removed)
        assertNull(dao.getByUri("doc1"))
        assertEquals(3, dao.deleteAllReturningUris().size)
    }

    @Test
    fun updateLastPage_de_un_uri_inexistente_no_falla_ni_inserta() = runTest {
        assertEquals(0, dao.updateLastPage("no-existe", 4))
        assertNull(dao.getByUri("no-existe"))
    }

    private fun entity(uri: String, openedAt: Long, lastPage: Int = 0) = RecentDocumentEntity(
        uri = uri,
        displayName = "$uri.pdf",
        sizeBytes = null,
        pageCount = 10,
        lastPageIndex = lastPage,
        lastOpenedAt = openedAt,
    )
}
