package org.fallout2ce;

/**
 * The Fallout 2 script API: the builtins a script can call.
 *
 * <p>Each method is one builtin. The game's own opcodes are the intlib's,
 * and they are the surface the scripts are written against - the rest of
 * the engine exists to serve them. Keeping them behind an interface is
 * what makes a script testable: a script can be run against a mock
 * instead of the game.
 *
 * <p>The argument types are what the builtin reads off the script's value
 * stack, in the order the script compiler passed them. The values the VM
 * holds are looser than this - an int slot may hold a float, a pointer
 * slot may hold a string - so the implementation owns the conversions the
 * original makes.
 *
 * <p>Generated from the C handlers' argument shapes; see the commit that
 * added this file.
 */
public interface GameApi {

    /**
     * Whether this API provides the builtin in the given dispatch slot.
     *
     * <p>The original refuses an opcode it has no handler for before it
     * reads the opcode's arguments, and the script stops with a fatal
     * error. An API that does not provide a builtin has to refuse it the
     * same way, so the slot is checked before the arguments are read.
     */
    default boolean supports(int slot) {
        return true;
    }

    /**
     * {@code opSayEnd} (0x804D) - 3 calls in the shipped scripts.
     */
    void opSayEnd();

    /**
     * {@code opSayStart} (0x804E) - 3 calls in the shipped scripts.
     */
    void opSayStart();

    /**
     * {@code opSayOption} (0x8052) - 12 calls in the shipped scripts.
     */
    void opSayOption(Program.ProgramValue a0, Program.ProgramValue a1);

    /**
     * {@code opSayReply} (0x8053) - 46 calls in the shipped scripts.
     */
    void opSayReply(Program.ProgramValue a0, Program.ProgramValue a1);

    /**
     * {@code opSayMessage} (0x8054) - 12 calls in the shipped scripts.
     */
    void opSayMessage(Program.ProgramValue a0, Program.ProgramValue a1);

    /**
     * {@code opDisplay} (0x806B) - 1 call in the shipped scripts.
     */
    void opDisplay(String a0);

    /**
     * {@code op_give_exp_points} (0x80A1) - 1665 calls in the shipped scripts.
     */
    void giveExpPoints(int a0);

    /**
     * {@code op_scr_return} (0x80A2) - 6 calls in the shipped scripts.
     */
    void scrReturn(int a0);

    /**
     * {@code op_play_sfx} (0x80A3) - 76 calls in the shipped scripts.
     */
    void playSfx(String a0);

    /**
     * {@code op_obj_name} (0x80A4) - 2764 calls in the shipped scripts.
     */
    String objName(Object a0);

    /**
     * {@code op_get_pc_stat} (0x80A6) - 212 calls in the shipped scripts.
     */
    int getPcStat(int a0);

    /**
     * {@code op_tile_contains_pid_obj} (0x80A7) - 1221 calls in the shipped scripts.
     */
    Object tileContainsPidObj(int a0, int a1, int a2);

    /**
     * {@code op_set_map_start} (0x80A8) - 3 calls in the shipped scripts.
     */
    void setMapStart(int a0, int a1, int a2, int a3);

    /**
     * {@code op_override_map_start} (0x80A9) - 56 calls in the shipped scripts.
     */
    void overrideMapStart(int a0, int a1, int a2, int a3);

    /**
     * {@code op_has_skill} (0x80AA) - 502 calls in the shipped scripts.
     */
    int hasSkill(Object a0, int a1);

    /**
     * {@code op_using_skill} (0x80AB) - 103 calls in the shipped scripts.
     */
    int usingSkill(Object a0, int a1);

    /**
     * {@code op_roll_vs_skill} (0x80AC) - 1727 calls in the shipped scripts.
     */
    int rollVsSkill(Object a0, int a1, int a2);

    /**
     * {@code op_do_check} (0x80AE) - 544 calls in the shipped scripts.
     */
    int doCheck(Object a0, int a1, int a2);

    /**
     * {@code op_success} (0x80AF) - 3633 calls in the shipped scripts.
     */
    int success(int a0);

    /**
     * {@code op_critical} (0x80B0) - 4442 calls in the shipped scripts.
     */
    int critical(int a0);

    /**
     * {@code op_mark_area_known} (0x80B2) - 126 calls in the shipped scripts.
     */
    void markAreaKnown(int a0, int a1, int a2);

    /**
     * {@code op_random} (0x80B4) - 9127 calls in the shipped scripts.
     */
    int random(int a0, int a1);

    /**
     * {@code op_move_to} (0x80B6) - 823 calls in the shipped scripts.
     */
    int moveTo(Object a0, int a1, int a2);

    /**
     * {@code op_create_object} (0x80B7) - 6349 calls in the shipped scripts.
     */
    Object createObject(int a0, int a1, int a2, int a3);

    /**
     * {@code op_display_msg} (0x80B8) - 17015 calls in the shipped scripts.
     */
    void displayMsg(String a0);

    /**
     * {@code op_script_overrides} (0x80B9) - 4196 calls in the shipped scripts.
     */
    void scriptOverrides();

    /**
     * {@code op_obj_is_carrying_obj} (0x80BA) - 14686 calls in the shipped scripts.
     */
    int objIsCarryingObj(Object a0, int a1);

    /**
     * {@code op_tile_contains_obj_pid} (0x80BB) - 293 calls in the shipped scripts.
     */
    int tileContainsObjPid(int a0, int a1, int a2);

    /**
     * {@code op_self_obj} (0x80BC) - 97990 calls in the shipped scripts.
     */
    Object selfObj();

    /**
     * {@code op_source_obj} (0x80BD) - 8385 calls in the shipped scripts.
     */
    Object sourceObj();

    /**
     * {@code op_target_obj} (0x80BE) - 212 calls in the shipped scripts.
     */
    Object targetObj();

    /**
     * {@code opGetDude} (0x80BF) - 24317 calls in the shipped scripts.
     */
    Object opGetDude();

    /**
     * {@code op_obj_being_used_with} (0x80C0) - 662 calls in the shipped scripts.
     */
    Object objBeingUsedWith();

    /**
     * {@code op_get_local_var} (0x80C1) - 20115 calls in the shipped scripts.
     */
    Program.ProgramValue getLocalVar(int a0);

    /**
     * {@code op_set_local_var} (0x80C2) - 16193 calls in the shipped scripts.
     */
    void setLocalVar(int a0, Program.ProgramValue a1);

    /**
     * {@code op_get_map_var} (0x80C3) - 1955 calls in the shipped scripts.
     */
    Program.ProgramValue getMapVar(int a0);

    /**
     * {@code op_set_map_var} (0x80C4) - 1671 calls in the shipped scripts.
     */
    void setMapVar(int a0, Program.ProgramValue a1);

    /**
     * {@code op_get_global_var} (0x80C5) - 57300 calls in the shipped scripts.
     */
    Program.ProgramValue getGlobalVar(int a0);

    /**
     * {@code op_set_global_var} (0x80C6) - 60443 calls in the shipped scripts.
     */
    void setGlobalVar(int a0, Program.ProgramValue a1);

    /**
     * {@code op_script_action} (0x80C7) - 602 calls in the shipped scripts.
     */
    int scriptAction();

    /**
     * {@code op_obj_type} (0x80C8) - 2081 calls in the shipped scripts.
     */
    int objType(Object a0);

    /**
     * {@code op_item_subtype} (0x80C9) - 2541 calls in the shipped scripts.
     */
    int itemSubtype(Object a0);

    /**
     * {@code op_get_critter_stat} (0x80CA) - 5834 calls in the shipped scripts.
     */
    int getCritterStat(Object a0, int a1);

    /**
     * {@code op_set_critter_stat} (0x80CB) - 29 calls in the shipped scripts.
     */
    int setCritterStat(Object a0, int a1, int a2);

    /**
     * {@code op_animate_stand_obj} (0x80CC) - 41 calls in the shipped scripts.
     */
    void animateStandObj(Object a0);

    /**
     * {@code animate_stand_reverse_obj} (0x80CD) - 16 calls in the shipped scripts.
     */
    void animateStandReverseObj(Object a0);

    /**
     * {@code animate_move_obj_to_tile} (0x80CE) - 1601 calls in the shipped scripts.
     */
    void animateMoveObjToTile(Object a0, int a1, int a2);

    /**
     * {@code tile_in_tile_rect} (0x80CF) - 1073 calls in the shipped scripts.
     */
    int tileInTileRect(int a0, int a1, int a2, int a3, int a4);

    /**
     * {@code op_attack} (0x80D0) - 1184 calls in the shipped scripts.
     */
    void attack(Object a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7);

    /**
     * {@code op_tile_distance} (0x80D2) - 839 calls in the shipped scripts.
     */
    int tileDistance(int a0, int a1);

    /**
     * {@code op_tile_distance_objs} (0x80D3) - 26018 calls in the shipped scripts.
     */
    int tileDistanceObjs(Object a0, Object a1);

    /**
     * {@code op_tile_num} (0x80D4) - 9044 calls in the shipped scripts.
     */
    int tileNum(Object a0);

    /**
     * {@code op_tile_num_in_direction} (0x80D5) - 5860 calls in the shipped scripts.
     */
    int tileNumInDirection(int a0, int a1, int a2);

    /**
     * {@code op_drop_obj} (0x80D7) - 2 calls in the shipped scripts.
     */
    void dropObj(Object a0);

    /**
     * {@code op_add_obj_to_inven} (0x80D8) - 2535 calls in the shipped scripts.
     */
    void addObjToInven(Object a0, Object a1);

    /**
     * {@code op_rm_obj_from_inven} (0x80D9) - 2169 calls in the shipped scripts.
     */
    void rmObjFromInven(Object a0, Object a1);

    /**
     * {@code op_wield_obj_critter} (0x80DA) - 621 calls in the shipped scripts.
     */
    void wieldObjCritter(Object a0, Object a1);

    /**
     * {@code op_use_obj} (0x80DB) - 11 calls in the shipped scripts.
     */
    void useObj(Object a0);

    /**
     * {@code op_obj_can_see_obj} (0x80DC) - 1836 calls in the shipped scripts.
     */
    int objCanSeeObj(Object a0, Object a1);

    /**
     * {@code op_start_gdialog} (0x80DE) - 1224 calls in the shipped scripts.
     */
    void startGdialog(int a0, Object a1, int a2, int a3, int a4);

    /**
     * {@code op_end_gdialog} (0x80DF) - 1224 calls in the shipped scripts.
     */
    void endGdialog();

    /**
     * {@code op_metarule3} (0x80E1) - 994 calls in the shipped scripts.
     */
    Program.ProgramValue metarule3(int a0, Program.ProgramValue a1, Program.ProgramValue a2, Program.ProgramValue a3);

    /**
     * {@code op_set_obj_visibility} (0x80E3) - 922 calls in the shipped scripts.
     */
    void setObjVisibility(Object a0, int a1);

    /**
     * {@code op_load_map} (0x80E4) - 82 calls in the shipped scripts.
     */
    void loadMap(Program.ProgramValue a0, int a1);

    /**
     * {@code op_wm_area_set_pos} (0x80E5) - 96 calls in the shipped scripts.
     */
    void wmAreaSetPos(int a0, int a1, int a2);

    /**
     * {@code op_set_exit_grids} (0x80E6) - 104 calls in the shipped scripts.
     */
    void setExitGrids(int a0, int a1, int a2, int a3, int a4);

    /**
     * {@code op_anim_busy} (0x80E7) - 1552 calls in the shipped scripts.
     */
    int animBusy(Object a0);

    /**
     * {@code op_critter_heal} (0x80E8) - 166 calls in the shipped scripts.
     */
    int critterHeal(Object a0, int a1);

    /**
     * {@code op_set_light_level} (0x80E9) - 2466 calls in the shipped scripts.
     */
    void setLightLevel(int a0);

    /**
     * {@code op_game_time} (0x80EA) - 1697 calls in the shipped scripts.
     */
    int gameTime();

    /**
     * {@code op_elevation} (0x80EC) - 2863 calls in the shipped scripts.
     */
    int elevation(Object a0);

    /**
     * {@code op_kill_critter} (0x80ED) - 72 calls in the shipped scripts.
     */
    void killCritter(Object a0, int a1);

    /**
     * {@code op_kill_critter_type} (0x80EE) - 85 calls in the shipped scripts.
     */
    void killCritterType(int a0, int a1);

    /**
     * {@code op_critter_damage} (0x80EF) - 428 calls in the shipped scripts.
     */
    void critterDamage(Object a0, int a1, int a2);

    /**
     * {@code op_add_timer_event} (0x80F0) - 2255 calls in the shipped scripts.
     */
    void addTimerEvent(Object a0, int a1, int a2);

    /**
     * {@code op_rm_timer_event} (0x80F1) - 46 calls in the shipped scripts.
     */
    void rmTimerEvent(Object a0);

    /**
     * {@code op_game_ticks} (0x80F2) - 1646 calls in the shipped scripts.
     */
    int gameTicks(int a0);

    /**
     * {@code op_has_trait} (0x80F3) - 2754 calls in the shipped scripts.
     */
    int hasTrait(int a0, Object a1, int a2);

    /**
     * {@code op_destroy_object} (0x80F4) - 3983 calls in the shipped scripts.
     */
    void destroyObject(Object a0);

    /**
     * {@code op_obj_can_hear_obj} (0x80F5) - 7 calls in the shipped scripts.
     */
    int objCanHearObj(Object a0, Object a1);

    /**
     * {@code op_game_time_hour} (0x80F6) - 5467 calls in the shipped scripts.
     */
    int gameTimeHour();

    /**
     * {@code op_fixed_param} (0x80F7) - 1094 calls in the shipped scripts.
     */
    int fixedParam();

    /**
     * {@code op_tile_is_visible} (0x80F8) - 1 call in the shipped scripts.
     */
    int tileIsVisible(int a0);

    /**
     * {@code op_dialogue_system_enter} (0x80F9) - 150 calls in the shipped scripts.
     */
    void dialogueSystemEnter();

    /**
     * {@code op_action_being_used} (0x80FA) - 362 calls in the shipped scripts.
     */
    int actionBeingUsed();

    /**
     * {@code critter_state} (0x80FB) - 507 calls in the shipped scripts.
     */
    int critterState(Object a0);

    /**
     * {@code op_game_time_advance} (0x80FC) - 370 calls in the shipped scripts.
     */
    void gameTimeAdvance(int a0);

    /**
     * {@code op_radiation_inc} (0x80FD) - 14 calls in the shipped scripts.
     */
    void radiationInc(Object a0, int a1);

    /**
     * {@code op_radiation_dec} (0x80FE) - 17 calls in the shipped scripts.
     */
    void radiationDec(Object a0, int a1);

    /**
     * {@code critter_attempt_placement} (0x80FF) - 471 calls in the shipped scripts.
     */
    int critterAttemptPlacement(Object a0, int a1, int a2);

    /**
     * {@code op_obj_pid} (0x8100) - 4332 calls in the shipped scripts.
     */
    int objPid(Object a0);

    /**
     * {@code op_cur_map_index} (0x8101) - 1712 calls in the shipped scripts.
     */
    int curMapIndex();

    /**
     * {@code op_critter_add_trait} (0x8102) - 1780 calls in the shipped scripts.
     */
    int critterAddTrait(Object a0, int a1, int a2, int a3);

    /**
     * {@code critter_rm_trait} (0x8103) - 4 calls in the shipped scripts.
     */
    int critterRmTrait(Object a0, int a1, int a2, int a3);

    /**
     * {@code op_proto_data} (0x8104) - 131 calls in the shipped scripts.
     */
    Program.ProgramValue protoData(int a0, int a1);

    /**
     * {@code op_message_str} (0x8105) - 32040 calls in the shipped scripts.
     */
    String messageStr(int a0, int a1);

    /**
     * {@code op_critter_inven_obj} (0x8106) - 11327 calls in the shipped scripts.
     */
    Program.ProgramValue critterInvenObj(Object a0, int a1);

    /**
     * {@code op_obj_set_light_level} (0x8107) - 4 calls in the shipped scripts.
     */
    void objSetLightLevel(Object a0, int a1, int a2);

    /**
     * {@code op_inven_cmds} (0x8109) - 4 calls in the shipped scripts.
     */
    Object invenCmds(Object a0, int a1, int a2);

    /**
     * {@code op_float_msg} (0x810A) - 3942 calls in the shipped scripts.
     */
    void floatMsg(Program.ProgramValue a0, int a1);

    /**
     * {@code op_metarule} (0x810B) - 15675 calls in the shipped scripts.
     */
    int metarule(int a0, Program.ProgramValue a1);

    /**
     * {@code op_anim} (0x810C) - 866 calls in the shipped scripts.
     */
    void anim(Object a0, int a1, int a2, Program.ProgramValue a3);

    /**
     * {@code op_obj_carrying_pid_obj} (0x810D) - 5004 calls in the shipped scripts.
     */
    Object objCarryingPidObj(Object a0, int a1);

    /**
     * {@code op_reg_anim_func} (0x810E) - 1344 calls in the shipped scripts.
     */
    void regAnimFunc(int a0, Program.ProgramValue a1);

    /**
     * {@code op_reg_anim_animate} (0x810F) - 223 calls in the shipped scripts.
     */
    void regAnimAnimate(Object a0, int a1, int a2);

    /**
     * {@code op_reg_anim_animate_reverse} (0x8110) - 2 calls in the shipped scripts.
     */
    void regAnimAnimateReverse(Object a0, int a1, int a2);

    /**
     * {@code op_reg_anim_obj_move_to_tile} (0x8113) - 5 calls in the shipped scripts.
     */
    void regAnimObjMoveToTile(Object a0, int a1, int a2);

    /**
     * {@code op_play_gmovie} (0x8115) - 9 calls in the shipped scripts.
     */
    void playGmovie(int a0);

    /**
     * {@code op_add_mult_objs_to_inven} (0x8116) - 2482 calls in the shipped scripts.
     */
    void addMultObjsToInven(Object a0, Object a1, int a2);

    /**
     * {@code rm_mult_objs_from_inven} (0x8117) - 2351 calls in the shipped scripts.
     */
    int rmMultObjsFromInven(Object a0, Object a1, int a2);

    /**
     * {@code op_month} (0x8118) - 897 calls in the shipped scripts.
     */
    int month();

    /**
     * {@code op_day} (0x8119) - 52 calls in the shipped scripts.
     */
    int day();

    /**
     * {@code op_explosion} (0x811A) - 212 calls in the shipped scripts.
     */
    void explosion(int a0, int a1, int a2);

    /**
     * {@code _op_gsay_start} (0x811C) - 1221 calls in the shipped scripts.
     */
    void opGsayStart();

    /**
     * {@code _op_gsay_end} (0x811D) - 1221 calls in the shipped scripts.
     */
    void opGsayEnd();

    /**
     * {@code op_gsay_reply} (0x811E) - 10354 calls in the shipped scripts.
     */
    void gsayReply(int a0, Program.ProgramValue a1);

    /**
     * {@code op_gsay_option} (0x811F) - 214 calls in the shipped scripts.
     */
    void gsayOption(int a0, Program.ProgramValue a1, Program.ProgramValue a2, int a3);

    /**
     * {@code op_gsay_message} (0x8120) - 936 calls in the shipped scripts.
     */
    void gsayMessage(int a0, Program.ProgramValue a1, int a2);

    /**
     * {@code op_giq_option} (0x8121) - 26082 calls in the shipped scripts.
     */
    void giqOption(int a0, int a1, Program.ProgramValue a2, Program.ProgramValue a3, int a4);

    /**
     * {@code op_poison} (0x8122) - 44 calls in the shipped scripts.
     */
    void poison(Object a0, int a1);

    /**
     * {@code op_get_poison} (0x8123) - 126 calls in the shipped scripts.
     */
    int getPoison(Object a0);

    /**
     * {@code op_party_add} (0x8124) - 43 calls in the shipped scripts.
     */
    void partyAdd(Object a0);

    /**
     * {@code op_party_remove} (0x8125) - 91 calls in the shipped scripts.
     */
    void partyRemove(Object a0);

    /**
     * {@code op_reg_anim_animate_forever} (0x8126) - 2 calls in the shipped scripts.
     */
    void regAnimAnimateForever(Object a0, int a1);

    /**
     * {@code op_critter_injure} (0x8127) - 301 calls in the shipped scripts.
     */
    void critterInjure(Object a0, int a1);

    /**
     * {@code op_is_in_combat} (0x8128) - 322 calls in the shipped scripts.
     */
    int isInCombat();

    /**
     * {@code op_gdialog_barter} (0x8129) - 77 calls in the shipped scripts.
     */
    void gdialogBarter(int a0);

    /**
     * {@code op_inven_unwield} (0x812C) - 28 calls in the shipped scripts.
     */
    void invenUnwield();

    /**
     * {@code op_obj_is_locked} (0x812D) - 416 calls in the shipped scripts.
     */
    int objIsLocked(Object a0);

    /**
     * {@code op_obj_lock} (0x812E) - 553 calls in the shipped scripts.
     */
    void objLock(Object a0);

    /**
     * {@code op_obj_unlock} (0x812F) - 682 calls in the shipped scripts.
     */
    void objUnlock(Object a0);

    /**
     * {@code op_obj_is_open} (0x8130) - 170 calls in the shipped scripts.
     */
    int objIsOpen(Object a0);

    /**
     * {@code op_obj_open} (0x8131) - 90 calls in the shipped scripts.
     */
    void objOpen(Object a0);

    /**
     * {@code op_obj_close} (0x8132) - 235 calls in the shipped scripts.
     */
    void objClose(Object a0);

    /**
     * {@code op_game_ui_disable} (0x8133) - 87 calls in the shipped scripts.
     */
    void gameUiDisable();

    /**
     * {@code op_game_ui_enable} (0x8134) - 89 calls in the shipped scripts.
     */
    void gameUiEnable();

    /**
     * {@code op_gfade_out} (0x8136) - 462 calls in the shipped scripts.
     */
    void gfadeOut(int a0);

    /**
     * {@code op_gfade_in} (0x8137) - 460 calls in the shipped scripts.
     */
    void gfadeIn(int a0);

    /**
     * {@code op_item_caps_total} (0x8138) - 632 calls in the shipped scripts.
     */
    int itemCapsTotal(Object a0);

    /**
     * {@code op_item_caps_adjust} (0x8139) - 889 calls in the shipped scripts.
     */
    int itemCapsAdjust(Object a0, int a1);

    /**
     * {@code op_reg_anim_play_sfx} (0x813B) - 28 calls in the shipped scripts.
     */
    void regAnimPlaySfx(Object a0, String a1, int a2);

    /**
     * {@code op_critter_mod_skill} (0x813C) - 60 calls in the shipped scripts.
     */
    void critterModSkill(Object a0, int a1, int a2);

    /**
     * {@code op_sfx_build_char_name} (0x813D) - 21 calls in the shipped scripts.
     */
    String sfxBuildCharName(Object a0, int a1, int a2);

    /**
     * {@code op_sfx_build_weapon_name} (0x8141) - 4 calls in the shipped scripts.
     */
    String sfxBuildWeaponName(int a0, Object a1, int a2, Object a3);

    /**
     * {@code op_attack_setup} (0x8143) - 206 calls in the shipped scripts.
     */
    void attackSetup(Object a0, Object a1);

    /**
     * {@code op_destroy_mult_objs} (0x8144) - 4 calls in the shipped scripts.
     */
    int destroyMultObjs(Object a0, int a1);

    /**
     * {@code op_use_obj_on_obj} (0x8145) - 91 calls in the shipped scripts.
     */
    void useObjOnObj(Object a0, Object a1);

    /**
     * {@code op_endgame_slideshow} (0x8146) - 1 call in the shipped scripts.
     */
    void endgameSlideshow();

    /**
     * {@code op_move_obj_inven_to_obj} (0x8147) - 247 calls in the shipped scripts.
     */
    void moveObjInvenToObj(Object a0, Object a1);

    /**
     * {@code op_obj_art_fid} (0x8149) - 39 calls in the shipped scripts.
     */
    int objArtFid(Object a0);

    /**
     * {@code op_art_anim} (0x814A) - 20 calls in the shipped scripts.
     */
    int artAnim(int a0);

    /**
     * {@code op_party_member_obj} (0x814B) - 55310 calls in the shipped scripts.
     */
    Object partyMemberObj(int a0);

    /**
     * {@code op_rotation_to_tile} (0x814C) - 2149 calls in the shipped scripts.
     */
    int rotationToTile(int a0, int a1);

    /**
     * {@code op_jam_lock} (0x814D) - 67 calls in the shipped scripts.
     */
    void jamLock(Object a0);

    /**
     * {@code op_gdialog_set_barter_mod} (0x814E) - 21 calls in the shipped scripts.
     */
    void gdialogSetBarterMod(int a0);

    /**
     * {@code op_obj_on_screen} (0x8150) - 49 calls in the shipped scripts.
     */
    int objOnScreen(Object a0);

    /**
     * {@code op_critter_is_fleeing} (0x8151) - 340 calls in the shipped scripts.
     */
    int critterIsFleeing(Object a0);

    /**
     * {@code op_critter_set_flee_state} (0x8152) - 30 calls in the shipped scripts.
     */
    void critterSetFleeState(Object a0, int a1);

    /**
     * {@code op_terminate_combat} (0x8153) - 27 calls in the shipped scripts.
     */
    void terminateCombat();

    /**
     * {@code op_debug_msg} (0x8154) - 13552 calls in the shipped scripts.
     */
    void debugMsg(String a0);

    /**
     * {@code op_critter_stop_attacking} (0x8155) - 1 call in the shipped scripts.
     */
    void critterStopAttacking(Object a0);

}
