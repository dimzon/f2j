# docker/ — reference C++ environment + differential-test harness

## Игровые данные (`C:\Work\f2j-gamedata\game`, вне репозитория)

Из `Fallout 2.iso` (7-Zip): `master.dat`, `critter.dat`,
`DATA\SOUND\MUSIC` → `data/sound/music` (имена в нижнем регистре),
`fallout2.cfg` из `files/`, `ce.dat` собирается тулзой:

```powershell
docker run --rm -v "C:\Work\f2j:/src" -v "C:\Work\f2j-gamedata\game:/game" f2j-ref bash -lc '/src/out/build/linux-x64-debug/ce-dat-tool create /src/files/ce.dat /game/ce.dat'
```

**Внимание: `game/` (`Fallout 2.iso`) — русский** (`text/english/*.msg`
в cp1251, проверено через `ce-dat-tool cat`). Для parity движка неважно
(байты есть байты, extended-ASCII даже полезен — см. NOTE в `word_wrap`),
но текстовые фикстуры будут русскими.

## Английские данные (`C:\Work\f2j-gamedata\game-en`, основной эталон)

Из GOG-инсталлера (`Fallout.Anthology-Rutracker`,
`setup_fallout_2_1.02_gog_v1`, распаковка через `innoextract`):
`master.dat`, `critter.dat`, **`patch000.dat`**, `data/` (art/tiles,
worldmap.dat), `sound/music`, `f2_res.dat`, `fallout2.cfg` из `files/`,
`ce.dat` собирается тулзой так же. `run-game.ps1 -GameDir` выбирает набор
(дефолт — `game-en`).

## Прогон игры (`docker/run-game.ps1`)

## Контейнер `f2j-ref` (Ubuntu 22.04, как CI `linux (x64)`)

```powershell
# из корня репо
powershell -File docker/build-image.ps1   # собрать образ (нужен разово)
powershell -File docker/build-ref.ps1     # cmake --preset linux-x64-debug + build
powershell -File docker/shell.ps1         # интерактивный шелл, /src = корень репо
powershell -File docker/run-game.ps1      # игра под Xvfb: лог + shot-N.jpg в /game
```

Бинарники: `out/build/linux-x64-debug/{fallout2-ce,mapper-ce,ce-dat-tool,ce-frm2png}`
(`out/` уже в `.gitignore` апстрима). Без игровых данных `fallout2-ce`
печатает `Could not find the master datafile` и выходит с кодом 1 — это норма.

## Харнес `docker/refharness`

`ref_main.cc` линкуется против **всех** объектников движка (кроме
`main.cc.o`/`win32.cc.o`, у которых свой `main`; недостающий
`fallout::gProgramIsActive` доопределён в харнесе) и печатает
детерминированные векторы чистых функций:

- `rgb555` (color.h, constexpr)
- `rectUnion` / `rectIntersection` / `rect_clip` (geometry.cc)
- `randomSeedPrerandom` + `randomBetween` с фиксированными сидами (random.cc)

```powershell
powershell -File docker/refharness/build-refharness.ps1  # пересобрать и показать векторы
powershell -File docker/refharness/check-refharness.ps1  # сверить с golden/v1.txt
```

`golden/v1.txt` — эталон фазы 1 (geometry/random/rgb555). Внимание: в v1
строка `INFO:` от `SDL_Log` стоит первой — тогда stdout ещё был
буферизован; с v2 харнес делает `setvbuf(stdout, _IONBF)` и порядок
хронологический. `v3` добавляет `[dict]`/`[dictFile]` (включая фикстуру
`java/engine/src/test/resources/fixtures/dict-v1.bin`, которую грузит
Java-тест), `v4` — `[paths]`/`[fileUtils]`, `v5/v6` — `[pattern]`/
`[dbase]`/`[zip]`/`[fixtureDat]` на реальных DAT
(`F2J_GAMEDATA=.../game-en` монтируется в `/game-en`), `v7` —
`[xbase]`/`[xfile]`/`[db]` (VFS-приоритеты, gzip-ветка, BE-чтения,
прогресс-хендлер, списки имён), `v8` — `[config]` (INI: strtol-квирки,
int-списки, CLI-оверрайды, standard + side-by-side запись),
`v9` — `[message]` (`.msg` на реальных данных, badwords, gender-слова,
репозиторий). Java-порт обязан
воспроизводить последний golden
побайтово (см. `java/`). При расширении покрытия харнеса —
новый файл `golden/vN.txt`, старые не трогать.
