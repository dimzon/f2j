package org.fallout2ce;

/**
 * Opcode names for the Fallout 2 script VM, indexed by dispatch slot
 * ({@code opcode & 0x3FF}).
 *
 * <p>The core opcodes are the interpreter's own, from
 * {@code src/interpreter.h}. The game's opcodes are the intlib's, and they are
 * registered with raw values in {@code src/interpreter_lib.cc},
 * {@code src/interpreter_extra.cc} and {@code src/sfall_opcodes.cc}; where the
 * source carries the original script-level name it is kept as the script name,
 * because that is the name the script compiler used.
 *
 * <p>Generated from the C sources by the script that accompanies the commit
 * that added this file.
 */
final class ScriptOpcodes {

    /** Dispatch slots: {@code opcode & 0x3FF}. */
    static final int SLOT_COUNT = 768;

    /** The C handler name, or null when the slot has no opcode. */
    static final String[] HANDLER = new String[SLOT_COUNT];

    /** The original script-level name, or null when the source has none. */
    static final String[] SCRIPT_NAME = new String[SLOT_COUNT];

    /** True for the interpreter's own opcodes, false for the game's. */
    static final boolean[] CORE = new boolean[SLOT_COUNT];

    static {
        HANDLER[0] = "NOOP";
        CORE[0] = true;
    }
    static {
        HANDLER[1] = "PUSH";
        CORE[1] = true;
    }
    static {
        HANDLER[2] = "ENTER_CRITICAL_SECTION";
        CORE[2] = true;
    }
    static {
        HANDLER[3] = "LEAVE_CRITICAL_SECTION";
        CORE[3] = true;
    }
    static {
        HANDLER[4] = "JUMP";
        CORE[4] = true;
    }
    static {
        HANDLER[5] = "CALL";
        CORE[5] = true;
    }
    static {
        HANDLER[6] = "CALL_AT";
        CORE[6] = true;
    }
    static {
        HANDLER[7] = "CALL_WHEN";
        CORE[7] = true;
    }
    static {
        HANDLER[8] = "CALLSTART";
        CORE[8] = true;
    }
    static {
        HANDLER[9] = "EXEC";
        CORE[9] = true;
    }
    static {
        HANDLER[10] = "SPAWN";
        CORE[10] = true;
    }
    static {
        HANDLER[11] = "FORK";
        CORE[11] = true;
    }
    static {
        HANDLER[12] = "A_TO_D";
        CORE[12] = true;
    }
    static {
        HANDLER[13] = "D_TO_A";
        CORE[13] = true;
    }
    static {
        HANDLER[14] = "EXIT";
        CORE[14] = true;
    }
    static {
        HANDLER[15] = "DETACH";
        CORE[15] = true;
    }
    static {
        HANDLER[16] = "EXIT_PROGRAM";
        CORE[16] = true;
    }
    static {
        HANDLER[17] = "STOP_PROGRAM";
        CORE[17] = true;
    }
    static {
        HANDLER[18] = "FETCH_GLOBAL";
        CORE[18] = true;
    }
    static {
        HANDLER[19] = "STORE_GLOBAL";
        CORE[19] = true;
    }
    static {
        HANDLER[20] = "FETCH_EXTERNAL";
        CORE[20] = true;
    }
    static {
        HANDLER[21] = "STORE_EXTERNAL";
        CORE[21] = true;
    }
    static {
        HANDLER[22] = "EXPORT_VARIABLE";
        CORE[22] = true;
    }
    static {
        HANDLER[23] = "EXPORT_PROCEDURE";
        CORE[23] = true;
    }
    static {
        HANDLER[24] = "SWAP";
        CORE[24] = true;
    }
    static {
        HANDLER[25] = "SWAPA";
        CORE[25] = true;
    }
    static {
        HANDLER[26] = "POP";
        CORE[26] = true;
    }
    static {
        HANDLER[27] = "DUP";
        CORE[27] = true;
    }
    static {
        HANDLER[28] = "POP_RETURN";
        CORE[28] = true;
    }
    static {
        HANDLER[29] = "POP_EXIT";
        CORE[29] = true;
    }
    static {
        HANDLER[30] = "POP_ADDRESS";
        CORE[30] = true;
    }
    static {
        HANDLER[31] = "POP_FLAGS";
        CORE[31] = true;
    }
    static {
        HANDLER[32] = "POP_FLAGS_RETURN";
        CORE[32] = true;
    }
    static {
        HANDLER[33] = "POP_FLAGS_EXIT";
        CORE[33] = true;
    }
    static {
        HANDLER[34] = "POP_FLAGS_RETURN_EXTERN";
        CORE[34] = true;
    }
    static {
        HANDLER[35] = "POP_FLAGS_EXIT_EXTERN";
        CORE[35] = true;
    }
    static {
        HANDLER[36] = "POP_FLAGS_RETURN_VAL_EXTERN";
        CORE[36] = true;
    }
    static {
        HANDLER[37] = "POP_FLAGS_RETURN_VAL_EXIT";
        CORE[37] = true;
    }
    static {
        HANDLER[38] = "POP_FLAGS_RETURN_VAL_EXIT_EXTERN";
        CORE[38] = true;
    }
    static {
        HANDLER[39] = "CHECK_PROCEDURE_ARGUMENT_COUNT";
        CORE[39] = true;
    }
    static {
        HANDLER[40] = "LOOKUP_PROCEDURE_BY_NAME";
        CORE[40] = true;
    }
    static {
        HANDLER[41] = "POP_BASE";
        CORE[41] = true;
    }
    static {
        HANDLER[42] = "POP_TO_BASE";
        CORE[42] = true;
    }
    static {
        HANDLER[43] = "PUSH_BASE";
        CORE[43] = true;
    }
    static {
        HANDLER[44] = "SET_GLOBAL";
        CORE[44] = true;
    }
    static {
        HANDLER[45] = "FETCH_PROCEDURE_ADDRESS";
        CORE[45] = true;
    }
    static {
        HANDLER[46] = "DUMP";
        CORE[46] = true;
    }
    static {
        HANDLER[47] = "IF";
        CORE[47] = true;
    }
    static {
        HANDLER[48] = "WHILE";
        CORE[48] = true;
    }
    static {
        HANDLER[49] = "STORE";
        CORE[49] = true;
    }
    static {
        HANDLER[50] = "FETCH";
        CORE[50] = true;
    }
    static {
        HANDLER[51] = "EQUAL";
        CORE[51] = true;
    }
    static {
        HANDLER[52] = "NOT_EQUAL";
        CORE[52] = true;
    }
    static {
        HANDLER[53] = "LESS_THAN_EQUAL";
        CORE[53] = true;
    }
    static {
        HANDLER[54] = "GREATER_THAN_EQUAL";
        CORE[54] = true;
    }
    static {
        HANDLER[55] = "LESS_THAN";
        CORE[55] = true;
    }
    static {
        HANDLER[56] = "GREATER_THAN";
        CORE[56] = true;
    }
    static {
        HANDLER[57] = "ADD";
        CORE[57] = true;
    }
    static {
        HANDLER[58] = "SUB";
        CORE[58] = true;
    }
    static {
        HANDLER[59] = "MUL";
        CORE[59] = true;
    }
    static {
        HANDLER[60] = "DIV";
        CORE[60] = true;
    }
    static {
        HANDLER[61] = "MOD";
        CORE[61] = true;
    }
    static {
        HANDLER[62] = "AND";
        CORE[62] = true;
    }
    static {
        HANDLER[63] = "OR";
        CORE[63] = true;
    }
    static {
        HANDLER[64] = "BITWISE_AND";
        CORE[64] = true;
    }
    static {
        HANDLER[65] = "BITWISE_OR";
        CORE[65] = true;
    }
    static {
        HANDLER[66] = "BITWISE_XOR";
        CORE[66] = true;
    }
    static {
        HANDLER[67] = "BITWISE_NOT";
        CORE[67] = true;
    }
    static {
        HANDLER[68] = "FLOOR";
        CORE[68] = true;
    }
    static {
        HANDLER[69] = "NOT";
        CORE[69] = true;
    }
    static {
        HANDLER[70] = "NEGATE";
        CORE[70] = true;
    }
    static {
        HANDLER[71] = "WAIT";
        CORE[71] = true;
    }
    static {
        HANDLER[72] = "CANCEL";
        CORE[72] = true;
    }
    static {
        HANDLER[73] = "CANCEL_ALL";
        CORE[73] = true;
    }
    static {
        HANDLER[74] = "START_CRITICAL";
        CORE[74] = true;
    }
    static {
        HANDLER[75] = "END_CRITICAL";
        CORE[75] = true;
    }
    static {
        HANDLER[76] = "opSayQuit";
        CORE[76] = false;
    }
    static {
        HANDLER[77] = "opSayEnd";
        CORE[77] = false;
    }
    static {
        HANDLER[78] = "opSayStart";
        CORE[78] = false;
    }
    static {
        HANDLER[79] = "opSayStartPos";
        CORE[79] = false;
    }
    static {
        HANDLER[80] = "opSayReplyTitle";
        CORE[80] = false;
    }
    static {
        HANDLER[81] = "opSayGoToReply";
        CORE[81] = false;
    }
    static {
        HANDLER[82] = "opSayOption";
        CORE[82] = false;
    }
    static {
        HANDLER[83] = "opSayReply";
        CORE[83] = false;
    }
    static {
        HANDLER[84] = "opSayMessage";
        CORE[84] = false;
    }
    static {
        HANDLER[85] = "opSayReplyWindow";
        CORE[85] = false;
    }
    static {
        HANDLER[86] = "opSayOptionWindow";
        CORE[86] = false;
    }
    static {
        HANDLER[87] = "opSayBorder";
        CORE[87] = false;
    }
    static {
        HANDLER[88] = "opSayScrollUp";
        CORE[88] = false;
    }
    static {
        HANDLER[89] = "opSayScrollDown";
        CORE[89] = false;
    }
    static {
        HANDLER[90] = "opSaySetSpacing";
        CORE[90] = false;
    }
    static {
        HANDLER[91] = "opSayOptionColor";
        CORE[91] = false;
    }
    static {
        HANDLER[92] = "opSayReplyColor";
        CORE[92] = false;
    }
    static {
        HANDLER[93] = "opSayRestart";
        CORE[93] = false;
    }
    static {
        HANDLER[94] = "opSayGetLastPos";
        CORE[94] = false;
    }
    static {
        HANDLER[95] = "opSayReplyFlags";
        CORE[95] = false;
    }
    static {
        HANDLER[96] = "opSayOptionFlags";
        CORE[96] = false;
    }
    static {
        HANDLER[97] = "opSayMessageTimeout";
        CORE[97] = false;
    }
    static {
        HANDLER[98] = "opCreateWin";
        CORE[98] = false;
    }
    static {
        HANDLER[99] = "opDeleteWin";
        CORE[99] = false;
    }
    static {
        HANDLER[100] = "opSelect";
        CORE[100] = false;
    }
    static {
        HANDLER[101] = "opResizeWin";
        CORE[101] = false;
    }
    static {
        HANDLER[102] = "opScaleWin";
        CORE[102] = false;
    }
    static {
        HANDLER[103] = "opShowWin";
        CORE[103] = false;
    }
    static {
        HANDLER[104] = "opFillWin";
        CORE[104] = false;
    }
    static {
        HANDLER[105] = "opFillRect";
        CORE[105] = false;
    }
    static {
        HANDLER[106] = "opFillWin3x3";
        CORE[106] = false;
    }
    static {
        HANDLER[107] = "opDisplay";
        CORE[107] = false;
    }
    static {
        HANDLER[108] = "opDisplayGfx";
        CORE[108] = false;
    }
    static {
        HANDLER[109] = "opDisplayRaw";
        CORE[109] = false;
    }
    static {
        HANDLER[110] = "opLoadPaletteTable";
        CORE[110] = false;
    }
    static {
        HANDLER[111] = "opFadeIn";
        CORE[111] = false;
    }
    static {
        HANDLER[112] = "opFadeOut";
        CORE[112] = false;
    }
    static {
        HANDLER[113] = "opGotoXY";
        CORE[113] = false;
    }
    static {
        HANDLER[114] = "opPrint";
        CORE[114] = false;
    }
    static {
        HANDLER[115] = "opFormat";
        CORE[115] = false;
    }
    static {
        HANDLER[116] = "opPrintRect";
        CORE[116] = false;
    }
    static {
        HANDLER[117] = "opSetFont";
        CORE[117] = false;
    }
    static {
        HANDLER[118] = "opSetTextFlags";
        CORE[118] = false;
    }
    static {
        HANDLER[119] = "opSetTextColor";
        CORE[119] = false;
    }
    static {
        HANDLER[120] = "opSetHighlightColor";
        CORE[120] = false;
    }
    static {
        HANDLER[121] = "opStopMovie";
        CORE[121] = false;
    }
    static {
        HANDLER[122] = "opPlayMovie";
        CORE[122] = false;
    }
    static {
        HANDLER[123] = "opSetMovieFlags";
        CORE[123] = false;
    }
    static {
        HANDLER[124] = "opPlayMovieRect";
        CORE[124] = false;
    }
    static {
        HANDLER[127] = "opAddRegion";
        CORE[127] = false;
    }
    static {
        HANDLER[128] = "opAddRegionFlag";
        CORE[128] = false;
    }
    static {
        HANDLER[129] = "opAddRegionProc";
        CORE[129] = false;
    }
    static {
        HANDLER[130] = "opAddRegionRightProc";
        CORE[130] = false;
    }
    static {
        HANDLER[131] = "opDeleteRegion";
        CORE[131] = false;
    }
    static {
        HANDLER[132] = "opActivateRegion";
        CORE[132] = false;
    }
    static {
        HANDLER[133] = "opCheckRegion";
        CORE[133] = false;
    }
    static {
        HANDLER[134] = "opAddButton";
        CORE[134] = false;
    }
    static {
        HANDLER[135] = "opAddButtonText";
        CORE[135] = false;
    }
    static {
        HANDLER[136] = "opAddButtonFlag";
        CORE[136] = false;
    }
    static {
        HANDLER[137] = "opAddButtonGfx";
        CORE[137] = false;
    }
    static {
        HANDLER[138] = "opAddButtonProc";
        CORE[138] = false;
    }
    static {
        HANDLER[139] = "opAddButtonRightProc";
        CORE[139] = false;
    }
    static {
        HANDLER[140] = "opDeleteButton";
        CORE[140] = false;
    }
    static {
        HANDLER[141] = "opHideMouse";
        CORE[141] = false;
    }
    static {
        HANDLER[142] = "opShowMouse";
        CORE[142] = false;
    }
    static {
        HANDLER[143] = "opMouseShape";
        CORE[143] = false;
    }
    static {
        HANDLER[144] = "opRefreshMouse";
        CORE[144] = false;
    }
    static {
        HANDLER[145] = "opSetGlobalMouseFunc";
        CORE[145] = false;
    }
    static {
        HANDLER[146] = "opAddNamedEvent";
        CORE[146] = false;
    }
    static {
        HANDLER[147] = "opAddNamedHandler";
        CORE[147] = false;
    }
    static {
        HANDLER[148] = "opClearNamed";
        CORE[148] = false;
    }
    static {
        HANDLER[149] = "opSignalNamed";
        CORE[149] = false;
    }
    static {
        HANDLER[150] = "opAddKey";
        CORE[150] = false;
    }
    static {
        HANDLER[151] = "opDeleteKey";
        CORE[151] = false;
    }
    static {
        HANDLER[152] = "opSoundPlay";
        CORE[152] = false;
    }
    static {
        HANDLER[153] = "opSoundPause";
        CORE[153] = false;
    }
    static {
        HANDLER[154] = "opSoundResume";
        CORE[154] = false;
    }
    static {
        HANDLER[155] = "opSoundStop";
        CORE[155] = false;
    }
    static {
        HANDLER[156] = "opSoundRewind";
        CORE[156] = false;
    }
    static {
        HANDLER[157] = "opSoundDelete";
        CORE[157] = false;
    }
    static {
        HANDLER[158] = "opSetOneOptPause";
        CORE[158] = false;
    }
    static {
        HANDLER[159] = "opSelectFileList";
        CORE[159] = false;
    }
    static {
        HANDLER[160] = "opTokenize";
        CORE[160] = false;
    }
    static {
        HANDLER[161] = "opGiveExpPoints";
        SCRIPT_NAME[161] = "op_give_exp_points";
        CORE[161] = false;
    }
    static {
        HANDLER[162] = "opScrReturn";
        SCRIPT_NAME[162] = "op_scr_return";
        CORE[162] = false;
    }
    static {
        HANDLER[163] = "opPlaySfx";
        SCRIPT_NAME[163] = "op_play_sfx";
        CORE[163] = false;
    }
    static {
        HANDLER[164] = "opGetObjectName";
        SCRIPT_NAME[164] = "op_obj_name";
        CORE[164] = false;
    }
    static {
        HANDLER[165] = "opSfxBuildOpenName";
        SCRIPT_NAME[165] = "op_sfx_build_open_name";
        CORE[165] = false;
    }
    static {
        HANDLER[166] = "opGetPcStat";
        SCRIPT_NAME[166] = "op_get_pc_stat";
        CORE[166] = false;
    }
    static {
        HANDLER[167] = "opTileGetObjectWithPid";
        SCRIPT_NAME[167] = "op_tile_contains_pid_obj";
        CORE[167] = false;
    }
    static {
        HANDLER[168] = "opSetMapStart";
        SCRIPT_NAME[168] = "op_set_map_start";
        CORE[168] = false;
    }
    static {
        HANDLER[169] = "opOverrideMapStart";
        SCRIPT_NAME[169] = "op_override_map_start";
        CORE[169] = false;
    }
    static {
        HANDLER[170] = "opHasSkill";
        SCRIPT_NAME[170] = "op_has_skill";
        CORE[170] = false;
    }
    static {
        HANDLER[171] = "opUsingSkill";
        SCRIPT_NAME[171] = "op_using_skill";
        CORE[171] = false;
    }
    static {
        HANDLER[172] = "opRollVsSkill";
        SCRIPT_NAME[172] = "op_roll_vs_skill";
        CORE[172] = false;
    }
    static {
        HANDLER[173] = "opSkillContest";
        SCRIPT_NAME[173] = "op_skill_contest";
        CORE[173] = false;
    }
    static {
        HANDLER[174] = "opDoCheck";
        SCRIPT_NAME[174] = "op_do_check";
        CORE[174] = false;
    }
    static {
        HANDLER[175] = "opSuccess";
        SCRIPT_NAME[175] = "op_success";
        CORE[175] = false;
    }
    static {
        HANDLER[176] = "opCritical";
        SCRIPT_NAME[176] = "op_critical";
        CORE[176] = false;
    }
    static {
        HANDLER[177] = "opHowMuch";
        SCRIPT_NAME[177] = "op_how_much";
        CORE[177] = false;
    }
    static {
        HANDLER[178] = "opMarkAreaKnown";
        SCRIPT_NAME[178] = "op_mark_area_known";
        CORE[178] = false;
    }
    static {
        HANDLER[179] = "opReactionInfluence";
        SCRIPT_NAME[179] = "op_reaction_influence";
        CORE[179] = false;
    }
    static {
        HANDLER[180] = "opRandom";
        SCRIPT_NAME[180] = "op_random";
        CORE[180] = false;
    }
    static {
        HANDLER[181] = "opRollDice";
        SCRIPT_NAME[181] = "op_roll_dice";
        CORE[181] = false;
    }
    static {
        HANDLER[182] = "opMoveTo";
        SCRIPT_NAME[182] = "op_move_to";
        CORE[182] = false;
    }
    static {
        HANDLER[183] = "opCreateObject";
        SCRIPT_NAME[183] = "op_create_object";
        CORE[183] = false;
    }
    static {
        HANDLER[184] = "opDisplayMsg";
        SCRIPT_NAME[184] = "op_display_msg";
        CORE[184] = false;
    }
    static {
        HANDLER[185] = "opScriptOverrides";
        SCRIPT_NAME[185] = "op_script_overrides";
        CORE[185] = false;
    }
    static {
        HANDLER[186] = "opObjectIsCarryingObjectWithPid";
        SCRIPT_NAME[186] = "op_obj_is_carrying_obj";
        CORE[186] = false;
    }
    static {
        HANDLER[187] = "opTileContainsObjectWithPid";
        SCRIPT_NAME[187] = "op_tile_contains_obj_pid";
        CORE[187] = false;
    }
    static {
        HANDLER[188] = "opGetSelf";
        SCRIPT_NAME[188] = "op_self_obj";
        CORE[188] = false;
    }
    static {
        HANDLER[189] = "opGetSource";
        SCRIPT_NAME[189] = "op_source_obj";
        CORE[189] = false;
    }
    static {
        HANDLER[190] = "opGetTarget";
        SCRIPT_NAME[190] = "op_target_obj";
        CORE[190] = false;
    }
    static {
        HANDLER[191] = "opGetDude";
        CORE[191] = false;
    }
    static {
        HANDLER[192] = "opGetObjectBeingUsed";
        SCRIPT_NAME[192] = "op_obj_being_used_with";
        CORE[192] = false;
    }
    static {
        HANDLER[193] = "opGetLocalVar";
        SCRIPT_NAME[193] = "op_get_local_var";
        CORE[193] = false;
    }
    static {
        HANDLER[194] = "opSetLocalVar";
        SCRIPT_NAME[194] = "op_set_local_var";
        CORE[194] = false;
    }
    static {
        HANDLER[195] = "opGetMapVar";
        SCRIPT_NAME[195] = "op_get_map_var";
        CORE[195] = false;
    }
    static {
        HANDLER[196] = "opSetMapVar";
        SCRIPT_NAME[196] = "op_set_map_var";
        CORE[196] = false;
    }
    static {
        HANDLER[197] = "opGetGlobalVar";
        SCRIPT_NAME[197] = "op_get_global_var";
        CORE[197] = false;
    }
    static {
        HANDLER[198] = "opSetGlobalVar";
        SCRIPT_NAME[198] = "op_set_global_var";
        CORE[198] = false;
    }
    static {
        HANDLER[199] = "opGetScriptAction";
        SCRIPT_NAME[199] = "op_script_action";
        CORE[199] = false;
    }
    static {
        HANDLER[200] = "opGetObjectType";
        SCRIPT_NAME[200] = "op_obj_type";
        CORE[200] = false;
    }
    static {
        HANDLER[201] = "opGetItemType";
        SCRIPT_NAME[201] = "op_item_subtype";
        CORE[201] = false;
    }
    static {
        HANDLER[202] = "opGetCritterStat";
        SCRIPT_NAME[202] = "op_get_critter_stat";
        CORE[202] = false;
    }
    static {
        HANDLER[203] = "opSetCritterStat";
        SCRIPT_NAME[203] = "op_set_critter_stat";
        CORE[203] = false;
    }
    static {
        HANDLER[204] = "opAnimateStand";
        SCRIPT_NAME[204] = "op_animate_stand_obj";
        CORE[204] = false;
    }
    static {
        HANDLER[205] = "opAnimateStandReverse";
        SCRIPT_NAME[205] = "animate_stand_reverse_obj";
        CORE[205] = false;
    }
    static {
        HANDLER[206] = "opAnimateMoveObjectToTile";
        SCRIPT_NAME[206] = "animate_move_obj_to_tile";
        CORE[206] = false;
    }
    static {
        HANDLER[207] = "opTileInTileRect";
        SCRIPT_NAME[207] = "tile_in_tile_rect";
        CORE[207] = false;
    }
    static {
        HANDLER[208] = "opAttackComplex";
        SCRIPT_NAME[208] = "op_attack";
        CORE[208] = false;
    }
    static {
        HANDLER[209] = "opMakeDayTime";
        SCRIPT_NAME[209] = "op_make_daytime";
        CORE[209] = false;
    }
    static {
        HANDLER[210] = "opTileDistanceBetween";
        SCRIPT_NAME[210] = "op_tile_distance";
        CORE[210] = false;
    }
    static {
        HANDLER[211] = "opTileDistanceBetweenObjects";
        SCRIPT_NAME[211] = "op_tile_distance_objs";
        CORE[211] = false;
    }
    static {
        HANDLER[212] = "opGetObjectTile";
        SCRIPT_NAME[212] = "op_tile_num";
        CORE[212] = false;
    }
    static {
        HANDLER[213] = "opGetTileInDirection";
        SCRIPT_NAME[213] = "op_tile_num_in_direction";
        CORE[213] = false;
    }
    static {
        HANDLER[214] = "opPickup";
        SCRIPT_NAME[214] = "op_pickup_obj";
        CORE[214] = false;
    }
    static {
        HANDLER[215] = "opDrop";
        SCRIPT_NAME[215] = "op_drop_obj";
        CORE[215] = false;
    }
    static {
        HANDLER[216] = "opAddObjectToInventory";
        SCRIPT_NAME[216] = "op_add_obj_to_inven";
        CORE[216] = false;
    }
    static {
        HANDLER[217] = "opRemoveObjectFromInventory";
        SCRIPT_NAME[217] = "op_rm_obj_from_inven";
        CORE[217] = false;
    }
    static {
        HANDLER[218] = "opWieldItem";
        SCRIPT_NAME[218] = "op_wield_obj_critter";
        CORE[218] = false;
    }
    static {
        HANDLER[219] = "opUseObject";
        SCRIPT_NAME[219] = "op_use_obj";
        CORE[219] = false;
    }
    static {
        HANDLER[220] = "opObjectCanSeeObject";
        SCRIPT_NAME[220] = "op_obj_can_see_obj";
        CORE[220] = false;
    }
    static {
        HANDLER[221] = "opAttackComplex";
        SCRIPT_NAME[221] = "op_attack";
        CORE[221] = false;
    }
    static {
        HANDLER[222] = "opStartGameDialog";
        SCRIPT_NAME[222] = "op_start_gdialog";
        CORE[222] = false;
    }
    static {
        HANDLER[223] = "opEndGameDialog";
        SCRIPT_NAME[223] = "op_end_gdialog";
        CORE[223] = false;
    }
    static {
        HANDLER[224] = "opGameDialogReaction";
        SCRIPT_NAME[224] = "op_dialogue_reaction";
        CORE[224] = false;
    }
    static {
        HANDLER[225] = "opMetarule3";
        SCRIPT_NAME[225] = "op_metarule3";
        CORE[225] = false;
    }
    static {
        HANDLER[226] = "opSetMapMusic";
        SCRIPT_NAME[226] = "op_set_map_music";
        CORE[226] = false;
    }
    static {
        HANDLER[227] = "opSetObjectVisibility";
        SCRIPT_NAME[227] = "op_set_obj_visibility";
        CORE[227] = false;
    }
    static {
        HANDLER[228] = "opLoadMap";
        SCRIPT_NAME[228] = "op_load_map";
        CORE[228] = false;
    }
    static {
        HANDLER[229] = "opWorldmapCitySetPos";
        SCRIPT_NAME[229] = "op_wm_area_set_pos";
        CORE[229] = false;
    }
    static {
        HANDLER[230] = "opSetExitGrids";
        SCRIPT_NAME[230] = "op_set_exit_grids";
        CORE[230] = false;
    }
    static {
        HANDLER[231] = "opAnimBusy";
        SCRIPT_NAME[231] = "op_anim_busy";
        CORE[231] = false;
    }
    static {
        HANDLER[232] = "opCritterHeal";
        SCRIPT_NAME[232] = "op_critter_heal";
        CORE[232] = false;
    }
    static {
        HANDLER[233] = "opSetLightLevel";
        SCRIPT_NAME[233] = "op_set_light_level";
        CORE[233] = false;
    }
    static {
        HANDLER[234] = "opGetGameTime";
        SCRIPT_NAME[234] = "op_game_time";
        CORE[234] = false;
    }
    static {
        HANDLER[235] = "opGetGameTimeInSeconds";
        SCRIPT_NAME[235] = "op_game_time";
        CORE[235] = false;
    }
    static {
        HANDLER[236] = "opGetObjectElevation";
        SCRIPT_NAME[236] = "op_elevation";
        CORE[236] = false;
    }
    static {
        HANDLER[237] = "opKillCritter";
        SCRIPT_NAME[237] = "op_kill_critter";
        CORE[237] = false;
    }
    static {
        HANDLER[238] = "opKillCritterType";
        SCRIPT_NAME[238] = "op_kill_critter_type";
        CORE[238] = false;
    }
    static {
        HANDLER[239] = "opCritterDamage";
        SCRIPT_NAME[239] = "op_critter_damage";
        CORE[239] = false;
    }
    static {
        HANDLER[240] = "opAddTimerEvent";
        SCRIPT_NAME[240] = "op_add_timer_event";
        CORE[240] = false;
    }
    static {
        HANDLER[241] = "opRemoveTimerEvent";
        SCRIPT_NAME[241] = "op_rm_timer_event";
        CORE[241] = false;
    }
    static {
        HANDLER[242] = "opGameTicks";
        SCRIPT_NAME[242] = "op_game_ticks";
        CORE[242] = false;
    }
    static {
        HANDLER[243] = "opHasTrait";
        SCRIPT_NAME[243] = "op_has_trait";
        CORE[243] = false;
    }
    static {
        HANDLER[244] = "opDestroyObject";
        SCRIPT_NAME[244] = "op_destroy_object";
        CORE[244] = false;
    }
    static {
        HANDLER[245] = "opObjectCanHearObject";
        SCRIPT_NAME[245] = "op_obj_can_hear_obj";
        CORE[245] = false;
    }
    static {
        HANDLER[246] = "opGameTimeHour";
        SCRIPT_NAME[246] = "op_game_time_hour";
        CORE[246] = false;
    }
    static {
        HANDLER[247] = "opGetFixedParam";
        SCRIPT_NAME[247] = "op_fixed_param";
        CORE[247] = false;
    }
    static {
        HANDLER[248] = "opTileIsVisible";
        SCRIPT_NAME[248] = "op_tile_is_visible";
        CORE[248] = false;
    }
    static {
        HANDLER[249] = "opGameDialogSystemEnter";
        SCRIPT_NAME[249] = "op_dialogue_system_enter";
        CORE[249] = false;
    }
    static {
        HANDLER[250] = "opGetActionBeingUsed";
        SCRIPT_NAME[250] = "op_action_being_used";
        CORE[250] = false;
    }
    static {
        HANDLER[251] = "opGetCritterState";
        SCRIPT_NAME[251] = "critter_state";
        CORE[251] = false;
    }
    static {
        HANDLER[252] = "opGameTimeAdvance";
        SCRIPT_NAME[252] = "op_game_time_advance";
        CORE[252] = false;
    }
    static {
        HANDLER[253] = "opRadiationIncrease";
        SCRIPT_NAME[253] = "op_radiation_inc";
        CORE[253] = false;
    }
    static {
        HANDLER[254] = "opRadiationDecrease";
        SCRIPT_NAME[254] = "op_radiation_dec";
        CORE[254] = false;
    }
    static {
        HANDLER[255] = "opCritterAttemptPlacement";
        SCRIPT_NAME[255] = "critter_attempt_placement";
        CORE[255] = false;
    }
    static {
        HANDLER[256] = "opGetObjectPid";
        SCRIPT_NAME[256] = "op_obj_pid";
        CORE[256] = false;
    }
    static {
        HANDLER[257] = "opGetCurrentMap";
        SCRIPT_NAME[257] = "op_cur_map_index";
        CORE[257] = false;
    }
    static {
        HANDLER[258] = "opCritterAddTrait";
        SCRIPT_NAME[258] = "op_critter_add_trait";
        CORE[258] = false;
    }
    static {
        HANDLER[259] = "opCritterRemoveTrait";
        SCRIPT_NAME[259] = "critter_rm_trait";
        CORE[259] = false;
    }
    static {
        HANDLER[260] = "opGetProtoData";
        SCRIPT_NAME[260] = "op_proto_data";
        CORE[260] = false;
    }
    static {
        HANDLER[261] = "opGetMessageString";
        SCRIPT_NAME[261] = "op_message_str";
        CORE[261] = false;
    }
    static {
        HANDLER[262] = "opCritterGetInventoryObject";
        SCRIPT_NAME[262] = "op_critter_inven_obj";
        CORE[262] = false;
    }
    static {
        HANDLER[263] = "opSetObjectLightLevel";
        SCRIPT_NAME[263] = "op_obj_set_light_level";
        CORE[263] = false;
    }
    static {
        HANDLER[264] = "opWorldmap";
        SCRIPT_NAME[264] = "op_scripts_request_world_map";
        CORE[264] = false;
    }
    static {
        HANDLER[265] = "_op_inven_cmds";
        SCRIPT_NAME[265] = "op_inven_cmds";
        CORE[265] = false;
    }
    static {
        HANDLER[266] = "opFloatMessage";
        SCRIPT_NAME[266] = "op_float_msg";
        CORE[266] = false;
    }
    static {
        HANDLER[267] = "opMetarule";
        SCRIPT_NAME[267] = "op_metarule";
        CORE[267] = false;
    }
    static {
        HANDLER[268] = "opAnim";
        SCRIPT_NAME[268] = "op_anim";
        CORE[268] = false;
    }
    static {
        HANDLER[269] = "opObjectCarryingObjectByPid";
        SCRIPT_NAME[269] = "op_obj_carrying_pid_obj";
        CORE[269] = false;
    }
    static {
        HANDLER[270] = "opRegAnimFunc";
        SCRIPT_NAME[270] = "op_reg_anim_func";
        CORE[270] = false;
    }
    static {
        HANDLER[271] = "opRegAnimAnimate";
        SCRIPT_NAME[271] = "op_reg_anim_animate";
        CORE[271] = false;
    }
    static {
        HANDLER[272] = "opRegAnimAnimateReverse";
        SCRIPT_NAME[272] = "op_reg_anim_animate_reverse";
        CORE[272] = false;
    }
    static {
        HANDLER[273] = "opRegAnimObjectMoveToObject";
        SCRIPT_NAME[273] = "op_reg_anim_obj_move_to_obj";
        CORE[273] = false;
    }
    static {
        HANDLER[274] = "opRegAnimObjectRunToObject";
        SCRIPT_NAME[274] = "op_reg_anim_obj_run_to_obj";
        CORE[274] = false;
    }
    static {
        HANDLER[275] = "opRegAnimObjectMoveToTile";
        SCRIPT_NAME[275] = "op_reg_anim_obj_move_to_tile";
        CORE[275] = false;
    }
    static {
        HANDLER[276] = "opRegAnimObjectRunToTile";
        SCRIPT_NAME[276] = "op_reg_anim_obj_run_to_tile";
        CORE[276] = false;
    }
    static {
        HANDLER[277] = "opPlayGameMovie";
        SCRIPT_NAME[277] = "op_play_gmovie";
        CORE[277] = false;
    }
    static {
        HANDLER[278] = "opAddMultipleObjectsToInventory";
        SCRIPT_NAME[278] = "op_add_mult_objs_to_inven";
        CORE[278] = false;
    }
    static {
        HANDLER[279] = "opRemoveMultipleObjectsFromInventory";
        SCRIPT_NAME[279] = "rm_mult_objs_from_inven";
        CORE[279] = false;
    }
    static {
        HANDLER[280] = "opGetMonth";
        SCRIPT_NAME[280] = "op_month";
        CORE[280] = false;
    }
    static {
        HANDLER[281] = "opGetDay";
        SCRIPT_NAME[281] = "op_day";
        CORE[281] = false;
    }
    static {
        HANDLER[282] = "opExplosion";
        SCRIPT_NAME[282] = "op_explosion";
        CORE[282] = false;
    }
    static {
        HANDLER[283] = "opGetDaysSinceLastVisit";
        SCRIPT_NAME[283] = "op_days_since_visited";
        CORE[283] = false;
    }
    static {
        HANDLER[284] = "_op_gsay_start";
        CORE[284] = false;
    }
    static {
        HANDLER[285] = "_op_gsay_end";
        CORE[285] = false;
    }
    static {
        HANDLER[286] = "_op_gsay_reply";
        SCRIPT_NAME[286] = "op_gsay_reply";
        CORE[286] = false;
    }
    static {
        HANDLER[287] = "_op_gsay_option";
        SCRIPT_NAME[287] = "op_gsay_option";
        CORE[287] = false;
    }
    static {
        HANDLER[288] = "_op_gsay_message";
        SCRIPT_NAME[288] = "op_gsay_message";
        CORE[288] = false;
    }
    static {
        HANDLER[289] = "_op_giq_option";
        SCRIPT_NAME[289] = "op_giq_option";
        CORE[289] = false;
    }
    static {
        HANDLER[290] = "opPoison";
        SCRIPT_NAME[290] = "op_poison";
        CORE[290] = false;
    }
    static {
        HANDLER[291] = "opGetPoison";
        SCRIPT_NAME[291] = "op_get_poison";
        CORE[291] = false;
    }
    static {
        HANDLER[292] = "opPartyAdd";
        SCRIPT_NAME[292] = "op_party_add";
        CORE[292] = false;
    }
    static {
        HANDLER[293] = "opPartyRemove";
        SCRIPT_NAME[293] = "op_party_remove";
        CORE[293] = false;
    }
    static {
        HANDLER[294] = "opRegAnimAnimateForever";
        SCRIPT_NAME[294] = "op_reg_anim_animate_forever";
        CORE[294] = false;
    }
    static {
        HANDLER[295] = "opCritterInjure";
        SCRIPT_NAME[295] = "op_critter_injure";
        CORE[295] = false;
    }
    static {
        HANDLER[296] = "opCombatIsInitialized";
        SCRIPT_NAME[296] = "op_is_in_combat";
        CORE[296] = false;
    }
    static {
        HANDLER[297] = "_op_gdialog_barter";
        SCRIPT_NAME[297] = "op_gdialog_barter";
        CORE[297] = false;
    }
    static {
        HANDLER[298] = "opGetGameDifficulty";
        SCRIPT_NAME[298] = "op_game_difficulty";
        CORE[298] = false;
    }
    static {
        HANDLER[299] = "opGetRunningBurningGuy";
        SCRIPT_NAME[299] = "op_running_burning_guy";
        CORE[299] = false;
    }
    static {
        HANDLER[300] = "_op_inven_unwield";
        SCRIPT_NAME[300] = "op_inven_unwield";
        CORE[300] = false;
    }
    static {
        HANDLER[301] = "opObjectIsLocked";
        SCRIPT_NAME[301] = "op_obj_is_locked";
        CORE[301] = false;
    }
    static {
        HANDLER[302] = "opObjectLock";
        SCRIPT_NAME[302] = "op_obj_lock";
        CORE[302] = false;
    }
    static {
        HANDLER[303] = "opObjectUnlock";
        SCRIPT_NAME[303] = "op_obj_unlock";
        CORE[303] = false;
    }
    static {
        HANDLER[304] = "opObjectIsOpen";
        SCRIPT_NAME[304] = "op_obj_is_open";
        CORE[304] = false;
    }
    static {
        HANDLER[305] = "opObjectOpen";
        SCRIPT_NAME[305] = "op_obj_open";
        CORE[305] = false;
    }
    static {
        HANDLER[306] = "opObjectClose";
        SCRIPT_NAME[306] = "op_obj_close";
        CORE[306] = false;
    }
    static {
        HANDLER[307] = "opGameUiDisable";
        SCRIPT_NAME[307] = "op_game_ui_disable";
        CORE[307] = false;
    }
    static {
        HANDLER[308] = "opGameUiEnable";
        SCRIPT_NAME[308] = "op_game_ui_enable";
        CORE[308] = false;
    }
    static {
        HANDLER[309] = "opGameUiIsDisabled";
        SCRIPT_NAME[309] = "op_game_ui_is_disabled";
        CORE[309] = false;
    }
    static {
        HANDLER[310] = "opGameFadeOut";
        SCRIPT_NAME[310] = "op_gfade_out";
        CORE[310] = false;
    }
    static {
        HANDLER[311] = "opGameFadeIn";
        SCRIPT_NAME[311] = "op_gfade_in";
        CORE[311] = false;
    }
    static {
        HANDLER[312] = "opItemCapsTotal";
        SCRIPT_NAME[312] = "op_item_caps_total";
        CORE[312] = false;
    }
    static {
        HANDLER[313] = "opItemCapsAdjust";
        SCRIPT_NAME[313] = "op_item_caps_adjust";
        CORE[313] = false;
    }
    static {
        HANDLER[314] = "_op_anim_action_frame";
        SCRIPT_NAME[314] = "op_anim_action_frame";
        CORE[314] = false;
    }
    static {
        HANDLER[315] = "opRegAnimPlaySfx";
        SCRIPT_NAME[315] = "op_reg_anim_play_sfx";
        CORE[315] = false;
    }
    static {
        HANDLER[316] = "opCritterModifySkill";
        SCRIPT_NAME[316] = "op_critter_mod_skill";
        CORE[316] = false;
    }
    static {
        HANDLER[317] = "opSfxBuildCharName";
        SCRIPT_NAME[317] = "op_sfx_build_char_name";
        CORE[317] = false;
    }
    static {
        HANDLER[318] = "opSfxBuildAmbientName";
        SCRIPT_NAME[318] = "op_sfx_build_ambient_name";
        CORE[318] = false;
    }
    static {
        HANDLER[319] = "opSfxBuildInterfaceName";
        SCRIPT_NAME[319] = "op_sfx_build_interface_name";
        CORE[319] = false;
    }
    static {
        HANDLER[320] = "opSfxBuildItemName";
        SCRIPT_NAME[320] = "op_sfx_build_item_name";
        CORE[320] = false;
    }
    static {
        HANDLER[321] = "opSfxBuildWeaponName";
        SCRIPT_NAME[321] = "op_sfx_build_weapon_name";
        CORE[321] = false;
    }
    static {
        HANDLER[322] = "opSfxBuildSceneryName";
        SCRIPT_NAME[322] = "op_sfx_build_scenery_name";
        CORE[322] = false;
    }
    static {
        HANDLER[323] = "opAttackSetup";
        SCRIPT_NAME[323] = "op_attack_setup";
        CORE[323] = false;
    }
    static {
        HANDLER[324] = "opDestroyMultipleObjects";
        SCRIPT_NAME[324] = "op_destroy_mult_objs";
        CORE[324] = false;
    }
    static {
        HANDLER[325] = "opUseObjectOnObject";
        SCRIPT_NAME[325] = "op_use_obj_on_obj";
        CORE[325] = false;
    }
    static {
        HANDLER[326] = "opEndgameSlideshow";
        SCRIPT_NAME[326] = "op_endgame_slideshow";
        CORE[326] = false;
    }
    static {
        HANDLER[327] = "opMoveObjectInventoryToObject";
        SCRIPT_NAME[327] = "op_move_obj_inven_to_obj";
        CORE[327] = false;
    }
    static {
        HANDLER[328] = "opEndgameMovie";
        SCRIPT_NAME[328] = "op_endgame_movie";
        CORE[328] = false;
    }
    static {
        HANDLER[329] = "opGetObjectFid";
        SCRIPT_NAME[329] = "op_obj_art_fid";
        CORE[329] = false;
    }
    static {
        HANDLER[330] = "opGetFidAnim";
        SCRIPT_NAME[330] = "op_art_anim";
        CORE[330] = false;
    }
    static {
        HANDLER[331] = "opGetPartyMember";
        SCRIPT_NAME[331] = "op_party_member_obj";
        CORE[331] = false;
    }
    static {
        HANDLER[332] = "opGetRotationToTile";
        SCRIPT_NAME[332] = "op_rotation_to_tile";
        CORE[332] = false;
    }
    static {
        HANDLER[333] = "opJamLock";
        SCRIPT_NAME[333] = "op_jam_lock";
        CORE[333] = false;
    }
    static {
        HANDLER[334] = "opGameDialogSetBarterMod";
        SCRIPT_NAME[334] = "op_gdialog_set_barter_mod";
        CORE[334] = false;
    }
    static {
        HANDLER[335] = "opGetCombatDifficulty";
        SCRIPT_NAME[335] = "op_combat_difficulty";
        CORE[335] = false;
    }
    static {
        HANDLER[336] = "opObjectOnScreen";
        SCRIPT_NAME[336] = "op_obj_on_screen";
        CORE[336] = false;
    }
    static {
        HANDLER[337] = "opCritterIsFleeing";
        SCRIPT_NAME[337] = "op_critter_is_fleeing";
        CORE[337] = false;
    }
    static {
        HANDLER[338] = "opCritterSetFleeState";
        SCRIPT_NAME[338] = "op_critter_set_flee_state";
        CORE[338] = false;
    }
    static {
        HANDLER[339] = "opTerminateCombat";
        SCRIPT_NAME[339] = "op_terminate_combat";
        CORE[339] = false;
    }
    static {
        HANDLER[340] = "opDebugMessage";
        SCRIPT_NAME[340] = "op_debug_msg";
        CORE[340] = false;
    }
    static {
        HANDLER[341] = "opCritterStopAttacking";
        SCRIPT_NAME[341] = "op_critter_stop_attacking";
        CORE[341] = false;
    }
    static {
        HANDLER[342] = "op_read_byte";
        SCRIPT_NAME[342] = "read_byte";
        CORE[342] = false;
    }
    static {
        HANDLER[346] = "op_set_pc_base_stat";
        CORE[346] = false;
    }
    static {
        HANDLER[347] = "op_set_pc_bonus_stat";
        CORE[347] = false;
    }
    static {
        HANDLER[348] = "op_get_pc_base_stat";
        CORE[348] = false;
    }
    static {
        HANDLER[349] = "op_get_pc_bonus_stat";
        CORE[349] = false;
    }
    static {
        HANDLER[350] = "op_set_critter_base_stat";
        CORE[350] = false;
    }
    static {
        HANDLER[351] = "op_set_critter_extra_stat";
        CORE[351] = false;
    }
    static {
        HANDLER[352] = "op_get_critter_base_stat";
        SCRIPT_NAME[352] = "get_critter_base_stat";
        CORE[352] = false;
    }
    static {
        HANDLER[353] = "op_get_critter_extra_stat";
        SCRIPT_NAME[353] = "get_critter_extra_stat";
        CORE[353] = false;
    }
    static {
        HANDLER[354] = "op_tap_key";
        SCRIPT_NAME[354] = "tap_key";
        CORE[354] = false;
    }
    static {
        HANDLER[355] = "op_get_year";
        SCRIPT_NAME[355] = "get_year";
        CORE[355] = false;
    }
    static {
        HANDLER[356] = "op_game_loaded";
        SCRIPT_NAME[356] = "game_loaded";
        CORE[356] = false;
    }
    static {
        HANDLER[362] = "op_set_global_script_repeat";
        CORE[362] = false;
    }
    static {
        HANDLER[364] = "op_key_pressed";
        CORE[364] = false;
    }
    static {
        HANDLER[368] = "op_in_world_map";
        SCRIPT_NAME[368] = "in_world_map";
        CORE[368] = false;
    }
    static {
        HANDLER[369] = "op_force_encounter";
        SCRIPT_NAME[369] = "force_encounter";
        CORE[369] = false;
    }
    static {
        HANDLER[370] = "op_set_world_map_pos";
        SCRIPT_NAME[370] = "set_world_map_pos";
        CORE[370] = false;
    }
    static {
        HANDLER[371] = "op_get_world_map_x_pos";
        SCRIPT_NAME[371] = "get_world_map_x_pos";
        CORE[371] = false;
    }
    static {
        HANDLER[372] = "op_get_world_map_y_pos";
        SCRIPT_NAME[372] = "get_world_map_y_pos";
        CORE[372] = false;
    }
    static {
        HANDLER[375] = "op_set_movie_path";
        SCRIPT_NAME[375] = "set_movie_path";
        CORE[375] = false;
    }
    static {
        HANDLER[376] = "op_set_perk_image";
        SCRIPT_NAME[376] = "set_perk_image";
        CORE[376] = false;
    }
    static {
        HANDLER[377] = "op_set_perk_ranks";
        SCRIPT_NAME[377] = "set_perk_ranks";
        CORE[377] = false;
    }
    static {
        HANDLER[378] = "op_set_perk_level";
        CORE[378] = false;
    }
    static {
        HANDLER[379] = "op_set_perk_stat";
        CORE[379] = false;
    }
    static {
        HANDLER[380] = "op_set_perk_stat_mag";
        CORE[380] = false;
    }
    static {
        HANDLER[381] = "op_set_perk_skill1";
        CORE[381] = false;
    }
    static {
        HANDLER[382] = "op_set_perk_skill1_mag";
        CORE[382] = false;
    }
    static {
        HANDLER[383] = "op_set_perk_type";
        CORE[383] = false;
    }
    static {
        HANDLER[384] = "op_set_perk_skill2";
        SCRIPT_NAME[384] = "set_perk_skill2";
        CORE[384] = false;
    }
    static {
        HANDLER[385] = "op_set_perk_skill2_mag";
        SCRIPT_NAME[385] = "set_perk_skill2_mag";
        CORE[385] = false;
    }
    static {
        HANDLER[386] = "op_set_perk_str";
        SCRIPT_NAME[386] = "set_perk_str";
        CORE[386] = false;
    }
    static {
        HANDLER[387] = "op_set_perk_per";
        SCRIPT_NAME[387] = "set_perk_per";
        CORE[387] = false;
    }
    static {
        HANDLER[388] = "op_set_perk_end";
        SCRIPT_NAME[388] = "set_perk_end";
        CORE[388] = false;
    }
    static {
        HANDLER[389] = "op_set_perk_chr";
        SCRIPT_NAME[389] = "set_perk_chr";
        CORE[389] = false;
    }
    static {
        HANDLER[390] = "op_set_perk_int";
        SCRIPT_NAME[390] = "set_perk_int";
        CORE[390] = false;
    }
    static {
        HANDLER[391] = "op_set_perk_agl";
        SCRIPT_NAME[391] = "set_perk_agl";
        CORE[391] = false;
    }
    static {
        HANDLER[392] = "op_set_perk_lck";
        SCRIPT_NAME[392] = "set_perk_lck";
        CORE[392] = false;
    }
    static {
        HANDLER[393] = "op_set_perk_name";
        SCRIPT_NAME[393] = "set_perk_name";
        CORE[393] = false;
    }
    static {
        HANDLER[394] = "op_set_perk_desc";
        CORE[394] = false;
    }
    static {
        HANDLER[398] = "op_get_perk_owed";
        CORE[398] = false;
    }
    static {
        HANDLER[399] = "op_set_perk_owed";
        CORE[399] = false;
    }
    static {
        HANDLER[401] = "op_get_critter_current_ap";
        SCRIPT_NAME[401] = "get_critter_current_ap";
        CORE[401] = false;
    }
    static {
        HANDLER[402] = "op_set_critter_current_ap";
        SCRIPT_NAME[402] = "set_critter_current_ap";
        CORE[402] = false;
    }
    static {
        HANDLER[403] = "op_active_hand";
        SCRIPT_NAME[403] = "active_hand";
        CORE[403] = false;
    }
    static {
        HANDLER[404] = "op_toggle_active_hand";
        SCRIPT_NAME[404] = "toggle_active_hand";
        CORE[404] = false;
    }
    static {
        HANDLER[411] = "op_set_global_script_type";
        CORE[411] = false;
    }
    static {
        HANDLER[413] = "op_set_sfall_global";
        CORE[413] = false;
    }
    static {
        HANDLER[414] = "op_get_sfall_global_int";
        CORE[414] = false;
    }
    static {
        HANDLER[415] = "op_get_sfall_global_float";
        CORE[415] = false;
    }
    static {
        HANDLER[418] = "op_set_skill_max";
        CORE[418] = false;
    }
    static {
        HANDLER[428] = "op_get_ini_setting";
        CORE[428] = false;
    }
    static {
        HANDLER[431] = "op_get_game_mode";
        CORE[431] = false;
    }
    static {
        HANDLER[435] = "op_get_uptime";
        CORE[435] = false;
    }
    static {
        HANDLER[436] = "op_set_stat_max";
        CORE[436] = false;
    }
    static {
        HANDLER[437] = "op_set_stat_min";
        CORE[437] = false;
    }
    static {
        HANDLER[438] = "op_set_car_current_town";
        CORE[438] = false;
    }
    static {
        HANDLER[439] = "op_set_pc_stat_max";
        CORE[439] = false;
    }
    static {
        HANDLER[440] = "op_set_pc_stat_min";
        CORE[440] = false;
    }
    static {
        HANDLER[441] = "op_set_npc_stat_max";
        CORE[441] = false;
    }
    static {
        HANDLER[442] = "op_set_npc_stat_min";
        CORE[442] = false;
    }
    static {
        HANDLER[444] = "op_set_fake_trait";
        CORE[444] = false;
    }
    static {
        HANDLER[476] = "op_show_iface_tag";
        CORE[476] = false;
    }
    static {
        HANDLER[477] = "op_hide_iface_tag";
        CORE[477] = false;
    }
    static {
        HANDLER[478] = "op_is_iface_tag_active";
        CORE[478] = false;
    }
    static {
        HANDLER[479] = "op_get_bodypart_hit_modifier";
        CORE[479] = false;
    }
    static {
        HANDLER[480] = "op_set_bodypart_hit_modifier";
        CORE[480] = false;
    }
    static {
        HANDLER[481] = "op_set_critical_table";
        CORE[481] = false;
    }
    static {
        HANDLER[482] = "op_get_critical_table";
        CORE[482] = false;
    }
    static {
        HANDLER[483] = "op_reset_critical_table";
        CORE[483] = false;
    }
    static {
        HANDLER[486] = "op_set_unspent_ap_bonus";
        CORE[486] = false;
    }
    static {
        HANDLER[487] = "op_get_unspent_ap_bonus";
        CORE[487] = false;
    }
    static {
        HANDLER[488] = "op_set_unspent_ap_perk_bonus";
        CORE[488] = false;
    }
    static {
        HANDLER[489] = "op_get_unspent_ap_perk_bonus";
        CORE[489] = false;
    }
    static {
        HANDLER[491] = "op_get_ini_string";
        CORE[491] = false;
    }
    static {
        HANDLER[492] = "op_sqrt";
        CORE[492] = false;
    }
    static {
        HANDLER[493] = "op_abs";
        CORE[493] = false;
    }
    static {
        HANDLER[494] = "op_sin";
        CORE[494] = false;
    }
    static {
        HANDLER[495] = "op_cos";
        CORE[495] = false;
    }
    static {
        HANDLER[496] = "op_tan";
        CORE[496] = false;
    }
    static {
        HANDLER[497] = "op_arctan";
        CORE[497] = false;
    }
    static {
        HANDLER[499] = "op_remove_script";
        CORE[499] = false;
    }
    static {
        HANDLER[500] = "op_set_script";
        CORE[500] = false;
    }
    static {
        HANDLER[501] = "op_get_script";
        CORE[501] = false;
    }
    static {
        HANDLER[516] = "op_get_proto_data";
        SCRIPT_NAME[516] = "get_proto_data";
        CORE[516] = false;
    }
    static {
        HANDLER[517] = "op_set_proto_data";
        SCRIPT_NAME[517] = "set_proto_data";
        CORE[517] = false;
    }
    static {
        HANDLER[518] = "op_set_self";
        SCRIPT_NAME[518] = "set_self";
        CORE[518] = false;
    }
    static {
        HANDLER[519] = "op_register_hook";
        SCRIPT_NAME[519] = "register_hook";
        CORE[519] = false;
    }
    static {
        HANDLER[525] = "op_list_begin";
        CORE[525] = false;
    }
    static {
        HANDLER[526] = "op_list_next";
        CORE[526] = false;
    }
    static {
        HANDLER[527] = "op_list_end";
        CORE[527] = false;
    }
    static {
        HANDLER[528] = "op_get_version_major";
        SCRIPT_NAME[528] = "sfall_ver_major";
        CORE[528] = false;
    }
    static {
        HANDLER[529] = "op_get_version_minor";
        SCRIPT_NAME[529] = "sfall_ver_minor";
        CORE[529] = false;
    }
    static {
        HANDLER[530] = "op_get_version_patch";
        SCRIPT_NAME[530] = "sfall_ver_build";
        CORE[530] = false;
    }
    static {
        HANDLER[532] = "op_set_hero_race";
        SCRIPT_NAME[532] = "set_hero_race";
        CORE[532] = false;
    }
    static {
        HANDLER[533] = "op_set_hero_style";
        SCRIPT_NAME[533] = "set_hero_style";
        CORE[533] = false;
    }
    static {
        HANDLER[534] = "op_set_critter_burst_disable";
        SCRIPT_NAME[534] = "set_critter_burst_disable";
        CORE[534] = false;
    }
    static {
        HANDLER[535] = "op_get_weapon_ammo_pid";
        SCRIPT_NAME[535] = "get_weapon_ammo_pid";
        CORE[535] = false;
    }
    static {
        HANDLER[536] = "op_set_weapon_ammo_pid";
        SCRIPT_NAME[536] = "set_weapon_ammo_pid";
        CORE[536] = false;
    }
    static {
        HANDLER[537] = "op_get_weapon_ammo_count";
        SCRIPT_NAME[537] = "get_weapon_ammo_count";
        CORE[537] = false;
    }
    static {
        HANDLER[538] = "op_set_weapon_ammo_count";
        CORE[538] = false;
    }
    static {
        HANDLER[540] = "op_get_mouse_x";
        CORE[540] = false;
    }
    static {
        HANDLER[541] = "op_get_mouse_y";
        CORE[541] = false;
    }
    static {
        HANDLER[542] = "op_get_mouse_buttons";
        CORE[542] = false;
    }
    static {
        HANDLER[543] = "op_get_window_under_mouse";
        CORE[543] = false;
    }
    static {
        HANDLER[544] = "op_get_screen_width";
        SCRIPT_NAME[544] = "get_screen_width";
        CORE[544] = false;
    }
    static {
        HANDLER[545] = "op_get_screen_height";
        SCRIPT_NAME[545] = "get_screen_height";
        CORE[545] = false;
    }
    static {
        HANDLER[548] = "op_create_message_window";
        SCRIPT_NAME[548] = "create_message_window";
        CORE[548] = false;
    }
    static {
        HANDLER[550] = "op_get_light_level";
        SCRIPT_NAME[550] = "get_light_level";
        CORE[550] = false;
    }
    static {
        HANDLER[551] = "op_refresh_pc_art";
        SCRIPT_NAME[551] = "refresh_pc_art";
        CORE[551] = false;
    }
    static {
        HANDLER[552] = "op_get_attack_type";
        SCRIPT_NAME[552] = "get_attack_type";
        CORE[552] = false;
    }
    static {
        HANDLER[553] = "op_force_encounter_with_flags";
        SCRIPT_NAME[553] = "force_encounter_with_flags";
        CORE[553] = false;
    }
    static {
        HANDLER[554] = "op_set_map_time_multi";
        CORE[554] = false;
    }
    static {
        HANDLER[555] = "op_play_sfall_sound";
        CORE[555] = false;
    }
    static {
        HANDLER[556] = "op_stop_sfall_sound";
        CORE[556] = false;
    }
    static {
        HANDLER[557] = "op_create_array";
        CORE[557] = false;
    }
    static {
        HANDLER[558] = "op_set_array";
        CORE[558] = false;
    }
    static {
        HANDLER[559] = "op_get_array";
        CORE[559] = false;
    }
    static {
        HANDLER[560] = "op_free_array";
        SCRIPT_NAME[560] = "free_array";
        CORE[560] = false;
    }
    static {
        HANDLER[561] = "op_len_array";
        SCRIPT_NAME[561] = "len_array";
        CORE[561] = false;
    }
    static {
        HANDLER[562] = "op_resize_array";
        SCRIPT_NAME[562] = "resize_array";
        CORE[562] = false;
    }
    static {
        HANDLER[563] = "op_temp_array";
        SCRIPT_NAME[563] = "temp_array";
        CORE[563] = false;
    }
    static {
        HANDLER[564] = "op_fix_array";
        SCRIPT_NAME[564] = "fix_array";
        CORE[564] = false;
    }
    static {
        HANDLER[565] = "op_string_split";
        SCRIPT_NAME[565] = "string_split";
        CORE[565] = false;
    }
    static {
        HANDLER[566] = "op_list_as_array";
        SCRIPT_NAME[566] = "list_as_array";
        CORE[566] = false;
    }
    static {
        HANDLER[567] = "op_parse_int";
        SCRIPT_NAME[567] = "atoi";
        CORE[567] = false;
    }
    static {
        HANDLER[568] = "op_atof";
        SCRIPT_NAME[568] = "atof";
        CORE[568] = false;
    }
    static {
        HANDLER[569] = "op_scan_array";
        SCRIPT_NAME[569] = "scan_array";
        CORE[569] = false;
    }
    static {
        HANDLER[570] = "op_get_tile_fid";
        CORE[570] = false;
    }
    static {
        HANDLER[574] = "op_force_aimed_shots";
        CORE[574] = false;
    }
    static {
        HANDLER[575] = "op_disable_aimed_shots";
        CORE[575] = false;
    }
    static {
        HANDLER[576] = "op_mark_movie_played";
        SCRIPT_NAME[576] = "mark_movie_played";
        CORE[576] = false;
    }
    static {
        HANDLER[578] = "op_set_critter_skill_points";
        SCRIPT_NAME[578] = "set_critter_skill_points";
        CORE[578] = false;
    }
    static {
        HANDLER[579] = "op_get_critter_skill_points";
        SCRIPT_NAME[579] = "get_critter_skill_points";
        CORE[579] = false;
    }
    static {
        HANDLER[580] = "op_set_available_skill_points";
        SCRIPT_NAME[580] = "set_available_skill_points";
        CORE[580] = false;
    }
    static {
        HANDLER[581] = "op_get_available_skill_points";
        SCRIPT_NAME[581] = "get_available_skill_points";
        CORE[581] = false;
    }
    static {
        HANDLER[582] = "op_mod_skill_points_per_level";
        SCRIPT_NAME[582] = "mod_skill_points_per_level";
        CORE[582] = false;
    }
    static {
        HANDLER[583] = "op_set_perk_freq";
        SCRIPT_NAME[583] = "set_perk_freq";
        CORE[583] = false;
    }
    static {
        HANDLER[587] = "op_tile_under_cursor";
        CORE[587] = false;
    }
    static {
        HANDLER[588] = "op_gdialog_get_barter_mod";
        CORE[588] = false;
    }
    static {
        HANDLER[589] = "op_set_inven_ap_cost";
        CORE[589] = false;
    }
    static {
        HANDLER[590] = "op_substr";
        CORE[590] = false;
    }
    static {
        HANDLER[591] = "op_get_string_length";
        CORE[591] = false;
    }
    static {
        HANDLER[592] = "op_sprintf";
        SCRIPT_NAME[592] = "sprintf";
        CORE[592] = false;
    }
    static {
        HANDLER[593] = "op_charcode";
        SCRIPT_NAME[593] = "charcode";
        CORE[593] = false;
    }
    static {
        HANDLER[595] = "op_type_of";
        SCRIPT_NAME[595] = "typeof";
        CORE[595] = false;
    }
    static {
        HANDLER[596] = "op_save_array";
        SCRIPT_NAME[596] = "save_array";
        CORE[596] = false;
    }
    static {
        HANDLER[597] = "op_load_array";
        SCRIPT_NAME[597] = "load_array";
        CORE[597] = false;
    }
    static {
        HANDLER[598] = "op_get_array_key";
        SCRIPT_NAME[598] = "array_key";
        CORE[598] = false;
    }
    static {
        HANDLER[599] = "op_arrayexpr";
        SCRIPT_NAME[599] = "arrayexpr";
        CORE[599] = false;
    }
    static {
        HANDLER[602] = "op_reg_anim_destroy";
        CORE[602] = false;
    }
    static {
        HANDLER[603] = "op_reg_anim_animate_and_hide";
        CORE[603] = false;
    }
    static {
        HANDLER[604] = "op_reg_anim_combat_check";
        CORE[604] = false;
    }
    static {
        HANDLER[605] = "op_reg_anim_light";
        CORE[605] = false;
    }
    static {
        HANDLER[606] = "op_reg_anim_change_fid";
        CORE[606] = false;
    }
    static {
        HANDLER[607] = "op_reg_anim_take_out";
        CORE[607] = false;
    }
    static {
        HANDLER[608] = "op_reg_anim_turn_towards";
        SCRIPT_NAME[608] = "reg_anim_turn_towards";
        CORE[608] = false;
    }
    static {
        HANDLER[609] = "op_explosions_metarule";
        SCRIPT_NAME[609] = "metarule2_explosions";
        CORE[609] = false;
    }
    static {
        HANDLER[610] = "op_register_hook_proc";
        SCRIPT_NAME[610] = "register_hook_proc";
        CORE[610] = false;
    }
    static {
        HANDLER[611] = "op_power";
        SCRIPT_NAME[611] = "operator";
        CORE[611] = false;
    }
    static {
        HANDLER[612] = "op_log";
        SCRIPT_NAME[612] = "log";
        CORE[612] = false;
    }
    static {
        HANDLER[613] = "op_exponent";
        SCRIPT_NAME[613] = "exponent";
        CORE[613] = false;
    }
    static {
        HANDLER[614] = "op_ceil";
        SCRIPT_NAME[614] = "ceil";
        CORE[614] = false;
    }
    static {
        HANDLER[615] = "op_round";
        SCRIPT_NAME[615] = "round";
        CORE[615] = false;
    }
    static {
        HANDLER[619] = "op_get_message";
        CORE[619] = false;
    }
    static {
        HANDLER[620] = "op_sneak_success";
        CORE[620] = false;
    }
    static {
        HANDLER[621] = "op_tile_light";
        CORE[621] = false;
    }
    static {
        HANDLER[622] = "op_make_straight_path";
        CORE[622] = false;
    }
    static {
        HANDLER[623] = "op_obj_blocking_at";
        CORE[623] = false;
    }
    static {
        HANDLER[624] = "op_tile_get_objects";
        SCRIPT_NAME[624] = "tile_get_objs";
        CORE[624] = false;
    }
    static {
        HANDLER[625] = "op_party_member_list";
        SCRIPT_NAME[625] = "party_member_list";
        CORE[625] = false;
    }
    static {
        HANDLER[626] = "op_make_path";
        SCRIPT_NAME[626] = "path_find_to";
        CORE[626] = false;
    }
    static {
        HANDLER[627] = "op_create_spatial";
        SCRIPT_NAME[627] = "create_spatial";
        CORE[627] = false;
    }
    static {
        HANDLER[628] = "op_art_exists";
        SCRIPT_NAME[628] = "art_exists";
        CORE[628] = false;
    }
    static {
        HANDLER[629] = "op_obj_is_carrying_obj";
        SCRIPT_NAME[629] = "obj_is_carrying_obj";
        CORE[629] = false;
    }
    static {
        HANDLER[630] = "op_sfall_func0";
        SCRIPT_NAME[630] = "sfall_func0";
        CORE[630] = false;
    }
    static {
        HANDLER[631] = "op_sfall_func1";
        SCRIPT_NAME[631] = "sfall_func1";
        CORE[631] = false;
    }
    static {
        HANDLER[632] = "op_sfall_func2";
        SCRIPT_NAME[632] = "sfall_func2";
        CORE[632] = false;
    }
    static {
        HANDLER[633] = "op_sfall_func3";
        SCRIPT_NAME[633] = "sfall_func3";
        CORE[633] = false;
    }
    static {
        HANDLER[634] = "op_sfall_func4";
        CORE[634] = false;
    }
    static {
        HANDLER[635] = "op_sfall_func5";
        CORE[635] = false;
    }
    static {
        HANDLER[636] = "op_sfall_func6";
        CORE[636] = false;
    }
    static {
        HANDLER[639] = "op_div";
        CORE[639] = false;
    }
    static {
        HANDLER[640] = "op_sfall_func7";
        SCRIPT_NAME[640] = "sfall_func7";
        CORE[640] = false;
    }
    static {
        HANDLER[641] = "op_sfall_func8";
        SCRIPT_NAME[641] = "sfall_func8";
        CORE[641] = false;
    }

    private ScriptOpcodes() {
    }

    /** The best available name for a dispatch slot. */
    static String name(int slot) {
        String script = SCRIPT_NAME[slot];
        if (script != null) {
            return script;
        }
        String handler = HANDLER[slot];
        if (handler != null) {
            return handler;
        }
        return "op_" + Integer.toHexString(slot);
    }
}
