package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.NodeEntity
import com.hihusky.mnemora.data.model.Node
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        suspend fun getNodes(bookId: Int): List<Node> {
            val allNodes = db.nodeDao().getByBookIdOnce(bookId)
            return buildNodeTree(allNodes)
        }

        private fun buildNodeTree(nodes: List<NodeEntity>): List<Node> {
            val nodeMap = nodes.associateBy { it.id }
            val childrenMap = nodes.groupBy { it.parentId }

            fun build(nodeId: String): Node {
                val entity = nodeMap[nodeId]!!
                val children = childrenMap[nodeId]?.map { build(it.id) } ?: emptyList()
                return Node(
                    id = entity.id,
                    bookId = entity.bookId,
                    parentId = entity.parentId,
                    title = entity.title ?: "",
                    questionCount = entity.questionCount,
                    sortOrder = entity.sortOrder,
                    depth = entity.depth,
                    children = children,
                )
            }

            return nodes
                .filter { it.parentId == null }
                .sortedBy { it.sortOrder }
                .map { build(it.id) }
        }
    }
