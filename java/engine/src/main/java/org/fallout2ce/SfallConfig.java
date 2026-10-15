package org.fallout2ce;

/**
 * Port of {@code src/sfall_config.h} / {@code src/sfall_config.cc}
 * (sfall {@code ddraw.ini} global config).
 */
public final class SfallConfig {

    private SfallConfig() {
    }

    public static final String FILE_NAME = "ddraw.ini";

    public static final String MAIN_KEY = "Main";
    public static final String MISC_KEY = "Misc";
    public static final String SCRIPTS_KEY = "Scripts";

    public static final String OVERRIDE_CRITICALS_MODE_KEY = "OverrideCriticalTable";
    public static final String OVERRIDE_CRITICALS_FILE_KEY = "OverrideCriticalFile";
    public static final String BOOKS_FILE_KEY = "BooksFile";
    public static final String ELEVATORS_FILE_KEY = "ElevatorsFile";
    public static final String PERKS_FILE_KEY = "PerksFile";
    public static final String SKILLS_FILE_KEY = "SkillsFile";
    public static final String UNARMED_FILE_KEY = "UnarmedFile";
    public static final String INI_CONFIG_FOLDER_KEY = "IniConfigFolder";
    public static final String CONFIG_FILE_KEY = "ConfigFile";
    public static final String PATCH_FILE_KEY = "PatchFile";

    static boolean initialized;
    static final Config.Cfg sfallConfig = new Config.Cfg();

    /** Port of {@code sfallConfigInit} (reads ddraw.ini next to argv[0]). */
    public static boolean sfallConfigInit(String[] argv) {
        if (argv == null || argv.length == 0) {
            return false;
        }
        if (initialized) {
            return false;
        }

        if (!Config.configInit(sfallConfig)) {
            return false;
        }

        // Initialize defaults.
        Config.configSetInt(sfallConfig, MISC_KEY, OVERRIDE_CRITICALS_MODE_KEY, 2);
        Config.configSetString(sfallConfig, MISC_KEY, OVERRIDE_CRITICALS_FILE_KEY, "");
        Config.configSetString(sfallConfig, MISC_KEY, BOOKS_FILE_KEY, "");
        Config.configSetString(sfallConfig, MISC_KEY, ELEVATORS_FILE_KEY, "");
        Config.configSetString(sfallConfig, MISC_KEY, PERKS_FILE_KEY, "");
        Config.configSetString(sfallConfig, MISC_KEY, SKILLS_FILE_KEY, "");
        Config.configSetString(sfallConfig, MISC_KEY, CONFIG_FILE_KEY, "");
        Config.configSetString(sfallConfig, MISC_KEY, PATCH_FILE_KEY, "");
        Config.configSetString(sfallConfig, SCRIPTS_KEY, INI_CONFIG_FOLDER_KEY, "");

        String executable = argv[0];
        int sep = executable.lastIndexOf('\\');
        String path = sep != -1
            ? executable.substring(0, sep) + "\\" + FILE_NAME
            : FILE_NAME;

        Config.configRead(sfallConfig, path, false);

        initialized = true;

        return true;
    }

    /** Port of {@code sfallConfigExit}. */
    public static void sfallConfigExit() {
        if (initialized) {
            Config.configFree(sfallConfig);
            initialized = false;
        }
    }

    /** Test-only reset (mirrors a fresh process). */
    static void resetForTests() {
        if (initialized) {
            Config.configFree(sfallConfig);
            initialized = false;
        }
    }
}
