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
        0x80B6, // op_move_to
        0x80BE, // op_target_obj
        0x80AA, // op_has_skill
        0x8128, // op_is_in_combat
        0x812D, // op_obj_is_locked
        0x8151, // op_critter_is_fleeing
        0x80FC, // op_game_time_advance
        0x8147, // op_move_obj_inven_to_obj
        0x80FA, // op_action_being_used
        0x8136, // op_gfade_out
        0x8137, // op_gfade_in
        0x80EF, // op_critter_damage
        0x80AE, // op_do_check
        0x810C, // op_anim
        0x8132, // op_obj_close
        0x80E8, // op_critter_heal
        0x8123, // op_get_poison
        0x8127, // op_critter_injure
        0x8104, // op_proto_data
        0x80AB, // op_using_skill
        0x8125, // op_party_remove
        0x80E6, // op_set_exit_grids
        0x80A3, // op_play_sfx
        0x810F, // op_reg_anim_animate
        0x8110, // op_reg_anim_animate_reverse
        0x8126, // op_reg_anim_animate_forever
        0x813B, // op_reg_anim_play_sfx
        0x811A, // op_explosion
        0x811F, // op_gsay_option
        0x8129, // op_gdialog_barter
        0x8131, // op_obj_open
        0x8133, // op_game_ui_disable
        0x8134, // op_game_ui_enable
        0x8119, // op_get_day
        0x8124, // op_party_add
        0x8122, // op_poison
        0x8152, // op_critter_set_flee_state
        0x80ED, // op_kill_critter
        0x80CC, // op_animate_stand_obj
        0x814D, // op_jam_lock
        0x8150, // op_obj_on_screen
        0x8149, // op_obj_art_fid
        0x814A, // op_art_anim
        0x80FD, // op_radiation_inc
        0x80FE, // op_radiation_dec
        0x8153, // op_terminate_combat
        0x8115, // op_play_gmovie
        0x812C, // op_inven_unwield
        0x80DB, // op_use_obj
        0x813D, // op_sfx_build_char_name
        0x814E, // op_gdialog_set_barter_mod
        0x80A2, // op_scr_return
        0x80A8, // op_set_map_start
        0x80D7, // op_drop_obj
        0x80F5, // op_obj_can_hear_obj
        0x80F8, // op_tile_is_visible
        0x8113, // op_reg_anim_obj_move_to_tile
        0x8141, // op_sfx_build_weapon_name
        0x8145, // op_use_obj_on_obj
        0x8146, // op_endgame_slideshow
        0x8155, // op_critter_stop_attacking
        0x80E4, // op_load_map
        0x80C0, // op_obj_being_used_with
        0x80E3, // op_set_obj_visibility
        0x812E, // op_obj_lock
        0x812F, // op_obj_unlock
        0x8130, // op_obj_is_open
        0x80DA, // op_wield_obj_critter
        0x8118, // op_month
        0x8138, // op_item_caps_total
        0x8139, // op_item_caps_adjust
        0x80D0, // op_attack
        0x8143, // op_attack_setup
        0x80B9, // op_script_overrides
        0x810E, // op_reg_anim_func
        0x80DE, // op_start_gdialog
        0x80DF, // op_end_gdialog
        0x80F9, // op_dialogue_system_enter
        0x811E, // op_gsay_reply
        0x8120, // op_gsay_message
        0x8121, // op_giq_option
        0x80E1, // op_metarule3
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
        0x80BB, // op_tile_contains_obj_pid
        0x80F7, // op_fixed_param
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
    public int random(int min, int max) {
        return GameRandom.randomBetween(min, max);
    }

    /**
     * op_get_map_var (0x80C3): one of the map's variables.
     *
     * <p>A variable the map does not have answers -1.
     */
    @Override
    public Program.ProgramValue getMapVar(int variable) {
        MapVars.ProgramValue value = new MapVars.ProgramValue();
        if (MapVars.mapGetGlobalVar(variable, value) == -1) {
            value.opcode = MapVars.VALUE_TYPE_INT;
            value.integerValue = -1;
        }
        return value;
    }

    /**
     * op_set_map_var (0x80C4): write one of the map's variables.
     */
    @Override
    public void setMapVar(int variable, Program.ProgramValue value) {
        MapVars.ProgramValue mapValue = new MapVars.ProgramValue();
        mapValue.opcode = value.opcode;
        mapValue.integerValue = value.integerValue;
        mapValue.floatValue = value.floatValue;
        mapValue.pointerValue = value.pointerValue;
        MapVars.mapSetGlobalVar(variable, mapValue);
    }

    /**
     * op_game_ticks (0x80F2): the game's clock, in tenths of a second.
     *
     * <p>The original multiplies the ticks by ten, and a negative number
     * of ticks is no ticks at all.
     */
    @Override
    public int gameTicks(int seconds) {
        int ticks = seconds < 0 ? 0 : seconds;
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
    public int elevation(Object object) {
        if (object instanceof ObjTypes.GameObject obj) {
            return obj.elevation;
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
        // The perk engine and the trait engine are not ported yet, so no
        // perk or trait can be added.
        return -1;
    }

    /**
     * op_critter_rm_trait (0x8103): remove a perk or trait from a critter.
     *
     * <p>The perk engine and the trait engine are not ported yet, so no
     * perk or trait can be removed.
     */
    @Override
    public int critterRmTrait(Object object, int kind, int param, int value) {
        if (object == null) {
            return -1;
        }
        return -1;
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
    public int tileDistance(int tile1, int tile2) {
        return TileMath.tileDistanceBetween(tile1, tile2);
    }

    /**
     * op_tile_distance_objs (0x80D3): the distance between the tiles two
     * objects stand on.
     *
     * <p>Only objects on the same elevation are close enough to have a
     * distance; the rest are as far apart as the grid allows.
     */
    @Override
    public int tileDistanceObjs(Object first, Object second) {
        if (first == null || second == null) {
            return TileMath.TILE_MAX_DISTANCE;
        }

        ObjTypes.GameObject objFirst = (ObjTypes.GameObject) first;
        ObjTypes.GameObject objSecond = (ObjTypes.GameObject) second;
        if (objFirst.elevation != objSecond.elevation) {
            return TileMath.TILE_MAX_DISTANCE;
        }
        return TileMath.tileDistanceBetween(objFirst.tile, objSecond.tile);
    }

    /**
     * op_tile_num_in_direction (0x80D5): the tile that lies so many steps
     * away in a direction.
     *
     * <p>The direction is the number the script names it by, and a tile at
     * the edge of the grid has nowhere to go.
     */
    @Override
    public int tileNumInDirection(int tile, int rotation, int distance) {
        if (!Rotation.isValid(rotation)) {
            return tile;
        }
        return TileMath.tileGetTileInDirection(tile, Rotation.fromInt(rotation), distance);
    }

    /**
     * op_item_subtype (0x80C9): the type of an item.
     *
     * <p>Something that is not an item has no type at all, and the original
     * answers the invalid type for it.
     */
    @Override
    public int itemSubtype(Object object) {
        if (object == null) {
            return ProtoTypes.ItemType.ITEM_TYPE_INVALID.id();
        }
        ObjTypes.GameObject item = (ObjTypes.GameObject) object;
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
    public void addObjToInven(Object owner, Object item) {
        if (owner == null || item == null) {
            return;
        }
        Item.itemAdd((ObjTypes.GameObject) owner, (ObjTypes.GameObject) item, 1);
    }

    /**
     * op_rm_obj_from_inven (0x80D9): take an object out of the inventory of
     * another one.
     *
     * <p>Nothing happens when there is no owner or no object at all.
     */
    @Override
    public void rmObjFromInven(Object owner, Object item) {
        if (owner == null || item == null) {
            return;
        }
        Item.itemRemove((ObjTypes.GameObject) owner, (ObjTypes.GameObject) item, 1);
    }

    /**
     * op_rotation_to_tile (0x814C): the direction one tile lies in from
     * another one.
     *
     * <p>The direction is the number the game gives it: north-east is zero.
     */
    @Override
    public int rotationToTile(int tile1, int tile2) {
        return TileMath.tileGetRotationTo(tile1, tile2).value();
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
    public Object createObject(int protoId, int tile, int elevation, int sid) {
        if (protoId == 0) {
            return null;
        }
        ObjTypes.GameObject[] slot = new ObjTypes.GameObject[1];
        if (ObjectSystem.objectCreateWithProtoId(slot, ProtoTypes.ProtoId.ofPid(protoId)) == -1) {
            return null;
        }
        ObjTypes.GameObject object = slot[0];
        object.sid = sid;
        ObjectSystem.objectSetLocation(object, tile == -1 ? 0 : tile, elevation);
        return object;
    }

    /**
     * op_destroy_object (0x80F4): take an object out of the game.
     *
     * <p>Nothing happens when there is no object at all.
     */
    @Override
    public void destroyObject(Object object) {
        if (object == null) {
            return;
        }
        ObjectSystem.objectDestroy((ObjTypes.GameObject) object);
    }

    /**
     * op_obj_name (0x80A4): the name of an object.
     *
     * <p>The name comes with the game's message stack, and the port has no
     * game phase yet, so the name of an object is not known.
     */
    @Override
    public String objName(Object object) {
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
    public void addMultObjsToInven(Object owner, Object item, int count) {
        if (owner == null || item == null) {
            return;
        }
        Item.itemAdd((ObjTypes.GameObject) owner, (ObjTypes.GameObject) item, count < 0 ? 1 : count);
    }

    /**
     * op_rm_mult_objs_from_inven (0x8117): take a number of objects out of
     * the inventory of another one.
     *
     * <p>Not more objects are taken than the inventory holds, and nothing
     * is taken when there is no owner or no object at all.
     */
    @Override
    public int rmMultObjsFromInven(Object owner, Object item, int count) {
        if (owner == null || item == null) {
            return 0;
        }
        ObjTypes.GameObject objOwner = (ObjTypes.GameObject) owner;
        ObjTypes.GameObject objItem = (ObjTypes.GameObject) item;
        int quantity = Item.itemGetQuantity(objOwner, objItem);
        if (quantity > count) {
            quantity = count;
        }
        if (quantity != 0) {
            Item.itemRemoveWithReason(objOwner, objItem, quantity,
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
    public int destroyMultObjs(Object object, int count) {
        if (object == null) {
            return 0;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        int destroyed = 0;
        if (obj.owner != null) {
            int quantity = Item.itemGetQuantity(obj.owner, obj);
            if (quantity > count) {
                quantity = count;
            }
            if (quantity != 0) {
                Item.itemRemoveWithReason(obj.owner, obj, quantity,
                    Item.RemoveInventoryObjectHookReason.ITEM_DESTROY_MULTI, null);
                destroyed = quantity;
            }
        }
        ObjectSystem.objectDestroy(obj);
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
    public int hasTrait(int kind, Object object, int param) {
        if (object == null) {
            return 0;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        switch (kind) {
        case CRITTER_TRAIT_PERK:
            return 0;
        case CRITTER_TRAIT_OBJECT:
            switch (param) {
            case CRITTER_TRAIT_OBJECT_ROTATION:
                return obj.rotation.id();
            case CRITTER_TRAIT_OBJECT_IS_INVISIBLE:
                return obj.flags.and(ObjTypes.ObjectFlags.HIDDEN).bits() == 0 ? 1 : 0;
            case CRITTER_TRAIT_OBJECT_GET_INVENTORY_WEIGHT:
                return Item.objectGetInventoryWeight(obj);
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
    public int success(int roll) {
        switch (roll) {
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
    public int critical(int roll) {
        switch (roll) {
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
    public void floatMsg(Program.ProgramValue message, int type) {
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
    public int rollVsSkill(Object object, int skill, int modifier) {
        if (object == null) {
            return GameRandom.Roll.CRITICAL_FAILURE.ordinal();
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        if (ProtoTypes.ProtoId.ofGameObject(obj).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return GameRandom.Roll.CRITICAL_FAILURE.ordinal();
        }
        return Skill.skillRoll(obj, skill, modifier, new int[1]).ordinal();
    }

    /**
     * op_give_exp_points (0x80A1): give experience to the player.
     *
     * <p>The experience goes to the player, and the port has no player
     * yet, so the experience goes nowhere.
     */
    @Override
    public void giveExpPoints(int experience) {
    }

    /**
     * op_tile_contains_pid_obj (0x80A7): the object of a proto on a tile.
     *
     * <p>The proto id is the number the game gives it, and there is no
     * object when the tile is the one of "nowhere".
     */
    @Override
    public Object tileContainsPidObj(int protoId, int elevation, int tile) {
        if (tile == -1) {
            return null;
        }
        ProtoTypes.ProtoId target = ProtoTypes.ProtoId.ofPid(protoId);
        ObjTypes.GameObject object = ObjectSystem.objectFindFirstAtLocation(elevation, tile);
        while (object != null) {
            if (ProtoTypes.ProtoId.ofGameObject(object).equals(target)) {
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
    public void addTimerEvent(Object object, int delay, int param) {
        if (object == null) {
            return;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        EventQueue.queueAddEvent(delay, obj, new EventQueue.ScriptEvent(obj.sid, param),
            EventQueue.EventType.SCRIPT);
    }

    /**
     * op_rm_timer_event (0x80F1): take the timer events of an object away.
     *
     * <p>Nothing happens when there is no object at all.
     */
    @Override
    public void rmTimerEvent(Object object) {
        if (object == null) {
            return;
        }
        EventQueue.queueRemoveEvents((ObjTypes.GameObject) object);
    }

    /**
     * op_set_light_level (0x80E9): the ambient light of the map.
     *
     * <p>The level is a percentage: 50 is the middle of the range, above it
     * the light goes up to the top of the range and below it down to the
     * bottom, the way the original spreads it over the two halves.
     */
    @Override
    public void setLightLevel(int level) {
        int min = Light.LIGHT_INTENSITY_MIN;
        int max = Light.LIGHT_INTENSITY_MAX;
        int middle = (min + max) / 2;
        if (level == 50) {
            Light.lightSetAmbientIntensity(middle, true);
            return;
        }
        int intensity = level > 50
                ? middle + level * (max - middle) / 100
                : min + level * (middle - min) / 100;
        Light.lightSetAmbientIntensity(intensity, true);
    }

    /**
     * op_obj_set_light_level (0x8107): the light of an object.
     *
     * <p>The intensity is a percentage of the full light, and a light of
     * zero puts the object out.
     */
    @Override
    public void objSetLightLevel(Object object, int distance, int intensity) {
        if (object == null) {
            return;
        }
        ObjectSystem.objectSetLight((ObjTypes.GameObject) object, distance,
            intensity != 0 ? (intensity * 65536) / 100 : 0);
    }

    /**
     * op_anim_busy (0x80E7): whether an object is busy animating.
     *
     * <p>The port keeps no animation queue, so an object is never busy, and
     * the original's error value is returned for no object at all.
     */
    @Override
    public int animBusy(Object object) {
        if (object == null) {
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
    public int objCanSeeObj(Object watcher, Object target) {
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
    public Object invenCmds(Object object, int cmd, int index) {
        if (object == null) {
            return null;
        }
        if (!(object instanceof ObjTypes.GameObject)) {
            return null;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        if (ProtoId.ofGameObject(obj).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return null;
        }
        if (cmd != 13) {
            return null;
        }
        return Inventory.inventoryItemByIndex(obj, index);
    }

    /**
     * op_set_obj_visibility (0x80E3): show or hide an object.
     *
     * <p>A non-zero value hides the object and a zero shows it. Nothing
     * happens when there is no object at all, and hiding a critter also
     * makes it not block the way.
     */
    @Override
    public void setObjVisibility(Object object, int invisible) {
        if (object == null) {
            return;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        if (invisible != 0) {
            if (obj.flags.and(ObjTypes.ObjectFlags.HIDDEN).equals(ObjTypes.ObjectFlags.NONE)) {
                if (ProtoId.ofGameObject(obj).objectType() == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
                    obj.flags = obj.flags.or(ObjTypes.ObjectFlags.NO_BLOCK);
                }
                ObjectSystem.objectHide(obj);
            }
        } else {
            if (!obj.flags.and(ObjTypes.ObjectFlags.HIDDEN).equals(ObjTypes.ObjectFlags.NONE)) {
                if (ProtoId.ofGameObject(obj).objectType() == ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
                    obj.flags = obj.flags.and(ObjTypes.ObjectFlags.NO_BLOCK.not());
                }
                ObjectSystem.objectShow(obj);
            }
        }
    }

    /**
     * op_target_obj (0x80BE): the object the script is aimed at.
     *
     * <p>The target is not the object the script runs for, but the one it is
     * aimed at, and the scripts of the game are started by the game itself,
     * so there is no target: the original answers the same in its harness.
     */
    @Override
    public Object targetObj() {
        return null;
    }

    /**
     * op_fixed_param (0x80F7): the parameter of the event the script runs
     * for.
     *
     * <p>The parameter belongs to the event that started the script, and the
     * port has no event that started the script, so the parameter is 0.
     */
    @Override
    public int fixedParam() {
        return 0;
    }

    /**
     * op_tile_contains_obj_pid (0x80BB): whether an object of a proto is on
     * a tile.
     *
     * <p>The proto id is the number the game gives it, and the tile of
     * "nowhere" has nothing on it.
     */
    @Override
    public int tileContainsObjPid(int protoId, int elevation, int tile) {
        if (tile == -1) {
            return 0;
        }
        ProtoTypes.ProtoId target = ProtoTypes.ProtoId.ofPid(protoId);
        ObjTypes.GameObject object = ObjectSystem.objectFindFirstAtLocation(elevation, tile);
        while (object != null) {
            if (ProtoTypes.ProtoId.ofGameObject(object).equals(target)) {
                return 1;
            }
            object = ObjectSystem.objectFindNextAtLocation();
        }
        return 0;
    }

    /**
     * op_metarule3 (0x80E1): a metarule of the game.
     *
     * <p>The rule is the number the game gives it, and the parameter is the
     * number or the object it asks about. The port has no metarules, so the
     * answer is 0 and the events of a timer are not cleared.
     */
    @Override
    public Program.ProgramValue metarule3(int rule, Program.ProgramValue param1,
            Program.ProgramValue param2, Program.ProgramValue param3) {
        if (rule == 100) {
            // CLR_FIXED_TIMED_EVENTS: clear the timer events of an object.
            if (param1.pointerValue instanceof ObjTypes.GameObject) {
                EventQueue.queueRemoveEvents((ObjTypes.GameObject) param1.pointerValue);
            }
        }
        return new Program.ProgramValue(0);
    }

    /**
     * op_start_gdialog (0x80DE): start a dialog with a critter.
     *
     * <p>The dialog needs the dialog system, and the port has no dialog
     * system yet, so nothing is shown.
     */
    @Override
    public void startGdialog(int unused, Object object, int reactionLevel,
            int headFrmId, int backgroundFrmId) {
    }

    /**
     * op_end_gdialog (0x80DF): end the dialog.
     *
     * <p>The port has no dialog system, so nothing is ended.
     */
    @Override
    public void endGdialog() {
    }

    /**
     * op_dialogue_system_enter (0x80F9): enter the dialog system.
     *
     * <p>The port has no dialog system, so nothing is entered.
     */
    @Override
    public void dialogueSystemEnter() {
    }

    /**
     * op_gsay_reply (0x811E): the reply of a dialog.
     *
     * <p>The port has no dialog system, so the reply is not shown.
     */
    @Override
    public void gsayReply(int messageListId, Program.ProgramValue msg) {
    }

    /**
     * op_gsay_message (0x8120): a message of a dialog.
     *
     * <p>The port has no dialog system, so the message is not shown.
     */
    @Override
    public void gsayMessage(int messageListId, Program.ProgramValue msg, int reaction) {
    }

    /**
     * op_giq_option (0x8121): an option of a dialog.
     *
     * <p>The port has no dialog system, so the option is not shown.
     */
    @Override
    public void giqOption(int iq, int messageListId, Program.ProgramValue msg,
            Program.ProgramValue a3, int a4) {
    }

    /**
     * op_reg_anim_func (0x810E): begin, clear or end an animation.
     *
     * <p>The animation needs the animation queue, and the port has no
     * animation queue yet, so nothing is registered.
     */
    @Override
    public void regAnimFunc(int cmd, Program.ProgramValue param) {
    }

    /**
     * op_script_overrides (0x80B9): the script takes over the action.
     *
     * <p>The flag belongs to the script that runs, and the port has no
     * registry of the scripts, so there is no script to set it on.
     */
    @Override
    public void scriptOverrides() {
    }

    /**
     * op_attack (0x80D0): attack a critter.
     *
     * <p>The attack needs the combat system, and the port has no combat
     * system yet, so there is no attack.
     */
    @Override
    public void attack(Object target, int attackerResults, int targetResults,
            int maxDamage, int minDamage, int accuracyBonus, int unused1,
            int unused2) {
    }

    /**
     * op_attack_setup (0x8143): set up an attack of a critter on another.
     *
     * <p>The attack needs the combat system, and the port has no combat
     * system yet, so there is no attack.
     */
    @Override
    public void attackSetup(Object attacker, Object defender) {
    }

    /**
     * op_month (0x8118): the month of the game's clock.
     */
    @Override
    public int month() {
        int[] monthOut = new int[1];
        GameTime.gameTimeGetDate(monthOut, null, null);
        return monthOut[0];
    }

    /**
     * op_item_caps_total (0x8138): how many caps the object carries.
     */
    @Override
    public int itemCapsTotal(Object object) {
        if (object == null) {
            return 0;
        }
        return Item.itemGetTotalCaps((ObjTypes.GameObject) object);
    }

    /**
     * op_item_caps_adjust (0x8139): add or remove caps of an object.
     */
    @Override
    public int itemCapsAdjust(Object object, int amount) {
        if (object == null) {
            return -1;
        }
        return Item.itemCapsAdjust((ObjTypes.GameObject) object, amount);
    }

    /**
     * op_wield_obj_critter (0x80DA): put an object into a hand of a
     * critter.
     *
     * <p>Only a critter can hold a thing in a hand, and the player holds it
     * in the hand the interface has chosen. Nothing happens when there is
     * no critter or no object at all.
     */
    @Override
    public void wieldObjCritter(Object critter, Object item) {
        if (critter == null || item == null) {
            return;
        }
        ObjTypes.GameObject crit = (ObjTypes.GameObject) critter;
        if (ProtoId.ofGameObject(crit).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return;
        }
        Inventory.Hand hand = Inventory.Hand.HAND_RIGHT;
        if (crit == ObjectSystem.getDude()
                && Interface.interfaceGetCurrentHand() == Inventory.Hand.HAND_LEFT) {
            hand = Inventory.Hand.HAND_LEFT;
        }
        Inventory.inventoryEquipFunc(crit, (ObjTypes.GameObject) item, hand, false);
    }

    /**
     * op_obj_lock (0x812E): lock a container or a door.
     *
     * <p>Only a container or a door can be locked, and nothing happens for
     * the other kinds of objects.
     */
    @Override
    public void objLock(Object object) {
        if (object == null) {
            return;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        ObjTypes.ObjectType type = ProtoTypes.ProtoId.ofGameObject(obj).objectType();
        if (type == ObjTypes.ObjectType.OBJ_TYPE_ITEM) {
            obj.data.flags |= ObjTypes.CONTAINER_FLAG_LOCKED;
        } else if (type == ObjTypes.ObjectType.OBJ_TYPE_SCENERY) {
            obj.data.scenery.door.openFlags |= ObjTypes.DOOR_FLAG_LOCKED;
        }
    }

    /**
     * op_obj_unlock (0x812F): unlock a container or a door.
     *
     * <p>Only a container or a door can be unlocked, and nothing happens for
     * the other kinds of objects.
     */
    @Override
    public void objUnlock(Object object) {
        if (object == null) {
            return;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        ObjTypes.ObjectType type = ProtoTypes.ProtoId.ofGameObject(obj).objectType();
        if (type == ObjTypes.ObjectType.OBJ_TYPE_ITEM) {
            obj.data.flags &= ~ObjTypes.CONTAINER_FLAG_LOCKED;
        } else if (type == ObjTypes.ObjectType.OBJ_TYPE_SCENERY) {
            obj.data.scenery.door.openFlags &= ~ObjTypes.DOOR_FLAG_LOCKED;
        }
    }

    /**
     * op_obj_is_open (0x8130): whether a door is open.
     *
     * <p>A door is open when its frame is not the first one.
     */
    @Override
    public int objIsOpen(Object object) {
        if (object == null) {
            return 0;
        }
        return ((ObjTypes.GameObject) object).frame != 0 ? 1 : 0;
    }

    /**
     * op_move_to (0x80B6): move an object to a tile.
     *
     * <p>The new tile of the object is the answer, and the tile of
     * "nowhere" has no tile to move to.
     */
    @Override
    public int moveTo(Object object, int tile, int elevation) {
        if (object == null) {
            return -1;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) object;
        int newTile = ObjectSystem.objectSetLocation(obj, tile, elevation);
        if (newTile != -1 && obj == ObjectSystem.getDude()) {
            TileMath.tileSetCenter(obj.tile,
                TileMath.TILE_SET_CENTER_REFRESH_WINDOW);
        }
        return newTile;
    }

    /**
     * op_obj_being_used_with (0x80C0): the object the script is being used
     * with.
     *
     * <p>The object is the one the script is used with, and the port has no
     * script registry, so there is no such object: the original answers the
     * same in its harness.
     */
    @Override
    public Object objBeingUsedWith() {
        return null;
    }

    /**
     * op_has_skill (0x80AA): the value of a skill of a critter.
     *
     * <p>A critter that is not a critter has no skill, and the original
     * answers 0 for it.
     */
    @Override
    public int hasSkill(Object a0, int a1) {
        if (a0 == null) {
            return 0;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        if (ProtoTypes.ProtoId.ofGameObject(object).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }
        return Skill.skillGetValue(object, a1);
    }

    /**
     * op_obj_is_locked (0x812D): whether a container or a door is locked.
     *
     * <p>Only a container or a door can be locked, and nothing else is
     * locked.
     */
    @Override
    public int objIsLocked(Object a0) {
        if (a0 == null) {
            return 0;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        ObjTypes.ObjectType type = ProtoTypes.ProtoId.ofGameObject(object).objectType();
        if (type == ObjTypes.ObjectType.OBJ_TYPE_ITEM) {
            return (object.data.flags & ObjTypes.CONTAINER_FLAG_LOCKED) != 0 ? 1 : 0;
        } else if (type == ObjTypes.ObjectType.OBJ_TYPE_SCENERY) {
            return (object.data.scenery.door.openFlags & ObjTypes.DOOR_FLAG_LOCKED) != 0 ? 1 : 0;
        }
        return 0;
    }

    /**
     * op_critter_is_fleeing (0x8151): whether a critter is fleeing.
     *
     * <p>A critter that is not a critter is not fleeing, and the original
     * answers 0 for it.
     */
    @Override
    public int critterIsFleeing(Object a0) {
        if (a0 == null) {
            return 0;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        return object.data.critter.combat.maneuver
                .and(ObjTypes.CritterManeuver.FLEEING)
                .equals(ObjTypes.CritterManeuver.NONE) ? 0 : 1;
    }

    /**
     * op_is_in_combat (0x8128): whether the game is in combat.
     *
     * <p>The combat state is not ported, and the port is never in combat:
     * the original answers the same in its harness, where there is no game.
     */
    @Override
    public int isInCombat() {
        return 0;
    }

    /**
     * op_game_time_advance (0x80FC): advance the game's clock.
     *
     * <p>The clock is advanced by the ticks, and the events of the queue
     * that are due are processed after each day and at the end.
     */
    @Override
    public void gameTimeAdvance(int a0) {
        int days = a0 / GameTime.TICKS_PER_DAY;
        int remainder = a0 % GameTime.TICKS_PER_DAY;
        for (int day = 0; day < days; day++) {
            GameTime.gameTimeAddTicks(GameTime.TICKS_PER_DAY);
            EventQueue.queueProcessEvents();
        }
        GameTime.gameTimeAddTicks(remainder);
        EventQueue.queueProcessEvents();
    }

    /**
     * op_move_obj_inven_to_obj (0x8147): move every item of an object into
     * the inventory of another one.
     *
     * <p>Nothing happens when there is no object at all.
     */
    @Override
    public void moveObjInvenToObj(Object a0, Object a1) {
        if (a0 == null || a1 == null) {
            return;
        }
        Item.itemMoveAll((ObjTypes.GameObject) a0, (ObjTypes.GameObject) a1);
    }

    /**
     * op_action_being_used (0x80FA): the action the script is being used
     * for.
     *
     * <p>The action belongs to the script, and the port has no script
     * registry, so there is no action, and the original answers -1 for a
     * program with no script.
     */
    @Override
    public int actionBeingUsed() {
        return -1;
    }

    /**
     * op_gfade_out (0x8136): fade the screen out.
     *
     * <p>The fade is drawn by the render engine, and the port has no render
     * engine, so the screen is not faded.
     */
    @Override
    public void gfadeOut(int a0) {
    }

    /**
     * op_gfade_in (0x8137): fade the screen in.
     *
     * <p>The fade is drawn by the render engine, and the port has no render
     * engine, so the screen is not faded.
     */
    @Override
    public void gfadeIn(int a0) {
    }

    /**
     * op_critter_damage (0x80EF): damage a critter.
     *
     * <p>The damage is given by the combat system, and the port has no
     * combat system, so there is no damage.
     */
    @Override
    public void critterDamage(Object a0, int a1, int a2) {
    }

    /**
     * op_do_check (0x80AE): roll a primary stat of a critter.
     *
     * <p>The roll is the number the game gives it: a critical failure is
     * zero, a failure is one, a success is two and a critical success is
     * three. Only the six primary stats can be rolled, and a critter that
     * is not there has no roll at all.
     */
    @Override
    public int doCheck(Object a0, int a1, int a2) {
        if (a0 == null) {
            return 0;
        }
        if (a1 < 0 || a1 > StatDefs.Stat.STAT_LUCK.id()) {
            return 0;
        }
        return Stat.statRoll((ObjTypes.GameObject) a0, a1, a2, new int[1]).ordinal();
    }

    /**
     * op_anim (0x810C): play an animation of an object.
     *
     * <p>The animation needs the animation engine, and the port has no
     * animation engine, so nothing is played.
     */
    @Override
    public void anim(Object a0, int a1, int a2, Program.ProgramValue a3) {
    }

    /**
     * op_obj_close (0x8132): close a door.
     *
     * <p>The close is played by the animation engine, and the port has no
     * animation engine, so nothing is played.
     */
    @Override
    public void objClose(Object a0) {
    }

    /**
     * op_critter_heal (0x80E8): heal a critter.
     *
     * <p>The hit points of the critter are healed by the amount, and a
     * critter that is not a critter is not healed.
     */
    @Override
    public int critterHeal(Object a0, int a1) {
        return Critter.critterAdjustHitPoints((ObjTypes.GameObject) a0, a1);
    }

    /**
     * op_get_poison (0x8123): the poison of a critter.
     *
     * <p>A critter that is not a critter has no poison, and the original
     * answers 0 for it.
     */
    @Override
    public int getPoison(Object a0) {
        if (a0 == null) {
            return 0;
        }
        ObjTypes.GameObject obj = (ObjTypes.GameObject) a0;
        if (ProtoTypes.ProtoId.ofGameObject(obj).objectType() != ObjTypes.ObjectType.OBJ_TYPE_CRITTER) {
            return 0;
        }
        return Critter.critterGetPoison(obj);
    }

    /**
     * op_critter_injure (0x8127): injure a critter.
     *
     * <p>The injure sets the crippling flags on the critter, and the
     * reverse clears them instead, the way the original does.
     */
    @Override
    public void critterInjure(Object a0, int a1) {
        if (a0 == null) {
            return;
        }
        ObjTypes.GameObject critter = (ObjTypes.GameObject) a0;
        boolean reverse = (a1 & ObjTypes.Dam.PERFORM_REVERSE.bits()) != 0;
        ObjTypes.Dam flags = new ObjTypes.Dam(a1 & ObjTypes.Dam.CRIP.bits());
        if (reverse) {
            critter.data.critter.combat.results =
                critter.data.critter.combat.results.and(flags.not());
        } else {
            critter.data.critter.combat.results =
                critter.data.critter.combat.results.or(flags);
        }
    }

    /**
     * op_proto_data (0x8104): read a member of the proto of an object.
     *
     * <p>The member is the number the game gives it, and the answer is the
     * value of the member, or 0 when the game has no such member.
     */
    @Override
    public Program.ProgramValue protoData(int a0, int a1) {
        ProtoDefs.ProtoDataMemberValue value = new ProtoDefs.ProtoDataMemberValue();
        int valueType = ProtoDb.protoGetDataMember(ProtoTypes.ProtoId.ofPid(a0), a1, value);
        Program.ProgramValue result = new Program.ProgramValue();
        if (valueType == ProtoDefs.ProtoDataMemberType.PROTO_DATA_MEMBER_TYPE_STRING.id()) {
            result.opcode = Program.VALUE_TYPE_STRING;
            result.pointerValue = value.stringValue;
        } else {
            result.opcode = Program.VALUE_TYPE_INT;
            result.integerValue = value.integerValue;
        }
        return result;
    }

    /**
     * op_using_skill (0x80AB): whether the player is using a skill.
     *
     * <p>Only the sneaking skill of the player is asked, and the sneaking
     * state belongs to the game phase, so the player is never sneaking.
     */
    @Override
    public int usingSkill(Object a0, int a1) {
        if (a1 == SkillDefs.Skill.SKILL_SNEAK.id() && a0 == ObjectSystem.getDude()) {
            return Critter.dudeHasState(Critter.DUDE_STATE_SNEAKING) ? 1 : 0;
        }
        return 0;
    }

    /**
     * op_party_remove (0x8125): take an object out of the party.
     *
     * <p>Nothing happens when there is no object at all.
     */
    @Override
    public void partyRemove(Object a0) {
        if (a0 == null) {
            return;
        }
        PartyMembers.partyMemberRemove((ObjTypes.GameObject) a0);
    }

    /**
     * op_set_exit_grids (0x80E6): set the destination of the exit grids of
     * an elevation.
     *
     * <p>The exit grids of the elevation get the destination, the way the
     * original does.
     */
    @Override
    public void setExitGrids(int a0, int a1, int a2, int a3, int a4) {
        ObjTypes.GameObject object = ObjectSystem.objectFindFirstAtElevation(a0);
        while (object != null) {
            if (ProtoDefs.isExitGridProtoId(ProtoTypes.ProtoId.ofGameObject(object))) {
                object.data.misc.map = WorldmapDefs.ofMap(a1);
                object.data.misc.tile = a3;
                object.data.misc.elevation = a2;
            }
            object = ObjectSystem.objectFindNextAtElevation();
        }
    }

    /**
     * op_play_sfx (0x80A3): play a sound effect.
     *
     * <p>The sound is played by the sound engine, and the port has no sound
     * engine, so nothing is played.
     */
    @Override
    public void playSfx(String a0) {
    }

    /**
     * op_reg_anim_animate (0x810F): play an animation of an object.
     *
     * <p>The animation needs the animation engine, and the port has no
     * animation engine, so nothing is played.
     */
    @Override
    public void regAnimAnimate(Object a0, int a1, int a2) {
    }

    /**
     * op_reg_anim_animate_reverse (0x8110): play an animation of an object
     * backwards.
     *
     * <p>The animation needs the animation engine, and the port has no
     * animation engine, so nothing is played.
     */
    @Override
    public void regAnimAnimateReverse(Object a0, int a1, int a2) {
    }

    /**
     * op_reg_anim_animate_forever (0x8126): play an animation of an object
     * forever.
     *
     * <p>The animation needs the animation engine, and the port has no
     * animation engine, so nothing is played.
     */
    @Override
    public void regAnimAnimateForever(Object a0, int a1) {
    }

    /**
     * op_reg_anim_play_sfx (0x813B): play a sound effect with an animation.
     *
     * <p>The sound and the animation need their engines, and the port has
     * neither, so nothing is played.
     */
    @Override
    public void regAnimPlaySfx(Object a0, String a1, int a2) {
    }

    /**
     * op_explosion (0x811A): explode at a tile.
     *
     * <p>The explosion needs the combat system, and the port has no combat
     * system, so there is no explosion.
     */
    @Override
    public void explosion(int a0, int a1, int a2) {
    }

    /**
     * op_gsay_option (0x811F): an option of a dialog.
     *
     * <p>The port has no dialog system, so the option is not shown.
     */
    @Override
    public void gsayOption(int a0, Program.ProgramValue a1,
            Program.ProgramValue a2, int a3) {
    }

    /**
     * op_gdialog_barter (0x8129): start a barter in a dialog.
     *
     * <p>The port has no dialog system, so the barter is not started.
     */
    @Override
    public void gdialogBarter(int a0) {
    }

    /**
     * op_obj_open (0x8131): open a door.
     *
     * <p>The open is played by the animation engine, and the port has no
     * animation engine, so nothing is played.
     */
    @Override
    public void objOpen(Object a0) {
    }

    /**
     * op_game_ui_disable (0x8133): disable the interface of the game.
     *
     * <p>The interface belongs to the render engine, and the port has no
     * render engine, so the interface is not disabled.
     */
    @Override
    public void gameUiDisable() {
    }

    /**
     * op_game_ui_enable (0x8134): enable the interface of the game.
     *
     * <p>The interface belongs to the render engine, and the port has no
     * render engine, so the interface is not enabled.
     */
    @Override
    public void gameUiEnable() {
    }

    /**
     * op_get_day (0x8119): the day of the game's clock.
     */
    @Override
    public int day() {
        int[] dayOut = new int[1];
        GameTime.gameTimeGetDate(null, dayOut, null);
        return dayOut[0];
    }

    /**
     * op_party_add (0x8124): add an object to the party.
     *
     * <p>Nothing happens when there is no object at all.
     */
    @Override
    public void partyAdd(Object a0) {
        if (a0 == null) {
            return;
        }
        PartyMembers.partyMemberAdd((ObjTypes.GameObject) a0);
    }

    /**
     * op_poison (0x8122): poison a critter.
     *
     * <p>The poison of the critter is changed by the amount, and a critter
     * that is not a critter is not poisoned.
     */
    @Override
    public void poison(Object a0, int a1) {
        if (a0 == null) {
            return;
        }
        Critter.critterAdjustPoison((ObjTypes.GameObject) a0, a1);
    }

    /**
     * op_critter_set_flee_state (0x8152): set or clear the fleeing of a
     * critter.
     *
     * <p>A non-zero value makes the critter flee and a zero stops it.
     */
    @Override
    public void critterSetFleeState(Object a0, int a1) {
        if (a0 == null) {
            return;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        if (a1 != 0) {
            object.data.critter.combat.maneuver =
                object.data.critter.combat.maneuver.or(ObjTypes.CritterManeuver.FLEEING);
        } else {
            object.data.critter.combat.maneuver =
                object.data.critter.combat.maneuver.and(ObjTypes.CritterManeuver.FLEEING.not());
        }
    }

    /**
     * op_kill_critter (0x80ED): kill a critter.
     *
     * <p>The kill needs the death animation, and the kill branch of the
     * port is not reachable without a game phase, so the critter is not
     * killed.
     */
    @Override
    public void killCritter(Object a0, int a1) {
    }

    /**
     * op_animate_stand_obj (0x80CC): make an object stand.
     *
     * <p>The stand is played by the animation engine, and the port has no
     * animation engine, so nothing is played.
     */
    @Override
    public void animateStandObj(Object a0) {
    }

    /**
     * op_jam_lock (0x814D): jam a lock of a container or a door.
     *
     * <p>Only a container or a door has a lock, and nothing happens for the
     * other kinds of objects.
     */
    @Override
    public void jamLock(Object a0) {
        if (a0 == null) {
            return;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        ObjTypes.ObjectType type = ProtoTypes.ProtoId.ofGameObject(object).objectType();
        if (type == ObjTypes.ObjectType.OBJ_TYPE_ITEM) {
            object.data.flags |= ObjTypes.CONTAINER_FLAG_JAMMED;
        } else if (type == ObjTypes.ObjectType.OBJ_TYPE_SCENERY) {
            object.data.scenery.door.openFlags |= ObjTypes.DOOR_FLAG_JAMMED;
        }
    }

    /**
     * op_obj_on_screen (0x8150): whether an object is on the screen.
     *
     * <p>The screen belongs to the render engine, and the port has no render
     * engine, so no object is on the screen.
     */
    @Override
    public int objOnScreen(Object a0) {
        return 0;
    }

    /**
     * op_obj_art_fid (0x8149): the fid of the art of an object.
     *
     * <p>The fid is the number the game gives it, and an object that is not
     * there has fid 0.
     */
    @Override
    public int objArtFid(Object a0) {
        if (a0 == null) {
            return 0;
        }
        return ((ObjTypes.GameObject) a0).fid;
    }

    /**
     * op_art_anim (0x814A): the animation of a fid.
     *
     * <p>The animation is the part of the fid the game names it by.
     */
    @Override
    public int artAnim(int a0) {
        return (a0 & 0xFF0000) >> 16;
    }

    /**
     * op_radiation_inc (0x80FD): increase the radiation of a critter.
     *
     * <p>Only the radiation of the player can be changed, and the
     * radiation of the player is changed by the amount, after the
     * radiation resistance of the player.
     */
    @Override
    public void radiationInc(Object a0, int a1) {
        if (a0 == null) {
            return;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        if (object != ObjectSystem.getDude()) {
            return;
        }
        int amount = a1;
        if (amount > 0) {
            amount -= Stat.critterGetStat(object,
                StatDefs.Stat.STAT_RADIATION_RESISTANCE.id()) * amount / 100;
        }
        object.data.critter.radiation += amount;
    }

    /**
     * op_radiation_dec (0x80FE): decrease the radiation of a critter.
     *
     * <p>Only the radiation of the player can be changed, and the
     * radiation of the player is decreased by the amount, but not below
     * zero.
     */
    @Override
    public void radiationDec(Object a0, int a1) {
        if (a0 == null) {
            return;
        }
        ObjTypes.GameObject object = (ObjTypes.GameObject) a0;
        if (object != ObjectSystem.getDude()) {
            return;
        }
        int adjustment = object.data.critter.radiation >= 0 ? -a1 : 0;
        object.data.critter.radiation += adjustment;
    }

    /**
     * op_terminate_combat (0x8153): end the combat.
     *
     * <p>The combat state is not ported, and the port is never in combat,
     * so there is no combat to end.
     */
    @Override
    public void terminateCombat() {
    }

    /**
     * op_play_gmovie (0x8115): play a movie.
     *
     * <p>The movie is played by the render engine, and the port has no
     * render engine, so nothing is played.
     */
    @Override
    public void playGmovie(int a0) {
    }

    /**
     * op_inven_unwield (0x812C): take the item out of the hand of the
     * script's critter.
     *
     * <p>The critter is the one the script runs for, and the port has no
     * script registry, so there is no critter to unwield.
     */
    @Override
    public void invenUnwield() {
    }

    /**
     * op_use_obj (0x80DB): use an object.
     *
     * <p>The use needs the action system, and the port has no action
     * system, so nothing is used.
     */
    @Override
    public void useObj(Object a0) {
    }

    /**
     * op_sfx_build_char_name (0x813D): the name of the sound of a critter.
     *
     * <p>The names of the sounds belong to the sound engine, and the port
     * has no sound engine, so there is no name.
     */
    @Override
    public String sfxBuildCharName(Object a0, int a1, int a2) {
        return null;
    }

    /**
     * op_gdialog_set_barter_mod (0x814E): set the barter modifier of a
     * dialog.
     *
     * <p>The port has no dialog system, so the modifier is not set.
     */
    @Override
    public void gdialogSetBarterMod(int a0) {
    }

    /**
     * op_scr_return (0x80A2): return a value from the script.
     *
     * <p>The value is returned to the caller of the script, and the port
     * has no script registry, so there is no caller.
     */
    @Override
    public void scrReturn(int a0) {
    }

    /**
     * op_set_map_start (0x80A8): set the start of a map.
     *
     * <p>The start of a map belongs to the map system, and the port has no
     * map system, so there is no start.
     */
    @Override
    public void setMapStart(int a0, int a1, int a2, int a3) {
    }

    /**
     * op_drop_obj (0x80D7): drop an object.
     *
     * <p>The drop is played by the animation engine, and the port has no
     * animation engine, so nothing is dropped.
     */
    @Override
    public void dropObj(Object a0) {
    }

    /**
     * op_obj_can_hear_obj (0x80F5): whether one object hears another.
     *
     * <p>The port has no perception, so two objects never hear each other:
     * the original answers the same in its harness, where there is no game.
     */
    @Override
    public int objCanHearObj(Object a0, Object a1) {
        return 0;
    }

    /**
     * op_tile_is_visible (0x80F8): whether a tile is visible.
     *
     * <p>The visibility belongs to the render engine, and the port has no
     * render engine, so no tile is visible.
     */
    @Override
    public int tileIsVisible(int a0) {
        return 0;
    }

    /**
     * op_reg_anim_obj_move_to_tile (0x8113): move an object to a tile with
     * an animation.
     *
     * <p>The animation needs the animation engine, and the port has no
     * animation engine, so nothing is played.
     */
    @Override
    public void regAnimObjMoveToTile(Object a0, int a1, int a2) {
    }

    /**
     * op_sfx_build_weapon_name (0x8141): the name of the sound of a
     * weapon.
     *
     * <p>The names of the sounds belong to the sound engine, and the port
     * has no sound engine, so there is no name.
     */
    @Override
    public String sfxBuildWeaponName(int a0, Object a1, int a2, Object a3) {
        return null;
    }

    /**
     * op_use_obj_on_obj (0x8145): use one object on another.
     *
     * <p>The use needs the action system, and the port has no action
     * system, so nothing is used.
     */
    @Override
    public void useObjOnObj(Object a0, Object a1) {
    }

    /**
     * op_endgame_slideshow (0x8146): play the slideshow of the end of the
     * game.
     *
     * <p>The slideshow is played by the render engine, and the port has no
     * render engine, so nothing is played.
     */
    @Override
    public void endgameSlideshow() {
    }

    /**
     * op_critter_stop_attacking (0x8155): stop a critter attacking.
     *
     * <p>The attack belongs to the combat system, and the port has no
     * combat system, so there is no attack to stop.
     */
    @Override
    public void critterStopAttacking(Object a0) {
    }

    /**
     * op_load_map (0x80E4): load a map.
     *
     * <p>The map is the number the game gives it, and the transition to the
     * map is set, the way the original does. A map that is not a map of
     * the game is not loaded.
     */
    @Override
    public void loadMap(Program.ProgramValue a0, int a1) {
        int mapIndex = -1;
        if (a0.opcode == Program.VALUE_TYPE_INT
                && MapRegistry.mapIsValid(a0.integerValue)) {
            mapIndex = a0.integerValue;
        }
        if (mapIndex != -1) {
            Map.MapTransition transition = new Map.MapTransition();
            transition.map = WorldmapDefs.ofMap(mapIndex);
            transition.elevation = -1;
            transition.tile = -1;
            transition.rotation = Rotation.INVALID;
            Map.mapSetTransition(transition);
        }
    }

    /**
     * op_get_critter_stat (0x80CA): a stat of a critter.
     *
     * <p>The stat is the number the game gives it, and a critter that is
     * not there has no stat at all.
     */
    @Override
    public int getCritterStat(Object critter, int stat) {
        if (critter == null) {
            return -1;
        }
        return Stat.critterGetStat((ObjTypes.GameObject) critter, stat);
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
    public int setCritterStat(Object critter, int stat, int amount) {
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
