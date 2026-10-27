package org.fallout2ce;

/**
 * Game boot: the {@code gameInit}-minimum for the headless engine. The C++
 * chain ({@code _game_init}) bootstraps the world, scripts and the UI; the
 * port currently covers the subsystems that are ported - art, mouse, tile
 * grid, party, stats, items, AI packets, light, fonts, worldmap tables, the
 * map registry and the event queue. The script engine, global-vars file and
 * the interactive screens remain documented seams (their C++ init entries
 * are listed but not invoked).
 *
 * <p>Implementation notes:
 * <ul>
 * <li>The order follows the C++ sequence where the subsystem is ported;
 * the skipped entries are the seams (see {@link #gameInit}).</li>
 * <li>Tests run the same boot sequence they have always run; this class
 * collects it in one place so the game loop can start from it.</li>
 * </ul>
 */
public final class Game {

    private Game() {
    }

    /**
     * Minimal {@code _game_init}: runs the ported init chain and returns
     * the first failing code (0 on success). Seams (not yet ported):
     * scriptsInit, gameLoadGlobalVars, scr_game_init, wmWorldMap_init
     * (worldmap tables are ported; the full init is a seam), character
     * editor, pipboy, save/load, dialog, combat (combat.cc init), automap,
     * misc message list, endgame death ending, and the UI/audio backends.
     */
    public static int gameInit() {
        // C++ order (src/game.cc): animation, scripts, global vars,
        // scr_game_init, worldmap, editor/pipboy/saveload/dialog/combat/
        // automap/misc messages/endgame, interface, sound, mouse, kbd,
        // input, 3d, queue.
        // UI-dependent seams that stay out of the headless boot: the font
        // metrics (textFontsInit/interfaceFontsInit), the keyboard and the
        // interface screens, the direct input (Input.inputInit) and the
        // sound backend - they are game-phase and currently no-ops.
        // The worldmap tables (wmTilesInit/wmTerrainInit/
        // wmEncounterTablesInit/wmMapInit) need the game config, so they
        // join once the config is loaded (game loop).
        java.util.function.IntSupplier[] inits = {
            () -> Art.artInit(),
            () -> Mouse.mouseInit(),
            () -> Light.lightInit(),
            () -> Stat.statsInit(),
            () -> PartyMembers.partyMembersInit(),
            () -> Item.itemsInit(),
            () -> CombatAi.aiInit(),
        };
        for (int index = 0; index < inits.length; index++) {
            int rc = inits[index].getAsInt();
            if (rc != 0) {
                Debug.print("gameInit step %d failed rc=%d", index + 1, rc);
                return rc;
            }
        }
        EventQueue.queueInit();
        return 0;
    }

    /** Test seam: full component reset, mirroring the test setups. */
    static void resetForTests() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ContentConfig.resetForTests();
        Message.resetForTests();
        Item.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Map.resetForTests();
        Stat.resetForTests();
        Skill.resetForTests();
        TraitDefs.resetForTests();
        GameRandom.resetProviders();
        Combat.resetForTests();
        EventQueue.resetForTests();
        ScriptsState.resetForTests();
        CombatAi.resetForTests();
        Critter.sneakWorking = false;
    }
}
