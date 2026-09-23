# Releasing LanShare

LanShare releases are created from Git tags. Pushing a tag such as `v0.2.0` triggers
`.github/workflows/release.yml`, which tests the project, builds signed release APKs,
checks every APK is signed, non-debuggable, and uses `top.nexur.lanshare`, enforces
the 15 MiB limit for each ABI APK, creates SHA-256 checksums, and publishes a GitHub
Release. The size breakdown is written to the Actions summary and workflow artifact;
it is not added as a public Release asset.

The universal APK measured 24.06 MiB in CI, with native libraries accounting for
80.9% of its compressed size. Release builds therefore provide four APKs instead:
`arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`. Install the APK matching the
device's Android ABI. This keeps all four architectures available while each
download contains only its own native libraries.

Pull request verification also builds the optimized, unsigned Release variant and
enforces the per-APK 15 MiB limit and checks that there is exactly one APK per ABI
before changes can merge. The tagged Release build repeats the checks after signing
so the gate applies to the exact published APKs.

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

- `LanShare-vX.Y.Z-arm64-v8a.apk`
- `LanShare-vX.Y.Z-armeabi-v7a.apk`
- `LanShare-vX.Y.Z-x86.apk`
- `LanShare-vX.Y.Z-x86_64.apk`
- `SHA256SUMS.txt`
