package com.hihusky.mnemora.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiConnectionProfileTest {
    @Test
    fun `profiles are isolated by provider and model`() {
        var raw = "{}"
        raw =
            AiConnectionProfiles.put(
                raw,
                provider = "custom-openai",
                model = "gpt-5.6",
                profile = AiConnectionProfile(baseUrl = "https://sol.example.com/v1"),
            )
        raw =
            AiConnectionProfiles.put(
                raw,
                provider = "custom-openai",
                model = "gpt-5.6-luna",
                profile = AiConnectionProfile(baseUrl = "https://luna.example.com/v1"),
            )

        assertEquals(
            "https://sol.example.com/v1",
            AiConnectionProfiles.get(raw, "custom-openai", "gpt-5.6")?.baseUrl,
        )
        assertEquals(
            "https://luna.example.com/v1",
            AiConnectionProfiles.get(raw, "custom-openai", "gpt-5.6-luna")?.baseUrl,
        )
        assertNull(AiConnectionProfiles.get(raw, "openai", "gpt-5.6"))
    }

    @Test
    fun `malformed cache is treated as empty`() {
        assertNull(AiConnectionProfiles.get("not-json", "openai", "gpt-5.6"))
    }
}
