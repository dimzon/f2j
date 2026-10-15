package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Schema vectors from {@code docker/refharness/golden/v27.txt}
 * ({@code [options]}), parsed from an equivalent schema file.
 */
class OptionsSchemaTest {

    static Config.Cfg schema() throws Exception {
        String text = "[screen.resolution_x]\nfile = fallout2.cfg\ntype = int\ncategory = screen\n"
            + "default = 640\nlabel_id = 10\nlabel = Resolution Width\n"
            + "desc_id = 11\ndescription = Horizontal resolution.\napply = restart\n"
            + "[ui.anim_speed]\nfile = fallout2.cfg\ntype = real\ncategory = interface\n"
            + "default = 1.0\n"
            + "[preferences.combat_speed]\ntype = choice\ncategory = gameplay\n"
            + "choices = 0:Slow, 1:Normal, 2:Fast\ndefault = 1\nvanilla = 1\n"
            + "[debug.show_fps]\ntype = bool\ncategory = debug\ndefault = 0\n"
            + "[ui.motd]\ntype = text\ncategory = interface\ndefault = hello\n"
            + "[ui.hotkey]\ntype = keybinding\ncategory = interface\ndefault = 13\n";
        Path tmp = Files.createTempFile("f2j-schema", ".cfg");
        Files.writeString(tmp, text);
        Config.Cfg cfg = new Config.Cfg();
        assertTrue(Config.configInit(cfg));
        assertTrue(Config.configRead(cfg, tmp.toString(), false));
        return cfg;
    }

    static Settings.Descriptor parseOne(String section) throws Exception {
        Config.Cfg cfg = schema();
        Settings.Descriptor out = new Settings.Descriptor();
        assertTrue(OptionsSchema.optionsSchemaParseSection(cfg, section, out));
        return out;
    }

    @Test
    void parseAll() throws Exception {
        List<Settings.Descriptor> descriptors = new ArrayList<>();
        assertTrue(OptionsSchema.optionsSchemaParse(schema(), descriptors));
        assertEquals(6, descriptors.size());

        // Dictionary order (matches the C dictionary iteration).
        assertEquals("debug.show_fps", descriptors.get(0).id);
        assertEquals("preferences.combat_speed", descriptors.get(1).id);
        assertEquals("screen.resolution_x", descriptors.get(2).id);
        assertEquals("ui.anim_speed", descriptors.get(3).id);
        assertEquals("ui.hotkey", descriptors.get(4).id);
        assertEquals("ui.motd", descriptors.get(5).id);
    }

    @Test
    void intDescriptor() throws Exception {
        Settings.Descriptor desc = parseOne("screen.resolution_x");
        assertEquals("screen.resolution_x", desc.id);
        assertEquals(Settings.ValueType.Integer, desc.valueType);
        assertEquals(Settings.Category.Screen, desc.category);
        assertEquals("screen", desc.section);
        assertEquals("resolution_x", desc.key);
        assertEquals("fallout2.cfg", desc.source);
        assertEquals(new Settings.IntVal(640), desc.defaultValue);
        assertEquals(10, desc.labelMessageId);
        assertEquals("Resolution Width", desc.fallbackLabel);
        assertEquals(11, desc.descriptionMessageId);
        assertEquals("Horizontal resolution.", desc.fallbackDescription);
        assertEquals(Settings.ApplyPolicy.Restart, desc.applyPolicy);
        assertTrue(desc.choices.isEmpty());
    }

    @Test
    void otherTypes() throws Exception {
        Settings.Descriptor real = parseOne("ui.anim_speed");
        assertEquals(Settings.ValueType.Real, real.valueType);
        assertEquals(new Settings.RealVal(1.0), real.defaultValue);
        assertEquals("Anim Speed", real.fallbackLabel);

        Settings.Descriptor bool = parseOne("debug.show_fps");
        assertEquals(Settings.ValueType.Boolean, bool.valueType);
        assertEquals(new Settings.BoolVal(false), bool.defaultValue);

        Settings.Descriptor text = parseOne("ui.motd");
        assertEquals(Settings.ValueType.Text, text.valueType);
        assertEquals(new Settings.TextVal("hello"), text.defaultValue);

        Settings.Descriptor key = parseOne("ui.hotkey");
        assertEquals(Settings.ValueType.KeyBinding, key.valueType);
        assertEquals(new Settings.IntVal(13), key.defaultValue);

        Settings.Descriptor choice = parseOne("preferences.combat_speed");
        assertEquals(Settings.ValueType.Choice, choice.valueType);
        assertEquals(new Settings.IntVal(1), choice.defaultValue);
        assertEquals(new Settings.IntVal(1), choice.vanillaValue);
        assertEquals(3, choice.choices.size());
        assertEquals("Normal", choice.choices.get(1).fallbackLabel());
    }

    @Test
    void badSections() throws Exception {
        Path tmp = Files.createTempFile("f2j-schema-bad", ".cfg");
        Files.writeString(tmp,
            "[screen.ok]\ntype = int\ncategory = screen\ndefault = 1\n"
                + "[screen.bad]\ntype = frobnicate\ncategory = screen\ndefault = 1\n");
        Config.Cfg cfg = new Config.Cfg();
        assertTrue(Config.configInit(cfg));
        assertTrue(Config.configRead(cfg, tmp.toString(), false));
        assertFalse(OptionsSchema.optionsSchemaParse(cfg, new ArrayList<>()));

        Config.Cfg empty = new Config.Cfg();
        assertTrue(Config.configInit(empty));
        assertFalse(OptionsSchema.optionsSchemaParseSection(empty, "screennodot",
            new Settings.Descriptor()));
        assertFalse(OptionsSchema.optionsSchemaParseSection(null, "screen.x",
            new Settings.Descriptor()));
        Config.configFree(empty);
    }
}
