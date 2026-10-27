package org.fallout2ce;

import org.fallout2ce.EventQueue.EventType;
import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.Proto;
import org.fallout2ce.ProtoTypes.ProtoId;
import org.fallout2ce.ProtoTypes.ProtoItemDrugData;

/**
 * Port of the drug/withdrawal subsystem ({@code src/item.cc}: the drug
 * descriptions, the effect events ({@code DrugEffectEvent}) with their
 * queue registration, the withdrawal events ({@code WithdrawalEvent}) and
 * {@code drugItemTakeDrug} itself, plus the game-global addiction flags for
 * the dude. The messages and the screen hooks are documented seams: the
 * headless harness never renders them, and the events themselves stay in
 * the event queue (the tick pump is game phase).
 *
 * <p>Vectors: {@code [aimdrug]} in {@code docker/refharness/golden/v171.txt}
 * (produced by {@code sectionAiDrug} in
 * {@code docker/refharness/ref_main.cc}).
 */
public final class Drugs {

    private Drugs() {
    }

    /** Port of {@code ADDICTION_COUNT} (src/item.cc:151). */
    private static final int ADDICTION_COUNT = 9;

    /** Port of {@code DrugDescription} (src/item.cc). */
    private static final class DrugDescription {
        final int drugPid;
        final int gvar;
        final int maxActiveEffects;

        DrugDescription(int drugPid, int gvar, int maxActiveEffects) {
            this.drugPid = drugPid;
            this.gvar = gvar;
            this.maxActiveEffects = maxActiveEffects;
        }
    }

    // The gvar indices mirror the Java {@code GameGlobalVars.NAMES} order.
    private static final DrugDescription[] DRUG_DESCRIPTIONS = {
        new DrugDescription(106, 21, 0),  // NukaCola
        new DrugDescription(87, 22, 4),   // Buffout
        new DrugDescription(53, 23, 4),   // Mentats
        new DrugDescription(110, 24, 4),  // Psycho
        new DrugDescription(48, 25, 0),   // Radaway
        new DrugDescription(124, 26, 0),  // Beer
        new DrugDescription(125, 26, 0),  // Booze
        new DrugDescription(259, 296, 4), // Jet
        new DrugDescription(304, 295, 0), // Deck of Tragic Cards
    };

    /** Port of {@code DrugEffectEvent} (src/queue.h:38). */
    public static final class DrugEffectEvent {
        public int drugPid;
        public final int[] stats = new int[3];
        public final int[] modifiers = new int[3];
    }

    /** Port of {@code WithdrawalEvent} (src/queue.h:44). */
    public static final class WithdrawalEvent {
        public int active;
        public int pid;
        public int perk;
    }

    // Port of {@code _wd_obj} / {@code _wd_gvar} / {@code _wd_onset}
    // (src/item.cc:171-173).
    private static GameObject wdObj;
    private static int wdGvar;
    private static int wdOnset;

    // Message/UI seams: displayMonitorAddMessage, getmsg, interfaceRenderHitPoints
    // are game phase; the vectors never render them.
    private static void displayMonitorAddMessageSeam(String text) {
    }

    private static String getMessageSeam(int num) {
        return null;
    }

    // perkAddEffect/perkRemoveEffect: perk effects are game phase (their
    // table is unported); the withdrawal bookkeeping still runs.
    private static void perkAddEffectSeam(GameObject obj, int perk) {
    }

    private static void perkRemoveEffectSeam(GameObject obj, int perk) {
    }

    private static int drugGetAddictionGvarByProtoId(ProtoId drugProtoId) {
        for (int index = 0; index < ADDICTION_COUNT; index++) {
            DrugDescription description = DRUG_DESCRIPTIONS[index];
            if (description.drugPid == drugProtoId.pid()) {
                return description.gvar;
            }
        }
        return -1;
    }

    private static boolean dudeIsAddicted(ProtoId drugProtoId) {
        for (int index = 0; index < ADDICTION_COUNT; index++) {
            DrugDescription description = DRUG_DESCRIPTIONS[index];
            if (!drugProtoId.valid() || description.drugPid == drugProtoId.pid()) {
                if (GameGlobalVars.get(description.gvar) != 0) {
                    return true;
                } else if (drugProtoId.valid()) {
                    return false;
                }
            }
        }
        return false;
    }

    private static void dudeSetAddiction(ProtoId drugProtoId) {
        int gvar = drugGetAddictionGvarByProtoId(drugProtoId);
        if (gvar != -1) {
            GameGlobalVars.set(gvar, 1);
        }
        setDudeAddictedFlag(true);
    }

    private static void dudeClearAddiction(ProtoId drugProtoId) {
        int gvar = drugGetAddictionGvarByProtoId(drugProtoId);
        if (gvar != -1) {
            GameGlobalVars.set(gvar, 0);
        }
        if (!dudeIsAddicted(ProtoId.empty())) {
            setDudeAddictedFlag(false);
        }
    }

    private static void setDudeAddictedFlag(boolean addicted) {
        GameObject dude = ObjectSystem.getDude();
        if (dude == null) {
            return;
        }
        Proto[] slot = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(dude), slot);
        if (addicted) {
            slot[0].critter.data.flags = slot[0].critter.data.flags
                    .or(ObjTypes.CritterFlags.DUDE_ADDICTED);
        } else {
            slot[0].critter.data.flags = slot[0].critter.data.flags
                    .and(ObjTypes.CritterFlags.DUDE_ADDICTED.not());
        }
    }

    // 0x4796E4 _insert_drug_effect: allocate and schedule the effect event.
    private static int insertDrugEffect(GameObject critter, GameObject item, int duration,
            int[] stats, int[] mods) {
        int index = 0;
        for (; index < 3; index++) {
            if (mods[index] != 0) {
                break;
            }
        }

        if (index == 3) {
            return -1;
        }

        DrugEffectEvent drugEffectEvent = new DrugEffectEvent();
        drugEffectEvent.drugPid = item.pid;
        for (index = 0; index < 3; index++) {
            drugEffectEvent.stats[index] = stats[index];
            drugEffectEvent.modifiers[index] = mods[index];
        }

        int delay = 600 * duration;
        if (critter == ObjectSystem.getDude()) {
            if (TraitDefs.traitIsSelectedAndActive(
                    TraitDefs.Trait.TRAIT_CHEM_RESISTANT.id())) {
                delay /= 2;
            }
        }

        if (EventQueue.queueAddEvent(delay, critter, drugEffectEvent, EventType.DRUG) == -1) {
            return -1;
        }

        return 0;
    }

    // 0x479C20 _perform_drug_effect.
    private static void performDrugEffect(GameObject critter, int[] stats, int[] mods,
            boolean isImmediate) {
        boolean statsChanged = false;

        int startIndex = 0;
        boolean firstStatIsMinimum = false;
        if (stats[0] == -2) {
            startIndex = 1;
            firstStatIsMinimum = true;
        }

        for (int index = startIndex; index < 3; index++) {
            int stat = stats[index];
            if (stat == StatDefs.Stat.STAT_INVALID.id()) {
                continue;
            }

            if (stat == StatDefs.Stat.STAT_CURRENT_HIT_POINTS.id()) {
                critter.data.critter.combat.maneuver = critter.data.critter.combat.maneuver
                        .and(ObjTypes.CritterManeuver.FLEEING.not());
            }

            int oldStatBonus = Stat.critterGetBonusStat(critter, stat);

            int before = (critter == ObjectSystem.getDude())
                    ? Stat.critterGetStat(ObjectSystem.getDude(), stat)
                    : 0;

            int statBonus;
            if (firstStatIsMinimum) {
                statBonus = GameRandom.randomBetween(mods[index - 1], mods[index]) + oldStatBonus;
                firstStatIsMinimum = false;
            } else {
                statBonus = mods[index] + oldStatBonus;
            }

            if (stat == StatDefs.Stat.STAT_CURRENT_HIT_POINTS.id()) {
                int currentHp = Stat.critterGetBaseStatWithTraitModifier(critter,
                        StatDefs.Stat.STAT_CURRENT_HIT_POINTS.id());
                if (statBonus + currentHp <= 0 && critter != ObjectSystem.getDude()) {
                    // %s succumbs to the adverse effects of chems: the
                    // out-of-combat kill is a game-phase seam.
                }
            }

            Stat.critterSetBonusStat(critter, stat, statBonus);

            if (critter == ObjectSystem.getDude()) {
                if (stat == StatDefs.Stat.STAT_CURRENT_HIT_POINTS.id()) {
                    // interfaceRenderHitPoints: UI seam.
                }

                int after = Stat.critterGetStat(critter, stat);
                if (after != before) {
                    // 1 - You gained %d %s. / 2 - You lost %d %s.
                    // messageListGetItem + displayMonitorAddMessage: UI seam.
                    statsChanged = true;
                }
            }
        }

        if (Stat.critterGetStat(critter, StatDefs.Stat.STAT_CURRENT_HIT_POINTS.id()) > 0) {
            if (critter == ObjectSystem.getDude() && !statsChanged && isImmediate) {
                // "Nothing happens." - UI seam.
            }
        } else {
            if (critter == ObjectSystem.getDude()) {
                // "You suffer a fatal heart attack from chem overdose." - UI seam.
            } else {
                // %s succumbs... - UI seam.
            }
        }
    }

    // 0x479EE4 _drug_effect_allowed.
    private static boolean drugEffectAllowed(GameObject critter, ProtoId protoId) {
        int index = 0;
        DrugDescription drugDescription = null;
        for (; index < ADDICTION_COUNT; index++) {
            drugDescription = DRUG_DESCRIPTIONS[index];
            if (drugDescription.drugPid == protoId.pid()) {
                break;
            }
        }

        if (index == ADDICTION_COUNT) {
            return true;
        }

        if (drugDescription.maxActiveEffects == 0) {
            return true;
        }

        int count = 0;
        Object event = EventQueue.queueFindFirstEvent(critter, EventType.DRUG);
        while (event instanceof DrugEffectEvent) {
            if (((DrugEffectEvent) event).drugPid == protoId.pid()) {
                count++;
                if (count >= drugDescription.maxActiveEffects) {
                    return false;
                }
            }
            event = EventQueue.queueFindNextEvent(critter, EventType.DRUG);
        }

        return true;
    }

    // 0x479A20 _insert_withdrawal.
    private static int insertWithdrawal(GameObject obj, int active, int duration, int perk,
            ProtoId protoId) {
        WithdrawalEvent withdrawalEvent = new WithdrawalEvent();
        withdrawalEvent.active = active;
        withdrawalEvent.pid = protoId.pid();
        withdrawalEvent.perk = perk;

        if (EventQueue.queueAddEvent(600 * duration, obj, withdrawalEvent,
                EventType.WITHDRAWAL) == -1) {
            return -1;
        }

        return 0;
    }

    // withdrawalGetEvent (src/item.cc:3140).
    static WithdrawalEvent withdrawalGetEvent(GameObject obj, ProtoId protoId) {
        Object event = EventQueue.queueFindFirstEvent(obj, EventType.WITHDRAWAL);
        while (event instanceof WithdrawalEvent
                && ((WithdrawalEvent) event).pid != protoId.pid()) {
            event = EventQueue.queueFindNextEvent(obj, EventType.WITHDRAWAL);
        }
        return event instanceof WithdrawalEvent ? (WithdrawalEvent) event : null;
    }

    // performWithdrawalStart (src/item.cc:3269).
    private static void performWithdrawalStart(GameObject obj, int perk, ProtoId protoId) {
        if (ProtoId.ofGameObject(obj).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return;
        }

        perkAddEffectSeam(obj, perk);

        if (obj == ObjectSystem.getDude()) {
            displayMonitorAddMessageSeam(null);
        }

        int duration = 10080;
        if (obj == ObjectSystem.getDude()) {
            if (TraitDefs.traitIsSelectedAndActive(
                    TraitDefs.Trait.TRAIT_CHEM_RELIANT.id())) {
                duration /= 2;
            }

            if (Perk.perkGetRank(obj, PerkDefs.Perk.PERK_FLOWER_CHILD.id()) != 0) {
                duration /= 2;
            }
        }

        insertWithdrawal(obj, 0, duration, perk, protoId);
    }

    // performWithdrawalEnd (src/item.cc:3303).
    private static void performWithdrawalEnd(GameObject obj, int perk) {
        if (ProtoId.ofGameObject(obj).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return;
        }

        perkRemoveEffectSeam(obj, perk);

        if (obj == ObjectSystem.getDude()) {
            // messageListGetItem(3) + displayMonitorAddMessage: UI seam.
        }
    }

    // 0x47A2FC withdrawalClear.
    static int withdrawalClear(GameObject obj, Object data) {
        if (PartyMembers.objectIsPartyMember(obj)) {
            return 0;
        }
        WithdrawalEvent withdrawalEvent = (WithdrawalEvent) data;
        if (withdrawalEvent.active == 0) {
            performWithdrawalEnd(obj, withdrawalEvent.perk);
        }
        return 1;
    }

    // queueEventHandler-compatible wrapper.
    private static int itemWdClearAllCompatible(Object owner, Object data) {
        return itemWdClearAll((GameObject) owner, data);
    }

    // queueEventHandler-compatible wrapper.
    private static int itemClearJetWithdrawalCompatible(Object owner, Object data) {
        return itemClearJetWithdrawal((GameObject) owner, data);
    }

    // 0x47A324 _item_wd_clear_all.
    private static int itemWdClearAll(GameObject obj, Object data) {
        WithdrawalEvent withdrawalEvent = (WithdrawalEvent) data;
        if (obj != wdObj) {
            return 0;
        }
        ProtoId protoId = ProtoId.ofPid(withdrawalEvent.pid);
        if (drugGetAddictionGvarByProtoId(protoId) != wdGvar) {
            return 0;
        }
        if (withdrawalEvent.active == 0) {
            performWithdrawalEnd(wdObj, withdrawalEvent.perk);
        }
        insertWithdrawal(obj, 1, wdOnset, withdrawalEvent.perk, protoId);
        wdObj = null;
        return 1;
    }

    // 0x47A3A0 itemClearJetWithdrawal.
    private static int itemClearJetWithdrawal(GameObject obj, Object data) {
        WithdrawalEvent withdrawalEvent = (WithdrawalEvent) data;
        return obj == wdObj && withdrawalEvent.pid == ProtoId.ofItemType(
                ProtoTypes.ItemProtoTypeId.Jet).pid() ? 1 : 0;
    }

    // 0x47A384 withdrawalEventProcess.
    static int withdrawalEventProcess(GameObject obj, Object data) {
        WithdrawalEvent withdrawalEvent = (WithdrawalEvent) data;
        ProtoId protoId = ProtoId.ofPid(withdrawalEvent.pid);

        if (withdrawalEvent.active != 0) {
            performWithdrawalStart(obj, withdrawalEvent.perk, protoId);
        } else {
            if (withdrawalEvent.perk == PerkDefs.Perk.PERK_JET_ADDICTION.id()) {
                insertWithdrawal(obj, 0, 10080, withdrawalEvent.perk, protoId);
                return 0;
            }

            performWithdrawalEnd(obj, withdrawalEvent.perk);

            if (obj == ObjectSystem.getDude()) {
                dudeClearAddiction(protoId);
            }
        }

        return obj == ObjectSystem.getDude() ? 1 : 0;
    }

    // 0x479F60 drugItemTakeDrug.
    public static int drugItemTakeDrug(GameObject critter, GameObject item) {
        // scriptHooks_UseItemOn: identity seam (no hooks -> -1).
        int hookResult = -1;
        if (hookResult != -1) {
            return hookResult;
        }

        if (Critter.critterIsDead(critter)) {
            return -1; // USE_ITEM_RESULT_ERROR
        }

        if (Critter.critterGetBodyType(critter)
                == ProtoTypes.BodyType.BODY_TYPE_ROBOTIC) {
            return -1;
        }

        Proto[] proto = new Proto[1];
        ProtoDb.protoGetProto(ProtoId.ofGameObject(item), proto);

        ProtoId itemId = ProtoId.ofGameObject(item);
        if (itemId.pid() == ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.JetAntidote).pid()) {
            WithdrawalEvent withdrawalEvent = withdrawalGetEvent(critter,
                    ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.Jet));
            boolean isLegacyDudeAddiction = critter == ObjectSystem.getDude()
                    && withdrawalEvent == null
                    && dudeIsAddicted(ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.Jet));

            if (withdrawalEvent != null || isLegacyDudeAddiction) {
                if (withdrawalEvent == null || withdrawalEvent.active == 0) {
                    performWithdrawalEnd(critter, PerkDefs.Perk.PERK_JET_ADDICTION.id());
                }

                if (withdrawalEvent != null) {
                    wdObj = critter;
                    EventQueue.queueClearByEventType(EventType.WITHDRAWAL,
                            Drugs::itemClearJetWithdrawalCompatible);
                }

                if (critter == ObjectSystem.getDude()) {
                    dudeClearAddiction(ProtoId.ofItemType(ProtoTypes.ItemProtoTypeId.Jet));
                }

                return 1; // USE_ITEM_RESULT_REMOVE
            }
        }

        wdObj = critter;
        wdGvar = drugGetAddictionGvarByProtoId(itemId);
        wdOnset = proto[0].item.data.drug.withdrawalOnset;

        EventQueue.queueClearByEventType(EventType.WITHDRAWAL, Drugs::itemWdClearAllCompatible);

        if (drugEffectAllowed(critter, itemId)) {
            ProtoItemDrugData drug = proto[0].item.data.drug;
            int[] statIds = new int[3];
            int[] amount = new int[3];
            for (int index = 0; index < 3; index++) {
                statIds[index] = drug.stat[index].id();
                amount[index] = drug.amount[index];
            }
            performDrugEffect(critter, statIds, amount, true);
            insertDrugEffect(critter, item, drug.duration1, statIds, drug.amount1);
            insertDrugEffect(critter, item, drug.duration2, statIds, drug.amount2);
        } else {
            if (critter == ObjectSystem.getDude()) {
                // getmsg(50) + displayMonitorAddMessage: UI seam.
            }
        }

        boolean isAddicted = withdrawalGetEvent(critter, itemId) != null;
        if (critter == ObjectSystem.getDude()) {
            isAddicted = isAddicted || dudeIsAddicted(itemId);
        }

        if (!isAddicted) {
            int addictionChance = proto[0].item.data.drug.addictionChance;
            if (critter == ObjectSystem.getDude()) {
                if (TraitDefs.traitIsSelectedAndActive(
                        TraitDefs.Trait.TRAIT_CHEM_RELIANT.id())) {
                    addictionChance *= 2;
                }

                if (TraitDefs.traitIsSelectedAndActive(
                        TraitDefs.Trait.TRAIT_CHEM_RESISTANT.id())) {
                    addictionChance /= 2;
                }

                if (Perk.perkGetRank(critter,
                        PerkDefs.Perk.PERK_FLOWER_CHILD.id()) != 0) {
                    addictionChance /= 2;
                }
            }

            if (GameRandom.randomBetween(1, 100) <= addictionChance) {
                insertWithdrawal(critter, 1,
                        proto[0].item.data.drug.withdrawalOnset,
                        proto[0].item.data.drug.withdrawalEffect.id(), itemId);

                if (critter == ObjectSystem.getDude()) {
                    dudeSetAddiction(itemId);
                }
            }
        }

        return 1; // USE_ITEM_RESULT_REMOVE
    }

    /** Test seam: the wakeup drug/withdrawal event handlers. */
    static int drugEffectEventProcess(GameObject obj, Object data) {
        DrugEffectEvent drugEffectEvent = (DrugEffectEvent) data;

        if (ProtoId.ofGameObject(obj).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }

        performDrugEffect(obj, drugEffectEvent.stats, drugEffectEvent.modifiers, false);

        if ((obj.data.critter.combat.results.bits() & ObjTypes.Dam.DEAD.bits()) == 0) {
            return 0;
        }

        return 1;
    }
}
