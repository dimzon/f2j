package org.fallout2ce;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of {@code src/options_settings_model.h} /
 * {@code src/options_settings_model.cc} (staged options edits with
 * validate/commit/rollback).
 */
public final class OptionsSettingsModel {

    /** Port of {@code OptionsSettingEdit}. */
    public static final class Edit {
        public final Settings.Descriptor descriptor;
        public Settings.SettingValue originalValue;
        public Settings.SettingValue value;

        Edit(Settings.Descriptor descriptor, Settings.SettingValue originalValue,
                Settings.SettingValue value) {
            this.descriptor = descriptor;
            this.originalValue = originalValue;
            this.value = value;
        }
    }

    private final List<Edit> edits = new ArrayList<>();

    /** Port of {@code OptionsSettingsModel()} (snapshots configured values). */
    public OptionsSettingsModel() {
        List<Settings.Descriptor> descriptors = Settings.settingsGetDescriptors();
        for (Settings.Descriptor descriptor : descriptors) {
            Settings.SettingValue value = Settings.settingsGetConfiguredValue(descriptor);
            edits.add(new Edit(descriptor, value, value));
        }
    }

    /** Port of {@code edits()}. */
    public List<Edit> edits() {
        return edits;
    }

    /** Port of {@code setValue} (error goes to the builder when given). */
    public boolean setValue(String id, Settings.SettingValue value, StringBuilder error) {
        Edit edit = findEdit(id);
        if (edit == null) {
            setError(error, "Setting is not registered.");
            return false;
        }
        if (edit.descriptor.readOnly) {
            setError(error, "Setting is read-only.");
            return false;
        }
        if (edit.descriptor.commandLineOverride) {
            setError(error, "Setting is overridden by the command line.");
            return false;
        }
        if (!Settings.settingsValidateValue(edit.descriptor, value, error)) {
            return false;
        }

        edit.value = value;
        return true;
    }

    /** Port of {@code resetToDefault}. */
    public boolean resetToDefault(String id, StringBuilder error) {
        Edit edit = findEdit(id);
        if (edit == null) {
            setError(error, "Setting is not registered.");
            return false;
        }
        return setValue(id, edit.descriptor.defaultValue, error);
    }

    /** Port of {@code isDirty()}. */
    public boolean isDirty() {
        for (Edit edit : edits) {
            if (!edit.value.equals(edit.originalValue)) {
                return true;
            }
        }
        return false;
    }

    /** Port of {@code isDirty(id)}. */
    public boolean isDirty(String id) {
        Edit edit = findEdit(id);
        return edit != null && !edit.value.equals(edit.originalValue);
    }

    /** Port of {@code rollback}. */
    public void rollback() {
        for (Edit edit : edits) {
            edit.value = edit.originalValue;
        }
    }

    /** Port of {@code commit} (applies, saves, refreshes the snapshot). */
    public boolean commit(StringBuilder error) {
        for (Edit edit : edits) {
            if (!Settings.settingsValidateValue(edit.descriptor, edit.value, error)) {
                return false;
            }
        }

        for (Edit edit : edits) {
            if (edit.value.equals(edit.originalValue)) {
                continue;
            }
            if (!Settings.settingsSetValue(edit.descriptor, edit.value, error)) {
                for (Edit rollback : edits) {
                    if (rollback.value.equals(rollback.originalValue)) {
                        continue;
                    }
                    Settings.settingsSetValue(rollback.descriptor, rollback.originalValue, null);
                }
                return false;
            }
        }

        if (!Settings.settingsSave()) {
            for (Edit edit : edits) {
                if (edit.value.equals(edit.originalValue)) {
                    continue;
                }
                Settings.settingsSetValue(edit.descriptor, edit.originalValue, null);
            }
            Settings.settingsWriteToConfig(false);
            setError(error, "Unable to save the configuration file.");
            return false;
        }

        for (Edit edit : edits) {
            edit.originalValue = Settings.settingsGetConfiguredValue(edit.descriptor);
            edit.value = edit.originalValue;
        }
        return true;
    }

    private Edit findEdit(String id) {
        for (Edit edit : edits) {
            if (edit.descriptor.id.equals(id)) {
                return edit;
            }
        }
        return null;
    }

    private static void setError(StringBuilder error, String message) {
        if (error != null) {
            error.setLength(0);
            error.append(message);
        }
    }
}
