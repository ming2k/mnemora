package com.hihusky.mnemora.domain.service

import androidx.room.withTransaction
import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.BookEntity
import com.hihusky.mnemora.data.local.db.entity.NodeEntity
import com.hihusky.mnemora.data.local.db.entity.QuestionEntity
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookImporter
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun importData(
            data: Map<String, Any?>,
            packageId: String,
            onProgress: ((Float) -> Unit)? = null,
        ): String? {
            return try {
                val existing = db.bookDao().getAllOnce().find { it.filename == packageId }
                if (existing != null) {
                    return null // Already imported, skip silently
                }

                val bookName = data["name"] as? String ?: "Imported"
                val nodesData = data["nodes"] as? List<*>
                val totalWork = (countImportWork(nodesData) + 2).coerceAtLeast(1)
                var completedWork = 0

                fun reportProgress() {
                    completedWork++
                    onProgress?.invoke((completedWork.toFloat() / totalWork).coerceIn(0f, 1f))
                }

                db.withTransaction {
                    val bookEntity =
                        BookEntity(
                            filename = packageId,
                            name = bookName,
                            description = data["description"] as? String,
                            icon = data["icon"] as? String,
                        )
                    val bookId = db.bookDao().insert(bookEntity).toInt()
                    reportProgress()

                    var totalQuestions = 0
                    var totalNodes = 0

                    suspend fun importNodes(
                        nodes: List<*>,
                        parentId: String?,
                        depth: Int,
                    ) {
                        if (depth > MAX_NODE_DEPTH) {
                            throw IllegalStateException(
                                "Maximum node depth ($MAX_NODE_DEPTH) exceeded. " +
                                    "The schema allows arbitrary nesting, but the app limits depth to prevent UI degradation.",
                            )
                        }

                        nodes.forEachIndexed { index, node ->
                            if (node !is Map<*, *>) return@forEachIndexed
                            val nodeMap = node.mapKeys { it.key.toString() }.mapValues { it.value }

                            val nodeId =
                                if (parentId == null) {
                                    "${packageId}_n${depth}_$index"
                                } else {
                                    "${parentId}_$index"
                                }
                            totalNodes++

                            db.nodeDao().insert(
                                NodeEntity(
                                    id = nodeId,
                                    bookId = bookId,
                                    parentId = parentId,
                                    title = nodeMap["title"] as? String,
                                    sortOrder = index,
                                    depth = depth,
                                ),
                            )
                            reportProgress()

                            val questionsData = nodeMap["questions"] as? List<*>
                            questionsData?.forEach { q ->
                                if (q !is Map<*, *>) return@forEach
                                val qMap = q.mapKeys { it.key.toString() }.mapValues { it.value }.toMutableMap()
                                val subQuestionsData = qMap.remove("sub_questions") as? List<*>

                                val parentEntity = parseQuestionEntity(qMap, bookId, nodeId)
                                val parentQuestionId = db.questionDao().insert(parentEntity).toInt()
                                totalQuestions++
                                reportProgress()

                                subQuestionsData?.forEach { sq ->
                                    if (sq !is Map<*, *>) return@forEach
                                    totalQuestions++
                                    val sqMap = sq.mapKeys { it.key.toString() }.mapValues { it.value }.toMutableMap()
                                    val childEntity =
                                        parseQuestionEntity(
                                            sqMap,
                                            bookId,
                                            nodeId,
                                        ).copy(parentId = parentQuestionId)
                                    db.questionDao().insert(childEntity)
                                    reportProgress()
                                }
                            }

                            val childrenData = nodeMap["children"] as? List<*>
                            childrenData?.let { importNodes(it, nodeId, depth + 1) }
                        }
                    }

                    nodesData?.let { importNodes(it, null, 0) }

                    db.bookDao().update(
                        bookEntity.copy(
                            id = bookId,
                            totalQuestions = totalQuestions,
                            totalNodes = totalNodes,
                        ),
                    )
                    reportProgress()
                    reportProgress()
                }

                null
            } catch (e: Exception) {
                e.message ?: "Import failed"
            }
        }

        private fun countImportWork(nodes: List<*>?): Int {
            if (nodes == null) return 0
            var count = 0

            fun scan(nodeList: List<*>) {
                nodeList.forEach { node ->
                    if (node !is Map<*, *>) return@forEach
                    count++
                    val questions = node["questions"] as? List<*>
                    questions?.forEach { q ->
                        if (q !is Map<*, *>) return@forEach
                        count++
                        val subQuestions = q["sub_questions"] as? List<*>
                        subQuestions?.forEach { sq -> if (sq is Map<*, *>) count++ }
                    }
                    val children = node["children"] as? List<*>
                    if (children != null) scan(children)
                }
            }

            scan(nodes)
            return count
        }

        private fun parseQuestionEntity(
            map: Map<String, Any?>,
            bookId: Int,
            nodeId: String,
        ): QuestionEntity {
            val choicesData = map["choices"]
            val choicesJson =
                when (choicesData) {
                    is List<*> -> {
                        val list =
                            choicesData.mapNotNull { item ->
                                if (item is Map<*, *>) {
                                    val key = (item["key"] as? String) ?: ""
                                    val content =
                                        (item["content"] as? String)
                                            ?: (item["html"] as? String)
                                            ?: (item["text"] as? String)
                                            ?: ""
                                    mapOf("key" to key, "content" to content)
                                } else {
                                    null
                                }
                            }
                        JSONArray(list.map { JSONObject(it) }).toString()
                    }

                    else -> {
                        null
                    }
                }

            return QuestionEntity(
                bookId = bookId,
                nodeId = nodeId,
                content = map["content"] as? String,
                choices = choicesJson,
                answer = map["answer"] as? String,
                explanation = map["explanation"] as? String,
                questionType = (map["question_type"] as? String) ?: "multiple_choice",
                frontTemplate = map["front_template"] as? String,
                backTemplate = map["back_template"] as? String,
                format = map["format"] as? String ?: "markdown",
            )
        }

        companion object {
            private const val MAX_NODE_DEPTH = 5
        }
    }
