package org.fallout2ce;

/**
 * The script API used when the game is not behind it.
 *
 * <p>Every builtin reports that it is not available and the script stops,
 * the way the original does when a procedure it calls does not exist. The
 * game is a different implementation of {@link ScriptApi}, and so is
 * {@link MockScriptApi}, which records the calls instead.
 *
 * <p>Generated together with {@link ScriptApi}.
 */
class UnsupportedScriptApi implements ScriptApi {

    @Override
    public boolean supports(int slot) {
        return false;
    }

    protected static Program.FatalError unsupported(String name) {
        return new Program.FatalError(
            String.format("\nScript Error: %s: this builtin is not available.", name));
    }

    @Override
    public void opSayEnd(Program program) {
        throw unsupported("opSayEnd");
    }

    @Override
    public void opSayStart(Program program) {
        throw unsupported("opSayStart");
    }

    @Override
    public void opSayOption(Program program, Program.ProgramValue a0, Program.ProgramValue a1) {
        throw unsupported("opSayOption");
    }

    @Override
    public void opSayReply(Program program, Program.ProgramValue a0, Program.ProgramValue a1) {
        throw unsupported("opSayReply");
    }

    @Override
    public void opSayMessage(Program program, Program.ProgramValue a0, Program.ProgramValue a1) {
        throw unsupported("opSayMessage");
    }

    @Override
    public void opDisplay(Program program, String a0) {
        throw unsupported("opDisplay");
    }

    @Override
    public void giveExpPoints(Program program, int a0) {
        throw unsupported("op_give_exp_points");
    }

    @Override
    public void scrReturn(Program program, int a0) {
        throw unsupported("op_scr_return");
    }

    @Override
    public void playSfx(Program program, String a0) {
        throw unsupported("op_play_sfx");
    }

    @Override
    public String objName(Program program, Object a0) {
        throw unsupported("op_obj_name");
    }

    @Override
    public int getPcStat(Program program, int a0) {
        throw unsupported("op_get_pc_stat");
    }

    @Override
    public Object tileContainsPidObj(Program program, int a0, int a1, int a2) {
        throw unsupported("op_tile_contains_pid_obj");
    }

    @Override
    public void setMapStart(Program program, int a0, int a1, int a2, int a3) {
        throw unsupported("op_set_map_start");
    }

    @Override
    public void overrideMapStart(Program program, int a0, int a1, int a2, int a3) {
        throw unsupported("op_override_map_start");
    }

    @Override
    public int hasSkill(Program program, Object a0, int a1) {
        throw unsupported("op_has_skill");
    }

    @Override
    public int usingSkill(Program program, Object a0, int a1) {
        throw unsupported("op_using_skill");
    }

    @Override
    public int rollVsSkill(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_roll_vs_skill");
    }

    @Override
    public int doCheck(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_do_check");
    }

    @Override
    public int success(Program program, int a0) {
        throw unsupported("op_success");
    }

    @Override
    public int critical(Program program, int a0) {
        throw unsupported("op_critical");
    }

    @Override
    public void markAreaKnown(Program program, int a0, int a1, int a2) {
        throw unsupported("op_mark_area_known");
    }

    @Override
    public int random(Program program) {
        throw unsupported("op_random");
    }

    @Override
    public int moveTo(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_move_to");
    }

    @Override
    public Object createObject(Program program, int a0, int a1, int a2, int a3) {
        throw unsupported("op_create_object");
    }

    @Override
    public void displayMsg(Program program, String a0) {
        throw unsupported("op_display_msg");
    }

    @Override
    public void scriptOverrides(Program program) {
        throw unsupported("op_script_overrides");
    }

    @Override
    public int objIsCarryingObj(Program program, Object a0, int a1) {
        throw unsupported("op_obj_is_carrying_obj");
    }

    @Override
    public int tileContainsObjPid(Program program, int a0, int a1, int a2) {
        throw unsupported("op_tile_contains_obj_pid");
    }

    @Override
    public Object selfObj(Program program) {
        throw unsupported("op_self_obj");
    }

    @Override
    public Object sourceObj(Program program) {
        throw unsupported("op_source_obj");
    }

    @Override
    public Object targetObj(Program program) {
        throw unsupported("op_target_obj");
    }

    @Override
    public Object opGetDude(Program program) {
        throw unsupported("opGetDude");
    }

    @Override
    public Object objBeingUsedWith(Program program) {
        throw unsupported("op_obj_being_used_with");
    }

    @Override
    public Program.ProgramValue getLocalVar(Program program, int a0) {
        throw unsupported("op_get_local_var");
    }

    @Override
    public void setLocalVar(Program program, int a0, Program.ProgramValue a1) {
        throw unsupported("op_set_local_var");
    }

    @Override
    public Program.ProgramValue getMapVar(Program program, int a0) {
        throw unsupported("op_get_map_var");
    }

    @Override
    public void setMapVar(Program program, int a0, Program.ProgramValue a1) {
        throw unsupported("op_set_map_var");
    }

    @Override
    public Program.ProgramValue getGlobalVar(Program program, int a0) {
        throw unsupported("op_get_global_var");
    }

    @Override
    public void setGlobalVar(Program program, int a0, Program.ProgramValue a1) {
        throw unsupported("op_set_global_var");
    }

    @Override
    public int scriptAction(Program program) {
        throw unsupported("op_script_action");
    }

    @Override
    public int objType(Program program, Object a0) {
        throw unsupported("op_obj_type");
    }

    @Override
    public int itemSubtype(Program program, Object a0) {
        throw unsupported("op_item_subtype");
    }

    @Override
    public int getCritterStat(Program program, Object a0, int a1) {
        throw unsupported("op_get_critter_stat");
    }

    @Override
    public int setCritterStat(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_set_critter_stat");
    }

    @Override
    public void animateStandObj(Program program, Object a0) {
        throw unsupported("op_animate_stand_obj");
    }

    @Override
    public void animateStandReverseObj(Program program, Object a0) {
        throw unsupported("animate_stand_reverse_obj");
    }

    @Override
    public void animateMoveObjToTile(Program program, Object a0, int a1, int a2) {
        throw unsupported("animate_move_obj_to_tile");
    }

    @Override
    public int tileInTileRect(Program program) {
        throw unsupported("tile_in_tile_rect");
    }

    @Override
    public void attack(Program program) {
        throw unsupported("op_attack");
    }

    @Override
    public int tileDistance(Program program, int a0, int a1) {
        throw unsupported("op_tile_distance");
    }

    @Override
    public int tileDistanceObjs(Program program, Object a0, Object a1) {
        throw unsupported("op_tile_distance_objs");
    }

    @Override
    public int tileNum(Program program, Object a0) {
        throw unsupported("op_tile_num");
    }

    @Override
    public int tileNumInDirection(Program program, int a0, int a1, int a2) {
        throw unsupported("op_tile_num_in_direction");
    }

    @Override
    public void dropObj(Program program, Object a0) {
        throw unsupported("op_drop_obj");
    }

    @Override
    public void addObjToInven(Program program, Object a0, Object a1) {
        throw unsupported("op_add_obj_to_inven");
    }

    @Override
    public void rmObjFromInven(Program program, Object a0, Object a1) {
        throw unsupported("op_rm_obj_from_inven");
    }

    @Override
    public void wieldObjCritter(Program program, Object a0, Object a1) {
        throw unsupported("op_wield_obj_critter");
    }

    @Override
    public void useObj(Program program, Object a0) {
        throw unsupported("op_use_obj");
    }

    @Override
    public int objCanSeeObj(Program program, Object a0, Object a1) {
        throw unsupported("op_obj_can_see_obj");
    }

    @Override
    public void startGdialog(Program program, int a0, Object a1, int a2, int a3, int a4) {
        throw unsupported("op_start_gdialog");
    }

    @Override
    public void endGdialog(Program program) {
        throw unsupported("op_end_gdialog");
    }

    @Override
    public Program.ProgramValue metarule3(Program program, int a0, Program.ProgramValue a1, Program.ProgramValue a2, Program.ProgramValue a3) {
        throw unsupported("op_metarule3");
    }

    @Override
    public void setObjVisibility(Program program, Object a0, int a1) {
        throw unsupported("op_set_obj_visibility");
    }

    @Override
    public void loadMap(Program program, Program.ProgramValue a0, int a1) {
        throw unsupported("op_load_map");
    }

    @Override
    public void wmAreaSetPos(Program program, int a0, int a1, int a2) {
        throw unsupported("op_wm_area_set_pos");
    }

    @Override
    public void setExitGrids(Program program, int a0, int a1, int a2, int a3, int a4) {
        throw unsupported("op_set_exit_grids");
    }

    @Override
    public int animBusy(Program program, Object a0) {
        throw unsupported("op_anim_busy");
    }

    @Override
    public int critterHeal(Program program, Object a0, int a1) {
        throw unsupported("op_critter_heal");
    }

    @Override
    public void setLightLevel(Program program, int a0) {
        throw unsupported("op_set_light_level");
    }

    @Override
    public int gameTime(Program program) {
        throw unsupported("op_game_time");
    }

    @Override
    public int elevation(Program program, Object a0) {
        throw unsupported("op_elevation");
    }

    @Override
    public void killCritter(Program program, Object a0, int a1) {
        throw unsupported("op_kill_critter");
    }

    @Override
    public void killCritterType(Program program, int a0, int a1) {
        throw unsupported("op_kill_critter_type");
    }

    @Override
    public void critterDamage(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_critter_damage");
    }

    @Override
    public void addTimerEvent(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_add_timer_event");
    }

    @Override
    public void rmTimerEvent(Program program, Object a0) {
        throw unsupported("op_rm_timer_event");
    }

    @Override
    public int gameTicks(Program program, int a0) {
        throw unsupported("op_game_ticks");
    }

    @Override
    public int hasTrait(Program program, int a0, Object a1, int a2) {
        throw unsupported("op_has_trait");
    }

    @Override
    public void destroyObject(Program program, Object a0) {
        throw unsupported("op_destroy_object");
    }

    @Override
    public int objCanHearObj(Program program, Object a0, Object a1) {
        throw unsupported("op_obj_can_hear_obj");
    }

    @Override
    public int gameTimeHour(Program program) {
        throw unsupported("op_game_time_hour");
    }

    @Override
    public int fixedParam(Program program) {
        throw unsupported("op_fixed_param");
    }

    @Override
    public int tileIsVisible(Program program, int a0) {
        throw unsupported("op_tile_is_visible");
    }

    @Override
    public void dialogueSystemEnter(Program program) {
        throw unsupported("op_dialogue_system_enter");
    }

    @Override
    public int actionBeingUsed(Program program) {
        throw unsupported("op_action_being_used");
    }

    @Override
    public int critterState(Program program, Object a0) {
        throw unsupported("critter_state");
    }

    @Override
    public void gameTimeAdvance(Program program, int a0) {
        throw unsupported("op_game_time_advance");
    }

    @Override
    public void radiationInc(Program program, Object a0, int a1) {
        throw unsupported("op_radiation_inc");
    }

    @Override
    public void radiationDec(Program program, Object a0, int a1) {
        throw unsupported("op_radiation_dec");
    }

    @Override
    public int critterAttemptPlacement(Program program, Object a0, int a1, int a2) {
        throw unsupported("critter_attempt_placement");
    }

    @Override
    public int objPid(Program program, Object a0) {
        throw unsupported("op_obj_pid");
    }

    @Override
    public int curMapIndex(Program program) {
        throw unsupported("op_cur_map_index");
    }

    @Override
    public int critterAddTrait(Program program, Object a0, int a1, int a2, int a3) {
        throw unsupported("op_critter_add_trait");
    }

    @Override
    public int critterRmTrait(Program program, Object a0, int a1, int a2, int a3) {
        throw unsupported("critter_rm_trait");
    }

    @Override
    public Program.ProgramValue protoData(Program program, int a0, int a1) {
        throw unsupported("op_proto_data");
    }

    @Override
    public String messageStr(Program program, int a0, int a1) {
        throw unsupported("op_message_str");
    }

    @Override
    public Program.ProgramValue critterInvenObj(Program program, Object a0, int a1) {
        throw unsupported("op_critter_inven_obj");
    }

    @Override
    public void objSetLightLevel(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_obj_set_light_level");
    }

    @Override
    public Object invenCmds(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_inven_cmds");
    }

    @Override
    public void floatMsg(Program program, Program.ProgramValue a0, int a1) {
        throw unsupported("op_float_msg");
    }

    @Override
    public int metarule(Program program, int a0, Program.ProgramValue a1) {
        throw unsupported("op_metarule");
    }

    @Override
    public void anim(Program program, Object a0, int a1, int a2, Program.ProgramValue a3) {
        throw unsupported("op_anim");
    }

    @Override
    public Object objCarryingPidObj(Program program, Object a0, int a1) {
        throw unsupported("op_obj_carrying_pid_obj");
    }

    @Override
    public void regAnimFunc(Program program, int a0, Program.ProgramValue a1) {
        throw unsupported("op_reg_anim_func");
    }

    @Override
    public void regAnimAnimate(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_reg_anim_animate");
    }

    @Override
    public void regAnimAnimateReverse(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_reg_anim_animate_reverse");
    }

    @Override
    public void regAnimObjMoveToTile(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_reg_anim_obj_move_to_tile");
    }

    @Override
    public void playGmovie(Program program, int a0) {
        throw unsupported("op_play_gmovie");
    }

    @Override
    public void addMultObjsToInven(Program program, Object a0, Object a1, int a2) {
        throw unsupported("op_add_mult_objs_to_inven");
    }

    @Override
    public int rmMultObjsFromInven(Program program, Object a0, Object a1, int a2) {
        throw unsupported("rm_mult_objs_from_inven");
    }

    @Override
    public int month(Program program) {
        throw unsupported("op_month");
    }

    @Override
    public int day(Program program) {
        throw unsupported("op_day");
    }

    @Override
    public void explosion(Program program, int a0, int a1, int a2) {
        throw unsupported("op_explosion");
    }

    @Override
    public void opGsayStart(Program program) {
        throw unsupported("_op_gsay_start");
    }

    @Override
    public void opGsayEnd(Program program) {
        throw unsupported("_op_gsay_end");
    }

    @Override
    public void gsayReply(Program program, int a0, Program.ProgramValue a1) {
        throw unsupported("op_gsay_reply");
    }

    @Override
    public void gsayOption(Program program, int a0, Program.ProgramValue a1, Program.ProgramValue a2, int a3) {
        throw unsupported("op_gsay_option");
    }

    @Override
    public void gsayMessage(Program program, int a0, Program.ProgramValue a1, int a2) {
        throw unsupported("op_gsay_message");
    }

    @Override
    public void giqOption(Program program, int a0, int a1, Program.ProgramValue a2, Program.ProgramValue a3, int a4) {
        throw unsupported("op_giq_option");
    }

    @Override
    public void poison(Program program, Object a0, int a1) {
        throw unsupported("op_poison");
    }

    @Override
    public int getPoison(Program program, Object a0) {
        throw unsupported("op_get_poison");
    }

    @Override
    public void partyAdd(Program program, Object a0) {
        throw unsupported("op_party_add");
    }

    @Override
    public void partyRemove(Program program, Object a0) {
        throw unsupported("op_party_remove");
    }

    @Override
    public void regAnimAnimateForever(Program program, Object a0, int a1) {
        throw unsupported("op_reg_anim_animate_forever");
    }

    @Override
    public void critterInjure(Program program, Object a0, int a1) {
        throw unsupported("op_critter_injure");
    }

    @Override
    public int isInCombat(Program program) {
        throw unsupported("op_is_in_combat");
    }

    @Override
    public void gdialogBarter(Program program, int a0) {
        throw unsupported("op_gdialog_barter");
    }

    @Override
    public void invenUnwield(Program program) {
        throw unsupported("op_inven_unwield");
    }

    @Override
    public int objIsLocked(Program program, Object a0) {
        throw unsupported("op_obj_is_locked");
    }

    @Override
    public void objLock(Program program, Object a0) {
        throw unsupported("op_obj_lock");
    }

    @Override
    public void objUnlock(Program program, Object a0) {
        throw unsupported("op_obj_unlock");
    }

    @Override
    public int objIsOpen(Program program, Object a0) {
        throw unsupported("op_obj_is_open");
    }

    @Override
    public void objOpen(Program program, Object a0) {
        throw unsupported("op_obj_open");
    }

    @Override
    public void objClose(Program program, Object a0) {
        throw unsupported("op_obj_close");
    }

    @Override
    public void gameUiDisable(Program program) {
        throw unsupported("op_game_ui_disable");
    }

    @Override
    public void gameUiEnable(Program program) {
        throw unsupported("op_game_ui_enable");
    }

    @Override
    public void gfadeOut(Program program, int a0) {
        throw unsupported("op_gfade_out");
    }

    @Override
    public void gfadeIn(Program program, int a0) {
        throw unsupported("op_gfade_in");
    }

    @Override
    public int itemCapsTotal(Program program, Object a0) {
        throw unsupported("op_item_caps_total");
    }

    @Override
    public int itemCapsAdjust(Program program, Object a0, int a1) {
        throw unsupported("op_item_caps_adjust");
    }

    @Override
    public void regAnimPlaySfx(Program program, Object a0, String a1, int a2) {
        throw unsupported("op_reg_anim_play_sfx");
    }

    @Override
    public void critterModSkill(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_critter_mod_skill");
    }

    @Override
    public String sfxBuildCharName(Program program, Object a0, int a1, int a2) {
        throw unsupported("op_sfx_build_char_name");
    }

    @Override
    public String sfxBuildWeaponName(Program program, int a0, Object a1, int a2, Object a3) {
        throw unsupported("op_sfx_build_weapon_name");
    }

    @Override
    public void attackSetup(Program program, Object a0, Object a1) {
        throw unsupported("op_attack_setup");
    }

    @Override
    public int destroyMultObjs(Program program, Object a0, int a1) {
        throw unsupported("op_destroy_mult_objs");
    }

    @Override
    public void useObjOnObj(Program program, Object a0, Object a1) {
        throw unsupported("op_use_obj_on_obj");
    }

    @Override
    public void endgameSlideshow(Program program) {
        throw unsupported("op_endgame_slideshow");
    }

    @Override
    public void moveObjInvenToObj(Program program, Object a0, Object a1) {
        throw unsupported("op_move_obj_inven_to_obj");
    }

    @Override
    public int objArtFid(Program program, Object a0) {
        throw unsupported("op_obj_art_fid");
    }

    @Override
    public int artAnim(Program program, int a0) {
        throw unsupported("op_art_anim");
    }

    @Override
    public Object partyMemberObj(Program program, int a0) {
        throw unsupported("op_party_member_obj");
    }

    @Override
    public int rotationToTile(Program program, int a0, int a1) {
        throw unsupported("op_rotation_to_tile");
    }

    @Override
    public void jamLock(Program program, Object a0) {
        throw unsupported("op_jam_lock");
    }

    @Override
    public void gdialogSetBarterMod(Program program, int a0) {
        throw unsupported("op_gdialog_set_barter_mod");
    }

    @Override
    public int objOnScreen(Program program, Object a0) {
        throw unsupported("op_obj_on_screen");
    }

    @Override
    public int critterIsFleeing(Program program, Object a0) {
        throw unsupported("op_critter_is_fleeing");
    }

    @Override
    public void critterSetFleeState(Program program, Object a0, int a1) {
        throw unsupported("op_critter_set_flee_state");
    }

    @Override
    public void terminateCombat(Program program) {
        throw unsupported("op_terminate_combat");
    }

    @Override
    public void debugMsg(Program program, String a0) {
        throw unsupported("op_debug_msg");
    }

    @Override
    public void critterStopAttacking(Program program, Object a0) {
        throw unsupported("op_critter_stop_attacking");
    }

}
