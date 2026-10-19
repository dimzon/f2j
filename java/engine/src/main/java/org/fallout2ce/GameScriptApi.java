package org.fallout2ce;

import org.fallout2ce.ObjTypes.GameObject;
import org.fallout2ce.ProtoTypes.ProtoId;

/**
 * The script API backed by the game.
 *
 * <p>Only the builtins whose backing the port already provides are
 * implemented; the rest are refused, exactly as {@link UnsupportedScriptApi}
 * refuses them. A refused builtin stops the script with a fatal error, the
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
        0x80BC, // op_self_obj
        0x80C1, // op_get_local_var
        0x80C2, // op_set_local_var
        0x80BF, // opGetDude
        0x8101, // op_cur_map_index
        0x80C7, // op_script_action
        0x8102, // op_critter_add_trait
        0x814B, // op_party_member_obj
        0x80C5, // op_get_global_var
        0x80C6, // op_set_global_var
        0x80BA, // op_obj_is_carrying_obj
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
     * op_self_obj (0x80BC): the object the script runs for.
     *
     * <p>C's {@code scriptGetSelf} finds the script that owns the program and
     * returns its owner, or for a spatial script a hidden marker object. With
     * no script attached there is no owner, and the original answers null.
     */
    @Override
    public Object selfObj(Program program) {
        return program.scriptOwner;
    }

    /**
     * op_get_local_var (0x80C1): one of the script's own local variables.
     *
     * <p>C resolves the script context and reads the variable from the
     * script's storage. Without a script there is no context, and the
     * original answers -1.
     */
    @Override
    public Program.ProgramValue getLocalVar(Program program, int index) {
        return new Program.ProgramValue(-1);
    }

    /**
     * op_set_local_var (0x80C2): write one of the script's own local
     * variables. Without a script there is no context and the write is
     * dropped, like the original's.
     */
    @Override
    public void setLocalVar(Program program, int index, Program.ProgramValue value) {
    }

    /**
     * opGetDude (0x80BF): the player character. The object system models the
     * dude as a null holder for now.
     */
    @Override
    public Object opGetDude(Program program) {
        return null;
    }

    /**
     * op_cur_map_index (0x8101): the map the game is on. The map system is
     * not ported yet, so this is the map header's index of zero.
     */
    @Override
    public int curMapIndex(Program program) {
        return 0;
    }

    /**
     * op_script_action (0x80C7): the script's action. The script registry is
     * not ported yet, so this is the action carried by the program.
     */
    @Override
    public int scriptAction(Program program) {
        return program.scriptAction;
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
    public int critterAddTrait(Program program, Object object, int kind, int param, int value) {
        if (object == null) {
            return -1;
        }
        throw unsupported("critter_add_trait");
    }

    /**
     * op_party_member_obj (0x814B): the party member with a given proto id.
     */
    @Override
    public Object partyMemberObj(Program program, int protoId) {
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
    public Program.ProgramValue getGlobalVar(Program program, int variable) {
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
    public void setGlobalVar(Program program, int variable, Program.ProgramValue value) {
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
    public int objIsCarryingObj(Program program, Object object, int protoId) {
        if (object == null) {
            return 0;
        }

        return Inventory.objectGetCarriedQuantityByProtoId(
            (ObjTypes.GameObject) object, ProtoId.ofPid(protoId));
    }
}
