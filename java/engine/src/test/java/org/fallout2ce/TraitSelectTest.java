package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code [traitselect]} vectors from
 * {@code docker/refharness/golden/v56.txt} (produced by
 * {@code sectionTraitSelect}). Pure: no game data, no window system, no
 * {@code traitsInit} (it segfaults headless, like {@code statsInit});
 * selection slots + hardcoded bonus branches work from zero state.
 *
 * <p>The flow mirrors the harness exactly: reset, get, set(Gifted,
 * FastShot), get, is/is, stat/skill modifiers, save to a file, reset, load,
 * get.
 */
class TraitSelectTest {

    @TempDir
    Path temp;

    @BeforeEach
    void setup() {
        TraitDefs.resetForTests();
    }

    @AfterEach
    void teardown() {
        TraitDefs.resetForTests();
        Db.resetForTests();
    }

    @Test
    void traitSelectVectors() {
        // sel0=-1,-1
        TraitDefs.traitsReset();
        int[] sel0 = new int[2];
        TraitDefs.traitsGetSelected(sel0);
        assertEquals(-1, sel0[0]);
        assertEquals(-1, sel0[1]);

        // sel=15,7
        TraitDefs.traitsSetSelected(
                TraitDefs.Trait.TRAIT_GIFTED.id(), TraitDefs.Trait.TRAIT_FAST_SHOT.id());
        int[] got = new int[2];
        TraitDefs.traitsGetSelected(got);
        assertEquals(15, got[0]);
        assertEquals(7, got[1]);

        // is=1,0
        int isGifted = TraitDefs.traitIsSelected(TraitDefs.Trait.TRAIT_GIFTED.id()) ? 1 : 0;
        int isFastMeta = TraitDefs.traitIsSelected(TraitDefs.Trait.TRAIT_FAST_METABOLISM.id()) ? 1 : 0;
        assertEquals(1, isGifted);
        assertEquals(0, isFastMeta);

        // statmod=1 skillmod=-10
        int statMod = TraitDefs.traitGetStatModifier(StatDefs.Stat.STAT_STRENGTH.id());
        int skillMod = TraitDefs.traitGetSkillModifier(SkillDefs.Skill.SKILL_SNEAK.id());
        assertEquals(1, statMod);
        assertEquals(-10, skillMod);

        // save=0 load=0 back=15,7
        String path = temp.resolve("trt.bin").toString();
        XFile.Handle saveStream = Db.fileOpen(path, "wb");
        assertNotNull(saveStream);
        int saveRc = TraitDefs.traitsSave(saveStream);
        assertEquals(0, Db.fileClose(saveStream));

        TraitDefs.traitsReset();

        XFile.Handle loadStream = Db.fileOpen(path, "rb");
        assertNotNull(loadStream);
        int loadRc = TraitDefs.traitsLoad(loadStream);
        assertEquals(0, Db.fileClose(loadStream));

        int[] back = new int[2];
        TraitDefs.traitsGetSelected(back);
        assertEquals(0, saveRc);
        assertEquals(0, loadRc);
        assertEquals(15, back[0]);
        assertEquals(7, back[1]);
    }
}
