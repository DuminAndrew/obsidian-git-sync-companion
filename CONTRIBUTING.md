# Contributing to Obsidian Git-Sync Companion

Thanks for taking the time to contribute! This project welcomes bug reports,
feature ideas, documentation fixes, and pull requests.

## Code of Conduct

This project adheres to the [Contributor Covenant](CODE_OF_CONDUCT.md). By
participating, you are expected to uphold it. Report unacceptable behaviour to
**duminandrew@gmail.com**.

## Ways to contribute

- **Report a bug** using the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md).
- **Request a feature** using the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md).
- **Improve docs** — typos, clarifications, and examples are all welcome.
- **Send a pull request** for a fix or feature (see below).

## Development setup

- **Android Studio** Koala (2024.1) or newer, or the command line with **JDK 17**.
- **Kotlin 2.0.x**, **AGP 8.5.x**, **Compose BOM 2024.09**, **Material 3**.
- `minSdk 26`, `targetSdk`/`compileSdk 34`.

```bash
# Regenerate the Gradle wrapper jar if it is missing (not committed):
gradle wrapper --gradle-version 8.9

# Point Gradle at your SDK:
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
```

## Before opening a pull request

Run the same checks CI runs, locally:

```bash
./gradlew ktlintCheck detekt test assembleDebug
```

- **Formatting:** `./gradlew ktlintFormat` auto-fixes most style issues.
- **Static analysis:** keep `detekt` clean; the config lives in
  `config/detekt/detekt.yml`.
- **Tests:** add JVM unit tests for any pure-Kotlin logic you change. Tests must
  be **deterministic and offline** — no network, no real device, and never touch
  `EncryptedSharedPreferences`/the Android Keystore (use the `TokenStore` /
  `SettingsStorage` interfaces and their in-memory fakes).

## Pull request guidelines

1. Fork the repo and create a topic branch off `main`
   (`feature/short-description` or `fix/short-description`).
2. Keep changes focused; one logical change per PR.
3. Update `CHANGELOG.md` under an `Unreleased` section when behaviour changes.
4. Ensure the CI checklist passes locally (command above).
5. Fill in the [pull request template](.github/PULL_REQUEST_TEMPLATE.md) and link
   any related issues.

## Commit messages

Write clear, imperative commit subjects (e.g. "Add conflict timestamp test").
Reference issues with `#123` where relevant.

## Coding style

- Official Kotlin style (`ktlint_official`), enforced by ktlint via `.editorconfig`.
- Prefer pure, framework-free logic in the `core` package so it stays unit-testable.
- Never log or persist the Personal Access Token anywhere outside
  `EncryptedSharedPreferences`. See [SECURITY.md](SECURITY.md).

## Security issues

Please do **not** open public issues for security vulnerabilities. Follow the
process in [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contributions will be licensed under the
project's [MIT License](LICENSE).
