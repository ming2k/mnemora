package com.hihusky.mnemora.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.BookEntity
import com.hihusky.mnemora.data.local.db.entity.NodeEntity
import com.hihusky.mnemora.data.local.db.entity.QuestionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class QuestionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var questionDao: QuestionDao
    private lateinit var bookDao: BookDao
    private lateinit var nodeDao: NodeDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        questionDao = db.questionDao()
        bookDao = db.bookDao()
        nodeDao = db.nodeDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun createTestData(bookId: Int, vararg nodeIds: String) {
        bookDao.insert(BookEntity(id = bookId, filename = "test_pkg", name = "Test Book"))
        for (nodeId in nodeIds) {
            nodeDao.insert(
                NodeEntity(
                    id = nodeId,
                    bookId = bookId,
                    title = nodeId,
                    sortOrder = 0,
                    depth = 0
                )
            )
        }
    }

    @Test
    fun `insert and query by bookId`() = runTest {
        createTestData(1, "node_1", "node_2")

        questionDao.insert(
            QuestionEntity(
                bookId = 1,
                nodeId = "node_1",
                content = "What is 2+2?",
                answer = "A",
                questionType = "multiple_choice"
            )
        )
        questionDao.insert(
            QuestionEntity(
                bookId = 1,
                nodeId = "node_2",
                content = "What is 2+3?",
                answer = "B",
                questionType = "multiple_choice"
            )
        )

        val questions = questionDao.getByBookId(1)
        assertEquals(2, questions.size)
    }

    @Test
    fun `query by nodeId returns questions for that node`() = runTest {
        createTestData(1, "node_a", "node_b")

        questionDao.insert(
            QuestionEntity(
                bookId = 1,
                nodeId = "node_a",
                content = "Question in A",
                answer = "A",
                questionType = "multiple_choice"
            )
        )
        questionDao.insert(
            QuestionEntity(
                bookId = 1,
                nodeId = "node_b",
                content = "Question in B",
                answer = "B",
                questionType = "multiple_choice"
            )
        )

        val nodeAQuestions = questionDao.getByNodeId("node_a")
        assertEquals(1, nodeAQuestions.size)
        assertEquals("Question in A", nodeAQuestions.first().content)
    }

    @Test
    fun `query by ids returns multiple questions`() = runTest {
        createTestData(1, "node_1")

        val id1 = questionDao.insert(
            QuestionEntity(bookId = 1, nodeId = "node_1", content = "Q1", answer = "A", questionType = "multiple_choice")
        ).toInt()
        val id2 = questionDao.insert(
            QuestionEntity(bookId = 1, nodeId = "node_1", content = "Q2", answer = "B", questionType = "multiple_choice")
        ).toInt()

        val results = questionDao.getByIds(listOf(id1, id2))
        assertEquals(2, results.size)
    }

    @Test
    fun `getAnswerableQuestionIds excludes passage type`() = runTest {
        createTestData(1, "node_1", "node_2")

        questionDao.insert(
            QuestionEntity(bookId = 1, nodeId = "node_1", content = "Q1", answer = "A", questionType = "multiple_choice")
        )
        questionDao.insert(
            QuestionEntity(bookId = 1, nodeId = "node_2", content = "Passage 1", answer = "", questionType = "passage")
        )

        val answerableIds = questionDao.getAnswerableQuestionIds(1)
        assertEquals(1, answerableIds.size)
    }

    @Test
    fun `deleteByBookId removes all questions for book`() = runTest {
        createTestData(1, "node_1")
        bookDao.insert(BookEntity(id = 2, filename = "pkg2", name = "Book 2"))
        nodeDao.insert(NodeEntity(id = "node_2", bookId = 2, title = "n", sortOrder = 0, depth = 0))

        questionDao.insert(
            QuestionEntity(bookId = 1, nodeId = "node_1", content = "Q1", answer = "A", questionType = "multiple_choice")
        )
        questionDao.insert(
            QuestionEntity(bookId = 2, nodeId = "node_2", content = "Q2", answer = "B", questionType = "multiple_choice")
        )

        questionDao.deleteByBookId(1)

        val book1Questions = questionDao.getByBookId(1)
        val book2Questions = questionDao.getByBookId(2)

        assertTrue(book1Questions.isEmpty())
        assertEquals(1, book2Questions.size)
    }
}
