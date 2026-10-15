package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [settings]} vectors from {@code docker/refharness/golden/v10.txt}.
 * Config paths stay inside {@code @TempDir} via a backslashed argv[0]
 * (the C dir extraction looks for {@code \\} only).
 */
class SettingsTest {

    @TempDir
    Path temp;

    String argv0;

    @BeforeEach
    void reset() {
        Settings.resetStateForTests();
        Db.resetForTests();
        argv0 = temp.toString() + "\\prog";
    }

    static Settings.Descriptor find(String id) {
        for (Settings.Descriptor d : Settings.settingsGetDescriptors()) {
            if (d.id.equals(id)) {
                return d;
            }
        }
        throw new AssertionError("missing descriptor " + id);
    }

    @Test
    void defaults() {
        assertTrue(Settings.settingsInit(false, new String[] { argv0 }));
        assertEquals(640, Settings.settings.screen.resolution_x);
        assertEquals(480, Settings.settings.screen.resolution_y);
        assertEquals(0, Settings.settings.screen.windowed);
        assertTrue(Settings.settings.sound.music);
        assertEquals("english", Settings.settings.system.language);
        assertFalse(Settings.settingsInit(false, new String[] { argv0 }));
        assertTrue(Settings.settingsExit(false));
    }

    @Test
    void setValidateSplit() {
        assertTrue(Settings.settingsInit(false, new String[] { argv0 }));
        Settings.Descriptor resx = find("screen.resolution_x");
        assertEquals(new Settings.IntVal(640), Settings.settingsGetValue(resx));

        StringBuilder error = new StringBuilder();
        assertTrue(Settings.settingsSetValue(resx, new Settings.IntVal(800), error));
        assertEquals(new Settings.IntVal(640), Settings.settingsGetValue(resx));
        assertEquals(new Settings.IntVal(800), Settings.settingsGetConfiguredValue(resx));

        assertFalse(Settings.settingsSetValue(resx, new Settings.IntVal(100), error));
        assertEquals("Value is outside the allowed range.", error.toString());
        assertFalse(Settings.settingsSetValue(resx, new Settings.BoolVal(true), error));
        assertEquals("Value has the wrong type.", error.toString());
        assertTrue(Settings.settingsExit(false));
    }

    @Test
    void clampOnRead() throws Exception {
        Files.writeString(Path.of(temp.toString(), "fallout2.cfg"), "[screen]\nresolution_x=100\n");
        assertTrue(Settings.settingsInit(false, new String[] { argv0 }));
        assertEquals(640, Settings.settings.screen.resolution_x);
        assertTrue(Settings.settingsExit(false));
    }

    @Test
    void cliOverrideFlagAndRefusal() {
        String[] argv = { argv0, "[screen]resolution_x=1024", "[ui]skip_opening_movies=2" };
        assertTrue(Settings.settingsInit(false, argv));
        assertEquals(1024, Settings.settings.screen.resolution_x);
        assertEquals(2, Settings.settings.ui.skip_opening_movies);

        Settings.Descriptor resx = find("screen.resolution_x");
        assertTrue(resx.commandLineOverride);

        StringBuilder error = new StringBuilder();
        assertFalse(Settings.settingsSetValue(resx, new Settings.IntVal(800), error));
        assertEquals("Setting is overridden by the command line.", error.toString());
        assertTrue(Settings.settingsExit(false));
    }

    @Test
    void saveStripsOverrides() throws Exception {
        String[] argv = { argv0, "[screen]resolution_x=1024", "[ui]skip_opening_movies=2" };
        assertTrue(Settings.settingsInit(false, argv));
        assertTrue(Settings.settingsSave());

        String saved = Files.readString(Path.of(temp.toString(), "fallout2.cfg"));
        assertTrue(!saved.contains("resolution_x"));
        assertTrue(!saved.contains("skip_opening_movies"));
        assertTrue(Settings.settingsExit(false));
    }

    @Test
    void fieldsSurviveReinit() {
        String[] argv = { argv0, "[screen]resolution_x=1024" };
        assertTrue(Settings.settingsInit(false, argv));
        assertTrue(Settings.settingsExit(false));

        // No CLI this time and no key on disk: the field keeps 1024
        // (C never resets struct fields).
        assertTrue(Settings.settingsInit(false, new String[] { argv0 }));
        assertEquals(1024, Settings.settings.screen.resolution_x);
        assertTrue(Settings.settingsExit(false));
    }

    @Test
    void gameConfigGuards() {
        assertTrue(GameConfig.gameConfigInit(false, new String[] { argv0 }));
        assertFalse(GameConfig.gameConfigInit(false, new String[] { argv0 }));
        assertTrue(GameConfig.gameConfigExit(false));
        assertFalse(GameConfig.gameConfigExit(false));
        assertTrue(GameConfig.gameConfigInit(false, new String[] { argv0 }));
        assertTrue(GameConfig.gameConfigExit(false));
    }

    @Test
    void descriptorDefaults() {
        assertTrue(Settings.settingsInit(false, new String[] { argv0 }));
        Settings.Descriptor resx = find("screen.resolution_x");
        assertEquals("fallout2.cfg", resx.source);
        assertEquals(Settings.ValueType.Integer, resx.valueType);
        assertEquals(new Settings.IntVal(640), resx.defaultValue);
        assertTrue(Settings.settingsExit(false));
    }
}
