package com.hihusky.mnemora.domain.service

import com.hihusky.mnemora.data.local.db.entity.CollectionItemEntity
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.repository.CollectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionManagerTest {
    private val dbRepository = mockk<CollectionRepository>(relaxed = true)
    private val manager = CollectionManager(dbRepository)

    private fun createTestCollection(
        id: Int = 1,
        bookId: Int = 1,
        behavior: CollectionBehavior = CollectionBehavior.Manual,
    ): Collection =
        Collection(
            id = id,
            bookId = bookId,
            kind = CollectionKind.Custom,
            behavior = behavior,
            name = "Test Collection",
            description = null,
            sortOrder = 0,
            createdAt = 0L,
            updatedAt = 0L,
        )

    @Test
    fun `getAllCollections returns collections from repository`() =
        runTest {
            val expected = listOf(createTestCollection(1), createTestCollection(2))
            coEvery { dbRepository.getAllCollections() } returns expected

            val result = manager.getAllCollections()

            assertEquals(expected, result)
            coVerify { dbRepository.getAllCollections() }
        }

    @Test
    fun `getCollectionById returns collection when found`() =
        runTest {
            val expected = createTestCollection(1)
            coEvery { dbRepository.getCollectionById(1) } returns expected

            val result = manager.getCollectionById(1)

            assertNotNull(result)
            assertEquals(expected, result)
        }

    @Test
    fun `getCollectionById returns null when not found`() =
        runTest {
            coEvery { dbRepository.getCollectionById(999) } returns null

            val result = manager.getCollectionById(999)

            assertNull(result)
        }

    @Test
    fun `createCollection returns created collection`() =
        runTest {
            val created = createTestCollection(1)
            coEvery { dbRepository.insertCollection(any()) } returns 1L
            coEvery { dbRepository.getCollectionById(1) } returns created

            val result = manager.createCollection(1, "My Collection")

            assertNotNull(result)
            assertEquals(1, result?.id)
            coVerify { dbRepository.insertCollection(any()) }
        }

    @Test
    fun `deleteCollection returns true`() =
        runTest {
            coEvery { dbRepository.deleteCollection(1) } returns Unit

            val result = manager.deleteCollection(1)

            assertTrue(result)
            coVerify { dbRepository.deleteCollection(1) }
        }

    @Test
    fun `addToCollection returns false when collection not found`() =
        runTest {
            coEvery { dbRepository.getCollectionById(999) } returns null

            val result = manager.addToCollection(999, 1, 1)

            assertFalse(result)
        }

    @Test
    fun `addToCollection returns false for smart collection`() =
        runTest {
            val smartCollection = createTestCollection(1).copy(kind = CollectionKind.Smart)
            coEvery { dbRepository.getCollectionById(1) } returns smartCollection

            val result = manager.addToCollection(1, 1, 1)

            assertFalse(result)
        }

    @Test
    fun `addToCollection returns false when bookId does not match`() =
        runTest {
            val collection = createTestCollection(1, bookId = 2)
            coEvery { dbRepository.getCollectionById(1) } returns collection

            val result = manager.addToCollection(1, 1, 1)

            assertFalse(result)
        }

    @Test
    fun `addToCollection adds item and returns true`() =
        runTest {
            val collection = createTestCollection(1, bookId = 1)
            coEvery { dbRepository.getCollectionById(1) } returns collection
            coEvery { dbRepository.insertCollectionItem(any()) } returns 1L

            val result = manager.addToCollection(1, 1, 5)

            assertTrue(result)
            coVerify { dbRepository.insertCollectionItem(any<CollectionItemEntity>()) }
        }

    @Test
    fun `removeFromCollection delegates to repository`() =
        runTest {
            coEvery { dbRepository.deleteCollectionItem(1, 5) } returns Unit

            val result = manager.removeFromCollection(1, 5)

            assertTrue(result)
            coVerify { dbRepository.deleteCollectionItem(1, 5) }
        }

    @Test
    fun `getCollectionItems delegates to repository`() =
        runTest {
            coEvery { dbRepository.getCollectionItems(1) } returns emptyList()

            val result = manager.getCollectionItems(1)

            assertTrue(result.isEmpty())
            coVerify { dbRepository.getCollectionItems(1) }
        }

    @Test
    fun `getCollectionsForQuestion returns mapped collections`() =
        runTest {
            val collection = createTestCollection(1)
            coEvery { dbRepository.getCollectionIdsForQuestion(1, 1) } returns listOf(1)
            coEvery { dbRepository.getCollectionById(1) } returns collection

            val result = manager.getCollectionsForQuestion(1, 1)

            assertEquals(1, result.size)
            assertEquals(collection, result.first())
        }

    @Test
    fun `getCustomCollectionIdsForQuestion returns set of ids`() =
        runTest {
            coEvery { dbRepository.getCollectionIdsForQuestion(1, 1) } returns listOf(1, 2)
            coEvery { dbRepository.getCollectionById(1) } returns createTestCollection(1)
            coEvery { dbRepository.getCollectionById(2) } returns createTestCollection(2)

            val result = manager.getCustomCollectionIdsForQuestion(1, 1)

            assertEquals(setOf(1, 2), result)
        }
}
