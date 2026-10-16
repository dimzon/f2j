package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ItemProtoTypeId;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [explosives]} vectors from
 * {@code docker/refharness/golden/v65.txt}, produced by
 * {@code sectionExplosives} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>The damage globals start zeroed (explosions init is static/game
 * phase) and the custom registry starts empty; the book registry stays
 * empty (books init never runs headless), so the book lookup misses.
 * Critter body/kill reads need game data for the {@code .pro} loads
 * (setup mirrors {@link ObjectCreateTest}).
 */
class ExplosivesTest {

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
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        Item.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        Item.resetForTests();
    }

    @Test
    void explosivesVectors() {
        // dyn=1:0,0 dynis=1 armedis=1 stimis=0 customis=1 custom=1:5,9
        // activated=1:41 setdyn=1 dyn2=10,20
        ProtoId dynamite = ProtoId.ofItemType(ItemProtoTypeId.Dynamite);
        int[] dynMin = { -999 };
        int[] dynMax = { -999 };
        assertTrue(Item.explosiveGetDamage(dynamite, dynMin, dynMax));
        assertEquals(0, dynMin[0]);
        assertEquals(0, dynMax[0]);

        assertTrue(Item.explosiveIsExplosive(dynamite));
        assertTrue(Item.explosiveIsActiveExplosive(
                ProtoId.ofItemType(ItemProtoTypeId.ArmedDynamite)));
        assertFalse(Item.explosiveIsExplosive(ProtoId.ofPid(40)));

        Item.explosiveAdd(ProtoId.ofPid(40), ProtoId.ofPid(41), 5, 9);
        assertTrue(Item.explosiveIsExplosive(ProtoId.ofPid(40)));

        int[] custMin = { -999 };
        int[] custMax = { -999 };
        assertTrue(Item.explosiveGetDamage(ProtoId.ofPid(40), custMin, custMax));
        assertEquals(5, custMin[0]);
        assertEquals(9, custMax[0]);

        ProtoId[] activated = { ProtoId.ofPid(40) };
        assertTrue(Item.explosiveActivate(activated));
        assertEquals(41, activated[0].pid());

        assertTrue(Item.explosiveSetDamage(dynamite, 10, 20));
        int[] dynMin2 = { -999 };
        int[] dynMax2 = { -999 };
        Item.explosiveGetDamage(dynamite, dynMin2, dynMax2);
        assertEquals(10, dynMin2[0]);
        assertEquals(20, dynMax2[0]);

        // book=0 body=1,0 kill=11,-1
        int[] bookMsg = { -999 };
        SkillDefs.Skill[] bookSkill = { null };
        assertFalse(Item.booksGetInfo(ProtoId.ofPid(40), bookMsg, bookSkill));

        GameObject[] dogSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558)));
        assertNotNull(dogSlot[0]);
        GameObject[] stimSlot = new GameObject[1];
        assertEquals(0, ObjectSystem.objectCreateWithProtoId(stimSlot, ProtoId.ofPid(40)));
        assertNotNull(stimSlot[0]);

        assertEquals(1, Critter.critterGetBodyType(dogSlot[0]).id());
        assertEquals(0, Critter.critterGetBodyType(stimSlot[0]).id());
        assertEquals(11, Critter.critterGetKillType(dogSlot[0]).id());
        assertEquals(-1, Critter.critterGetKillType(stimSlot[0]).id());
    }
}
