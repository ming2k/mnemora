package com.hihusky.mnemora.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hihusky.mnemora.data.local.db.AppDatabase
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
class BookImporterTest {

    private lateinit var db: AppDatabase
    private lateinit var importer: BookImporter

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        importer = BookImporter(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `importData skips if already imported`() = runTest {
        val data = mapOf<String, Any?>("name" to "Test Book")
        importer.importData(data, "test_123")
        importer.importData(data, "test_123")

        val books = db.bookDao().getAllOnce()
        assertEquals(1, books.size)
    }

    @Test
    fun `importData inserts book entity`() = runTest {
        val data = mapOf<String, Any?>(
            "name" to "Test Book",
            "description" to "A description",
            "icon" to "star"
        )

        val error = importer.importData(data, "test_book")
        assertNull(error)

        val books = db.bookDao().getAllOnce()
        assertEquals(1, books.size)
        with(books.first()) {
            assertEquals("test_book", filename)
            assertEquals("Test Book", name)
            assertEquals("A description", description)
            assertEquals("star", icon)
        }
    }

    @Test
    fun `importData inserts node entities`() = runTest {
        val data = mapOf<String, Any?>(
            "name" to "Book",
            "nodes" to listOf(
                mapOf(
                    "title" to "Chapter 1",
                    "questions" to listOf(
                        mapOf(
                            "content" to "What is 2+2?",
                            "answer" to "A",
                            "question_type" to "multiple_choice",
                            "choices" to listOf(
                                mapOf("key" to "A", "content" to "4"),
                                mapOf("key" to "B", "content" to "3")
                            )
                        )
                    )
                )
            )
        )

        val error = importer.importData(data, "test_nodes")
        assertNull(error)

        val nodes = db.nodeDao().getByBookIdOnce(1)
        assertEquals(1, nodes.size)
        assertEquals("Chapter 1", nodes.first().title)
    }

    @Test
    fun `importData inserts question entities`() = runTest {
        val data = mapOf<String, Any?>(
            "name" to "Book",
            "nodes" to listOf(
                mapOf(
                    "title" to "Chapter 1",
                    "questions" to listOf(
                        mapOf(
                            "content" to "What is 2+2?",
                            "answer" to "A",
                            "question_type" to "multiple_choice",
                            "choices" to listOf(
                                mapOf("key" to "A", "content" to "4"),
                                mapOf("key" to "B", "content" to "3")
                            )
                        ),
                        mapOf(
                            "content" to "Is the sky blue?",
                            "answer" to "true",
                            "question_type" to "true_false"
                        )
                    )
                )
            )
        )

        val error = importer.importData(data, "test_questions")
        assertNull(error)

        val questions = db.questionDao().getByBookId(1)
        assertEquals(2, questions.size)
        val contentSet = questions.map { it.content }.toSet()
        assertTrue(contentSet.contains("What is 2+2?"))
        assertTrue(contentSet.contains("Is the sky blue?"))
    }

    @Test
    fun `importData builds nested node hierarchy`() = runTest {
        val data = mapOf<String, Any?>(
            "name" to "Nested Book",
            "nodes" to listOf(
                mapOf(
                    "title" to "Parent",
                    "children" to listOf(
                        mapOf(
                            "title" to "Child",
                            "questions" to listOf(
                                mapOf(
                                    "content" to "Nested question",
                                    "answer" to "A",
                                    "question_type" to "multiple_choice"
                                )
                            )
                        )
                    )
                )
            )
        )

        val error = importer.importData(data, "test_nested")
        assertNull(error)

        val allNodes = db.nodeDao().getByBookIdOnce(1)
        assertEquals(2, allNodes.size)
        val parent = allNodes.find { it.title == "Parent" }
        val child = allNodes.find { it.title == "Child" }
        assertNotNull(parent)
        assertNotNull(child)
        assertEquals(parent!!.id, child!!.parentId)
        assertEquals(0, parent.depth)
        assertEquals(1, child.depth)
    }

    @Test
    fun `importData stores choices as JSON`() = runTest {
        val data = mapOf<String, Any?>(
            "name" to "Book",
            "nodes" to listOf(
                mapOf(
                    "title" to "Chapter 1",
                    "questions" to listOf(
                        mapOf(
                            "content" to "Pick one",
                            "answer" to "B",
                            "question_type" to "multiple_choice",
                            "choices" to listOf(
                                mapOf("key" to "A", "content" to "Option A"),
                                mapOf("key" to "B", "content" to "Option B"),
                                mapOf("key" to "C", "content" to "Option C")
                            )
                        )
                    )
                )
            )
        )

        val error = importer.importData(data, "test_choices")
        assertNull(error)

        val questions = db.questionDao().getByBookId(1)
        assertEquals(1, questions.size)
        assertNotNull(questions.first().choices)
        assertTrue(questions.first().choices!!.contains("Option B"))
    }

    @Test
    fun `importData sets total counts on book`() = runTest {
        val data = mapOf<String, Any?>(
            "name" to "Count Book",
            "nodes" to listOf(
                mapOf(
                    "title" to "Chapter 1",
                    "questions" to listOf(
                        mapOf("content" to "Q1", "answer" to "A", "question_type" to "multiple_choice"),
                        mapOf("content" to "Q2", "answer" to "B", "question_type" to "multiple_choice")
                    )
                ),
                mapOf(
                    "title" to "Chapter 2",
                    "questions" to listOf(
                        mapOf("content" to "Q3", "answer" to "C", "question_type" to "multiple_choice")
                    )
                )
            )
        )

        val error = importer.importData(data, "test_counts")
        assertNull(error)

        val book = db.bookDao().getAllOnce().first()
        assertEquals(3, book.totalQuestions)
        assertEquals(2, book.totalNodes)
    }

    @Test
    fun `importData reports progress`() = runTest {
        val progressValues = mutableListOf<Float>()
        val data = mapOf<String, Any?>(
            "name" to "Progress Book",
            "nodes" to listOf(
                mapOf(
                    "title" to "Node 1",
                    "questions" to listOf(
                        mapOf("content" to "Q1", "answer" to "A", "question_type" to "multiple_choice"),
                        mapOf("content" to "Q2", "answer" to "B", "question_type" to "multiple_choice")
                    )
                )
            )
        )

        importer.importData(data, "test_progress") { progress ->
            progressValues.add(progress)
        }

        assertTrue(progressValues.isNotEmpty())
        assertEquals(1.0f, progressValues.last(), 0.01f)
    }
}
