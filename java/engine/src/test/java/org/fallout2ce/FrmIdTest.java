package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Fid math vectors from {@code docker/refharness/golden/v11.txt}
 * ({@code [art]}); path vectors need loaded art lists (see ArtTest).
 */
class FrmIdTest {

    @Test
    void miscFid() {
        FrmId misc = FrmId.ofType(ObjectType.MISC, 5);
        assertEquals(83886085, misc.fid()); // 0x05000005
        assertTrue(misc.valid());
        assertEquals(ObjectType.MISC, misc.objectType());
        assertEquals(5, misc.frameId());
        assertEquals(Rotation.NE.value(), misc.rotation());
        assertEquals(AnimationType.STAND.value(), misc.animationType());
        assertEquals(WeaponAnimation.NONE.value(), misc.weaponAnimation());
        assertTrue(misc.hasFid() && misc.hasObjectType() && misc.hasWeaponAnimation());
    }

    @Test
    void empty() {
        FrmId empty = FrmId.emptyId();
        assertEquals(-1, empty.fid());
        assertFalse(empty.valid());
        assertFalse(empty.hasFid());
    }

    @Test
    void critterWalkForcesNe() {
        // Walk (< 20) forces rotation NE even when SE is requested.
        FrmId critter = FrmId.ofCritter(12, AnimationType.WALK.value(),
                WeaponAnimation.KNIFE.value(), Rotation.SE.value());
        assertEquals(16846860, critter.fid()); // 0x0111100C
        assertEquals(Rotation.NE.value(), critter.rotation());
        assertEquals(WeaponAnimation.KNIFE.value(), critter.weaponAnimation());
    }

    @Test
    void fireFid() {
        FrmId fire = FrmId.ofCritter(7, AnimationType.FIRE_SINGLE.value(),
                WeaponAnimation.PISTOL.value(), Rotation.NE.value());
        assertEquals(19746823, fire.fid()); // 0x012D5007
    }

    @Test
    void invalidBuild() {
        assertEquals(-1, FrmId.buildFid(ObjectType.INVALID, 5));
        assertEquals(-1, FrmId.buildFid(ObjectType.MISC, -1));
    }

    @Test
    void equality() {
        assertEquals(FrmId.ofType(ObjectType.MISC, 5), FrmId.ofType(ObjectType.MISC, 5));
        assertFalse(FrmId.ofType(ObjectType.MISC, 5).equals(FrmId.ofType(ObjectType.MISC, 6)));
        assertFalse(FrmId.ofType(ObjectType.MISC, 5).equals(FrmId.ofPath(ObjectType.MISC, "x")));
        assertEquals(FrmId.ofPath(ObjectType.MISC, "x"), FrmId.ofPath(ObjectType.MISC, "x"));
    }

    @Test
    void enums() {
        assertTrue(ObjectType.isValid(5) && !ObjectType.isValid(11));
        assertTrue(Rotation.isValid(5) && !Rotation.isValid(6));
        assertTrue(AnimationType.isValid(64) && !AnimationType.isValid(65));
        assertTrue(WeaponAnimation.isValid(15) && !WeaponAnimation.isValid(16));
        assertEquals(ObjectType.CRITTER, ObjectType.fromInt(1));
        assertEquals(Rotation.SE, Rotation.fromInt(2));
    }
}
