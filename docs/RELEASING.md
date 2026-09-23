# Releasing LanShare

LanShare releases are created from Git tags. Pushing a tag such as v0.2.0 triggers
.github/workflows/release.yml, which tests the project, builds one signed ARM APK,
verifies its application ID, non-debuggable state, and signature, enforces the 15 MiB
limit and the expected ARM ABI set, creates a SHA-256 checksum, and publishes a
GitHub Release. The size breakdown is written to the Actions summary and workflow
artifact; it is not added as a public Release asset.

The original universal APK measured 24.06 MiB in CI, with native libraries
accounting for 80.9% of its compressed size. The public Release APK contains both
arm64-v8a and armeabi-v7a native libraries, so Android users download one package
without choosing their device ABI. x86 and x86_64 APKs remain part of PR
verification builds for emulator and automated testing; they are not published as
user-facing Release assets.

Pull request verification builds and size-checks four separate unsigned ABI APKs,
then builds the same combined ARM Release APK used for publishing and checks its
application ID, non-debuggable state, ABI set, and 15 MiB limit. The tag workflow
repeats those checks on the signed APK that will actually be published.

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

- LanShare-vX.Y.Z.apk — one install package for arm64-v8a and armeabi-v7a devices.
- SHA256SUMS.txt — SHA-256 checksum for the APK.
