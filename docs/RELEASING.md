# Releasing LanShare

LanShare releases are created from Git tags. Pushing a tag such as `v0.2.0` triggers
`.github/workflows/release.yml`, which tests the project, builds a signed release APK,
verifies its signature, creates a SHA-256 checksum, and publishes a GitHub Release.

## Signing key storage

The Android release keystore is a long-lived private key. Do not commit it to Git.

Keep two independent copies:

1. An offline/private backup of the original `.jks` file and its passwords.
2. A Base64 copy in the repository's GitHub Actions Secrets for CI releases.

Required repository secrets:

- `ANDROID_KEYSTORE_BASE64`: Base64 of the complete keystore file.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: signing key alias.
- `ANDROID_KEY_PASSWORD`: signing key password.

GitHub path: **Settings → Secrets and variables → Actions → New repository secret**.

To create the Base64 value locally:

```bash
base64 -w0 lanshare-release.jks
```

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("lanshare-release.jks"))
```

The same signing key must be kept for all future versions. Losing it can prevent users
from installing future APKs as an update over an existing installation.

## Release procedure

Before tagging, update `versionName` and increment `versionCode` in
`app/build.gradle.kts`. The tag must exactly match `v<versionName>`.

Example for version 0.2.0:

```bash
git checkout main
git pull --ff-only
git tag v0.2.0
git push origin v0.2.0
```

The workflow fails closed if signing secrets are missing, the tag does not match
`versionName`, the release APK cannot be built, or `apksigner` cannot verify it.

Published assets:

- `LanShare-vX.Y.Z.apk`
- `SHA256SUMS.txt`
