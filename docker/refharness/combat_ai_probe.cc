// Compile the reference combat-AI translation unit for probing. Its static
// state stays private to this TU; upstream sources are included unchanged.
#include "../../src/combat_ai.cc"

namespace fallout {

int f2jReferenceAiInit()
{
    return aiInit();
}

int f2jReferenceAiCount()
{
    return gAiPacketsLength;
}

const char* f2jReferenceAiName(int index)
{
    return index >= 0 && index < gAiPacketsLength ? gAiPackets[index].name : nullptr;
}

void f2jReferenceAiPacketDump(int index, int* out)
{
    if (index < 0 || index >= gAiPacketsLength) {
        return;
    }
    AiPacket* ai = &(gAiPackets[index]);
    out[0] = ai->packet_num;
    out[1] = ai->max_dist;
    out[2] = ai->min_to_hit;
    out[3] = ai->min_hp;
    out[4] = ai->aggression;
    out[5] = ai->hurt_too_much;
    out[6] = ai->secondary_freq;
    out[7] = ai->called_freq;
    out[8] = ai->font;
    out[9] = ai->color;
    out[10] = ai->outline_color;
    out[11] = ai->chance;
    out[12] = ai->run.start;
    out[13] = ai->run.end;
    out[14] = ai->move.start;
    out[15] = ai->move.end;
    out[16] = ai->attack.start;
    out[17] = ai->attack.end;
    out[18] = ai->miss.start;
    out[19] = ai->miss.end;
    out[20] = ai->hit[HIT_LOCATION_HEAD].start;
    out[21] = ai->hit[HIT_LOCATION_HEAD].end;
    out[22] = ai->hit[HIT_LOCATION_TORSO].start;
    out[23] = ai->hit[HIT_LOCATION_TORSO].end;
    out[24] = ai->hit[HIT_LOCATION_GROIN].start;
    out[25] = ai->hit[HIT_LOCATION_GROIN].end;
    out[26] = ai->area_attack_mode;
    out[27] = ai->run_away_mode;
    out[28] = ai->best_weapon;
    out[29] = ai->distance;
    out[30] = ai->attack_who;
    out[31] = ai->chem_use;
    out[32] = ai->chem_primary_desire[0];
    out[33] = ai->chem_primary_desire[1];
    out[34] = ai->chem_primary_desire[2];
    out[35] = ai->disposition;
}

int f2jReferenceRating(Object* obj)
{
    return _combatai_rating(obj);
}

AreaAttackMode f2jReferenceAreaAttackMode(Object* obj)
{
    return aiGetAreaAttackMode(obj);
}

RunAwayMode f2jReferenceRunAwayMode(Object* obj)
{
    return aiGetRunAwayMode(obj);
}

AiPacket* f2jReferenceAiPacketRef(int index)
{
    return index >= 0 && index < gAiPacketsLength ? &(gAiPackets[index]) : nullptr;
}

int f2jReferenceAiRangeForHitMode(Object* critter, Object* weapon, HitMode mode)
{
    return aiGetWeaponRangeForHitMode(critter, weapon, mode);
}

bool f2jReferenceAiWeaponAttackInRange(Object* critter, Object* weapon, Object* target)
{
    return aiWeaponAttackInRange(critter, weapon, target);
}

int f2jReferenceCaiGetMinHp(AiPacket* ai)
{
    return _cai_get_min_hp(ai);
}

bool f2jReferenceCaiHasWeapPrefType(AiPacket* ai, AttackType type)
{
    return _caiHasWeapPrefType(ai, type);
}

bool f2jReferenceAiCanUseWeapon(Object* critter, Object* weapon, HitMode mode)
{
    return _ai_can_use_weapon(critter, weapon, mode);
}

void f2jReferenceCheckRetaliation(Object* a1, Object* a2)
{
    _combatai_check_retaliation(a1, a2);
}

void f2jReferenceAiSetBestWeapon(AiPacket* ai, BestWeapon bestWeapon)
{
    ai->best_weapon = bestWeapon;
}

void f2jReferenceAiSetRunAwayMode(AiPacket* ai, RunAwayMode mode)
{
    ai->run_away_mode = mode;
}

void f2jReferenceAiSetMinHp(AiPacket* ai, int minHp)
{
    ai->min_hp = minHp;
}

void f2jReferenceAiSetMinToHit(AiPacket* ai, int minToHit)
{
    ai->min_to_hit = minToHit;
}

int f2jReferenceAiMinToHit(AiPacket* ai)
{
    return ai->min_to_hit;
}

int f2jReferenceAiBestWeapon(AiPacket* ai)
{
    return ai->best_weapon;
}

int f2jReferenceAiRunAwayMode(AiPacket* ai)
{
    return ai->run_away_mode;
}

int f2jReferenceAiMinHp(AiPacket* ai)
{
    return ai->min_hp;
}

bool f2jReferenceCritterCanUseWeapon(Object* critter, Object* weapon, HitMode mode)
{
    return critterCanUseWeapon(critter, weapon, mode);
}

void f2jReferenceAiAssignPacket(Object* obj, AiPacket* ai)
{
    obj->data.critter.combat.aiPacket = ai->packet_num;
}

} // namespace fallout
