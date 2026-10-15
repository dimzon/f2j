# PORTING.md — правила портирования fallout2-ce → Java

## База

- Оригинал: `fallout2-ce/fallout2-ce` @ `d08c9ea` (ветка `main` в этом клоне).
  Апстрим не трогаем: наши файлы — только `java/`, `docker/`.
- Эталон поведения: `docker/refharness/golden/v*.txt`. Спорный случай —
  побеждает бинарь из контейнера, не память.

## Стиль (1:1, дифф читаем рядом с C++)

1. Пакет `org.fallout2ce`, класс на пару `.h/.cc`: `geometry.h` → `Geometry.java`.
   В шапке каждого класса — какие адреса/функции покрыты (`// 0x4C6C18`).
2. **Java first, без велосипедов из C++.** Идиоматичная Java по умолчанию:
   records, enums, foreach, стандартные коллекции, `AutoCloseable`,
   `StringBuilder`, `Optional`/nullable вместо out-параметров там, где это
   не меняет наблюдаемое поведение. 1:1 с C — только там, где поведение
   наблюдаемо: коды возврата, порядок обхода/сериализации, квирки,
   сайд-эффекты, точность форматов. `NOTE:` помечает каждое отклонение.
2. C-структуры с мутацией — mutable-классы с публичными полями (`Rect`);
   чистые данные — `record` (`Point`, `Size`).
3. Out-параметры — caller-provided объекты; коды возврата (`0`/`-1`) сохраняем.
4. `unsigned char` → `byte` + `Byte.toUnsignedInt` на чтении; 32-битная
   арифметика C — обычный `int` (проверить отсутствие переполнений в C!).
5. Глобалы `namespace fallout` — `static` поля/методы utility-класса.
6. `debugPrint` → `Debug.print` (slf4j, логгер `fallout`). Формат
   бэкенда — дело бэкенда, тесты assert'ят поведение, а не текст лога.
7. Баги оригинала портируем как есть + тест фиксирует (пример:
   `randomBetween(5,3)` всегда возвращает `min`).
8. Отклонения от C (хостед-RNG в `randomInit`, пул без лимита) — только там,
   где поведение ненаблюдаемо, и с `NOTE:` в коде.
9. Состояние игры/движка, которого ещё нет (`Game`, `Combat`, файлы, SDL) —
   injectable seams (`GameClock`, `CriticalsPolicy`, `initColors()→false`),
   дефолт = «свежая игра, день 0».

## Порядок портирования

1. Чистая логика без I/O (geometry, random, color, config-парсеры...).
2. Файловый слой (`db`/`xfile`/`dfile` → `java.nio`), затем данные (art, map, proto).
3. Платформа последней (SDL → LWJGL/обёртка; `svga`/`window`/`audio_engine`).

## Платформа (SPI)

- Бэкенды, которым нужны сторонние нативные библиотеки (SDL/OpenGL —
  будущие `svga`/`window`/`audio_engine`), живут в отдельных Maven-модулях
  и подключаются через `ServiceLoader`. Ядро (`engine`) их не видит.
- `f2j-platform-spi`: `VideoBackend`, `SystemClock`, `Sleeper`,
  `TickerRegistry`, `PlatformPlugin` (только JRE-типы, ноль зависимостей).
- `f2j-platform-headless`: managed-дефолт (`headless`: null-video, системные
  часы/слип, in-memory тикеры). `runtime`-зависимость `engine`, провайдер
  регистрируется через `META-INF/services`.
- `f2j-platform-awt`: рабочий managed-рендерер (`awt`, только JRE: окно
  Swing, палитровый фреймбуфер 640x480, `component << 2` как в
  `directDrawSetPalette`). Выбор: `-Df2j.platform=awt`. Без дисплея окно не
  открывается, offscreen-пиксели сверяются пиксель-в-пиксель тестами.
- `engine` по умолчанию использует только managed-код (JRE + slf4j + SPI):
  встроенные no-op/system-дефолты швов + `headless` из коробки.
- `Platform.load()`: `-Df2j.platform=<name>` побеждает; иначе `headless`,
  если есть; иначе единственный провайдер; иначе (или при неизвестном
  имени) — fail fast. Без провайдеров остаются встроенные дефолты.
- Швы тестов (`Color.video`, `Ticks.clock`, ...) — те же SPI-типы;
  тесты присваивают фейки напрямую, `Platform.install/resetForTests`
  — для интеграционных проверок.
- Игровая логика по умолчанию wired на портированный managed-код
  (пример: `Palette.colorCycle` → `ColorCycle`), а не на no-op.

## Проверка

```powershell
mvn -pl engine test          # из java/
powershell -File docker/refharness/check-refharness.ps1  # эталон цел
```

Тесты на реальных DAT (`DBaseTest.realMasterDat/realPatchDat`) берут
каталог из `F2J_GAMEDATA` (`.../game-en`) и abort'ятся без него.
Новый модуль = векторы в харнес (`ref_main.cc` + `golden/vN.txt`) ДО Java-класса.
