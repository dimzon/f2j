package org.fallout2ce;

/**
 * Port of {@code src/content_config.h} / {@code src/content_config.cc}
 * (content {@code game.cfg} global config).
 */
public final class ContentConfig {

    private ContentConfig() {
    }

    public static final String START_SECTION = "start";
    public static final String KARMA_SECTION = "karma";
    public static final String ITEMS_SECTION = "items";
    public static final String MAPS_SECTION = "maps";
    public static final String DIALOG_SECTION = "dialog";
    public static final String MISC_SECTION = "misc";
    public static final String MAIN_MENU_SECTION = "main_menu";
    public static final String SOUND_SECTION = "sound";
    public static final String MOVIES_SECTION = "movies";
    public static final String STATS_SECTION = "stats";
    public static final String COMBAT_SECTION = "combat";
    public static final String EXPLOSIONS_SECTION = "explosions";
    public static final String SKILLDEX_SECTION = "skilldex";
    public static final String WORLDMAP_SECTION = "worldmap";
    public static final String CHARACTERS_SECTION = "characters";
    public static final String TEXT_SECTION = "text";

    static final String CONFIG_PATH = "config\\game.cfg";
    static final String CONFIG_PATCH_PATH = "config\\game#patch.cfg";

    static final Config.Cfg contentConfig = new Config.Cfg();

    /** Port of {@code contentConfigInit}. */
    public static void contentConfigInit() {
        if (contentConfig.initialized) {
            return;
        }

        // Try to migrate some settings from legacy config files.
        GameConfigMigration.contentConfigTryMigrateFromSfall(CONFIG_PATCH_PATH);
        GameConfigMigration.contentConfigTryMigrateFromF2Res(CONFIG_PATCH_PATH);

        if (!Config.configInit(contentConfig)) {
            return;
        }

        Config.configRead(contentConfig, CONFIG_PATH, true);
        // Patch config allows to override only certain fields, without replacing the whole file.
        // TODO: remove this after config patching by mods is implemented inside configRead
        Config.configRead(contentConfig, CONFIG_PATCH_PATH, true);
    }

    /** Port of {@code contentConfigExit}. */
    public static void contentConfigExit() {
        if (!contentConfig.initialized) {
            return;
        }

        Config.configFree(contentConfig);
    }

    /** Test-only reset (mirrors a fresh process). */
    static void resetForTests() {
        if (contentConfig.initialized) {
            Config.configFree(contentConfig);
        }
    }
}
