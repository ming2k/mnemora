# AI Providers Reference

Supported AI providers, models, API endpoints, and feature compatibility.

## Provider Matrix

| Provider ID | Display Name | Protocol | Default Host |
|:------------|:-------------|:---------|:-------------|
| `anthropic` | Anthropic API | Anthropic Messages | `https://api.anthropic.com` |
| `custom` | Custom (Anthropic-compatible) | Anthropic Messages | User-configured |
| `gemini` | Google AI Studio | Gemini GenerateContent | `https://generativelanguage.googleapis.com` |
| `vertex-ai` | GCP Vertex AI | Vertex AI | `https://aiplatform.googleapis.com` |
| `deepseek` | DeepSeek API | OpenAI Chat Completions | `https://api.deepseek.com` |
| `kimi` | Moonshot API | OpenAI Chat Completions | `https://api.moonshot.cn` |

## Anthropic (Claude)

### Supported Models

| Model | API ID | Context Window | Max Output |
|:------|:-------|:---------------|:-----------|
| Claude Fable 5 | `claude-fable-5` | 1M tokens | 128k tokens |
| Claude Opus 4.8 | `claude-opus-4-8` | 1M tokens | 128k tokens |
| Claude Opus 4.7 | `claude-opus-4-7` | 1M tokens | 128k tokens |
| Claude Sonnet 4.6 | `claude-sonnet-4-6` | 1M tokens | 64k tokens |
| Claude Haiku 4.5 | `claude-haiku-4-5` | 200k tokens | 64k tokens |

### Thinking Mode Compatibility

| Model | Adaptive Thinking | Extended Thinking |
|:------|:------------------|:------------------|
| Claude Fable 5 | Supported (only mode) | Not supported (400 error) |
| Claude Opus 4.8 | Supported (only mode) | Not supported (400 error) |
| Claude Opus 4.7 | Supported (only mode) | Not supported (400 error) |
| Claude Sonnet 4.6 | Supported (recommended) | Deprecated but functional |
| Claude Haiku 4.5 | Not supported | Supported |

### API Details

| Field | Value |
|:------|:------|
| Endpoint | `/v1/messages` |
| Auth header | `x-api-key: <key>` + `Authorization: Bearer <key>` |
| API version header | `anthropic-version: 2023-06-01` |
| Streaming | SSE (`stream: true`) |
| Official docs | <https://docs.anthropic.com/en/api/messages> |
| Model overview | <https://docs.anthropic.com/en/docs/about-claude/models> |
| Extended thinking | <https://docs.anthropic.com/en/docs/build-with-claude/extended-thinking> |
| Adaptive thinking | <https://docs.anthropic.com/en/docs/build-with-claude/adaptive-thinking> |

### Thinking Mode Configuration

| Mode | Request Body | Description |
|:-----|:-------------|:------------|
| `disabled` | _(omit `thinking` key)_ | No thinking. Lowest latency. |
| `adaptive` | `"thinking": {"type": "adaptive", "display": "summarized"}` | Model decides when and how much to think. |
| `enabled` | `"thinking": {"type": "enabled", "budget_tokens": N, "display": "summarized"}` | Manual budget control. Not valid on Fable 5 or Opus 4.8/4.7. |

On Claude Fable 5, an explicit `"thinking": {"type": "disabled"}` returns a 400 error; the `disabled` mode omits the `thinking` key, which is the required form.

### SSE Event Types (Streaming)

| Event `type` | `delta.type` | Emitted Content |
|:-------------|:-------------|:----------------|
| `content_block_delta` | `thinking_delta` | Thinking text (when thinking enabled) |
| `content_block_delta` | `text_delta` | Final response text |
| `content_block_delta` | `signature_delta` | Encrypted thinking signature |

The app only emits `text_delta` content to the user. Thinking blocks are processed internally but not displayed.

### Custom Provider

Selecting "Custom" as the provider uses the same Anthropic Messages protocol with a user-specified base URL. This enables compatibility with self-hosted API gateways such as [Sub2API](https://github.com/Wei-Shaw/sub2api).

Configuration fields:

| Field | Purpose | Example |
|:------|:---------|:---------|
| Base URL | Anthropic-compatible endpoint | `https://your-gateway.example.com` |
| API Key | Gateway-issued key | `sk-...` |

See [Configure a Custom AI Provider](../how-to/configure-custom-ai-provider.md) for a step-by-step setup guide.

## Google (Gemini)

### Supported Models

| Model | API ID |
|:------|:-------|
| Gemini 3.1 Pro Preview | `gemini-3.1-pro-preview` |
| Gemini 3.1 Flash Lite Preview | `gemini-3.1-flash-lite-preview` |

### API Details

| Field | Value |
|:------|:------|
| Endpoint | `/v1beta/models/{model}:streamGenerateContent?alt=sse` |
| Auth | API key as `?key=` query parameter |
| Streaming | SSE |
| AI Studio docs | <https://ai.google.dev/gemini-api/docs> |
| Vertex AI docs | <https://cloud.google.com/vertex-ai/generative-ai/docs> |

## DeepSeek

### Supported Models

| Model | API ID |
|:------|:-------|
| DeepSeek V4 Pro | `deepseek-v4-pro` |
| DeepSeek V4 Flash | `deepseek-v4-flash` |

### API Details

| Field | Value |
|:------|:------|
| Endpoint | `/chat/completions` |
| Auth | `Authorization: Bearer <key>` |
| Streaming | SSE (`"stream": true`) |
| Official docs | <https://platform.deepseek.com/api-docs> |

## Moonshot (Kimi)

### Supported Models

| Model | API ID |
|:------|:-------|
| Kimi K2.6 | `kimi-k2.6` |

### API Details

| Field | Value |
|:------|:------|
| Endpoint | `/v1/chat/completions` |
| Auth | `Authorization: Bearer <key>` |
| Streaming | SSE (`"stream": true`) |
| Official docs | <https://platform.moonshot.cn/docs> |

## App Configuration Keys

Settings are persisted in Android DataStore.

| Key | Type | Default | Purpose |
|:----|:-----|:--------|:--------|
| `ai_provider` | String | `"gemini"` | Active provider ID |
| `ai_model` | String | `"gemini-3.1-flash-lite-preview"` | Model identifier |
| `ai_api_key` | String | `""` | Current API key |
| `ai_api_key_cache` | String (JSON) | `"{}"` | Per-provider cached keys |
| `ai_base_url` | String | `""` | Custom provider base URL |
| `ai_project_id` | String | `""` | Vertex AI project ID |
| `ai_location` | String | `""` | Vertex AI region |
| `ai_system_prompt` | String | _(see source)_ | System prompt sent with each request |
| `ai_thinking_mode` | String | `"disabled"` | `"disabled"`, `"adaptive"`, or `"enabled"` |
| `ai_context_include_stem` | Boolean | `true` | Include question stem in context |
| `ai_context_include_options` | Boolean | `true` | Include answer choices in context |
| `ai_context_include_answer` | Boolean | `true` | Include correct answer in context |
| `ai_context_include_explanation` | Boolean | `true` | Include existing explanation in context |
