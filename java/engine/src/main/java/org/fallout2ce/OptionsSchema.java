package org.fallout2ce;

/**
 * Port of {@code src/options_schema.h} / {@code src/options_schema.cc}
 * (options-screen schema parsing from a Config).
 */
public final class OptionsSchema {

    private OptionsSchema() {
    }

    private static String trim(String str) {
        int start = 0;
        int end = str.length();
        while (start < end && isSpace(str.charAt(start))) {
            start++;
        }
        while (end > start && isSpace(str.charAt(end - 1))) {
            end--;
        }
        return str.substring(start, end);
    }

    private static boolean isSpace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f';
    }

    private static String toLower(String str) {
        StringBuilder out = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            out.append(ch >= 'A' && ch <= 'Z' ? (char) (ch + ('a' - 'A')) : ch);
        }
        return out.toString();
    }

    private static String humanize(String key) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < key.length(); i++) {
            char ch = key.charAt(i);
            if (ch == '_') {
                result.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(ch >= 'a' && ch <= 'z' ? (char) (ch - ('a' - 'A')) : ch);
                capitalizeNext = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static Settings.Category parseCategory(String raw) {
        switch (toLower(trim(raw))) {
            case "screen":
            case "display":
                return Settings.Category.Screen;
            case "interface":
            case "ui":
                return Settings.Category.Interface;
            case "audio":
            case "sound":
                return Settings.Category.Audio;
            case "gameplay":
                return Settings.Category.Gameplay;
            case "preferences":
                return Settings.Category.Preferences;
            case "qualityoflife":
            case "quality_of_life":
            case "qol":
                return Settings.Category.QualityOfLife;
            case "system":
                return Settings.Category.System;
            case "debug":
                return Settings.Category.Debug;
            case "combatai":
            case "combat_ai":
            case "combat":
                return Settings.Category.CombatAi;
            case "mapper":
                return Settings.Category.Mapper;
            default:
                return null;
        }
    }

    private static Settings.ValueType parseValueType(String raw) {
        switch (toLower(trim(raw))) {
            case "bool":
            case "boolean":
                return Settings.ValueType.Boolean;
            case "int":
            case "integer":
                return Settings.ValueType.Integer;
            case "real":
            case "float":
            case "double":
                return Settings.ValueType.Real;
            case "text":
            case "string":
                return Settings.ValueType.Text;
            case "choice":
            case "enum":
                return Settings.ValueType.Choice;
            case "key":
            case "key_binding":
            case "keybinding":
                return Settings.ValueType.KeyBinding;
            default:
                return null;
        }
    }

    private static Settings.ApplyPolicy parseApplyPolicy(String raw) {
        switch (toLower(trim(raw))) {
            case "on_close":
            case "onclose":
                return Settings.ApplyPolicy.OnClose;
            case "next_game":
            case "nextgame":
                return Settings.ApplyPolicy.NextGame;
            case "restart":
                return Settings.ApplyPolicy.Restart;
            default:
                return null;
        }
    }

    private static String readOptionalText(Config.Cfg config, String sectionName, String key,
            String fallback) {
        String[] raw = new String[1];
        if (!Config.configGetString(config, sectionName, key, raw)
                || raw[0] == null || raw[0].isEmpty()) {
            return fallback;
        }
        return trim(raw[0]);
    }

    private static String readOptionalText(Config.Cfg config, String sectionName, String key) {
        return readOptionalText(config, sectionName, key, "");
    }

    static Integer parseInteger(String raw) {
        String trimmed = trim(raw);
        try {
            // NOTE: Integer.decode also accepts '#', which strtol rejects.
            return Integer.decode(trimmed.isEmpty() ? "x" : trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Settings.SettingValue parseSettingValue(String raw, Settings.ValueType type) {
        String trimmed = trim(raw);
        switch (type) {
            case Boolean: {
                String lower = toLower(trimmed);
                if (lower.equals("1") || lower.equals("true") || lower.equals("yes")
                        || lower.equals("on")) {
                    return new Settings.BoolVal(true);
                }
                if (lower.equals("0") || lower.equals("false") || lower.equals("no")
                        || lower.equals("off")) {
                    return new Settings.BoolVal(false);
                }
                return null;
            }
            case Integer:
            case Choice:
            case KeyBinding: {
                Integer number = parseInteger(raw);
                if (number == null) {
                    return null;
                }
                return new Settings.IntVal(number);
            }
            case Real: {
                // NOTE: Double.parseDouble also accepts d/f suffixes, which
                // strtod rejects.
                double number;
                try {
                    number = Double.parseDouble(trimmed);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (!Double.isFinite(number)) {
                    return null;
                }
                return new Settings.RealVal(number);
            }
            case Text:
            default:
                return new Settings.TextVal(trimmed);
        }
    }

    static boolean parseChoices(String raw, java.util.List<Settings.Choice> choices) {
        for (String item : raw.split(",", -1)) {
            item = trim(item);
            if (item.isEmpty()) {
                continue;
            }
            int colon = item.indexOf(':');
            if (colon < 0) {
                return false;
            }
            String label = trim(item.substring(colon + 1));
            Integer value = parseInteger(trim(item.substring(0, colon)));
            if (label.isEmpty() || value == null) {
                return false;
            }
            choices.add(new Settings.Choice(value, label));
        }
        return !choices.isEmpty();
    }

    private static Settings.SettingValue parseDescriptorValue(String raw,
            Settings.Descriptor descriptor) {
        Settings.SettingValue value = parseSettingValue(raw, descriptor.valueType);
        if (value == null) {
            return null;
        }
        if (descriptor.valueType != Settings.ValueType.Choice) {
            return value;
        }
        int choiceValue = ((Settings.IntVal) value).value();
        for (Settings.Choice choice : descriptor.choices) {
            if (choice.value() == choiceValue) {
                return value;
            }
        }
        return null;
    }

    private static Integer parseMessageId(Config.Cfg config, String sectionName, String key) {
        String[] raw = new String[1];
        if (!Config.configGetString(config, sectionName, key, raw)) {
            return 0;
        }
        if (raw[0] == null) {
            return null;
        }
        return parseInteger(raw[0]);
    }

    /**
     * Port of {@code optionsSchemaParseSection} (leaves {@code out} alone
     * on failure).
     */
    public static boolean optionsSchemaParseSection(Config.Cfg config, String sectionName,
            Settings.Descriptor out) {
        if (config == null || sectionName == null || out == null) {
            return false;
        }

        int dot = sectionName.indexOf('.');
        if (dot <= 0 || dot == sectionName.length() - 1) {
            return false;
        }

        Settings.Descriptor descriptor = new Settings.Descriptor();
        descriptor.id = sectionName;
        descriptor.section = sectionName.substring(0, dot);
        descriptor.key = sectionName.substring(dot + 1);

        descriptor.source = readOptionalText(config, sectionName, "file", "fallout2.cfg");

        String[] typeStr = new String[1];
        Settings.ValueType valueType;
        if (!Config.configGetString(config, sectionName, "type", typeStr)
                || typeStr[0] == null
                || (valueType = parseValueType(typeStr[0])) == null) {
            return false;
        }
        descriptor.valueType = valueType;

        String[] catStr = new String[1];
        Settings.Category category;
        if (!Config.configGetString(config, sectionName, "category", catStr)
                || catStr[0] == null
                || (category = parseCategory(catStr[0])) == null) {
            return false;
        }
        descriptor.category = category;

        descriptor.subsection = readOptionalText(config, sectionName, "subsection");

        String[] choicesStr = new String[1];
        if (Config.configGetString(config, sectionName, "choices", choicesStr)
                && choicesStr[0] != null) {
            if (!parseChoices(choicesStr[0], descriptor.choices)) {
                return false;
            }
        }
        if (descriptor.valueType == Settings.ValueType.Choice && descriptor.choices.isEmpty()) {
            return false;
        }
        if (descriptor.valueType != Settings.ValueType.Choice && !descriptor.choices.isEmpty()) {
            return false;
        }

        String[] defStr = new String[1];
        Settings.SettingValue defaultValue;
        if (!Config.configGetString(config, sectionName, "default", defStr)
                || defStr[0] == null
                || (defaultValue = parseDescriptorValue(defStr[0], descriptor)) == null) {
            return false;
        }
        descriptor.defaultValue = defaultValue;

        String[] vanillaStr = new String[1];
        if (Config.configGetString(config, sectionName, "vanilla", vanillaStr)
                && vanillaStr[0] != null) {
            Settings.SettingValue vanillaValue = parseDescriptorValue(vanillaStr[0], descriptor);
            if (vanillaValue == null) {
                return false;
            }
            descriptor.vanillaValue = vanillaValue;
        }

        Integer labelId = parseMessageId(config, sectionName, "label_id");
        if (labelId == null) {
            return false;
        }
        descriptor.labelMessageId = labelId;

        descriptor.fallbackLabel =
            readOptionalText(config, sectionName, "label", humanize(descriptor.key));

        Integer descId = parseMessageId(config, sectionName, "desc_id");
        if (descId == null) {
            return false;
        }
        descriptor.descriptionMessageId = descId;

        descriptor.fallbackDescription = readOptionalText(config, sectionName, "description");

        descriptor.asset = readOptionalText(config, sectionName, "asset");

        String[] applyStr = new String[1];
        if (Config.configGetString(config, sectionName, "apply", applyStr)
                && applyStr[0] != null) {
            Settings.ApplyPolicy applyPolicy = parseApplyPolicy(applyStr[0]);
            if (applyPolicy == null) {
                return false;
            }
            descriptor.applyPolicy = applyPolicy;
        }

        out.id = descriptor.id;
        out.section = descriptor.section;
        out.key = descriptor.key;
        out.source = descriptor.source;
        out.valueType = descriptor.valueType;
        out.category = descriptor.category;
        out.subsection = descriptor.subsection;
        out.choices.clear();
        out.choices.addAll(descriptor.choices);
        out.defaultValue = descriptor.defaultValue;
        out.vanillaValue = descriptor.vanillaValue;
        out.labelMessageId = descriptor.labelMessageId;
        out.fallbackLabel = descriptor.fallbackLabel;
        out.descriptionMessageId = descriptor.descriptionMessageId;
        out.fallbackDescription = descriptor.fallbackDescription;
        out.asset = descriptor.asset;
        out.applyPolicy = descriptor.applyPolicy;
        return true;
    }

    /**
     * Port of {@code optionsSchemaParse} (leaves {@code out} alone on
     * failure).
     */
    public static boolean optionsSchemaParse(Config.Cfg config,
            java.util.List<Settings.Descriptor> out) {
        if (config == null || out == null) {
            return false;
        }

        java.util.List<Settings.Descriptor> descriptors = new java.util.ArrayList<>();
        for (String sectionName : config.sections.keySet()) {
            Settings.Descriptor descriptor = new Settings.Descriptor();
            if (!optionsSchemaParseSection(config, sectionName, descriptor)) {
                return false;
            }
            descriptors.add(descriptor);
        }
        out.clear();
        out.addAll(descriptors);
        return true;
    }
}
