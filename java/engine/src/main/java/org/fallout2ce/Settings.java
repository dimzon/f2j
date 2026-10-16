package org.fallout2ce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Port of {@code src/settings.h} / {@code src/settings.cc}: typed settings
 * structs with defaults, the config-backed registry (read/write/validate)
 * and init/save/exit lifecycle.
 */
public final class Settings {

    private Settings() {
    }

    public enum ValueType {
        Boolean,
        Integer,
        Real,
        Text,
        Choice,
        KeyBinding
    }

    public enum Category {
        Uncategorized,
        System,
        Screen,
        Interface,
        Gameplay,
        Preferences,
        Audio,
        Debug,
        CombatAi,
        QualityOfLife,
        Mapper
    }

    public enum ApplyPolicy {
        OnClose,
        NextGame,
        Restart
    }

    /**
     * Port of {@code GameDifficulty} in {@code src/game_config.h} (values
     * travel as int, like C; {@code PreferencesSettings.game_difficulty}
     * defaults to {@code NORMAL}).
     */
    public static final int GAME_DIFFICULTY_EASY = 0;
    public static final int GAME_DIFFICULTY_NORMAL = 1;
    public static final int GAME_DIFFICULTY_HARD = 2;
    public static final int GAME_DIFFICULTY_MIN = GAME_DIFFICULTY_EASY;
    public static final int GAME_DIFFICULTY_MAX = GAME_DIFFICULTY_HARD;

    public enum GameDifficulty {
        Easy(GAME_DIFFICULTY_EASY),
        Normal(GAME_DIFFICULTY_NORMAL),
        Hard(GAME_DIFFICULTY_HARD);

        private final int id;

        GameDifficulty(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    private static final java.util.Map<Integer, GameDifficulty> GAME_DIFFICULTY_BY_ID = new java.util.HashMap<>();

    static {
        for (GameDifficulty e : GameDifficulty.values()) {
            GAME_DIFFICULTY_BY_ID.put(e.id, e);
        }
    }

    public static GameDifficulty ofGameDifficulty(int id) {
        return GAME_DIFFICULTY_BY_ID.get(id);
    }

    /** Port of {@code SettingValue} (enums travel as int, like C). */
    public sealed interface SettingValue permits BoolVal, IntVal, RealVal, TextVal {
    }

    public record BoolVal(boolean value) implements SettingValue {
    }

    public record IntVal(int value) implements SettingValue {
    }

    public record RealVal(double value) implements SettingValue {
    }

    public record TextVal(String value) implements SettingValue {
    }

    public record Choice(int value, String fallbackLabel) {
    }

    public static final class Descriptor {
        public String id = "";
        public String source = "fallout2.cfg";
        public String section = "";
        public String key = "";
        public ValueType valueType = ValueType.Boolean;
        public Category category = Category.Uncategorized;
        public ApplyPolicy applyPolicy = ApplyPolicy.Restart;
        public SettingValue defaultValue = new BoolVal(false);
        public boolean readOnly;
        public boolean sensitive;
        public boolean applicable = true;
        public boolean commandLineOverride;
        public int categoryOrder;
        public String subsection = "";
        public int labelMessageId = -1;
        public int descriptionMessageId = -1;
        public String fallbackLabel = "";
        public String fallbackDescription = "";
        public String asset = "";
        public final List<Choice> choices = new ArrayList<>();
        public SettingValue vanillaValue;
    }

    // --- structs (defaults verbatim from settings.h) ---

    public static final class SystemSettings {
        public String executable = "game";
        public String master_dat_path = "master.dat";
        public String master_patches_path = "data";
        public String f2_res_dat_path = "f2_res.dat";
        public String critter_dat_path = "critter.dat";
        public String critter_patches_path = "data";
        public String language = Message.ENGLISH;
        public int scroll_lock;
        public boolean interrupt_walk = true;
        public int art_cache_size = 32;
        public boolean color_cycling = true;
        public int cycle_speed_factor = 1;
        public boolean hashing = true;
        public int splash;
        public int free_space = 20480;
        public int times_run;
        public String screenshots_format = "png";

        public boolean executableIsMapper() {
            return executable.equalsIgnoreCase("mapper");
        }
    }

    public static final class ScreenSettings {
        public int resolution_x = 640;
        public int resolution_y = 480;
        public int windowed = 0; // WindowMode::Fullscreen
        public boolean mouse_lock;
        public int scale = 1;
    }

    public static final class UISettings {
        public int main_menu_scale_mode = 1;
        public boolean in_game_menu_help = true;
        public boolean main_menu_overlay_subscreens;
        public boolean iface_bar_mode;
        public boolean perks_progress_bar;
        public int iface_bar_width = 800;
        public int iface_bar_side_art = 2;
        public boolean iface_bar_sides_ori;
        public int alternate_ammo_meter;
        public int splash_screen_size = 1;
        public int death_screen_size = 1;
        public int end_slide_size = 1;
        public boolean movie_aspect_fit = true;
        public boolean edg_support = true;
        public boolean ignore_scroll_limit;
        public boolean ignore_map_edges;
        public boolean quick_toolbar_visible;
        public double anim_speed = 1.0;
        public int skip_opening_movies;
        public boolean display_karma_changes;
        public boolean display_bonus_damage;
        public boolean numbers_in_dialogue;
        public boolean party_member_extra_info;
        public boolean dialog_border = true;
        public int auto_quick_save;
        public boolean enable_high_resolution_stencil = true;
        public boolean extend_ap_bar;
        public boolean expand_barter_window;
        public int inventory_columns = 1;
        public int loot_weight_indicator = 1;
        public int loot_container_size_indicator_threshold = 50;
    }

    public static final class GameplaySettings {
        public int perk_carryover = 1; // PERK_CARRY_OVER_MODE_ON
    }

    public static final class PreferencesSettings {
        public int game_difficulty = 1; // NORMAL
        public int combat_difficulty = 1; // NORMAL
        public int violence_level = 3; // VIOLENCE_LEVEL_MAXIMUM_BLOOD
        public int target_highlight = 2; // TARGET_HIGHLIGHT_TARGETING_ONLY
        public boolean item_highlight = true;
        public boolean combat_looks;
        public boolean combat_messages = true;
        public boolean combat_taunts = true;
        public boolean language_filter;
        public boolean running;
        public boolean subtitles;
        public int combat_speed;
        public boolean player_speedup;
        public double text_base_delay = 3.5;
        public double text_line_delay = 1.399994;
        public double brightness = 1.0;
        public double mouse_sensitivity = 1.0;
        public boolean running_burning_guy = true;
    }

    public static final class SoundSettings {
        public boolean initialize = true;
        public boolean debug;
        public boolean debug_sfxc = true;
        public boolean sounds = true;
        public boolean music = true;
        public boolean speech = true;
        public boolean float_speech = true;
        public boolean pipboy_speech = true;
        public int master_volume = 22281;
        public int music_volume = 22281;
        public int sndfx_volume = 22281;
        public int speech_volume = 22281;
        public int cache_size = 448;
        public String music_path1 = "sound\\music\\";
        public String music_path2 = "sound\\music\\";
        public int gapless_music = 1;
        public int music_channels = 1;
        public int speech_channels = 1;
        public int sfx_channels = 4;
        public int movie_channels = 1;
        public int script_channels = 4;
        public int float_channels = 4;
        public int pipboy_channels = 1;
    }

    public static final class DebugSettings {
        public String mode = "environment";
        public boolean show_fps;
        public boolean show_tile_num;
        public boolean show_script_messages;
        public boolean show_load_info;
        public boolean output_map_data_info;
        public int window_width = 300;
        public int window_height = 192;
        public String console_output_path = "";
    }

    public static final class CombatAiSettings {
        public int npcs_try_to_spend_extra_ap;
    }

    public static final class QolSettings {
        public int use_walk_distance = 3;
        public boolean walk_when_sneaking;
        public boolean auto_open_doors;
        public boolean party_trade_from_menu = true;
        public boolean party_loot_and_barter;
        public boolean fast_ammo_load = true;
    }

    public static final class MapperSettings {
        public boolean override_librarian;
        public boolean librarian;
        public boolean use_art_not_protos;
        public boolean rebuild_protos;
        public boolean fix_map_objects;
        public boolean fix_map_inventory;
        public boolean ignore_rebuild_errors;
        public boolean show_pid_numbers;
        public boolean save_text_maps;
        public boolean run_mapper_as_game;
        public boolean default_f8_as_game = true;
        public boolean sort_script_list;
        public String map = "";
        public String dev_path = "";
        public boolean use_grid_item_picker = true;
    }

    public static final class State {
        public final SystemSettings system = new SystemSettings();
        public final ScreenSettings screen = new ScreenSettings();
        public final UISettings ui = new UISettings();
        public final GameplaySettings gameplay = new GameplaySettings();
        public final PreferencesSettings preferences = new PreferencesSettings();
        public final SoundSettings sound = new SoundSettings();
        public final DebugSettings debug = new DebugSettings();
        public final CombatAiSettings combat_ai = new CombatAiSettings();
        public final QolSettings qol = new QolSettings();
        public final MapperSettings mapper = new MapperSettings();
    }

    public static State settings = new State();

    static final class RegistryEntry {
        final Descriptor descriptor = new Descriptor();
        Runnable read = () -> {
        };
        Consumer<Boolean> write = onlyAdd -> {
        };
        Supplier<SettingValue> get = () -> new BoolVal(false);
        Consumer<SettingValue> set = value -> {
        };
        Function<SettingValue, Boolean> validate = value -> true;
        SettingValue restartValue;
    }

    static final List<RegistryEntry> registry = new ArrayList<>();
    static final List<Descriptor> descriptors = new ArrayList<>();

    // --- small value holders to wire struct fields without reflection ---

    interface IntField {
        int get();

        void set(int value);
    }

    interface BoolField {
        boolean get();

        void set(boolean value);
    }

    interface DoubleField {
        double get();

        void set(double value);
    }

    interface StringField {
        String get();

        void set(String value);
    }

    static int clampInt(int value, int min, int max, String section, String key) {
        int clamped = Math.min(max, Math.max(min, value));
        if (clamped != value) {
            Debug.print("config value %s.%s was clamped.\n", section, key);
        }
        return clamped;
    }

    static double clampDouble(double value, double min, double max, String section, String key) {
        double clamped = Math.min(max, Math.max(min, value));
        if (clamped != value) {
            Debug.print("config value %s.%s was clamped.\n", section, key);
        }
        return clamped;
    }

    static String normalizePathValue(String value) {
        return PlatformPaths.resolvePath(PlatformPaths.windowsPathToNative(value));
    }

    static void readInto(String section, String key, StringField field) {
        String[] v = new String[1];
        if (Config.configGetString(GameConfig.gameConfig, section, key, v)) {
            field.set(v[0]);
        }
    }

    static void readInto(String section, String key, IntField field) {
        int[] v = new int[1];
        if (Config.configGetInt(GameConfig.gameConfig, section, key, v)) {
            field.set(v[0]);
        }
    }

    static void readInto(String section, String key, BoolField field) {
        boolean[] v = new boolean[1];
        if (Config.configGetBool(GameConfig.gameConfig, section, key, v)) {
            field.set(v[0]);
        }
    }

    static void readInto(String section, String key, DoubleField field) {
        double[] v = new double[1];
        if (Config.configGetDouble(GameConfig.gameConfig, section, key, v)) {
            field.set(v[0]);
        }
    }

    static boolean keyExists(String section, String key) {
        return Config.configGetString(GameConfig.gameConfig, section, key, new String[1]);
    }

    // --- registration (one line per setting, like the SETTING macros) ---

    static void regBool(String section, String key, BoolField field) {
        regBool(section, key, field, null);
    }

    interface BoolPost {
        boolean apply(boolean value, String section, String key);
    }

    static void regBool(String section, String key, BoolField field, BoolPost post) {
        RegistryEntry entry = baseEntry(section, key, ValueType.Boolean,
                new BoolVal(field.get()));
        entry.read = () -> {
            readInto(section, key, field);
            if (post != null && post.apply(field.get(), section, key) != field.get()) {
                field.set(post.apply(field.get(), section, key));
            }
        };
        entry.write = onlyAdd -> {
            if (onlyAdd && keyExists(section, key)) {
                return;
            }
            Config.configSetBool(GameConfig.gameConfig, section, key, field.get());
        };
        entry.get = () -> new BoolVal(field.get());
        entry.set = value -> field.set(((BoolVal) value).value());
        addEntry(entry);
    }

    static void regInt(String section, String key, IntField field, int[] clamp) {
        RegistryEntry entry = baseEntry(section, key, ValueType.Integer,
                new IntVal(field.get()));
        entry.read = () -> {
            readInto(section, key, field);
            if (clamp != null) {
                field.set(clampInt(field.get(), clamp[0], clamp[1], section, key));
            }
        };
        entry.write = onlyAdd -> {
            if (onlyAdd && keyExists(section, key)) {
                return;
            }
            Config.configSetInt(GameConfig.gameConfig, section, key, field.get());
        };
        entry.get = () -> new IntVal(field.get());
        entry.set = value -> {
            int v = ((IntVal) value).value();
            field.set(clamp != null ? clampInt(v, clamp[0], clamp[1], section, key) : v);
        };
        if (clamp != null) {
            entry.validate = value -> {
                int v = ((IntVal) value).value();
                return Math.min(clamp[1], Math.max(clamp[0], v)) == v;
            };
        }
        addEntry(entry);
    }

    static void regDouble(String section, String key, DoubleField field, double[] clamp) {
        RegistryEntry entry = baseEntry(section, key, ValueType.Real,
                new RealVal(field.get()));
        entry.read = () -> {
            readInto(section, key, field);
            if (clamp != null) {
                field.set(clampDouble(field.get(), clamp[0], clamp[1], section, key));
            }
        };
        entry.write = onlyAdd -> {
            if (onlyAdd && keyExists(section, key)) {
                return;
            }
            Config.configSetDouble(GameConfig.gameConfig, section, key, field.get());
        };
        entry.get = () -> new RealVal(field.get());
        entry.set = value -> {
            double v = ((RealVal) value).value();
            field.set(clamp != null ? clampDouble(v, clamp[0], clamp[1], section, key) : v);
        };
        if (clamp != null) {
            entry.validate = value -> {
                double v = ((RealVal) value).value();
                return Math.min(clamp[1], Math.max(clamp[0], v)) == v;
            };
        }
        addEntry(entry);
    }

    static void regString(String section, String key, StringField field, boolean normalize) {
        RegistryEntry entry = baseEntry(section, key, ValueType.Text,
                new TextVal(field.get()));
        entry.read = () -> {
            readInto(section, key, field);
            if (normalize) {
                field.set(normalizePathValue(field.get()));
            }
        };
        entry.write = onlyAdd -> {
            if (onlyAdd && keyExists(section, key)) {
                return;
            }
            Config.configSetString(GameConfig.gameConfig, section, key, field.get());
        };
        entry.get = () -> new TextVal(field.get());
        entry.set = value -> {
            String v = ((TextVal) value).value();
            field.set(normalize ? normalizePathValue(v) : v);
        };
        addEntry(entry);
    }

    static RegistryEntry baseEntry(String section, String key, ValueType type, SettingValue def) {
        RegistryEntry entry = new RegistryEntry();
        entry.descriptor.id = section + "." + key;
        entry.descriptor.source = "fallout2.cfg";
        entry.descriptor.section = section;
        entry.descriptor.key = key;
        entry.descriptor.valueType = type;
        entry.descriptor.defaultValue = def;
        entry.descriptor.categoryOrder = descriptors.size();
        return entry;
    }

    static void addEntry(RegistryEntry entry) {
        registry.add(entry);
        descriptors.add(entry.descriptor);
    }

    static int[] clamp(int min, int max) {
        return new int[] { min, max };
    }

    static double[] clamp(double min, double max) {
        return new double[] { min, max };
    }

    static void initRegistry(boolean isMapper) {
        if (!registry.isEmpty()) {
            return;
        }

        // system
        regString("system", "executable", f(() -> settings.system.executable, v -> settings.system.executable = v),
                false);
        regPath("system", "master_dat", () -> settings.system.master_dat_path,
                v -> settings.system.master_dat_path = v);
        regPath("system", "master_patches", () -> settings.system.master_patches_path,
                v -> settings.system.master_patches_path = v);
        regPath("system", "f2_res_dat", () -> settings.system.f2_res_dat_path,
                v -> settings.system.f2_res_dat_path = v);
        regPath("system", "critter_dat", () -> settings.system.critter_dat_path,
                v -> settings.system.critter_dat_path = v);
        regPath("system", "critter_patches", () -> settings.system.critter_patches_path,
                v -> settings.system.critter_patches_path = v);
        regString("system", "language", f(() -> settings.system.language, v -> settings.system.language = v),
                false);
        regInt("system", "scroll_lock", i(() -> settings.system.scroll_lock, v -> settings.system.scroll_lock = v),
                null);
        regBool("system", "interrupt_walk", b(() -> settings.system.interrupt_walk, v -> settings.system.interrupt_walk = v));
        regInt("system", "art_cache_size", i(() -> settings.system.art_cache_size, v -> settings.system.art_cache_size = v),
                clamp(32, 512));
        regBool("system", "color_cycling", b(() -> settings.system.color_cycling, v -> settings.system.color_cycling = v));
        regInt("system", "cycle_speed_factor", i(() -> settings.system.cycle_speed_factor, v -> settings.system.cycle_speed_factor = v), null);
        regBool("system", "hashing", b(() -> settings.system.hashing, v -> settings.system.hashing = v));
        regInt("system", "splash", i(() -> settings.system.splash, v -> settings.system.splash = v), null);
        regInt("system", "free_space", i(() -> settings.system.free_space, v -> settings.system.free_space = v),
                null);
        regString("system", "screenshots_format", f(() -> settings.system.screenshots_format, v -> settings.system.screenshots_format = v), false);

        // screen
        regInt("screen", "resolution_x", i(() -> settings.screen.resolution_x, v -> settings.screen.resolution_x = v),
                clamp(640, 7680));
        regInt("screen", "resolution_y", i(() -> settings.screen.resolution_y, v -> settings.screen.resolution_y = v),
                clamp(480, 4320));
        regInt("screen", "windowed", i(() -> settings.screen.windowed, v -> settings.screen.windowed = v),
                clamp(0, 2));
        regBool("screen", "mouse_lock", b(() -> settings.screen.mouse_lock, v -> settings.screen.mouse_lock = v));
        regInt("screen", "scale", i(() -> settings.screen.scale, v -> settings.screen.scale = v), clamp(1, 4));

        // ui
        regInt("ui", "main_menu_scale_mode", i(() -> settings.ui.main_menu_scale_mode, v -> settings.ui.main_menu_scale_mode = v), clamp(0, 2));
        regBool("ui", "in_game_menu_help", b(() -> settings.ui.in_game_menu_help, v -> settings.ui.in_game_menu_help = v));
        regBool("ui", "main_menu_overlay_subscreens", b(() -> settings.ui.main_menu_overlay_subscreens, v -> settings.ui.main_menu_overlay_subscreens = v));
        regBool("ui", "iface_bar_mode", b(() -> settings.ui.iface_bar_mode, v -> settings.ui.iface_bar_mode = v));
        regBool("ui", "perks_progress_bar", b(() -> settings.ui.perks_progress_bar, v -> settings.ui.perks_progress_bar = v));
        regInt("ui", "iface_bar_width", i(() -> settings.ui.iface_bar_width, v -> settings.ui.iface_bar_width = v),
                clamp(640, 4320));
        regInt("ui", "iface_bar_side_art", i(() -> settings.ui.iface_bar_side_art, v -> settings.ui.iface_bar_side_art = v), clamp(0, 999));
        regBool("ui", "iface_bar_sides_ori", b(() -> settings.ui.iface_bar_sides_ori, v -> settings.ui.iface_bar_sides_ori = v));
        regInt("ui", "alternate_ammo_meter", i(() -> settings.ui.alternate_ammo_meter, v -> settings.ui.alternate_ammo_meter = v), clamp(0, 2));
        regInt("ui", "splash_screen_size", i(() -> settings.ui.splash_screen_size, v -> settings.ui.splash_screen_size = v), clamp(0, 2));
        regInt("ui", "death_screen_size", i(() -> settings.ui.death_screen_size, v -> settings.ui.death_screen_size = v), clamp(0, 1));
        regInt("ui", "end_slide_size", i(() -> settings.ui.end_slide_size, v -> settings.ui.end_slide_size = v), clamp(0, 1));
        regBool("ui", "movie_aspect_fit", b(() -> settings.ui.movie_aspect_fit, v -> settings.ui.movie_aspect_fit = v));
        regBool("ui", "edg_support", b(() -> settings.ui.edg_support, v -> settings.ui.edg_support = v));
        regBool("ui", "ignore_scroll_limit", b(() -> settings.ui.ignore_scroll_limit, v -> settings.ui.ignore_scroll_limit = v));
        regBool("ui", "ignore_map_edges", b(() -> settings.ui.ignore_map_edges, v -> settings.ui.ignore_map_edges = v));
        regBool("ui", "quick_toolbar_visible", b(() -> settings.ui.quick_toolbar_visible, v -> settings.ui.quick_toolbar_visible = v));
        regDouble("ui", "anim_speed", d(() -> settings.ui.anim_speed, v -> settings.ui.anim_speed = v),
                clamp(0.1, 100.0));
        regInt("ui", "skip_opening_movies", i(() -> settings.ui.skip_opening_movies, v -> settings.ui.skip_opening_movies = v), clamp(0, 2));
        regBool("ui", "display_karma_changes", b(() -> settings.ui.display_karma_changes, v -> settings.ui.display_karma_changes = v));
        regBool("ui", "display_bonus_damage", b(() -> settings.ui.display_bonus_damage, v -> settings.ui.display_bonus_damage = v));
        regBool("ui", "numbers_in_dialogue", b(() -> settings.ui.numbers_in_dialogue, v -> settings.ui.numbers_in_dialogue = v));
        regBool("ui", "party_member_extra_info", b(() -> settings.ui.party_member_extra_info, v -> settings.ui.party_member_extra_info = v));
        regBool("ui", "dialog_border", b(() -> settings.ui.dialog_border, v -> settings.ui.dialog_border = v));
        regInt("ui", "auto_quick_save", i(() -> settings.ui.auto_quick_save, v -> settings.ui.auto_quick_save = v),
                clamp(0, 10));
        regBool("ui", "enable_high_resolution_stencil", b(() -> settings.ui.enable_high_resolution_stencil, v -> settings.ui.enable_high_resolution_stencil = v));
        regBool("ui", "extend_ap_bar", b(() -> settings.ui.extend_ap_bar, v -> settings.ui.extend_ap_bar = v));
        regBool("ui", "expand_barter_window", b(() -> settings.ui.expand_barter_window, v -> settings.ui.expand_barter_window = v));
        regInt("ui", "inventory_columns", i(() -> settings.ui.inventory_columns, v -> settings.ui.inventory_columns = v), clamp(1, 2));
        regInt("ui", "loot_weight_indicator", i(() -> settings.ui.loot_weight_indicator, v -> settings.ui.loot_weight_indicator = v), clamp(0, 3));
        regInt("ui", "loot_container_size_indicator_threshold", i(() -> settings.ui.loot_container_size_indicator_threshold, v -> settings.ui.loot_container_size_indicator_threshold = v),
                clamp(0, 100));

        // gameplay
        regInt("gameplay", "perk_carryover", i(() -> settings.gameplay.perk_carryover, v -> settings.gameplay.perk_carryover = v), clamp(0, 2));

        // preferences
        regInt("preferences", "game_difficulty", i(() -> settings.preferences.game_difficulty, v -> settings.preferences.game_difficulty = v), null);
        regInt("preferences", "combat_difficulty", i(() -> settings.preferences.combat_difficulty, v -> settings.preferences.combat_difficulty = v), null);
        regInt("preferences", "violence_level", i(() -> settings.preferences.violence_level, v -> settings.preferences.violence_level = v), null);
        regInt("preferences", "target_highlight", i(() -> settings.preferences.target_highlight, v -> settings.preferences.target_highlight = v), null);
        regBool("preferences", "item_highlight", b(() -> settings.preferences.item_highlight, v -> settings.preferences.item_highlight = v));
        regBool("preferences", "combat_looks", b(() -> settings.preferences.combat_looks, v -> settings.preferences.combat_looks = v));
        regBool("preferences", "combat_messages", b(() -> settings.preferences.combat_messages, v -> settings.preferences.combat_messages = v));
        regBool("preferences", "combat_taunts", b(() -> settings.preferences.combat_taunts, v -> settings.preferences.combat_taunts = v));
        regBool("preferences", "language_filter", b(() -> settings.preferences.language_filter, v -> settings.preferences.language_filter = v));
        regBool("preferences", "running", b(() -> settings.preferences.running, v -> settings.preferences.running = v));
        regBool("preferences", "subtitles", b(() -> settings.preferences.subtitles, v -> settings.preferences.subtitles = v));
        regInt("preferences", "combat_speed", i(() -> settings.preferences.combat_speed, v -> settings.preferences.combat_speed = v), null);
        regBool("preferences", "player_speedup", b(() -> settings.preferences.player_speedup, v -> settings.preferences.player_speedup = v));
        regDouble("preferences", "text_base_delay", d(() -> settings.preferences.text_base_delay, v -> settings.preferences.text_base_delay = v), null);
        regDouble("preferences", "text_line_delay", d(() -> settings.preferences.text_line_delay, v -> settings.preferences.text_line_delay = v), null);
        regDouble("preferences", "brightness", d(() -> settings.preferences.brightness, v -> settings.preferences.brightness = v), null);
        regDouble("preferences", "mouse_sensitivity", d(() -> settings.preferences.mouse_sensitivity, v -> settings.preferences.mouse_sensitivity = v), null);
        regBool("preferences", "running_burning_guy", b(() -> settings.preferences.running_burning_guy, v -> settings.preferences.running_burning_guy = v));

        // sound
        regBool("sound", "initialize", b(() -> settings.sound.initialize, v -> settings.sound.initialize = v));
        regBool("sound", "debug", b(() -> settings.sound.debug, v -> settings.sound.debug = v));
        regBool("sound", "debug_sfxc", b(() -> settings.sound.debug_sfxc, v -> settings.sound.debug_sfxc = v));
        regBool("sound", "sounds", b(() -> settings.sound.sounds, v -> settings.sound.sounds = v));
        regBool("sound", "music", b(() -> settings.sound.music, v -> settings.sound.music = v));
        regBool("sound", "speech", b(() -> settings.sound.speech, v -> settings.sound.speech = v));
        regBool("sound", "float_speech", b(() -> settings.sound.float_speech, v -> settings.sound.float_speech = v));
        regBool("sound", "pipboy_speech", b(() -> settings.sound.pipboy_speech, v -> settings.sound.pipboy_speech = v));
        regInt("sound", "master_volume", i(() -> settings.sound.master_volume, v -> settings.sound.master_volume = v), null);
        regInt("sound", "music_volume", i(() -> settings.sound.music_volume, v -> settings.sound.music_volume = v), null);
        regInt("sound", "sndfx_volume", i(() -> settings.sound.sndfx_volume, v -> settings.sound.sndfx_volume = v), null);
        regInt("sound", "speech_volume", i(() -> settings.sound.speech_volume, v -> settings.sound.speech_volume = v), null);
        regInt("sound", "cache_size", i(() -> settings.sound.cache_size, v -> settings.sound.cache_size = v), null);
        regPath("sound", "music_path1", () -> settings.sound.music_path1, v -> settings.sound.music_path1 = v);
        regPath("sound", "music_path2", () -> settings.sound.music_path2, v -> settings.sound.music_path2 = v);
        regInt("sound", "gapless_music", i(() -> settings.sound.gapless_music, v -> settings.sound.gapless_music = v), null);
        regInt("sound", "music_channels", i(() -> settings.sound.music_channels, v -> settings.sound.music_channels = v),
                clamp(1, 1));
        regInt("sound", "speech_channels", i(() -> settings.sound.speech_channels, v -> settings.sound.speech_channels = v),
                clamp(1, 1));
        regInt("sound", "sfx_channels", i(() -> settings.sound.sfx_channels, v -> settings.sound.sfx_channels = v),
                clamp(4, 16));
        regInt("sound", "movie_channels", i(() -> settings.sound.movie_channels, v -> settings.sound.movie_channels = v),
                clamp(1, 1));
        regInt("sound", "script_channels", i(() -> settings.sound.script_channels, v -> settings.sound.script_channels = v),
                clamp(1, 16));
        regInt("sound", "float_channels", i(() -> settings.sound.float_channels, v -> settings.sound.float_channels = v),
                clamp(1, 8));
        regInt("sound", "pipboy_channels", i(() -> settings.sound.pipboy_channels, v -> settings.sound.pipboy_channels = v),
                clamp(1, 1));

        // debug
        regString("debug", "mode", f(() -> settings.debug.mode, v -> settings.debug.mode = v), false);
        regBool("debug", "show_fps", b(() -> settings.debug.show_fps, v -> settings.debug.show_fps = v));
        regBool("debug", "show_tile_num", b(() -> settings.debug.show_tile_num, v -> settings.debug.show_tile_num = v));
        regBool("debug", "show_script_messages", b(() -> settings.debug.show_script_messages, v -> settings.debug.show_script_messages = v));
        regBool("debug", "show_load_info", b(() -> settings.debug.show_load_info, v -> settings.debug.show_load_info = v));
        regBool("debug", "output_map_data_info", b(() -> settings.debug.output_map_data_info, v -> settings.debug.output_map_data_info = v));
        regInt("debug", "window_width", i(() -> settings.debug.window_width, v -> settings.debug.window_width = v),
                clamp(200, 1920));
        regInt("debug", "window_height", i(() -> settings.debug.window_height, v -> settings.debug.window_height = v),
                clamp(100, 1080));
        regString("debug", "console_output_path", f(() -> settings.debug.console_output_path, v -> settings.debug.console_output_path = v), false);

        // combat_ai
        regInt("combat_ai", "npcs_try_to_spend_extra_ap", i(() -> settings.combat_ai.npcs_try_to_spend_extra_ap, v -> settings.combat_ai.npcs_try_to_spend_extra_ap = v),
                null);

        // qol
        regInt("qol", "use_walk_distance", i(() -> settings.qol.use_walk_distance, v -> settings.qol.use_walk_distance = v), clamp(0, 100));
        regBool("qol", "walk_when_sneaking", b(() -> settings.qol.walk_when_sneaking, v -> settings.qol.walk_when_sneaking = v));
        regBool("qol", "auto_open_doors", b(() -> settings.qol.auto_open_doors, v -> settings.qol.auto_open_doors = v));
        regBool("qol", "party_trade_from_menu", b(() -> settings.qol.party_trade_from_menu, v -> settings.qol.party_trade_from_menu = v));
        regBool("qol", "party_loot_and_barter", b(() -> settings.qol.party_loot_and_barter, v -> settings.qol.party_loot_and_barter = v));
        regBool("qol", "fast_ammo_load", b(() -> settings.qol.fast_ammo_load, v -> settings.qol.fast_ammo_load = v));

        if (isMapper) {
            regBool("mapper", "override_librarian", b(() -> settings.mapper.override_librarian, v -> settings.mapper.override_librarian = v));
            regBool("mapper", "librarian", b(() -> settings.mapper.librarian, v -> settings.mapper.librarian = v));
            regBool("mapper", "use_art_not_protos", b(() -> settings.mapper.use_art_not_protos, v -> settings.mapper.use_art_not_protos = v));
            regBool("mapper", "rebuild_protos", b(() -> settings.mapper.rebuild_protos, v -> settings.mapper.rebuild_protos = v));
            regBool("mapper", "fix_map_objects", b(() -> settings.mapper.fix_map_objects, v -> settings.mapper.fix_map_objects = v));
            regBool("mapper", "fix_map_inventory", b(() -> settings.mapper.fix_map_inventory, v -> settings.mapper.fix_map_inventory = v));
            regBool("mapper", "ignore_rebuild_errors", b(() -> settings.mapper.ignore_rebuild_errors, v -> settings.mapper.ignore_rebuild_errors = v));
            regBool("mapper", "show_pid_numbers", b(() -> settings.mapper.show_pid_numbers, v -> settings.mapper.show_pid_numbers = v));
            regBool("mapper", "save_text_maps", b(() -> settings.mapper.save_text_maps, v -> settings.mapper.save_text_maps = v));
            regBool("mapper", "run_mapper_as_game", b(() -> settings.mapper.run_mapper_as_game, v -> settings.mapper.run_mapper_as_game = v));
            regBool("mapper", "default_f8_as_game", b(() -> settings.mapper.default_f8_as_game, v -> settings.mapper.default_f8_as_game = v));
            regBool("mapper", "sort_script_list", b(() -> settings.mapper.sort_script_list, v -> settings.mapper.sort_script_list = v));
            regString("mapper", "map", f(() -> settings.mapper.map, v -> settings.mapper.map = v), false);
            regString("mapper", "dev_path", f(() -> settings.mapper.dev_path, v -> settings.mapper.dev_path = v), false);
            regBool("mapper", "use_grid_item_picker", b(() -> settings.mapper.use_grid_item_picker, v -> settings.mapper.use_grid_item_picker = v));
        }
    }

    // Functional adapters (method refs don't convert directly to our fields).
    interface IntGetter {
        int get();
    }

    interface IntSetter {
        void set(int value);
    }

    static IntField i(IntGetter get, IntSetter set) {
        return new IntField() {
            @Override
            public int get() {
                return get.get();
            }

            @Override
            public void set(int value) {
                set.set(value);
            }
        };
    }

    interface BoolGetter {
        boolean get();
    }

    interface BoolSetter {
        void set(boolean value);
    }

    static BoolField b(BoolGetter get, BoolSetter set) {
        return new BoolField() {
            @Override
            public boolean get() {
                return get.get();
            }

            @Override
            public void set(boolean value) {
                set.set(value);
            }
        };
    }

    interface DoubleGetter {
        double get();
    }

    interface DoubleSetter {
        void set(double value);
    }

    static DoubleField d(DoubleGetter get, DoubleSetter set) {
        return new DoubleField() {
            @Override
            public double get() {
                return get.get();
            }

            @Override
            public void set(double value) {
                set.set(value);
            }
        };
    }

    interface StringGetter {
        String get();
    }

    interface StringSetter {
        void set(String value);
    }

    static StringField f(StringGetter get, StringSetter set) {
        return new StringField() {
            @Override
            public String get() {
                return get.get();
            }

            @Override
            public void set(String value) {
                set.set(value);
            }
        };
    }

    static void regPath(String section, String key, StringGetter get, StringSetter set) {
        regString(section, key, f(get, set), true);
    }

    // --- lifecycle ---

    public static boolean settingsInit(boolean isMapper, String[] argv) {
        initRegistry(isMapper);
        for (int i = 0; i < registry.size(); i++) {
            registry.get(i).restartValue = null;
            registry.get(i).descriptor.commandLineOverride = false;
            descriptors.get(i).commandLineOverride = false;
        }
        if (!GameConfig.gameConfigInit(isMapper, argv)) {
            return false;
        }

        for (RegistryEntry entry : registry) {
            entry.read.run();
        }

        return true;
    }

    public static void settingsMarkCommandLineOverride(String section, String key) {
        for (int i = 0; i < registry.size(); i++) {
            Descriptor descriptor = descriptors.get(i);
            if (descriptor.section.equalsIgnoreCase(section) && descriptor.key.equalsIgnoreCase(key)) {
                descriptor.commandLineOverride = true;
                registry.get(i).descriptor.commandLineOverride = true;
                return;
            }
        }
    }

    public static void settingsWriteToConfig(boolean onlyAdd) {
        for (RegistryEntry entry : registry) {
            if (entry.restartValue != null) {
                Descriptor descriptor = entry.descriptor;
                if (onlyAdd && keyExists(descriptor.section, descriptor.key)) {
                    continue;
                }
                SettingValue value = entry.restartValue;
                if (value instanceof BoolVal v) {
                    Config.configSetBool(GameConfig.gameConfig, descriptor.section, descriptor.key, v.value());
                } else if (value instanceof IntVal v) {
                    Config.configSetInt(GameConfig.gameConfig, descriptor.section, descriptor.key, v.value());
                } else if (value instanceof RealVal v) {
                    Config.configSetDouble(GameConfig.gameConfig, descriptor.section, descriptor.key, v.value());
                } else if (value instanceof TextVal v) {
                    Config.configSetString(GameConfig.gameConfig, descriptor.section, descriptor.key, v.value());
                }
            } else {
                entry.write.accept(onlyAdd);
            }
        }
    }

    public static boolean settingsSave() {
        settingsWriteToConfig(false);
        return GameConfig.gameConfigSave();
    }

    public static boolean settingsExit(boolean shouldSave) {
        if (shouldSave) {
            settingsWriteToConfig(false);
        }

        boolean result = GameConfig.gameConfigExit(shouldSave);
        for (RegistryEntry entry : registry) {
            entry.restartValue = null;
        }
        return result;
    }

    public static List<Descriptor> settingsGetDescriptors() {
        return Collections.unmodifiableList(descriptors);
    }

    static RegistryEntry findEntry(Descriptor descriptor) {
        for (RegistryEntry entry : registry) {
            if (entry.descriptor.id.equals(descriptor.id)) {
                return entry;
            }
        }
        return null;
    }

    public static SettingValue settingsGetValue(Descriptor descriptor) {
        RegistryEntry entry = findEntry(descriptor);
        return entry != null ? entry.get.get() : descriptor.defaultValue;
    }

    public static SettingValue settingsGetConfiguredValue(Descriptor descriptor) {
        RegistryEntry entry = findEntry(descriptor);
        return entry != null && entry.restartValue != null ? entry.restartValue : settingsGetValue(descriptor);
    }

    public static boolean settingsValidateValue(Descriptor descriptor, SettingValue value, StringBuilder error) {
        RegistryEntry entry = findEntry(descriptor);
        if (entry == null) {
            setError(error, "Setting is not registered.");
            return false;
        }

        Descriptor registered = entry.descriptor;
        if (descriptor.valueType != registered.valueType) {
            setError(error, "Setting type does not match the registered type.");
            return false;
        }

        boolean typeMatches = (registered.valueType == ValueType.Boolean && value instanceof BoolVal)
                || ((registered.valueType == ValueType.Integer || registered.valueType == ValueType.Choice
                        || registered.valueType == ValueType.KeyBinding) && value instanceof IntVal)
                || (registered.valueType == ValueType.Real && value instanceof RealVal)
                || (registered.valueType == ValueType.Text && value instanceof TextVal);
        if (!typeMatches) {
            setError(error, "Value has the wrong type.");
            return false;
        }

        if (value instanceof TextVal v) {
            String text = v.value();
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '#' || ch == ';' || ch == '\r' || ch == '\n' || ch == 0) {
                    setError(error, "Text cannot contain #, ;, line breaks, or null characters.");
                    return false;
                }
            }
        }

        if (!registered.choices.isEmpty() && value instanceof IntVal v) {
            boolean validChoice = false;
            for (Choice choice : registered.choices) {
                if (choice.value() == v.value()) {
                    validChoice = true;
                    break;
                }
            }
            if (!validChoice) {
                setError(error, "Value is not an available choice.");
                return false;
            }
        }

        if (!entry.validate.apply(value)) {
            setError(error, "Value is outside the allowed range.");
            return false;
        }
        return true;
    }

    public static boolean settingsSetValue(Descriptor descriptor, SettingValue value, StringBuilder error) {
        RegistryEntry entry = findEntry(descriptor);
        if (entry == null) {
            setError(error, "Setting is not registered.");
            return false;
        }
        if (entry.descriptor.readOnly) {
            setError(error, "Setting is read-only.");
            return false;
        }
        if (entry.descriptor.commandLineOverride) {
            setError(error, "Setting is overridden by the command line.");
            return false;
        }
        if (!settingsValidateValue(entry.descriptor, value, error)) {
            return false;
        }

        switch (entry.descriptor.applyPolicy) {
        case OnClose:
            entry.set.accept(value);
            break;
        case Restart:
            if (value.equals(entry.get.get())) {
                entry.restartValue = null;
            } else {
                entry.restartValue = value;
            }
            break;
        case NextGame:
        default:
            setError(error, "Applying settings on the next game is not supported yet.");
            return false;
        }
        return true;
    }

    private static void setError(StringBuilder error, String message) {
        if (error != null) {
            error.setLength(0);
            error.append(message);
        }
    }

    /** Test seam: fresh struct defaults (C never resets; tests opt in). */
    static void resetStateForTests() {
        settings = new State();
        registry.clear();
        descriptors.clear();
        GameConfig.resetForTests();
    }
}
