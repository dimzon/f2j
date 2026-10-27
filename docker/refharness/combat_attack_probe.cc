// Compile the reference combat translation unit with a switchable animation
// scheduling seam. Its static state stays private to this TU; upstream sources
// are included unchanged. Normal harness calls still use the real action path.
#define _action_attack f2jReferenceActionAttack
#include "../../src/combat.cc"
#undef _action_attack

namespace fallout {

int _action_attack(Attack* attack);
static bool probeAttackScheduling = false;
static int probeSchedulingResult = 0;

int f2jReferenceActionAttack(Attack* attack)
{
    return probeAttackScheduling ? probeSchedulingResult : _action_attack(attack);
}

// Runs the real _combat_attack, including attackCompute and its private _gcsd
// branch. Completion is separately controlled, like the animation callback.
int f2jReferenceCombatAttack(Object* attacker, Object* defender, HitMode mode,
    HitLocation location, CombatStartData* start, int schedulingResult,
    bool finish, Attack* snapshot)
{
    CombatStartData* savedStart = _gcsd;
    _gcsd = start;
    _combat_turn_running = 0;
    _combat_cleanup_enabled = false;
    _combat_call_display = false;
    probeAttackScheduling = true;
    probeSchedulingResult = schedulingResult;

    int rc = _combat_attack(attacker, defender, mode, location);
    *snapshot = _main_ctd;
    if (rc == 0 && finish) {
        // No battle text/UI is initialized in this probe. The real callback
        // still consumes ammo, applies damage and resets _main_ctd.
        _combat_call_display = false;
        _combat_turn_running = 1;
        _combat_anim_finished();
    }

    probeAttackScheduling = false;
    _gcsd = savedStart;
    _combat_cleanup_enabled = false;
    _combat_call_display = false;
    return rc;
}

// _combat_check_bad_shot is a stat of combat.cc, but this TU includes that
// file, so it is observable here. A bare headless _combat_attack_this call
// would be a no-op (the interface window is not initialized), so the vector
// pins the guard logic itself: dead opponent, crippled arms, AP, range and
// ammo.
int f2jReferenceCheckBadShot(Object* attacker, Object* defender, HitMode mode,
    bool aiming, int ap, int ammo, int results)
{
    int savedAp = attacker->data.critter.combat.ap;
    Dam savedResults = attacker->data.critter.combat.results;
    attacker->data.critter.combat.ap = ap;
    if (results >= 0) {
        attacker->data.critter.combat.results = static_cast<Dam>(results);
    }
    Object* weapon = critterGetWeaponForHitMode(attacker, mode);
    int savedAmmo = -1;
    if (weapon != nullptr && ammo >= 0) {
        savedAmmo = ammoGetQuantity(weapon);
        ammoSetQuantity(weapon, ammo);
    }

    CombatBadShot rc = _combat_check_bad_shot(attacker, defender, mode, aiming);

    attacker->data.critter.combat.ap = savedAp;
    attacker->data.critter.combat.results = savedResults;
    if (weapon != nullptr && savedAmmo >= 0) {
        ammoSetQuantity(weapon, savedAmmo);
    }
    return static_cast<int>(rc);
}

// Real critical-hit computation with a seeded effect roll. Returns the
// multiplier and the final attack snapshot.
int f2jReferenceCriticalHit(Object* attacker, Object* defender, HitMode mode,
    HitLocation location, int seed, Attack* snapshot)
{
    Attack attack;
    attackInit(&attack, attacker, defender, mode, location);
    randomSeedPrerandom(seed);
    int multiplier = attackComputeCriticalHit(&attack);
    *snapshot = attack;
    return multiplier;
}

// Real spray computation. The weapon (as set up by the caller) defines the
// burst mode; returns roll, rounds fired and the attack snapshot.
int f2jReferenceSpray(Object* attacker, Object* defender, Object* weapon,
    HitMode mode, int accuracy, int seed, int* roundsFired, Attack* snapshot)
{
    Attack attack;
    attackInit(&attack, attacker, defender, mode, HIT_LOCATION_TORSO);
    int roundsHit = 0;
    int fired = 0;
    randomSeedPrerandom(seed);
    AnimationType anim = critterGetAnimationForHitMode(attacker, mode);
    int roll = _compute_spray(&attack, accuracy, &roundsHit, &fired, anim);
    *roundsFired = fired;
    *snapshot = attack;
    return roll;
}

// Real explosion-extras computation. The caller places critters around the
// defender and sets the explosion radius; the snapshot exposes the extras.
void f2jReferenceExplosionExtras(Object* attacker, Object* defender,
    Object* weapon, HitMode mode, int seed, Attack* snapshot)
{
    Attack attack;
    attackInit(&attack, attacker, defender, mode, HIT_LOCATION_TORSO);
    attack.attackerFlags |= DAM_HIT;
    randomSeedPrerandom(seed);
    explosionSettingsReset();
    explosionSetRadius(1);
    _compute_explosion_on_extras(&attack, 0, 1, 0);
    *snapshot = attack;
}

// Real random-trajectory / grenade-landing block of attackCompute (the part
// that runs after a miss: neither HIT nor CRITICAL). The caller sets up the
// weapon so the branch is reachable; the snapshot shows where the shot lands
// and whether it clips someone.
void f2jReferenceRangedMiss(Object* attacker, Object* defender, Object* weapon,
    HitMode mode, int seed, bool isGrenade, Attack* snapshot)
{
    Attack attack;
    attackInit(&attack, attacker, defender, mode, HIT_LOCATION_TORSO);
    randomSeedPrerandom(seed);

    int distance = objectGetDistanceBetweenTiles(attacker, attacker->tile,
        defender, defender->tile);
    int range = weaponGetRange(attacker, mode);

    int tile;
    if (isGrenade) {
        int throwDistance = randomBetween(1, distance / 2);
        if (throwDistance == 0) {
            throwDistance = 1;
        }
        Rotation rotation = static_cast<Rotation>(randomBetween(ROTATION_FIRST, ROTATION_LAST));
        tile = tileGetTileInDirection(defender->tile, rotation, throwDistance);
    } else {
        tile = _tile_num_beyond(attacker->tile, defender->tile, range);
    }

    attack.tile = tile;

    Object* accidentalTarget = defender;
    _make_straight_path_func(accidentalTarget, defender->tile, attack.tile, nullptr, &accidentalTarget, 32, _obj_shoot_blocking_at);
    if (accidentalTarget != nullptr && accidentalTarget != defender) {
        attack.tile = accidentalTarget->tile;
    } else {
        accidentalTarget = _obj_blocking_at(nullptr, attack.tile, defender->elevation);
    }

    if (accidentalTarget != nullptr && (accidentalTarget->flags & OBJECT_SHOOT_THRU) == OBJECT_NONE) {
        attack.attackerFlags |= DAM_HIT;
        attack.defender = accidentalTarget;
        attackComputeDamage(&attack, 1, 2);
    }

    *snapshot = attack;
}

// Real _combat_player_knocked_out_by with arranged dude/ending-guy state.
int f2jReferencePlayerKnockedOutBy(int dudeDead)
{
    if (dudeDead != 0) {
        gDude->data.critter.combat.results |= DAM_DEAD;
    } else {
        gDude->data.critter.combat.results &= ~DAM_DEAD;
    }
    return _combat_player_knocked_out_by();
}

void f2jReferenceSetEndingGuy(Object* guy)
{
    _combat_ending_guy = guy;
}

void f2jReferenceSetCombatRoster(Object** list, int com, int noncom, int elev)
{
    _combat_list = list;
    _list_com = com;
    _list_noncom = noncom;
    _combat_elev = elev;
}

int f2jReferenceCombatAttemptEnd()
{
    combatAttemptEnd();
    return (gCombatState & COMBAT_STATE_EXIT_REQUESTED) != 0 ? 1 : 0;
}

// The AI attack helpers call _combat_turn_run, which spins on this counter
// headless; the sections park it so the runs return.
void f2jReferenceSetTurnRunning(int value)
{
    _combat_turn_running = value;
}

int f2jReferenceGetTurnRunning()
{
    return _combat_turn_running;
}

// The AI attack probe calls _combat_attack; the bare headless scheduler
// rejects it, so the section arms the _action_attack substitution the same
// way f2jReferenceCombatAttack does.
void f2jReferenceSetProbeScheduling(bool enabled, int result)
{
    probeAttackScheduling = enabled;
    probeSchedulingResult = result;
}

// AI info list is a combat.cc static indexed by cid; allocate and zero it
// the way _combat_begin does (src/combat.cc:2195). _list_total is 0 in this
// process (no _combat_begin), so zero the entries directly.
void f2jReferenceAiInfoInit(int count)
{
    if (_aiInfoList != nullptr) {
        internal_free(_aiInfoList);
    }
    _aiInfoList = (CombatAiInfo*)internal_malloc(sizeof(*_aiInfoList) * count);
    for (int index = 0; index < count; index++) {
        _aiInfoList[index].friendlyDead = nullptr;
        _aiInfoList[index].lastTarget = nullptr;
        _aiInfoList[index].lastItem = nullptr;
        _aiInfoList[index].lastMove = 0;
    }
}

Object* f2jReferenceAiInfoGetLastTarget(Object* obj)
{
    return aiInfoGetLastTarget(obj);
}

int f2jReferenceAiInfoSetLastTarget(Object* a1, Object* a2)
{
    return aiInfoSetLastTarget(a1, a2);
}

Object* f2jReferenceAiInfoGetFriendlyDead(Object* obj)
{
    return aiInfoGetFriendlyDead(obj);
}

int f2jReferenceAiInfoSetFriendlyDead(Object* a1, Object* a2)
{
    return aiInfoSetFriendlyDead(a1, a2);
}

} // namespace fallout
