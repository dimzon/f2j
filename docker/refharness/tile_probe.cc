#include "tile.h"

// Map the unmodified tile.cc so the tile render statics (gTileWindowBuffer,
// gTileEnabled, gTileWindowWidth/Height, gTileSquares) can be observed and
// controlled by the harness sections. The regular tile.cc.o is excluded
// from the link.
#include "tile.cc"

namespace fallout {

unsigned char* f2jReferenceTileWindowBuffer()
{
    return gTileWindowBuffer;
}

int f2jReferenceTileEnabled()
{
    return gTileEnabled ? 1 : 0;
}

void f2jReferenceTileWindowSize(int* widthPtr, int* heightPtr)
{
    *widthPtr = gTileWindowWidth;
    *heightPtr = gTileWindowHeight;
}

Rect* f2jReferenceTileWindowRect()
{
    return &gTileWindowRect;
}

} // namespace fallout
