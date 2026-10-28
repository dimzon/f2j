#include "map.h"

// Map the unmodified map.cc so the static gMapTransition can be observed by
// the harness sections. The regular map.cc.o is excluded from the link.
#include "map.cc"

namespace fallout {

void f2jReferenceMapTransitionGet(int* mapPtr, int* tilePtr, int* elevPtr, int* rotPtr)
{
    *mapPtr = (int)gMapTransition.map;
    *tilePtr = gMapTransition.tile;
    *elevPtr = gMapTransition.elevation;
    *rotPtr = (int)gMapTransition.rotation;
}

} // namespace fallout
