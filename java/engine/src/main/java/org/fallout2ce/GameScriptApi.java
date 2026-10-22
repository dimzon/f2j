package org.fallout2ce;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The script API backed by the game.
 *
 * <p>Only the builtins whose backing the port already provides are
 * implemented; the rest are refused, exactly as {@link UnsupportedScriptApi}
 * refuses them. The builtins that are the script's own state - its locals,
 * its owner and its action - are not here: they belong to the compiled
 * script's base class. A refused builtin stops the script with a fatal error, the
 * way the original stops when it meets an opcode it has no handler for.
 *
 * <p>The builtins that are implemented here are the ones the shipped scripts
 * lean on most, and the ones whose backing is a pure lookup or a no-op when
 * the game has no state to offer - which is what makes them comparable
 * against the reference without a map or a party.
 */
public class GameScriptApi extends UnsupportedScriptApi {

    /**
     * The opcodes this API provides. The slots are {@code opcode & 0x3FF}, so
     * they are derived rather than written out - the two are easy to mix up,
     * and a wrong slot means the builtin is refused while a different one is
     * claimed.
     */
    private static final int[] PROVIDED_OPCODES = {
        0x80EA, // op_game_time
        0x80EC, // op_elevation
        0x80F6, // op_game_time_hour
        0x80B4, // op_random
        0x80C3, // op_get_map_var
        0x80C4, // op_set_map_var
        0x80F2, // op_game_ticks
        0x80BC, // op_self_obj
        0x80C1, // op_get_local_var
        0x80C2, // op_set_local_var
        0x80C7, // op_script_action
        0x80BF, // opGetDude
        0x8101, // op_cur_map_index
        0x8102, // op_critter_add_trait
        0x814B, // op_party_member_obj
        0x80C5, // op_get_global_var
        0x80C6, // op_set_global_var
        0x80BA, // op_obj_is_carrying_obj
        0x810D, // op_obj_carrying_pid_obj
        0x8106, // op_critter_inven_obj
        0x8105, // op_message_str
        0x80B8, // op_display_msg
        0x8154, // op_debug_msg
        0x80C8, // op_obj_type
        0x80D4, // op_tile_num
        0x8100, // op_obj_pid
        0x80D2, // op_tile_distance
        0x80D3, // op_tile_distance_objs
        0x80D5, // op_tile_num_in_direction
        0x80A4, // op_obj_name
        0x80A7, // op_tile_contains_pid_obj
        0x80B7, // op_create_object
        0x80A1, // op_give_exp_points
        0x80AC, // op_roll_vs_skill
        0x80AF, // op_success
        0x80B0, // op_critical
        0x80BD, // op_source_obj
        0x810A, // op_float_msg
        0x80C9, // op_item_subtype
        0x80CA, // op_get_critter_stat
        0x80D8, // op_add_obj_to_inven
        0x80D9, // op_rm_obj_from_inven
        0x80DC, // op_obj_can_see_obj
        0x80E7, // op_anim_busy
        0x80E9, // op_set_light_level
        0x80F0, // op_add_timer_event
        0x80F1, // op_rm_timer_event
        0x80F3, // op_has_trait
        0x80F4, // op_destroy_object
        0x8107, // op_obj_set_light_level
        0x8116, // op_add_mult_objs_to_inven
        0x8117, // op_rm_mult_objs_from_inven
        0x8144, // op_destroy_mult_objs
        0x814C, // op_rotation_to_tile
        0x80CB, // op_set_critter_stat
        0x8109, // op_inven_cmds
    };

    @Override
    public boolean supports(int slot) {
        for (int opcode : PROVIDED_OPCODES) {
            if ((opcode & 0x3FF) == slot) {
                return true;
            }
        }
        return false;
    }




    /**
     * opGetDude (0x80BF): the player character. The object system models the
     * dude as a null holder for now.
     */
    /**
     * op_random (0x80B4): a number between the two.
     *
     * <p>The original takes the bounds the other way round than they are
     * written, and asks the random number generator for a number in the
     * range.
     */
    @Override
    public int random(int a0, int a1) {
        return GameRandom.randomBetween(a0, a1);
    }

    /**
     * op_get_map_var (0x80C3): one of the map's variables.
     *
     * <p>A variable the map does not have answers -1.
     */
    @Override
    public Program.ProgramValue getMapVar(int a0) {
        MapVars.ProgramValue value = new MapVars.ProgramValue();
        if (MapVars.mapGetGlobalVar(a0, value) == -1) {
            value.opcode = MapVars.VALUE_TYPE_INT;
            value.integerValue = -1;
        }
        return value;
    }

    /**
     * op_set_map_var (0x80C4): write one of the map's variables.
     */
    @Override
    public void setMapVar(int a0, Program.ProgramValue a1) {
        MapVars.ProgramValue value = new MapVars.ProgramValue();
        value.opcode = a1.opcode;
        value.integerValue = a1.integerValue;
        value.floatValue = a1.floatValue;
        value.pointerValue = a1.pointerValue;
        MapVars.mapSetGlobalVar(a0, value);
    }

    /**
     * op_game_ticks (0x80F2): the game's clock, in tenths of a second.
     *
     * <p>The original multiplies the ticks by ten, and a negative number
     * of ticks is no ticks at all.
     */
    @Override
    public int gameTicks(int a0) {
        int ticks = a0 < 0 ? 0 : a0;
        return ticks * 10;
    }

    /**
     * op_game_time (0x80EA): the game's clock, in ticks.
     */
    @Override
    public int gameTime() {
        return GameTime.gameTimeGetTime();
    }

    /**
     * op_game_time_hour (0x80F6): the hour of the game's clock.
     */
    @Override
    public int gameTimeHour() {
        return GameTime.gameTimeGetHour();
    }

    /**
     * op_elevation (0x80EC): the elevation of the object's tile.
     *
     * <p>The original reports a script error for an object that is not
     * there, and the elevation of nothing is zero.
     */
    @Override
    public int elevation(Object a0) {
        if (a0 instanceof ObjTypes.GameObject object) {
            return object.elevation;
        }
        return 0;
    }

    @Override
    public Object opGetDude() {
        // The player is the object the game makes when the objects of the
        // game are made, and the port has made it in objectsInit.
        return ObjectSystem.getDude();
    }

    /**
     * op_cur_map_index (0x8101): the map the game is on. The map system is
     * not ported yet, so this is the map header's index of zero.
     */
    @Override
    public int curMapIndex() {
        return 0;
    }


    /**
     * op_critter_add_trait (0x8102): add a perk or trait to a critter.
     *
     * <p>C does nothing when the object is null, and the shipped scripts call
     * it with the result of {@code op_self_obj}, which is null when there is
     * no script. The critter system is not ported yet, so only the null case
     * is implemented. C pushes -1 on every path, and so does this.
     */
    @Override
    public int critterAddTrait(Object object, int kind, int param, int value) {
        if (object == null) {
            return -1;
        }
        throw unsupported("critter_add_trait");
    }

    /**
     * op_party_member_obj (0x814B): the party member with a given proto id.
     */
    @Override
    public Object partyMemberObj(int protoId) {
        return PartyMembers.partyMemberFindByProtoId(ProtoId.ofPid(protoId));
    }

    /**
     * op_get_global_var (0x80C5): read one of the game's global variables.
     *
     * <p>C pushes a pointer when the global holds an object and the value
     * otherwise, and -1 when the game has no global variables at all. The
     * globals are read during the engine's startup, so they are empty until
     * the engine's init is ported.
     */
    @Override
    public Program.ProgramValue getGlobalVar(int variable) {
        if (GameGlobalVars.length == 0) {
            return new Program.ProgramValue(-1);
        }

        Object pointer = GameGlobalVars.getPointer(variable);
        if (pointer != null) {
            Program.ProgramValue value = new Program.ProgramValue();
            value.opcode = Program.VALUE_TYPE_PTR;
            value.pointerValue = pointer;
            return value;
        }

        return new Program.ProgramValue(GameGlobalVars.get(variable));
    }

    /**
     * op_set_global_var (0x80C6): write one of the game's global variables.
     *
     * <p>A pointer value goes into the pointer table and zeroes the value;
     * anything else goes into the value table and clears the pointer.
     */
    @Override
    public void setGlobalVar(int variable, Program.ProgramValue value) {
        if (GameGlobalVars.length == 0) {
            return;
        }

        if (value.opcode == Program.VALUE_TYPE_PTR) {
            GameGlobalVars.setPointer(variable, value.pointerValue);
            GameGlobalVars.set(variable, 0);
        } else {
            GameGlobalVars.setPointer(variable, null);
            GameGlobalVars.set(variable, value.integerValue);
        }
    }

    /**
     * op_obj_is_carrying_obj (0x80BA): how many of a proto the object
     * carries, counting what its items carry in turn.
     *
     * <p>C answers 0 for a null object and prints a script error.
     */
    @Override
    public int objIsCarryingObj(Object object, int protoId) {
        if (object == null) {
            return 0;
        }

        return Inventory.objectGetCarriedQuantityByProtoId(
            (ObjTypes.GameObject) object, ProtoId.ofPid(protoId));
    }

    /**
     * op_obj_carrying_pid_obj (0x810D): the object of a proto the object
     * carries, looking through what its items carry in turn.
     *
     * <p>C answers null for a null object and prints a script error.
     */
    @Override
    public Object objCarryingPidObj(Object object, int protoId) {
        if (object == null) {
            return null;
        }

        return Inventory.objectGetCarriedObjectByProtoId(
            (ObjTypes.GameObject) object, ProtoId.ofPid(protoId));
    }

    /**
     * op_critter_inven_obj (0x8106): one of a critter's equipped items, or
     * the number of items it carries.
     *
     * <p>The slot tags are the original's: 0 is the armour, 1 the right hand,
     * 2 the left hand, and -2 asks for the number of items carried. A
     * critter that is not a critter answers 0.
     */
    @Override
    public Program.ProgramValue critterInvenObj(Object critter, int type) {
        if (critter == null) {
            return new Program.ProgramValue(0);
        }

        ObjTypes.GameObject object = (ObjTypes.GameObject) critter;
        if (ProtoId.ofGameObject(object).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return new Program.ProgramValue(0);
        }

        if (type == -2) {
            // kInvenSlotInvCount.
            return new Program.ProgramValue(object.data.inventory.items.size());
        }

        ObjTypes.GameObject item = null;
        switch (type) {
        case 0:
            item = Inventory.critterGetArmor(object);
            break;
        case 1:
            if (object == ObjectSystem.getDude()) {
                // The player holds the item in the hand the interface has
                // chosen, and the other hand is empty.
                item = Interface.interfaceGetCurrentHand() != Inventory.Hand.HAND_LEFT
                        ? Inventory.critterGetItem2(object)
                        : null;
            } else {
                item = Inventory.critterGetItem2(object);
            }
            break;
        case 2:
            if (object == ObjectSystem.getDude()) {
                item = Interface.interfaceGetCurrentHand() == Inventory.Hand.HAND_LEFT
                        ? Inventory.critterGetItem1(object)
                        : null;
            } else {
                item = Inventory.critterGetItem1(object);
            }
            break;
        default:
            return new Program.ProgramValue(0);
        }

        Program.ProgramValue value = new Program.ProgramValue();
        value.opcode = Program.VALUE_TYPE_PTR;
        value.pointerValue = item;
        return value;
    }

    /**
     * op_message_str (0x8105): the text of one of the game's messages.
     *
     * <p>C answers "Error" when the message is not there and when the index
     * is negative.
     */
    @Override
    public String messageStr(int messageListIndex, int messageIndex) {
        ensureScriptsListLoaded();

        String text = null;
        if (messageIndex >= 0) {
            text = ScriptMessages.messageText(messageListIndex, messageIndex);
        }

        return text != null ? text : "Error";
    }

    /**
     * op_display_msg (0x80B8): put a message on the message monitor.
     */
    @Override
    public void displayMsg(String message) {
        DisplayMonitor.addMessage(message);

        if (Settings.settings.debug.show_script_messages) {
            Debug.print("\n%s", message);
        }
    }

    /**
     * op_debug_msg (0x8154): write a message to the debug log.
     */
    @Override
    public void debugMsg(String message) {
        if (Settings.settings.debug.show_script_messages) {
            Debug.print("\n%s", message);
        }
    }

    /**
     * op_obj_type (0x80C8): the kind of object it is, taken from its frame.
     *
     * <p>A null object is the invalid type.
     */
    @Override
    public int objType(Object object) {
        if (object == null) {
            return ObjectType.INVALID.value();
        }

        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        return FrmId.ofFid(obj.fid).objectType().value();
    }

    /**
     * op_tile_distance (0x80D2): the distance between two tiles.
     *
     * <p>The original measures it on the hex grid, and a tile that is not
     * on the grid has no distance at all.
     */
    @Override
    public int tileDistance(int a0, int a1) {
        return TileMath.tileDistanceBetween(a0, a1);
    }

    /**
     * op_tile_distance_objs (0x80D3): the distance between the tiles two
     * objects stand on.
     *
     * <p>Only objects on the same elevation are close enough to have a
     * distance; the rest are as far apart as the grid allows.
     */
    @Override
    public int tileDistanceObjs(Object a0, Object a1) {
        if (a0 == null || a1 == null) {
            return TileMath.TILE_MAX_DISTANCE;
        }

        ObjTypes.GameObject first = (ObjTypes.GameObject) a0;
        ObjTypes.GameObject second = (ObjTypes.GameObject) a1;
        if (first.elevation != second.elevation) {
            return TileMath.TILE_MAX_DISTANCE;
        }
        return TileMath.tileDistanceBetween(first.tile, second.tile);
    }

    /**
     * op_tile_num_in_direction (0x80D5): the tile that lies so many steps
     * away in a direction.
     *
     * <p>The direction is the number the script names it by, and a tile at
     * the edge of the grid has nowhere to go.
     */
    @Override
    public int tileNumInDirection(int a0, int a1, int a2) {
        if (!Rotation.isValid(a1)) {
            return a0;
        }
        return TileMath.tileGetTileInDirection(a0, Rotation.fromInt(a1), a2);
    }

    /**
     * op_item_subtype (0x80C9): the type of an item.
     *
     * <p>Something that is not an item has no type at all, and the original
     * answers the invalid type for it.
     */
    @Override
    public int itemSubtype(Object a0) {
        if (a0 == null) {
            return ProtoTypes.ItemType.ITEM_TYPE_INVALID.id();
        }
        ObjTypes.GameObject item = (ObjTypes.GameObject) a0;
        ProtoTypes.ProtoId protoId = ProtoTypes.ProtoId.ofGameObject(item);
        if (protoId.objectType() != ObjTypes.ObjectType.OBJ_TYPE_ITEM) {
            return ProtoTypes.ItemType.ITEM_TYPE_INVALID.id();
        }
        ProtoTypes.Proto[] slot = new ProtoTypes.Proto[1];
        if (ProtoDb.protoGetProto(protoId, slot) == -1) {
            return ProtoTypes.ItemType.ITEM_TYPE_INVALID.id();
        }
        return Item.itemGetType(item).id();
    }

    /**
     * op_add_obj_to_inven (0x80D8): put an object into the inventory of
     * another one.
     *
     * <p>Nothing happens when there is no owner or no object at all, and an
     * object that is attached to something else is not taken.
     */
    @Override
    public void addObjToInven(Object a0, Object a1) {
        if (a0 == null || a1 == null) {
            return;
        }
        Item.itemAdd((ObjTypes.GameObject) a0, (ObjTypes.GameObject) a1, 1);
    }

    /**
     * op_rm_obj_from_inven (0x80D9): take an object out of the inventory of
     * another one.
     *
     * <p>Nothing happens when there is no owner or no object at all.
     */
    @Override
    public void rmObjFromInven(Object a0, Object a1) {
        if (a0 == null || a1 == null) {
            return;
        }
        Item.itemRemove((ObjTypes.GameObject) a0, (ObjTypes.GameObject) a1, 1);
    }

    /**
     * op_rotation_to_tile (0x814C): the direction one tile lies in from
     * another one.
     *
     * <p>The direction is the number the game gives it: north-east is zero.
     */
    @Override
    public int rotationToTile(int a0, int a1) {
        return TileMath.tileGetRotationTo(a0, a1).value();
    }

    /**
     * op_create_object (0x80B7): put a new object of a proto on a tile.
     *
     * <p>The proto id is the number the game gives it, the tile is the
     * number of the tile and the elevation is the level. Nothing is created
     * when the proto id is zero or when the game has no such proto, and
     * the object is put on the tile zero when there is no tile.
     */
    @Override
    public Object createObject(int a0, int a1, int a2, int a3) {
        if (a0 == 0) {
            return null;
        }
        ObjTypes.GameObject[] slot = new ObjTypes.GameObject[1];
        if (ObjectSystem.objectCreateWithProtoId(slot, ProtoTypes.ProtoId.ofPid(a0)) == -1) {
            return null;
        }
        ObjTypes.GameObject object = slot[0];
        object.sid = a3;
        ObjectSystem.objectSetLocation(object, a1 == -1 ? 0 : a1, a2);
        return object;
    }

    /**
     * op_destroy_object (0x80F4): take an object out of the game.
     *
     * <p>Nothing happens when there is no object at all.
     */
    @Override
    public void destroyObject(Object a0) {
        if (a0 == null) {
            return;
        }
        ObjectSystem.objectDestroy((ObjTypes.GameObject) a0);
    }

    /**
     * op_obj_name (0x80A4): the name of an object.
     *
     * <p>The name comes with the game's message stack, and the port has no
     * game phase yet, so the name of an object is not known.
     */
    @Override
    public String objName(Object a0) {
        return null;
    }

    /**
     * op_add_mult_objs_to_inven (0x8116): put a number of objects into the
     * inventory of another one.
     *
     * <p>One object is put in when the number is not a number of objects,
     * and nothing is put in when there is no owner or no object at all.
     */
    @Override
    public void addMultObjsToInven(Object a0, Object a1, int a2) {
        if (a0 == null || a1 == null) {
            return;
        }
        Item.itemAdd((ObjTypes.GameObject) a0, (ObjTypes.GameObject) a1, a2 < 0 ? 1 : a2);
    }

    /**
     * op_rm_mult_objs_from_inven (0x8117): take a number of objects out of
     * the inventory of another one.
     *
     * <p>Not more objects are taken than the inventory holds, and nothing
     * is taken when there is no owner or no object at all.
     */
    @Override
    public int rmMultObjsFromInven(Object a0, Object a1, int a2) {
        if (a0 == null || a1 == null) {
            return 0;
        }
        ObjTypes.GameObject owner = (ObjTypes.GameObject) a0;
        ObjTypes.GameObject item = (ObjTypes.GameObject) a1;
        int quantity = Item.itemGetQuantity(owner, item);
        if (quantity > a2) {
            quantity = a2;
        }
        if (quantity != 0) {
            Item.itemRemoveWithReason(owner, item, quantity,
                Item.RemoveInventoryObjectHookReason.ITEM_REMOVED_MULTI, null);
        }
        return quantity;
    }

    /**
     * op_destroy_mult_objs (0x8144): destroy a number of objects.
     *
     * <p>Not more objects are destroyed than there are, and the number of
     * the objects that were destroyed is the answer.
     */
    @Override
    public int destroyMultObjs(Object a0, int a1) {
        if (a0 == null) {
            return 0;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        int destroyed = 0;
        if (object.owner != null) {
            int quantity = Item.itemGetQuantity(object.owner, object);
            if (quantity > a1) {
                quantity = a1;
            }
            if (quantity != 0) {
                Item.itemRemoveWithReason(object.owner, object, quantity,
                    Item.RemoveInventoryObjectHookReason.ITEM_DESTROY_MULTI, null);
                destroyed = quantity;
            }
        }
        ObjectSystem.objectDestroy(object);
        return destroyed;
    }

    /** The kind of a trait the original knows. */
    private static final int CRITTER_TRAIT_PERK = 0;
    private static final int CRITTER_TRAIT_OBJECT = 1;
    private static final int CRITTER_TRAIT_TRAIT = 2;

    /** The thing of an object the original knows. */
    private static final int CRITTER_TRAIT_OBJECT_AI_PACKET = 5;
    private static final int CRITTER_TRAIT_OBJECT_TEAM = 6;
    private static final int CRITTER_TRAIT_OBJECT_ROTATION = 10;
    private static final int CRITTER_TRAIT_OBJECT_IS_INVISIBLE = 666;
    private static final int CRITTER_TRAIT_OBJECT_GET_INVENTORY_WEIGHT = 669;

    /**
     * op_has_trait (0x80F3): a trait of a critter.
     *
     * <p>The kind of the trait is the number the game gives it, and the
     * number of the trait is the number of the perk or of the thing of the
     * object. The perks need the perk engine and the traits the trait
     * engine, and the port has neither of them yet, so only the things of
     * the object are known.
     */
    @Override
    public int hasTrait(int a0, Object a1, int a2) {
        if (a1 == null) {
            return 0;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a1;
        switch (a0) {
        case CRITTER_TRAIT_PERK:
            return 0;
        case CRITTER_TRAIT_OBJECT:
            switch (a2) {
            case CRITTER_TRAIT_OBJECT_ROTATION:
                return object.rotation.id();
            case CRITTER_TRAIT_OBJECT_IS_INVISIBLE:
                return object.flags.and(ObjTypes.ObjectFlags.HIDDEN).bits() == 0 ? 1 : 0;
            case CRITTER_TRAIT_OBJECT_GET_INVENTORY_WEIGHT:
                return Item.objectGetInventoryWeight(object);
            default:
                return 0;
            }
        default:
            return 0;
        }
    }

    /**
     * op_source_obj (0x80BD): the object that started the script.
     *
     * <p>The source is not the object the script runs for, but the one that
     * started it. The scripts of the game are started by the game itself and
     * have no source, so the original answers the same in its harness.
     */
    @Override
    public Object sourceObj() {
        return null;
    }

    /** The rolls the game knows. */
    private static final int ROLL_CRITICAL_FAILURE = 0;
    private static final int ROLL_FAILURE = 1;
    private static final int ROLL_SUCCESS = 2;
    private static final int ROLL_CRITICAL_SUCCESS = 3;

    /**
     * op_success (0x80AF): whether a roll was a success.
     *
     * <p>The roll is the number the game gives it, and a roll that is none
     * of the four the game knows has no answer.
     */
    @Override
    public int success(int a0) {
        switch (a0) {
        case ROLL_CRITICAL_FAILURE:
        case ROLL_FAILURE:
            return 0;
        case ROLL_SUCCESS:
        case ROLL_CRITICAL_SUCCESS:
            return 1;
        default:
            return -1;
        }
    }

    /**
     * op_critical (0x80B0): whether a roll was a critical one.
     *
     * <p>The roll is the number the game gives it, and a roll that is none
     * of the four the game knows has no answer.
     */
    @Override
    public int critical(int a0) {
        switch (a0) {
        case ROLL_CRITICAL_FAILURE:
        case ROLL_CRITICAL_SUCCESS:
            return 1;
        case ROLL_FAILURE:
        case ROLL_SUCCESS:
            return 0;
        default:
            return -1;
        }
    }

    /**
     * op_float_msg (0x810A): show a text over an object.
     *
     * <p>The text is a floating one that the render engine draws over the
     * object, and the port has no render engine yet, so the text is not
     * shown.
     */
    @Override
    public void floatMsg(Program.ProgramValue a0, int a1) {
    }

    /**
     * op_roll_vs_skill (0x80AC): roll a skill of a critter.
     *
     * <p>The roll is the number the game gives it: a critical failure is
     * zero, a failure is one, a success is two and a critical success is
     * three. A critter that is not there has no roll at all, and the
     * original answers a critical failure for it.
     */
    @Override
    public int rollVsSkill(Object a0, int a1, int a2) {
        if (a0 == null) {
            return GameRandom.Roll.CRITICAL_FAILURE.ordinal();
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        if (ProtoTypes.ProtoId.ofGameObject(object).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return GameRandom.Roll.CRITICAL_FAILURE.ordinal();
        }
        return Skill.skillRoll(object, a1, a2, new int[1]).ordinal();
    }

    /**
     * op_give_exp_points (0x80A1): give experience to the player.
     *
     * <p>The experience goes to the player, and the port has no player
     * yet, so the experience goes nowhere.
     */
    @Override
    public void giveExpPoints(int a0) {
    }

    /**
     * op_tile_contains_pid_obj (0x80A7): the object of a proto on a tile.
     *
     * <p>The proto id is the number the game gives it, and there is no
     * object when the tile is the one of "nowhere".
     */
    @Override
    public Object tileContainsPidObj(int a0, int a1, int a2) {
        if (a2 == -1) {
            return null;
        }
        ProtoTypes.ProtoId protoId = ProtoTypes.ProtoId.ofPid(a0);
        ObjTypes.GameObject object = ObjectSystem.objectFindFirstAtLocation(a1, a2);
        while (object != null) {
            if (ProtoTypes.ProtoId.ofGameObject(object).equals(protoId)) {
                return object;
            }
            object = ObjectSystem.objectFindNextAtLocation();
        }
        return null;
    }

    /**
     * op_add_timer_event (0x80F0): set a timer event for the script of an
     * object.
     *
     * <p>The event fires after the delay in game ticks and carries the
     * parameter the script reads back. The original looks the script of the
     * object up and refuses the event when the script is not there; the port
     * has no registry of the scripts yet, so the event is always set.
     */
    @Override
    public void addTimerEvent(Object a0, int a1, int a2) {
        if (a0 == null) {
            return;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        EventQueue.queueAddEvent(a1, object, new EventQueue.ScriptEvent(object.sid, a2),
            EventQueue.EventType.SCRIPT);
    }

    /**
     * op_rm_timer_event (0x80F1): take the timer events of an object away.
     *
     * <p>Nothing happens when there is no object at all.
     */
    @Override
    public void rmTimerEvent(Object a0) {
        if (a0 == null) {
            return;
        }
        EventQueue.queueRemoveEvents((ObjTypes.GameObject) a0);
    }

    /**
     * op_set_light_level (0x80E9): the ambient light of the map.
     *
     * <p>The level is a percentage: 50 is the middle of the range, above it
     * the light goes up to the top of the range and below it down to the
     * bottom, the way the original spreads it over the two halves.
     */
    @Override
    public void setLightLevel(int a0) {
        int min = Light.LIGHT_INTENSITY_MIN;
        int max = Light.LIGHT_INTENSITY_MAX;
        int middle = (min + max) / 2;
        if (a0 == 50) {
            Light.lightSetAmbientIntensity(middle, true);
            return;
        }
        int intensity = a0 > 50
                ? middle + a0 * (max - middle) / 100
                : min + a0 * (middle - min) / 100;
        Light.lightSetAmbientIntensity(intensity, true);
    }

    /**
     * op_obj_set_light_level (0x8107): the light of an object.
     *
     * <p>The intensity is a percentage of the full light, and a light of
     * zero puts the object out.
     */
    @Override
    public void objSetLightLevel(Object a0, int a1, int a2) {
        if (a0 == null) {
            return;
        }
        ObjectSystem.objectSetLight((ObjTypes.GameObject) a0, a1,
            a2 != 0 ? (a2 * 65536) / 100 : 0);
    }

    /**
     * op_anim_busy (0x80E7): whether an object is busy animating.
     *
     * <p>The port keeps no animation queue, so an object is never busy, and
     * the original's error value is returned for no object at all.
     */
    @Override
    public int animBusy(Object a0) {
        if (a0 == null) {
            return 0;
        }
        return 0;
    }

    /**
     * op_obj_can_see_obj (0x80DC): whether one object sees another.
     *
     * <p>The port has no perception or line of sight, so two objects never
     * see each other: the original returns the same false answer in its
     * harness, where there is no game.
     */
    @Override
    public int objCanSeeObj(Object a0, Object a1) {
        return 0;
    }

    /**
     * op_inven_cmds (0x8109): an item of an object by its index.
     *
     * <p>Only the command 13 is known: the item at the given index. A
     * critter that is not a critter or an index that is not an item
     * answers nothing, and the original answers nothing then as well.
     */
    @Override
    public Object invenCmds(Object a0, int a1, int a2) {
        if (a0 == null) {
            return null;
        }
        if (!(a0 instanceof ObjTypes.GameObject)) {
            return null;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        if (ProtoId.ofGameObject(object).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return null;
        }
        if (a1 != 13) {
            return null;
        }
        return Inventory.inventoryItemByIndex(object, a2);
    }

    /**
     * op_get_critter_stat (0x80CA): a stat of a critter.
     *
     * <p>The stat is the number the game gives it, and a critter that is
     * not there has no stat at all.
     */
    @Override
    public int getCritterStat(Object a0, int a1) {
        if (a0 == null) {
            return -1;
        }
        return Stat.critterGetStat((ObjTypes.GameObject) a0, a1);
    }

    /**
     * op_set_critter_stat (0x80CB): change a stat of a critter.
     *
     * <p>The stat is changed by an amount and not set, and only the dude's
     * own stats may be changed: the original refuses anybody else. The
     * object system keeps the dude as a game-phase global that the port
     * does not have, so there is nobody whose stats may be changed.
     */
    @Override
    public int setCritterStat(Object a0, int a1, int a2) {
        return -1;
    }

    /**
     * op_tile_num (0x80D4): the tile the object is on, or -1 when there is
     * no object.
     */
    @Override
    public int tileNum(Object object) {
        if (object == null) {
            return -1;
        }

        return ((ObjTypes.GameObject) object).tile;
    }

    /**
     * op_obj_pid (0x8100): the object's proto id.
     *
     * <p>A null object has the empty pid, which is what the original's
     * ProtoId gives for a null object.
     */
    @Override
    public int objPid(Object object) {
        if (object == null) {
            return ProtoId.EMPTY_PID;
        }

        return ProtoId.ofGameObject((ObjTypes.GameObject) object).pid();
    }

    /**
     * Reads the script list the first time it is needed.
     *
     * <p>C does this during the game's startup; here it happens on demand,
     * because the message files are only reachable through the script API.
     */
    /**
     * op_self_obj (0x80BC): the object the script runs for.
     *
     * <p>C's scriptGetSelf finds the script that owns the program and
     * returns its owner. The script is the game's own, so the game answers
     * this - not the interpreter. With no script behind the API there is no
     * owner, and the answer is null, which is what C's scriptGetSelf
     * returns for a program with no script.
     */
    @Override
    public Object selfObj() {
        return null;
    }

    /**
     * op_get_local_var (0x80C1): one of the script's own local variables.
     *
     * <p>C resolves the script context and reads the variable from the
     * script's storage. Without a script there is no context, and the
     * original answers -1.
     */
    @Override
    public Program.ProgramValue getLocalVar(int index) {
        return new Program.ProgramValue(-1);
    }

    /**
     * op_set_local_var (0x80C2): write one of the script's own local
     * variables. Without a script there is no context and the write is
     * dropped, which is what C's scriptContextSetLocalVar does.
     */
    @Override
    public void setLocalVar(int index, Program.ProgramValue value) {
    }

    /**
     * op_script_action (0x80C7): the script's action.
     *
     * <p>The action belongs to the script, so the game answers it. A
     * program with no script has no action, and the answer is 0.
     */
    @Override
    public int scriptAction() {
        return 0;
    }

    private static void ensureScriptsListLoaded() {
        if (!ScriptMessages.scriptFileName(0).isEmpty()) {
            return;
        }

        int[] size = { -1 };
        if (Db.dbGetFileSize("scripts\\scripts.lst", size) != 0 || size[0] <= 0) {
            return;
        }
        byte[] data = new byte[size[0]];
        if (Db.dbGetFileContents("scripts\\scripts.lst", data, 0) != 0) {
            return;
        }
        ScriptMessages.loadScriptsList(data);
    }
}
