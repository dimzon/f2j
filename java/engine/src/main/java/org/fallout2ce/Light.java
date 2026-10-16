package org.fallout2ce;

/**
 * Light intensity slice, mirroring {@code src/light.cc} /
 * {@code src/light.h}: ambient intensity plus per-elevation per-tile
 * intensity.
 *
 * <p>Only the harness-vectored surface is ported ({@code [lightreact]} in
 * {@code docker/refharness/golden/v51.txt}, produced by
 * {@code sectionLightReact} in {@code docker/refharness/ref_main.cc}):
 * {@code lightInit} (tile reset, returns 0), {@code lightReset},
 * {@code lightExit}, {@code lightGetAmbientIntensity},
 * {@code lightSetAmbientIntensity}, {@code lightGetTileIntensity}
 * (clamped), {@code lightGetTrueTileIntensity} (raw),
 * {@code lightSetTileIntensity}, {@code lightIncreaseTileIntensity},
 * {@code lightDecreaseTileIntensity}, {@code lightResetTileIntensity},
 * {@code lightDecreaseAmbient}, {@code lightIncreaseAmbient}.
 *
 * <p>Guards reuse {@link MapDefs#elevationIsValid} and
 * {@link MapDefs#hexGridTileIsValid} (v33 {@code MapDefs} already provides
 * them); nothing is duplicated here. No {@code tileInit} is needed: the
 * tile arrays are compile-time sized
 * ({@code [ELEVATION_COUNT][HEX_GRID_SIZE]}), exactly as the harness
 * comment notes.
 *
 * <p>NOTES / seams (all documented at the use site too):
 * <ul>
 * <li>{@code lightSetAmbientIntensity} needs the dude's Night Vision perk
 * rank ({@code perkGetRank(gDude, PERK_NIGHT_VISION)}) and the sfall
 * {@code NightVisionBonus} tweak (default 20). The perk engine
 * ({@code src/perk.cc}) is unported and {@link Perk} only carries
 * property ids, so both arrive via the {@link #dudeNightVisionRank}
 * (default 0 = no dude, headless) and {@link #nightVisionBonus}
 * (default 20) seams. The harness never calls the setter (gDude perks
 * are unavailable headless), so the vectors only observe the
 * fresh-process ambient ({@code LIGHT_INTENSITY_MAX}).</li>
 * <li>Screen refresh ({@code tileWindowRefresh}, only when the ambient
 * value actually changes and {@code shouldUpdateScreen} is true) arrives
 * via the {@link #tileWindowRefresh} seam (default no-op), following the
 * {@code Input.windowRefreshAll} runnable-seam convention. No window
 * system is touched by the tests.</li>
 * <li>Clamp placement: C applies {@code std::clamp(adjusted, MIN, MAX)}
 * to the perk-adjusted ambient inside the setter only;
 * {@code lightGetTileIntensity} clamps with
 * {@code std::min(value, MAX)} (upper side only, no lower clamp) while
 * {@code lightGetTrueTileIntensity} returns the raw stored value.
 * Both are reproduced exactly.</li>
 * <li>{@code lightSetThresholds}-style stderr noise does not exist here;
 * light has no debug prints. (Reaction's {@code debugPrint} is the
 * silent one; see {@link Reaction}.)</li>
 * </ul>
 */
public final class Light {

    private Light() {
    }

    /** Port of {@code LIGHT_INTENSITY_MIN} ({@code 65536 / 4}). */
    public static final int LIGHT_INTENSITY_MIN = 65536 / 4;

    /** Port of {@code LIGHT_INTENSITY_MAX} ({@code 65536}). */
    public static final int LIGHT_INTENSITY_MAX = 65536;

    /** Fresh-process tile intensity written by {@code lightResetTileIntensity}. */
    public static final int TILE_INTENSITY_DEFAULT = 655;

    /** Port of {@code gAmbientIntensity} (init {@code LIGHT_INTENSITY_MAX}). */
    private static int ambientIntensity = LIGHT_INTENSITY_MAX;

    /** Port of {@code gTileIntensity[ELEVATION_COUNT][HEX_GRID_SIZE]}. */
    private static final int[][] tileIntensity =
        new int[MapDefs.ELEVATION_COUNT][MapDefs.HEX_GRID_SIZE];

    static {
        lightResetTileIntensity();
    }

    /**
     * Seam for {@code perkGetRank(gDude, PERK_NIGHT_VISION)}: dude object
     * unported, defaults to 0 (no dude headless). Set by the future
     * perk/critter ports.
     */
    public static int dudeNightVisionRank = 0;

    /**
     * Seam for {@code perkGetNightVisionBonus()} (sfall
     * {@code PerksTweak:NightVisionBonus}, default 20, range 0-100).
     */
    public static int nightVisionBonus = 20;

    /**
     * Seam for {@code tileWindowRefresh()} (tile render pipeline,
     * unported). Follows the {@code Input.windowRefreshAll}
     * runnable-seam convention; default no-op.
     */
    public static Runnable tileWindowRefresh = () -> {
    };

    /** Port of {@code lightInit} (0x47A8F0): resets tiles, returns 0. */
    public static int lightInit() {
        lightResetTileIntensity();
        return 0;
    }

    /** Port of {@code lightReset} (0x47A8F0): resets tiles. */
    public static void lightReset() {
        lightResetTileIntensity();
    }

    /** Port of {@code lightExit} (0x47A8F0): resets tiles. */
    public static void lightExit() {
        lightResetTileIntensity();
    }

    /** Port of {@code lightGetAmbientIntensity} (0x47A908). */
    public static int lightGetAmbientIntensity() {
        return ambientIntensity;
    }

    /**
     * Port of {@code lightSetAmbientIntensity} (0x47A908).
     *
     * <p>Perk adjustment and clamp placement mirror C exactly:
     * {@code adjusted = intensity + rank * ((MAX * bonus) / 100)}, then
     * {@code clamp(adjusted, MIN, MAX)}. Integer division truncates as in
     * C. Refresh fires only when {@code shouldUpdateScreen} is true and
     * the normalized value differs from the old ambient.
     */
    public static void lightSetAmbientIntensity(int intensity, boolean shouldUpdateScreen) {
        int adjustedIntensity =
            intensity + dudeNightVisionRank * ((LIGHT_INTENSITY_MAX * nightVisionBonus) / 100);
        int normalizedIntensity =
            Math.min(Math.max(adjustedIntensity, LIGHT_INTENSITY_MIN), LIGHT_INTENSITY_MAX);

        int oldAmbientIntensity = ambientIntensity;
        ambientIntensity = normalizedIntensity;

        if (shouldUpdateScreen) {
            if (oldAmbientIntensity != normalizedIntensity) {
                tileWindowRefresh.run();
            }
        }
    }

    /**
     * Port of {@code lightGetTileIntensity} (0x47A980): invalid
     * elevation/tile yields 0, otherwise
     * {@code min(stored, LIGHT_INTENSITY_MAX)} (upper clamp only).
     */
    public static int lightGetTileIntensity(int elevation, int tile) {
        if (!MapDefs.elevationIsValid(elevation)) {
            return 0;
        }

        if (!MapDefs.hexGridTileIsValid(tile)) {
            return 0;
        }

        return Math.min(tileIntensity[elevation][tile], LIGHT_INTENSITY_MAX);
    }

    /**
     * Port of {@code lightGetTrueTileIntensity} (0x47A9C4): invalid
     * elevation/tile yields 0, otherwise the raw stored value (no clamp).
     */
    public static int lightGetTrueTileIntensity(int elevation, int tile) {
        if (!MapDefs.elevationIsValid(elevation)) {
            return 0;
        }

        if (!MapDefs.hexGridTileIsValid(tile)) {
            return 0;
        }

        return tileIntensity[elevation][tile];
    }

    /** Port of {@code lightSetTileIntensity} (0x47A9EC) with validity guards. */
    public static void lightSetTileIntensity(int elevation, int tile, int intensity) {
        if (!MapDefs.elevationIsValid(elevation)) {
            return;
        }

        if (!MapDefs.hexGridTileIsValid(tile)) {
            return;
        }

        tileIntensity[elevation][tile] = intensity;
    }

    /** Port of {@code lightIncreaseTileIntensity} (0x47AA10) with validity guards. */
    public static void lightIncreaseTileIntensity(int elevation, int tile, int intensity) {
        if (!MapDefs.elevationIsValid(elevation)) {
            return;
        }

        if (!MapDefs.hexGridTileIsValid(tile)) {
            return;
        }

        tileIntensity[elevation][tile] += intensity;
    }

    /** Port of {@code lightDecreaseTileIntensity} (0x47AA48) with validity guards. */
    public static void lightDecreaseTileIntensity(int elevation, int tile, int intensity) {
        if (!MapDefs.elevationIsValid(elevation)) {
            return;
        }

        if (!MapDefs.hexGridTileIsValid(tile)) {
            return;
        }

        tileIntensity[elevation][tile] -= intensity;
    }

    /** Port of {@code lightResetTileIntensity} (0x47AA84): all tiles to 655. */
    public static void lightResetTileIntensity() {
        for (int elevation = 0; elevation < MapDefs.ELEVATION_COUNT; elevation++) {
            for (int tile = 0; tile < MapDefs.HEX_GRID_SIZE; tile++) {
                tileIntensity[elevation][tile] = TILE_INTENSITY_DEFAULT;
            }
        }
    }

    /** Port of {@code lightDecreaseAmbient}: setter with screen update. */
    public static void lightDecreaseAmbient(int val) {
        lightSetAmbientIntensity(ambientIntensity - val, true);
    }

    /** Port of {@code lightIncreaseAmbient}: setter with screen update. */
    public static void lightIncreaseAmbient(int val) {
        lightSetAmbientIntensity(ambientIntensity + val, true);
    }

    static void resetForTests() {
        ambientIntensity = LIGHT_INTENSITY_MAX;
        dudeNightVisionRank = 0;
        nightVisionBonus = 20;
        tileWindowRefresh = () -> {
        };
        lightResetTileIntensity();
    }
}
