package org.fallout2ce;

/**
 * Engine version string, mirroring {@code src/version.h/cc}.
 *
 * <p>Reads the sfall {@code [main_menu] version_string} override from the
 * shared content config; an empty or missing value falls back to the
 * compiled {@code FALLOUT II 1.02}. The result is truncated to
 * {@code VERSION_MAX - 1} chars ({@code snprintf} semantics).
 */
public final class Version {

    public static final int VERSION_MAX = 32;

    public static final int VERSION_MAJOR = 1;
    public static final int VERSION_MINOR = 2;
    public static final char VERSION_RELEASE = 'R';

    private Version() {
    }

    public static String versionGetVersion() {
        String[] value = new String[1];
        Config.configGetString(ContentConfig.contentConfig, ContentConfig.MAIN_MENU_SECTION, "version_string", value,
                "");
        String result = value[0].isEmpty()
                ? "FALLOUT II " + VERSION_MAJOR + "." + String.format("%02d", VERSION_MINOR)
                : value[0];
        if (result.length() >= VERSION_MAX) {
            result = result.substring(0, VERSION_MAX - 1);
        }
        return result;
    }
}
