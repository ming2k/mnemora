# Contributing to Mnemora

Thank you for your interest in contributing!

## Quick Links

- [Developer Setup](docs/dev/setup.md) — clone, build, and run tests
- [Project Layout](docs/dev/project-layout.md) — tour of the source tree
- [Testing](docs/dev/testing.md) — how to run and write tests
- [Code Style](docs/dev/code-style.md) — formatting, naming, and language policy

## Pull Request Etiquette

1. **Open an issue first** for significant changes (new features, architectural shifts) so we can agree on direction before you invest time.
2. **Keep PRs focused.** One logical change per PR. If you have unrelated fixes, split them.
3. **All tests must pass.** Run `./gradlew test` and `./gradlew connectedAndroidTest` (if applicable) before submitting.
4. **Follow the code style.** See [docs/dev/code-style.md](docs/dev/code-style.md).
5. **Update the changelog.** Add an entry under `[Unreleased]` in `CHANGELOG.md` describing the user-visible impact.
6. **Do not commit local-only files.** Ensure `local.properties`, build output, and IDE caches are excluded by `.gitignore`.
7. **If your change involves an architectural decision**, add an ADR following the template in `docs/adr/template.md`.

## Reporting Issues

- Use GitHub Issues.
- Include: device/Android version, reproduction steps, expected vs actual behavior, and relevant log output.

## Communication

The working language for all documentation, comments, commit messages, and issue discussions is **English**.
