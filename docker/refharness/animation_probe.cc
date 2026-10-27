// Compile the reference animation translation unit for probing. Its static
// state stays private to this TU; the source is included unchanged. The
// regular animation.cc.o object is excluded from the harness link (see
// build.sh), so this TU serves the engine's animation symbols too.
#include "../../src/animation.cc"

namespace fallout {

// Dump the most recently registered animation sequence: for actions 0-2,
// kind, anim, destination (tile or pid) and action points. out[0..11];
// returns 0 when a sequence was found, -1 otherwise.
int f2jReferenceAnimLastAction(int* out)
{
    for (int index = ANIMATION_SEQUENCE_LIST_CAPACITY - 1; index >= 0; index--) {
        AnimationSequence* sequence = &(gAnimationSequences[index]);
        if (sequence->length > 0) {
            for (int actionIndex = 0; actionIndex < 3; actionIndex++) {
                AnimationDescription* description = &(sequence->animations[actionIndex]);
                int base = actionIndex * 4;
                if (actionIndex >= sequence->length) {
                    out[base] = -1;
                    out[base + 1] = -1;
                    out[base + 2] = -1;
                    out[base + 3] = -1;
                    continue;
                }
                out[base] = (int)description->kind;
                out[base + 1] = (int)description->anim;
                if (description->kind == ANIM_KIND_MOVE_TO_OBJECT) {
                    Object* destination = description->destination;
                    out[base + 2] = destination != nullptr ? (int)destination->pid : -1;
                } else {
                    out[base + 2] = description->tile;
                }
                out[base + 3] = description->actionPoints;
            }
            return 0;
        }
    }

    return -1;
}

} // namespace fallout
