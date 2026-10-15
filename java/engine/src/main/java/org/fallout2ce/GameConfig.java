package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of {@code src/game_config.h} / {@code src/game_config.cc}.
 *
 * <p>Seams (default = harness behavior): {@code customConfigFile} for the
 * sfall custom-config name, {@code migration} for the f2_res migration.
 */
public final class GameConfig {

    private GameConfig() {
    }

    public static final String DEFAULT_NAME = "fallout2.cfg";
    public static final String MAPPER_NAME = "mapper2.cfg";

    public static final String DEBUG_KEY = "debug";
    public static final String SYSTEM_KEY = "system";
    public static final String SCREEN_KEY = "screen";
    public static final String UI_KEY = "ui";
    public static final String SOUND_KEY = "sound";

    static boolean initialized;
    static final Config.Cfg gameConfig = new Config.Cfg();
    static String filePath = "";

    static final class Override {
        String section = "";
        String key = "";
        // Null when the key had no original value.
        String originalValue;
    }

    static final List<Override> commandLineOverrides = new ArrayList<>();

    /** Sfall seam: custom config file name (null = default). */
    static String customConfigFile;

    /** Migration seam: runs f2_res migration, true when it did. */
    static boolean migrationDone;

    // 0x444570 gconfig_init. argv[0] is the executable path (must exist).
    public static boolean gameConfigInit(boolean isMapper, String[] argv) {
        if (argv == null || argv.length == 0) {
            return false;
        }
        if (initialized) {
            return false;
        }

        if (!Config.configInit(gameConfig)) {
            return false;
        }
        commandLineOverrides.clear();

        // CE: alternative default music directory probe.
        String alternativeMusicPath = PlatformPaths.resolvePath(
                PlatformPaths.windowsPathToNative("data\\sound\\music\\*.acm"));
        List<String> acms = Db.fileNameListInit(alternativeMusicPath);
        if (!acms.isEmpty()) {
            // TODO: apply these to settings directly instead of config
            Config.configSetString(gameConfig, SOUND_KEY, "music_path1", "data\\sound\\music\\");
            Config.configSetString(gameConfig, SOUND_KEY, "music_path2", "data\\sound\\music\\");
        }
        Db.fileNameListFree(acms);

        String configFileName = customConfigFile != null && !customConfigFile.isEmpty()
                ? customConfigFile
                : DEFAULT_NAME;

        String executable = argv[0];
        int sep = executable.lastIndexOf('\\');
        if (sep >= 0) {
            String dir = executable.substring(0, sep);
            filePath = dir + "\\" + (isMapper ? MAPPER_NAME : configFileName);
        } else {
            filePath = isMapper ? MAPPER_NAME : configFileName;
        }

        Config.configRead(gameConfig, filePath, false);

        Debug.print("Game config loaded from %s.\n", filePath);

        if (!isMapper && migrationDone) {
            Debug.print("Migrated settings from f2_res.ini.\n");
            Config.configWriteEx(gameConfig, filePath, Config.RETAIN_ALL);
        }

        Config.configParseCommandLineArguments(gameConfig, argv, GameConfig::recordOverride);

        Settings.settingsWriteToConfig(true);

        initialized = true;

        return true;
    }

    static void recordOverride(String section, String key) {
        Settings.settingsMarkCommandLineOverride(section, key);

        for (Override override : commandLineOverrides) {
            if (override.section.equalsIgnoreCase(section) && override.key.equalsIgnoreCase(key)) {
                return;
            }
        }

        Override override = new Override();
        override.section = section;
        override.key = key;
        String[] originalValue = new String[1];
        if (Config.configGetString(gameConfig, section, key, originalValue)) {
            override.originalValue = originalValue[0];
        }
        commandLineOverrides.add(override);
    }

    // 0x444C14 gconfig_save.
    public static boolean gameConfigSave() {
        if (!initialized) {
            return false;
        }

        try (Config.ScopedConfig savedConfig = new Config.ScopedConfig()) {
            if (!savedConfig.isLoaded() || !Config.configCopy(savedConfig.get(), gameConfig)) {
                return false;
            }

            for (Override override : commandLineOverrides) {
                if (override.originalValue != null) {
                    if (!Config.configSetString(savedConfig.get(), override.section, override.key,
                            override.originalValue)) {
                        return false;
                    }
                } else {
                    if (!Config.configRemoveKey(savedConfig.get(), override.section, override.key)) {
                        return false;
                    }
                }
            }

            if (!Config.configWriteEx(savedConfig.get(), filePath, Config.RETAIN_ALL)) {
                return false;
            }
        }

        return true;
    }

    // 0x444C3C gconfig_exit.
    public static boolean gameConfigExit(boolean shouldSave) {
        if (!initialized) {
            return false;
        }

        boolean result = true;

        if (shouldSave) {
            if (!gameConfigSave()) {
                result = false;
            }
        }

        Config.configFree(gameConfig);

        initialized = false;
        commandLineOverrides.clear();

        return result;
    }

    /** Test seam: vanilla defaults (sfall/migration phases wire these). */
    static void resetForTests() {
        initialized = false;
        Config.configInit(gameConfig);
        filePath = "";
        commandLineOverrides.clear();
        customConfigFile = null;
        migrationDone = false;
    }
}
