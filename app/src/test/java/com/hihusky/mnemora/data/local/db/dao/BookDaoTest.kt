package com.hihusky.mnemora.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hihusky.mnemora.data.local.db.dao.BookDao
import com.hihusky.mnemora.data.local.db.entity.BookEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BookDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BookDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.bookDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insert and get by id`() = runTest {
        val book = BookEntity(
            filename = "test_pkg",
            name = "Test Book",
            description = "A test book"
        )
        val id = dao.insert(book)

        val retrieved = dao.getById(id.toInt())
        assertNotNull(retrieved)
        assertEquals("test_pkg", retrieved!!.filename)
        assertEquals("Test Book", retrieved.name)
        assertEquals("A test book", retrieved.description)
    }

    @Test
    fun `getAllOnce returns all books`() = runTest {
        dao.insert(BookEntity(filename = "pkg1", name = "Book 1"))
        dao.insert(BookEntity(filename = "pkg2", name = "Book 2"))

        val books = dao.getAllOnce()
        assertEquals(2, books.size)
    }

    @Test
    fun `getAll flow emits books`() = runTest {
        dao.insert(BookEntity(filename = "pkg1", name = "Book 1"))
        dao.insert(BookEntity(filename = "pkg2", name = "Book 2"))

        val books = dao.getAll().first()
        assertEquals(2, books.size)
    }

    @Test
    fun `search finds books by name`() = runTest {
        dao.insert(BookEntity(filename = "pkg1", name = "Math 101"))
        dao.insert(BookEntity(filename = "pkg2", name = "History"))
        dao.insert(BookEntity(filename = "pkg3", name = "Advanced Math"))

        val results = dao.search("Math").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.name == "Math 101" })
        assertTrue(results.any { it.name == "Advanced Math" })
    }

    @Test
    fun `search finds books by filename`() = runTest {
        dao.insert(BookEntity(filename = "math-basics", name = "Basics"))
        dao.insert(BookEntity(filename = "history-world", name = "World History"))

        val results = dao.search("math").first()
        assertEquals(1, results.size)
        assertEquals("Basics", results.first().name)
    }

    @Test
    fun `insert replaces on conflict`() = runTest {
        val book = BookEntity(id = 1, filename = "pkg", name = "Original")
        dao.insert(book)

        val updated = BookEntity(id = 1, filename = "pkg", name = "Updated")
        dao.insert(updated)

        val retrieved = dao.getById(1)
        assertNotNull(retrieved)
        assertEquals("Updated", retrieved!!.name)
    }

    @Test
    fun `getById returns null for missing id`() = runTest {
        val result = dao.getById(999)
        assertNull(result)
    }

    @Test
    fun `deleteById removes book`() = runTest {
        val book = BookEntity(filename = "pkg", name = "To Delete")
        val id = dao.insert(book).toInt()

        dao.deleteById(id)

        val result = dao.getById(id)
        assertNull(result)
    }

    @Test
    fun `updateSortOrder changes sort order`() = runTest {
        dao.insert(BookEntity(filename = "pkg1", name = "Book 1", sortOrder = 0))
        dao.insert(BookEntity(filename = "pkg2", name = "Book 2", sortOrder = 1))

        dao.updateSortOrder(1, 10)

        val books = dao.getAllOnce()
        assertEquals(2, books.size)
    }
}
