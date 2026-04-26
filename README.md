# Mnemora

An Android app for structured learning and spaced repetition. Organize books into hierarchical nodes with arbitrary depth, quiz yourself with configurable question types, and let the SRS scheduler surface material for review at optimal intervals.

## Quick Start

Requires JDK 17+, Android SDK 35, and a connected device or emulator with USB debugging enabled.

```bash
# Clone
git clone <repo-url> && cd mnemora

# One-shot build, install, launch, and tail logs
./scripts/dev.sh run
```

You should see the app launch on your device and log output in the terminal.

## Documentation

- [Full documentation](docs/index.md)
- [Getting Started Tutorial](docs/tutorials/01-getting-started.md)
- [CLI Reference](docs/reference/cli.md)
- [Architecture Overview](docs/explanation/architecture-overview.md)
- [Contributing](CONTRIBUTING.md)

## When to use this project

Mnemora is a good fit if you want:
- A local-first, offline-capable study app with Room database persistence
- Spaced repetition review scheduling (SM-2 inspired)
- AI-assisted explanations via a chat interface
- Custom collections and progress tracking

Consider established alternatives like Anki if you need cross-platform sync or a mature plugin ecosystem.

## License

MIT — see [LICENSE](LICENSE).
