# Verification record

Verified on **23 October 2026** using macOS, Microsoft OpenJDK 17.0.16, Gradle 8.8, Processing 4.5.7, and JUnit 5.10.3. These are observed results for this portfolio edition, not a claim that every possible gameplay path is tested.

## Reproduce

With JDK 17 and a desktop display:

```bash
./gradlew clean build installDist -PcaptureScreenshots --no-daemon
```

For a Linux environment without a physical display, CI uses:

```bash
xvfb-run -a ./gradlew clean build -PcaptureScreenshots --no-daemon
```

A normal `./gradlew clean build` runs the 65 unit tests and skips the opt-in rendering test. The complete command above ran **66 tests: 66 passed, 0 failed, 0 skipped**. It also generated the runnable JAR, distribution archives, and installed application scripts.

| Test class | Passed | Main checks |
| --- | ---: | --- |
| AppTest | 33 | Configuration, spawning, timers, scoring, pause, restart, level transitions, input and collision orchestration |
| BallTest | 13 | Motion, reflection, attraction, size restoration and idempotent capture |
| BrickTest | 8 | Colour matching, durability and destruction |
| GameResourcesTest | 8 | Bundled resources, external configuration, relative layouts and explicit failures |
| HoleTest | 2 | Target geometry and colour |
| LineTest | 1 | Barrier point storage |
| GameScreenshotTest | 1 | Actual Processing setup/draw, mouse-drawn barriers, keyboard pause and two rendered captures |

The screenshot test opens a real Processing window; it is not an image mock. The two captures in [images](images/) were visually checked for loaded sprites, visible barriers, queue clipping and readable status text. They are 1152 × 1280 on the tested Retina display, corresponding to a logical 576 × 640 window. Other displays may produce a different pixel density.

## Measured coverage

JaCoCo 0.8.12 reported the following from the complete local run, including the rendering check:

| Metric | Covered / total | Coverage |
| --- | ---: | ---: |
| Lines | 648 / 711 | 91.14% |
| Branches | 353 / 453 | 77.92% |
| Instructions | 3449 / 3779 | 91.27% |
| Methods | 72 / 76 | 94.74% |
| Classes | 6 / 6 | 100% |

The [machine-readable record](verification-results.json) preserves test counts and coverage counters. Local HTML reports are under `build/reports/tests/test/` and `build/reports/jacoco/test/html/`; CI uploads reports and its rendered captures as artifacts. Coverage can vary slightly with rendering paths and platform.

## Packaged launch checks

Both launch methods were executed from an unrelated temporary directory, using absolute paths to the packaged artifacts:

- `java -jar .../build/libs/inkball-arcade-1.0.0.jar`
- `.../build/install/inkball-arcade/bin/inkball-arcade`

Each remained running for six seconds without a startup exception and was then terminated by the verification script. This checks that configuration, maps and sprites do not depend on the working directory. Processing emitted its informational high-density-display notice. These startup checks do not substitute for completing all levels interactively.

## Baseline findings and repairs

The supplied baseline executed 67 tests with **17 failures**, but Gradle reported success because `ignoreFailures = true` was enabled. Several failures came from invalid test fixtures or expectations, including uninitialised Processing drawing state and missing level/spawner setup. The revised suite repairs those fixtures, adds regression coverage, removes an empty sample test and removes a test-only collision implementation. The resulting count should not be interpreted as the same 67 tests with one simply deleted.

Gameplay repairs cover consistent restart/progression state, elapsed-time timers, visible paused balls, wrong-hole re-queuing, stable hole scaling, capture idempotence, and consistent brick destruction. Failed tests now fail the build.

A real packaged launch also exposed a Processing 3.3.7 / modern macOS compatibility failure: `NoClassDefFoundError: com/apple/eawt/QuitHandler`. Upgrading to Processing 4.5.7 resolved the observed failure. Processing documents its supported Java/platform combinations in its [official platform guide](https://github.com/processing/processing4/wiki/Supported-Platforms). The game uses Java2D, so optional OpenGL runtime dependencies are excluded.

## Limits

- Ball displacement remains frame-based at a target of 30 FPS, while the countdown and spawn timer use elapsed time. Slow rendering can affect motion speed.
- Discrete collision detection can miss thin obstacles at extreme custom velocities.
- The automated render check covers representative gameplay and pause screens; it is not a full campaign play-through or a pixel-perfect snapshot assertion.
- Local launch checks were performed on macOS. Linux is checked by the published CI workflow; Windows launch scripts are generated but were not run locally.
- Gradle 8.8 reports deprecated behavior that must be revisited before upgrading to Gradle 9. The included wrapper remains at the tested version.
