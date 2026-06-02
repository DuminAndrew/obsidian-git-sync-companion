# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-06-02

### Added

- Initial release of **obsidian-git-sync-companion** — an Android app that syncs
  an Obsidian vault to a GitHub repository using the **GitHub REST API** and a
  Personal Access Token (no local `git` binary required).
- **Secure token storage** via EncryptedSharedPreferences (Android Keystore);
  the PAT is never logged or stored in plaintext.
- **Background sync** with WorkManager and **Storage Access Framework** access to
  the vault folder.
- Pure-Kotlin **sync core** extracted for testability: GitHub API path building,
  URL encoding, Base64 content encode/decode, git blob SHA computation, push/pull
  sync decisions, conflict file naming, retry/backoff, and PAT validation.
- Jetpack Compose UI with a settings/repository layer abstracted behind interfaces
  so the secure store can be faked in tests.
- Comprehensive JVM unit-test suite covering the sync core (Base64, paths, URL
  encoding, SHA, conflict naming, sync decisions, retry, PAT validation, settings).
- **ktlint** and **detekt** static analysis, and a GitHub Actions CI workflow that
  lints, tests, and assembles a debug APK.

[Unreleased]: https://github.com/DuminAndrew/obsidian-git-sync-companion/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/DuminAndrew/obsidian-git-sync-companion/releases/tag/v0.1.0
