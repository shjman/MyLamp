# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context

This is an experimental pet project for personal home use (smart home, see `PLAN.md`) — not a public product and not a production app. It won't be distributed and isn't designed for anyone other than the author on his own home network. This should shape the approach taken: no measures or abstractions meant for a public/multi-tenant product are needed (e.g. strict secret protection from other users, support for multiple configurations, product-grade error handling) — simplicity and speed matter more.

## Rule: don't assume

If anything in a task is unclear (which connection method to use, what data format, what behavior on error, etc.) — ask the user right away instead of picking something and making assumptions. This matters especially in this project because the lamp is controlled through several independent means (WiFi/MQTT, REST API, BLE), each with its own details, and a wrong assumption easily leads to working on the wrong thing.

## Rule: we build this together, not solo

The user wants to actually learn Android/Kotlin development and steps like setting up Home Assistant, not just get a finished result — it matters to him to understand what's happening at each step, and he dislikes "black boxes" (the same stance as in his smart-home notes — see `smarthome.md` → "Rule: explain commands"). This implies:

- Steps the user can do himself by hand (clicks in a third-party service's UI — e.g. Home Assistant, creating tokens, looking up values in a device console, etc.) — don't do them for him silently, even if technically possible (e.g. via browser automation). Spell out step by step what to do and why, and let him do it himself.
- Explain every command/tool that's new to him: what it does, what its flags/parameters/output mean. Don't just hand him a command to copy-paste.
- Don't take over an entire large task by yourself — move through it together step by step, narrating what's being done and why at each one.

## HARD RULES

- Dependency versions — only through `gradle/libs.versions.toml`, never inline in `build.gradle.kts`.
- New dependencies (including new detekt/ktlint rule plugins) — don't add without an explicit request.
- Async — only Kotlin Coroutines/Flow. RxJava is not used in the project and is not to be added (see `PLAN.md`).
- Don't use `!!` (double-bang) — use `requireNotNull(x) { "message" }`, `checkNotNull`, a safe `?.`/`?:`, or an explicit check with a clear error instead.

## Working with the device/emulator

- Tapping the screen of a connected device/emulator (`adb shell input tap`) — only on the user's explicit request and permission. Screenshots (`adb shell screencap`) — free to take, it's read-only.
- Don't reinstall or rebuild the app "just in case" before a screenshot or test, even if the build on the device looks stale — that resets screen state that may have been prepared by hand. If the build looks suspiciously old, first do literally what was asked, and only then separately ask whether to rebuild.
- Any mutating action on the device (`install`, `uninstall`, `am force-stop`, `pm clear`, `logcat -c`) — same logic: ask if it's not literally what was requested.

## Local network access (ACCESS_LOCAL_NETWORK)

This Android version has a separate dangerous permission `android.permission.ACCESS_LOCAL_NETWORK` — without it, any socket connection to addresses like `192.168.1.x` silently hangs until timeout (no explicit refusal is thrown, no `SecurityException`, no cleartext error — just a `SocketTimeoutException` after 10 seconds, as if the network were unreachable). Discovered and confirmed by digging on 2026-08-23 while debugging the REST screen (see `PLAN.md → Журнал` [Journal], `smarthome.md`) — compared a bare `java.net.Socket` against a working request from the browser on the same device/network, then found the cause via `adb shell cmd appops get`.

The permission is declared in `AndroidManifest.xml` and requested once at app startup in `MainActivity.kt` (`MyLampApp`) — already covers all screens (WiFi/REST/BLE), no need to request it separately in each screen. When adding new network calls (WiFi/MQTT, BLE), there's no need to redo this investigation — the permission is already app-wide.

## Local secrets

Passwords, tokens, and other sensitive values are hardcoded in `app/src/main/java/com/yahorshymanchyk/mylamp/secrets/Secrets.kt` — this file is in `.gitignore` and never pushed. Next to it is `Secrets.kt.template` (committed) with the same structure but empty values — update both files when adding a new field. Access secrets from code via `Secrets.<FIELD>`, never hardcode passwords/tokens directly in other files.

## Commands

Build and test from the project root using the Gradle wrapper:

- Build debug APK: `./gradlew assembleDebug`
- Run unit tests (`app/src/test`): `./gradlew testDebugUnitTest`
- Run a single unit test: `./gradlew testDebugUnitTest --tests "com.yahorshymanchyk.mylamp.ExampleUnitTest"`
- Run instrumented tests (`app/src/androidTest`, requires a connected device/emulator): `./gradlew connectedDebugAndroidTest`
- Install debug build on a connected device: `./gradlew installDebug`
- Static analysis: `./gradlew :app:detekt` and `./gradlew :app:ktlintCheck` (baseline in `config/detekt/baseline.xml` grandfathers pre-existing code — only new violations fail; `@Composable` PascalCase names are excepted via `.editorconfig`/detekt config, not treated as naming violations)
- Full check (lint + tests): `./gradlew check`

## Agents (`/swarm`)

For tasks with >50 lines of changes or spanning several files/modules, the project has `.claude/skills/swarm/SKILL.md` — a researcher → executor → reviewer chain with a mandatory pause for plan APPROVE. This file (`CLAUDE.md`) is the single source of HARD RULES for all agents, read by them directly (not `.claude/CLAUDE.md`). Runs only on the user's explicit request, never automatically.

MemPalace (cross-session memory) for this project uses the `SmartHome` wing.

## Serena (semantic code search)

Serena (MCP plugin, enabled via `.claude/settings.json` → `enabledPlugins`) provides semantic code tools (`find_symbol`, `find_referencing_symbols`, etc.), backed by a Kotlin language server. It does **not** auto-activate a project on session start — call `activate_project` with this repo's path before using any other Serena tool, otherwise calls fail with "No active project".

## Architecture

MyLamp is a single-module Android app (`app`, namespace `com.yahorshymanchyk.mylamp`) built with Kotlin and Jetpack Compose. The REST screen and the home-screen widget follow a light Clean Architecture split (UI → ViewModel → Repository → DataSource, no usecase layer, no DI); the WiFi and BLE screens are still local-state-only stub screens and will get the same layering once actually implemented (see `PLAN.md`):

- `MainActivity.kt` — single entry point `ComponentActivity`, sets Compose content directly via `setContent`; also requests `ACCESS_LOCAL_NETWORK` once at startup (see above).
- `ui/theme/` — standard generated Compose theme (`Color.kt`, `Theme.kt`, `Type.kt`) wrapping content in `MyLampTheme`. Unrelated to the widget's own fixed dark palette in `widget/LampWidgetColors.kt`.
- `domain/LampRepository.kt` — the only interface the ViewModel and the widget's `ActionCallback`s depend on (`turnOn(brightnessPct): Result<Unit>`, `turnOff(): Result<Unit>`).
- `data/` — DataSource + Repository: `HomeAssistantApi.kt` (Retrofit interface + request DTOs), `HomeAssistantClient.kt` (OkHttp/Moshi/Retrofit factory, Bearer token from `Secrets`), `HomeAssistantLampRepository.kt` (the sole `LampRepository` implementation; wraps IOException handling, maps to `Result<Unit>`).
- `rest/` — REST screen, UI-only: `RestScreen.kt` (Compose UI, gets state via `viewModel()` + `collectAsState()`), `RestLampViewModel.kt` (owns `StateFlow<RestScreenUiState>`, calls `LampRepository`; instantiates the repository directly in its body — no DI framework), `RestScreenUiState.kt` (sealed interface: `Idle` / `Sending` / `Ready`, not the more common `Loading/Content/Error` — the screen never does a GET on open, so those don't fit).
- `widget/` — Jetpack Glance home-screen widgets (2×2 power-only, 3×2 power + brightness), package `com.yahorshymanchyk.mylamp.widget`: `LampWidget2x2.kt`/`LampWidget3x2.kt` (`GlanceAppWidget`/`GlanceAppWidgetReceiver` pairs), `LampWidgetContent.kt` (shared composables), `LampWidgetColors.kt` (fixed dark palette, `@Suppress("RestrictedApi")` — see below), `LampWidgetState.kt` (Preferences keys/constants), `LampWidgetActions.kt` (`ActionCallback`s calling `LampRepository` via a top-level `private val` — a Glance `AppWidget` has no `ViewModelStoreOwner`, so it cannot use a ViewModel the way the REST screen does), `LampWidgetPreviews.kt` (Android Studio `@Preview`s, IDE-tooling only).
- No dependency injection, navigation, or persistence libraries beyond what's listed above. Dependencies (`gradle/libs.versions.toml`): Compose BOM, core-ktx, lifecycle-runtime-ktx + lifecycle-viewmodel-compose, activity-compose, Retrofit/OkHttp/Moshi (reflection-based, no codegen), Glance (`glance-appwidget` + IDE-only `glance-preview`/`glance-appwidget-preview`), JUnit4, Espresso, and Compose test/tooling artifacts.
- Min SDK 28, target/compile SDK 37, Kotlin 2.4.10, AGP 9.3.1, Gradle 9.7.1, Java 21 source/target compatibility. Versions are kept at the latest stable on each update round (see `PLAN.md` for when this was last done) — check for newer stable releases before assuming these are still current.
- detekt (`config/detekt/detekt.yml` + `config/detekt/baseline.xml`) and ktlint (`.editorconfig`) are wired into `:app` — see Commands above. `UnusedPrivateMember.ignoreAnnotated: ['Preview']` is configured so Compose/Glance `@Preview` functions (tooling-only callers) aren't flagged as dead code.
- Known Android Studio false positive: the IDE's `RestrictedApi` inspection flags `ColorProvider(color = Color(...))` calls in `widget/LampWidgetColors.kt` — verified by inspecting the `androidx.glance.unit.ColorProviderKt` (1.1.1) bytecode that the `Color`-parameter overload has no `@RestrictTo`; only the unrelated `@ColorRes resId` overload is restricted. detekt/ktlint/Android Lint (CLI) don't flag it — suppressed there with `@Suppress("RestrictedApi")` and a comment.
