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
 * the ones the C++ original pops in {@code src/interpreter_extra.cc}.
 */
class GameApiDispatchStackTest {

    /** The number of the values the original takes off the stack. */
    private static final Map<String, Integer> POPS = new LinkedHashMap<>();

    static {
        POPS.put("80A4", 1); // op_obj_name
        POPS.put("80AF", 1); // op_success
        POPS.put("80B0", 1); // op_critical
        POPS.put("80B7", 4); // op_create_object
        POPS.put("80C9", 1); // op_item_subtype
        POPS.put("80CA", 2); // op_get_critter_stat
        POPS.put("80CB", 3); // op_set_critter_stat
        POPS.put("80D2", 2); // op_tile_distance
        POPS.put("80D3", 2); // op_tile_distance_objs
        POPS.put("80D5", 3); // op_tile_num_in_direction
        POPS.put("80D8", 2); // op_add_obj_to_inven
        POPS.put("80D9", 2); // op_rm_obj_from_inven
        POPS.put("80F3", 3); // op_has_trait
        POPS.put("80F4", 1); // op_destroy_object
        POPS.put("8106", 2); // op_critter_inven_obj
        POPS.put("810A", 3); // op_float_msg
        POPS.put("810D", 2); // op_obj_carrying_pid_obj
        POPS.put("8116", 3); // op_add_mult_objs_to_inven
        POPS.put("8117", 3); // op_rm_mult_objs_from_inven
        POPS.put("8144", 2); // op_destroy_mult_objs
        POPS.put("814C", 2); // op_rotation_to_tile
    }

    @Test
    void theBuiltinsTakeTheSameValuesOffTheStack() throws Exception {
        Path source = Path.of("src", "main", "java", "org", "fallout2ce", "GameApiDispatch.java");
        assertTrue(Files.isRegularFile(source), "нет " + source);

        String[] blocks = Files.readString(source).split("// 0x");
        int checked = 0;
        for (String block : blocks) {
            Matcher matcher = Pattern.compile("^([0-9A-F]{4}) (\\w+)").matcher(block);
            if (!matcher.find()) {
                continue;
            }
            String opcode = matcher.group(1);
            Integer expected = POPS.get(opcode);
            if (expected == null) {
                continue;
            }
            int pops = countPops(block);
            assertEquals(expected.intValue(), pops,
                "0x" + opcode + " " + matcher.group(2) + " берёт со стека не то число значений");
            checked++;
        }
        assertEquals(POPS.size(), checked, "не все билд-ины найдены в диспетчере");
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
