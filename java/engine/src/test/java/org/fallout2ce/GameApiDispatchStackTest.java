package org.fallout2ce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * The builtins the game hands to the scripts have to take the same number
 * of values off the stack as the original takes.
 *
 * <p>Otherwise the stack of a compiled script is left with the values on
 * it, and the script reads the wrong values afterwards. The numbers are
 * the ones the C++ original pops: {@code src/interpreter_extra.cc} for the
 * game builtins, {@code src/interpreter_lib.cc} for the six say-screen
 * opcodes (0x804D/4E/52/53/54/6B) that belong to the base interpreter.
 * Loop pops are unfolded (op_random pops 2, op_attack pops 8,
 * tile_in_tile_rect pops 5).
 */
class GameApiDispatchStackTest {

    /** The number of the values the original takes off the stack. */
    private static final Map<String, Integer> POPS = new LinkedHashMap<>();

    static {
        POPS.put("804D", 0); // opSayEnd
        POPS.put("804E", 0); // opSayStart
        POPS.put("8052", 2); // opSayOption
        POPS.put("8053", 2); // opSayReply
        POPS.put("8054", 2); // opSayMessage
        POPS.put("806B", 1); // opDisplay
        POPS.put("80A1", 1); // op_give_exp_points
        POPS.put("80A2", 1); // op_scr_return
        POPS.put("80A3", 1); // op_play_sfx
        POPS.put("80A4", 1); // op_obj_name
        POPS.put("80A6", 1); // op_get_pc_stat
        POPS.put("80A7", 3); // op_tile_contains_pid_obj
        POPS.put("80A8", 4); // op_set_map_start
        POPS.put("80A9", 4); // op_override_map_start
        POPS.put("80AA", 2); // op_has_skill
        POPS.put("80AB", 2); // op_using_skill
        POPS.put("80AC", 3); // op_roll_vs_skill
        POPS.put("80AE", 3); // op_do_check
        POPS.put("80AF", 1); // op_success
        POPS.put("80B0", 1); // op_critical
        POPS.put("80B2", 3); // op_mark_area_known
        POPS.put("80B4", 2); // op_random
        POPS.put("80B6", 3); // op_move_to
        POPS.put("80B7", 4); // op_create_object
        POPS.put("80B8", 1); // op_display_msg
        POPS.put("80B9", 0); // op_script_overrides
        POPS.put("80BA", 2); // op_obj_is_carrying_obj
        POPS.put("80BB", 3); // op_tile_contains_obj_pid
        POPS.put("80BC", 0); // op_self_obj
        POPS.put("80BD", 0); // op_source_obj
        POPS.put("80BE", 0); // op_target_obj
        POPS.put("80BF", 0); // opGetDude
        POPS.put("80C0", 0); // op_obj_being_used_with
        POPS.put("80C1", 1); // op_get_local_var
        POPS.put("80C2", 2); // op_set_local_var
        POPS.put("80C3", 1); // op_get_map_var
        POPS.put("80C4", 2); // op_set_map_var
        POPS.put("80C5", 1); // op_get_global_var
        POPS.put("80C6", 2); // op_set_global_var
        POPS.put("80C7", 0); // op_script_action
        POPS.put("80C8", 1); // op_obj_type
        POPS.put("80C9", 1); // op_item_subtype
        POPS.put("80CA", 2); // op_get_critter_stat
        POPS.put("80CB", 3); // op_set_critter_stat
        POPS.put("80CC", 1); // op_animate_stand_obj
        POPS.put("80CD", 1); // animate_stand_reverse_obj
        POPS.put("80CE", 3); // animate_move_obj_to_tile
        POPS.put("80CF", 5); // tile_in_tile_rect
        POPS.put("80D0", 8); // op_attack
        POPS.put("80D2", 2); // op_tile_distance
        POPS.put("80D3", 2); // op_tile_distance_objs
        POPS.put("80D4", 1); // op_tile_num
        POPS.put("80D5", 3); // op_tile_num_in_direction
        POPS.put("80D7", 1); // op_drop_obj
        POPS.put("80D8", 2); // op_add_obj_to_inven
        POPS.put("80D9", 2); // op_rm_obj_from_inven
        POPS.put("80DA", 2); // op_wield_obj_critter
        POPS.put("80DB", 1); // op_use_obj
        POPS.put("80DC", 2); // op_obj_can_see_obj
        POPS.put("80DE", 5); // op_start_gdialog
        POPS.put("80DF", 0); // op_end_gdialog
        POPS.put("80E1", 4); // op_metarule3
        POPS.put("80E3", 2); // op_set_obj_visibility
        POPS.put("80E4", 2); // op_load_map
        POPS.put("80E5", 3); // op_wm_area_set_pos
        POPS.put("80E6", 5); // op_set_exit_grids
        POPS.put("80E7", 1); // op_anim_busy
        POPS.put("80E8", 2); // op_critter_heal
        POPS.put("80E9", 1); // op_set_light_level
        POPS.put("80EA", 0); // op_game_time
        POPS.put("80EC", 1); // op_elevation
        POPS.put("80ED", 2); // op_kill_critter
        POPS.put("80EE", 2); // op_kill_critter_type
        POPS.put("80EF", 3); // op_critter_damage
        POPS.put("80F0", 3); // op_add_timer_event
        POPS.put("80F1", 1); // op_rm_timer_event
        POPS.put("80F2", 1); // op_game_ticks
        POPS.put("80F3", 3); // op_has_trait
        POPS.put("80F4", 1); // op_destroy_object
        POPS.put("80F5", 2); // op_obj_can_hear_obj
        POPS.put("80F6", 0); // op_game_time_hour
        POPS.put("80F7", 0); // op_fixed_param
        POPS.put("80F8", 1); // op_tile_is_visible
        POPS.put("80F9", 0); // op_dialogue_system_enter
        POPS.put("80FA", 0); // op_action_being_used
        POPS.put("80FB", 1); // critter_state
        POPS.put("80FC", 1); // op_game_time_advance
        POPS.put("80FD", 2); // op_radiation_inc
        POPS.put("80FE", 2); // op_radiation_dec
        POPS.put("80FF", 3); // critter_attempt_placement
        POPS.put("8100", 1); // op_obj_pid
        POPS.put("8101", 0); // op_cur_map_index
        POPS.put("8102", 4); // op_critter_add_trait
        POPS.put("8103", 4); // critter_rm_trait
        POPS.put("8104", 2); // op_proto_data
        POPS.put("8105", 2); // op_message_str
        POPS.put("8106", 2); // op_critter_inven_obj
        POPS.put("8107", 3); // op_obj_set_light_level
        POPS.put("8109", 3); // op_inven_cmds
        POPS.put("810A", 3); // op_float_msg
        POPS.put("810B", 2); // op_metarule
        POPS.put("810C", 4); // op_anim
        POPS.put("810D", 2); // op_obj_carrying_pid_obj
        POPS.put("810E", 2); // op_reg_anim_func
        POPS.put("810F", 3); // op_reg_anim_animate
        POPS.put("8110", 3); // op_reg_anim_animate_reverse
        POPS.put("8113", 3); // op_reg_anim_obj_move_to_tile
        POPS.put("8115", 1); // op_play_gmovie
        POPS.put("8116", 3); // op_add_mult_objs_to_inven
        POPS.put("8117", 3); // rm_mult_objs_from_inven
        POPS.put("8118", 0); // op_month
        POPS.put("8119", 0); // op_day
        POPS.put("811A", 3); // op_explosion
        POPS.put("811C", 0); // _op_gsay_start
        POPS.put("811D", 0); // _op_gsay_end
        POPS.put("811E", 2); // op_gsay_reply
        POPS.put("811F", 4); // op_gsay_option
        POPS.put("8120", 3); // op_gsay_message
        POPS.put("8121", 5); // op_giq_option
        POPS.put("8122", 2); // op_poison
        POPS.put("8123", 1); // op_get_poison
        POPS.put("8124", 1); // op_party_add
        POPS.put("8125", 1); // op_party_remove
        POPS.put("8126", 2); // op_reg_anim_animate_forever
        POPS.put("8127", 2); // op_critter_injure
        POPS.put("8128", 0); // op_is_in_combat
        POPS.put("8129", 1); // op_gdialog_barter
        POPS.put("812C", 0); // op_inven_unwield
        POPS.put("812D", 1); // op_obj_is_locked
        POPS.put("812E", 1); // op_obj_lock
        POPS.put("812F", 1); // op_obj_unlock
        POPS.put("8130", 1); // op_obj_is_open
        POPS.put("8131", 1); // op_obj_open
        POPS.put("8132", 1); // op_obj_close
        POPS.put("8133", 0); // op_game_ui_disable
        POPS.put("8134", 0); // op_game_ui_enable
        POPS.put("8136", 1); // op_gfade_out
        POPS.put("8137", 1); // op_gfade_in
        POPS.put("8138", 1); // op_item_caps_total
        POPS.put("8139", 2); // op_item_caps_adjust
        POPS.put("813B", 3); // op_reg_anim_play_sfx
        POPS.put("813C", 3); // op_critter_mod_skill
        POPS.put("813D", 3); // op_sfx_build_char_name
        POPS.put("8141", 4); // op_sfx_build_weapon_name
        POPS.put("8143", 2); // op_attack_setup
        POPS.put("8144", 2); // op_destroy_mult_objs
        POPS.put("8145", 2); // op_use_obj_on_obj
        POPS.put("8146", 0); // op_endgame_slideshow
        POPS.put("8147", 2); // op_move_obj_inven_to_obj
        POPS.put("8149", 1); // op_obj_art_fid
        POPS.put("814A", 1); // op_art_anim
        POPS.put("814B", 1); // op_party_member_obj
        POPS.put("814C", 2); // op_rotation_to_tile
        POPS.put("814D", 1); // op_jam_lock
        POPS.put("814E", 1); // op_gdialog_set_barter_mod
        POPS.put("8150", 1); // op_obj_on_screen
        POPS.put("8151", 1); // op_critter_is_fleeing
        POPS.put("8152", 2); // op_critter_set_flee_state
        POPS.put("8153", 0); // op_terminate_combat
        POPS.put("8154", 1); // op_debug_msg
        POPS.put("8155", 1); // op_critter_stop_attacking
    }

    @Test
    void theBuiltinsTakeTheSameValuesOffTheStack() throws Exception {
        Path source = Path.of("src", "main", "java", "org", "fallout2ce", "GameApiDispatch.java");
        assertTrue(Files.isRegularFile(source), "нет " + source);

        String[] blocks = Files.readString(source).split("// 0x");
        int checked = 0;
        int total = 0;
        for (String block : blocks) {
            Matcher matcher = Pattern.compile("^([0-9A-F]{4}) (\\w+)").matcher(block);
            if (!matcher.find()) {
                continue;
            }
            total++;
            String opcode = matcher.group(1);
            Integer expected = POPS.get(opcode);
            assertTrue(expected != null, "0x" + opcode + " " + matcher.group(2) + " нет в таблице POPS");
            int pops = countPops(block);
            assertEquals(expected.intValue(), pops,
                "0x" + opcode + " " + matcher.group(2) + " берёт со стека не то число значений");
            checked++;
        }
        assertEquals(POPS.size(), checked, "не все билд-ины найдены в диспетчере");
        assertEquals(total, checked, "не все блоки диспетчера покрыты таблицей POPS");
    }

    private static int countPops(String block) {
        int count = 0;
        int at = block.indexOf("programStackPop");
        while (at >= 0) {
            count++;
            at = block.indexOf("programStackPop", at + 1);
        }
        return count;
    }
}
