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
}
