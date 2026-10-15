package org.fallout2ce;

/**
 * Port of {@code src/game_config_migration.h} /
 * {@code src/game_config_migration.cc} (one-shot f2_res/sfall setting
 * migration into the managed configs).
 *
 * <p>Movie default names mirror {@code game_movie.cc} (the only thing this
 * module needs from there; they move over when game movies land).
 */
public final class GameConfigMigration {

    private GameConfigMigration() {
    }

    // game_config.h key names used by the f2_res table.
    private static final String SCREEN = "screen";
    private static final String SYSTEM = "system";
    private static final String UI = "ui";
    private static final String RESOLUTION_X = "resolution_x";
    private static final String RESOLUTION_Y = "resolution_y";
    private static final String WINDOWED = "windowed";
    private static final String F2_RES_DAT = "f2_res_dat";
    private static final String IFACE_BAR_MODE = "iface_bar_mode";
    private static final String IFACE_BAR_WIDTH = "iface_bar_width";
    private static final String IFACE_BAR_SIDE_ART = "iface_bar_side_art";
    private static final String IFACE_BAR_SIDES_ORI = "iface_bar_sides_ori";
    private static final String IGNORE_SCROLL_LIMIT = "ignore_scroll_limit";
    private static final String ALTERNATE_AMMO_METER = "alternate_ammo_meter";
    private static final String IGNORE_MAP_EDGES = "ignore_map_edges";
    private static final String SPLASH_SCREEN_SIZE = "splash_screen_size";
    private static final String DEATH_SCREEN_SIZE = "death_screen_size";
    private static final String END_SLIDE_SIZE = "end_slide_size";
    private static final String MOVIE_ASPECT_FIT = "movie_aspect_fit";
    private static final String SCALE = "scale";
    private static final String MAIN_MENU_SCALE_MODE = "main_menu_scale_mode";

    private static final String F2_RES_CONFIG_FILE_NAME = "f2_res.ini";
    private static final int F2_RES_MAIN_MENU_BUTTON_X = 30;
    private static final int F2_RES_MAIN_MENU_BUTTON_Y = 19;
    private static final int F2_RES_DEFAULT_MAIN_MENU_PANEL_OFFSET_X = 16;
    private static final int F2_RES_DEFAULT_MAIN_MENU_PANEL_OFFSET_Y = 15;

    private record F2ResEntry(String legacySection, String legacyKey,
            String targetSection, String targetKey) {
    }

    private static final F2ResEntry[] F2_RES_ENTRIES = {
        new F2ResEntry("MAIN", "SCR_WIDTH", SCREEN, RESOLUTION_X),
        new F2ResEntry("MAIN", "SCR_HEIGHT", SCREEN, RESOLUTION_Y),
        new F2ResEntry("MAIN", "WINDOWED", SCREEN, WINDOWED),
        new F2ResEntry("MAIN", "f2_res_dat", SYSTEM, F2_RES_DAT),
        new F2ResEntry("IFACE", "IFACE_BAR_MODE", UI, IFACE_BAR_MODE),
        new F2ResEntry("IFACE", "IFACE_BAR_WIDTH", UI, IFACE_BAR_WIDTH),
        new F2ResEntry("IFACE", "IFACE_BAR_SIDE_ART", UI, IFACE_BAR_SIDE_ART),
        new F2ResEntry("IFACE", "IFACE_BAR_SIDES_ORI", UI, IFACE_BAR_SIDES_ORI),
        new F2ResEntry("MAPS", "IGNORE_PLAYER_SCROLL_LIMITS", UI, IGNORE_SCROLL_LIMIT),
        new F2ResEntry("IFACE", "ALTERNATE_AMMO_METRE", UI, ALTERNATE_AMMO_METER),
        new F2ResEntry("MAPS", "IGNORE_MAP_EDGES", UI, IGNORE_MAP_EDGES),
        new F2ResEntry("STATIC_SCREENS", "SPLASH_SCRN_SIZE", UI, SPLASH_SCREEN_SIZE),
        new F2ResEntry("STATIC_SCREENS", "DEATH_SCRN_SIZE", UI, DEATH_SCREEN_SIZE),
        new F2ResEntry("STATIC_SCREENS", "END_SLIDE_SIZE", UI, END_SLIDE_SIZE),
        new F2ResEntry("MOVIES", "MOVIE_SIZE", UI, MOVIE_ASPECT_FIT),
    };

    private record SfallEntry(String sfallSection, String sfallKey,
            String targetSection, String targetKey, String defaultValue) {
    }

    private static final String SFALL_MISC = "Misc";
    private static final String SFALL_INTERFACE = "Interface";
    private static final String SFALL_SOUND = "Sound";

    private static final SfallEntry[] SFALL_ENTRIES = {
        new SfallEntry(SFALL_MISC, "StartingMap", ContentConfig.START_SECTION, "map", ""),
        new SfallEntry(SFALL_MISC, "StartXPos", ContentConfig.START_SECTION, "worldmap_x", "-1"),
        new SfallEntry(SFALL_MISC, "StartYPos", ContentConfig.START_SECTION, "worldmap_y", "-1"),
        new SfallEntry(SFALL_MISC, "ViewXPos", ContentConfig.START_SECTION, "worldmap_view_x", "-1"),
        new SfallEntry(SFALL_MISC, "ViewYPos", ContentConfig.START_SECTION, "worldmap_view_y", "-1"),
        new SfallEntry(SFALL_MISC, "MaleStartModel", ContentConfig.START_SECTION, "model_male", "hmwarr"),
        new SfallEntry(SFALL_MISC, "MaleDefaultModel", ContentConfig.START_SECTION, "model_male_default", "hmjmps"),
        new SfallEntry(SFALL_MISC, "FemaleStartModel", ContentConfig.START_SECTION, "model_female", "hfprim"),
        new SfallEntry(SFALL_MISC, "FemaleDefaultModel", ContentConfig.START_SECTION, "model_female_default", "hfjmps"),
        new SfallEntry(SFALL_MISC, "PipBoyAvailableAtGameStart", ContentConfig.START_SECTION, "pipboy", "0"),
        new SfallEntry(SFALL_MISC, "DisableSpecialMapIDs", ContentConfig.MAPS_SECTION, "disable_special_map_ids", "0"),
        new SfallEntry(SFALL_MISC, "KarmaFRMs", ContentConfig.KARMA_SECTION, "frms", null),
        new SfallEntry(SFALL_MISC, "KarmaPoints", ContentConfig.KARMA_SECTION, "points", null),
        new SfallEntry(SFALL_MISC, "DialogueFix", ContentConfig.DIALOG_SECTION, "no_exit_hotkey", "1"),
        new SfallEntry(SFALL_MISC, "DialogGenderWords", ContentConfig.DIALOG_SECTION, "gender_words", "0"),
        new SfallEntry(SFALL_MISC, "StartGDialogFix", ContentConfig.DIALOG_SECTION, "start_gdialog_fix", "0"),
        new SfallEntry(SFALL_MISC, "VersionString", ContentConfig.MAIN_MENU_SECTION, "version_string", null),
        new SfallEntry(SFALL_MISC, "MainMenuFontColour", ContentConfig.MAIN_MENU_SECTION, "font_color", "0"),
        new SfallEntry(SFALL_MISC, "MainMenuBigFontColour", ContentConfig.MAIN_MENU_SECTION, "big_font_color", "0"),
        new SfallEntry(SFALL_MISC, "MainMenuOffsetX", ContentConfig.MAIN_MENU_SECTION, "offset_x", "0"),
        new SfallEntry(SFALL_MISC, "MainMenuOffsetY", ContentConfig.MAIN_MENU_SECTION, "offset_y", "0"),
        new SfallEntry(SFALL_MISC, "MainMenuCreditsOffsetX", ContentConfig.MAIN_MENU_SECTION, "credits_offset_x", "0"),
        new SfallEntry(SFALL_MISC, "MainMenuCreditsOffsetY", ContentConfig.MAIN_MENU_SECTION, "credits_offset_y", "0"),
        new SfallEntry(SFALL_SOUND, "MainMenuMusic", ContentConfig.SOUND_SECTION, "main_menu_music", "07desert"),
        new SfallEntry(SFALL_SOUND, "WorldMapMusic", ContentConfig.SOUND_SECTION, "worldmap_music", "23world"),
        new SfallEntry(SFALL_SOUND, "WorldMapCarMusic", ContentConfig.SOUND_SECTION, "worldmap_car_music", "20car"),
        new SfallEntry(SFALL_SOUND, "EndGameMovieMusic0", ContentConfig.SOUND_SECTION, "endgame_movie_music0", "akiss"),
        new SfallEntry(SFALL_SOUND, "EndGameMovieMusic1", ContentConfig.SOUND_SECTION, "endgame_movie_music1", "10labone"),
        new SfallEntry(SFALL_SOUND, "MapLoadingSound", ContentConfig.SOUND_SECTION, "map_loading_sound", "wind2"),
        new SfallEntry(SFALL_MISC, "MovieTimer_artimer1", ContentConfig.MOVIES_SECTION, "artimer1", "90"),
        new SfallEntry(SFALL_MISC, "MovieTimer_artimer2", ContentConfig.MOVIES_SECTION, "artimer2", "180"),
        new SfallEntry(SFALL_MISC, "MovieTimer_artimer3", ContentConfig.MOVIES_SECTION, "artimer3", "270"),
        new SfallEntry(SFALL_MISC, "MovieTimer_artimer4", ContentConfig.MOVIES_SECTION, "artimer4", "360"),
        new SfallEntry(SFALL_MISC, "DamageFormula", ContentConfig.COMBAT_SECTION, "damage_formula", "0"),
        new SfallEntry(SFALL_MISC, "BonusHtHDamageFix", ContentConfig.COMBAT_SECTION, "bonus_hth_damage_fix", "1"),
        new SfallEntry(SFALL_MISC, "RemoveCriticalTimelimits", ContentConfig.COMBAT_SECTION, "remove_critical_time_limits", "0"),
        new SfallEntry(SFALL_MISC, "ScienceOnCritters", ContentConfig.COMBAT_SECTION, "science_on_critters", "0"),
        new SfallEntry(SFALL_MISC, "CheckWeaponAmmoCost", ContentConfig.COMBAT_SECTION, "check_weapon_ammo_cost", null),
        new SfallEntry(SFALL_MISC, "InventoryApCost", ContentConfig.COMBAT_SECTION, "inventory_ap_cost", "4"),
        new SfallEntry(SFALL_MISC, "QuickPocketsApCostReduction", ContentConfig.COMBAT_SECTION, "quick_pockets_ap_cost_reduction", "2"),
        new SfallEntry(SFALL_MISC, "ComputeSprayMod", ContentConfig.COMBAT_SECTION, "burst_enabled", "0"),
        new SfallEntry(SFALL_MISC, "ComputeSpray_CenterMult", ContentConfig.COMBAT_SECTION, "burst_center_mult", "1"),
        new SfallEntry(SFALL_MISC, "ComputeSpray_CenterDiv", ContentConfig.COMBAT_SECTION, "burst_center_div", "3"),
        new SfallEntry(SFALL_MISC, "ComputeSpray_TargetMult", ContentConfig.COMBAT_SECTION, "burst_target_mult", "1"),
        new SfallEntry(SFALL_MISC, "ComputeSpray_TargetDiv", ContentConfig.COMBAT_SECTION, "burst_target_div", "2"),
        new SfallEntry(SFALL_MISC, "ExplosionsEmitLight", ContentConfig.EXPLOSIONS_SECTION, "emit_light", "0"),
        new SfallEntry(SFALL_MISC, "Dynamite_DmgMax", ContentConfig.EXPLOSIONS_SECTION, "dynamite_max", "50"),
        new SfallEntry(SFALL_MISC, "Dynamite_DmgMin", ContentConfig.EXPLOSIONS_SECTION, "dynamite_min", "30"),
        new SfallEntry(SFALL_MISC, "PlasticExplosive_DmgMax", ContentConfig.EXPLOSIONS_SECTION, "plastic_explosive_max", "80"),
        new SfallEntry(SFALL_MISC, "PlasticExplosive_DmgMin", ContentConfig.EXPLOSIONS_SECTION, "plastic_explosive_min", "40"),
        new SfallEntry(SFALL_MISC, "Lockpick", ContentConfig.SKILLDEX_SECTION, "lockpick", "293"),
        new SfallEntry(SFALL_MISC, "Steal", ContentConfig.SKILLDEX_SECTION, "steal", "293"),
        new SfallEntry(SFALL_MISC, "Traps", ContentConfig.SKILLDEX_SECTION, "traps", "293"),
        new SfallEntry(SFALL_MISC, "FirstAid", ContentConfig.SKILLDEX_SECTION, "first_aid", "293"),
        new SfallEntry(SFALL_MISC, "Doctor", ContentConfig.SKILLDEX_SECTION, "doctor", "293"),
        new SfallEntry(SFALL_MISC, "Science", ContentConfig.SKILLDEX_SECTION, "science", "293"),
        new SfallEntry(SFALL_MISC, "Repair", ContentConfig.SKILLDEX_SECTION, "repair", "293"),
        new SfallEntry(SFALL_MISC, "TownMapHotkeysFix", ContentConfig.WORLDMAP_SECTION, "town_map_hotkeys_fix", "1"),
        new SfallEntry(SFALL_MISC, "DisableHorrigan", ContentConfig.WORLDMAP_SECTION, "disable_horrigan", "0"),
        new SfallEntry(SFALL_MISC, "CityRepsList", ContentConfig.WORLDMAP_SECTION, "city_reputation_list", null),
        new SfallEntry(SFALL_INTERFACE, "WorldMapTravelMarkers", ContentConfig.WORLDMAP_SECTION, "trail_markers", "0"),
        new SfallEntry(SFALL_INTERFACE, "WorldMapTerrainInfo", ContentConfig.WORLDMAP_SECTION, "terrain_info", "0"),
        new SfallEntry(SFALL_MISC, "PremadePaths", ContentConfig.CHARACTERS_SECTION, "premade_paths", null),
        new SfallEntry(SFALL_MISC, "PremadeFIDs", ContentConfig.CHARACTERS_SECTION, "premade_fids", null),
        new SfallEntry(SFALL_MISC, "ExtraGameMsgFileList", ContentConfig.TEXT_SECTION, "extra_msg_file_list", null),
        new SfallEntry(SFALL_MISC, "XPTable", ContentConfig.STATS_SECTION, "xp_table", null),
    };

    // game_movie.cc movieDefaultFileNames (MOVIE_COUNT = 17).
    static final int MOVIE_COUNT = 17;
    static final int GAME_MOVIE_MAX_COUNT = 32;

    private static final String[] MOVIE_DEFAULT_FILE_NAMES = {
        "iplogo.mve",
        "intro.mve",
        "elder.mve",
        "vsuit.mve",
        "afailed.mve",
        "adestroy.mve",
        "car.mve",
        "cartucci.mve",
        "timeout.mve",
        "tanker.mve",
        "enclave.mve",
        "derrick.mve",
        "artimer1.mve",
        "artimer2.mve",
        "artimer3.mve",
        "artimer4.mve",
        "credits.mve",
    };

    /** Port of {@code gameMovieGetDefaultFileName} (moves with game movies). */
    static String gameMovieGetDefaultFileName(int movie) {
        if (movie < 0 || movie >= MOVIE_COUNT) {
            return null;
        }
        return MOVIE_DEFAULT_FILE_NAMES[movie];
    }

    private static boolean gameConfigHasKey(Config.Cfg config, String section, String key) {
        String[] value = new String[1];
        return Config.configGetString(config, section, key, value);
    }

    private static boolean gameConfigMigrateStringKey(Config.Cfg legacyConfig, Config.Cfg gameConfig,
            F2ResEntry entry) {
        if (gameConfigHasKey(gameConfig, entry.targetSection(), entry.targetKey())) {
            return false;
        }

        String[] value = new String[1];
        if (!Config.configGetString(legacyConfig, entry.legacySection(), entry.legacyKey(), value)) {
            return false;
        }

        return Config.configSetString(gameConfig, entry.targetSection(), entry.targetKey(), value[0]);
    }

    private static boolean gameConfigMigrateMainMenuScaleModeKey(Config.Cfg legacyConfig,
            Config.Cfg gameConfig) {
        if (gameConfigHasKey(gameConfig, UI, MAIN_MENU_SCALE_MODE)) {
            return false;
        }

        int[] scaleMode = new int[1];
        if (!Config.configGetInt(legacyConfig, "MAINMENU", "MAIN_MENU_SIZE", scaleMode)) {
            return false;
        }

        boolean[] legacyScaleButtonsAndText = new boolean[1];
        if (Config.configGetBool(legacyConfig, "MAINMENU", "SCALE_BUTTONS_AND_TEXT_MENU",
                legacyScaleButtonsAndText)
                && legacyScaleButtonsAndText[0]
                && scaleMode[0] != 0) {
            scaleMode[0] = 2;
        }

        return Config.configSetInt(gameConfig, UI, MAIN_MENU_SCALE_MODE, scaleMode[0]);
    }

    private static boolean gameConfigMigrateScaleKey(Config.Cfg legacyConfig, Config.Cfg gameConfig) {
        if (gameConfigHasKey(gameConfig, SCREEN, SCALE)) {
            return false;
        }

        int[] value = new int[1];
        if (!Config.configGetInt(legacyConfig, "MAIN", "SCALE_2X", value)) {
            return false;
        }

        return Config.configSetInt(gameConfig, SCREEN, SCALE, value[0] + 1);
    }

    private static boolean contentConfigMigrateF2ResMainMenuPanelOffsetKey(Config.Cfg legacyConfig,
            Config.Cfg contentConfig, String legacyKey, String targetKey, int baseValue, int defaultValue) {
        if (gameConfigHasKey(contentConfig, ContentConfig.MAIN_MENU_SECTION, targetKey)) {
            return false;
        }

        int[] value = new int[1];
        if (!Config.configGetInt(legacyConfig, "MAINMENU", legacyKey, value)) {
            return false;
        }

        value[0] += baseValue;
        if (value[0] == defaultValue) {
            return false;
        }

        return Config.configSetInt(contentConfig, ContentConfig.MAIN_MENU_SECTION, targetKey, value[0]);
    }

    private static boolean contentConfigMigrateF2ResBoolKey(Config.Cfg legacyConfig,
            Config.Cfg contentConfig, String legacyKey, String targetKey) {
        if (gameConfigHasKey(contentConfig, ContentConfig.MAIN_MENU_SECTION, targetKey)) {
            return false;
        }

        boolean[] value = new boolean[1];
        if (!Config.configGetBool(legacyConfig, "MAINMENU", legacyKey, value)) {
            return false;
        }

        return Config.configSetBool(contentConfig, ContentConfig.MAIN_MENU_SECTION, targetKey, value[0]);
    }

    /** Port of {@code gameConfigMigrateFromF2Res}. */
    public static boolean gameConfigMigrateFromF2Res(String gameConfigFilePath, Config.Cfg gameConfig) {
        if (gameConfigFilePath == null || gameConfig == null) {
            return false;
        }

        if (gameConfigHasKey(gameConfig, SCREEN, RESOLUTION_X)) {
            return false;
        }

        PlatformPaths.Split split = PlatformPaths.splitPath(gameConfigFilePath);
        String f2ResFilePath = PlatformPaths.makePath(split.drive(), split.dir(),
            F2_RES_CONFIG_FILE_NAME, "");

        boolean migrated = false;
        try (Config.ScopedConfig legacyConfig = new Config.ScopedConfig(f2ResFilePath, false)) {
            if (!legacyConfig.isLoaded()) {
                return false;
            }

            for (F2ResEntry entry : F2_RES_ENTRIES) {
                if (gameConfigMigrateStringKey(legacyConfig.get(), gameConfig, entry)) {
                    migrated = true;
                }
            }

            if (gameConfigMigrateMainMenuScaleModeKey(legacyConfig.get(), gameConfig)) {
                migrated = true;
            }

            if (gameConfigMigrateScaleKey(legacyConfig.get(), gameConfig)) {
                migrated = true;
            }
        }

        return migrated;
    }

    private static boolean contentConfigMigrateSfallMovieOverrides(Config.Cfg sfallConfig,
            Config.Cfg migratedConfig) {
        boolean migrated = false;
        for (int index = 0; index < GAME_MOVIE_MAX_COUNT; index++) {
            String sfallKey = "Movie" + (index + 1);

            String[] value = new String[1];
            if (!Config.configGetString(sfallConfig, SFALL_MISC, sfallKey, value)
                    || value[0].isEmpty()) {
                continue;
            }

            String defaultFileName = gameMovieGetDefaultFileName(index);
            if (defaultFileName != null && value[0].equals(defaultFileName)) {
                continue;
            }

            String targetKey = "movie" + (index + 1);
            if (gameConfigHasKey(migratedConfig, ContentConfig.MOVIES_SECTION, targetKey)) {
                continue;
            }

            Config.configSetString(migratedConfig, ContentConfig.MOVIES_SECTION, targetKey, value[0]);
            migrated = true;
        }

        return migrated;
    }

    private static boolean contentConfigMigrateSfallFallout1MovieBehavior(Config.Cfg sfallConfig,
            Config.Cfg migratedConfig) {
        boolean[] enabled = new boolean[1];
        if (!Config.configGetBool(sfallConfig, SFALL_MISC, "Fallout1Behavior", enabled)
                || !enabled[0]) {
            return false;
        }

        boolean migrated = false;
        if (!gameConfigHasKey(migratedConfig, ContentConfig.MOVIES_SECTION, "endgame_play_after_slideshow")) {
            Config.configSetInt(migratedConfig, ContentConfig.MOVIES_SECTION,
                "endgame_play_after_slideshow", 0);
            migrated = true;
        }

        if (!gameConfigHasKey(migratedConfig, ContentConfig.MOVIES_SECTION, "endgame_movie_male")) {
            Config.configSetInt(migratedConfig, ContentConfig.MOVIES_SECTION, "endgame_movie_male", 10);
            migrated = true;
        }

        if (!gameConfigHasKey(migratedConfig, ContentConfig.MOVIES_SECTION, "endgame_movie_female")) {
            Config.configSetInt(migratedConfig, ContentConfig.MOVIES_SECTION, "endgame_movie_female", 11);
            migrated = true;
        }

        return migrated;
    }

    private static int entriesLength(Config.Cfg config) {
        int length = 0;
        for (java.util.TreeMap<String, String> section : config.sections.values()) {
            length += section.size();
        }
        return length;
    }

    /** Port of {@code contentConfigMigrateFromSfall}. */
    static boolean contentConfigMigrateFromSfall(Config.Cfg sfallConfig, String contentConfigFilePath) {
        if (sfallConfig == null || contentConfigFilePath == null) {
            return false;
        }

        // Skip if a local game.cfg already exists (already migrated or user-managed).
        if (CompatFiles.fileExists(contentConfigFilePath)) {
            return false;
        }

        boolean migrated = false;
        try (Config.ScopedConfig migratedConfig = new Config.ScopedConfig()) {
            if (!migratedConfig.isLoaded()) {
                return false;
            }

            boolean[] worldMapFpsPatch = new boolean[1];
            if (Config.configGetBool(sfallConfig, SFALL_MISC, "WorldMapFPSPatch", worldMapFpsPatch)
                    && worldMapFpsPatch[0]) {
                int[] travelDelay = new int[] { 66 };
                Config.configGetInt(sfallConfig, SFALL_MISC, "WorldMapDelay2", travelDelay);
                travelDelay[0] = Math.min(Math.max(travelDelay[0], 1), 150);
                Config.configSetInt(migratedConfig.get(), ContentConfig.WORLDMAP_SECTION,
                    "travel_delay", travelDelay[0]);
                migrated = true;
            }

            migrated |= migrateStartInt(sfallConfig, migratedConfig.get(),
                "StartYear", "year", 2241);
            migrated |= migrateStartInt(sfallConfig, migratedConfig.get(),
                "StartMonth", "month", 6);
            migrated |= migrateStartInt(sfallConfig, migratedConfig.get(),
                "StartDay", "day", 24);
            migrated |= migrateStartInt(sfallConfig, migratedConfig.get(),
                "StartTime", "time", 824);

            for (SfallEntry entry : SFALL_ENTRIES) {
                String[] value = new String[1];
                if (Config.configGetString(sfallConfig, entry.sfallSection(), entry.sfallKey(), value)) {
                    if (value[0].isEmpty()
                            || entry.defaultValue() != null && value[0].equals(entry.defaultValue())) {
                        continue;
                    }
                    Config.configSetString(migratedConfig.get(), entry.targetSection(),
                        entry.targetKey(), value[0]);
                    migrated = true;
                }
            }

            if (contentConfigMigrateSfallMovieOverrides(sfallConfig, migratedConfig.get())) {
                migrated = true;
            }

            if (contentConfigMigrateSfallFallout1MovieBehavior(sfallConfig, migratedConfig.get())) {
                migrated = true;
            }

            if (migrated) {
                PlatformPaths.Split split = PlatformPaths.splitPath(contentConfigFilePath);
                String dir = PlatformPaths.makePath(split.drive(), split.dir(), "", "");
                CompatFiles.mkdirRecursive(dir);

                if (!Config.configWrite(migratedConfig.get(), contentConfigFilePath, false)) {
                    Debug.print("Failed to write migrated settings to %s!\n", contentConfigFilePath);
                }
            }
        }

        return migrated;
    }

    private static boolean migrateStartInt(Config.Cfg sfallConfig, Config.Cfg migratedConfig,
            String sfallKey, String targetKey, int defaultValue) {
        int[] value = new int[1];
        Config.configGetIntBase(sfallConfig, SFALL_MISC, sfallKey, value, -1, 10);
        if (value[0] >= 0 && value[0] != defaultValue) {
            Config.configSetInt(migratedConfig, ContentConfig.START_SECTION, targetKey, value[0]);
            return true;
        }
        return false;
    }

    private static boolean contentConfigEnsureDirectory(String contentConfigFilePath) {
        PlatformPaths.Split split = PlatformPaths.splitPath(contentConfigFilePath);
        String dir = PlatformPaths.makePath(split.drive(), split.dir(), "", "");
        return CompatFiles.isDir(dir)
            || CompatFiles.mkdirRecursive(dir) == 0
            || CompatFiles.isDir(dir);
    }

    /** Port of {@code contentConfigMigrateFromF2Res} (test-visible). */
    static boolean contentConfigMigrateFromF2Res(Config.Cfg legacyConfig, String contentConfigFilePath) {
        if (legacyConfig == null || contentConfigFilePath == null) {
            return false;
        }

        Config.Cfg migratedConfig = new Config.Cfg();
        if (!Config.configInit(migratedConfig)) {
            return false;
        }
        try {
            Config.configRead(migratedConfig, contentConfigFilePath, false);

            boolean migrated = false;
            if (contentConfigMigrateF2ResMainMenuPanelOffsetKey(legacyConfig, migratedConfig,
                    "MENU_BG_OFFSET_X", "main_menu_panel_offset_x",
                    F2_RES_MAIN_MENU_BUTTON_X, F2_RES_DEFAULT_MAIN_MENU_PANEL_OFFSET_X)) {
                migrated = true;
            }
            if (contentConfigMigrateF2ResMainMenuPanelOffsetKey(legacyConfig, migratedConfig,
                    "MENU_BG_OFFSET_Y", "main_menu_panel_offset_y",
                    F2_RES_MAIN_MENU_BUTTON_Y, F2_RES_DEFAULT_MAIN_MENU_PANEL_OFFSET_Y)) {
                migrated = true;
            }
            if (contentConfigMigrateF2ResBoolKey(legacyConfig, migratedConfig,
                    "SCALE_BUTTONS_AND_TEXT_MENU", "scale_buttons_and_text")) {
                migrated = true;
            }

            if (migrated) {
                if (contentConfigEnsureDirectory(contentConfigFilePath)) {
                    if (!Config.configWriteEx(migratedConfig, contentConfigFilePath,
                            Config.RETAIN_ALL)) {
                        Debug.print("Failed to write migrated settings to %s!\n",
                            contentConfigFilePath);
                    }
                } else {
                    Debug.print("Failed to create directory for migrated settings at %s!\n",
                        contentConfigFilePath);
                }
            }

            return migrated;
        } finally {
            Config.configFree(migratedConfig);
        }
    }

    /** Port of {@code contentConfigTryMigrateFromF2Res}. */
    public static void contentConfigTryMigrateFromF2Res(String contentConfigPath) {
        String masterPatches = Settings.settings.system.master_patches_path;
        if (masterPatches == null || masterPatches.isEmpty()) {
            Debug.print("Failed to migrate from f2_res.ini: no master_patches is set.\n");
            return;
        }
        if (!CompatFiles.isDir(masterPatches)) {
            return;
        }

        PlatformPaths.Split split = PlatformPaths.splitPath(GameConfig.filePath);
        String f2ResFilePath = PlatformPaths.makePath(split.drive(), split.dir(),
            F2_RES_CONFIG_FILE_NAME, "");

        Config.Cfg legacyConfig = new Config.Cfg();
        if (!Config.configInit(legacyConfig)) {
            return;
        }
        try {
            if (Config.configRead(legacyConfig, f2ResFilePath, false)) {
                String contentCfgPath = masterPatches + "\\" + contentConfigPath;
                if (contentConfigMigrateFromF2Res(legacyConfig, contentCfgPath)) {
                    Debug.print("Migrated settings from f2_res.ini to %s.\n", contentCfgPath);
                }
            }
        } finally {
            Config.configFree(legacyConfig);
        }
    }

    /** Port of {@code contentConfigTryMigrateFromSfall}. */
    public static void contentConfigTryMigrateFromSfall(String contentConfigPath) {
        if (entriesLength(SfallConfig.sfallConfig) == 0) {
            // Nothing to migrate.
            return;
        }
        String masterPatches = Settings.settings.system.master_patches_path;
        if (masterPatches == null || masterPatches.isEmpty()) {
            Debug.print("Failed to migrate from ddraw.ini: no master_patches is set.\n");
            return;
        }
        if (!CompatFiles.isDir(masterPatches)) {
            // master_patches must point to an existing folder. Don't migrate when it's missing or not a directory.
            return;
        }
        String contentCfgPath = masterPatches + "\\" + contentConfigPath;
        if (contentConfigMigrateFromSfall(SfallConfig.sfallConfig, contentCfgPath)) {
            Debug.print("Migrated settings from ddraw.ini to game.cfg.\n");
        }
    }
}
