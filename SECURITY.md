# Security Policy

## Supported Versions

obsidian-git-sync-companion is in active development. Security fixes are applied
to the latest released version on the `main` branch.

| Version | Supported          |
|---------|--------------------|
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability, please report it **privately** — do not
open a public issue.

- Email **duminandrew@gmail.com** with a clear description and reproduction steps.
- Alternatively, use GitHub's [private security advisories](https://github.com/DuminAndrew/obsidian-git-sync-companion/security/advisories/new).

Please include:

- The affected component (e.g. token storage, GitHub REST client, sync engine, WorkManager job).
- Steps to reproduce or a proof-of-concept.
- The potential impact as you see it.

You can expect an initial acknowledgement within **5 business days**. Once a fix
is available, a coordinated disclosure timeline will be agreed upon.

## How your credentials are handled

This app authenticates to GitHub with a **Personal Access Token (PAT)** that you
provide. To keep it safe:

- The PAT is stored using **EncryptedSharedPreferences** (backed by the Android
  Keystore) and is **never logged**, never written to disk in plaintext, and
  never committed.
- The token is sent **only** to the GitHub REST API over HTTPS, and to no other
  host. The app makes no analytics or telemetry calls.
- **Use a least-privilege token.** Prefer a fine-grained PAT scoped to the single
  repository you sync, with only the permissions it needs (Contents: read/write).
  Avoid classic tokens with broad `repo` scope where possible.
- You can revoke the token at any time from your GitHub settings; the app keeps
  no server-side copy.

If you believe a code path could leak the token (logs, crash reports, backups,
exported intents), please treat it as security-sensitive and report it privately.
