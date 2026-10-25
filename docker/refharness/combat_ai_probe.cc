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

} // namespace fallout
