# PLAN.md — план доведения порта до играбельного состояния

Дата снимка: 2026-10-24, `HEAD = 79a27e18` (`main`, чисто, в sync с `origin`).
Правила не меняются: сначала векторы в `docker/refharness` (`ref_main.cc` + новый
`golden/vN.txt`, старые immutable), потом Java-класс; перед каждым коммитом
`mvn -B test` из `java/` + `check-refharness.ps1` → `VECTORS_MATCH`;
коммит только по завершении этапа с разбором «что перенесено / расхождения /
что отложено» (см. `AGENTS.md`, `java/PORTING.md`, `F2J.md`).

## 1. Снимок состояния (факты, не оценки)

| Метрика | Значение | Где проверено |
|---|---|---|
| Файлов Java в `engine/src/main` | 136 | `rglob('*.java')` |
| Файлов Java в `engine/src/test` | 186 | то же |
| Билд-инов объявлено в `GameApi` / слотов в `GameApiSlots` | 161 / 161 | `GameApi.java`, `GameApiSlots.java` |
| Билд-инов в `PROVIDED_OPCODES` (`GameScriptApi`) | **161/161** | `GameScriptApi.java:38+` |
| **Не отдаётся игрой** | **0** | полноту держит `GameApiDispatchStackTest` |
| Тесты (последний прогон) | **712, 0 падений, 0 ошибок, 3 skipped** | `target/surefire-reports` (195 файлов) |
| `CompiledSweepTest` | 1443 скрипта, **1442 совпадают**, 1 расхождение — только `KRUPPER.int` | отчёт surefire; `KNOWN` в тесте |
| Эталон харнеса | `golden/v155.txt` (файлов v1..v155, нет v102, v144 — пропуски нумерации, не данных) | `docker/refharness/golden`, `check.sh` (с `diff -I '^block50=' -I '^INFO: Random number'` — см. §3.5) |
| Активный фронт работ | Этап 3: каркас, читалки, `attackCompute` закрыты (до `6b3df2b5`); дальше — баллистика/криты-таблицы, AI/ввод; после — Этап 4 | `git log` |

Про `CompiledSweepTest`: HUNTER.int и ROMERO.int из старых отчётов **уже пофикшены**
(общая блочная куча строк интерпретатора и компилятора). Осталось только
фундаментальное ограничение KRUPPER.int — прыжок на вычисленный адрес,
который скомпилированный код знать не может.

**Важно: `PORTING.md` сейчас врёт.** Строка «Игра даёт все 161 билд-инов»
не соответствует коду (147/161). Правильные числа — выше; PORTING.md
требует правки в Этапе 0.

## 2. Карта недостающих билд-инов: закрыта Этапом 1

Все 13 недостававших билд-инов отданны (коммиты указаны). Таблица ниже —
история скоупа; актуальное состояние — `PROVIDED_OPCODES` (161/161),
полноту держит `GameApiDispatchStackTest`.

Сверка `PROVIDED_OPCODES` против слотов `GameApiSlots` (порядок — по числу
вызовов в shipped-скриптах, т.е. по цене). `critter_rm_trait` (0x8103) уже
отдаётся — метод, диспетчер и слот были, не хватало одной строки в
`PROVIDED_OPCODES` (добавлена Этапом 0):

| Вызовов | Opcode | Метод | Что нужно | Вердикт |
|---|---|---|---|---|
| 1601 | 0x80CE | `animateMoveObjToTile` | очередь анимаций + straight path; вне боя | Этап 1: телепорт через `objectSetLocation` + проверки (`9a35dd4d`, `[animovetile]`/`v149`) |
| 1221 | 0x811C | `opGsayStart` | диалоговая подсистема | Этап 1: no-op (`a9dfaaf`) |
| 1221 | 0x811D | `opGsayEnd` | то же | Этап 1: no-op (`a9dfaaf`) |
| 1073 | 0x80CF | `tileInTileRect` | чистая арифметика тайла (`% 200`, `/ 200`, границы) | Этап 1: **полный порт** (`d4487e75`, `[tileinrect]`/`v147`) |
| 507 | 0x80FB | `critterState` | `critterIsActive` + `FrmId.animationType` + `DAM_CRIP` — всё портировано | Этап 1: **полный порт** (`2ce3d230`, `[critterstateop]`/`v148`) |
| 471 | 0x80FF | `critterAttemptPlacement` | `objectAttemptPlacement` не портирован (есть только party-вариант); combat-delete — seam | Этап 1: портирован (`51b2a3ca`, `[critterplace]`/`v150`) |
| 46/12/12/3/3/0 | 0x8053/52/54/4D/4E/6B | `opSay*`, `opDisplay` | say-экран (рендер) | Этап 1: no-op заглушки (`a9dfaaf`) |
| 16 | 0x80CD | `animateStandReverseObj` | анимация | Этап 1: no-op (`a9dfaaf`) |

Вне `GameApi` — ещё 26 опкодов, зарегистрированных C++ без слотов и блоков
диспетчера (`op_roll_dice` и в оригинале только отвечает NOT_IMPLEMENTED,
`op_pickup`, `op_skill_contest`, алиас `0x80DD` и др., полный список — в
`PORTING.md`). Это граница интерфейса, не долг: слот+реализация+вектор —
только вместе.

Итого после Этапа 1: **161/161 заявленных**, из них часть — честные no-op
под documented seam'ами (как уже принято для `animBusy`, `objCanSeeObj`,
`floatMsg` и др.).

## 3. Технический долг (зафиксировано осмотром, не гипотезы)

1. **`GameScriptApi.java` в корне репозитория (38 КБ).** Коммит `6154d351`
   создал его неверным путём `Write` (настоящий файл — 89 КБ в
   `java/engine/...`). Maven его не видит, но он трекается git'ом, а
   `F2J.md` прямо говорит, что наши файлы — только `java/` и `docker/`.
   → Удалить после `grep` на ссылки (риск: нулевой, файл мёртв).
2. **`ProbeProtosTest.java` закоммичен** в `engine/src/test` — временный
   `System.out`-пробник, не тест (assert'ов по сути нет). Противоречит
   правилу «инструментируй временно». → Удалить (его история уже в git).
3. ~~`GameApiDispatchStackTest` покрывает только 19 опкодов из 147.~~
   Закрыто Этапом 0: таблица расширена на все 161, тест зелёный.
4. **`PORTING.md` дрейфует от кода**: «все 161» (на деле 147), блок
   «Осталось 4» с `metarule`/`get_pc_stat` — уже неактуален (оба отданны).
   → Переписать раздел состояния по фактам из §1–2.
5. **`block50` и `INFO: Random number` во всех golden-файлах — timing-зависимые.**
   Подтверждено: `block50` флапает 0/1 между прогонами, `INFO: Random number`
   не всегда флашится. Митигировано: `check.sh` сравнивает с
   `diff -I '^block50=' -I '^INFO: Random number'`, значения остаются в
   эталонах для истории.

## 4. Блокеры и зависимости (что реально мешает игре, а не счётчику)

- **Реестр скриптов.** `selfObj`/`targetObj`/`fixedParam`/`scriptOverrides`
  сейчас возвращают null/0 — скрипты, привязанные к объектам, не знают свой
  контекст. Нужен `scriptGetScript(sid)` + przyвязка `Program ↔ GameObject`.
  Разблокирует осмысленные `attack`, `use_obj`, `scr_return`.
- **Цикл боя и применение урона.** Формула, хит-шанс, данные оружия
  портированы; нет применения урона к HP, смерти/анимаций смерти, хода боя.
  Без этого `attack`, `critter_damage`, `kill_critter(_type)` — заглушки.
- **Игровой цикл / загрузка карт.** `objectsInit` создаёт dude в тестах,
  но нет `gameInit → map load → script attach`. Без этого игрок, таймеры и
  партия существуют только в харнесе/тестах.
- **Рендер/звук.** AWT-бэкенд есть как managed-заглушка с пиксельными
  тестами; нативный путь (SDL/LWJGL) — отдельный модуль, не начат. Всё
  визуальное/звуковое — документированные no-op.

## 5. Этапы

### Этап 0 — гигиена и правда в документах (1–2 часа, ноль риска)
- [x] Удалить корневой `GameScriptApi.java` и `ProbeProtosTest.java` (ссылки отсутствуют — проверено grep).
- [x] `PORTING.md`: 148/161, актуальный список 13 недостающих, убраны устаревшие блоки + правило «числа только из проверок».
- [x] Добавить `0x8103` в `PROVIDED_OPCODES` (метод и диспетчер уже были).
- [x] Расширить `POPS`-таблицу `GameApiDispatchStackTest` на все 161 опкод (сверка против `interpreter_extra.cc` + `interpreter_lib.cc`; loop-pop развёрнуты; тест зелёный — диспетчер снимает корректно везде, включая ещё не отданные билд-ины).
- Приёмка: `mvn -B test` зелёный, `VECTORS_MATCH`, `git status` чист, коммит «f2j: housekeeping».

### Этап 1 — закрыт: все 161 отдаются (коммиты `d4487e75`, `2ce3d230`, `a9dfaaf`, `9a35dd4d`, `51b2a3ca`)
Порядок был по вызовам, каждый пункт = векторы + класс + тест + коммит:
1. `tileInTileRect` (1073) — **полный порт**: чистая арифметика тайла.
2. `critterState` (507) — **полный порт**: `critterIsDead/critterIsActive` + `FrmId.animationType` в диапазоне FALL_BACK_SF..FALL_FRONT_SF → PRONE + `DAM_CRIP`.
3. `critterAttemptPlacement` (471) — портирован `objectAttemptPlacement` из `proto_instance.cc` (+ его `_obj_blocking_at`); `_combat_delete_critter` → seam; поиск как написан никогда не останавливается на находке (break только из rotation-цикла) — воспроизведено дословно, зафиксировано векторами.
4. `animateMoveObjToTile` (1601) — **телепорт** через `objectSetLocation` + проверки оригинала; очередь анимаций и `reg_anim_*` — seam.
5. Say/diag-заглушки (1221×2, 46/12/12/3/3/0) и `animateStandReverseObj` (16): no-op; проверены `GameSayStubsTest` + `POPS`-таблицей.
- Приёмка: `PROVIDED_OPCODES` = 161/161, свип без новых `KNOWN`, `check.sh` зелёный — выполнено.

### Этап 2 — реестр скриптов — закрыт (`efe77878`)
`scriptGetScript(sid)` + `scriptGetSid(program)` + `scriptAdd` + `objectSetScriptFromProto` (привязка при создании объекта) + `scriptGetSelf/scriptGetTarget/scriptGetSource/scriptGetFixedParam/scriptGetActionBeingUsed` + `scriptSetOverrides`. Векторы: `[scriptattach]` (golden `v151`). `GameScriptApi` теперь читает контекст из реестра; «голый» запуск программы (как в `[scriptrun]`) по-прежнему отвечает null/0 — тесты `ScriptApiTest`/`CompiledSweepTest` не изменились. Отложено: `scriptExecProc`, `scriptRemove`, detached-контексты, `_scr_find_str_run_info`-хвост (`localVarsCount` из `scripts.lst`), поиск «дома» для spatial-маркера.

### Этап 3 — боевой цикл — в работе
Применение урона закрыто (`e3f6eb67`): `_check_for_death`/`attackComputeDeathFlags`, `_set_new_results`, `_damage_object`, `_apply_damage`, `_compute_dmg_damage` (через результат), `actionDamage` (не-анимированная ветка), `critterKill` полный, счётчики убийств/опыта; векторы `[combatdamage]` (golden `v152`). Боевой запрос закрыт (`311c3822`): `scriptsRequestCombat`/locked/clear/handle, `CombatStartData`, `combatState`/`isInCombat`, опкоды `attack`/`attack_setup` с гардами и quirk'ом override-результатов; векторы `[combatrequest]` (golden `v153`). Структура боя закрыта (`0f5c3284`): `_combat_begin`, `_combat_sequence_init`, `_combat_sequence`, `_combat_should_end`, `_compare_faster` — тесты `GameCombatSeqTest` (функции static, недоступны харнесу — задокументировано). Перк-ранги закрыты (`90f01002`): векторы `[perkrank]` (golden `v154`), `perkGetRank`/`perkHasRank`/`perkIsValid`. Скелет хода закрыт (`272e01cf`): `_combat_turn` (LOSE_TURN, script-процедура COMBAT+override, standup, PLAYER_TURN, завершения) и `_combat_standup` — тесты `GameCombatTurnTest`; ввод/AI/анимации — швы. Цикл боя закрыт (`e1512aa7`): `_combat` (цикл раундов), `_combat_set_move_all`, `_combat_over` — тесты `GameCombatLoopTest`. Читалки оружия для `attackCompute` закрыты (`36dbdacb`): векторы `[weapondata]` (golden `v155`), `critterGetAnimationForHitMode`, `weaponGetCriticalFailureType`, `weaponComputeAmmoCost`, `unarmedGetBonusCriticalChance` (BSS). `attackCompute` закрыт (`6b3df2b5`): single-shot ролл-путь, критич. провал по `_cf_table`, критич. хит (структура; таблицы эффектов отложены — крит = ванильный ×2), enhanced knockout, `_attackFindInvalidFlags`, `_do_random_cripple`, `_is_hit_from_front`, `explosionGetDamageType`; найден и исправлен баг порта `attackDetermineToHit` (база = `skillGetValue(attacker, weaponGetSkillValue(...))`). Отложено: spray/баллистика, перехват промаха, взрывы, таблицы критических хитов, `_combat_input`, `combat_ai`, `_combat_attack_this`. Дальше: `_combat_attack`.

### Этап 4 — игровой цикл
`gameInit`-минимум: `objectsInit` → загрузка карты (`mapLoad` уже отдан) → привязка скриптов → обработка очереди событий (`queueProcessEvents` уже вызывается таймерами). Это делает dude/таймеры/партию настоящими. Критерий: скрипт уровня `miJerky` проходит end-to-end в headless.

### Этап 5 — диалоги/мир/рендер (параллелизуемо после 2–4)
Диалоговая подсистема (giq/gsay уже как no-op с арностью — заменить на состояние), области мировой карты (`mark_area_known` частично есть), затем нативный рендер/звук отдельными модулями (`f2j-platform-*`).

## 6. Риски

- Дрейф `PORTING.md` от кода (уже факт) — лечить правилом «числа в доку только из скрипта сверки», а не руками.
- `block50` флапает эталон на загруженных машинах — вынести из строгого diff или зафиксировать детерминированно.
- `KRUPPER.int` — неустранимо для компилятора (прыжок на вычисленный адрес); держать в `KNOWN` с комментарием, не пытаться «починить».
- Временные `Probe*`-тесты и файлы мимо `java/`/`docker` — ловить ревью чек-листом: `git status`, `git ls-files`, отсутствие `System.out.println("PROBE`.

## 7. Ближайшие конкретные коммиты (порядок) — Этап 1 выполнен

1. ~~`f2j: housekeeping` (Этап 0).~~ — `60acaef0`.
2. ~~`f2j: give the game the tile rect and the critter state builtins`~~ — `d4487e75` (`0x80CF`), `2ce3d230` (`0x80FB`).
3. ~~`f2j: give the game the say screen and dialog stubs`~~ — `a9dfaaf` (say-экран, gsay start/end, `0x80CD`).
4. ~~`f2j: give the game the move to tile and the placement builtins`~~ — `9a35dd4d` (`0x80CE`), `51b2a3ca` (`0x80FF`).
5. Далее — Этап 2 (реестр скриптов).
