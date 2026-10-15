package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.fallout2ce.ArtDefs.BackgroundFrameId;
import org.fallout2ce.ArtDefs.HeadFidget;
import org.fallout2ce.ArtDefs.InterfaceFrameId;
import org.fallout2ce.ArtDefs.MiscFrameId;
import org.fallout2ce.ArtDefs.SceneryFrameId;
import org.fallout2ce.ArtDefs.SkillDexFrameId;
import org.fallout2ce.ArtDefs.TileFlags;
import org.fallout2ce.ArtDefs.TileFrameId;
import org.fallout2ce.ArtDefs.WeaponAnimation;

/**
 * {@code [artdefs]} vectors from {@code docker/refharness/golden/v30.txt}.
 * Enumerator counts match {@code src/art_defs.h} (verified mechanically).
 */
class ArtDefsTest {

    @Test
    void weaponAnimation() {
        assertEquals(0, WeaponAnimation.None.id());
        assertEquals(15, WeaponAnimation.SfallT.id());
        assertEquals(16, WeaponAnimation.values().length);
        assertFalse(ArtDefs.weaponAnimationIsValid(-1));
        assertTrue(ArtDefs.weaponAnimationIsValid(0));
        assertTrue(ArtDefs.weaponAnimationIsValid(15));
        assertFalse(ArtDefs.weaponAnimationIsValid(16));
        assertTrue(ArtDefs.weaponAnimationIsValid(WeaponAnimation.Pistol));
        assertEquals(WeaponAnimation.Smg, ArtDefs.weaponAnimationOf(6));
        assertNull(ArtDefs.weaponAnimationOf(16));
    }

    @Test
    void tileFlags() {
        // tile-or=3.
        assertEquals(3, TileFlags.AlwaysHidden.or(TileFlags.TemporarilyHidden).bits());
        // tile-mask=15: the C++ operator| masks with 0xF.
        assertEquals(15, new TileFlags(~0).or(TileFlags.AlwaysHidden).bits());
        // tile-and=2.
        assertEquals(2, new TileFlags(~1).and(TileFlags.AlwaysHidden).bits());
        assertEquals(1, TileFrameId.Grid.id());
        assertEquals(4095, TileFrameId.Last.id());
    }

    @Test
    void sentinels() {
        assertEquals(2, ArtDefs.DUDE_NATIVE_LOOK_COUNT);
        assertEquals(10, BackgroundFrameId.Vault.id());
        assertEquals(7, HeadFidget.Bad.id());
        assertEquals(12, MiscFrameId.ScrollBlocker.id());
        assertEquals(501, SceneryFrameId.ForceField3.id());
    }

    @Test
    void interfaceFrameIds() {
        assertEquals(305, InterfaceFrameId.values().length);
        assertEquals(0, InterfaceFrameId.First.id());
        assertEquals(4095, InterfaceFrameId.Last.id());
        assertEquals(140, InterfaceFrameId.MainMenuBackgroundImage.id());
        assertEquals(309, InterfaceFrameId.DeathScene.id());
        assertEquals(418, InterfaceFrameId.PartyWeaponUp.id());
        assertEquals(130, InterfaceFrameId.PipBoyNoteNumbers.id());
        assertEquals(290, InterfaceFrameId.MovementPointsNumbers.id());
        assertEquals(439, InterfaceFrameId.WorldMapRandomEncounterCursor2Dark.id());
        // First/Blank alias: last declared wins, like the C++ id space.
        assertEquals(InterfaceFrameId.Blank, ArtDefs.interfaceFrameIdOf(0));
        assertEquals(InterfaceFrameId.Last, ArtDefs.interfaceFrameIdOf(4095));
        assertNull(ArtDefs.interfaceFrameIdOf(4));
    }

    @Test
    void skillDexFrameIds() {
        assertEquals(165, SkillDexFrameId.values().length);
        assertEquals(45, SkillDexFrameId.Outdoorsman.id());
        assertEquals(123, SkillDexFrameId.Tag.id());
        assertEquals(64, SkillDexFrameId.Jinxed.id());
        assertEquals(144, SkillDexFrameId.PsychoAddiction.id());
        assertEquals(173, SkillDexFrameId.WeaponHandling.id());
        assertEquals(48, SkillDexFrameId.Reputation.id());
        assertEquals(67, SkillDexFrameId.DrugResistant.id());
        assertEquals(SkillDexFrameId.Mutate, ArtDefs.skillDexFrameIdOf(124));
        // Gaps in the table have no entry.
        assertNull(ArtDefs.skillDexFrameIdOf(49));
        assertNull(ArtDefs.skillDexFrameIdOf(143));
    }

    @Test
    void smallTables() {
        assertEquals(2, ArtDefs.HeadFrameId.values().length);
        assertEquals(12, ArtDefs.HeadAnimation.values().length);
        assertEquals(4, ArtDefs.HeadFidget.values().length);
        assertEquals(4, ArtDefs.HeadFidgetAnimation.values().length);
        assertEquals(22, BackgroundFrameId.values().length);
        assertEquals(2, ArtDefs.DudeNativeLook.values().length);
        assertEquals(3, ArtDefs.CritterFrameId.values().length);
        assertEquals(3, SceneryFrameId.values().length);
        assertEquals(2, ArtDefs.WallFrameId.values().length);
        assertEquals(2, ArtDefs.ItemFrameId.values().length);
        assertEquals(4, TileFrameId.values().length);
        assertEquals(10, MiscFrameId.values().length);
        assertEquals(ArtDefs.HeadFrameId.None, ArtDefs.headFrameIdOf(0));
        assertEquals(ArtDefs.DudeNativeLook.DUDE_NATIVE_LOOK_JUMPSUIT, ArtDefs.dudeNativeLookOf(1));
    }
}
