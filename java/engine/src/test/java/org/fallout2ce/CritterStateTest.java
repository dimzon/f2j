package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code [critterstate]} vectors from
 * {@code docker/refharness/golden/v109.txt}, produced by
 * {@code sectionCritterState} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The three critter predicates all gate on {@code OBJ_TYPE_CRITTER} first,
 * so the section runs {@code critterInit} to get a real critter proto. The
 * vectors then drive each predicate's branches directly, because
 * {@code critterAdjustHitPoints} clamps to {@code STAT_MAXIMUM_HIT_POINTS}
 * (zero for a headless dude) and could never walk HP back up.
 *
 * <p>Leaving states that need critter proto flags (sneaking, level-up,
 * addicted) are covered as a documented seam: {@code dudeHasState} reads them
 * from {@code CRITTER_DUDE_*}, which only {@code critterInit} sets.
 */
class CritterStateTest {

    private GameObject dude;

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        java.nio.file.Path p = java.nio.file.Paths.get(root, name);
        if (!java.nio.file.Files.isRegularFile(p)) {
            throw new IllegalStateException("missing " + p);
        }
        return p.toString();
    }

    @BeforeEach
    void setup() {
        Settings.resetStateForTests();
        Config.resetLastSectionKey();
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        PartyMembers.resetForTests();
        TextObjects.resetForTests();
        Art.resetForTests();
        Light.resetForTests();
        TileMath.resetForTests();
        Map.resetForTests();

        Svga.scrSize.left = 0;
        Svga.scrSize.top = 0;
        Svga.scrSize.right = 639;
        Svga.scrSize.bottom = 479;

        Settings.settings.system.language = Message.ENGLISH;
        assertTrue(XFile.xbaseOpen(gameData("master.dat")));
        assertTrue(XFile.xbaseOpen(gameData("patch000.dat")));
        assertTrue(XFile.xbaseOpen(gameData("critter.dat")));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf, 64, 64, 64,
                rect -> {
                }));

        // objectsInit is what gives the dude a critter proto; critterInit
        // itself only loads scrname.msg, and every predicate here gates on
        // OBJ_TYPE_CRITTER first. Party comes up first because objectsInit
        // wires the dude into it.
        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
        dude = ObjectSystem.getDude();
        assertEquals(ObjTypes.ObjectType.OBJ_TYPE_CRITTER,
                ProtoTypes.ProtoId.ofGameObject(dude).objectType());
    }

    @Test
    void deadFollowsHitPointsThenFlag() {
        dude.data.critter.hp = 10;
        assertFalse(Critter.critterIsDead(dude));

        dude.data.critter.hp = 0;
        assertTrue(Critter.critterIsDead(dude));

        // Alive by HP but flagged dead still reads dead.
        dude.data.critter.hp = 10;
        dude.data.critter.combat.results = Dam.DEAD;
        assertTrue(Critter.critterIsDead(dude));
    }

    @Test
    void crippledAndProneReadDamageBits() {
        dude.data.critter.combat.results = Dam.NONE;
        assertFalse(Critter.critterIsCrippled(dude));
        dude.data.critter.combat.results = Dam.CRIP;
        assertTrue(Critter.critterIsCrippled(dude));

        dude.data.critter.combat.results = Dam.NONE;
        assertFalse(Critter.critterIsProne(dude));
        dude.data.critter.combat.results = Dam.KNOCKED_DOWN;
        assertTrue(Critter.critterIsProne(dude));
    }

    @Test
    void dudeStatesFollowValueThresholds() {
        // The valid range is 0..4; 9 is past it.
        for (int state = 0; state <= 3; state++) {
            assertFalse(Critter.dudeHasState(state));
        }
        assertFalse(Critter.dudeHasState(9));

        // POISONED (state 1) is the only one reachable without critterInit's
        // flag setup, and it is a plain value threshold over critter poison.
        dude.data.critter.poison = 1;
        assertTrue(Critter.dudeHasState(Critter.DUDE_STATE_POISONED));
        dude.data.critter.poison = 0;
        assertFalse(Critter.dudeHasState(Critter.DUDE_STATE_POISONED));
    }
}