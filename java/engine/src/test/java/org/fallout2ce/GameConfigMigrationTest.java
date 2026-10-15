package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Vectors from {@code docker/refharness/golden/v22.txt} ({@code [cfgmig]}),
 * replayed against fresh temp dirs (the harness uses {@code /tmp/f2*}).
 */
class GameConfigMigrationTest {

    private String savedMasterPatches;
    private String savedGameConfigFilePath;

    @AfterEach
    void reset() {
        SfallConfig.resetForTests();
        ContentConfig.resetForTests();
        Db.resetForTests();
        if (savedMasterPatches != null) {
            Settings.settings.system.master_patches_path = savedMasterPatches;
            savedMasterPatches = null;
        }
        if (savedGameConfigFilePath != null) {
            GameConfig.filePath = savedGameConfigFilePath;
            savedGameConfigFilePath = null;
        }
    }

    void masterPatches(Path dir) {
        if (savedMasterPatches == null) {
            savedMasterPatches = Settings.settings.system.master_patches_path;
        }
        Settings.settings.system.master_patches_path = dir.toString();
    }

    void gameConfigFilePath(String path) {
        if (savedGameConfigFilePath == null) {
            savedGameConfigFilePath = GameConfig.filePath;
        }
        GameConfig.filePath = path;
    }

    static String get(Config.Cfg cfg, String section, String key) {
        String[] value = new String[1];
        assertTrue(Config.configGetString(cfg, section, key, value), section + "." + key);
        return value[0];
    }

    static Config.Cfg readFile(String path) {
        Config.Cfg cfg = new Config.Cfg();
        assertTrue(Config.configInit(cfg));
        assertTrue(Config.configRead(cfg, path, false), path);
        return cfg;
    }

    @Test
    void sfallDefaultsAndFile() throws Exception {
        Path dir = Files.createTempDirectory("f2cfg");
        String prog = dir.resolve("prog").toString();

        assertTrue(SfallConfig.sfallConfigInit(new String[] { prog }));
        int[] mode = new int[1];
        assertTrue(Config.configGetInt(SfallConfig.sfallConfig, "Misc", "OverrideCriticalTable", mode));
        assertEquals(2, mode[0]);
        assertFalse(SfallConfig.sfallConfigInit(new String[] { prog }));
        SfallConfig.sfallConfigExit();
        SfallConfig.sfallConfigExit();

        Files.writeString(dir.resolve("ddraw.ini"),
            "[Misc]\nOverrideCriticalTable=5\nBooksFile=bks.ini\n");
        assertTrue(SfallConfig.sfallConfigInit(new String[] { prog }));
        assertEquals("5", get(SfallConfig.sfallConfig, "Misc", "OverrideCriticalTable"));
        assertEquals("bks.ini", get(SfallConfig.sfallConfig, "Misc", "BooksFile"));
        SfallConfig.sfallConfigExit();
    }

    @Test
    void f2resGameMigration() throws Exception {
        Path dir = Files.createTempDirectory("f2mig1");
        Files.writeString(dir.resolve("fallout2.cfg"), "[screen]\nwindowed=1\n");
        Files.writeString(dir.resolve("f2_res.ini"),
            "[MAIN]\nSCR_WIDTH=800\nSCR_HEIGHT=600\nWINDOWED=1\nSCALE_2X=1\n"
                + "[MAINMENU]\nMAIN_MENU_SIZE=2\nSCALE_BUTTONS_AND_TEXT_MENU=1\n"
                + "[IFACE]\nIFACE_BAR_MODE=1\n");
        String gameCfgPath = dir.resolve("fallout2.cfg").toString();

        Config.Cfg gameCfg = new Config.Cfg();
        assertTrue(Config.configInit(gameCfg));
        assertTrue(Config.configRead(gameCfg, gameCfgPath, false));

        assertTrue(GameConfigMigration.gameConfigMigrateFromF2Res(gameCfgPath, gameCfg));
        assertEquals("800", get(gameCfg, "screen", "resolution_x"));
        assertEquals("2", get(gameCfg, "screen", "scale"));
        assertEquals("2", get(gameCfg, "ui", "main_menu_scale_mode"));
        assertEquals("1", get(gameCfg, "ui", "iface_bar_mode"));

        assertFalse(GameConfigMigration.gameConfigMigrateFromF2Res(gameCfgPath, gameCfg));
        assertFalse(GameConfigMigration.gameConfigMigrateFromF2Res(gameCfgPath, null));
        Config.configFree(gameCfg);
    }

    @Test
    void sfallContentMigration() throws Exception {
        Path dir = Files.createTempDirectory("f2mig2");
        Files.writeString(dir.resolve("ddraw.ini"),
            "[Misc]\nStartingMap=arroyo\nStartYear=2250\nWorldMapFPSPatch=1\n"
                + "WorldMapDelay2=100\nMovie1=custom.mve\nFallout1Behavior=1\n"
                + "CheckWeaponAmmoCost=1\n");
        masterPatches(dir);
        assertTrue(SfallConfig.sfallConfigInit(
            new String[] { dir.resolve("prog").toString() }));

        GameConfigMigration.contentConfigTryMigrateFromSfall("config\\game.cfg");

        Path target = dir.resolve("config").resolve("game.cfg");
        assertTrue(Files.isRegularFile(target));
        Config.Cfg migrated = readFile(target.toString());
        assertEquals("arroyo", get(migrated, "start", "map"));
        assertEquals("2250", get(migrated, "start", "year"));
        assertEquals("100", get(migrated, "worldmap", "travel_delay"));
        assertEquals("custom.mve", get(migrated, "movies", "movie1"));
        assertEquals("10", get(migrated, "movies", "endgame_movie_male"));
        assertEquals("1", get(migrated, "combat", "check_weapon_ammo_cost"));
        Config.configFree(migrated);

        // Target present now: second run is a silent no-op.
        GameConfigMigration.contentConfigTryMigrateFromSfall("config\\game.cfg");
        SfallConfig.sfallConfigExit();
    }

    @Test
    void f2resContentMigration() throws Exception {
        Path dir = Files.createTempDirectory("f2mig3");
        Files.writeString(dir.resolve("fallout2.cfg"), "[screen]\nwindowed=1\n");
        Files.writeString(dir.resolve("f2_res.ini"),
            "[MAINMENU]\nMENU_BG_OFFSET_X=20\nSCALE_BUTTONS_AND_TEXT_MENU=1\n");
        gameConfigFilePath(dir.resolve("fallout2.cfg").toString());
        masterPatches(dir);

        GameConfigMigration.contentConfigTryMigrateFromF2Res("config\\game.cfg");

        Path target = dir.resolve("config").resolve("game.cfg");
        assertTrue(Files.isRegularFile(target));
        Config.Cfg migrated = readFile(target.toString());
        assertEquals("50", get(migrated, "main_menu", "main_menu_panel_offset_x"));
        assertEquals("1", get(migrated, "main_menu", "scale_buttons_and_text"));
        Config.configFree(migrated);
    }

    @Test
    void movieDefaultNames() {
        assertEquals("iplogo.mve", GameConfigMigration.gameMovieGetDefaultFileName(0));
        assertEquals("credits.mve", GameConfigMigration.gameMovieGetDefaultFileName(16));
        assertNull(GameConfigMigration.gameMovieGetDefaultFileName(-1));
        assertNull(GameConfigMigration.gameMovieGetDefaultFileName(17));
        assertNull(GameConfigMigration.gameMovieGetDefaultFileName(32));
    }

    @Test
    void contentInitExit() {
        masterPatches(Path.of("/tmp/f2nope-definitely-missing"));
        ContentConfig.contentConfigInit();
        ContentConfig.contentConfigExit();
        ContentConfig.contentConfigExit();
    }
}
