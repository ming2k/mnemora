package com.hihusky.mnemora.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.BookEntity
import com.hihusky.mnemora.data.local.db.entity.NodeEntity
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
class NodeDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var nodeDao: NodeDao
    private lateinit var bookDao: BookDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        nodeDao = db.nodeDao()
        bookDao = db.bookDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun createTestBook(): Long {
        return bookDao.insert(BookEntity(filename = "test_pkg", name = "Test Book"))
    }

    @Test
    fun `insert and query by bookId`() = runTest {
        val bookId = createTestBook().toInt()

        nodeDao.insert(
            NodeEntity(
                id = "node_1",
                bookId = bookId,
                parentId = null,
                title = "Chapter 1",
                sortOrder = 0,
                depth = 0
            )
        )
        nodeDao.insert(
            NodeEntity(
                id = "node_2",
                bookId = bookId,
                parentId = null,
                title = "Chapter 2",
                sortOrder = 1,
                depth = 0
            )
        )

        val nodes = nodeDao.getByBookIdOnce(bookId)
        assertEquals(2, nodes.size)
        assertEquals("Chapter 1", nodes[0].title)
        assertEquals("Chapter 2", nodes[1].title)
    }

    @Test
    fun `flow emits nodes by bookId`() = runTest {
        val bookId = createTestBook().toInt()

        nodeDao.insert(
            NodeEntity(
                id = "node_1",
                bookId = bookId,
                title = "Node A",
                sortOrder = 0,
                depth = 0
            )
        )

        val nodes = nodeDao.getByBookId(bookId).first()
        assertEquals(1, nodes.size)
        assertEquals("Node A", nodes.first().title)
    }

    @Test
    fun `query by node id returns correct node`() = runTest {
        val bookId = createTestBook().toInt()

        nodeDao.insert(
            NodeEntity(
                id = "unique_node",
                bookId = bookId,
                title = "Unique",
                sortOrder = 0,
                depth = 0
            )
        )

        val node = nodeDao.getById("unique_node")
        assertNotNull(node)
        assertEquals("Unique", node!!.title)
    }

    @Test
    fun `getById returns null for missing node`() = runTest {
        val result = nodeDao.getById("nonexistent")
        assertNull(result)
    }

    @Test
    fun `parent-child hierarchy queries work`() = runTest {
        val bookId = createTestBook().toInt()

        nodeDao.insert(
            NodeEntity(
                id = "parent",
                bookId = bookId,
                parentId = null,
                title = "Parent",
                sortOrder = 0,
                depth = 0
            )
        )
        nodeDao.insert(
            NodeEntity(
                id = "child_1",
                bookId = bookId,
                parentId = "parent",
                title = "Child 1",
                sortOrder = 0,
                depth = 1
            )
        )
        nodeDao.insert(
            NodeEntity(
                id = "child_2",
                bookId = bookId,
                parentId = "parent",
                title = "Child 2",
                sortOrder = 1,
                depth = 1
            )
        )

        val roots = nodeDao.getRootsByBookId(bookId)
        assertEquals(1, roots.size)
        assertEquals("Parent", roots.first().title)

        val children = nodeDao.getChildren(bookId, "parent")
        assertEquals(2, children.size)
    }

    @Test
    fun `delete by bookId removes all nodes for that book`() = runTest {
        val bookId1 = createTestBook().toInt()
        val bookId2 = bookDao.insert(BookEntity(filename = "pkg2", name = "Book 2")).toInt()

        nodeDao.insert(NodeEntity(id = "n1", bookId = bookId1, title = "Node 1", sortOrder = 0, depth = 0))
        nodeDao.insert(NodeEntity(id = "n2", bookId = bookId1, title = "Node 2", sortOrder = 1, depth = 0))
        nodeDao.insert(NodeEntity(id = "n3", bookId = bookId2, title = "Node 3", sortOrder = 0, depth = 0))

        nodeDao.deleteByBookId(bookId1)

        val remainingBook1 = nodeDao.getByBookIdOnce(bookId1)
        val remainingBook2 = nodeDao.getByBookIdOnce(bookId2)

        assertTrue(remainingBook1.isEmpty())
        assertEquals(1, remainingBook2.size)
    }

    @Test
    fun `insertAll inserts multiple nodes`() = runTest {
        val bookId = createTestBook().toInt()

        val nodes = listOf(
            NodeEntity(id = "n1", bookId = bookId, title = "A", sortOrder = 0, depth = 0),
            NodeEntity(id = "n2", bookId = bookId, title = "B", sortOrder = 1, depth = 0),
            NodeEntity(id = "n3", bookId = bookId, title = "C", sortOrder = 2, depth = 0)
        )

        val ids = nodeDao.insertAll(nodes)
        assertEquals(3, ids.size)

        val all = nodeDao.getByBookIdOnce(bookId)
        assertEquals(3, all.size)
    }
}
