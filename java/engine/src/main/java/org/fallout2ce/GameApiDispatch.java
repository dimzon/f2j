package org.fallout2ce;

/**
 * Binds the script API to the VM's opcode table.
 *
 * <p>Each entry pops the builtin's arguments off the script's value stack
 * in the order the original pops them, calls the API, and pushes the
 * result back. The order matters: the compiler pushes arguments left to
 * right and the VM pops them right to left, so a builtin that takes two
 * arguments sees them reversed from the source.
 *
 * <p>Generated together with {@link GameApi}.
 */
final class GameApiDispatch {

    private GameApiDispatch() {
    }

    /** Installs the API's builtins into the opcode table. */
    static void install(GameApi api) {
        // 0x804D opSayEnd
        Program.INTERPRETER_OPCODE_HANDLERS[77] = program -> {
            if (!api.supports(77)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x804D.", 0x804D));
            }
            api.opSayEnd();
        };
        // 0x804E opSayStart
        Program.INTERPRETER_OPCODE_HANDLERS[78] = program -> {
            if (!api.supports(78)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x804E.", 0x804E));
            }
            api.opSayStart();
        };
        // 0x8052 opSayOption
        Program.INTERPRETER_OPCODE_HANDLERS[82] = program -> {
            if (!api.supports(82)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8052.", 0x8052));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            api.opSayOption(a1, a0);
        };
        // 0x8053 opSayReply
        Program.INTERPRETER_OPCODE_HANDLERS[83] = program -> {
            if (!api.supports(83)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8053.", 0x8053));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            api.opSayReply(a1, a0);
        };
        // 0x8054 opSayMessage
        Program.INTERPRETER_OPCODE_HANDLERS[84] = program -> {
            if (!api.supports(84)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8054.", 0x8054));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            api.opSayMessage(a1, a0);
        };
        // 0x806B opDisplay
        Program.INTERPRETER_OPCODE_HANDLERS[107] = program -> {
            if (!api.supports(107)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x806B.", 0x806B));
            }
            String a0 = Program.programStackPopString(program);
            api.opDisplay(a0);
        };
        // 0x80A1 op_give_exp_points
        Program.INTERPRETER_OPCODE_HANDLERS[161] = program -> {
            if (!api.supports(161)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80A1.", 0x80A1));
            }
            int a0 = Program.programStackPopInteger(program);
            api.giveExpPoints(a0);
        };
        // 0x80A2 op_scr_return
        Program.INTERPRETER_OPCODE_HANDLERS[162] = program -> {
            if (!api.supports(162)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80A2.", 0x80A2));
            }
            int a0 = Program.programStackPopInteger(program);
            api.scrReturn(a0);
        };
        // 0x80A3 op_play_sfx
        Program.INTERPRETER_OPCODE_HANDLERS[163] = program -> {
            if (!api.supports(163)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80A3.", 0x80A3));
            }
            String a0 = Program.programStackPopString(program);
            api.playSfx(a0);
        };
        // 0x80A4 op_obj_name
        Program.INTERPRETER_OPCODE_HANDLERS[164] = program -> {
            if (!api.supports(164)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80A4.", 0x80A4));
            }
            Object a0 = Program.programStackPopPointer(program);
            String result = api.objName(a0);
            Program.programStackPushString(program, result);
        };
        // 0x80A6 op_get_pc_stat
        Program.INTERPRETER_OPCODE_HANDLERS[166] = program -> {
            if (!api.supports(166)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80A6.", 0x80A6));
            }
            int a0 = Program.programStackPopInteger(program);
            int result = api.getPcStat(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80A7 op_tile_contains_pid_obj
        Program.INTERPRETER_OPCODE_HANDLERS[167] = program -> {
            if (!api.supports(167)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80A7.", 0x80A7));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            Object result = api.tileContainsPidObj(a2, a1, a0);
            Program.programStackPushPointer(program, result);
        };
        // 0x80A8 op_set_map_start
        Program.INTERPRETER_OPCODE_HANDLERS[168] = program -> {
            if (!api.supports(168)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80A8.", 0x80A8));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            int a3 = Program.programStackPopInteger(program);
            api.setMapStart(a3, a2, a1, a0);
        };
        // 0x80A9 op_override_map_start
        Program.INTERPRETER_OPCODE_HANDLERS[169] = program -> {
            if (!api.supports(169)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80A9.", 0x80A9));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            int a3 = Program.programStackPopInteger(program);
            api.overrideMapStart(a3, a2, a1, a0);
        };
        // 0x80AA op_has_skill
        Program.INTERPRETER_OPCODE_HANDLERS[170] = program -> {
            if (!api.supports(170)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80AA.", 0x80AA));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.hasSkill(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80AB op_using_skill
        Program.INTERPRETER_OPCODE_HANDLERS[171] = program -> {
            if (!api.supports(171)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80AB.", 0x80AB));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.usingSkill(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80AC op_roll_vs_skill
        Program.INTERPRETER_OPCODE_HANDLERS[172] = program -> {
            if (!api.supports(172)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80AC.", 0x80AC));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            int result = api.rollVsSkill(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80AE op_do_check
        Program.INTERPRETER_OPCODE_HANDLERS[174] = program -> {
            if (!api.supports(174)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80AE.", 0x80AE));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            int result = api.doCheck(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80AF op_success
        Program.INTERPRETER_OPCODE_HANDLERS[175] = program -> {
            if (!api.supports(175)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80AF.", 0x80AF));
            }
            int a0 = Program.programStackPopInteger(program);
            int result = api.success(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80B0 op_critical
        Program.INTERPRETER_OPCODE_HANDLERS[176] = program -> {
            if (!api.supports(176)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80B0.", 0x80B0));
            }
            int a0 = Program.programStackPopInteger(program);
            int result = api.critical(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80B2 op_mark_area_known
        Program.INTERPRETER_OPCODE_HANDLERS[178] = program -> {
            if (!api.supports(178)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80B2.", 0x80B2));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            api.markAreaKnown(a2, a1, a0);
        };
        // 0x80B4 op_random
        Program.INTERPRETER_OPCODE_HANDLERS[180] = program -> {
            if (!api.supports(180)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80B4.", 0x80B4));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int result = api.random(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80B6 op_move_to
        Program.INTERPRETER_OPCODE_HANDLERS[182] = program -> {
            if (!api.supports(182)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80B6.", 0x80B6));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            int result = api.moveTo(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80B7 op_create_object
        Program.INTERPRETER_OPCODE_HANDLERS[183] = program -> {
            if (!api.supports(183)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80B7.", 0x80B7));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            int a3 = Program.programStackPopInteger(program);
            Object result = api.createObject(a3, a2, a1, a0);
            Program.programStackPushPointer(program, result);
        };
        // 0x80B8 op_display_msg
        Program.INTERPRETER_OPCODE_HANDLERS[184] = program -> {
            if (!api.supports(184)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80B8.", 0x80B8));
            }
            String a0 = Program.programStackPopString(program);
            api.displayMsg(a0);
        };
        // 0x80B9 op_script_overrides
        Program.INTERPRETER_OPCODE_HANDLERS[185] = program -> {
            if (!api.supports(185)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80B9.", 0x80B9));
            }
            api.scriptOverrides();
        };
        // 0x80BA op_obj_is_carrying_obj
        Program.INTERPRETER_OPCODE_HANDLERS[186] = program -> {
            if (!api.supports(186)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80BA.", 0x80BA));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.objIsCarryingObj(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80BB op_tile_contains_obj_pid
        Program.INTERPRETER_OPCODE_HANDLERS[187] = program -> {
            if (!api.supports(187)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80BB.", 0x80BB));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            int result = api.tileContainsObjPid(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80BC op_self_obj
        Program.INTERPRETER_OPCODE_HANDLERS[188] = program -> {
            if (!api.supports(188)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80BC.", 0x80BC));
            }
            Object result = api.selfObj();
            Program.programStackPushPointer(program, result);
        };
        // 0x80BD op_source_obj
        Program.INTERPRETER_OPCODE_HANDLERS[189] = program -> {
            if (!api.supports(189)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80BD.", 0x80BD));
            }
            Object result = api.sourceObj();
            Program.programStackPushPointer(program, result);
        };
        // 0x80BE op_target_obj
        Program.INTERPRETER_OPCODE_HANDLERS[190] = program -> {
            if (!api.supports(190)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80BE.", 0x80BE));
            }
            Object result = api.targetObj();
            Program.programStackPushPointer(program, result);
        };
        // 0x80BF opGetDude
        Program.INTERPRETER_OPCODE_HANDLERS[191] = program -> {
            if (!api.supports(191)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80BF.", 0x80BF));
            }
            Object result = api.opGetDude();
            Program.programStackPushPointer(program, result);
        };
        // 0x80C0 op_obj_being_used_with
        Program.INTERPRETER_OPCODE_HANDLERS[192] = program -> {
            if (!api.supports(192)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C0.", 0x80C0));
            }
            Object result = api.objBeingUsedWith();
            Program.programStackPushPointer(program, result);
        };
        // 0x80C1 op_get_local_var
        Program.INTERPRETER_OPCODE_HANDLERS[193] = program -> {
            if (!api.supports(193)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C1.", 0x80C1));
            }
            int a0 = Program.programStackPopInteger(program);
            Program.ProgramValue result = api.getLocalVar(a0);
            Program.programStackPushValue(program, result);
        };
        // 0x80C2 op_set_local_var
        Program.INTERPRETER_OPCODE_HANDLERS[194] = program -> {
            if (!api.supports(194)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C2.", 0x80C2));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            int a1 = Program.programStackPopInteger(program);
            api.setLocalVar(a1, a0);
        };
        // 0x80C3 op_get_map_var
        Program.INTERPRETER_OPCODE_HANDLERS[195] = program -> {
            if (!api.supports(195)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C3.", 0x80C3));
            }
            int a0 = Program.programStackPopInteger(program);
            Program.ProgramValue result = api.getMapVar(a0);
            Program.programStackPushValue(program, result);
        };
        // 0x80C4 op_set_map_var
        Program.INTERPRETER_OPCODE_HANDLERS[196] = program -> {
            if (!api.supports(196)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C4.", 0x80C4));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            int a1 = Program.programStackPopInteger(program);
            api.setMapVar(a1, a0);
        };
        // 0x80C5 op_get_global_var
        Program.INTERPRETER_OPCODE_HANDLERS[197] = program -> {
            if (!api.supports(197)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C5.", 0x80C5));
            }
            int a0 = Program.programStackPopInteger(program);
            Program.ProgramValue result = api.getGlobalVar(a0);
            Program.programStackPushValue(program, result);
        };
        // 0x80C6 op_set_global_var
        Program.INTERPRETER_OPCODE_HANDLERS[198] = program -> {
            if (!api.supports(198)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C6.", 0x80C6));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            int a1 = Program.programStackPopInteger(program);
            api.setGlobalVar(a1, a0);
        };
        // 0x80C7 op_script_action
        Program.INTERPRETER_OPCODE_HANDLERS[199] = program -> {
            if (!api.supports(199)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C7.", 0x80C7));
            }
            int result = api.scriptAction();
            Program.programStackPushInteger(program, result);
        };
        // 0x80C8 op_obj_type
        Program.INTERPRETER_OPCODE_HANDLERS[200] = program -> {
            if (!api.supports(200)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C8.", 0x80C8));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.objType(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80C9 op_item_subtype
        Program.INTERPRETER_OPCODE_HANDLERS[201] = program -> {
            if (!api.supports(201)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80C9.", 0x80C9));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.itemSubtype(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80CA op_get_critter_stat
        Program.INTERPRETER_OPCODE_HANDLERS[202] = program -> {
            if (!api.supports(202)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80CA.", 0x80CA));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.getCritterStat(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80CB op_set_critter_stat
        Program.INTERPRETER_OPCODE_HANDLERS[203] = program -> {
            if (!api.supports(203)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80CB.", 0x80CB));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            int result = api.setCritterStat(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80CC op_animate_stand_obj
        Program.INTERPRETER_OPCODE_HANDLERS[204] = program -> {
            if (!api.supports(204)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80CC.", 0x80CC));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.animateStandObj(a0);
        };
        // 0x80CD animate_stand_reverse_obj
        Program.INTERPRETER_OPCODE_HANDLERS[205] = program -> {
            if (!api.supports(205)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80CD.", 0x80CD));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.animateStandReverseObj(a0);
        };
        // 0x80CE animate_move_obj_to_tile
        Program.INTERPRETER_OPCODE_HANDLERS[206] = program -> {
            if (!api.supports(206)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80CE.", 0x80CE));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            api.animateMoveObjToTile(a2, a1, a0);
        };
        // 0x80CF tile_in_tile_rect
        Program.INTERPRETER_OPCODE_HANDLERS[207] = program -> {
            if (!api.supports(207)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80CF.", 0x80CF));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            int a3 = Program.programStackPopInteger(program);
            int a4 = Program.programStackPopInteger(program);
            int result = api.tileInTileRect(a4, a3, a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80D0 op_attack
        Program.INTERPRETER_OPCODE_HANDLERS[208] = program -> {
            if (!api.supports(208)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80D0.", 0x80D0));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            int a3 = Program.programStackPopInteger(program);
            int a4 = Program.programStackPopInteger(program);
            int a5 = Program.programStackPopInteger(program);
            int a6 = Program.programStackPopInteger(program);
            Object a7 = Program.programStackPopPointer(program);
            api.attack(a7, a6, a5, a4, a3, a2, a1, a0);
        };
        // 0x80D2 op_tile_distance
        Program.INTERPRETER_OPCODE_HANDLERS[210] = program -> {
            if (!api.supports(210)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80D2.", 0x80D2));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int result = api.tileDistance(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80D3 op_tile_distance_objs
        Program.INTERPRETER_OPCODE_HANDLERS[211] = program -> {
            if (!api.supports(211)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80D3.", 0x80D3));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.tileDistanceObjs(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80D4 op_tile_num
        Program.INTERPRETER_OPCODE_HANDLERS[212] = program -> {
            if (!api.supports(212)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80D4.", 0x80D4));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.tileNum(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80D5 op_tile_num_in_direction
        Program.INTERPRETER_OPCODE_HANDLERS[213] = program -> {
            if (!api.supports(213)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80D5.", 0x80D5));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            int result = api.tileNumInDirection(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80D7 op_drop_obj
        Program.INTERPRETER_OPCODE_HANDLERS[215] = program -> {
            if (!api.supports(215)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80D7.", 0x80D7));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.dropObj(a0);
        };
        // 0x80D8 op_add_obj_to_inven
        Program.INTERPRETER_OPCODE_HANDLERS[216] = program -> {
            if (!api.supports(216)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80D8.", 0x80D8));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            api.addObjToInven(a1, a0);
        };
        // 0x80D9 op_rm_obj_from_inven
        Program.INTERPRETER_OPCODE_HANDLERS[217] = program -> {
            if (!api.supports(217)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80D9.", 0x80D9));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            api.rmObjFromInven(a1, a0);
        };
        // 0x80DA op_wield_obj_critter
        Program.INTERPRETER_OPCODE_HANDLERS[218] = program -> {
            if (!api.supports(218)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80DA.", 0x80DA));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            api.wieldObjCritter(a1, a0);
        };
        // 0x80DB op_use_obj
        Program.INTERPRETER_OPCODE_HANDLERS[219] = program -> {
            if (!api.supports(219)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80DB.", 0x80DB));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.useObj(a0);
        };
        // 0x80DC op_obj_can_see_obj
        Program.INTERPRETER_OPCODE_HANDLERS[220] = program -> {
            if (!api.supports(220)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80DC.", 0x80DC));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.objCanSeeObj(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80DE op_start_gdialog
        Program.INTERPRETER_OPCODE_HANDLERS[222] = program -> {
            if (!api.supports(222)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80DE.", 0x80DE));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            Object a3 = Program.programStackPopPointer(program);
            int a4 = Program.programStackPopInteger(program);
            api.startGdialog(a4, a3, a2, a1, a0);
        };
        // 0x80DF op_end_gdialog
        Program.INTERPRETER_OPCODE_HANDLERS[223] = program -> {
            if (!api.supports(223)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80DF.", 0x80DF));
            }
            api.endGdialog();
        };
        // 0x80E1 op_metarule3
        Program.INTERPRETER_OPCODE_HANDLERS[225] = program -> {
            if (!api.supports(225)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80E1.", 0x80E1));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            Program.ProgramValue a2 = Program.programStackPopValue(program);
            int a3 = Program.programStackPopInteger(program);
            Program.ProgramValue result = api.metarule3(a3, a2, a1, a0);
            Program.programStackPushValue(program, result);
        };
        // 0x80E3 op_set_obj_visibility
        Program.INTERPRETER_OPCODE_HANDLERS[227] = program -> {
            if (!api.supports(227)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80E3.", 0x80E3));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            api.setObjVisibility(a1, a0);
        };
        // 0x80E4 op_load_map
        Program.INTERPRETER_OPCODE_HANDLERS[228] = program -> {
            if (!api.supports(228)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80E4.", 0x80E4));
            }
            int a0 = Program.programStackPopInteger(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            api.loadMap(a1, a0);
        };
        // 0x80E5 op_wm_area_set_pos
        Program.INTERPRETER_OPCODE_HANDLERS[229] = program -> {
            if (!api.supports(229)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80E5.", 0x80E5));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            api.wmAreaSetPos(a2, a1, a0);
        };
        // 0x80E6 op_set_exit_grids
        Program.INTERPRETER_OPCODE_HANDLERS[230] = program -> {
            if (!api.supports(230)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80E6.", 0x80E6));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            int a3 = Program.programStackPopInteger(program);
            int a4 = Program.programStackPopInteger(program);
            api.setExitGrids(a4, a3, a2, a1, a0);
        };
        // 0x80E7 op_anim_busy
        Program.INTERPRETER_OPCODE_HANDLERS[231] = program -> {
            if (!api.supports(231)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80E7.", 0x80E7));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.animBusy(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80E8 op_critter_heal
        Program.INTERPRETER_OPCODE_HANDLERS[232] = program -> {
            if (!api.supports(232)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80E8.", 0x80E8));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.critterHeal(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80E9 op_set_light_level
        Program.INTERPRETER_OPCODE_HANDLERS[233] = program -> {
            if (!api.supports(233)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80E9.", 0x80E9));
            }
            int a0 = Program.programStackPopInteger(program);
            api.setLightLevel(a0);
        };
        // 0x80EA op_game_time
        Program.INTERPRETER_OPCODE_HANDLERS[234] = program -> {
            if (!api.supports(234)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80EA.", 0x80EA));
            }
            int result = api.gameTime();
            Program.programStackPushInteger(program, result);
        };
        // 0x80EC op_elevation
        Program.INTERPRETER_OPCODE_HANDLERS[236] = program -> {
            if (!api.supports(236)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80EC.", 0x80EC));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.elevation(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80ED op_kill_critter
        Program.INTERPRETER_OPCODE_HANDLERS[237] = program -> {
            if (!api.supports(237)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80ED.", 0x80ED));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            api.killCritter(a1, a0);
        };
        // 0x80EE op_kill_critter_type
        Program.INTERPRETER_OPCODE_HANDLERS[238] = program -> {
            if (!api.supports(238)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80EE.", 0x80EE));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            api.killCritterType(a1, a0);
        };
        // 0x80EF op_critter_damage
        Program.INTERPRETER_OPCODE_HANDLERS[239] = program -> {
            if (!api.supports(239)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80EF.", 0x80EF));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            api.critterDamage(a2, a1, a0);
        };
        // 0x80F0 op_add_timer_event
        Program.INTERPRETER_OPCODE_HANDLERS[240] = program -> {
            if (!api.supports(240)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F0.", 0x80F0));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            api.addTimerEvent(a2, a1, a0);
        };
        // 0x80F1 op_rm_timer_event
        Program.INTERPRETER_OPCODE_HANDLERS[241] = program -> {
            if (!api.supports(241)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F1.", 0x80F1));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.rmTimerEvent(a0);
        };
        // 0x80F2 op_game_ticks
        Program.INTERPRETER_OPCODE_HANDLERS[242] = program -> {
            if (!api.supports(242)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F2.", 0x80F2));
            }
            int a0 = Program.programStackPopInteger(program);
            int result = api.gameTicks(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80F3 op_has_trait
        Program.INTERPRETER_OPCODE_HANDLERS[243] = program -> {
            if (!api.supports(243)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F3.", 0x80F3));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            int a2 = Program.programStackPopInteger(program);
            int result = api.hasTrait(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80F4 op_destroy_object
        Program.INTERPRETER_OPCODE_HANDLERS[244] = program -> {
            if (!api.supports(244)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F4.", 0x80F4));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.destroyObject(a0);
        };
        // 0x80F5 op_obj_can_hear_obj
        Program.INTERPRETER_OPCODE_HANDLERS[245] = program -> {
            if (!api.supports(245)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F5.", 0x80F5));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.objCanHearObj(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80F6 op_game_time_hour
        Program.INTERPRETER_OPCODE_HANDLERS[246] = program -> {
            if (!api.supports(246)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F6.", 0x80F6));
            }
            int result = api.gameTimeHour();
            Program.programStackPushInteger(program, result);
        };
        // 0x80F7 op_fixed_param
        Program.INTERPRETER_OPCODE_HANDLERS[247] = program -> {
            if (!api.supports(247)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F7.", 0x80F7));
            }
            int result = api.fixedParam();
            Program.programStackPushInteger(program, result);
        };
        // 0x80F8 op_tile_is_visible
        Program.INTERPRETER_OPCODE_HANDLERS[248] = program -> {
            if (!api.supports(248)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F8.", 0x80F8));
            }
            int a0 = Program.programStackPopInteger(program);
            int result = api.tileIsVisible(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80F9 op_dialogue_system_enter
        Program.INTERPRETER_OPCODE_HANDLERS[249] = program -> {
            if (!api.supports(249)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80F9.", 0x80F9));
            }
            api.dialogueSystemEnter();
        };
        // 0x80FA op_action_being_used
        Program.INTERPRETER_OPCODE_HANDLERS[250] = program -> {
            if (!api.supports(250)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80FA.", 0x80FA));
            }
            int result = api.actionBeingUsed();
            Program.programStackPushInteger(program, result);
        };
        // 0x80FB critter_state
        Program.INTERPRETER_OPCODE_HANDLERS[251] = program -> {
            if (!api.supports(251)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80FB.", 0x80FB));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.critterState(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x80FC op_game_time_advance
        Program.INTERPRETER_OPCODE_HANDLERS[252] = program -> {
            if (!api.supports(252)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80FC.", 0x80FC));
            }
            int a0 = Program.programStackPopInteger(program);
            api.gameTimeAdvance(a0);
        };
        // 0x80FD op_radiation_inc
        Program.INTERPRETER_OPCODE_HANDLERS[253] = program -> {
            if (!api.supports(253)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80FD.", 0x80FD));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            api.radiationInc(a1, a0);
        };
        // 0x80FE op_radiation_dec
        Program.INTERPRETER_OPCODE_HANDLERS[254] = program -> {
            if (!api.supports(254)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80FE.", 0x80FE));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            api.radiationDec(a1, a0);
        };
        // 0x80FF critter_attempt_placement
        Program.INTERPRETER_OPCODE_HANDLERS[255] = program -> {
            if (!api.supports(255)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x80FF.", 0x80FF));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            int result = api.critterAttemptPlacement(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8100 op_obj_pid
        Program.INTERPRETER_OPCODE_HANDLERS[256] = program -> {
            if (!api.supports(256)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8100.", 0x8100));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.objPid(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8101 op_cur_map_index
        Program.INTERPRETER_OPCODE_HANDLERS[257] = program -> {
            if (!api.supports(257)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8101.", 0x8101));
            }
            int result = api.curMapIndex();
            Program.programStackPushInteger(program, result);
        };
        // 0x8102 op_critter_add_trait
        Program.INTERPRETER_OPCODE_HANDLERS[258] = program -> {
            if (!api.supports(258)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8102.", 0x8102));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            Object a3 = Program.programStackPopPointer(program);
            int result = api.critterAddTrait(a3, a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8103 critter_rm_trait
        Program.INTERPRETER_OPCODE_HANDLERS[259] = program -> {
            if (!api.supports(259)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8103.", 0x8103));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            Object a3 = Program.programStackPopPointer(program);
            int result = api.critterRmTrait(a3, a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8104 op_proto_data
        Program.INTERPRETER_OPCODE_HANDLERS[260] = program -> {
            if (!api.supports(260)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8104.", 0x8104));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Program.ProgramValue result = api.protoData(a1, a0);
            Program.programStackPushValue(program, result);
        };
        // 0x8105 op_message_str
        Program.INTERPRETER_OPCODE_HANDLERS[261] = program -> {
            if (!api.supports(261)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8105.", 0x8105));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            String result = api.messageStr(a1, a0);
            Program.programStackPushString(program, result);
        };
        // 0x8106 op_critter_inven_obj
        Program.INTERPRETER_OPCODE_HANDLERS[262] = program -> {
            if (!api.supports(262)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8106.", 0x8106));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            Program.ProgramValue result = api.critterInvenObj(a1, a0);
            Program.programStackPushValue(program, result);
        };
        // 0x8107 op_obj_set_light_level
        Program.INTERPRETER_OPCODE_HANDLERS[263] = program -> {
            if (!api.supports(263)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8107.", 0x8107));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            api.objSetLightLevel(a2, a1, a0);
        };
        // 0x8109 op_inven_cmds
        Program.INTERPRETER_OPCODE_HANDLERS[265] = program -> {
            if (!api.supports(265)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8109.", 0x8109));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            Object result = api.invenCmds(a2, a1, a0);
            Program.programStackPushPointer(program, result);
        };
        // 0x810A op_float_msg
        Program.INTERPRETER_OPCODE_HANDLERS[266] = program -> {
            if (!api.supports(266)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x810A.", 0x810A));
            }
            int a0 = Program.programStackPopInteger(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            api.floatMsg(a1, a0);
        };
        // 0x810B op_metarule
        Program.INTERPRETER_OPCODE_HANDLERS[267] = program -> {
            if (!api.supports(267)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x810B.", 0x810B));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            int a1 = Program.programStackPopInteger(program);
            int result = api.metarule(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x810C op_anim
        Program.INTERPRETER_OPCODE_HANDLERS[268] = program -> {
            if (!api.supports(268)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x810C.", 0x810C));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            Object a3 = Program.programStackPopPointer(program);
            api.anim(a3, a2, a1, a0);
        };
        // 0x810D op_obj_carrying_pid_obj
        Program.INTERPRETER_OPCODE_HANDLERS[269] = program -> {
            if (!api.supports(269)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x810D.", 0x810D));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            Object result = api.objCarryingPidObj(a1, a0);
            Program.programStackPushPointer(program, result);
        };
        // 0x810E op_reg_anim_func
        Program.INTERPRETER_OPCODE_HANDLERS[270] = program -> {
            if (!api.supports(270)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x810E.", 0x810E));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            int a1 = Program.programStackPopInteger(program);
            api.regAnimFunc(a1, a0);
        };
        // 0x810F op_reg_anim_animate
        Program.INTERPRETER_OPCODE_HANDLERS[271] = program -> {
            if (!api.supports(271)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x810F.", 0x810F));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            api.regAnimAnimate(a2, a1, a0);
        };
        // 0x8110 op_reg_anim_animate_reverse
        Program.INTERPRETER_OPCODE_HANDLERS[272] = program -> {
            if (!api.supports(272)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8110.", 0x8110));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            api.regAnimAnimateReverse(a2, a1, a0);
        };
        // 0x8113 op_reg_anim_obj_move_to_tile
        Program.INTERPRETER_OPCODE_HANDLERS[275] = program -> {
            if (!api.supports(275)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8113.", 0x8113));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            api.regAnimObjMoveToTile(a2, a1, a0);
        };
        // 0x8115 op_play_gmovie
        Program.INTERPRETER_OPCODE_HANDLERS[277] = program -> {
            if (!api.supports(277)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8115.", 0x8115));
            }
            int a0 = Program.programStackPopInteger(program);
            api.playGmovie(a0);
        };
        // 0x8116 op_add_mult_objs_to_inven
        Program.INTERPRETER_OPCODE_HANDLERS[278] = program -> {
            if (!api.supports(278)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8116.", 0x8116));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            Object a2 = Program.programStackPopPointer(program);
            api.addMultObjsToInven(a2, a1, a0);
        };
        // 0x8117 rm_mult_objs_from_inven
        Program.INTERPRETER_OPCODE_HANDLERS[279] = program -> {
            if (!api.supports(279)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8117.", 0x8117));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            Object a2 = Program.programStackPopPointer(program);
            int result = api.rmMultObjsFromInven(a2, a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8118 op_month
        Program.INTERPRETER_OPCODE_HANDLERS[280] = program -> {
            if (!api.supports(280)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8118.", 0x8118));
            }
            int result = api.month();
            Program.programStackPushInteger(program, result);
        };
        // 0x8119 op_day
        Program.INTERPRETER_OPCODE_HANDLERS[281] = program -> {
            if (!api.supports(281)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8119.", 0x8119));
            }
            int result = api.day();
            Program.programStackPushInteger(program, result);
        };
        // 0x811A op_explosion
        Program.INTERPRETER_OPCODE_HANDLERS[282] = program -> {
            if (!api.supports(282)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x811A.", 0x811A));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int a2 = Program.programStackPopInteger(program);
            api.explosion(a2, a1, a0);
        };
        // 0x811C _op_gsay_start
        Program.INTERPRETER_OPCODE_HANDLERS[284] = program -> {
            if (!api.supports(284)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x811C.", 0x811C));
            }
            api.opGsayStart();
        };
        // 0x811D _op_gsay_end
        Program.INTERPRETER_OPCODE_HANDLERS[285] = program -> {
            if (!api.supports(285)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x811D.", 0x811D));
            }
            api.opGsayEnd();
        };
        // 0x811E op_gsay_reply
        Program.INTERPRETER_OPCODE_HANDLERS[286] = program -> {
            if (!api.supports(286)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x811E.", 0x811E));
            }
            Program.ProgramValue a0 = Program.programStackPopValue(program);
            int a1 = Program.programStackPopInteger(program);
            api.gsayReply(a1, a0);
        };
        // 0x811F op_gsay_option
        Program.INTERPRETER_OPCODE_HANDLERS[287] = program -> {
            if (!api.supports(287)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x811F.", 0x811F));
            }
            int a0 = Program.programStackPopInteger(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            Program.ProgramValue a2 = Program.programStackPopValue(program);
            int a3 = Program.programStackPopInteger(program);
            api.gsayOption(a3, a2, a1, a0);
        };
        // 0x8120 op_gsay_message
        Program.INTERPRETER_OPCODE_HANDLERS[288] = program -> {
            if (!api.supports(288)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8120.", 0x8120));
            }
            int a0 = Program.programStackPopInteger(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            int a2 = Program.programStackPopInteger(program);
            api.gsayMessage(a2, a1, a0);
        };
        // 0x8121 op_giq_option
        Program.INTERPRETER_OPCODE_HANDLERS[289] = program -> {
            if (!api.supports(289)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8121.", 0x8121));
            }
            int a0 = Program.programStackPopInteger(program);
            Program.ProgramValue a1 = Program.programStackPopValue(program);
            Program.ProgramValue a2 = Program.programStackPopValue(program);
            int a3 = Program.programStackPopInteger(program);
            int a4 = Program.programStackPopInteger(program);
            api.giqOption(a4, a3, a2, a1, a0);
        };
        // 0x8122 op_poison
        Program.INTERPRETER_OPCODE_HANDLERS[290] = program -> {
            if (!api.supports(290)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8122.", 0x8122));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            api.poison(a1, a0);
        };
        // 0x8123 op_get_poison
        Program.INTERPRETER_OPCODE_HANDLERS[291] = program -> {
            if (!api.supports(291)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8123.", 0x8123));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.getPoison(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8124 op_party_add
        Program.INTERPRETER_OPCODE_HANDLERS[292] = program -> {
            if (!api.supports(292)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8124.", 0x8124));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.partyAdd(a0);
        };
        // 0x8125 op_party_remove
        Program.INTERPRETER_OPCODE_HANDLERS[293] = program -> {
            if (!api.supports(293)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8125.", 0x8125));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.partyRemove(a0);
        };
        // 0x8126 op_reg_anim_animate_forever
        Program.INTERPRETER_OPCODE_HANDLERS[294] = program -> {
            if (!api.supports(294)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8126.", 0x8126));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            api.regAnimAnimateForever(a1, a0);
        };
        // 0x8127 op_critter_injure
        Program.INTERPRETER_OPCODE_HANDLERS[295] = program -> {
            if (!api.supports(295)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8127.", 0x8127));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            api.critterInjure(a1, a0);
        };
        // 0x8128 op_is_in_combat
        Program.INTERPRETER_OPCODE_HANDLERS[296] = program -> {
            if (!api.supports(296)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8128.", 0x8128));
            }
            int result = api.isInCombat();
            Program.programStackPushInteger(program, result);
        };
        // 0x8129 op_gdialog_barter
        Program.INTERPRETER_OPCODE_HANDLERS[297] = program -> {
            if (!api.supports(297)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8129.", 0x8129));
            }
            int a0 = Program.programStackPopInteger(program);
            api.gdialogBarter(a0);
        };
        // 0x812C op_inven_unwield
        Program.INTERPRETER_OPCODE_HANDLERS[300] = program -> {
            if (!api.supports(300)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x812C.", 0x812C));
            }
            api.invenUnwield();
        };
        // 0x812D op_obj_is_locked
        Program.INTERPRETER_OPCODE_HANDLERS[301] = program -> {
            if (!api.supports(301)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x812D.", 0x812D));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.objIsLocked(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x812E op_obj_lock
        Program.INTERPRETER_OPCODE_HANDLERS[302] = program -> {
            if (!api.supports(302)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x812E.", 0x812E));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.objLock(a0);
        };
        // 0x812F op_obj_unlock
        Program.INTERPRETER_OPCODE_HANDLERS[303] = program -> {
            if (!api.supports(303)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x812F.", 0x812F));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.objUnlock(a0);
        };
        // 0x8130 op_obj_is_open
        Program.INTERPRETER_OPCODE_HANDLERS[304] = program -> {
            if (!api.supports(304)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8130.", 0x8130));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.objIsOpen(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8131 op_obj_open
        Program.INTERPRETER_OPCODE_HANDLERS[305] = program -> {
            if (!api.supports(305)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8131.", 0x8131));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.objOpen(a0);
        };
        // 0x8132 op_obj_close
        Program.INTERPRETER_OPCODE_HANDLERS[306] = program -> {
            if (!api.supports(306)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8132.", 0x8132));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.objClose(a0);
        };
        // 0x8133 op_game_ui_disable
        Program.INTERPRETER_OPCODE_HANDLERS[307] = program -> {
            if (!api.supports(307)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8133.", 0x8133));
            }
            api.gameUiDisable();
        };
        // 0x8134 op_game_ui_enable
        Program.INTERPRETER_OPCODE_HANDLERS[308] = program -> {
            if (!api.supports(308)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8134.", 0x8134));
            }
            api.gameUiEnable();
        };
        // 0x8136 op_gfade_out
        Program.INTERPRETER_OPCODE_HANDLERS[310] = program -> {
            if (!api.supports(310)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8136.", 0x8136));
            }
            int a0 = Program.programStackPopInteger(program);
            api.gfadeOut(a0);
        };
        // 0x8137 op_gfade_in
        Program.INTERPRETER_OPCODE_HANDLERS[311] = program -> {
            if (!api.supports(311)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8137.", 0x8137));
            }
            int a0 = Program.programStackPopInteger(program);
            api.gfadeIn(a0);
        };
        // 0x8138 op_item_caps_total
        Program.INTERPRETER_OPCODE_HANDLERS[312] = program -> {
            if (!api.supports(312)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8138.", 0x8138));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.itemCapsTotal(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8139 op_item_caps_adjust
        Program.INTERPRETER_OPCODE_HANDLERS[313] = program -> {
            if (!api.supports(313)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8139.", 0x8139));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.itemCapsAdjust(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x813B op_reg_anim_play_sfx
        Program.INTERPRETER_OPCODE_HANDLERS[315] = program -> {
            if (!api.supports(315)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x813B.", 0x813B));
            }
            int a0 = Program.programStackPopInteger(program);
            String a1 = Program.programStackPopString(program);
            Object a2 = Program.programStackPopPointer(program);
            api.regAnimPlaySfx(a2, a1, a0);
        };
        // 0x813C op_critter_mod_skill
        Program.INTERPRETER_OPCODE_HANDLERS[316] = program -> {
            if (!api.supports(316)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x813C.", 0x813C));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            api.critterModSkill(a2, a1, a0);
        };
        // 0x813D op_sfx_build_char_name
        Program.INTERPRETER_OPCODE_HANDLERS[317] = program -> {
            if (!api.supports(317)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x813D.", 0x813D));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            String result = api.sfxBuildCharName(a2, a1, a0);
            Program.programStackPushString(program, result);
        };
        // 0x8141 op_sfx_build_weapon_name
        Program.INTERPRETER_OPCODE_HANDLERS[321] = program -> {
            if (!api.supports(321)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8141.", 0x8141));
            }
            Object a0 = Program.programStackPopPointer(program);
            int a1 = Program.programStackPopInteger(program);
            Object a2 = Program.programStackPopPointer(program);
            int a3 = Program.programStackPopInteger(program);
            String result = api.sfxBuildWeaponName(a3, a2, a1, a0);
            Program.programStackPushString(program, result);
        };
        // 0x8143 op_attack_setup
        Program.INTERPRETER_OPCODE_HANDLERS[323] = program -> {
            if (!api.supports(323)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8143.", 0x8143));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            api.attackSetup(a1, a0);
        };
        // 0x8144 op_destroy_mult_objs
        Program.INTERPRETER_OPCODE_HANDLERS[324] = program -> {
            if (!api.supports(324)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8144.", 0x8144));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            int result = api.destroyMultObjs(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8145 op_use_obj_on_obj
        Program.INTERPRETER_OPCODE_HANDLERS[325] = program -> {
            if (!api.supports(325)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8145.", 0x8145));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            api.useObjOnObj(a1, a0);
        };
        // 0x8146 op_endgame_slideshow
        Program.INTERPRETER_OPCODE_HANDLERS[326] = program -> {
            if (!api.supports(326)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8146.", 0x8146));
            }
            api.endgameSlideshow();
        };
        // 0x8147 op_move_obj_inven_to_obj
        Program.INTERPRETER_OPCODE_HANDLERS[327] = program -> {
            if (!api.supports(327)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8147.", 0x8147));
            }
            Object a0 = Program.programStackPopPointer(program);
            Object a1 = Program.programStackPopPointer(program);
            api.moveObjInvenToObj(a1, a0);
        };
        // 0x8149 op_obj_art_fid
        Program.INTERPRETER_OPCODE_HANDLERS[329] = program -> {
            if (!api.supports(329)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8149.", 0x8149));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.objArtFid(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x814A op_art_anim
        Program.INTERPRETER_OPCODE_HANDLERS[330] = program -> {
            if (!api.supports(330)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x814A.", 0x814A));
            }
            int a0 = Program.programStackPopInteger(program);
            int result = api.artAnim(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x814B op_party_member_obj
        Program.INTERPRETER_OPCODE_HANDLERS[331] = program -> {
            if (!api.supports(331)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x814B.", 0x814B));
            }
            int a0 = Program.programStackPopInteger(program);
            Object result = api.partyMemberObj(a0);
            Program.programStackPushPointer(program, result);
        };
        // 0x814C op_rotation_to_tile
        Program.INTERPRETER_OPCODE_HANDLERS[332] = program -> {
            if (!api.supports(332)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x814C.", 0x814C));
            }
            int a0 = Program.programStackPopInteger(program);
            int a1 = Program.programStackPopInteger(program);
            int result = api.rotationToTile(a1, a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x814D op_jam_lock
        Program.INTERPRETER_OPCODE_HANDLERS[333] = program -> {
            if (!api.supports(333)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x814D.", 0x814D));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.jamLock(a0);
        };
        // 0x814E op_gdialog_set_barter_mod
        Program.INTERPRETER_OPCODE_HANDLERS[334] = program -> {
            if (!api.supports(334)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x814E.", 0x814E));
            }
            int a0 = Program.programStackPopInteger(program);
            api.gdialogSetBarterMod(a0);
        };
        // 0x8150 op_obj_on_screen
        Program.INTERPRETER_OPCODE_HANDLERS[336] = program -> {
            if (!api.supports(336)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8150.", 0x8150));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.objOnScreen(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8151 op_critter_is_fleeing
        Program.INTERPRETER_OPCODE_HANDLERS[337] = program -> {
            if (!api.supports(337)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8151.", 0x8151));
            }
            Object a0 = Program.programStackPopPointer(program);
            int result = api.critterIsFleeing(a0);
            Program.programStackPushInteger(program, result);
        };
        // 0x8152 op_critter_set_flee_state
        Program.INTERPRETER_OPCODE_HANDLERS[338] = program -> {
            if (!api.supports(338)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8152.", 0x8152));
            }
            int a0 = Program.programStackPopInteger(program);
            Object a1 = Program.programStackPopPointer(program);
            api.critterSetFleeState(a1, a0);
        };
        // 0x8153 op_terminate_combat
        Program.INTERPRETER_OPCODE_HANDLERS[339] = program -> {
            if (!api.supports(339)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8153.", 0x8153));
            }
            api.terminateCombat();
        };
        // 0x8154 op_debug_msg
        Program.INTERPRETER_OPCODE_HANDLERS[340] = program -> {
            if (!api.supports(340)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8154.", 0x8154));
            }
            String a0 = Program.programStackPopString(program);
            api.debugMsg(a0);
        };
        // 0x8155 op_critter_stop_attacking
        Program.INTERPRETER_OPCODE_HANDLERS[341] = program -> {
            if (!api.supports(341)) {
                Program.programFatalError(
                    String.format("Undefined opcode 0x8155.", 0x8155));
            }
            Object a0 = Program.programStackPopPointer(program);
            api.critterStopAttacking(a0);
        };
    }
}
