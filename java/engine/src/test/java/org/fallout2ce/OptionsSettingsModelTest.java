package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Model vectors from {@code docker/refharness/golden/v27.txt}
 * ({@code [options]}): the commit applies to configured (Restart defers
 * the live value, like the harness {@code active=640 configured=800}).
 */
class OptionsSettingsModelTest {

    @TempDir
    Path temp;

    String argv0;

    @BeforeEach
    void reset() throws Exception {
        Settings.resetStateForTests();
        Db.resetForTests();
        argv0 = temp.toString() + "\\prog";
        Files.writeString(temp.resolve("fallout2.cfg"), "[screen]\nresolution_x=640\n");
    }

    @AfterEach
    void cleanup() {
        Settings.resetStateForTests();
        Db.resetForTests();
    }

    static Settings.Descriptor find(String id) {
        for (Settings.Descriptor descriptor : Settings.settingsGetDescriptors()) {
            if (descriptor.id.equals(id)) {
                return descriptor;
            }
        }
        throw new AssertionError("missing descriptor " + id);
    }

    @Test
    void editLifecycle() {
        assertTrue(Settings.settingsInit(false, new String[] { argv0 }));

        OptionsSettingsModel model = new OptionsSettingsModel();
        assertFalse(model.edits().isEmpty());
        assertFalse(model.isDirty());

        StringBuilder error = new StringBuilder();
        assertTrue(model.setValue("screen.resolution_x", new Settings.IntVal(800), error));
        assertTrue(model.isDirty());
        assertTrue(model.isDirty("screen.resolution_x"));
        assertFalse(model.isDirty("sound.music"));

        assertFalse(model.setValue("nope.nope", new Settings.IntVal(1), error));
        assertEquals("Setting is not registered.", error.toString());
        error.setLength(0);
        assertFalse(model.setValue("screen.resolution_x", new Settings.BoolVal(true), error));
        assertEquals("Value has the wrong type.", error.toString());

        model.rollback();
        assertFalse(model.isDirty());

        assertTrue(model.resetToDefault("screen.resolution_x", error));
        model.rollback();

        assertTrue(model.setValue("screen.resolution_x", new Settings.IntVal(800), null));
        assertTrue(model.commit(error));
        assertEquals(new Settings.IntVal(640),
            Settings.settingsGetValue(find("screen.resolution_x")));
        assertEquals(new Settings.IntVal(800),
            Settings.settingsGetConfiguredValue(find("screen.resolution_x")));
        assertFalse(model.isDirty());

        assertTrue(Settings.settingsExit(false));
    }

    @Test
    void cliOverrideRefuses() {
        String[] argv = { argv0, "[screen]resolution_x=1024" };
        assertTrue(Settings.settingsInit(false, argv));

        OptionsSettingsModel model = new OptionsSettingsModel();
        StringBuilder error = new StringBuilder();
        assertFalse(model.setValue("screen.resolution_x", new Settings.IntVal(800), error));
        assertEquals("Setting is overridden by the command line.", error.toString());

        assertTrue(Settings.settingsExit(false));
    }
}
