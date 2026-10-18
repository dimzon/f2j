package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * {@code [itemsinit]} vectors from
 * {@code docker/refharness/golden/v111.txt}, produced by
 * {@code sectionItemsInit} in {@code docker/refharness/ref_main.cc}.
 *
 * <p>{@code itemsInit} is the shared seed every item-dependent subsystem
 * reads: the healing pid table, the explosive damage globals and the
 * book table. The first two are reachable headless because they come from
 * the content config, and they are what this slice pins.
 *
 * <p>The damage ranges are the reason this vector is worth having on its
 * own: {@code explosiveGetDamage} was already ported, but its four damage
 * globals were never initialised, so every call answered zero. The
 * {@code stimpak -> 0 [0,0]} line is the negative case that a port
 * which guessed the pid tables would get wrong.
 */
class ItemInitTest {

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
        ContentConfig.resetForTests();
        Item.resetForTests();
        Message.resetForTests();

        String gameEn = Path.of(gameData("master.dat")).getParent().toString();
        XFile.xbaseOpen(gameEn + "/master.dat");
        XFile.xbaseOpen(gameEn + "/patch000.dat");
        Settings.settings.system.language = Message.ENGLISH;
        // The explosive and healing tables read the content config, which
        // gameInit loads from the game database.
        GameConfig.gameConfigInit(false, new String[] { "prog" });

        assertEquals(0, Item.itemsInit());
    }

    @Test
    void healingPidsSeededFromContentConfig() {
        assertTrue(Item.itemIsHealing(ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.Stimpak)));
        assertTrue(Item.itemIsHealing(ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.SuperStimpak)));
        assertTrue(Item.itemIsHealing(ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.HealingPowder)));
    }

    @Test
    void explosiveDamageGlobalsAreSeeded() {
        int[] lo = new int[1];
        int[] hi = new int[1];

        assertTrue(Item.explosiveGetDamage(
                ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.Dynamite), lo, hi));
        assertEquals(30, lo[0]);
        assertEquals(50, hi[0]);

        lo[0] = 0;
        hi[0] = 0;
        assertTrue(Item.explosiveGetDamage(
                ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.PlasticExplosives), lo, hi));
        assertEquals(40, lo[0]);
        assertEquals(80, hi[0]);
    }

    @Test
    void nonExplosiveMissesWithoutTouchingOutParams() {
        int[] lo = new int[1];
        int[] hi = new int[1];
        lo[0] = 7;
        hi[0] = 9;
        assertFalse(Item.explosiveGetDamage(
                ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.Stimpak), lo, hi));
        assertEquals(7, lo[0]);
        assertEquals(9, hi[0]);

        assertFalse(Item.explosionEmitsLight());
    }
}