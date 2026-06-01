# How to Configure a Custom AI Provider

Set up a self-hosted API gateway for Anthropic Claude models and connect Mnemora to it.

## Prerequisites

- A running API gateway that exposes an Anthropic-compatible `/v1/messages` endpoint
- A gateway-issued API key

## Recommended: Sub2API

[Sub2API](https://github.com/Wei-Shaw/sub2api) is an open-source AI API gateway that distributes Claude, OpenAI, and Gemini subscriptions through a unified interface. It handles authentication, billing, load balancing, and request forwarding.

Key features:

- Anthropic Messages API compatibility (works with Mnemora's "Custom" provider)
- Multi-account management and concurrency control
- Docker Compose one-click deployment
- Built-in payment system (Stripe, Alipay, WeChat Pay)

### Quick Deploy

```bash
mkdir -p sub2api-deploy && cd sub2api-deploy
curl -sSL https://raw.githubusercontent.com/Wei-Shaw/sub2api/main/deploy/docker-deploy.sh | bash
docker compose up -d
```

The setup wizard is available at `http://YOUR_SERVER_IP:8080`.

For full deployment options (Docker, script, source build), see the [Sub2API README](https://github.com/Wei-Shaw/sub2api).

## Configure Mnemora

1. Open **Settings** in Mnemora.
2. Under **AI Settings**, set **Company** to **Anthropic**.
3. Set **Model** to the desired Claude model (e.g., Claude Opus 4.8).
4. Set **Provider** to **Custom**.
5. Enter the gateway's base URL in the **Base URL** field (e.g., `https://your-gateway.example.com`).
6. Enter the gateway-issued API key in the **API Key** field.
7. Optionally set **Thinking Mode** to **Adaptive** for reasoning-enhanced responses (Opus 4.8/4.7 and Sonnet 4.6).

## Verification

Send a test question in the AI chat panel. If the gateway is correctly configured, the streaming response appears in the chat UI.

## Troubleshooting

| Symptom | Likely Cause | Fix |
|:--------|:-------------|:----|
| 401 / 403 error | Invalid API key | Verify the key in the gateway dashboard |
| Connection refused | Wrong base URL or gateway down | Check URL, ensure gateway is running |
| 400 error with Opus 4.8 | Thinking mode set to "Extended" | Switch to "Adaptive" or "Disabled" |
| Empty responses | Gateway endpoint mismatch | Ensure gateway exposes `/v1/messages` |

## See Also

- [AI Providers Reference](../reference/ai-providers.md) — Full provider and model matrix
- [Sub2API GitHub](https://github.com/Wei-Shaw/sub2api) — Project source and documentation
