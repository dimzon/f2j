package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.Dam;
import org.fallout2ce.ObjTypes.GameObject;

/**
 * {@code [critterstateop]} vectors from
 * {@code docker/refharness/golden/v148.txt}, produced by
 * {@code sectionCritterStateOp} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The op answers 0 (standing), 1 (dead) or 2 (prone), with the crippled
 * bits carried on top. An active critter stands unless its animation is
 * the falling pair (48-49) - the narrow pair, not the whole death range
 * {@code critterIsProne} reads. An inactive critter that is not dead lies;
 * hit points alone never inactivate (0 HP with clear results still reads
 * standing). Anything that is not a critter reads dead.
 *
 * <p>Setup mirrors {@link CritterStateTest}: the dude comes from
 * {@code objectsInit} with a real critter proto.
 */
class GameCritterStateTest {

    private GameScriptApi api;
    private GameObject dude;

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
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("master.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("patch000.dat")));
        assertTrue(XFile.xbaseOpen(CritterStateTest.gameData("critter.dat")));
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf, 64, 64, 64,
                rect -> {
                }));

        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(0, ObjectSystem.objectsInit(new byte[640 * 480], 640, 480, 640));
        dude = ObjectSystem.getDude();
        api = new GameScriptApi();
    }

    private void setFidAnim(int anim) {
        dude.fid = (dude.fid & ~0x00FF0000) | ((anim << 16) & 0x00FF0000);
    }

    @Test
    void theStandingCrippledAndProneReads() {
        dude.data.critter.hp = 10;
        dude.data.critter.combat.results = Dam.NONE;
        assertEquals(0, api.critterState(dude));

        dude.data.critter.combat.results = Dam.CRIP;
        assertEquals(124, api.critterState(dude));
        dude.data.critter.combat.results = Dam.NONE;

        int[] anims = { 47, 48, 49, 50, 20 };
        int[] states = { 0, 2, 2, 0, 0 };
        for (int i = 0; i < anims.length; i++) {
            setFidAnim(anims[i]);
            assertEquals(states[i], api.critterState(dude));
        }

        setFidAnim(48);
        dude.data.critter.combat.results = Dam.CRIP;
        assertEquals(126, api.critterState(dude));
        dude.data.critter.combat.results = Dam.NONE;
    }

    @Test
    void theInactiveAndDeadReads() {
        dude.data.critter.hp = 10;

        dude.data.critter.combat.results = Dam.KNOCKED_OUT;
        assertEquals(2, api.critterState(dude));

        dude.data.critter.combat.results = Dam.LOSE_TURN;
        assertEquals(2, api.critterState(dude));

        dude.data.critter.hp = 0;
        dude.data.critter.combat.results = Dam.NONE;
        assertEquals(0, api.critterState(dude));

        dude.data.critter.hp = 10;
        dude.data.critter.combat.results = Dam.DEAD;
        assertEquals(1, api.critterState(dude));
        dude.data.critter.combat.results = Dam.NONE;
    }

    @Test
    void theThingsThatAreNotCrittersReadDead() {
        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoTypes.ProtoId.ofPid(40)));
        assertEquals(1, api.critterState(stimSlot[0]));
        assertEquals(1, api.critterState(null));
    }
}
