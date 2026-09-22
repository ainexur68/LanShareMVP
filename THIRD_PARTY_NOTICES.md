# Third-party notices

The Apache-2.0 license in [`LICENSE`](LICENSE) applies only to original
LocalShare source code and documentation. Dependencies brought in by Gradle
remain under their own licenses and notices.

The current direct dependencies are declared in
[`app/build.gradle.kts`](app/build.gradle.kts), including AndroidX/Jetpack
artifacts, CameraX, ML Kit Barcode Scanning, ZXing Core, Kotlin/Compose tooling,
and JUnit. When redistributing a built
APK or changing dependencies, regenerate the dependency/license inventory
from the exact resolved versions and include any required upstream notices.

LocalSend is a protocol/design reference, not a bundled dependency. See
[`docs/REFERENCES.md`](docs/REFERENCES.md) and [`NOTICE`](NOTICE) for the
separate attribution and license review.
