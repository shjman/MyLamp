# MyLamp — план разработки

_Создано: 2026-08-23_

## Список задач (по порядку, как просил пользователь)

1. ✅ **Warning на `targetSdk = 37`** — исправлено. Был реальный lint `InlinedApi` на `ACCESS_LOCAL_NETWORK` (minSdk 28 < 37). Обёрнуто в `Build.VERSION.SDK_INT >= LOCAL_NETWORK_PERMISSION_MIN_SDK` в `MainActivity.kt`.
2. ✅ **Виджет на рабочий стол (Jetpack Glance)** — готово, `/swarm`-цепочка завершена с `PASS`, виджеты размещены пользователем на рабочем столе и визуально проверены. Детали — в разделе "Виджет — итоговый статус" ниже.
3. ✅ **Перевести все комментарии в коде проекта на английский** — готово 2026-08-23. Переведены только комментарии (`//`, `/* */`, KDoc) в `MainActivity.kt`, `rest/RestLampViewModel.kt`, `rest/RestScreenUiState.kt`, `secrets/Secrets.kt`/`.template`; UI-строки/сообщения статуса (видимые пользователю тексты на русском — заголовки экранов, `statusMessage` и т.п.) намеренно не трогались, это отдельная, не запрошенная задача. `compileDebugKotlin`/`detekt`/`ktlintCheck` зелёные после правок.
4. ✅ **Перевести файлы `/Users/yahorshymanchyk/AndroidStudioProjects/MyLamp/.claude/agents/*.md` и `.claude/skills/swarm/SKILL.md` на английский** — готово 2026-08-23 (через фоновый агент), проверено `grep`-ом на отсутствие кириллицы во всех 5 файлах.
5. ➖ `/Users/yahorshymanchyk/all in/claude/smarthome.md` — **оставить на русском**, не трогать (явное указание пользователя).
6. ✅ **Перевести `/Users/yahorshymanchyk/AndroidStudioProjects/MyLamp/CLAUDE.md` на английский** — готово 2026-08-23. Заодно актуализирован раздел `## Architecture` (был устаревшим — описывал состояние до Clean Architecture рефакторинга задачи #7); см. сам файл, здесь не дублируется.
7. ✅ **Clean Architecture рефакторинг (REST-экран + виджет)** — готово, `/swarm`-цепочка (researcher → APPROVE → executor → reviewer) завершена с `PASS` за 1 итерацию. Детали — в разделе "Clean Architecture — статус" ниже.
8. ⏳ **Иконка питания на виджете должна менять цвет в зависимости от состояния лампы** — добавлено пользователем 2026-08-23, в бэклоге (пользователь допустил вариант "или как минимум положить в план"). Следующая задача в очереди. Причина: сейчас единственный визуальный индикатор ON/OFF — текстовая надпись в шапке (`StatusLabel`), сама круглая кнопка питания (`PowerButton` в `LampWidgetContent.kt`) всегда одного и того же цвета (`LampWidgetColors.Accent`, янтарный фон + тёмная иконка) независимо от состояния — неудобно ориентироваться только по надписи. Предполагаемое решение (не согласовано, требует своего APPROVAL GATE при реализации): например, иконка/фон `PowerButton` меняет цвет в зависимости от `state.isOn` — конкретную палитру для OFF-состояния нужно обсудить отдельно.

Пользователь просил делать всё **последовательно** и запускать `/swarm` при сомнениях, спрашивать на каждом шаге.

### Пересмотр решения "без архитектуры в фазе 1"

Изначально в этом файле (см. секцию "Осознанно НЕ делаем в фазе 1" ниже) было явно решено не делать ViewModel/Repository/DI в фазе 1 — 3 простых независимых экрана без общей архитектуры. Пользователь **осознанно пересмотрел** это решение 2026-08-23: после того как заработали REST-экран и виджет (оба независимо дублируют один и тот же паттерн REST-вызова к Home Assistant), решено ввести Clean Architecture до того, как будут добавлены WiFi/BLE-экраны. Старый пункт ниже оставлен как есть для истории (там же и объяснение, почему изначально решили не делать) — см. новый раздел "Clean Architecture — статус" за актуальным решением.

### Clean Architecture — статус (задача #7)

Запрошено пользователем 2026-08-23. Объём — **только REST-экран (`rest/`) + виджет (`widget/`)**, то есть то, что уже реально реализовано; WiFi/BLE-экраны ещё не начаты и получат такую же структуру уже сразу при реализации, без отдельного повторного рефакторинга.

Ключевые решения (зафиксированы, не переспрашивать):
- Слои: **UI → ViewModel → Repository → DataSource**. Бизнес-логики в UI-слое быть не должно.
- **Без usecase-слоя** — ViewModel обращается к Repository напрямую.
- **Отдельный `sealed class`/`sealed interface` для UI-состояния** экрана (например `Loading`/`Content`/`Error` — точный набор состояний решает researcher/план).
- **Виджет не может иметь ViewModel** (у Glance `AppWidget`/`ActionCallback` нет `ViewModelStoreOwner`) — виджет обращается к Repository/DataSource напрямую, минуя слой ViewModel. Слой ViewModel — только для Compose-экранов с Activity.
- **Отдельный package на каждый слой** — точную раскладку (layer-first vs feature-first с вложенными слоями) предлагает researcher в плане, пользователь утверждает на APPROVAL GATE.
- **Нейминг — не минимально короткий, а описательный** (например `RestLampRepository`, а не просто `Repository`) — и для пакетов, и для файлов/классов.
- Цвета виджета (`CardBackgroundColor`, `AccentColor`, `PrimaryTextColor`, `MutedTextColor`, `StepButtonSurfaceColor`, `StepButtonSurfaceMutedColor` — сейчас захардкожены прямо в `LampWidgetContent.kt`) — вынести в отдельный файл-класс вроде `Color.kt`/theme, наружу тянуть только alias'ы, не хардкодить `ColorProvider(Color(0x...))` по месту использования.
- Новая зависимость **`androidx.lifecycle:lifecycle-viewmodel-compose`** — нужна для ViewModel в Compose-экранах (сейчас в `libs.versions.toml` есть только `lifecycle-runtime-ktx`), пользователь одобрил её добавление явно в рамках этой задачи.
- Побочная деталь, не относится напрямую к архитектуре, но всплыла в этом же разговоре: Android Studio показывает ложное предупреждение `RestrictedApi` на вызовах `ColorProvider(color = Color(...))` в `LampWidgetContent.kt` — проверено байткодом `androidx.glance.unit.ColorProviderKt` (1.1.1), у перегрузки с `Color`-параметром реально нет `@RestrictTo`, restricted — только соседняя перегрузка с `@ColorRes resId: Int`, которую мы не используем. Detekt/ktlint/Android Lint (CLI) эту проблему не видят вообще — это чисто IDE-инспекция, ложное срабатывание из-за перегрузки одноимённых функций в одном файле. Можно по пути добавить `@Suppress("RestrictedApi")` при переносе в новый Color-файл, если удобно, не обязательно.

Дальше — по `/swarm`: `researcher` составит план (включая раскладку пакетов и точный набор sealed-состояний), пользователь даёт APPROVE, `executor` реализует, `reviewer` проверяет + обязательный живой скриншот виджета на устройстве после (по уроку из задачи #2 — reviewer не ловит визуальные баги).

**Итог — 1 итерация, `PASS`:**
- Researcher сначала вернул `next_step: clarification` с одним вопросом — раскладка пакетов (перенести `HomeAssistantApi`/`HomeAssistantClient` из `rest/` в новый `data/`, чтобы `widget/` не зависел от `rest/`). Пользователь согласился с предложенной структурой, researcher зафиксировал `next_step: plan`.
- Финальная структура: `domain/LampRepository.kt` (интерфейс) → `data/HomeAssistantApi.kt` + `data/HomeAssistantClient.kt` (перенесены из `rest/`) + `data/HomeAssistantLampRepository.kt` (единственная реализация, try/catch `IOException`, возвращает `Result<Unit>`) → `rest/RestLampViewModel.kt` (`StateFlow<RestScreenUiState>`) → `rest/RestScreen.kt` (чистый UI, `viewModel()` + `collectAsState()`). `RestScreenUiState` — sealed interface `Idle`/`Sending(isOn, brightnessPct)`/`Ready(isOn, brightnessPct, statusMessage)` (не классический `Loading/Content/Error`, т.к. экран не делает GET при открытии).
- Виджет (`widget/LampWidgetActions.kt`) обращается к `LampRepository` напрямую через top-level `private val` — без ViewModel, как и решили (у Glance `ActionCallback` нет `ViewModelStoreOwner`).
- Цвета виджета вынесены в `widget/LampWidgetColors.kt` (`internal object`, 6 `ColorProvider`-значений), с `@Suppress("RestrictedApi")` — подавлено то самое ложное IDE-предупреждение.
- Новая зависимость `androidx-lifecycle-viewmodel-compose` добавлена в `libs.versions.toml`, переиспользует существующий `version.ref = lifecycleRuntimeKtx` (новую версию не заводили).
- `rest/HomeAssistantApi.kt`/`HomeAssistantClient.kt` удалены (переехали в `data/`).
- Executor задокументировал 8 minor deviations — все ktlint/detekt форматирование (переносы строк, пустые строки, неиспользуемый `TAG`, `@file:Suppress("MatchingDeclarationName")` для `LampWidgetContent.kt`), ни одна не архитектурная.
- `compileDebugKotlin`/`detekt`/`ktlintCheck` зелёные, направление зависимостей проверено reviewer'ом построчно (domain ни от чего не зависит, data → domain+secrets, rest/widget → data+domain, widget не импортирует rest). Живой скриншот виджета (сделан и executor'ом, и отдельно перепроверен после `PASS`) подтвердил визуальную идентичность до/после рефакторинга — регрессий нет.
- MemPalace прогнан (`wing SmartHome`, 4 файла контекста).

### Виджет — итоговый статус (задача #2)

Дизайн взят из Claude Design проекта `3dff2625-25d2-439e-8836-1b27137a2aa0`, файл `lamp widget.dc.html` (доступ через `DesignSync` tool, `get_file`).

Ключевые решения:
- Библиотека — **Jetpack Glance**, стабильная версия **1.1.1**
- Два виджета как **отдельные pickable-виджеты** в системном пикере (2×2 и 3×2), НЕ один resizable
- Яркость в виджете — **кнопки `−10%`/`+10%`**, НЕ слайдер (в Glance 1.1.1 нет composable `Slider`)
- **minSdk остался 28**
- Заодно переименована кнопка/заголовок REST-экрана: `"REST (Home Assistant)"` → `"REST (локальная сеть)"`
- Физический размер обоих виджетов в пикере (`minWidth`/`minHeight` в XML) не менялся — все доработки ниже это только про содержимое карточки, не про размер в системном пикере

**Итог цепочки `/swarm` — 2 итерации:**
- Итерация 1: executor реализовал всё по плану (новый пакет `widget/`: `LampWidgetState.kt`, `LampWidgetActions.kt`, `LampWidgetContent.kt`, `LampWidget2x2.kt`, `LampWidget3x2.kt`, XML info-файлы, manifest-ресиверы), сборка/детект/ktlint зелёные. Автоматический reviewer дал `PASS` по коду — но живой скриншот на Pixel 6 (по просьбе пользователя) показал 2 визуальных бага, которые статический анализ не ловит:
  1. Иконка питания — Unicode-глиф `⏻` не поддерживался системным шрифтом устройства → рендерилась как квадрат-заглушка.
  2. Строка яркости `− NN% +` (и статус ON/OFF на 2×2) не помещалась в `minHeight=110dp` и обрезалась RemoteViews.
- Итерация 2: executor исправил оба — заменил Unicode-глиф на новый `res/drawable/ic_widget_power.xml` (vector), и вместо увеличения `minHeight` (что сломало бы деление на 2×2/3×2) уменьшил внутренние отступы/размер кнопки, чтобы контент влез в 110dp. Reviewer подтвердил `PASS`.
- Живой скриншот после фикса подтвердил: иконка питания рендерится корректно, строка `− 50% +` и статус ON/OFF видны на обоих виджетах, кнопка питания реально переключает лампу (подтверждено визуально — второй виджет показал `ON`).
- MemPalace прогнан (`wing SmartHome`, 4 файла контекста, 53 записи).

**Доработки макета после `/swarm` (2026-08-23, тот же день, напрямую в чате — задача маленькая, цепочка избыточна):**
- Кнопка питания и управление яркостью объединены в один ряд на 3×2 (раньше были друг под другом); статус ON/OFF перенесён в шапку на обоих размерах (раньше — только на 3×2, у 2×2 был внизу рядом с яркостью).
- Первая попытка увеличить кнопки `−`/`+` и добавить в тот же ряд кнопку питания на 2×2 не влезла по ширине (110dp) — `+` обрезался. Решение: у **2×2 яркость убрана совсем**, остался только крупный (64dp) toggle питания; у **3×2** (180dp, ширины хватает) — питание + яркость в одном ряду, кнопки `−`/`+` крупные (20sp, área нажатия 36×36dp) и получили видимую поверхность-подложку (закруглённый прямоугольник) — в базовом Glance API нет modifier для просто обводки/border без заливки, поэтому "границы кнопки" сделаны через `background+cornerRadius`, а не через stroke.
- Логика яркости: `AdjustBrightnessAction` (`LampWidgetActions.kt`) теперь не даёт `−` увести яркость до 0% пока лампа включена — минимум **1%** (`MIN_BRIGHTNESS_PCT_WHILE_ON = 1` в `LampWidgetState.kt`, заменил старый `MIN_BRIGHTNESS_PCT = 0`). Причина: `brightness_pct=0` в Home Assistant означает "выключить", что рассинхронило бы статус ON на виджете с реальным состоянием лампы.
- Добавлен `@Preview` для Android Studio (новый файл `LampWidgetPreviews.kt`, 4 превью-функции: 2×2/3×2 × on/off) — потребовало новых зависимостей `androidx.glance:glance-preview` и `glance-appwidget-preview` (1.1.1, добавлены в `libs.versions.toml`/`app/build.gradle.kts` по явному запросу пользователя). Заодно добавлен `ignoreAnnotated: ['Preview']` в `UnusedPrivateMember` в `config/detekt/detekt.yml` — иначе detekt считает превью-функции мёртвым кодом (вызываются только IDE-tooling'ом, не кодом проекта); это правка общего конфига проекта, не только виджета. Рендер `@Preview` в самой панели Android Studio не проверялся — это IDE-фича, недоступна из CLI, только `compileDebugKotlin` подтверждает, что аннотация и `@OptIn(ExperimentalGlancePreviewApi::class)` компилируются.
- Все правки проверены: `compileDebugKotlin`/`ktlintCheck`/`detekt` зелёные, `installDebug` на Pixel 6, макет подтверждён живым скриншотом после каждой итерации (3 скриншот-цикла подряд, пока ширина 2×2 не перестала обрезать контент).

Рабочий план для Android-приложения управления лампой. Контекст инфраструктуры (Pi, MQTT, Home Assistant, устройства) — в `/Users/yahorshymanchyk/all in/claude/smarthome.md`, здесь не дублируется, только то, что напрямую касается приложения.

## Итоговая цель

Приложение, умеющее управлять лампой несколькими независимыми способами: напрямую по WiFi (MQTT), через REST API (Home Assistant), и по BLE напрямую к Raspberry Pi. Это отдельно от уже намеченного в smarthome.md большого Android-роадмапа (этапы 0-5: Glance-виджет → Tailscale → port forwarding → FastAPI backend → Bluetooth Classic → BLE GATT) — экран BLE в фазе 1 фактически забирает себе идею этапа 5 из того роадмапа заранее, не дожидаясь этапов 1-4.

## Целевое устройство

Лампа `lamp_cabinet` — NOUS P3Z (Zigbee TS0505B), топик `zigbee2mqtt/0xa4c138ec052f4cce/set`, payload вида `{"state":"ON"}`, `{"brightness":0-254}`. Сама лампа BLE не поддерживает — BLE-экран будет говорить не с лампой напрямую, а с Raspberry Pi, который дальше сам публикует в MQTT (единый источник правды по-прежнему MQTT, независимо от экрана).

## Фаза 1 — MVP, 3 экрана, всё захардкожено

### Общее для всех экранов
- Стек: Jetpack Compose (UI) + Kotlin Coroutines (вся асинхронщина — MQTT/HTTP/BLE вызовы), без RxJava
- 3 простых экрана в одном Compose NavHost (или TabRow) — WiFi / REST / BLE
- На каждом экране: Switch on/off + Slider яркости (0–100%)
- Никакого сохранения настроек/токенов через UI — все адреса/токены/топики хардкодятся в коде
- ~~Каждый экран независим, без общего ViewModel/Repository — это 3 демо разных транспортов, а не общая архитектура~~ — **пересмотрено 2026-08-23** для REST-экрана (см. "Clean Architecture — статус" выше): REST теперь UI → ViewModel → Repository → DataSource. WiFi/BLE получат ту же структуру при реализации, не общий ViewModel/Repository между экранами — каждый со своим

### Экран 1 — WiFi (прямой MQTT) — делаем первым
- Подключение напрямую к Mosquitto на Pi, 192.168.1.36:1883 — тот же принцип, что и у текущего Telegram-бота
- Publish в `zigbee2mqtt/0xa4c138ec052f4cce/set`
- Библиотека — обсудить при реализации (кандидат: HiveMQ MQTT Client; учитывать, что async API у неё на CompletableFuture/Reactive, а не coroutines напрямую — понадобится тонкая обёртка)
- Работает только в домашней WiFi-сети, пока не сделан Tailscale/port forwarding из большого роадмапа
- Открытый вопрос: есть ли логин/пароль на Mosquitto — проверить на Pi перед реализацией

### Экран 2 — REST API (через Home Assistant) — ✅ готово (2026-08-23)
- Long-Lived Access Token создан в HA, лежит в `Secrets.kt` (`HA_LONG_LIVED_TOKEN`)
- entity_id подтверждён: `light.lamp_cabinet`
- Реализовано на Retrofit + OkHttp + Moshi (реальная библиотека, не HiveMQ — тот кандидат был для
  экрана WiFi/MQTT, не REST): изначально `rest/HomeAssistantApi.kt`/`HomeAssistantClient.kt`,
  после Clean Architecture рефакторинга (задача #7, 2026-08-23) перенесены в
  `data/HomeAssistantApi.kt`/`HomeAssistantClient.kt` + `data/HomeAssistantLampRepository.kt`,
  UI-слой (`rest/RestScreen.kt`) обращается к ним через `rest/RestLampViewModel.kt`
- HTTP POST на `http://192.168.1.36:8123/api/services/light/turn_on` / `turn_off`, яркость через
  `brightness_pct`; ответ типизирован как `Response<ResponseBody>`, не `Response<Unit>` — HA
  возвращает непустой JSON-массив состояний, который Moshi не смог бы смаппить в `Unit`
- Проверено вживую на реальной лампе — переключение и яркость реально доходят до устройства
- Требует `android.permission.ACCESS_LOCAL_NETWORK` (см. CLAUDE.md) — без него все сокет-запросы
  к 192.168.1.x молча висят в таймаут; разрешение запрашивается один раз на старте приложения,
  общее для всех экранов
- Тоже работает пока только в домашней сети, но из трёх способов именно этот проще всего потом
  продлить через Tailscale — тот же HTTP-запрос, просто другой хост вместо локального IP

### Экран 3 — BLE (напрямую к Малине) — делаем третьим, самый трудоёмкий
Требует изменений на два конца, это не только Android-задача:

**Бэкенд на Pi (новое):**
- BLE GATT peripheral-сервис на встроенном Bluetooth Pi4 (BlueZ), отдельный systemd-сервис по аналогии с `bot.service`
- 1 GATT-сервис, характеристики для power (on/off) и brightness
- При записи в характеристику сервис на Pi публикует в тот же MQTT-топик, что и остальные способы
- Без pairing/bonding (Just Works) — достаточно, так как использование только в пределах дома
- Библиотека на стороне Pi — обсудить при реализации (кандидат: bluezero)

**Android BLE-клиент:**
- Сканирование по UUID сервиса Pi, подключение, запись характеристик при изменении Switch/Slider
- Разрешения Android 12+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
- Голый `BluetoothGatt` callback API исторически болезненный — обсудить обёртку (кандидат: Nordic Android BLE Library, есть ktx-модуль с coroutines/suspend-функциями) при реализации
- Тестируется только физически рядом с Pi (BLE — это ~10-30 метров, без всякого Tailscale)

## Рекомендуемый порядок реализации внутри фазы 1

1. **WiFi (MQTT)** — работает с текущим бэкендом без изменений, самый быстрый результат
2. **REST (HA)** — тоже без изменений бэкенда, нужен только токен + entity_id
3. **BLE** — требует новый сервис на Pi, поэтому последним, это отдельная под-задача с двумя сторонами

## Осознанно НЕ делаем в фазе 1

- Сохранение credentials через UI/DataStore — это фаза 2+
- ~~Архитектуру (ViewModel/Repository/DI) — фаза 1 это 3 простых независимых экрана~~ — **пересмотрено 2026-08-23**, см. "Пересмотр решения "без архитектуры в фазе 1"" и "Clean Architecture — статус" выше (без DI — по-прежнему без него, только UI/ViewModel/Repository/DataSource)
- Удалённый доступ (Tailscale/port forwarding/FastAPI backend) — отдельно, в большом роадмапе из smarthome.md, после фазы 1

## Открытые вопросы (уточнить до/во время реализации соответствующего экрана)

- [ ] Есть ли логин/пароль на Mosquitto на Pi (для экрана WiFi)
- [x] Точный entity_id лампы в Home Assistant — `light.lamp_cabinet` (для экрана REST)
- [ ] Название/UUID нового BLE GATT-сервиса на Pi, и какой библиотекой его поднимать (для экрана BLE)

## Статус

Экран REST (Home Assistant) готов и работает на реальной лампе. WiFi (MQTT) и BLE — следующие.

## Журнал

- **2026-08-23** — настроен `/swarm` (researcher → executor → reviewer, было скопировано из чужого
  проекта AeroTask и адаптировано), подключены detekt+ktlint. Обновлён весь тулчейн до последних
  стабильных версий: Gradle 9.7.1, Kotlin 2.4.10, Compose BOM 2026.08.00, compileSdk/targetSdk 37
  (заодно почистило исходный баг несовместимости core-ktx 1.19.0 с compileSdk 36), Java 21
  source/target compatibility. AGP и остальные androidx-зависимости уже были на последних стабильных
  версиях, без изменений. Экраны WiFi/REST/BLE ещё не начаты.
- **2026-08-23** — реализован экран REST (Home Assistant): Retrofit+OkHttp+Moshi, реальное
  управление лампой подтверждено физически. По пути нашли и исправили важный системный баг:
  без `android.permission.ACCESS_LOCAL_NETWORK` любые сокет-соединения к 192.168.1.x на этой
  версии Android молча висят в таймаут (см. CLAUDE.md → "Доступ к локальной сети"). Заодно
  зарезервировали статический IP для TP-Link AX55 (192.168.1.34) на Funbox — см. smarthome.md.
- **2026-08-23** — реализованы два home-screen виджета (Jetpack Glance 1.1.1, 2×2 и 3×2,
  отдельные pickable-провайдеры) через `/swarm`, 2 итерации. Первый автоматический reviewer
  (статический анализ+код) дал `PASS`, но живой скриншот на реальном устройстве после того, как
  пользователь сам разместил оба виджета на рабочем столе, вскрыл 2 визуальных бага, которые
  reviewer не мог поймать: (1) иконка питания как Unicode-глиф `⏻` рендерилась квадратом-заглушкой
  — не было в системном шрифте; (2) строка яркости `− NN% +` (и статус на 2×2) обрезалась, не
  помещаясь в `minHeight=110dp`. Оба исправлены (vector drawable вместо глифа; уменьшение
  внутренних отступов вместо увеличения `minHeight`, чтобы не сломать деление 2×2/3×2), вторая
  итерация reviewer дала `PASS`, повторный скриншот подтвердил исправление. Вывод для будущих
  задач с виджетами/визуальным UI: автоматический reviewer проверяет только код, реальный рендер
  стоит перепроверять живым скриншотом на устройстве перед тем как считать задачу закрытой.
- **2026-08-23** — внедрена Clean Architecture (UI → ViewModel → Repository → DataSource, без
  usecase-слоя и без DI) для REST-экрана и виджета через `/swarm`, 1 итерация, `PASS`. Новые
  пакеты `domain/` (интерфейс `LampRepository`) и `data/` (`HomeAssistantApi`/`HomeAssistantClient`,
  перенесены из `rest/`; `HomeAssistantLampRepository` — единственная реализация). `rest/` теперь
  чистый UI + `RestLampViewModel` + sealed `RestScreenUiState` (`Idle`/`Sending`/`Ready`). Виджет
  обращается к `LampRepository` напрямую (без ViewModel — у Glance `ActionCallback` нет
  `ViewModelStoreOwner`). Цвета виджета вынесены в `widget/LampWidgetColors.kt`. Подробности — в
  разделе "Clean Architecture — статус" выше.
- **2026-08-23** — переведены на английский: комментарии в коде проекта (UI-строки намеренно не
  трогали — отдельная задача), `.claude/agents/*.md` + `.claude/skills/swarm/SKILL.md` (фоновым
  агентом), и `CLAUDE.md` (заодно актуализирован раздел `## Architecture`, был устаревшим после
  Clean Architecture рефакторинга). `smarthome.md` осознанно остаётся на русском — отдельная
  заметка пользователя, не в этом проекте.
