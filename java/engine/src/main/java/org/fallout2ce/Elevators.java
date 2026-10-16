package org.fallout2ce;

/**
 * Port of the sfall custom-elevator init from {@code src/elevator.cc} /
 * {@code src/elevator.h}: {@code elevatorsInit} (the config read plus its
 * early return).
 *
 * <p>Vectors: {@code [isorest]} in
 * {@code docker/refharness/golden/v97.txt}, produced by
 * {@code sectionIsoRest} in {@code docker/refharness/ref_main.cc}
 * ({@code iface=0 enter=0}). The embedded
 * {@code Elevators init: no ElevatorsFile configured} line is
 * {@code debugPrint} noise and is not asserted; the config-sourced cause is
 * reproduced - with no {@code [Misc]} {@code ElevatorsFile} in the sfall
 * global config the read misses and the function returns before touching
 * any state.
 *
 * <p>NOTES / seams:
 * <ul>
 * <li>C leaves {@code elevatorsFileName} uninitialized when
 * {@code configGetString} misses the key; Java keeps the {@code null}
 * out-param default and treats it like the empty string, so both misses
 * take the documented no-op branch.</li>
 * <li>The custom table build (25 default {@code gElevatorBackgrounds} /
 * {@code gElevatorLevels} / {@code gElevatorLevelLabels} /
 * {@code gElevatorDescriptions} rows plus the {@code ScopedConfig} parse
 * and the sfall {@code Image} remap) is NOT ported: it needs the
 * {@code Map} enum string table ({@code configGetEnum<Map>}) and the
 * elevator window/draw paths, and no vector can reach it (it is
 * unreachable unless {@code ElevatorsFile} names a readable INI). It
 * arrives with the worldmap phase.</li>
 * </ul>
 */
public final class Elevators {

    private Elevators() {
    }

    /** The maximum number of elevator levels (elevator.cc). */
    public static final int ELEVATOR_LEVEL_MAX = 4;

    /** Max number of elevators loadable from elevators.ini (sfall limit). */
    public static final int ELEVATORS_MAX = 50;

    /** Port of {@code elevatorsInit}. */
    public static void elevatorsInit() {
        String[] elevatorsFileName = new String[1];
        Config.configGetString(SfallConfig.sfallConfig, SfallConfig.MISC_KEY,
                SfallConfig.ELEVATORS_FILE_KEY, elevatorsFileName);
        if (elevatorsFileName[0] == null || elevatorsFileName[0].isEmpty()) {
            Debug.print("\nElevators init: no ElevatorsFile configured, custom elevators disabled");
            return;
        }

        // NOTE: Unported seam: ScopedConfig load + the custom elevator table
        // parse (unreachable without a configured elevators.ini; see the
        // class notes).
    }

    /** Test-only reset (the ported slice holds no state). */
    static void resetForTests() {
    }
}