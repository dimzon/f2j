# PLAN.md — план доведения порта до играбельного состояния

Дата снимка: 2026-10-27, `HEAD = 693594ca` (`main`, чисто, в sync с `origin`).
Правила не меняются: сначала векторы в `docker/refharness` (`ref_main.cc` + новый
`golden/vN.txt`, старые immutable), потом Java-класс; перед каждым коммитом
`mvn -B test` из `java/` + `check-refharness.ps1` → `VECTORS_MATCH`;
коммит только по завершении этапа с разбором «что перенесено / расхождения /
что отложено» (см. `AGENTS.md`, `java/PORTING.md`, `F2J.md`).

## 1. Снимок состояния (факты, не оценки)

| Метрика | Значение | Где проверено |
|---|---|---|
| Файлов Java в `engine/src/main` | 145 | `rglob('*.java')` |
| Файлов Java в `engine/src/test` | 214 | то же |
| Билд-инов объявлено в `GameApi` / слотов в `GameApiSlots` | 161 / 161 | `GameApi.java`, `GameApiSlots.java` |
| Билд-инов в `PROVIDED_OPCODES` (`GameScriptApi`) | **161/161** | `GameScriptApi.java:38+` |
| **Не отдаётся игрой** | **0** | полноту держит `GameApiDispatchStackTest` |
| Тесты (последний прогон, 2026-10-27) | **800 engine + 23 platform, 0 падений, 0 ошибок, 3 skipped** | полный `mvn -B -o test` с `F2J_GAMEDATA` |
| `CompiledSweepTest` | 1443 скрипта, **1442 совпадают**, 1 расхождение — только `KRUPPER.int` | отчёт surefire; `KNOWN` в тесте |
| Эталон харнеса | `golden/v174.txt`; `[combatattack]`, `[combatbadshot]`, `[criticalhit]`, `[combatend]`, `[ai]`, `[aidec]`, `[aiinv]`, `[rangedmiss]`, `[explosionextras]`, `[spray]`, `[path]`, `[aitarget]`, `[aimove]`, `[aidist]`, `[aiattack]`, `[aimdrug]`, `[combatai]`, `[combatendattempt]`, `[aimteam]` снимают настоящий C++ `_combat_attack`, `_combat_anim_finished`, `_combat_check_bad_shot`, `attackComputeCriticalHit`, `_combat_player_knocked_out_by`, `aiInit`/`_combatai_rating`/`aiGetRunAwayMode`, хелперы AI-решений, AI-инвентарь/выбор оружия, баллистику (spray/ranged-miss/explosion-extras), `pathfinderFindPath`, выбор AI-цели и AI-движение, дистанционные предпочтения, голову AI-атаки, химию (`drugItemTakeDrug`+эффекты/аддикция, `_ai_search_environ`), сборку `_combat_ai` (FLEEING/calm/join/stop), гейт конца боя (`combatAttemptEnd`) и командные whoHitMe-манипуляции | `docker/refharness/combat_attack_probe.cc`, `combat_ai_probe.cc`, `animation_probe.cc`, `check.sh` |
| Активный фронт работ | Этап 3 закрыт; **Этап 4**: `Game.gameInit`-минимум + end-to-end map-скрипт (ARTEMPLE.MAP: scriptAdd→ScrollBlocker→MAP_ENTER через настоящую VM с GameScriptApi) проходит headless; дальше — криттеры/скрипты/очередь на живой карте, затем `_combat_input` | `Combat.combatAttack`, `CombatAi.combatAi`, `Combat.combatAttemptEnd`, `CombatAi.caiTeamCombatInit`, `Drugs.drugItemTakeDrug`, `Animation.pathfinderFindPath`, `Combat.computeSpray` и др. |
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
Последний слайс (2026-10-25): `_combat_attack` → планирование → расход AP →
`_combat_anim_finished` → расход аммо / `_apply_damage` → сброс главной атаки.
Векторы `[combatattack]` (`v156`) снимаются с настоящего C++-кода через отдельный
translation unit, включающий неизменённый `combat.cc`; подменяется только
`_action_attack`, чтобы контролировать успех/отказ планирования без кадров.
Это позволяет проверять и private `_gcsd`: объявление функции `static`
не делает инструментирование исходника невозможным.

Следующий слайс (2026-10-25): `_combat_check_bad_shot` и `_combat_attack_this`.
Векторы `[combatbadshot]` (`v157`) снимают все ветви guard'а: OK, dead,
low-AP, no-ammo, out-of-range, обе руки искалечены, одна рука (пистолет не
двуручный → OK). В порт добавлены `combatCheckBadShot`, `combatAttackThis`
(с seam'ами: выбор hit mode/прицела интерфейсом и окно прицела — игровая
фаза), а также первые читалки линии огня `objShootBlockingAt`,
`makeStraightPath` и дистанций `objectGetDistanceBetween[Tiles]`,
`weaponHasAmmoForAttack`, `weaponGetBurstRounds`.

Следующий слайс (2026-10-25): критические таблицы.
Векторы `[criticalhit]` (`v158`) снимают `attackComputeCriticalHit` с реальных
C++-данных: `gCriticalHitTables[38][9][6]` и `gPlayerCriticalHitTable[9][6]`
перенесены байт-в-байт генератором из `src/combat.cc` в фрагменты
`CriticalHitTablesPart0..4` и `CriticalHitTablesPlayer` (крупный единый
инициализатор превышал лимит размера метода JVM). `attackComputeCriticalHit`
теперь полностью воспроизводит эффект, флаги, massive-ролл, random-калечение
и фильтр invalid-флагов; `criticalHitVectors` проверяет 6 сидов против C++.

Следующий слайс (2026-10-25): баллистика.
Векторы `[spray]` (`v159`) снимают `_compute_spray` с настоящего C++-кода на
модифицированном прото пистолета (4 патрона, FIRE_BURST): все четыре сида
даютют=2, fired=4. Портированы `_check_ranged_miss` и `_shoot_along_path`
(с `extras`/`extrasDamage`/`extrasFlags`/`extrasKnockback`), и `attackCompute`
теперь использует `computeSpray` для burst/continuous и `checkRangedMiss` при
промахе. Случайные траектории/гранаты остаются швами.

Следующий слайс (2026-10-25): взрывные-эффекты.
Векторы `[explosionextras]` (`v160`) снимают `_compute_explosion_on_extras`
с настоящего C++-кода: два критера на первом кольце взрыва (seed 1/7, урон
7/8/8/7) и ветка backwash, когда атакующий сам стоит на соседнем тайле
(`aFlags=0x400100`, `extras=0`). Портированы `combatIsShotBlocked`
(`_combat_is_shot_blocked`), `computeExplosionOnExtras` (кольцевой обход с
поворотами, `MAX_TARGETS=6` после `explosionSettingsReset`), globals
`explosionRadius`/`explosionMaxTargets` и вызовы из `attackCompute` (ветки
`isGrenade`/`DAM_EXPLODE`). Важно: BSS-состояние взрывов в харнесе даёт
`maxTargets=0` (как и в C++ без `explosionsInit`), поэтому секция перед
съёмкой вызывает `explosionSettingsReset()`. `noDamage`/`isFromAttacker`
(`DAM_EXPLODE`) и взрыв-анимации остаются швами.

Следующий слайс (2026-10-25): случайные траектории и гранатный лендинг.
Векторы `[rangedmiss]` (`v161`) снимают блок `attackCompute` после промаха:
гранатная ветка (случайный поворот и дистанция броска от цели; три сида дают
tile 2301/2102/1902, без попаданий) и дальнобойная (tile через
`_tile_num_beyond`; 1825 для всех трёх сидов). Портированы
`applyRangedMissTrajectory` (использует `tileNumBeyond`, случайный бросок и
полный `_make_straight_path_func`-обход с возможным случайным попаданием в
стороннюю цель). Остаются швами: `noDamage`/`isFromAttacker` и взрывной
рендеринг.

Следующий слайс (2026-10-25): завершение боя.
Векторы `[combatend]` (`v162`) снимают `_combat_player_knocked_out_by` с
напрямую расставленным состоянием: мёртвый dude → -1, пустой ending guy → -1,
ending guy с team=3 → 3, снова мёртвый → -1. Портированы
`combatPlayerKnockedOutBy`, поле `combatEndingGuy` (сбрасывается в combatBegin/
resetForTests и выставляется в `combatAnimFinished` после `scrEndCombat`),
`scrEndCombat` (guard по `gMapSid`; полный сценарный путь с картой-скриптом
остаётся швом, т.к. харнесу некуда зарегистрировать dude) и
`combatToHit` (`_combat_to_hit`, шов hit-mode как в `combatAttackThis`; точное
число точности для dude не пиннится — харнессный C++ `objectSetLocation(gDude)`
segfault'ит headless, поэтому тест сверяет `combatToHit` с прямым вызовом
`attackDetermineToHit`).

Следующий слайс (2026-10-25): AI-пакеты и рейтинг.
Векторы `[ai]` (`v163`) снимают `aiInit` с настоящего C++-кода (файл
`data/ai.txt`: 187 пакетов; детальная выгрузка пакетов 179/149/130), читалки
`aiGetAreaAttackMode`/`aiGetRunAwayMode` (explicit-ветка для 179 и производная
ветка по `min_hp` vs maxHP для 130) и `_combatai_rating` (ac-бонус
нормализуется через `critterSetBonusStat`, чтобы вектор не зависел от различий
загрузчика прото). Портирован `CombatAi`: `AiPacket`/`AiMessageRange`, `aiInit`
(парсинг `ai.txt` + `combatai.msg` канал; путь `game\combatai.msg` — читалка
`messageListLoad` добавляет `text\<language>\`), `aiGetPacketByNum`,
`aiGetPacket`, `aiGetAreaAttackMode`, `aiGetRunAwayMode`, `combataiRating`.
Отложено: движение/атака/химия (остаток `combat_ai.cc`), `_combat_ai`,
`aiLoad/aiSave`, `combat_ai_num/name` и интерфейсные штуки.

Следующий слайс (2026-10-25): хелперы AI-решений.
Векторы `[aidec]` (`v164`) снимают настоящий C++: `_cai_get_min_hp` (null,
явные режимы, `RUN_AWAY_MODE_LAST`, производный `min_hp`, режим вне
диапазона), `_caiHasWeapPrefType` (все 9 строк `_weapPrefOrderings`),
`aiGetWeaponRangeForHitMode` (прото-ранги, unarmed-досягаемость по
`CRITTER_LONG_LIMBS` и кламп метательного оружия `3 x STR`), путь
`aiWeaponAttackInRange` на 1/2/3/9/26 тайлах, `_ai_can_use_weapon`
(art-гейт, навык против `min_to_hit`, предпочтение оружия) и
`critterCanUseWeapon` (тип, две/одна crippled-рука, art), плюс
`_combatai_check_retaliation` (нет цели, мёртвая цель, сильнее, равно).
Портированы `CombatAi.caiGetMinHp`, `caiHasWeapPrefType`,
`aiGetWeaponRangeForHitMode`, `aiWeaponAttackInRange`, `aiCanUseWeapon`
(шов `scriptHooks_CanUseWeapon` — identity headless),
`combataiCheckRetaliation` и `Critter.critterCanUseWeapon`.
Найдены и исправлены два расхождения: `combataiRating` читал max-урон
оружия из неверного слота общего out-массива (урон оружия не учитывался
вовсе), а `critterSetWhoHitMe` имел перевёрнутое условие и всегда бросал
кубик интеллекта; теперь условие и ленивый `statRoll` совпадают с C++.
Прото-мутации `[ai]` (STR=5, AC-бонус 50) общие для всего харнесс-процесса,
поэтому тесты зеркалят их явно; art-гейт требует boot как в харнессе
(xbase master/patch/critter + `artInit` + `tileInit` + `partyMembersInit`).
Следующий слайс (2026-10-26): AI-инвентарь/выбор оружия.
Векторы `[aiinv]` (`v165`) снимают настоящий C++: `weaponCanBeReloadedWith`
(тип/калибр/ammo-тип, solar-scorcher light-гейт, правило «непустое оружие»),
`aiHaveAmmo` (поиск ammo, scorcher, right-hand unequip по дальности),
`_ai_best_weapon` (порядок `_weapPrefOrderings`, tie-break по урону/цене,
hidden/flare/random-режимы, safety-invalidate), `_ai_search_inven_weap`
(body/AP/навык/ammo-гейты, right-hand skip), `_ai_search_inven_armor`
(party/body, броневой счёт), `aiCanUseItem` (chem-desire, body/kill/INT,
healing, HP-пороги), `_combat_safety_invalidate_weapon` (AutoCannon,
explosive-radius vs INT/дистанция, burst/continuous spray) и
`_ai_pick_hit_mode` (area-политика, burst-disable, предпочтение, дистанция,
AP, THROW-гейт).
Портированы `Item.weaponCanBeReloadedWith[ReplacingAmmo]`,
`CombatAi.aiHaveAmmo`, `aiBestWeapon`, `aiSearchInvenWeap`,
`aiSearchInvenArmor`, `aiCanUseItem`, `aiPickHitMode`, `aiIs/Set/RemoveBurstDisabled`,
`Inventory.inventoryUnequip(critter, hand)` и `Combat.combatSafetyInvalidateWeapon`/
`determineToHit`.
Найдено и исправлено: `aiPickHitMode` не имел финального THROW-гейта
(`attackType != THROW || aiSearchInvenWeap(...) != null || statRoll <= 1`), а
несуществующие поля `best_weapon`/`area_attack_mode`/`chem_use` теперь читаются
из `AiPacket` напрямую. Расхождение по `pick-tohit=-22` (против 34 в Java)
вызвано глобальной мутацией прото/сложности в харнесс-процессе до `[aiinv]`
(AC-бонус → 50, unarmed-скилл 34 vs 50), а не портом: в изоляции гейт
BE_SURE остаётся закрытым, что и проверяется.
Отложено: само дерево решений `_combat_ai`/`_ai_*` (движение, атака, химия),
`_ai_search_environ`, `_ai_retrieve_object`, `_ai_switch_weapons`,
`aiLoad/aiSave`.

Следующий слайс (2026-10-26): pathfinder.
Векторы `[path]` (`v166`) снимают `pathfinderFindPath` (`_make_path`,
`animation.cc`) с настоящим `_obj_blocking_at`: прямые/заблокированные/замурованные
поиски, гард `requireEmptyDest`, SFALL-quirk ретаргета на соседний тайл для
multihex-блокера, гейт `canUseDoor` (тип двери, замок, biped, гекон,
dude-only walk-through флаг прoto) и надбавка стоимости за радиоактивную гgoo
(400, у гекона 100). Секция стоит последней в parent-процессе. Найдено по ходу:
`objectSetLocation(gDude)` сегфолтится в конце прогона (как и в `[combatend]`),
поэтому dude-сценарий указывает тайл прямым полем (`dude->tile = A`), dude в
tile-списках и так не участвует; у dude-прото в харнессе тайл -1. Портированы
`Animation.pathfinderFindPath`/`makePath`/`canUseDoor` (+ `_idist`,
`_tile_idistance`, BSS-списки узлов), `ObjectSystem.objPortalIsWalkThru` и
`ProtoInstance.objectIsLocked`. Найдено и исправлено расхождение: зеркало
`gDudeProto` в `ProtoDb` не заполняло поля из C-инициализатора
(`bodyType=BIPED`, `killType=MAN`, `damageType=NORMAL`, `headFid=-1`,
`aiPacket=0`, `team=0`) — Java-enum'ы оставались null, из-за чего
`canUseDoor(dude, …)` не пропускал игрока через дверь. Сценарии двери/гgoo
управляют флагами объекта напрямую (NO_BLOCK/DOOR_FLAG_LOCKED) и мутациями
прото (тип двери, generic-флаг), как прежние слайсы. Отложено: вызовы
`_make_path` из движения (reg-anim), `canUseDoor` для roboti body (ветка
есть, вектором не зацеплена).

Следующий слайс (2026-10-27): AI-выбор цели.
Векторы `[aitarget]` (`v167`) снимают настоящий C++: рейтинги ростера,
`_ai_sort_list_distance/strength/weakness` (ключи различны — C qsort
нестабилен, тай-ордер невоспроизводим), `_ai_find_nearest_team[_in_combat]`
(флаги 0x01/0x02, пропуск мёртвых и себя, гейт whoHitMe), `aiFindAttackers`
(слот 0 сохраняется, категории 1-3 по whoHitMe/командам, sfall-дедуп) и
`_ai_danger_source` (ветка без партии: живой whoHitMe → сразу, мёртвый чужой
→ ближайший той команды, пусто → null; партия: WHOMEVER_ATTACKING_ME с
lastTarget, пропуск убегающих при DEFENSIVE и скан атакующих дуда через
pathfinder+bad-shot, STRONGEST/WEAKEST/CLOSEST с сохранённым whoHitMe и
очисткой его же, финальный цикл перцепция+bad-shot+путь), плюс
`isWithinPerceptionDetailed` (видимость конусом `_can_see`, стекло пополняет
дистанцию, слух в бою ×2, слайс-ветки sneak). Портированы `CombatAi`
(ростер `_combat_ai_begin/_over`, сортировки вставкой — C qsort нестабилен,
тай-ы держат порядок вставки, векторы тай-ов избегают; выбор цели,
перцепция, читалки пакета `aiGetBestWeapon/Distance/AttackWho/Disposition`,
шов `scriptHooks_WithinPerception`), `Combat` (`CombatAiInfo`/`_aiInfoList`
по cid: аллокация в `combatBegin`, сброс в `combatOver`, аксессоры
LastTarget/LastItem/FriendlyDead/LastMove с точными гейтами) и `Critter`
(`critterIsFleeing`, `dudeIsSneaking`, `dudeHasState(SNEAKING/...)` теперь
читает флаги dude-прото как C++). Найдено по ходу: `critterIsDead` читает
текущий HP — свежие объекты с HP 0 «мертвы» для гейдов aiInfo, поэтому
ростер в секции сидируется hp=50; `_obj_portal_is_walk_thru`-стайл нюанс —
пакет дуда резолвится по `packet_num`, а не по индексу массива; мутации
прото из прежних секций харнеса (AC-бонус собаки 50 из `[ai]`, случайные
melee/armor-бонусы) зеркалятся в тесте явно. `objectSetLocation(gDude)`
по-прежнему сегфолтит — тайл дуда задаётся полем. Отложено: `aiSet*`
писалки пакета (консоль/UI), `aiMessageListInit/Free/Reload` хвост,
`_caiTeamCombatInit/_caiSetupTeamCombat` (командные манипуляции whoHitMe),
`_ai_search_environ`/`_ai_retrieve_object`/химия.

Следующий слайс (2026-10-27): AI-движение.
Векторы `[aimove]` (`v168`) снимают настоящий C++: quirk `_obj_ai_blocking_at`
(первый найденный криттер запоминается в `_moveBlockObj` и пропускается —
вызов возвращает не-криттера или null, а следующий вызов отдаёт
запомненного), `_cai_attackWouldIntersect` (линия выстрела через
`_make_straight_path_func` с `_obj_shoot_blocking_at`; друг на линии →
пересечение, вне линии → проверка случайного попадания через
safety-invalidate), ретаргет `_cai_retargetTileFromFriendlyFire` (союзники
той же команды с lastTarget и рейтингом ≥ источника; фланки от поворота
союзника; блок одного фланка → другой; слабые союзники игнорируются) и
движение: `_ai_run_away` (маневр FLEEING, цикл трёх направлений через
`_make_path` с пустой целью, бег при max_dist), `_ai_move_away` (STAY → -1,
шаги прочь), `_ai_move_steps_closer` (гарды AP/дистанции/STAY_CLOSE,
ретаргет, ветки бег/шаг по AP и тайл/объект по multihex), `_ai_find_friend`.
Наблюдаемый эффект — зарегистрированная анимация (kind/anim/цель/AP),
читаемая новым probe TU `animation_probe.cc` (включает неизменённый
`animation.cc`; его объект исключён из линковки). Движение гоняется вне боя:
`reg_anim_end` тогда не входит в `_combat_anim_begin`, и
`_combat_turn_run` не крутится. Портированы `Animation` (хранилище
последовательностей: `animationReset` — только step/flags, длина
сохраняется как в C; `reg_anim_begin/_end`, `_anim_free_slot`,
`_check_registry`, `_anim_cleanup`, регистраторы move/run-to-tile,
run/move-to-object, rotate-to-tile; швы: тик-насос, первый шаг, ключи
арт-кэша — преклоб существует = `_anim_preload`), `ObjectSystem`
(`objAiBlockingAt` + глобал `_moveBlockObj`), `CombatAi` (движение и
ретаргет; `_combatai_msg` — шов интерфейсных сообщений),
`Combat.combatTestIncidentalHit`. Найдено по ходу: гарды
`_ai_move_steps_closer` (AP ≤ 0, STAY/STAY_CLOSE-дистанция, цель рядом)
стоят до `reg_anim_begin`; hp-сид ростера (криттерIsDead читает текущий
HP) нужен и здесь; сетка вокруг старых секций замусорена их объектами —
секция переехала на чистый тайл 38101; `_anim_free_slot` требует
инициализированного хранилища (без `animationInit` все слоты
«незавершённые» — секция зовёт `animationReset`); pending-последовательность
держит владельца для `_check_registry`, поэтому харнес сбрасывает
хранилище перед каждой регистрацией; INT-гард ретаргета недостижим (кламп
INT ≥ 1) — вектор документирует кламп. Отложено: тик-насос анимаций и
обработчики шагов (игровая фаза), хвост `_combat_turn_run` в
`aiFinishMovement`, перк/сник-ветки дуда в регистраторах (швы на месте).
Следующий слайс (2026-10-27): командные whoHitMe-манипуляции.
Векторы `[aimteam]` (`v174`) снимают `_caiSetupTeamCombat`
(пометка ENGAGING всем криттерам elevation кроме dude), `_caiTeamCombatInit`
(крос-проводка whoHitMe по ближайшей команде: team1→d2(cid 2), team2→d1(cid 1)),
гарды null/пустого списка и no-op после `_caiTeamCombatExit`.
Портированы `CombatAi.caiSetupTeamCombat`/`caiTeamCombatInit`/
`caiTeamCombatExit` (статика _attackerTeamObj/_targetTeamObj).
Найдено по ходу: печать только pid неразличима для однотипных криттеров —
вектор печатает cid.
Следующий слайс (2026-10-27): гейт конца боя.
Векторы `[combatendattempt]` (`v173`) снимают `combatAttemptEnd`
(0x4212D0): активный враг с живой целью отказывает (exit=0), мёртвые
враги соглашаются (exit=1), пустой список (нет врагов) тоже даёт exit=1.
Секция расставляет боевой ростер напрямую (списки и счётчики — статики
combat.cc, зонды-сеттеры) и вне боя: want-to-stop/join не требуют aiInfo
для непартийных. Портирован `Combat.combatAttemptEnd` (шов: сообщение
103). Найдено по ходу: враги должны иметь живую цель (whoHitMe), иначе
danger-source пуст и want-to-stop истинно — без целевых рёбер все сценарии
дали бы 1; капча снова упиралась в `RepoRoot` из рабочего каталога —
зафиксирован корень репозитория.
Следующий слайс (2026-10-27): сборка главной функции AI-хода.
Векторы `[combatai]` (`v172`) снимают `_combat_ai` вне боя на ветках,
не входящих в шовные контуры: FLEEING по HP (HP 30 < min_hp 40; маневр
ставится, run регистрируется), calm (target null, без опасности: префсы
прогоняются, AP остаётся 10) и join/stop-решения (fleeing=0/calm=1,
после FLEEING stop=1). Портированы `CombatAi.combatAi`,
`combataiWantToJoin`, `combataiWantToStop` (швы: химия-контур
`_ai_check_drugs`, атака `_ai_try_attack`, скрипт-процедуры, тик).
Найдено по ходу: `_combat_turn_run` спиннит при IN_COMBAT и
зарегистрированной run-анимации (первый шаг в режиме боя), поэтому секция
идёт вне боя — при этом аксессоры aiInfo недоступны, и ветка friendlydead
покрыта по частям (аксессоры — в Combat, движение — в `[aimove]`);
спин диагностировался по wchan=hrtimer_nanosleep (FPS-троттл) —
процесс крутился в ожидании кадров.
Следующий слайс (2026-10-27): химия.
Векторы `[aimdrug]` (`v171`) снимают настоящий C++: прото Jet/Buffout
(статы/моды/длительности/шанс аддикции), принятие через
`drugItemTakeDrug` (результат REMOVE, два DRUG-события на длительность,
бонусы +2 PE/+1 ST/+1 AG для Jet, +2/+2/+3 по статам Buffout, аддикция
по сиду 1 даёт WITHDRAWAL-событие) и `_ai_search_environ` с предметами
вне порога перцепции (все три скана 0). Портирован новый `Drugs.java`
(порт drug-подсистемы из item.cc: таблица описаний, DrugEffectEvent/
WithdrawalEvent, queue-события, аддикция в GameGlobalVars и флаги dude,
`drugItemTakeDrug`; швы: сообщения/UI, perkAddEffect/perkRemoveEffect),
`ObjectSystem.objectListCreate` (обобщение по типу предмета) и
`CombatAi.aiSearchEnviron`. Найдено по ходу: `_ai_check_drugs`
(полный AI-контур) нестабильно зависает в объектных циклах headless, и
разные прогоны давали разное количество событий при повторных приёмах,
поэтому вектор фиксирует проверяемые компоненты — принятие и сканирование
окружения; аддикционный ролл засеян единым сидом 1 в обоих мирах, иначе
C++ и Java разошлись бы на WITHDRAWAL=0/1; «распаковка» предметов вне
порога перцепции (тайл 199) делает границы сканов детерминированными.
Следующий слайс (2026-10-27): голова AI-атаки.
Векторы `[aiattack]` (`v170`) снимают `_ai_called_shot` (сетка по
цене прицеливания, `critterCanAim`, ролл called-shot при called_freq=1,
INT-гейт 8 >= 5 (NORMAL), перепроверка шанса против min_to_hit=40 — со
скиллом Small Guns 200 шанс 95, поэтому локация 1 сохраняется) и
`_ai_attack`-гард FLEEING (возврат -1 до регистрации). три кейса:
swith пистолетом AP 10 (loc=1), с AP 1 (TORSO — нет денег на прицел),
без оружия (TORSO). Портированы `Item.critterCanAim` (+ sfall-оверрайды
aimed-shot), `CombatAi.aiCalledShot`/`aiAttack` (швы: тик-насос
поворота и `_combat_turn_run`, полный путь атаки — `_combat_attack`
через `Combat.combatAttack`). Найдено по ходу: полный путь
`_ai_attack` headless запинается в первом шаге поворота
(`_dude_stand`/арт-сетка), поэтому вектор фиксирует только дограмдационный
гард; диагностика показала, что зависимость возникла из-за сломанных
C-строк в инструментированном `animation.cc` (бинарник молча оставался
старым) — править тестовый C++ следует через Edit, а не python.
Следующий слайс (2026-10-27): дистанционные предпочтения.
Векторы `[aidist]` (`v169`) снимают `_cai_perform_distance_prefs`: STAY_CLOSE
(ктоHitMe != dude и dude дальше 5 тайлов → подход к dude), CHARGE (таунт
печатает строку пакета), SNIPE-slack (человек rating 10 против собаки 70,
после стоимости выстрела хватает очков действий и слабый отступает), SNIPE
без запаса (AP 3 против стоимости 5) и SNIPE с AP = стоимость (5 = 5:
очков действий ноль → ветка else отступает даже при равных рейтингах —
это и есть исходное поведение C++). Точность вектора держится на
`f2jReferenceAnimLastAction`, дополненном индексом слота: когда новая
регистрация не происходит, дамп показывает прежний слот — так
документируется «без движения». Хвост ретаргета с анимацией не гоняется
(в бою `_combat_turn_run` крутится на счётчике анимаций headless) —
сам ретаргет и регистраторы покрыты `[aimove]`. Портирован
`CombatAi.caiPerformDistancePrefs` (шав `_combat_turn_run` в хвосте).
Найдено по ходу: стоимость основного выстрела пистолета равна 5 AP (не 3,
как unarmed), поэтому SNIPE-ветка else срабатывает даже при AP = полная
стоимость; при сборке векторного харнеса нельзя приравнивать void-зонд к
int (правка sn-diag ломала сборку и капча молча показывала старый
бинарник).

- Исправлена читалка `weaponGetSkillValue`: C++ возвращает **значение навыка**
  через `weaponGetSkillForHitMode`, а не ID из таблицы типов атак. Solar
  Scorcher использует Energy Weapons; это проверяется регрессионным тестом.
- Бонус точности `_gcsd` применяется до ограничения 95%; проверен выстрел,
  который без бонуса имеет отрицательную точность, а с бонусом попадает.
- Сохранён баг C++: два override-присваивания записывают `defenderFlags`,
  второе (`targetResults`) затирает первое (`attackerResults`).
- Headless-планировщик завершает pending-атаку на `combatTurnRun`, без
  визуальных кадров. Отдельный тест подтверждает отсутствие двойного урона
  и расхода патронов при повторном ожидании.
- `_combat_attack_this`, выбор прицела интерфейсом, AI lastTarget,
  `_scr_end_combat`, реальные анимации и случайные траектории ещё требуют
  порта.

История предыдущих слайсов (формулировки «дальше» в ней относятся к тому
снимку; актуальная очередь — `_combat_attack_this`, затем критические таблицы,
баллистика и AI):

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

## 7. Ближайшие конкретные коммиты (порядок)

1. ~~`f2j: housekeeping` (Этап 0).~~ — `60acaef0`.
2. ~~`f2j: give the game the tile rect and the critter state builtins`~~ — `d4487e75` (`0x80CF`), `2ce3d230` (`0x80FB`).
3. ~~`f2j: give the game the say screen and dialog stubs`~~ — `a9dfaaf`.
4. ~~`f2j: give the game the move to tile and the placement builtins`~~ — `9a35dd4d` (`0x80CE`), `51b2a3ca` (`0x80FF`).
5. ~~Этап 2 (реестр скриптов).~~ — `efe77878`.
6. ~~Этап 3: боевой цикл и AI-решения.~~ — `3123e056`..`693594ca` (см. §1).
7. Далее — `_combat_input` (UI-фаза, будет собран с игровым циклом), затем Этап 4.
