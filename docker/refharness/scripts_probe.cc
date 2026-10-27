// Compile the reference scripts translation unit for probing. Its static
// state stays private to this TU; the source is included unchanged. The
// regular scripts.cc.o object is excluded from the harness link (see
// build.sh), so this TU serves the engine's scripts symbols too.
#include "../../src/scripts.cc"

namespace fallout {

void f2jReferenceScriptsDoBkProcesses()
{
    _doBkProcesses();
}

// The scripts-tick logic without the render/UI tail: the C++ _doBkProcesses
// ends with scriptWindowUpdateAll() (window manager, movie, mouse regions) -
// a game-phase seam. The observable AI tick is the critter round-robin and
// the timed-queue drain, which is what this probe runs.
void f2jReferenceScriptsChkOnly()
{
    if (!_gdialogActive() && !gameMovieIsPlaying()) {
        _script_chk_critters();
        _script_chk_timed_events();
    }
}

void f2jReferenceScriptsRemoveAll()
{
    _scr_remove_all();
}

int f2jReferenceScriptsCritterCount()
{
    int count = 0;
    ScriptListExtent* extent = gScriptLists[SCRIPT_TYPE_CRITTER].head;
    while (extent != nullptr) {
        count += extent->length;
        extent = extent->next;
    }
    return count;
}

int f2jReferenceCritterProcessingIndex()
{
    return gCritterProcessingIndex;
}

void f2jReferenceScriptsEnableScripts()
{
    gScriptsEnabled = true;
    gCritterProcessingEnabled = true;
}

void f2jReferenceScriptsDisableScripts()
{
    gScriptsEnabled = false;
    gCritterProcessingEnabled = false;
}

int f2jReferenceScriptAddTimerEvent(int sid, int delay, int param)
{
    return scriptAddTimerEvent(sid, delay, param);
}

int f2jReferenceScriptRemoveTimerEvents(int sid, int fixedParam)
{
    return scriptRemoveTimerEvents(sid, fixedParam);
}

bool f2jReferenceQueueIsEmpty()
{
    return queueIsEmpty();
}



unsigned int f2jReferenceQueueNextEventTime()
{
    return queueGetNextEventTime();
}

unsigned int f2jReferenceGameTime()
{
    return gameTimeGetTime();
}

} // namespace fallout
