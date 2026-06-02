# Obsidian Git-Sync Companion

[![CI](https://github.com/DuminAndrew/obsidian-git-sync-companion/actions/workflows/ci.yml/badge.svg)](https://github.com/DuminAndrew/obsidian-git-sync-companion/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)

Android app for **background, conflict-free synchronization** of an
[Obsidian](https://obsidian.md) vault folder with a **private GitHub repository** —
no native git, no Termux, no SSH keys. It talks directly to the GitHub REST API
using a Personal Access Token stored **encrypted on-device**.

> Free and open source (MIT). Built with Kotlin, Jetpack Compose (Material 3),
> WorkManager, and a clean MVVM architecture.

---

## Features

- **Two-way sync** of a local Obsidian vault folder with a private GitHub repo.
- **Conflict-free by design** — divergent edits are never silently overwritten.
  The remote version is kept as `note (conflict <timestamp>).md` so you can merge
  it inside Obsidian.
- **No storage permissions** — the vault is chosen via the Storage Access
  Framework (SAF / `OpenDocumentTree`) with a persisted URI grant.
- **No native git / NDK** — uses the GitHub **Git Data API** (refs, commits,
  recursive trees) + **Contents API** (read/write files). Lightweight APK.
- **Encrypted token storage** — the PAT lives only in
  `EncryptedSharedPreferences` (AES-256-GCM, Android Keystore) and is **never
  logged**.
- **Background auto-sync** — periodic `WorkManager` job (~15 min) with
  network constraints (Wi-Fi-only toggle).
- **Manual "Synchronize now"** button + live status log.
- Material 3 UI with dynamic color (Android 12+).

---

## Why the GitHub REST API (and not JGit / native git)?

| Aspect | REST API + PAT (this app) | JGit / native git |
|---|---|---|
| Works with SAF vault on SD/external storage | Yes | No (`.git` needs a real `java.io.File` tree) |
| APK size / complexity | Small, no NDK | Large, NDK/FFI |
| Local `.git` clone required | No | Yes |
| Auth | One PAT header | SSH keys / credential helper |

Obsidian vaults on Android are usually only reachable through SAF, and we only
need a reliable two-way upsert of a notes folder — not branching/rebasing on the
device. See `research_and_benchmarks.md` for the full analysis (compared against
obsidian-git, GitJournal, Pocket Git, JGit-based clients).

---

## Build instructions

There is **no Android SDK in the authoring environment**, so the project is
written to be built in **Android Studio** (or via Gradle once an SDK is present).

### Option A — Android Studio (recommended)

1. Install **Android Studio Koala (2024.1)** or newer.
2. `File ▸ Open…` → select the `ObsidianGitSync` folder.
3. Android Studio will **regenerate the Gradle wrapper JAR** and download the
   AGP/Kotlin/Compose dependencies automatically.
4. Let it create/point to a `local.properties` with your `sdk.dir`.
5. Select a device/emulator (min API 26 / Android 8.0) and press **Run ▸ Run 'app'**.

### Option B — Command line (SDK + JDK 17 installed)

```bash
# (One-time) regenerate the wrapper JAR if it is missing:
gradle wrapper --gradle-version 8.9

# Point Gradle at your SDK (or set ANDROID_HOME / sdk.dir in local.properties):
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# Build a debug APK:
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug

# Install on a connected device:
./gradlew installDebug
```

The built APK lands in `app/build/outputs/apk/debug/`.

> Note: `gradle/wrapper/gradle-wrapper.jar` is intentionally **not** committed by
> this generator. Android Studio or `gradle wrapper` recreates it on first sync.

### Toolchain

- AGP 8.5.x, Kotlin 2.0.x, JDK 17
- Compose BOM 2024.09, Material 3
- minSdk 26, targetSdk/compileSdk 34

---

## Creating the GitHub Personal Access Token (PAT)

The app needs write access to a **single private repository** that holds your
vault.

### Fine-grained token (recommended)

1. GitHub ▸ **Settings ▸ Developer settings ▸ Personal access tokens ▸
   Fine-grained tokens ▸ Generate new token**.
2. **Resource owner**: your account (or org owning the repo).
3. **Repository access ▸ Only select repositories** → pick your vault repo.
4. **Permissions ▸ Repository permissions ▸ Contents → Read and write**.
5. Set an expiration, generate, and **copy the token** (shown once).

### Classic token (alternative)

- Scope: **`repo`** (full control of private repositories).

### Where the token is stored

Paste the PAT into the app's **Personal Access Token** field and tap **Save
token**. It is written to `EncryptedSharedPreferences` (AES-256-GCM, keys in the
Android Keystore). It is:

- never stored in plaintext preferences,
- never written to logs (HTTP logging is BASIC-level and debug-only, so headers
  and bodies — including the `Authorization` header — are never printed),
- excluded from cloud backup and device-to-device transfer.

You can wipe it any time with **Clear**.

---

## How sync works (conflict-free strategy)

For every path the app compares three SHAs:

- **base** — the git blob SHA from the last successful sync (stored locally),
- **local** — git blob SHA computed from the current vault file bytes,
- **remote** — blob SHA from the GitHub recursive tree.

| local vs base | remote vs base | action |
|---|---|---|
| unchanged | unchanged | no-op |
| changed | unchanged | **push** local |
| unchanged | changed | **pull** remote |
| changed | changed (and local ≠ remote) | **conflict** |
| missing locally | present | pull (new) |
| present | missing remotely | push (new) |

**Conflict resolution (keep-both):** the remote version is written to
`note (conflict <timestamp>).md`, the local version is pushed so the repo
matches local, and the baseline advances. **No edit is ever lost**; you merge the
two files in Obsidian. Reruns are idempotent.

> Deletions are treated conservatively (re-created rather than auto-deleted) to
> avoid data loss; explicit deletion via tombstones is a planned enhancement.

---

## Crypto donations

If this project saves you a paid-sync subscription, donations are welcome and
entirely optional. **Replace the placeholders below with your real addresses
before publishing.**

| Asset | Address (placeholder) | QR |
|---|---|---|
| Bitcoin (BTC) | `bc1qXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX` | `docs/donate/btc_qr.png` (placeholder) |
| Ethereum (ETH / ERC-20) | `0xXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX` | `docs/donate/eth_qr.png` (placeholder) |
| USDT (TRC-20) | `TXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX` | `docs/donate/usdt_qr.png` (placeholder) |
| Monero (XMR) | `4XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX` | `docs/donate/xmr_qr.png` (placeholder) |

<!-- QR placeholders: drop PNGs at the paths above and they will render here. -->

### Donation security

- **Always verify the address** against the project's official repository over
  HTTPS before sending. Cross-check the first/last 4–6 characters.
- These are **placeholder addresses** — sending funds to them will lose your
  coins. Real addresses are published only by the maintainer.
- Crypto transactions are **irreversible**. Send a tiny test amount first.
- Beware of **fake forks / scam mirrors** that swap the addresses. Trust only the
  canonical repo URL.
- The maintainer will **never** DM you asking for funds or seed phrases.

---

## Project structure

```
ObsidianGitSync/
├── settings.gradle.kts, build.gradle.kts, gradle.properties
├── gradle/libs.versions.toml            # version catalog
├── gradlew, gradlew.bat                 # wrapper scripts (JAR regenerated by AS)
├── LICENSE                              # MIT
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml          # INTERNET, POST_NOTIFICATIONS; SAF (no storage perm)
        ├── java/io/github/duminandrew/obsidiangitsync/
        │   ├── ObsidianGitSyncApp.kt    # Application + WorkManager config
        │   ├── MainActivity.kt          # Compose host
        │   ├── data/
        │   │   ├── SecureStore.kt        # EncryptedSharedPreferences (PAT)
        │   │   ├── SettingsStore.kt      # repo/branch/vault/auto-sync
        │   │   ├── SyncStateStore.kt     # base SHA per path
        │   │   ├── VaultStorage.kt       # SAF / DocumentFile read/write
        │   │   └── github/
        │   │       ├── GitHubApi.kt      # Retrofit (refs/commits/trees/contents)
        │   │       ├── GitHubClient.kt   # OkHttp + auth interceptor (no token logging)
        │   │       └── GitHubModels.kt   # serialization DTOs
        │   ├── sync/
        │   │   ├── SyncEngine.kt         # conflict-free 3-way reconcile
        │   │   ├── SyncRepository.kt     # singleton state + flows
        │   │   ├── SyncWorker.kt         # WorkManager CoroutineWorker
        │   │   ├── SyncScheduler.kt      # periodic + one-off scheduling
        │   │   ├── GitBlobSha.kt         # git hash-object SHA-1
        │   │   └── SyncResult.kt         # result + log model
        │   └── ui/
        │       ├── SyncScreen.kt         # Compose Material3 UI
        │       ├── SyncViewModel.kt      # MVVM
        │       ├── SyncUiState.kt
        │       └── theme/                # Color / Theme / Type
        └── res/                         # strings, themes, icons, backup rules
```

## Security summary

- PAT only in `EncryptedSharedPreferences`; never logged, never backed up.
- HTTPS-only GitHub API; auth via `Authorization: Bearer` header injected by an
  interceptor.
- `allowBackup=false` + data-extraction rules exclude prefs/files.
- No broad storage permission — scoped SAF grant to the chosen folder only.

## License

[MIT](LICENSE) © 2026 Andrew Dumin
