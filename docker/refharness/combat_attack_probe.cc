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

} // namespace fallout
