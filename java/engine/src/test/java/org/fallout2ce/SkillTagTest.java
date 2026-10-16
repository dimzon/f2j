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
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [skilltag]} vectors from
 * {@code docker/refharness/golden/v77.txt}, produced by
 * {@code sectionSkillTag} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>Tag slots start BSS-zeroed ({@code {0,0,0,0}}, so
 * {@code SMALL_GUNS} id 0 already reads tagged); the dude-only bonuses
 * stay dormant (no dude in {@link ObjectSystem}). Tagging changes nothing
 * for the non-dude dog, so sneak stays 5.
 */
class SkillTagTest {

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
        Stat.resetForTests();
        Skill.resetForTests();
        TraitDefs.resetForTests();
        EventQueue.resetForTests();
        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        assertTrue(XFile.xbaseOpen(gameEn + "/master.dat"));
    }

    @AfterEach
    void teardown() {
        Db.resetForTests();
        ProtoDb.resetForTests();
        ObjectSystem.resetForTests();
        Stat.resetForTests();
        Skill.resetForTests();
        TraitDefs.resetForTests();
        EventQueue.resetForTests();
    }

    @Test
    void skillTagVectors() {
        int sneak = SkillDefs.Skill.SKILL_SNEAK.id();
        int smallGuns = SkillDefs.Skill.SKILL_SMALL_GUNS.id();
        assertEquals(8, sneak);
        assertEquals(0, smallGuns);

        // tagged0=0,1 (BSS {0,0,0,0}: sneak untagged, small guns tagged).
        int taggedSneak0 = Skill.isTagged(sneak) ? 1 : 0;
        int taggedGuns0 = Skill.isTagged(smallGuns) ? 1 : 0;
        assertEquals(0, taggedSneak0);
        assertEquals(1, taggedGuns0);

        // Set 2 slots, read back 4: back=8,0,0,0.
        Skill.setTagged(new int[] { sneak, smallGuns }, 2);
        int[] back = { -99, -99, -99, -99 };
        Skill.getTagged(back, 4);
        assertEquals(8, back[0]);
        assertEquals(0, back[1]);
        assertEquals(0, back[2]);
        assertEquals(0, back[3]);

        // tagged1=1 (sneak now tagged).
        assertTrue(Skill.isTagged(sneak));
        int taggedSneak1 = Skill.isTagged(sneak) ? 1 : 0;
        assertEquals(1, taggedSneak1);

        // sneak=5 (dude branch gated: tagging changes nothing for the dog).
        GameObject[] dogSlot = new GameObject[1];
        int dogRc = ObjectSystem.objectCreateWithProtoId(dogSlot, ProtoId.ofPid(16777558));
        assertEquals(0, dogRc);
        assertNotNull(dogSlot[0]);
        assertEquals(5, Skill.skillGetValue(dogSlot[0], sneak));
    }
}
