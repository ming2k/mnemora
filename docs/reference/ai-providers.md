# AI Providers Reference

Providers, models, and connection settings exposed by the in-app catalog,
plus the protocol families the streaming layer can drive.

## Provider Catalog

The catalog is provider-first: one entry owns its models and its connection
shape, so configuration is isolated per `(provider, model)` pair without any
grouping "company" notion. Source of truth:
`domain/service/AiProviderCatalog.kt`.

| Provider ID | Display Name | Protocol | Custom Host | Models |
|:------------|:-------------|:---------|:------------|:-------|
| `antigravity-sub2api` | Antigravity sub2api | Gemini GenerateContent | required | 4 (below) |

### Antigravity sub2api Models

| Model | API ID |
|:------|:-------|
| Gemini 3.7 Flash | `gemini-3.7-flash-tiered` |
| Gemini 3.6 Flash | `gemini-3.6-flash-tiered` |
| Gemini 3.1 Pro Low | `gemini-3.1-pro-low` |
| Gemini 3.1 Pro High | `gemini-3.1-pro-high` |

### Connection Shape

| Field | Purpose | Example |
|:------|:---------|:---------|
| Base URL | Gemini-compatible relay host root (no `/v1beta`) | `https://your-relay.com` |
| API Key | Relay-issued key | `sk-...` |

Requests go to `{Base URL}/v1beta/models/{model}:streamGenerateContent`.
The API key travels in the `x-goog-api-key` header — never in the URL — so it
does not leak through logs or proxy records.

## Protocol Families

Adapters live in `data/remote/ai/`. The catalog maps each provider to one
protocol; `AiProviderFactory` routes the request to the matching adapter.
Only protocols referenced by catalog entries are reachable from the UI; the
remaining families stay wired for catalog expansion.

| Protocol | Endpoint | Auth | Notes |
|:---------|:---------|:-----|:------|
| `GEMINI` | `/v1beta/models/{model}:streamGenerateContent?alt=sse` | `x-goog-api-key` header | Skips `thought` parts in stream chunks |
| `VERTEX` | `https://aiplatform.googleapis.com/v1/...:streamGenerateContent?alt=sse` | `x-goog-api-key` header | Preview models force the `global` location |
| `OPENAI` | `/chat/completions` | `Authorization: Bearer <key>` | Optional `reasoning_effort` |
| `DEEPSEEK` | `/chat/completions` | `Authorization: Bearer <key>` | Enables thinking; `reasoning_effort: high` |
| `KIMI` | `/v1/chat/completions` | `Authorization: Bearer <key>` | Moonshot API |
| `ANTHROPIC` | `/v1/messages` | `x-api-key` + `Authorization: Bearer <key>` | Thinking-mode negotiation per model family |

All families share one SSE engine (`SseStream`): `data:` framing,
`[DONE]` handling, malformed-frame recovery, and whitespace-preserving delta
extraction. HTTP failures raise `AiHttpException` with the provider name,
status code, and response body.

## Legacy Provider Migration

Persisted settings that reference a provider or model no longer in the
catalog resolve to the default catalog entry on launch
(`AiProviderCatalog.resolve`). Connection profiles remain cached per
`(provider, model)` in `ai_connection_profiles`, so re-adding a provider
later restores its saved key and base URL.

## App Configuration Keys

Settings persist in Android DataStore via `SettingsRepository`.

| Key | Type | Default | Purpose |
|:----|:-----|:--------|:--------|
| `ai_provider` | String | `antigravity-sub2api` | Active provider ID |
| `ai_model` | String | `gemini-3.7-flash-tiered` | Model identifier |
| `ai_api_key` | String | `""` | Current API key |
| `ai_api_key_cache` | String (JSON) | `"{}"` | Per-provider cached keys |
| `ai_connection_profiles` | String (JSON) | `"{}"` | Per `(provider, model)` connection profiles |
| `ai_base_url` | String | `""` | Custom provider base URL |
| `ai_project_id` | String | `""` | Vertex AI project ID |
| `ai_location` | String | `""` | Vertex AI region |
| `ai_system_prompt` | String | study-assistant prompt | System prompt sent with each request |
| `ai_thinking_mode` | String | `"disabled"` | `"disabled"`, `"adaptive"`, or `"enabled"` |
| `ai_reasoning_effort` | String | `""` | `""` or one of `minimal`…`max` |
| `ai_context_include_stem` | Boolean | `true` | Include question stem in context |
| `ai_context_include_options` | Boolean | `true` | Include answer choices in context |
| `ai_context_include_answer` | Boolean | `true` | Include correct answer in context |
| `ai_context_include_explanation` | Boolean | `true` | Include existing explanation in context |

## See also

- [How to configure a custom AI provider](../how-to/configure-custom-ai-provider.md)
- [Configuration Reference](configuration.md)
