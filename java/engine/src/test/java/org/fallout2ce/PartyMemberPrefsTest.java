package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [partyprefs]} vectors from
 * {@code docker/refharness/golden/v110.txt}, produced by
 * {@code sectionPartyPrefs} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Every predicate here resolves a party member through its proto id into
 * the parsed description table, so the flags are only reachable on a real
 * member: the section spawns one of the first parsed party member protos
 * because the dude himself is not in the table.
 *
 * <p>The invalid-enum lines are the first thing that would catch a port that
 * looked the description up before validating the index, since the flags
 * would answer from a garbage offset instead of rejecting the id.
 */
class PartyMemberPrefsTest {

    private GameObject member;

    static String gameData(String name) {
        String root = System.getenv("F2J_GAMEDATA");
        if (root == null) {
            throw new IllegalStateException("F2J_GAMEDATA not set");
        }
        Path p = Paths.get(root, name);
        if (!Files.isRegularFile(p)) {
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
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        XFile.xbaseOpen(gameEn + "/master.dat");
        XFile.xbaseOpen(gameEn + "/patch000.dat");
        XFile.xbaseOpen(gameEn + "/critter.dat");
        assertTrue(Color.colorPaletteLoad("color.pal"));
        assertEquals(0, Art.artInit());
        assertEquals(0, Mouse.mouseInit());

        byte[] tileBuf = new byte[64 * 64];
        assertEquals(0, TileMath.tileInit(Map.squareGrids(), 100, 100, 200, 200, tileBuf, 64, 64, 64,
                rect -> {
                }));

        assertEquals(0, PartyMembers.partyMembersInit());
        assertEquals(26, PartyMembers.descriptionsLength);
    }

    @Test
    void levelZeroForNonMember() {
        // The dude is not in the party member table, so both the level and
        // every description-backed predicate read zero/false.
        GameObject dude = ObjectSystem.getDude();
        assertEquals(0, PartyMembers.partyMemberGetCurrentLevel(dude));
        assertFalse(PartyMembers.partyMemberSupportsBestWeapon(dude, 0));
        assertFalse(PartyMembers.partyMemberSupportsDistance(dude, 0));
    }

    @Test
    void memberFlagsComeFromParsedDescription() {
        member = spawnFirstPartyMember();
        assertEquals(0, PartyMembers.partyMemberGetCurrentLevel(member));

        assertTrue(PartyMembers.partyMemberSupportsBestWeapon(member, 0));
        assertFalse(PartyMembers.partyMemberSupportsBestWeapon(member, 1));
        assertFalse(PartyMembers.partyMemberSupportsBestWeapon(member, 2));
        assertTrue(PartyMembers.partyMemberSupportsBestWeapon(member, 3));

        assertTrue(PartyMembers.partyMemberSupportsDistance(member, 0));
        assertTrue(PartyMembers.partyMemberSupportsDistance(member, 1));
        assertTrue(PartyMembers.partyMemberSupportsDistance(member, 2));

        assertTrue(PartyMembers.partyMemberSupportsChemUse(member, 0));
        assertTrue(PartyMembers.partyMemberSupportsChemUse(member, 1));
        assertTrue(PartyMembers.partyMemberSupportsChemUse(member, 2));

        assertTrue(PartyMembers.partyMemberSupportsAreaAttackMode(member, 0));
        assertTrue(PartyMembers.partyMemberSupportsAreaAttackMode(member, 1));
        assertFalse(PartyMembers.partyMemberSupportsAreaAttackMode(member, 2));
    }

    @Test
    void invalidEnumRejectedBeforeLookup() {
        member = spawnFirstPartyMember();
        assertFalse(PartyMembers.partyMemberSupportsBestWeapon(member, 99));
        assertFalse(PartyMembers.partyMemberSupportsDistance(member, 99));
    }

    private GameObject spawnFirstPartyMember() {
        assertTrue(PartyMembers.partyMemberProtoIds.size() > 1);
        GameObject[] out = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(out,
                PartyMembers.partyMemberProtoIds.get(1)));
        assertEquals(16777376, out[0].pid);
        return out[0];
    }
}