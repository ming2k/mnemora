package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Streams against a local [MockWebServer] to cover the parts every provider
 * adapter shares: SSE framing, [DONE] handling, malformed-frame recovery and
 * whitespace-preserving delta extraction.
 */
class SseStreamTest {
    private lateinit var server: MockWebServer
    private val client = OkHttpClient()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun config() =
        AiConfig(
            apiKey = "test-key",
            provider = "custom-openai",
            model = "test-model",
            baseUrl = server.url("/v1").toString().trimEnd('/'),
        )

    private fun provider() = OpenAIProvider(client)

    @Test
    fun `emits content deltas in order and skips DONE frames`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .body(
                        listOf(
                            "data: {\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}",
                            "",
                            "data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}",
                            "",
                            "data: [DONE]",
                            "",
                        ).joinToString("\n"),
                    ).build(),
            )

            val chunks = provider().streamChat(config(), "ctx", emptyList<ChatMessage>()).toList()

            assertEquals(listOf("Hel", "lo"), chunks)
        }

    @Test
    fun `preserves whitespace-only deltas so streaming markdown keeps line breaks`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .body(
                        listOf(
                            "data: {\"choices\":[{\"delta\":{\"content\":\"Para\"}}]}",
                            "",
                            "data: {\"choices\":[{\"delta\":{\"content\":\"\\n\\n\"}}]}",
                            "",
                            "data: {\"choices\":[{\"delta\":{\"content\":\"two\"}}]}",
                            "",
                        ).joinToString("\n"),
                    ).build(),
            )

            val chunks = provider().streamChat(config(), "ctx", emptyList<ChatMessage>()).toList()

            assertEquals(listOf("Para", "\n\n", "two"), chunks)
        }

    @Test
    fun `skips malformed frames without failing the stream`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .body(
                        listOf(
                            "data: not-json-at-all",
                            "",
                            "data: {\"choices\":[{\"delta\":{\"content\":\"ok\"}}]}",
                            "",
                            "data: {\"choices\":\"unexpected shape\"}",
                            "",
                        ).joinToString("\n"),
                    ).build(),
            )

            val chunks = provider().streamChat(config(), "ctx", emptyList<ChatMessage>()).toList()

            assertEquals(listOf("ok"), chunks)
        }

    @Test
    fun `throws AiHttpException with provider name and body on http error`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(500)
                    .body("boom")
                    .build(),
            )

            try {
                provider().streamChat(config(), "ctx", emptyList<ChatMessage>()).toList()
                fail("expected AiHttpException")
            } catch (e: AiHttpException) {
                assertTrue(e.message!!.startsWith("OpenAI API error: 500"))
                assertTrue(e.message!!.endsWith("boom"))
            }
        }

    @Test
    fun `sends authorization header and streaming chat request`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .body("data: {\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}\n\n")
                    .build(),
            )

            val messages =
                listOf(ChatMessage(text = "Explain", isUser = true))
            provider().streamChat(config(), "Question context", messages).toList()

            val recorded = server.takeRequest()
            assertEquals("POST", recorded.method)
            assertEquals("Bearer test-key", recorded.headers["Authorization"])
            val body = recorded.body!!.utf8()
            assertTrue(body.contains("\"stream\":true"))
            assertTrue(body.contains("Question context"))
            assertTrue(body.contains("\"role\":\"user\""))
        }
}
