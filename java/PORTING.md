# PORTING.md — правила портирования fallout2-ce → Java

## База

- Оригинал: `fallout2-ce/fallout2-ce` @ `d08c9ea` (ветка `main` в этом клоне).
  Апстрим не трогаем: наши файлы — только `java/`, `docker/`.
- Эталон поведения: `docker/refharness/golden/v*.txt`. Спорный случай —
  побеждает бинарь из контейнера, не память.

## Стиль (1:1, дифф читаем рядом с C++)

1. Пакет `org.fallout2ce`, класс на пару `.h/.cc`: `geometry.h` → `Geometry.java`.
   В шапке каждого класса — какие адреса/функции покрыты (`// 0x4C6C18`).
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

## Проверка

```powershell
mvn -pl engine test          # из java/
powershell -File docker/refharness/check-refharness.ps1  # эталон цел
```

Тесты на реальных DAT (`DBaseTest.realMasterDat/realPatchDat`) берут
каталог из `F2J_GAMEDATA` (`.../game-en`) и abort'ятся без него.
Новый модуль = векторы в харнес (`ref_main.cc` + `golden/vN.txt`) ДО Java-класса.
