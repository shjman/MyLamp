# Plan

## Clarified Spec

Ввести Clean Architecture (UI → ViewModel → Repository → DataSource) в уже готовые REST-экран
(`rest/`) и виджет (`widget/`). WiFi/BLE-экраны не трогаем. Конкретно:

- Общий `LampRepository` interface (`domain/`) + `HomeAssistantLampRepository` impl (`data/`).
- `HomeAssistantApi.kt` и `HomeAssistantClient.kt` переносятся из `rest/` в `data/` (они — DataSource,
  не UI-слой, и используются как REST-экраном, так и виджетом).
- `RestLampViewModel` (`rest/`) владеет бизнес-состоянием экрана, обращается к Repository.
- `RestScreen.kt` становится чисто UI: принимает state из ViewModel, отдаёт события.
- `RestScreenUiState` — sealed interface c тремя состояниями: `Idle`, `Sending`, `Ready`.
- Виджет (`LampWidgetActions.kt`) — без ViewModel, обращается к Repository напрямую (как и прежде
  к `HomeAssistantClient.api`, теперь через абстракцию).
- Цвета виджета выносятся из `LampWidgetContent.kt` в новый `LampWidgetColors.kt`.
- Новая зависимость `lifecycle-viewmodel-compose 2.11.0` (тот же версионный ref, что
  `lifecycleRuntimeKtx`) в `libs.versions.toml` + `build.gradle.kts`.
- DI нет, ViewModel инстанцирует Repository в собственном теле (не через конструктор-параметр) —
  это позволяет использовать дефолтный `viewModel()` без кастомной Factory.
- После рефакторинга — живой скриншот виджета на устройстве для визуальной регрессии.

## Context Found

**`rest/HomeAssistantApi.kt`** — Retrofit interface с двумя suspend-методами:
- `turnOn(@Body LightTurnOnRequest): Response<ResponseBody>` (POST `.../turn_on`)
- `turnOff(@Body LightTurnOffRequest): Response<ResponseBody>` (POST `.../turn_off`)
- `LightTurnOnRequest(entityId: String, brightnessPct: Int)` + `LightTurnOffRequest(entityId: String)` — data class-ы в том же файле.

**`rest/HomeAssistantClient.kt`** — `object HomeAssistantClient { val api: HomeAssistantApi by lazy { ... } }`. Инициализирует OkHttp + Moshi + Retrofit, добавляет Bearer-токен из `Secrets.HA_LONG_LIVED_TOKEN`, base URL из `Secrets.HA_BASE_URL`.

**`rest/RestScreen.kt`** — сейчас весь business-state (`isOn`, `brightness`, `statusText`) и логика (`sendTurnOn`, `sendTurnOff`) живут прямо в Composable. `sendTurnOn/sendTurnOff` — top-level private fun, принимающие `CoroutineScope` и callback.

**`widget/LampWidgetState.kt`** — константы (`DEFAULT_BRIGHTNESS_PCT = 50`, `BRIGHTNESS_STEP = 10`, `MAX_BRIGHTNESS_PCT = 100`, `MIN_BRIGHTNESS_PCT_WHILE_ON = 1`), Preferences keys (`isOnKey`, `brightnessPctKey`), data class `LampWidgetUiState(isOn, brightnessPct)`. Всё остаётся на месте, не трогаем.

**`widget/LampWidgetActions.kt`** — два `ActionCallback`: `TogglePowerAction` и `AdjustBrightnessAction`. Обращаются к `HomeAssistantClient.api` напрямую через two private suspend fun (`callTurnOn`, `callTurnOff`). Импортируют `HomeAssistantClient`, `LightTurnOnRequest`, `LightTurnOffRequest`, `Secrets` из `rest/` пакета.

**`widget/LampWidgetContent.kt`** — шесть `private val …Color = ColorProvider(color = Color(0xFF…))` захардкожены прямо в файле. Все 6 используются в `LampWidgetCard`, `PowerButton`, `BrightnessRow`, `BrightnessStepButton`, `StatusLabel`.

**`gradle/libs.versions.toml`** — `lifecycleRuntimeKtx = "2.11.0"` уже есть. `lifecycle-viewmodel-compose` добавляем как новую запись в `[libraries]` с `version.ref = "lifecycleRuntimeKtx"`.

**`app/build.gradle.kts`** — `implementation(libs.androidx.lifecycle.runtime.ktx)` уже есть. Добавляем аналогичную строку для `viewmodel-compose`.

**`MainActivity.kt`** — `RestScreen(modifier, onBack)` вызывается без параметров ViewModel — сигнатура `RestScreen` не меняется, ViewModel создаётся внутри самого Composable через `viewModel()`.

## Questions for User

Разрешено. Пользователь одобрил раскладку пакетов:
- `HomeAssistantApi.kt`/`HomeAssistantClient.kt` переезжают из `rest/` в новый `data/`.
- `data/HomeAssistantLampRepository.kt` — новый, реализует `LampRepository`.
- `domain/LampRepository.kt` — новый интерфейс.
- `rest/` и `widget/` остаются feature-пакетами только для UI/ViewModel/Actions.

## next_step
plan

---

## Implementation Plan

### Approach

Выносим DataSource (`HomeAssistantApi`, `HomeAssistantClient`) и Repository (`LampRepository` interface +
`HomeAssistantLampRepository` impl) в отдельные top-level пакеты `domain/` и `data/`. `RestScreen`
делегирует всю логику `RestLampViewModel`, который обращается к `LampRepository`. Виджетные
`ActionCallback`-классы используют тот же `LampRepository` напрямую (без ViewModel). Цвета виджета
переезжают в отдельный `LampWidgetColors` object. DI нет — всё инстанцируется прямо (ViewModel
создаёт Repository в теле, `LampWidgetActions.kt` — в top-level `val`).

### Files to Create

| Путь | Причина |
|------|---------|
| `app/src/main/java/com/yahorshymanchyk/mylamp/domain/LampRepository.kt` | Interface Repository — единственная точка зависимости для ViewModel и ActionCallback |
| `app/src/main/java/com/yahorshymanchyk/mylamp/data/HomeAssistantApi.kt` | DataSource (Retrofit interface + request DTOs) — перемещён из `rest/`, обновлён package |
| `app/src/main/java/com/yahorshymanchyk/mylamp/data/HomeAssistantClient.kt` | DataSource (OkHttp/Retrofit factory) — перемещён из `rest/`, обновлён package |
| `app/src/main/java/com/yahorshymanchyk/mylamp/data/HomeAssistantLampRepository.kt` | Единственная реализация `LampRepository`; инкапсулирует `HomeAssistantClient.api`, try/catch IOException, маппинг в `Result<Unit>` |
| `app/src/main/java/com/yahorshymanchyk/mylamp/rest/RestScreenUiState.kt` | Sealed interface UI-состояния экрана |
| `app/src/main/java/com/yahorshymanchyk/mylamp/rest/RestLampViewModel.kt` | ViewModel REST-экрана |
| `app/src/main/java/com/yahorshymanchyk/mylamp/widget/LampWidgetColors.kt` | Вынесенные цвета виджета |

### Files to Change

| Путь | Что меняем |
|------|------------|
| `gradle/libs.versions.toml` | Добавить запись `androidx-lifecycle-viewmodel-compose` в `[libraries]` |
| `app/build.gradle.kts` | Добавить `implementation(libs.androidx.lifecycle.viewmodel.compose)` |
| `app/src/main/java/com/yahorshymanchyk/mylamp/rest/RestScreen.kt` | Убрать весь business-state и private fun; получать state из ViewModel, отдавать события через ViewModel-методы |
| `app/src/main/java/com/yahorshymanchyk/mylamp/widget/LampWidgetActions.kt` | Заменить прямые вызовы `HomeAssistantClient.api` на `lampRepository: LampRepository`; убрать import'ы из `rest/` |
| `app/src/main/java/com/yahorshymanchyk/mylamp/widget/LampWidgetContent.kt` | Заменить 6 локальных `private val …Color` на `LampWidgetColors.*` |

### Files to Delete

| Путь | Причина |
|------|---------|
| `app/src/main/java/com/yahorshymanchyk/mylamp/rest/HomeAssistantApi.kt` | Содержимое переезжает в `data/HomeAssistantApi.kt` |
| `app/src/main/java/com/yahorshymanchyk/mylamp/rest/HomeAssistantClient.kt` | Содержимое переезжает в `data/HomeAssistantClient.kt` |

### Steps

**1. `gradle/libs.versions.toml` — добавить библиотеку**

В секцию `[libraries]` добавить строку после `androidx-lifecycle-runtime-ktx`:
```toml
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
```
Версионный ref переиспользует уже существующий `lifecycleRuntimeKtx = "2.11.0"` — новую версионную
запись добавлять не нужно.

**2. `app/build.gradle.kts` — добавить зависимость**

В блок `dependencies`, рядом с `androidx-lifecycle-runtime-ktx`:
```kotlin
implementation(libs.androidx.lifecycle.viewmodel.compose)
```

**3. Создать `domain/LampRepository.kt`**

```kotlin
package com.yahorshymanchyk.mylamp.domain

interface LampRepository {
    suspend fun turnOn(brightnessPct: Int): Result<Unit>
    suspend fun turnOff(): Result<Unit>
}
```

**4. Создать `data/HomeAssistantApi.kt`**

Файл идентичен `rest/HomeAssistantApi.kt`, но с `package com.yahorshymanchyk.mylamp.data`.
Содержит: `LightTurnOnRequest`, `LightTurnOffRequest`, `HomeAssistantApi` interface — всё без изменений.

**5. Создать `data/HomeAssistantClient.kt`**

Файл идентичен `rest/HomeAssistantClient.kt`, но:
- `package com.yahorshymanchyk.mylamp.data`
- import `Secrets` остаётся
- Никакого импорта из `rest/`

**6. Создать `data/HomeAssistantLampRepository.kt`**

```kotlin
package com.yahorshymanchyk.mylamp.data

import android.util.Log
import com.yahorshymanchyk.mylamp.domain.LampRepository
import com.yahorshymanchyk.mylamp.secrets.Secrets
import java.io.IOException

private const val TAG = "HomeAssistantLampRepo"

class HomeAssistantLampRepository : LampRepository {

    private val api = HomeAssistantClient.api
    private val entityId = Secrets.HA_LAMP_ENTITY_ID

    override suspend fun turnOn(brightnessPct: Int): Result<Unit> =
        try {
            Log.i(TAG, "turnOn: brightnessPct=$brightnessPct")
            val response = api.turnOn(LightTurnOnRequest(entityId = entityId, brightnessPct = brightnessPct))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(RuntimeException("HTTP ${response.code()}"))
        } catch (e: IOException) {
            Log.e(TAG, "turnOn failed", e)
            Result.failure(e)
        }

    override suspend fun turnOff(): Result<Unit> =
        try {
            Log.i(TAG, "turnOff")
            val response = api.turnOff(LightTurnOffRequest(entityId = entityId))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(RuntimeException("HTTP ${response.code()}"))
        } catch (e: IOException) {
            Log.e(TAG, "turnOff failed", e)
            Result.failure(e)
        }
}
```

Оба метода умещаются в ~10 строк каждый — ниже порогов LongMethod/ComplexMethod detekt.

**7. Создать `rest/RestScreenUiState.kt`**

```kotlin
package com.yahorshymanchyk.mylamp.rest

// Idle  — экран только открылся, команд ещё не отправляли
// Sending — команда в полёте, controls заблокированы
// Ready  — последняя команда завершилась (успешно или с ошибкой)
sealed interface RestScreenUiState {
    data object Idle : RestScreenUiState

    data class Sending(
        val isOn: Boolean,
        val brightnessPct: Int,
    ) : RestScreenUiState

    data class Ready(
        val isOn: Boolean,
        val brightnessPct: Int,
        val statusMessage: String,
    ) : RestScreenUiState
}
```

Обоснование набора состояний: экран не читает состояние лампы из HA при открытии (нет
GET-запроса), поэтому классический `Loading/Content/Error` избыточен. `Idle` — начальный
разомкнутый статус; `Sending` — блокировка UI на время запроса; `Ready` — живое состояние
с feedback от последней команды (текст Success или Error внутри `statusMessage`).

**8. Создать `rest/RestLampViewModel.kt`**

```kotlin
package com.yahorshymanchyk.mylamp.rest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yahorshymanchyk.mylamp.data.HomeAssistantLampRepository
import com.yahorshymanchyk.mylamp.domain.LampRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DEFAULT_BRIGHTNESS_PCT = 50

class RestLampViewModel : ViewModel() {

    // Repository инстанцируется в теле — no-arg ViewModel, работает с дефолтным viewModel()
    private val repository: LampRepository = HomeAssistantLampRepository()

    private val _uiState = MutableStateFlow<RestScreenUiState>(RestScreenUiState.Idle)
    val uiState: StateFlow<RestScreenUiState> = _uiState.asStateFlow()

    // Текущие значения храним отдельно, чтобы Sending-состояние могло их передать в UI
    private var currentIsOn = false
    private var currentBrightnessPct = DEFAULT_BRIGHTNESS_PCT

    fun onTogglePower(isOn: Boolean) {
        currentIsOn = isOn
        _uiState.value = RestScreenUiState.Sending(isOn, currentBrightnessPct)
        viewModelScope.launch {
            val result = if (isOn) repository.turnOn(currentBrightnessPct) else repository.turnOff()
            _uiState.value = result.toReadyState(currentIsOn, currentBrightnessPct, isOn)
        }
    }

    fun onBrightnessChanged(brightnessPct: Int) {
        currentBrightnessPct = brightnessPct
        if (!currentIsOn) return
        _uiState.value = RestScreenUiState.Sending(currentIsOn, brightnessPct)
        viewModelScope.launch {
            val result = repository.turnOn(brightnessPct)
            _uiState.value = result.toReadyState(currentIsOn, brightnessPct, null)
        }
    }
}

// Вспомогательная функция — отдельно чтобы не раздувать методы ViewModel
private fun Result<Unit>.toReadyState(
    isOn: Boolean,
    brightnessPct: Int,
    toggledIsOn: Boolean?,  // non-null только для toggle, чтобы сформировать текст
): RestScreenUiState.Ready =
    RestScreenUiState.Ready(
        isOn = isOn,
        brightnessPct = brightnessPct,
        statusMessage = fold(
            onSuccess = {
                if (toggledIsOn != null) {
                    if (toggledIsOn) "OK: включено, яркость $brightnessPct%" else "OK: выключено"
                } else {
                    "OK: яркость $brightnessPct%"
                }
            },
            onFailure = { "Ошибка: ${it.javaClass.simpleName}: ${it.message}" },
        ),
    )
```

Оба public метода — < 10 строк. `toReadyState` выделена в private extension, чтобы не превышать
LongMethod/ComplexMethod лимиты detekt.

**9. Рефакторинг `rest/RestScreen.kt`**

- Добавить `import androidx.lifecycle.viewmodel.compose.viewModel`.
- Использовать `collectAsState()` из стандартного Compose (`androidx.compose.runtime`) — не
  `collectAsStateWithLifecycle` (она из `lifecycle-runtime-compose`, который не добавляем).
- В теле `RestScreen`: `val viewModel: RestLampViewModel = viewModel()` + `val uiState by viewModel.uiState.collectAsState()`.
- Удалить все `remember { mutableStateOf/mutableFloatStateOf }` для `isOn`, `brightness`, `statusText`.
- Оставить `var sliderPosition by remember { mutableFloatStateOf(DEFAULT_BRIGHTNESS_PERCENT) }` —
  это локальное транзиентное UI-состояние для плавного drag без сетевых лагов. Синхронизировать
  его с `uiState` через `LaunchedEffect(uiState)` (обновлять `sliderPosition` когда uiState содержит
  новый `brightnessPct`, кроме состояния `Sending` — во время отправки не сбрасывать).
- Switch `onCheckedChange`: вызывает `viewModel.onTogglePower(checked)`.
- Slider `onValueChange`: обновляет только `sliderPosition` (локальный).
- Slider `onValueChangeFinished`: вызывает `viewModel.onBrightnessChanged(sliderPosition.roundToInt())`.
- `isOn` для Switch берётся из `uiState` (через when на sealed типах — fallback `false` для Idle).
- `statusText` берётся из `uiState` (Ready → `statusMessage`, Sending → "Отправка...", Idle → начальная строка).
- Удалить private fun `sendTurnOn`, `sendTurnOff`.
- Удалить import `rememberCoroutineScope`, `CoroutineScope`, `launch`.
- Удалить import `com.yahorshymanchyk.mylamp.secrets.Secrets` (больше не нужен в UI).
- Константу `DEFAULT_BRIGHTNESS_PERCENT` оставить в `RestScreen.kt` — нужна для начального значения `sliderPosition`.

Функция `RestScreen` остаётся не длиннее ~50 строк. Если растёт — вынести inner composable в private fun.

**10. Создать `widget/LampWidgetColors.kt`**

```kotlin
package com.yahorshymanchyk.mylamp.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

// Fixed dark theme for widget — not tied to system dynamic color.
// @Suppress("RestrictedApi"): Android Studio IDE inspection false-positive on ColorProvider(Color).
// ColorProviderKt bytecode (glance 1.1.1) shows no @RestrictTo on the Color overload — only the
// adjacent @ColorRes resId overload is restricted. detekt/ktlint/Android Lint (CLI) do not flag this.
@Suppress("RestrictedApi")
internal object LampWidgetColors {
    val CardBackground = ColorProvider(color = Color(color = 0xFF1C1C1E))
    val Accent = ColorProvider(color = Color(color = 0xFFFFB300))
    val PrimaryText = ColorProvider(color = Color(color = 0xFFFFFFFF))
    val MutedText = ColorProvider(color = Color(color = 0xFF8A8A8E))
    val StepButtonSurface = ColorProvider(color = Color(color = 0xFF3A3A3C))
    val StepButtonSurfaceMuted = ColorProvider(color = Color(color = 0xFF2A2A2C))
}
```

Имена без суффикса `Color` (был `CardBackgroundColor` → теперь `LampWidgetColors.CardBackground`) —
call-site читается как `LampWidgetColors.CardBackground`, что само по себе говорит о цвете.

**11. Рефакторинг `widget/LampWidgetContent.kt`**

- Удалить 6 `private val …Color = ColorProvider(color = Color(0xFF…))` из начала файла.
- Заменить каждое использование:
  - `CardBackgroundColor` → `LampWidgetColors.CardBackground`
  - `AccentColor` → `LampWidgetColors.Accent`
  - `PrimaryTextColor` → `LampWidgetColors.PrimaryText`
  - `MutedTextColor` → `LampWidgetColors.MutedText`
  - `StepButtonSurfaceColor` → `LampWidgetColors.StepButtonSurface`
  - `StepButtonSurfaceMutedColor` → `LampWidgetColors.StepButtonSurfaceMuted`
- Import не нужен — тот же пакет `widget`.

**12. Рефакторинг `widget/LampWidgetActions.kt`**

- Добавить top-level `private val lampRepository: LampRepository = HomeAssistantLampRepository()`.
- Заменить `callTurnOn(brightnessPct)` → `lampRepository.turnOn(brightnessPct).isSuccess`.
- Заменить `callTurnOff()` → `lampRepository.turnOff().isSuccess`.
- Удалить private suspend fun `callTurnOn` и `callTurnOff`.
- Удалить imports: `com.yahorshymanchyk.mylamp.rest.HomeAssistantClient`, `...LightTurnOnRequest`,
  `...LightTurnOffRequest`, `com.yahorshymanchyk.mylamp.secrets.Secrets`.
- Добавить imports: `com.yahorshymanchyk.mylamp.data.HomeAssistantLampRepository`,
  `com.yahorshymanchyk.mylamp.domain.LampRepository`.
- Удалить `import android.util.Log` если Log больше не используется напрямую (Log теперь внутри Repository).

**13. Удалить старые файлы из `rest/`**

- `app/src/main/java/com/yahorshymanchyk/mylamp/rest/HomeAssistantApi.kt`
- `app/src/main/java/com/yahorshymanchyk/mylamp/rest/HomeAssistantClient.kt`

**14. Проверка билда и статического анализа**

```bash
./gradlew compileDebugKotlin        # первым — быстро поймает compile-errors
./gradlew :app:detekt
./gradlew :app:ktlintCheck
./gradlew assembleDebug             # полный APK
./gradlew installDebug              # установка на устройство
```

**15. Живой скриншот виджета**

После `installDebug` — `adb shell screencap -p /sdcard/widget_after_ca.png && adb pull /sdcard/widget_after_ca.png` — визуально убедиться, что виджет рендерится идентично до-рефакторинговому состоянию (цвета, кнопки, текст).

### Validation Criteria

- `./gradlew compileDebugKotlin` без ошибок.
- `./gradlew :app:detekt` без новых нарушений (baseline грандфазерит существующий код).
- `./gradlew :app:ktlintCheck` без новых нарушений.
- `RestScreen.kt` не содержит прямых вызовов сетевых API, не импортирует `HomeAssistantClient` / `Secrets`.
- `LampWidgetActions.kt` не импортирует из `rest/` пакета.
- `LampWidgetContent.kt` не содержит `ColorProvider(color = Color(0x…))` inline — только ссылки на `LampWidgetColors.*`.
- Живой скриншот подтверждает: виджет визуально идентичен предыдущему (нет регрессий по цветам, тексту, кнопкам).
- Проходит `:app:detekt` и `:app:ktlintCheck` без новых нарушений (baseline не прощает новый код).

### Out of Scope

- WiFi/BLE-экраны — не трогаем; получат ту же архитектуру при реализации.
- Dependency Injection (Hilt/Koin) — явно не входит в фазу 1.
- Тесты (unit/instrumented) — отдельная задача, не часть этого рефакторинга.
- Чтение реального состояния лампы из HA при открытии экрана (GET-запрос) — Out of Scope.
- Persisting REST screen state across process death — Out of Scope.
- Системная тема / динамические цвета для виджета — Out of Scope (по PLAN.md).
