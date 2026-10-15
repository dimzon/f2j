package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code [gamedefs]} version vectors from
 * {@code docker/refharness/golden/v29.txt}.
 */
class VersionTest {

    @AfterEach
    void reset() {
        ContentConfig.resetForTests();
    }

    @Test
    void defaultVersion() {
        ContentConfig.resetForTests();
        assertEquals("FALLOUT II 1.02", Version.versionGetVersion());
    }

    @Test
    void customVersion() {
        ContentConfig.resetForTests();
        assertTrue(Config.configInit(ContentConfig.contentConfig));
        assertTrue(Config.configSetString(ContentConfig.contentConfig, ContentConfig.MAIN_MENU_SECTION,
                "version_string", "Custom Build 123456789012345678901234567890EXTRA"));
        // snprintf truncation to VERSION_MAX - 1 chars, like the C++ len=31.
        assertEquals("Custom Build 123456789012345678", Version.versionGetVersion());
    }

    @Test
    void emptyVersionFallsBack() {
        ContentConfig.resetForTests();
        assertTrue(Config.configInit(ContentConfig.contentConfig));
        assertTrue(Config.configSetString(ContentConfig.contentConfig, ContentConfig.MAIN_MENU_SECTION,
                "version_string", ""));
        assertEquals("FALLOUT II 1.02", Version.versionGetVersion());
    }
}
