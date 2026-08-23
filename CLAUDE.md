# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build and test from the project root using the Gradle wrapper:

- Build debug APK: `./gradlew assembleDebug`
- Run unit tests (`app/src/test`): `./gradlew testDebugUnitTest`
- Run a single unit test: `./gradlew testDebugUnitTest --tests "com.yahorshymanchyk.mylamp.ExampleUnitTest"`
- Run instrumented tests (`app/src/androidTest`, requires a connected device/emulator): `./gradlew connectedDebugAndroidTest`
- Install debug build on a connected device: `./gradlew installDebug`
- Full check (lint + tests): `./gradlew check`

## Architecture

MyLamp is a single-module Android app (`app`, namespace `com.yahorshymanchyk.mylamp`) built with Kotlin and Jetpack Compose — currently the stock Android Studio "Empty Activity" template with no custom architecture layered on yet:

- `MainActivity.kt` — single entry point `ComponentActivity`, sets Compose content directly via `setContent`.
- `ui/theme/` — standard generated Compose theme (`Color.kt`, `Theme.kt`, `Type.kt`) wrapping content in `MyLampTheme`.
- No dependency injection, navigation, networking, or persistence libraries are wired in yet — dependencies (`gradle/libs.versions.toml`) are limited to Compose BOM, core-ktx, lifecycle-runtime-ktx, activity-compose, JUnit4, Espresso, and Compose test/tooling artifacts.
- Min SDK 28, target/compile SDK 36, Kotlin 2.2.10, AGP 9.3.1, Java 11 source/target compatibility.
