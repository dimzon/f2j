package org.fallout2ce;

import java.util.HashMap;
import java.util.Map;

/**
 * World map id tables, mirroring the pure parts of {@code src/worldmap.h}.
 *
 * <p>Only {@code Map} is ported here (needed by {@code obj_types.h});
 * the rest of the header (world map state machine) comes with
 * {@code worldmap.cc}. See {@link StatDefs} for the {@code operator++}
 * iteration note.
 */
public final class WorldmapDefs {

    private WorldmapDefs() {
    }

    public static final int MAP_FIRST = 0;

    public enum Map {
        MAP_TRANSITION(-2),
        MAP_INVALID(-1),
        MAP_RND_DESERT_1(0),
        MAP_RND_DESERT_2(1),
        MAP_RND_DESERT_3(2),
        MAP_ARROYO_CAVES(3),
        MAP_ARROYO_VILLAGE(4),
        MAP_ARROYO_BRIDGE(5),
        MAP_DEN_ENTRANCE(6),
        MAP_DEN_BUSINESS(7),
        MAP_DEN_RESIDENTIAL(8),
        MAP_KLAMATH_1(9),
        MAP_KLAMATH_MALL(10),
        MAP_KLAMATH_RATCAVES(11),
        MAP_KLAMATH_TOXICCAVES(12),
        MAP_KLAMATH_TRAPCAVES(13),
        MAP_KLAMATH_GRAZE(14),
        MAP_VAULTCITY_COURTYARD(15),
        MAP_VAULTCITY_DOWNTOWN(16),
        MAP_VAULTCITY_COUNCIL(17),
        MAP_MODOC_MAINSTREET(18),
        MAP_MODOC_BEDNBREAKFAST(19),
        MAP_MODOC_BRAHMINPASTURES(20),
        MAP_MODOC_GARDEN(21),
        MAP_MODOC_DOWNTHESHITTER(22),
        MAP_MODOC_WELL(23),
        MAP_GHOST_FARM(24),
        MAP_GHOST_CAVERN(25),
        MAP_GHOST_LAKE(26),
        MAP_SIERRA_BATTLE(27),
        MAP_SIERRA_123(28),
        MAP_SIERRA_4(29),
        MAP_VAULT_CITY_VAULT(30),
        MAP_GECKO_SETTLEMENT(31),
        MAP_GECKO_POWER_PLANT(32),
        MAP_GECKO_JUNKYARD(33),
        MAP_GECKO_ACCESS_TUNNELS(34),
        MAP_ARROYO_WILDERNESS(35),
        MAP_VAULT_15(36),
        MAP_THE_SQUAT_A(37),
        MAP_THE_SQUAT_B(38),
        MAP_VAULT_15_EAST_ENTRANCE(39),
        MAP_VAULT_13(40),
        MAP_VAULT_13_ENTRANCE(41),
        MAP_NCR_DOWNTOWN(42),
        MAP_NCR_COUNCIL_1(43),
        MAP_NCR_WESTIN_RANCH(44),
        MAP_NCR_GRAZING_LANDS(45),
        MAP_NCR_BAZAAR(46),
        MAP_NCR_COUNCIL_2(47),
        MAP_KLAMATH_CANYON(48),
        MAP_MILITARY_BASE_12(49),
        MAP_MILITARY_BASE_34(50),
        MAP_MILITARY_BASE_ENTRANCE(51),
        MAP_DEN_SLAVE_RUN(52),
        MAP_CAR_DESERT(53),
        MAP_NEW_RENO_1(54),
        MAP_NEW_RENO_2(55),
        MAP_NEW_RENO_3(56),
        MAP_NEW_RENO_4(57),
        MAP_NEW_RENO_CHOP_SHOP(58),
        MAP_NEW_RENO_GOLGATHA(59),
        MAP_NEW_RENO_STABLES(60),
        MAP_NEW_RENO_BOXING(61),
        MAP_REDDING_WANAMINGO_ENT(62),
        MAP_REDDING_WANAMINGO_12(63),
        MAP_REDDING_DOWNTOWN(64),
        MAP_REDDING_MINE_ENT(65),
        MAP_REDDING_DTOWN_TUNNEL(66),
        MAP_REDDING_MINE_TUNNEL(67),
        MAP_RND_CITY1(68),
        MAP_RND_CAVERN0(69),
        MAP_RND_CAVERN1(70),
        MAP_RND_CAVERN2(71),
        MAP_RND_CAVERN3(72),
        MAP_RND_CAVERN4(73),
        MAP_RND_MOUNTAIN1(74),
        MAP_RND_MOUNTAIN2(75),
        MAP_RND_COAST1(76),
        MAP_RND_COAST2(77),
        MAP_BROKEN_HILLS1(78),
        MAP_BROKEN_HILLS2(79),
        MAP_RND_CAVERN5(80),
        MAP_RND_DESERT4(81),
        MAP_RND_DESERT5(82),
        MAP_RND_DESERT6(83),
        MAP_RND_DESERT7(84),
        MAP_RND_COAST3(85),
        MAP_RND_COAST4(86),
        MAP_RND_COAST5(87),
        MAP_RND_COAST6(88),
        MAP_RND_COAST7(89),
        MAP_RND_COAST8(90),
        MAP_RND_COAST9(91),
        MAP_RAIDERS_CAMP1(92),
        MAP_RAIDERS_CAMP2(93),
        MAP_BH_RND_DESERT(94),
        MAP_BH_RND_MOUNTAIN(95),
        MAP_SPECIAL_RND_WHALE(96),
        MAP_SPECIAL_RND_WOODSMAN(97),
        MAP_SPECIAL_RND_HEAD(98),
        MAP_SPECIAL_RND_SHUTTLE(99),
        MAP_SPECIAL_RND_UNWASHED(100),
        MAP_SPECIAL_RND_BRIDGE(101),
        MAP_SPECIAL_RND_CAFE(102),
        MAP_SPECIAL_RND_HOLY1(103),
        MAP_SPECIAL_RND_HOLY2(104),
        MAP_SPECIAL_RND_GUARDIAN(105),
        MAP_SPECIAL_RND_TOXIC(106),
        MAP_SPECIAL_RND_PARIAH(107),
        MAP_SPECIAL_RND_MAD_COW(108),
        MAP_NAVARRO_ENTRANCE(109),
        MAP_RND_COAST_10(110),
        MAP_RND_COAST_11(111),
        MAP_RND_COAST_12(112),
        MAP_RND_DESERT_8(113),
        MAP_RND_DESERT_9(114),
        MAP_RND_DESERT_10(115),
        MAP_RND_DESERT_11(116),
        MAP_RND_DESERT_12(117),
        MAP_RND_CAVERN_5(118),
        MAP_RND_CAVERN_6(119),
        MAP_RND_CAVERN_7(120),
        MAP_RND_MOUNTAIN_3(121),
        MAP_RND_MOUNTAIN_4(122),
        MAP_RND_MOUNTAIN_5(123),
        MAP_RND_MOUNTAIN_6(124),
        MAP_RND_CITY_2(125),
        MAP_ARROYO_TEMPLE(126),
        MAP_DESTROYED_ARROYO_BRIDGE(127),
        MAP_ENCLAVE_DETENTION(128),
        MAP_ENCLAVE_DOCK(129),
        MAP_ENCLAVE_END_FIGHT(130),
        MAP_ENCLAVE_BARRACKS(131),
        MAP_ENCLAVE_PRESIDENT(132),
        MAP_ENCLAVE_REACTOR(133),
        MAP_ENCLAVE_TRAP_ROOM(134),
        MAP_SAN_FRAN_TANKER(135),
        MAP_SAN_FRAN_DOCK(136),
        MAP_SAN_FRAN_CHINATOWN(137),
        MAP_SHUTTLE_EXTERIOR(138),
        MAP_SHUTTLE_INTERIOR(139),
        MAP_ELRONOLOGIST_BASE(140),
        MAP_RND_CITY_3(141),
        MAP_RND_CITY_4(142),
        MAP_RND_CITY_5(143),
        MAP_RND_CITY_6(144),
        MAP_RND_CITY_7(145),
        MAP_RND_CITY_8(146),
        MAP_NEW_RENO_VB(147),
        MAP_SHI_TEMPLE(148),
        MAP_IN_GAME_MOVIE1(149);

        private final int id;

        Map(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        /** Next id (C++ post-{@code ++} advance step), or null if unknown. */
        public Map next() {
            return WorldmapDefs.ofMap(id + 1);
        }
    }

    private static final java.util.Map<Integer, Map> BY_ID = new HashMap<>();

    static {
        for (Map map : Map.values()) {
            BY_ID.put(map.id, map);
        }
    }

    public static Map ofMap(int id) {
        return BY_ID.get(id);
    }
}
